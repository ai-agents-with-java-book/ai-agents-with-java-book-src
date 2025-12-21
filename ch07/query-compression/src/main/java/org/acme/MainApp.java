package org.acme;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;

import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;

import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;


import java.util.Collection;



import dev.langchain4j.data.segment.TextSegment;

public class MainApp {
    public static void main(String[] args) {

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .modelName(GPT_4_O_MINI)
                .build();

        

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build();
        
        DefaultQueryRouter defaultQueryRouter = new DefaultQueryRouter(contentRetriever);
        

        CompressingQuery compressingQuery = new CompressingQuery(chatModel);
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(defaultQueryRouter)
                .queryTransformer(compressingQuery)
                .build();

        Query query = Query.from(""" 
                Hello Lisa, I have a question for you that I'd love you find me an answer; 
                basically I'd like to know if I can cancel my booking with id 123.
                Thank you very much for your help, and hope you can answer my question."
            """);
        
        Collection<Query> transform = compressingQuery.transform(query);
        transform
            .stream()
            .map(Query::text)
            .forEach(System.out::println);
    }
}
