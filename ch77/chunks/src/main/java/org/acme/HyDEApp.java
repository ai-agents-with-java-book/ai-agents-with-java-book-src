package org.acme;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

public class HyDEApp {

    static String TEXT = """
            Stranger Things is set in the fictional rural town of Hawkins, Indiana, in the 1980s. 
            
            Hawkins is described (in Season 2) as, 'the town where nothing ever happens'. 
            
            The nearby Hawkins National Laboratory ostensibly performs scientific research for the United States Department of Energy, but also secretly experiments with the paranormal and supernatural, sometimes with human test subjects. 
            
            They inadvertently create a portal to an alternate dimension referred to as the Upside Down, whose presence begins to affect the residents of Hawkins in unusual ways.
            """;

    public static void main(String[] args) {
        InMemoryEmbeddingStore<TextSegment> embeddingStore =
                new InMemoryEmbeddingStore<>();

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        DocumentByParagraphSplitter splitter = new DocumentByParagraphSplitter(200, 0);
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(Document.from(TEXT));

        String queryString = "What institution is located near Hawkins, Indiana?";

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .modelName(GPT_4_O_MINI)
                .build();

        SystemMessage systemMessage = SystemMessage.from("""
            Answer user's questions with a clear and explicit subject, verb, and object"
        """);

        ChatResponse response = chatModel.chat(systemMessage, UserMessage.from(queryString));

        String hyptheticAnswer = response.aiMessage().text();

        System.out.println("LLM response " + hyptheticAnswer);

        EmbeddingSearchResult<TextSegment> searchHypothetical = embeddingStore.search(EmbeddingSearchRequest.builder()
                .minScore(0.5)
                .maxResults(3)
                .queryEmbedding(embeddingModel.embed(hyptheticAnswer).content())
                .build());

        searchHypothetical.matches().forEach(match -> {
            System.out.println("Similarity: " + match.score() + " " + match.embedded().text());
        });

    }

}
