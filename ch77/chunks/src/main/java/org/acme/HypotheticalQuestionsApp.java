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
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

import java.util.ArrayList;
import java.util.List;

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
        """);

        List<String> allQuestionParagraphs = new ArrayList<>();

        DocumentByParagraphSplitter splitter = new DocumentByParagraphSplitter(1000, 100);
        List<TextSegment> paragraphs = splitter.split(Document.from(TEXT));

        for (TextSegment paragraphSegment : paragraphs) {

            ChatResponse response = chatModel.chat(system, UserMessage.from(paragraphSegment.text()));
            System.out.println(response.aiMessage().text());
        }


    }

    
}
