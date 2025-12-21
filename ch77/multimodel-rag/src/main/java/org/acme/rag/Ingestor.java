package org.acme.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Singleton
public class Ingestor {

    @Inject
    DoclingDocumentParser doclingDocumentParser;

    @Inject
    MultiModalDocumentSplitter multiModalDocumentSplitter;

    @Inject
    AllMiniLmL6V2EmbeddingModel embeddingModel;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    public void ingest() {

        /**Document document = FileSystemDocumentLoader
                .loadDocument("src/test/resources/borns.pdf", doclingDocumentParser);**/

        Document document = Document
                .document(readFile(), Metadata.from("render", "docling"));

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor
                .builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .documentSplitter(multiModalDocumentSplitter)
                .build();

        ingestor.ingest(document);

    }

    private String readFile() {
        try {
            return Files.readString(Paths.get("src/test/resources/file.md"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
