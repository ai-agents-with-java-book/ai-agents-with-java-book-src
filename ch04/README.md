# Chapter 4: frameworks and tools

These four applications follow the support-summary example in chapter 4. Each is a separate project, so you can build the framework you want to try without starting the others. Use JDK 21. The Maven wrappers select Maven 3.9.11; the Gradle wrapper selects Gradle 8.14.3.

| Directory | Command from that directory | What it checks |
| --- | --- | --- |
| `support-core` | `./mvnw package` | Compiles the command-line example. This project has no test suite. |
| `support-quarkus` | `./mvnw package` | Runs the chapter's eight caller, graph, and HTTP/provider checks, then packages the application. |
| `support-quarkus-gradle` | `./gradlew build` | Builds the same sources and runs the same eight checks with Gradle. |
| `support-spring` | `./mvnw package` | Runs the three shared caller checks and three Spring HTTP/provider checks, then creates an executable JAR. |

The tests start WireMock on an available local port and supply an invented credential. You don't need `OPENAI_API_KEY`, `OPENAI_MODEL`, a model account, or a container engine for these commands. The Quarkus timeout case expects two attempts; Spring disables retries and expects one. Run the projects separately when using their default HTTP test ports.

The baseline is Quarkus 3.33.2, Quarkus LangChain4j 1.13.1, LangChain4j 1.20.0, LangGraph4j 1.8.0, Spring Boot 3.5.13, the LangChain4j Spring starters at 1.20.0-beta30, and WireMock standalone 3.13.1. The LangChain4j BOM comes first in the Quarkus Maven build; Gradle enforces the same BOM. To inspect the resolved dependencies, run `./mvnw quarkus:dependency-tree` in the Quarkus Maven project, `./gradlew dependencies` in the Gradle project, or `./mvnw dependency:tree` in core or Spring.

## Run an application

For a live model call, supply `OPENAI_API_KEY` and `OPENAI_MODEL` through your usual secret-handling process. The core entry point also accepts `OPENAI_BASE_URL` for a compatible endpoint:

```sh
cd support-core
./mvnw compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  -Dexec.mainClass=org.acme.support.SummaryCli \
  -Dexec.args="Where is ORD-1042?"
```

Start Quarkus with `./mvnw quarkus:dev` or `./gradlew quarkusDev -Dquarkus.http.host=127.0.0.1`. After stopping development mode, run the packaged application:

```sh
java -Dquarkus.http.host=127.0.0.1 -jar target/quarkus-app/quarkus-run.jar
```

Use `build/quarkus-app/quarkus-run.jar` for Gradle. Keep the complete `quarkus-app` directory with the JAR.

Start Spring with `./mvnw spring-boot:run`, or run `java -jar target/support-spring-1.0.0-SNAPSHOT.jar` after packaging. Its shared configuration binds both launch paths to `127.0.0.1`. Run one application at a time on port 8080, then send:

```sh
curl --fail-with-body http://localhost:8080/summaries \
  -H 'Content-Type: text/plain' --data 'Where is ORD-1042?'
```

For a compatible local endpoint, set `QUARKUS_LANGCHAIN4J_OPENAI_BASE_URL` for Quarkus, or pass `--langchain4j.open-ai.chat-model.base-url=...` to the Spring application. The tests supply these settings themselves. Live-model calls are separate from the stub checks above.

## Quarkus Flow build variant

The [Flow build variant](support-quarkus/optional-flow/README.md) reuses the existing summary service. It is disabled in the default builds. Enable it with `-Pflow` in the Maven or Gradle Quarkus build to add `/flow/summaries`. The linked instructions cover packaging, calling the endpoint, and switching back to the default build.
