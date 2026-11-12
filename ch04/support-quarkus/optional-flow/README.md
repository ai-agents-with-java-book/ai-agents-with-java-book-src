# Optional summary task with Quarkus Flow

The `flow` build variant includes `SummaryFlow`, its `/flow/summaries` endpoint, and Quarkus Flow 1.2.0.CR2. It reuses the chapter's model client, `SupportSummary`, and input/error contract. The default build keeps the direct `/summaries` endpoint and has no Flow dependency.

Use JDK 21 and build from `ch04/support-quarkus`:

```sh
./mvnw -Pflow clean package
./mvnw -Pflow quarkus:dependency-tree
```

For Gradle, from `ch04/support-quarkus-gradle`:

```sh
./gradlew -Pflow clean build
./gradlew -Pflow dependencies
```

The Gradle variant reads the same optional Java sources from the adjacent `support-quarkus/optional-flow/src/main/java` directory. Keep those chapter 4 directories together. Both variants run the existing eight tests without model credentials; these tests cover the shared summary contract and direct endpoint.

For a live call, supply `OPENAI_API_KEY` and `OPENAI_MODEL`. Start one packaged application:

```sh
java -Dquarkus.http.host=127.0.0.1 -jar target/quarkus-app/quarkus-run.jar
```

Use `build/quarkus-app/quarkus-run.jar` for Gradle. Keep the whole `quarkus-app` directory with the JAR. A compatible local provider can be selected with `QUARKUS_LANGCHAIN4J_OPENAI_BASE_URL`.

```sh
curl --fail-with-body http://localhost:8080/flow/summaries \
  -H 'Content-Type: text/plain' --data 'Where is ORD-1042?'
```

The blocking REST method runs on a worker thread with an active request context, which the request-scoped summary service needs. It waits up to one minute for the task result. `SupportDrafts` rejects blank input before a model call and translates a provider or workflow failure into the existing HTTP 502 response. Workflow state is held in memory; chapter 12 develops persistence and human review.

Stop the application before switching variants. Run `./mvnw clean package` or `./gradlew clean build` without `-Pflow` to remove the optional classes and dependency. A clean build is needed when switching back because incremental compilation can retain the earlier Flow classes.
