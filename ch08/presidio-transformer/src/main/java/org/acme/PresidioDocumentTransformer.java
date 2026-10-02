package org.acme;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentTransformer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PresidioDocumentTransformer implements DocumentTransformer {

    @Inject
    PresidioService presidioService;

    @Override
    public Document transform(Document document) {
        String content = document.text();
        
        var anonymizedContent = presidioService.process(content);
        return Document.from(anonymizedContent, document.metadata());
    }


}
