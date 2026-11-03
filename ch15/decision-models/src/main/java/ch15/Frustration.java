package ch15;

import dev.langchain4j.model.output.structured.Description;

public enum Frustration {
    @Description("A neutral or polite request") CALM,
    @Description("The customer complains, or has already asked more than once") ANNOYED,
    @Description("The customer threatens to leave, or uses hostile language") ANGRY
}
