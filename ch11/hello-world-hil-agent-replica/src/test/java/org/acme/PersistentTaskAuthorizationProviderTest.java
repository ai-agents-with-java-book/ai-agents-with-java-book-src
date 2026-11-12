package org.acme;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;
import io.quarkus.test.junit.QuarkusTest;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.AuthenticatedUser;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PersistentTaskAuthorizationProviderTest {
    @Inject PersistentTaskAuthorizationProvider provider;

    private static ServerCallContext user(String id) {
        return new ServerCallContext(new AuthenticatedUser(id), Map.of(), Set.of());
    }

    @Test
    void creationDoesNotRequireARowAndRecordedOwnershipSurvivesNewCalls() {
        String id = UUID.randomUUID().toString();
        assertTrue(provider.checkCreate(user("alice"), TaskOperation.MESSAGE_SEND));
        assertFalse(provider.isTaskRecorded(id));
        assertFalse(provider.checkRead(user("alice"), id, TaskOperation.GET_TASK));
        provider.recordOwnership(user("alice"), id, TaskOperation.MESSAGE_SEND);
        assertTrue(provider.isTaskRecorded(id));
        assertTrue(provider.checkRead(user("alice"), id, TaskOperation.GET_TASK));
        assertTrue(provider.checkWrite(user("alice"), id, TaskOperation.MESSAGE_SEND));
        assertFalse(provider.checkRead(user("bob"), id, TaskOperation.GET_TASK));
        assertFalse(provider.checkWrite(user("bob"), id, TaskOperation.MESSAGE_SEND));
    }

    @Test
    void repeatedRegistrationIsIdempotentAndCannotTransferOwnership() {
        String id = UUID.randomUUID().toString();
        provider.recordOwnership(user("alice"), id, TaskOperation.MESSAGE_SEND);
        provider.recordOwnership(user("alice"), id, TaskOperation.MESSAGE_SEND);
        assertThrows(TaskNotFoundError.class,
                () -> provider.recordOwnership(user("bob"), id, TaskOperation.MESSAGE_SEND));
        assertTrue(provider.checkRead(user("alice"), id, TaskOperation.GET_TASK));
        assertFalse(provider.checkWrite(user("bob"), id, TaskOperation.MESSAGE_SEND));
    }

    @Test
    void anonymousBlankAndMissingIdentitiesAreDenied() {
        String id = UUID.randomUUID().toString();
        provider.recordOwnership(user("alice"), id, TaskOperation.MESSAGE_SEND);
        for (ServerCallContext context : new ServerCallContext[] { null,
                new ServerCallContext(UnauthenticatedUser.INSTANCE, Map.of(), Set.of()),
                user(""), user(" ") }) {
            assertFalse(provider.checkCreate(context, TaskOperation.MESSAGE_SEND));
            assertFalse(provider.checkRead(context, id, TaskOperation.GET_TASK));
            assertFalse(provider.checkWrite(context, id, TaskOperation.MESSAGE_SEND));
            assertThrows(TaskNotFoundError.class,
                    () -> provider.recordOwnership(context, id, TaskOperation.MESSAGE_SEND));
        }
    }

    @Test
    void unknownOrMissingTaskIdsNeverGrantAccess() {
        for (String id : new String[] { null, "", " ", UUID.randomUUID().toString() }) {
            assertFalse(provider.isTaskRecorded(id));
            assertFalse(provider.checkRead(user("alice"), id, TaskOperation.GET_TASK));
            assertFalse(provider.checkWrite(user("alice"), id, TaskOperation.MESSAGE_SEND));
        }
    }
}
