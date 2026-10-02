package org.acme;

import io.serverlessworkflow.impl.WorkflowInstance;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.ce.OutgoingCloudEventMetadata;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.docling.BoardingPassScanner;
import org.acme.entity.Incidence;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestStreamElementType;
import org.jspecify.annotations.NonNull;

@Path("/ticket")
public class TicketResource {

    @Inject
    BoardingPassScanner boardingPassScanner;

    @Inject
    Logger logger;

    @ConfigProperty(name = "docling.enbaled", defaultValue = "false")
    boolean doclingEnabled;

    public record ScanResponse(Long id, boolean valid){}

    @POST
    @Path("/scan")
    public ScanResponse scan(String boardingPassBase64) {

        logger.infof("Scanning Boarding pass");

        if (doclingEnabled) {
            long id = boardingPassScanner.scanBookingId(boardingPassBase64);
            return new ScanResponse(id, id > 0 ? true : false);
        } else {
            return new ScanResponse(11L, 11 > 0 ? true : false);
        }
    }

    @Inject
    TicketResponseWorkflow ticketResponseWorkflow;

    @POST
    @Path("/storetest")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<TicketResponse> createSupportTicket(@Valid TicketRequest ticketRequest) {

        Map<String, Object> params = createParameters(ticketRequest);

        return ticketResponseWorkflow.startInstance(params)
            .onItem()
            .transform(data -> data.as(TicketResponse.class)
                .orElseThrow());

    }

    private static @NonNull Map<String, Object> createParameters(TicketRequest ticketRequest) {
        Map<String, Object> params =
            Map.of("request", ticketRequest.message(),
                   "user", ticketRequest.user(),
                   "email", ticketRequest.email(),
                   "message", ticketRequest.message(),
                   "bookingId", ticketRequest.bookingId());

        return params;
    }

    @POST
    @Path("/store")
    public Response supportTicketProcessing(@Valid TicketRequest ticketRequest) {

        logger.infof("Starting Support Ticket Workflow with Message: %s", ticketRequest.message());

        Map<String, Object> params = createParameters(ticketRequest);
        WorkflowInstance instance = ticketResponseWorkflow.instance(params);
        // fire and forget (agents will be called on a thread within the engine)
        instance.start();

        return Response
            .accepted(Map.of("instanceId", instance.id()))
            .build();
    }

    private static final JsonFormat CE_JSON = (JsonFormat) EventFormatProvider.getInstance()
        .resolveFormat(JsonFormat.CONTENT_TYPE);

    @Channel("flow-in-outgoing")
    Emitter<String> flowIn;

    @Inject
    ObjectMapper objectMapper;


    @PUT
    @Path("/update")
    public Response sendReview(ReviewedDraft review, @HeaderParam("X-Flow-Instance-Id") String instanceId)
        throws JsonProcessingException {

        logger.infof("Received %s review", review.draft());

        String body = objectMapper.writeValueAsString(review);

        OutgoingCloudEventMetadata<?> ceMeta = OutgoingCloudEventMetadata.builder()
            .withId(UUID.randomUUID().toString())
            .withSource(URI.create("api:/update"))
            .withType("org.acme.email.review.done")
            .withDataContentType("application/json")
            .withExtension("flowinstanceid", instanceId)
            .build();

        flowIn.send(Message.of(body).addMetadata(ceMeta));

        return Response.accepted().build();
    }

    @Channel("flow-out-incoming")
    Multi<byte[]> flowOut;

    @Inject
    ReviewFeedbackOutbridge reviewFeedbackOutbridge;

    @GET
    @Path("/hil")
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<String> stream() {
        return flowOut
            .map(reviewFeedbackOutbridge::onFlowOut);
    }

    @Inject
    Incidence.Repo incidenceRepo;

    // dashboard
    @GET
    @Path("/recent")
    public List<Incidence> findRecentCustomerSupport() {
        LocalDate daysBefore = LocalDate.now()
            .minus(Period.ofDays(60));
        return incidenceRepo.findByDate(daysBefore);
    }

    @Inject
    SecretEchoFlow secretFlow;

    @GET
    @Path("/secret")
    public Response execute() {
        secretFlow.startInstance().await();

        return Response.ok().build();
    }
}
