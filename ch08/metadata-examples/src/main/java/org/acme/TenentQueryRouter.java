package org.acme;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.router.QueryRouter;

public class TenentQueryRouter implements QueryRouter {

    Map<String, ContentRetriever> tenantRetrievers;

    public TenentQueryRouter(Map<String, ContentRetriever> tenantRetrievers) {
        this.tenantRetrievers = tenantRetrievers;
    }

    @Override
    public Collection<ContentRetriever> route(Query query) {
        
        String tenantId = query
            .metadata()
            .invocationParameters().get("tenant_id");
        
        System.out.println("Tenant ID: " + tenantId);

        return List.of(tenantRetrievers.get(tenantId));
        
    }
    
}
