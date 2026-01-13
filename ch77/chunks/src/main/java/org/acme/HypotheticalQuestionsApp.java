package org.acme;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.DefaultContent;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HypotheticalQuestionsApp {
    
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

        SystemMessage system = SystemMessage.from("""
            Suggest 10 clear questions whose answer could be given by the user provided text.
            Don't use pronouns, be explicit about the subjects and objects of the question.
            
            Don't prefix the questions with a numeration.
        """);

        DocumentByParagraphSplitter splitter = new DocumentByParagraphSplitter(1000, 100);
        List<TextSegment> paragraphs = splitter.split(Document.from(TEXT));

        for (TextSegment paragraphSegment : paragraphs) {

            String paragraphText = paragraphSegment.text();
            ChatResponse response = chatModel.chat(system, UserMessage.from(paragraphText));
            String[] questions = response.aiMessage().text().split("\\n");

            for(String question : questions) {

                System.out.println(question);

                TextSegment segment =
                        TextSegment.from(question, new Metadata().put("paragraph", paragraphText));

                Response<Embedding> embeddingResponse = embeddingModel.embed(segment);
                embeddingStore.add(embeddingResponse.content(), segment);
            }
        }

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.80)
                .build();
        DefaultRetrievalAugmentor defaultRetrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentInjector(new MetadataContentInjector("paragraph"))
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(defaultRetrievalAugmentor)
                .build();

        String response = assistant.chat("In which decade does the events of Stranger Things take place?");
        System.out.println(response);


    }

    public static class MetadataContentInjector extends DefaultContentInjector {

        private String metadataKey;

        public MetadataContentInjector(String metadataKey) {
            this.metadataKey = metadataKey;
        }

        @Override
        public ChatMessage inject(List<Content> contents, ChatMessage chatMessage) {
            System.out.println("Injecting the paragraph");
            List<Content> fullContent = contents.stream()
                    .map(content -> {
                        String newContent = content
                                .textSegment()
                                .metadata()
                                .getString(metadataKey);

                        return new DefaultContent(TextSegment.from(newContent),
                                content.metadata());
                    })
                    .collect(Collectors.toList());
            return super.inject(fullContent, chatMessage);
        }
    }

    public interface Assistant {
        String chat(String message);
    }
    
}
