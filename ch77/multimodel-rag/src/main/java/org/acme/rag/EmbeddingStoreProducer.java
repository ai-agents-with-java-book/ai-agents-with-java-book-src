package org.acme.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;


@Singleton
public class EmbeddingStoreProducer {

    @Produces
    @Singleton
    InMemoryEmbeddingStore<TextSegment> createVectorStore() {
        return new InMemoryEmbeddingStore<>();
    }
}
