package ch15;

import dev.langchain4j.model.output.structured.Description;

public enum Quality {
    @Description("The reply does not address the customer's problem") POOR,
    @Description("The reply addresses the problem only partially, or is unclear") FAIR,
    @Description("The reply addresses the problem with clear instructions") GOOD,
    @Description("The reply solves the problem and is clear, complete and courteous")
    EXCELLENT
}
