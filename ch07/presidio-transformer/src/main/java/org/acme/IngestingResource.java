package org.acme;

import java.util.Map;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByLineSplitter;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import io.quarkiverse.presidio.runtime.PresidioPipeline;
import io.quarkiverse.presidio.runtime.model.Mask;
import io.quarkiverse.presidio.runtime.model.Redact;
import io.quarkiverse.presidio.runtime.model.Replace;
import io.quarkiverse.presidio.runtime.model.SupportedEntities;
import io.quarkus.runtime.Startup;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;


@Path("/hello")
public class IngestingResource {

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    PresidioDocumentTransformer presidioDocumentTransformer;

    @Startup
    public void ingest() {
        Document document = ClassPathDocumentLoader
            .loadDocument("email.txt", new TextDocumentParser());
        EmbeddingStoreIngestor embeddingStoreIngestor = EmbeddingStoreIngestor
                                                            .builder()
                                                            .documentTransformer(presidioDocumentTransformer)
                                                            .documentSplitter(new DocumentByLineSplitter(180, 0))
                                                            .embeddingModel(embeddingModel)
                                                            .embeddingStore(embeddingStore)
                                                            .build();
        embeddingStoreIngestor.ingest(document);

    }


    static Replace REPLACE = new Replace("ANONYMIZED");
    static Mask MASK = new Mask("*", 4, true);
    static Redact REDACT = new Redact();
    
    @GET
    public String process() {
        PresidioPipeline presidioPipeline = PresidioPipeline.builder()
                        .withSupportedEntities(SupportedEntities.PHONE_NUMBER, 
                            SupportedEntities.EMAIL_ADDRESS, 
                            SupportedEntities.PERSON)
                        .withSupportedAnonymizers(
                            Map.of(
                                SupportedEntities.DEFAULT, REPLACE,
                                SupportedEntities.PHONE_NUMBER, MASK,
                                SupportedEntities.EMAIL_ADDRESS, REDACT

                            ))
                        .build();
        return presidioPipeline.process("""
                            Hi Lumina Support Team,

                My name is Sarah Mitchell, and I interested in purchasing the Lumina Smart Desk. 

                Before placing my order, I wanted to confirm whether the desk is compatible with dual-monitor setups and if the height adjustment memory feature supports multiple user profiles.

                Could you please provide more details about these features?

                You can reach me at (415) 829-3476 or by email at sarah.mitchell@example.com


                Thank you for your help! Best regards, Sarah Mitchell""", "en");
    }

}
