package org.acme.ai;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService(modelName = "vision", retrievalAugmentor = RegisterAiService.NoRetrievalAugmentorSupplier.class)
public interface ImageDescriptor {

    @UserMessage("""
            Describe only the factual content visible in the image.
            The image has the following description: {{description}}.
            
            1. If decorative/non-informational: output '<---image--->'
            
            2. For content images:
               - General Images: List visible objects, text, and measurable attributes
               - Charts/Infographics: State all numerical values and labels present in different sentences.
                                        
            Rules:
               * Include only directly observable information
               * Use original numbers and text without modification
               * Avoid any interpretation or analysis
               * Preserve all labels and measurements exactly as shown
            """)
    String describe(String description, Image image);

}
