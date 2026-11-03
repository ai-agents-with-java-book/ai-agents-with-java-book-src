package ch15;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.service.decision.Decide;

public interface DamageInspector {

    @Decide("Is the item in the photo visibly damaged?")
    boolean isDamaged(ImageContent photo, String customerComment);
}
