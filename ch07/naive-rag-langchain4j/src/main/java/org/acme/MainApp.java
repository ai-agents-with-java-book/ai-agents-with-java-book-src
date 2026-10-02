package org.acme;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;


import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;

import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;


import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

public class MainApp {

    

   
    public static interface Assistant {
        String answer(String query);
    }

    public static void main(String[] args) {
        Assistant assistant = createAssistant("rag.txt");
        String answer = assistant.answer("What is the total revenue of NovaTech");
        System.out.println(answer);

    }

    private static Assistant createAssistant(String documentPath) {


        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .modelName(GPT_4_O_MINI)
                .build();



        DocumentParser documentParser = new TextDocumentParser();
        Document document = ClassPathDocumentLoader.loadDocument(documentPath, documentParser);


        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
        List<TextSegment> segments = splitter.split(document);


        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);

        // Finishes the Ingestion phase

        // Query Vector  store

        Response<Embedding> qEmbed = embeddingModel.embed("What is the total revenue of NovaTech");
    
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                                                            .maxResults(3)
                                                            .minScore(0.8)
                                                            .queryEmbedding(qEmbed.content())
                                                            .build();
        EmbeddingSearchResult<TextSegment> search = embeddingStore.search(embeddingSearchRequest);

        search.matches().stream()
                        .forEach(e -> {
                            System.out.println("-------------------");
                            System.out.println("Score: " + e.score());
                            System.out.println("Text: " + e.embedded().text());
                        });

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3) // on each interaction we will retrieve the 2 most relevant segments
                .minScore(0.8) // we want to retrieve segments at least somewhat similar to user query
                .build();


        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .build();
    }

}
