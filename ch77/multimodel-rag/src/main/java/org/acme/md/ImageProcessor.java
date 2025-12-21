package org.acme.md;

import dev.langchain4j.data.image.Image;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.ai.ImageDescriptor;

@ApplicationScoped
public class ImageProcessor {

    @Inject
    ImageDescriptor imageDescriptor;

    public String describeEmbeddedImage(String description, String markdownImage) {
        ParsedImage image = parse(markdownImage);

        Image aiImage = Image.builder()
                .mimeType(image.mimeType())
                .base64Data(image.content())
                .build();

        return imageDescriptor.describe(description, aiImage);
    }


    public static ParsedImage parse(String markdownImage) {
        int positionMimeSeparator = markdownImage.indexOf(';');
        String mimeType = markdownImage.substring(5, positionMimeSeparator);
        String image = markdownImage.substring(positionMimeSeparator + 8);

        return new ParsedImage(mimeType, image);
    }

    public record ParsedImage(String mimeType, String content){}

}
