package org.acme;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

import java.util.Map;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByLineSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

public class ContextualRetrievalApp {
 
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

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .modelName(GPT_4_O_MINI)
                .build();


        PromptTemplate promptTemplate = PromptTemplate.from("""
            <document>
            {{wholeDocument}}
            </document>
            Here is the chunk we want to situate within the whole document
            <chunk>
            {{chunk}}
            </chunk>
            Please give a short succinct context to situate this chunk within the overall document
            for the purposes of improving search retrieval of the chunk.
            Answer only with the succinct context and nothing else be short.
            """);

            DocumentByLineSplitter lineSplitter = new DocumentByLineSplitter(200, 0);

            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
            .documentSplitter(lineSplitter)
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .textSegmentTransformer(segment -> {
                ChatResponse generatedChunk = chatModel.chat(promptTemplate.apply(Map.of(
                        "chunk", segment.text(),
                        "wholeDocument", TEXT))
                    .toUserMessage());

                System.out.println("\n" + "-".repeat(100));
                System.out.println(segment.text());
                System.out.println(generatedChunk.aiMessage().text());

                return TextSegment
                        .from(generatedChunk.aiMessage().text() 
                                + " " + segment.text());
            })
            .build();
        

        ingestor.ingest(Document.from(TEXT));

    }


}
