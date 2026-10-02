package org.acme;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;

import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import tools.jackson.databind.JsonNode;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


public class MainApp {

    interface Assistant {
        String chat(@UserMessage String userMessage, InvocationParameters parameters);
    }

    public static void main(String[] args) {
        
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .modelName(GPT_4_O_MINI)
                .logRequests(true)
                .build();



        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        DocumentParser documentParser = new TextDocumentParser();
        Document document = ClassPathDocumentLoader.loadDocument("customer.txt", documentParser);


        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(50, 0))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(document);

        printEmbeddingStore(embeddingStore);

        ContentRetriever contentRetrieverMeta = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build();
        
        ContentInjector contentInjector = DefaultContentInjector.builder()
                        .metadataKeysToInclude(List.of("file_name"))
                        .build();

        RetrievalAugmentor retrievalAugmentorMeta = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetrieverMeta)
                .contentInjector(contentInjector)
                .build();
        
        Assistant assistantMeta = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentorMeta)
                .build();
        
        InvocationParameters parametersMeta = InvocationParameters.from(Map.of("tenant_id", "123"));
        assistantMeta.chat("What is the question about the dual-monitor? ", parametersMeta);


        embeddingStore.removeAll();

        ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(50, 0))
                .documentTransformer(doc -> {
                        doc.metadata()
                            .put("tags", "customer")
                            .put("tenant_id", "123");
                        return doc;
                })
                .textSegmentTransformer(seg -> {
                    seg.metadata().put("timestamp", LocalDateTime.now().toString());
                    return seg;
                })
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(document);

        printEmbeddingStore(embeddingStore);

        Filter t1Filter = MetadataFilterBuilder.metadataKey("tenant_id").isEqualTo("123");

        ContentRetriever contentRetrieverT1 = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .filter(t1Filter) // by specifying the static filter, we limit the search to segments only about tenant1
                .build();

        Filter t2Filter = MetadataFilterBuilder.metadataKey("tenant_id").isEqualTo("456");

        ContentRetriever contentRetrieverT2 = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .filter(t2Filter) // by specifying the static filter, we limit the search to segments only about tenant1
                .build();

        TenentQueryRouter tenentQueryRouter = new TenentQueryRouter(
            Map.of("123", contentRetrieverT1, "456", contentRetrieverT2)
        );

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(tenentQueryRouter)
                .build();
        
        Assistant assistant = AiServices.builder(Assistant.class)
                    .chatModel(chatModel)
                    .retrievalAugmentor(retrievalAugmentor)
                    .build();
        
        InvocationParameters parameters = InvocationParameters.from(Map.of("tenant_id", "123"));
        String answer = assistant.chat("What is the question about the dual-monitor? ", parameters);
        System.out.println(answer);

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder() // <1>
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .dynamicFilter(query -> { // <2>
                    String tenantId = query.metadata().invocationParameters().get("tenant_id"); // <3>
                    return MetadataFilterBuilder.metadataKey("tenant_id").isEqualTo(tenantId); // <4>
                })
                .build();
    }

    private static void printEmbeddingStore(InMemoryEmbeddingStore<TextSegment> inMemoryEmbeddingStore) {
        System.out.println("***********************************************");
        JsonMapper mapper = JsonMapper.builder().build();
        JsonNode root = mapper.readTree(inMemoryEmbeddingStore.serializeToJson());

        if (root.has("entries") && root.get("entries").isArray()) {
            for (JsonNode entry : root.get("entries")) {
                JsonNode embeddingNode = entry.get("embedding");
                if (embeddingNode != null && embeddingNode.isObject()) {
                    // Remove "vector" field if present
                    ((ObjectNode) embeddingNode).remove("vector");
                }
            }
        }

        String cleanedJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        System.out.println(cleanedJson);
        System.out.println("***********************************************");
    }

}
