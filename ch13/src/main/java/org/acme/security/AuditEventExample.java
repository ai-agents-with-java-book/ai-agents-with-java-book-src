package org.acme.security;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.UUID;

public final class AuditEventExample {
    public enum EventType { AUTHORIZATION, EXECUTION_RESULT }
    public enum Outcome { DENIED, APPROVED, SUCCEEDED, FAILED, UNKNOWN }

    // References can still be personal data. The sink needs its own access/retention policy.
    public record SecurityEvent(UUID eventId, UUID operationId, String occurredAt,
                                String actorRef, String serviceRef, String tenantRef,
                                String targetRef, String operation, String policyVersion,
                                EventType eventType, Outcome outcome, String reasonCode,
                                UUID approvalRef, String businessResultRef) { }

    public static void main(String[] args) throws Exception {
        String scenario = args.length == 0 ? "denied" : args[0];
        if (!scenario.equals("denied") && !scenario.equals("completed")) {
            throw new IllegalArgumentException("Use denied or completed");
        }
        var json = JsonMapper.builder().build();
        var operationId = UUID.randomUUID();
        if (scenario.equals("denied")) {
            var denied = new SecurityEvent(UUID.randomUUID(), operationId, Instant.now().toString(),
                    "actor-17", "support-agent", "tenant-acme", "CASE-1042",
                    "EXPORT_CASES", ActionPolicyExample.POLICY_VERSION,
                    EventType.AUTHORIZATION, Outcome.DENIED, "OPERATION_NOT_GRANTED", null, null);
            System.out.println(json.writeValueAsString(denied));
        } else {
            // These two records illustrate evidence after a trusted service reports success.
            // This program does not contact a replacement service or issue a shipment.
            var approvalId = UUID.randomUUID();
            var authorized = new SecurityEvent(UUID.randomUUID(), operationId, Instant.now().toString(),
                    "actor-17", "support-agent", "tenant-acme", "CASE-1042",
                    "REQUEST_REPLACEMENT", ActionPolicyExample.POLICY_VERSION,
                    EventType.AUTHORIZATION, Outcome.APPROVED, "EXACT_ACTION_APPROVED", approvalId, null);
            var completed = new SecurityEvent(UUID.randomUUID(), operationId, Instant.now().toString(),
                    "actor-17", "support-agent", "tenant-acme", "CASE-1042",
                    "REQUEST_REPLACEMENT", ActionPolicyExample.POLICY_VERSION,
                    EventType.EXECUTION_RESULT, Outcome.SUCCEEDED, "BUSINESS_SERVICE_CONFIRMED",
                    approvalId, "replacement-204");
            System.out.println(json.writeValueAsString(authorized));
            System.out.println(json.writeValueAsString(completed));
        }
    }
}
