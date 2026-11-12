# LangChain4j A2A client

Use the [chapter 11 baseline](../README.md). The existing greeting interfaces and `GET /greeting` are retained. The HIL paths use authenticated POST requests and conversation handles.

## Start and resume a conversation

Start `hello-world-hil-agent` on port 7777. Start this application on port 8080. Dev Services supplies Keycloak on port 11001; the HIL endpoints require a valid bearer token from that issuer. For an existing identity provider or a packaged run, set `OIDC_AUTH_SERVER_URL` to its realm/issuer endpoint and use the `quarkus-app` client ID, or override the OIDC settings for that provider.

```sh
./mvnw package -DskipTests
export OIDC_AUTH_SERVER_URL=http://localhost:11001/realms/quarkus
java -Dquarkus.http.host=127.0.0.1 -jar target/quarkus-app/quarkus-run.jar
```

The packaged command requires Keycloak to be running already. Alternatively, `./mvnw quarkus:dev` starts Dev Services. Obtain a user access token from that issuer and put it in `ALICE_TOKEN` for these requests:

```sh
curl --fail-with-body http://localhost:8080/greeting/hil \
  -H "Authorization: Bearer $ALICE_TOKEN" \
  -H 'Content-Type: application/json' \
  --data '{"text":"Hello Alice"}'
```

An input request returns `handle` and `prompt`. A direct answer returns `result`. Set `HANDLE` to the returned handle, then reply:

```sh
curl --fail-with-body "http://localhost:8080/greeting/hil/$HANDLE" \
  -H "Authorization: Bearer $ALICE_TOKEN" \
  -H 'Content-Type: application/json' \
  --data '{"text":"upper"}'
```

`GreetingResource` derives the owner from the validated OIDC token's issuer and subject. `PendingReplies` stores that owner, both A2A IDs, the expiry and the submission state. Owner identity supplied in JSON is never used. The A2A IDs stay server-side. The remote HIL server in this example is the local single-user server; authentication here protects the adapter's HTTP entry points and continuation handles. The separately secured replica demonstrates enforcement at the remote agent itself.

The default lifetime is ten minutes (`hil.continuation-ttl=PT10M`). This store is in memory and loses handles on restart; it is not shared across adapter instances. The resource rejects a handle before calling the agent when it is unknown, belongs to another caller, has expired, or has already been claimed. Claiming is atomic, so concurrent submissions cannot both invoke the agent. A repeated input request creates a fresh handle for the same owner. The old handle stays consumed.

| Outcome | HTTP status |
| --- | ---: |
| Missing or invalid authentication | 401 |
| Unknown handle or another owner | 404 |
| Expired handle, before expired-entry cleanup | 410 |
| Already submitted, including an uncertain earlier result | 409 |
| Blank text or a choice other than `upper`/`lower` | 400 |
| Remote failure after claiming a reply | 502 |

Expired entries are removed as new handles are created; a removed handle returns 404. A 502 does not prove the remote task rejected the reply. Do not retry that handle or automatically create another task to repeat its effects. Inspect the remote outcome before deciding how to proceed.

## Verification

```sh
./mvnw test -Dtest=ContinuationTest
```

These focused tests cover interleaved conversations, owner checks, expiry, concurrent claims, another input request, blank identity, and uncertain outcomes. They use no external identity service. Verify authentication separately through the HTTP endpoints: start one conversation as Alice and another as Bob, resume them in reverse order, then try Alice's handle with Bob's token and without a token. Only the matching owner may invoke its pending task. Two simultaneous requests with the same handle must produce one successful invocation and one 409.

The pre-existing `HelloWorldAgentTest` requires the basic `hello-world-agent` on port 7777, not the HIL server. Run it separately with `./mvnw test -Dtest=HelloWorldAgentTest`; Quarkus starts the configured OIDC service in test mode unless an external issuer is supplied.
