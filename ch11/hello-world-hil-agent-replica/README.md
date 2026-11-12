# Replicated HIL agent with persistent ownership

Use the [chapter 11 baseline](../README.md): JDK 25, Maven 3.9.16, Quarkus 3.38.0 and A2A SDK 1.3.1.Final. This example needs PostgreSQL, Kafka and an OIDC provider. The Agent Card is public; task requests require authentication and the SDK task-authorization policy.

`PersistentTaskAuthorizationProvider` stores an immutable task-to-owner association in `a2a_task_owner`, alongside the SDK's task data in the `a2a-java` persistence unit. Its key combines the configured trusted issuer URL and the authenticated subject. `quarkus.oidc.token.principal-claim=sub` makes the SDK user name the stable subject, rather than a changeable display/login name. All replicas must use the same issuer configuration and database.

Creation requires a nonblank authenticated identity, without requiring an owner row in advance. The SDK invokes `recordOwnership` for the new task. The task ID is unique; PostgreSQL's `ON CONFLICT DO NOTHING` makes repeated registration by the same owner idempotent. A conflicting owner cannot replace it. Read, write, resume and cancel require the stored owner. An existing task without an owner record is denied. Keep `a2a.authorization.required=true`.

## Run two instances

Build once. Dev mode can start PostgreSQL, Kafka and Keycloak through Dev Services; the following packaged run uses services that are already running. The example retains schema generation with `update`; use your normal migration process for a deployed database.

```sh
./mvnw package -DskipTests
export DATABASE_URL=jdbc:postgresql://localhost:5432/a2a
export DATABASE_USER=a2a
export DATABASE_PASSWORD=a2a
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export OIDC_AUTH_SERVER_URL=http://localhost:11001/realms/quarkus
```

Start the first instance:

```sh
POD_NAME=one AGENT_URL=http://localhost:7777 \
  java -Dquarkus.http.host=127.0.0.1 -Dquarkus.http.port=7777 \
  -jar target/quarkus-app/quarkus-run.jar
```

In another terminal with the same service environment, start the second:

```sh
POD_NAME=two AGENT_URL=http://localhost:7778 \
  java -Dquarkus.http.host=127.0.0.1 -Dquarkus.http.port=7778 \
  -jar target/quarkus-app/quarkus-run.jar
```

Each instance needs its own Kafka consumer group, selected here by `POD_NAME`. Obtain Alice and Bob access tokens from this issuer and assign them to `ALICE_TOKEN` and `BOB_TOKEN` locally. Do not put access tokens in source files.

## Check ownership through HTTP

Create a task as Alice on the first instance. These are A2A 1.0 JSON-RPC wire operations; the `A2A-Version` header is required by this SDK baseline.

```sh
curl --fail-with-body http://localhost:7777/ \
  -H 'A2A-Version: 1.0' -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $ALICE_TOKEN" \
  --data '{"jsonrpc":"2.0","id":"start","method":"SendMessage","params":{"message":{"messageId":"greeting-1","role":"ROLE_USER","parts":[{"text":"Persist Me"}]}}}'
```

Set `TASK_ID` and `CONTEXT_ID` to the returned task's `id` and `contextId`. Its status is `TASK_STATE_INPUT_REQUIRED`. Read it through the other instance:

```sh
curl --fail-with-body http://localhost:7778/ \
  -H 'A2A-Version: 1.0' -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $ALICE_TOKEN" \
  --data "{\"jsonrpc\":\"2.0\",\"id\":\"read\",\"method\":\"GetTask\",\"params\":{\"id\":\"$TASK_ID\"}}"
```

Repeat with Bob's token: the JSON-RPC response must contain an error and no task data. Without a token, the HTTP response must be 401. An unknown task ID must also be denied.

Stop the first instance and restart it with the same service settings. Alice must be able to read the task there; Bob and anonymous callers must be denied. Resume it through the second instance:

```sh
curl --fail-with-body http://localhost:7778/ \
  -H 'A2A-Version: 1.0' -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $ALICE_TOKEN" \
  --data "{\"jsonrpc\":\"2.0\",\"id\":\"resume\",\"method\":\"SendMessage\",\"params\":{\"message\":{\"messageId\":\"greeting-2\",\"role\":\"ROLE_USER\",\"taskId\":\"$TASK_ID\",\"contextId\":\"$CONTEXT_ID\",\"parts\":[{\"text\":\"upper\"}]}}}"
```

The result must contain `PERSIST ME` and `TASK_STATE_COMPLETED`. The restarted first instance must read that completed task. Also try `SendMessage` and `CancelTask` as Bob and without a token, both before and after restart; they must be denied. `CancelTask` takes the same `params` shape as `GetTask`. JSON-RPC authorization failures can arrive with HTTP 200, so inspect the `error` field rather than HTTP status alone.

In a disposable test database, removing only this test task's `a2a_task_owner` row must make reads and replies fail even though the SDK task row still exists. Do not backfill missing ownership from whichever caller presents the task ID.

## Terminal client and regression tests

The replica terminal client uses the same `HilSession` coordinator as the local HIL client and attaches a supplied access token to its streaming requests:

```sh
./mvnw dependency:build-classpath -Dmdep.outputFile=target/client-classpath.txt
A2A_AGENT_URL=http://localhost:7777 A2A_BEARER_TOKEN="$ALICE_TOKEN" \
  java -cp "target/classes:$(cat target/client-classpath.txt)" org.acme.A2AClient
```

```sh
./mvnw test -Dtest=PersistentTaskAuthorizationProviderTest,GreetingResourceTest
```

The tests use the configured services or Dev Services and check creation, durable lookup across calls, immutable/idempotent registration, unknown tasks, and anonymous/blank identities. The earlier `hello-world-agent-sec` provider tests are retained. The HTTP sequence above additionally checks the actual token boundary, restart and routing through another instance.

This example resumes a task that deliberately paused for human input and persisted its original text. It does not recover Java code interrupted mid-step or guarantee replay of every missed Kafka event. Checkpoints, recovery ownership and protection against repeated external effects are separate application responsibilities.
