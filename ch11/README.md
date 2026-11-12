# Chapter 11: A2A baseline

Use JDK 25 for these projects. The server projects use Quarkus 3.38.0; `quarkus-client-agent` uses 3.38.1 and Quarkus LangChain4j 1.14.0.CR2. Each project has its own Maven wrapper, which selects Maven 3.9.16. These are the chapter 11 requirements; chapter 4's Java 21 setup is a separate baseline.

The server POMs use one `a2a.version` property, set to `1.3.1.Final`, for their direct A2A dependencies. The LangChain4j adapter stays at `1.20.0-beta30` and brings A2A client and JSON-RPC transport `1.3.1.Final`. Its resolved LangChain4j core is `1.20.0`. Inspect both runtime and deployment dependencies with `./mvnw quarkus:dependency-tree` after changing a version.

## Build and check the examples

Run commands from the named project directory. The first package command builds the application and compiles its test sources; it deliberately skips execution of tests that need a running service.

| Project | Build | Check and runtime requirements |
| --- | --- | --- |
| `hello-world-agent` | `./mvnw package -DskipTests` | `./mvnw test` checks its Agent Card. The greeting agent needs no LLM or external service. |
| `hello-world-hil-agent` | `./mvnw package -DskipTests` | `./mvnw test` checks its Agent Card and bounded terminal lifecycle. Its terminal client asks for `upper` or `lower`. |
| `hello-world-agent-sec` | `./mvnw package -DskipTests` | `./mvnw test -Dtest=CustomTaskAuthorizationProviderTest` runs the existing authorization-provider checks without Keycloak. Running the secured application needs its OIDC setup. |
| `hello-world-hil-agent-replica` | `./mvnw package -DskipTests` | PostgreSQL, Kafka and OIDC are required. See its README for durable ownership tests and the authenticated restart/two-instance sequence. |
| `quarkus-client-agent` | `./mvnw package -DskipTests` | Run `ContinuationTest` for handle/owner checks. The existing `HelloWorldAgentTest` needs the basic greeting server on port 7777; authenticated HIL HTTP commands are in the module README. |

Run HTTP tests in separate projects sequentially: their default Quarkus test port is 8081. The greeting tests request `/.well-known/agent-card.json`, the endpoint these agents expose.

## Start a local greeting server

From `hello-world-agent` or `hello-world-hil-agent`, run:

```sh
./mvnw quarkus:dev
```

Stop development mode before starting the packaged server on the same port:

```sh
java -Dquarkus.http.host=127.0.0.1 -jar target/quarkus-app/quarkus-run.jar
```

Both server examples advertise `http://localhost:7777`. Their `A2AClient` classes connect to that address. The LangChain4j adapter also connects to port 7777; if you start its own HTTP application, use a different port, such as `./mvnw quarkus:dev -Dquarkus.http.port=8080`.

To run a terminal client with the JDK 25 launcher, use its module directory in a second terminal:

```sh
./mvnw dependency:build-classpath -Dmdep.outputFile=target/client-classpath.txt
java -cp "target/classes:$(cat target/client-classpath.txt)" org.acme.A2AClient
```

The basic and HIL examples set `a2a.authorization.required=false` for the local single-user walkthrough. Keep that setting out of the secured and replica configurations. Those examples retain the SDK's required authorization policy. The secured example uses a process-local owner map; the replica persists immutable ownership in PostgreSQL and verifies the OIDC subject before task operations. Follow the [replica README](hello-world-hil-agent-replica/README.md) for restart and second-instance checks. The adapter's [continuation handles](quarkus-client-agent/README.md) use a separate in-memory store and do not survive its restart.
