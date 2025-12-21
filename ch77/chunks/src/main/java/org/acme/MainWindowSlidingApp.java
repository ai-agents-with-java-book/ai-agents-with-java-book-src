package org.acme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.segment.TextSegmentTransformer;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.DefaultContent;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

public class MainWindowSlidingApp {

    public static class WindowSlidingTextSegmentTransformer implements TextSegmentTransformer {

        @Override
        public TextSegment transform(TextSegment segment) {
            return transformAll(Collections.singletonList(segment))
                    .getFirst();
        }

        @Override
        public List<TextSegment> transformAll(List<TextSegment> segments) {
            return IntStream.range(0, segments.size())
                .mapToObj(i -> {
                    TextSegment current = segments.get(i);

                    // Build context: previous + current + next
                    String context = Stream.of(
                        i > 0 ? segments.get(i - 1).text() : null,
                        current.text(),
                        i < segments.size() - 1 ? 
                            segments.get(i + 1).text() : null
                    )
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(" "));

                    // Copy metadata and add context info
                    Metadata metadata = new Metadata(current.metadata().toMap());
                    metadata.put("window-content-retriever", context);

                    // Create new TextSegment with enriched metadata
                    return TextSegment.from(current.text(), metadata);
                })
                .collect(Collectors.toList());
        }  

    }

    public static class WindowContentInjector extends DefaultContentInjector {

        @Override
        public ChatMessage inject(List<Content> contents, ChatMessage chatMessage) {
            List<Content> fullContent = contents.stream() // <1>
            .map(content -> {
                    String newContent = content
                    .textSegment()
                    .metadata()
                    .getString("window-content-retriever"); // <2>
                    
                    return new DefaultContent(TextSegment.from(newContent),
                                                content.metadata()); // <3>
            })
            .collect(Collectors.toList());
                return super.inject(fullContent, chatMessage); // <4>
        }
    }

    public static void main(String[] args) {

        String TEXT = """
                Freddie Mercury was a British singer and songwriter who achieved global fame as the lead vocalist and pianist of the rock band Queen.
                Regarded as one of the greatest singers in the history of rock music, he was known for his flamboyant stage persona and four-octave vocal range. 
                Mercury defied the conventions of a rock frontman with his theatrical style, influencing the artistic direction of Queen.
                """;

        InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        EmbeddingStoreIngestor embeddingStoreIngestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(200, 20))
                .textSegmentTransformer(new WindowSlidingTextSegmentTransformer())
                .embeddingModel(embeddingModel)
                .embeddingStore(store)
                .build();
        embeddingStoreIngestor.ingest(Document.from(TEXT));

        System.out.println(store.serializeToJson());

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                                                    .embeddingStore(store)
                                                    .embeddingModel(embeddingModel)
                                                    .maxResults(5)
                                                    .minScore(0.75)
                                                    .build();
        DefaultRetrievalAugmentor defaultRetrievalAugmentor = DefaultRetrievalAugmentor.builder()
            .contentRetriever(contentRetriever)
            .contentInjector(new WindowContentInjector()) // <5>
            .build();

    }

}
