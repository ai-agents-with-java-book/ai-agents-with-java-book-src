# Observability for AI Agents

This project demonstrates two observability stacks for a multi-agent Quarkus + LangChain4j application. Both rely on OpenTelemetry — the only difference is where traces are exported.

## Prerequisites

```bash
export OPENAI_API_KEY=sk-...
```

Java 21+ and Maven are required.

## Option 1 — Local LGTM Stack (default)

Uses [Quarkus Dev Services](https://quarkus.io/guides/observability-devservices-lgtm) to spin up a local **Grafana / Tempo / Prometheus** stack automatically.

```bash
./mvnw quarkus:dev
```

Quarkus starts the LGTM containers on first run. The console output shows the Grafana URL (typically `http://localhost:<port>`).

| What you get | Where to look |
|---|---|
| Traces (agent pipeline, LLM calls) | Grafana > Explore > Tempo |
| Token usage metrics | Grafana > Dashboards > AI Dashboard |
| Logs with trace correlation | Console output (`traceId`, `spanId` fields) |

The bundled Grafana dashboard (`META-INF/grafana/grafana-dashboard-ai.json`) is provisioned automatically and shows input/output token counts and estimated cost.

No external accounts or API keys are needed — everything runs locally.

## Option 2 — LangSmith

Exports traces to [LangSmith](https://smith.langchain.com) via its OTLP endpoint. No LangSmith-specific SDK is required — Quarkus sends standard OpenTelemetry spans that LangSmith maps into its tracing UI.

### Setup

1. Create a LangSmith account and generate an API key.
2. Set environment variables:

```bash
export LANGSMITH_API_KEY=lsv2_pt_...
export LANGSMITH_PROJECT=expert-chatbot   # optional, defaults to expert-chatbot
```

For EU-hosted LangSmith:

```bash
export LANGSMITH_OTLP_ENDPOINT=https://eu.api.smith.langchain.com/otel
```

3. Run with the `langsmith` profile:

```bash
./mvnw quarkus:dev -Dquarkus.profile=langsmith
```

### What you see in LangSmith

Open the **Runs** tab for your project at [smith.langchain.com](https://smith.langchain.com). Each request produces a trace tree:

| Span | Content |
|---|---|
| HTTP request | Entry point (`POST` or WebSocket) |
| Agent service | `ExpertsChatbot` sequence, classifier, router |
| Model completion | Model ID, input/output text, token counts |

The `langsmith` profile enables rich trace content — full prompt text, completion text, tool arguments, and tool results are included on each span.

## Sending test requests

Open `http://localhost:8080` in a browser to use the WebSocket chat UI, or send a request via curl:

```bash
curl -s http://localhost:8080/chatbot
```

The agent pipeline classifies the request (medical / legal / technical) and routes it to the appropriate expert.

## How it works

Both stacks use the same mechanism: `quarkus-opentelemetry` emits spans with `gen_ai.*` semantic attributes for every LLM interaction. The `%langsmith` Quarkus profile simply redirects the OTLP exporter to LangSmith's endpoint and disables the local LGTM containers.

No code changes are needed to switch between stacks — it is purely a configuration concern.
