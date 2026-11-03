package ch15.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.V;
import dev.langchain4j.service.decision.Decide;
import dev.langchain4j.service.decision.Scale;

public interface WriteupScorer {

    enum Sign { ARIES, TAURUS, GEMINI, CANCER, LEO, VIRGO, LIBRA, SCORPIO, SAGITTARIUS, CAPRICORN, AQUARIUS, PISCES }

    enum Alignment {
        @Description("The writeup ignores the spirit of the sign") POOR,
        @Description("The writeup reflects the spirit of the sign only in places") FAIR,
        @Description("The writeup mostly reflects the spirit of the sign") GOOD,
        @Description("The writeup is a perfect portrait of the spirit of the sign")
        EXCELLENT
    }

    @Decide("How well does the writeup align with the spirit of the zodiac sign?")
    @Agent(value = "Scores a writeup against the spirit of a zodiac sign",
            outputKey = "score")
    Scale<Alignment> scoreWriteup(@V("writeup") String writeup, @V("sign") Sign sign);
}
