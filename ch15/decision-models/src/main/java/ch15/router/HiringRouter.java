package ch15.router;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

public interface HiringRouter {

    @Agent("Decides whether to invite or reject a candidate")
    String decide(@V("candidateCv") String candidateCv,
                  @V("jobDescription") String jobDescription,
                  @V("phoneInterviewNotes") String phoneInterviewNotes);
}
