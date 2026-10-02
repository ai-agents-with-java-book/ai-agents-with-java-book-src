package org.acme.security;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class ActionPolicyExample {
    public static final String POLICY_VERSION = "replacement-policy-1";

    public enum Operation { DRAFT_REPLY, REQUEST_REPLACEMENT, EXPORT_CASES }
    public enum Decision { ALLOW, REQUIRE_APPROVAL, DENY }

    // Created by trusted request-handling code, never deserialized from a tool call.
    public record Context(String actorId, String tenantId,
                          Set<String> caseIds, Set<Operation> operations) {
        public Context {
            Objects.requireNonNull(actorId);
            Objects.requireNonNull(tenantId);
            caseIds = Set.copyOf(caseIds);
            operations = Set.copyOf(operations);
        }
    }

    public record Proposal(UUID operationId, Operation operation,
                           String caseId, int quantity) {
        public Proposal {
            Objects.requireNonNull(operationId);
            Objects.requireNonNull(operation);
            Objects.requireNonNull(caseId);
        }
    }

    // Loaded from the business service using the trusted tenant and requested case.
    public record CaseRecord(String tenantId, String caseId, long version,
                             boolean replacementEligible, int replacementLimit) { }

    public record Verdict(Decision decision, String reason, String policyVersion) { }

    public static Verdict evaluate(Context context, Proposal proposal, CaseRecord current) {
        if (current == null
                || !context.tenantId().equals(current.tenantId())
                || !context.caseIds().contains(proposal.caseId())
                || !proposal.caseId().equals(current.caseId())) {
            return verdict(Decision.DENY, "CASE_ACCESS_DENIED");
        }
        if (!context.operations().contains(proposal.operation())) {
            return verdict(Decision.DENY, "OPERATION_NOT_GRANTED");
        }
        return switch (proposal.operation()) {
            case DRAFT_REPLY -> proposal.quantity() == 0
                    ? verdict(Decision.ALLOW, "DRAFT_ONLY")
                    : verdict(Decision.DENY, "UNEXPECTED_QUANTITY");
            case REQUEST_REPLACEMENT -> {
                if (!current.replacementEligible() || proposal.quantity() < 1
                        || proposal.quantity() > current.replacementLimit()) {
                    yield verdict(Decision.DENY, "REPLACEMENT_NOT_ELIGIBLE");
                }
                yield verdict(Decision.REQUIRE_APPROVAL, "REVIEW_REPLACEMENT");
            }
            case EXPORT_CASES -> verdict(Decision.DENY, "EXPORT_OUTSIDE_TASK");
        };
    }

    private static Verdict verdict(Decision decision, String reason) {
        return new Verdict(decision, reason, POLICY_VERSION);
    }

    public static void main(String[] args) {
        var context = new Context("support-17", "tenant-acme", Set.of("CASE-1042"),
                Set.of(Operation.DRAFT_REPLY, Operation.REQUEST_REPLACEMENT));
        var current = new CaseRecord("tenant-acme", "CASE-1042", 7, true, 1);
        for (Operation operation : Operation.values()) {
            var proposal = new Proposal(UUID.randomUUID(), operation, "CASE-1042",
                    operation == Operation.REQUEST_REPLACEMENT ? 1 : 0);
            var verdict = evaluate(context, proposal, current);
            System.out.println(operation + ": " + verdict.decision() + " / " + verdict.reason());
        }
        var otherCase = new CaseRecord("tenant-other", "CASE-2099", 1, true, 1);
        var proposal = new Proposal(UUID.randomUUID(), Operation.DRAFT_REPLY, "CASE-2099", 0);
        System.out.println("OTHER_CASE: " + evaluate(context, proposal, otherCase).reason());
    }
}
