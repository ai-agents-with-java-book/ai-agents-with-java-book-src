# AI Agents with Java — example source code

Companion examples for *AI Agents with Java* by **Alex Soto Bueno, Markus Eisele, and Mario Fusco**, published by **O’Reilly Media**.

[Read the book on O’Reilly](https://www.oreilly.com/library/view/ai-agents-with/0642572245856/) · [Book organization](https://github.com/ai-agents-with-java-book)

Explore agent coordination, memory, retrieval, tool use, communication protocols, security, and observability through Java and Quarkus examples.

## Find an example

Each project has its own `pom.xml` and dependencies. There is no root Maven build; run commands from the example’s directory.

| Directory | Topics and projects |
| --- | --- |
| [ch03/chatmemory](ch03/chatmemory/) | Chat memory, persistent memory, and input/output guardrails |
| [ch05](ch05/) | Agentic workflows, supervisors, goal-oriented action planning (GOAP), and peer-to-peer coordination |
| [ch07](ch07/) | Retrieval-augmented generation (RAG), HTML transformation, query compression, and Presidio document transformation |
| [ch08/current-time-tool](ch08/current-time-tool/) | Tool calling, low-level tool execution, and dynamic tools |
| [ch10](ch10/) | Model Context Protocol (MCP) clients and servers, incident management, resources, and advanced server features |
| [ch11](ch11/) | Agent2Agent (A2A) clients and agents, human interaction, security, and replication |
| [ch13](ch13/README.md) | Action policies, approval validation, context selection, audit events, and isolated Java execution |
| [ch14/observability](ch14/observability/) | Agent tracing and metrics with OpenTelemetry, Grafana, and LangSmith |
| [ch77](ch77/) | Embeddings, chunking, hybrid search, multimodal RAG, movie recommendations, and shopping question answering |

This index follows the source directories in this checkout. Use the book alongside each example for its explanation and context.

## Prerequisites

- **JDK:** Check the selected project’s `pom.xml`. Projects target Java 17, 21, or 25; several require JDK 25 to compile.
- **Maven:** Use `./mvnw` where a wrapper is included, or an installed Maven where it is absent. On Windows, use `mvnw.cmd` for projects with a wrapper.
- **Model access:** Requirements vary by example, as described below.
- **Additional services:** Some projects need containers, databases, or other services. MCP clients that launch `npx` also need Node.js and npm.

Check which Java installation your build uses:

```shell
java -version
mvn --version
```

## Get started without model credentials

Clone the source repository and run the Chapter 13 action-policy example with JDK 21 or later and Maven:

```shell
git clone https://github.com/ai-agents-with-java-book/ai-agents-with-java-book-src.git
cd ai-agents-with-java-book-src/ch13
mvn compile
java -cp target/classes org.acme.security.ActionPolicyExample
```

The example evaluates proposed actions and prints:

```text
DRAFT_REPLY: ALLOW / DRAFT_ONLY
REQUEST_REPLACEMENT: REQUIRE_APPROVAL / REVIEW_REPLACEMENT
EXPORT_CASES: DENY / OPERATION_NOT_GRANTED
OTHER_CASE: CASE_ACCESS_DENIED
```

See the [Chapter 13 README](ch13/README.md) for the other security examples and the separate container requirements for isolated execution.

## Run a tool-calling example

From the repository root, use JDK 21 or later to build and run the current-time tool:

```shell
cd ch08/current-time-tool
./mvnw package
java -jar target/my-app-1.0-SNAPSHOT-jar-with-dependencies.jar
```

The application asks a model to answer a date-and-time question using the Java `Clock` tool. Its source is configured for the LangChain4j demo endpoint with the `demo` API key, so it needs network access and depends on that service’s availability.

Other standalone Java projects can contain several `main` classes. Select the class for the example you are following; do not assume every project’s packaged JAR launches every example.

## Configure model access

There is no shared model configuration for the repository. Inspect the selected project’s Java model builder and, for Quarkus applications, `src/main/resources/application.properties`.

- **Chat memory:** `ch03/chatmemory` uses Ollama with `qwen2.5:7b`. Its default address is `http://127.0.0.1:11434`; `OLLAMA_BASE_URL` overrides it. Make the model available in Ollama before running an example.
- **Chapter 5:** Model providers read `OPENAI_API_KEY`; alternative provider code also references `CEREBRAS_API_KEY`.
- **Demo-backed examples:** Several RAG, tool, and MCP examples configure the LangChain4j demo endpoint directly in source.
- **Observability:** `ch14/observability` requires `OPENAI_API_KEY`. The LangSmith profile also uses `LANGSMITH_API_KEY`, with optional `LANGSMITH_PROJECT` and `LANGSMITH_OTLP_ENDPOINT` settings.

## Run Quarkus applications

From a Quarkus project directory, start development mode after configuring its required services:

```shell
./mvnw quarkus:dev
```

For projects without a wrapper, such as `ch14/observability`, use:

```shell
mvn quarkus:dev
```

Examples using Quarkus Dev Services need a running compatible container engine. Check the project configuration for ports and service dependencies, especially when running an MCP or A2A client and server together.

## Report a problem

[Open an issue](https://github.com/ai-agents-with-java-book/ai-agents-with-java-book-src/issues) with the example directory, Java and Maven versions, command you ran, and relevant error output. Remove credentials from logs before sharing them.
