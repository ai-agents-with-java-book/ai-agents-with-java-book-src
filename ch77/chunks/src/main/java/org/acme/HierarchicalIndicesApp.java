package org.acme;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;

public class HierarchicalIndicesApp {
    
static String TEXT = """
            Freddie Mercury was a British singer and songwriter who achieved global fame as the lead vocalist and pianist of the rock band Queen. 
            Regarded as one of the greatest singers in the history of rock music, he was known for his flamboyant stage persona and four-octave vocal range
            Mercury defied the conventions of a rock frontman with his theatrical style, influencing the artistic direction of Queen.
            Born in 1946 in Zanzibar to Parsi-Indian parents, Mercury attended British boarding schools in India from the age of eight and returned to Zanzibar after secondary school. 
            In 1964, his family fled the Zanzibar Revolution, moving to Middlesex, England. Having previously studied and written music, he formed Queen in 1970 with guitarist Brian May and drummer Roger Taylor. 
            Mercury wrote numerous hits for Queen, including "Killer Queen", "Bohemian Rhapsody", "Somebody to Love", "We Are the Champions", "Don't Stop Me Now" and "Crazy Little Thing Called Love".
            His charismatic stage performances often saw him interact with the audience, as displayed at the 1985 Live Aid concert.
            He also led a solo career and was a producer and guest musician for other artists.
            Mercury was diagnosed with AIDS in 1987. He continued to record with Queen, and was posthumously featured on their final album, Made in Heaven (1995). 
            In 1991, the day after publicly announcing his diagnosis, he died from complications of the disease at the age of 45. 
            In 1992, a concert in tribute to him was held at Wembley Stadium, in benefit of AIDS awareness.
            As a member of Queen, Mercury was posthumously inducted into the Rock and Roll Hall of Fame in 2001, the Songwriters Hall of Fame in 2003, and the UK Music Hall of Fame in 2004. 
            In 1990, he and the other Queen members received the Brit Award for Outstanding Contribution to British Music. 
            One year after his death, Mercury received the same award individually. 
            In 2005, Queen were awarded an Ivor Novello Award for Outstanding Song Collection from the British Academy of Songwriters, Composers, and Authors. 
            In 2002, Mercury was voted number 58 in the BBC's poll of the 100 Greatest Britons.
            """;

    public static void main(String[] args) {

        EmbeddingStore<TextSegment> summaryStore = new InMemoryEmbeddingStore<>();
        EmbeddingStore<TextSegment> detailedStore = new InMemoryEmbeddingStore<>();
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        Document document = Document.from(TEXT, 
                                            Metadata.from( 
                                                Map.of("source", "wikipedia", 
                                                    "topic", "Freddie_Mercury"
                                                )
                                            )
                                        );
        
        TextSegment summary = summarize(document);
        summaryStore.add(embeddingModel.embed(summary).content(), summary);

        DocumentSplitter splitter = DocumentSplitters
                                        .recursive(300, 20);
        List<TextSegment> chunks = splitter.split(document);

        List<Embedding> embeddings = embeddingModel.embedAll(chunks).content();
        detailedStore.addAll(embeddings, chunks);


        Response<Embedding> qEmbed = embeddingModel.embed("Where did Freddie Mercury born?");

        EmbeddingSearchRequest embeddingSummarySearchRequest = EmbeddingSearchRequest.builder()
                                                            .maxResults(1)
                                                            .minScore(0.8)
                                                            .queryEmbedding(qEmbed.content())
                                                            .build();

        EmbeddingSearchResult<TextSegment> search 
                = summaryStore.search(embeddingSummarySearchRequest);

        String metadataTopic = search.matches()
                .get(0)
                .embedded()
                .metadata()
                .getString("topic");
        
        Filter metadataFilter = MetadataFilterBuilder
            .metadataKey("topic").isEqualTo(metadataTopic);
        EmbeddingSearchRequest embeddingDetailedSearchRequest = EmbeddingSearchRequest
                                                            .builder()
                                                            .maxResults(3)
                                                            .minScore(0.8)
                                                            .filter(metadataFilter)
                                                            .queryEmbedding(qEmbed.content())
                                                            .build();

        EmbeddingSearchResult<TextSegment> detailedChunks = detailedStore
                                                                .search(embeddingDetailedSearchRequest);
        detailedChunks.matches()
                      .stream()
                      .forEach(ts -> System.out.println("* Score: %s Content: %s"
                                                            .formatted(ts.score(), ts.embedded().text())));

    }

    private static TextSegment summarize(Document text) {
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .modelName(GPT_4_O_MINI)
                .build();


        DocumentSplitter splitter = 
                DocumentSplitters.recursive(300, 20);

        List<String> summaries = mapSummaries(text, chatModel, splitter);
        System.out.println("***** Map ******");
        summaries.stream().forEach(System.out::println);
        System.out.println("**********************");
        System.out.println("***** Reduce ******");
        String reduce = reduceSummaries(summaries, chatModel);
        System.out.println(reduce);
        System.out.println("*******************");
        return TextSegment.from(reduce, text.metadata());
    }

    private static String reduceSummaries(List<String> summaries, ChatModel model) {
        String combined = String.join("\n\n", summaries);

        PromptTemplate reducePrompt = PromptTemplate.from("""
            The following are partial summaries of a larger document:

            {{it}}

            Combine them into a concise, 
            coherent overall summary (1 small sentences max). 
            
            Eliminate redundancy and preserve key details.
        """);

        Prompt prompt = reducePrompt.apply(combined);
        return model.chat(prompt.text());
    }

    private static  List<String> mapSummaries(Document inputText, ChatModel model, DocumentSplitter splitter) {
        
        List<TextSegment> segments = splitter.split(inputText);
        List<String> summaries = new ArrayList<>();

        PromptTemplate mapTemplate = PromptTemplate.from("""
                Summarize the following text with the fewer words possible. 
                Focus on key ideas and avoid redundancy.
                You can use bullet points.

                Text:
                {{it}}
            """);
        
        for (TextSegment segment : segments) {
            Prompt prompt = mapTemplate.apply(segment.text());

            String summary = model.chat(prompt.text());
            summaries.add(summary);
        }

        return summaries;
    }
}
