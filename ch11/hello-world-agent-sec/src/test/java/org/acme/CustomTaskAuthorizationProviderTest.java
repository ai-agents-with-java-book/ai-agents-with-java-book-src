package org.acme;

import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.AuthenticatedUser;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.a2aproject.sdk.server.auth.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.a2aproject.sdk.server.auth.TaskOperation.*;
import static org.junit.jupiter.api.Assertions.*;

class CustomTaskAuthorizationProviderTest {

    private final CustomTaskAuthorizationProvider provider = new CustomTaskAuthorizationProvider();
    private final ServerCallContext alice = context(new AuthenticatedUser("alice"));
    private final ServerCallContext bob = context(new AuthenticatedUser("bob"));

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidIdentities")
    void invalidIdentityCannotCreateReadWriteOrRecordOwnership(String description, ServerCallContext caller) {
        provider.recordOwnership(alice, "owned-task", MESSAGE_SEND);

        assertAll(
                () -> assertFalse(provider.checkCreate(caller, MESSAGE_SEND)),
                () -> assertFalse(provider.checkRead(caller, "owned-task", GET_TASK)),
                () -> assertFalse(provider.checkWrite(caller, "owned-task", CANCEL_TASK)),
                () -> {
                    provider.recordOwnership(caller, "new-task", MESSAGE_SEND);
                    assertFalse(provider.isTaskRecorded("new-task"));
                });
    }

    static Stream<Arguments> invalidIdentities() {
        return Stream.of(
                Arguments.of("missing context", null),
                Arguments.of("missing SDK user", context(null)),
                Arguments.of("SDK anonymous user", context(UnauthenticatedUser.INSTANCE)),
                Arguments.of("unauthenticated user claiming the owner's name", context(user(false, "alice"))),
                Arguments.of("authenticated user with null ID", context(user(true, null))),
                Arguments.of("authenticated user with empty ID", context(new AuthenticatedUser(""))),
                Arguments.of("authenticated user with blank ID", context(new AuthenticatedUser(" \t\n"))));
    }

    @Test
    void separateAnonymousRequestsCannotShareTaskOwnership() {
        var first = context(UnauthenticatedUser.INSTANCE);
        var second = context(UnauthenticatedUser.INSTANCE);

        provider.recordOwnership(first, "anonymous-task", MESSAGE_SEND);

        assertAll(
                () -> assertFalse(provider.isTaskRecorded("anonymous-task")),
                () -> assertFalse(provider.checkRead(second, "anonymous-task", GET_TASK)),
                () -> assertFalse(provider.checkWrite(second, "anonymous-task", CANCEL_TASK)));
    }

    @Test
    void authenticatedOwnerCanCreateReadAndWriteAcrossRequests() {
        assertTrue(provider.checkCreate(alice, MESSAGE_SEND));
        provider.recordOwnership(alice, "task", MESSAGE_SEND);
        var nextRequest = context(new AuthenticatedUser("alice"));

        assertAll(
                () -> assertTrue(provider.isTaskRecorded("task")),
                () -> assertTrue(provider.checkRead(nextRequest, "task", GET_TASK)),
                () -> assertTrue(provider.checkWrite(nextRequest, "task", CANCEL_TASK)));
    }

    @Test
    void differentAuthenticatedUserCannotReadOrWrite() {
        provider.recordOwnership(alice, "task", MESSAGE_SEND);

        assertAll(
                () -> assertFalse(provider.checkRead(bob, "task", GET_TASK)),
                () -> assertFalse(provider.checkWrite(bob, "task", CANCEL_TASK)));
    }

    @Test
    void unrecordedTaskIsDeniedEvenForAnAuthenticatedUser() {
        assertAll(
                () -> assertFalse(provider.isTaskRecorded("missing")),
                () -> assertFalse(provider.checkRead(alice, "missing", GET_TASK)),
                () -> assertFalse(provider.checkWrite(alice, "missing", CANCEL_TASK)));
    }

    @Test
    void recordingOwnershipIsIdempotentAndDoesNotReplaceTheOriginalOwner() {
        provider.recordOwnership(alice, "task", MESSAGE_SEND);
        provider.recordOwnership(alice, "task", MESSAGE_SEND);
        provider.recordOwnership(bob, "task", MESSAGE_SEND);

        assertAll(
                () -> assertTrue(provider.checkRead(alice, "task", GET_TASK)),
                () -> assertTrue(provider.checkWrite(alice, "task", CANCEL_TASK)),
                () -> assertFalse(provider.checkRead(bob, "task", GET_TASK)),
                () -> assertFalse(provider.checkWrite(bob, "task", CANCEL_TASK)));
    }

    private static ServerCallContext context(User user) {
        return new ServerCallContext(user, Map.of(), Set.of());
    }

    private static User user(boolean authenticated, String username) {
        return new User() {
            @Override
            public boolean isAuthenticated() {
                return authenticated;
            }

            @Override
            public String getUsername() {
                return username;
            }
        };
    }
}
