package org.acme;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.document.transformer.jsoup.HtmlToTextDocumentTransformer;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

public class MainApp {
    public static void main(String[] args) {
        DocumentParser documentParser = new TextDocumentParser();
        Document document = ClassPathDocumentLoader.loadDocument("index.html", documentParser);

        Map<String, String> metadataCssSelectors = new HashMap<>();
        metadataCssSelectors.put("title", "#title");

        HtmlToTextDocumentTransformer transformer = 
                new HtmlToTextDocumentTransformer("#p1", metadataCssSelectors, false);

        
        document = transformer.transform(document);

        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
        List<TextSegment> segments = splitter.split(document);


        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);

        System.out.println(embeddingStore.serializeToJson());

        // dev.langchain4j.store.embedding.EmbeddingStoreIngestor

        embeddingStore.removeAll();
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor
                .builder()
                .documentTransformer(transformer)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(ClassPathDocumentLoader
                            .loadDocument("index.html", documentParser));

        System.out.println(embeddingStore.serializeToJson());

    }
}
