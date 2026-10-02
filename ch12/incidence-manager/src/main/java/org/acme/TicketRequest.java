package org.acme;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record TicketRequest(
    @NotBlank
    String user,

    @Email
    String email,

    @Min(1)
    Long bookingId,

    @NotEmpty
    String message)
{}
