package org.acme.support;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("/summaries")
@Consumes(MediaType.TEXT_PLAIN)
@Produces(MediaType.TEXT_PLAIN)
public class SummaryResource {
    private final SupportDrafts drafts;

    @Inject
    public SummaryResource(SupportSummary summary) {
        drafts = new SupportDrafts(summary);
    }

    @POST
    public Response summarize(String request) {
        try {
            return Response.ok(drafts.prepareSummary(request)).build();
        } catch (IllegalArgumentException invalid) {
            return Response.status(400).entity(invalid.getMessage()).build();
        } catch (SummaryUnavailableException unavailable) {
            return Response.status(502).entity(unavailable.getMessage()).build();
        }
    }
}
