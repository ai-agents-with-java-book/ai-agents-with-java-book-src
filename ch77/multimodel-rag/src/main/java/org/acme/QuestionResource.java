package org.acme;

import io.quarkus.runtime.Startup;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.acme.ai.DocumentationAssistant;
import org.acme.rag.Ingestor;

@Path("/question")
public class QuestionResource {

    @Inject
    Ingestor ingestor;

    @Startup
    public void ingest() {
        ingestor.ingest();
    }

    @Inject
    DocumentationAssistant documentationAssistant;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return documentationAssistant.ask("How many parents named her baby Ada?");
    }
}
