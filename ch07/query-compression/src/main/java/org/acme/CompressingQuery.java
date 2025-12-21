package org.acme;

import java.util.Collection;
import java.util.Map;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import static java.util.Collections.singletonList;

public class CompressingQuery implements QueryTransformer {

    private ChatModel chatModel;

    public static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from(
            """
                    Analyze the new query from the User. 
                    Identify all relevant details, terms, and context from the new query. 
                    Reformulate this query into a clear, concise, and self-contained 
                        format suitable for information retrieval.
                    
                    User query: {{query}}
                    
                    It is very important that you provide only reformulated query and nothing else! \
                    Do not prepend a query with anything!"""
            );

    public CompressingQuery(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public Collection<Query> transform(Query query) {
        Prompt prompt = DEFAULT_PROMPT_TEMPLATE.apply(Map.of("query", query.text()));
        String compressedQueryText = chatModel.chat(prompt.text());
        Query compressedQuery = query.metadata() == null
                ? Query.from(compressedQueryText)
                : Query.from(compressedQueryText, query.metadata());
        
        return singletonList(compressedQuery);
    }
    
}
