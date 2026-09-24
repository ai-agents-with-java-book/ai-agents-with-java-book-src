package org.acme.security;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.acme.security.ActionPolicyExample.*;

public final class ApprovalExample {
    public record Approval(UUID id, String actorId, String tenantId,
                           Proposal proposal, long caseVersion, String policyVersion,
                           String reviewerId, Instant expiresAt) { }

    public enum Result { APPROVED, POLICY_DENIED, MISMATCH, EXPIRED, UNKNOWN_OR_USED }

    public static final class ApprovalGate {
        private final ConcurrentHashMap<UUID, Approval> pending = new ConcurrentHashMap<>();

        // Only the trusted approval handler calls this, after authorizing the reviewer.
        public void recordApproval(Approval approval) {
            if (pending.putIfAbsent(approval.id(), approval) != null) {
                throw new IllegalStateException("Approval already recorded");
            }
        }

        public Result authorizeAndConsume(UUID approvalId, Context context,
                                          Proposal proposal, CaseRecord current, Instant now) {
            Verdict verdict = evaluate(context, proposal, current);
            if (verdict.decision() != Decision.REQUIRE_APPROVAL) {
                return Result.POLICY_DENIED;
            }
            Approval approval = pending.get(approvalId);
            if (approval == null) {
                return Result.UNKNOWN_OR_USED;
            }
            if (!now.isBefore(approval.expiresAt())) {
                pending.remove(approvalId, approval);
                return Result.EXPIRED;
            }
            if (!approval.actorId().equals(context.actorId())
                    || !approval.tenantId().equals(context.tenantId())
                    || !approval.proposal().equals(proposal)
                    || approval.caseVersion() != current.version()
                    || !approval.policyVersion().equals(verdict.policyVersion())) {
                return Result.MISMATCH;
            }
            return pending.remove(approvalId, approval)
                    ? Result.APPROVED : Result.UNKNOWN_OR_USED;
        }
    }

    public static void main(String[] args) {
        String scenario = args.length == 0 ? "valid" : args[0];
        if (!Set.of("valid", "changed", "expired", "replay", "revoked", "missing", "stale")
                .contains(scenario)) {
            throw new IllegalArgumentException("Use valid, changed, expired, replay, revoked, missing, or stale");
        }
        var now = Instant.now();
        var context = new Context("support-17", "tenant-acme", Set.of("CASE-1042"),
                Set.of(Operation.REQUEST_REPLACEMENT));
        // Two eligible units let the changed-quantity example reach approval matching.
        var current = new CaseRecord("tenant-acme", "CASE-1042", 7, true, 2);
        var proposal = new Proposal(UUID.randomUUID(), Operation.REQUEST_REPLACEMENT, "CASE-1042", 1);
        var approval = new Approval(UUID.randomUUID(), context.actorId(), context.tenantId(),
                proposal, current.version(), POLICY_VERSION, "reviewer-3",
                scenario.equals("expired") ? now : now.plusSeconds(300));
        var gate = new ApprovalGate();
        if (!scenario.equals("missing")) {
            gate.recordApproval(approval);
        }
        if (scenario.equals("changed")) {
            proposal = new Proposal(proposal.operationId(), proposal.operation(), proposal.caseId(), 2);
        }
        if (scenario.equals("revoked")) {
            context = new Context(context.actorId(), context.tenantId(), context.caseIds(), Set.of());
        }
        if (scenario.equals("stale")) {
            current = new CaseRecord(current.tenantId(), current.caseId(), 8, true, 2);
        }
        System.out.println(gate.authorizeAndConsume(approval.id(), context, proposal, current, now));
        if (scenario.equals("replay")) {
            System.out.println(gate.authorizeAndConsume(approval.id(), context, proposal, current, now));
        }
    }
}
