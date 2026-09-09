package org.acme.repair;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import static org.acme.repair.Repair.*;

public final class Propose {
    public static void main(String[] args) throws Exception {
        Path fixture = Path.of("fixture");
        String source = Files.readString(fixture.resolve(MavenVerifier.SOURCE));
        var verifier = new MavenVerifier(fixture, System.getProperty("repair.maven", "mvn"));
        Observation observation = verifier.verify(source, Duration.ofMinutes(1));
        if (observation.check() != Check.REJECTED) {
            throw new IllegalStateException("Expected the fixture to fail acceptance checks");
        }
        Proposal proposal = ModelPlanner.openAi().propose(
                new Snapshot(Demo.GOAL, source, observation, List.of()));
        if (proposal == null || !proposal.valid()) {
            throw new IllegalStateException("Invalid model proposal");
        }
        System.out.println(proposal.action() + ": " + proposal.reason());
        System.out.println(proposal.plan());
        if (proposal.action() == Action.REPLACE_SOURCE) {
            Path destination = Files.createTempFile("calculator-proposal-", ".java");
            Files.writeString(destination, proposal.replacementSource());
            System.out.println("Proposed source, not executed: " + destination);
        }
    }
}
