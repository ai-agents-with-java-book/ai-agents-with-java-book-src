package org.acme;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentTransformer;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.nio.file.Paths;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

public class CanonicalDocumentTransformerApp {


    public static void main(String[] args) {

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .modelName(GPT_4_O_MINI)
                .build();


        SystemMessage system = SystemMessage.from("""
            You are preparing product content for semantic search.
                
            Remove:
            - ingredients
            - nutrition facts
            - legal disclaimers
                
            Keep:
            - product purpose
            - usage
            - category
            - dietary characteristics
                
            Return clean text.
        """);

        Document document = FileSystemDocumentLoader
                .loadDocument(Paths.get("src/main/resources", "kinder.txt"),
                        new TextDocumentParser());

        DocumentTransformer transformer = (Document doc) -> {
            ChatResponse response = chatModel.chat(system, UserMessage.from(doc.text()));
            return Document.document(response.aiMessage().text(), doc.metadata());
        };

        Document transformed = transformer.transform(document);
        System.out.println(transformed.text());

    }

}
