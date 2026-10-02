package org.acme;



import io.quarkiverse.flow.Flow;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowContextData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.ai.TicketResponseExperts;
import org.acme.entity.Incidence;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.time.LocalDate;

import static io.quarkiverse.flow.dsl.FlowDSL.agent;
import static io.quarkiverse.flow.dsl.FlowDSL.consume;
import static io.quarkiverse.flow.dsl.FlowDSL.consumed;
import static io.quarkiverse.flow.dsl.FlowDSL.emitJson;
import static io.quarkiverse.flow.dsl.FlowDSL.listen;
import static io.quarkiverse.flow.dsl.FlowDSL.switchWhenOrElse;
import static io.quarkiverse.flow.dsl.FlowDSL.toOne;
import static io.quarkiverse.flow.dsl.FlowDSL.withContext;
import static io.quarkiverse.flow.dsl.FlowWorkflowBuilder.workflow;

@ApplicationScoped
public class TicketResponseWorkflow extends Flow {

    @Inject
    TicketResponseExperts.AutoDraftFeedbackAgent autoDraftFeedbackAgent;

    @Inject
    MailService mailService;

    @Inject
    Incidence.Repo incidenceRepo;

    @Override
    public Workflow descriptor() {
        return workflow("support-ticket")
            .tasks(
                // Generates a draft
                agent("draftAgent", autoDraftFeedbackAgent::generateFeedback, String.class),
                switchWhenOrElse(tr -> Sentiment.POSITIVE == tr.sentiment().result(), "automatic", "manual", TicketResponse.class),

                // If it is positive send the email and move to store the activity
                withContext("automatic", (TicketResponse tr, WorkflowContextData ctx) -> {
                    CustomerSupportInformation customerSupportInformation = getAutomaticCustomerSupportInformation(ctx, "Thank you very much for your feedback", tr.draft());
                    mailService.sendEmail(customerSupportInformation);
                    return customerSupportInformation;
                }, TicketResponse.class).then("store"),
                /**withContext("manual", (TicketResponse tr, WorkflowContextData ctx) -> {
                    CustomerSupportInformation customerSupportInformation = getAutomaticCustomerSupportInformation(ctx, "Thank you very much for your feedback, our agent will take care of your case", tr.draft());
                    mailService.sendEmail(customerSupportInformation);
                    return customerSupportInformation;
                }, TicketResponse.class).then("store"),**/

                // If neutral or negative send the event to Kafka Topic so it is sent to UI to review
                emitJson("manual", "org.acme.email.review.required", SupportData.class)
                    .inputFrom(((TicketResponse ticketResponse, WorkflowContextData ctx) ->
                        new SupportData(ctx.instanceData().id(), ctx.instanceData().input()
                            .as(TicketRequest.class).get(),  ticketResponse))
                        , TicketResponse.class),

                // Wait till a New event is produced when the human review the text and it is produced to the topic
                listen("waitHumanReview",
                    toOne(consumed("org.acme.email.review.done").extensionByInstanceId("flowinstanceid"))),
                withContext("sendEmail", (ReviewedDraft rd, WorkflowContextData ctx) -> {
                    CustomerSupportInformation customerSupportInformation = getAutomaticCustomerSupportInformation(ctx, "We are sorry to hear from you", rd.draft());
                    mailService.sendEmail(customerSupportInformation);
                    return customerSupportInformation;
                }, ReviewedDraft.class).then("store"),

                // Stores event to DB
                consume("store", this::saveCustomerSupport,
                    CustomerSupportInformation.class)
            )
            .build();
    }

    @Transactional
    public void saveCustomerSupport(CustomerSupportInformation customerSupportInformation) {
        Incidence incidence = new Incidence();
        incidence.bookingId = customerSupportInformation.bookingId();
        incidence.userEmail = customerSupportInformation.userEmail();
        incidence.message = customerSupportInformation.request();
        incidence.response = customerSupportInformation.response();
        incidence.createdAt = LocalDate.now();

        incidenceRepo.insert(incidence);
    }

    private static @NonNull CustomerSupportInformation getAutomaticCustomerSupportInformation(WorkflowContextData ctx, String subject, String tr) {
        TicketRequest ticketRe = ctx
            .instanceData().input()
            .as(TicketRequest.class)
            .orElseThrow();

        CustomerSupportInformation customerSupportInformation =
            new CustomerSupportInformation(ticketRe.user(),
                ticketRe.email(),
                subject,
                ticketRe.message(),
                tr,
                ticketRe.bookingId());
        return customerSupportInformation;
    }
}
