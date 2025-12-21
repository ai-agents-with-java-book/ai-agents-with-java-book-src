package org.acme.ai;

import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService(retrievalAugmentor = RegisterAiService.NoRetrievalAugmentorSupplier.class)
public interface TableDescriptor {

    @UserMessage("""
            Summarize the following tabular data chunk in 1 sentence.
            The table is about {{description}}.
            
            Focus on: {{headers}}.
            
            Data:
            {{rows}}
            """)
    String summarizeTable(String description, String headers, String rows);

}
