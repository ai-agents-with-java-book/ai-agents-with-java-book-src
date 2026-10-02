package org.acme.security;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public final class ContextSelectionExample {
    public record Access(String tenantId, Set<String> caseIds) {
        public Access { caseIds = Set.copyOf(caseIds); }
    }

    public record StoredCase(String tenantId, String caseId, String orderId,
                             long version, LocalDate promisedDate, String deliveryStatus,
                             String customerEmail, String internalNote, boolean eligible,
                             Instant usableUntil) { }

    // A reference returned by retrieval; its text is deliberately not trusted here.
    public record RetrievedReference(String tenantId, String caseId, long sourceVersion) { }

    public record ModelCase(String orderId, LocalDate promisedDate, String deliveryStatus) {
        public String context() {
            return "Order: " + orderId + "\nPromised date: " + promisedDate
                    + "\nDelivery status: " + deliveryStatus;
        }
    }

    public static ModelCase select(Access access, RetrievedReference reference,
                                   StoredCase current, Instant now) {
        if (current == null || !access.tenantId().equals(current.tenantId())
                || !access.caseIds().contains(current.caseId())
                || !current.tenantId().equals(reference.tenantId())
                || !current.caseId().equals(reference.caseId())) {
            throw new IllegalArgumentException("CASE_ACCESS_DENIED");
        }
        if (!current.eligible() || !now.isBefore(current.usableUntil())
                || reference.sourceVersion() != current.version()) {
            throw new IllegalArgumentException("SOURCE_NOT_ELIGIBLE");
        }
        return new ModelCase(current.orderId(), current.promisedDate(), current.deliveryStatus());
    }

    public static void main(String[] args) {
        String scenario = args.length == 0 ? "current" : args[0];
        if (!Set.of("current", "other-tenant", "expired", "stale", "withdrawn").contains(scenario)) {
            throw new IllegalArgumentException("Use current, other-tenant, expired, stale, or withdrawn");
        }
        Instant now = Instant.now();
        var access = new Access("tenant-acme", Set.of("CASE-1042"));
        var current = new StoredCase("tenant-acme", "CASE-1042", "ORD-1042", 7,
                LocalDate.of(2026, 9, 8), "Carrier check pending", "customer@example.invalid",
                "Internal account note", !scenario.equals("withdrawn"),
                scenario.equals("expired") ? now : now.plusSeconds(3600));
        var reference = new RetrievedReference(
                scenario.equals("other-tenant") ? "tenant-other" : "tenant-acme",
                "CASE-1042", scenario.equals("stale") ? 6 : 7);
        try {
            System.out.println(select(access, reference, current, now).context());
        } catch (IllegalArgumentException rejected) {
            System.out.println("REJECTED: " + rejected.getMessage());
        }
    }
}
