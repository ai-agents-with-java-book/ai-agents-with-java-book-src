package org.acme;

public record CustomerSupportInformation(String user, String userEmail, String subject, String request, String response, Long bookingId) {
}
