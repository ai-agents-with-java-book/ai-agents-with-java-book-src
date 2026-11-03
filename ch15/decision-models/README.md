# Chapter 15: Decision Models and Decision Agents

Examples for the chapter, all on the same support desk: tickets are classified and routed to teams, replies are graded, urgent tickets are handed over to a human, and a money transfer is gated by a decision model before it is executed.

## Build

Use JDK 21 or later and Maven. Run the commands below from this directory.

```shell
mvn test
```

The tests run offline against a stub decision model and need no credentials. The project depends on LangChain4j 1.23.0 (`langchain4j-agentic` and `langchain4j-agentic-patterns` 1.23.0-beta33) and sets the `-parameters` compiler flag, which Decision Services need to read the names of the method parameters.

Until the 1.23.0 release is available on Maven Central, build LangChain4j from source with `mvn install -DskipTests` and run against the snapshots:

```shell
mvn -Dlangchain4j.version=1.23.0-SNAPSHOT -Dlangchain4j.beta.version=1.23.0-beta33-SNAPSHOT test
```

## Run against a real model

The `main` classes use the OpenAI decision model `gpt-6-luna` and the chat models configured in `Models`:

```shell
export OPENAI_API_KEY=sk-...
mvn -q compile exec:java -Dexec.mainClass=ch15.SupportDeskMain
```

| Main class | What it runs |
| --- | --- |
| `ch15.SupportDeskMain` | The raw decision API and the `SupportDesk` decision service: spam, team, frustration, triage and response metadata |
| `ch15.agents.DecisionAgentsMain` | The escalation gate in a conditional workflow, the quality loop and the transaction gate with its compliance approval |
| `ch15.router.RouterMain` | The `DecisionRouterPlanner` routing a ticket to one team, then to every team concerned followed by a synthesizer |
| `ch15.cascade.CascadeMain` | The `ConfidenceCascadePlanner` escalating from a small model to a large one and then to a human |

A workflow that hands over to a human suspends: the gate and transfer examples print which response they wait for, and the cascade completes the pending response with a canned human answer. `Models.typeSafeDecisionModel()` needs `TYPESAFE_API_KEY` and `Models.ollamaDecisionModel()` a local Ollama with the `nimble` model; they appear in the chapter's comparison of providers and are not used by the `main` classes.

## Where the listings are

| Chapter section | Classes |
| --- | --- |
| The Decision Model API | `RawDecisionApi`, `Models` |
| Decision Services | `SupportDesk`, `SupportDeskExamples`, `DamageInspector`, `Team`, `Frustration`, `Quality` |
| Decision Agents | `agents`: `EscalationGate`, `TicketTriage`, `SupportAssistant`, `ReplyGrader`, `ReplyEditor`, `TransactionGate`, `MoneyMover`, `WriteupScorer`, `DecisionAuditListener`, `DecisionAgentExamples` |
| Declarative decision agents | `declarative`: `TransactionGate` with `@DecisionModelSupplier` and `@DecisionThresholdSupplier`, `MoneyMover`, `TransferWorkflow` |
| The Decision Router Pattern | `router`: `BillingExpert`, `TechnicalExpert`, `AccountExpert`, `SupportRouter`, `MultiSupportRouter`, `AnswerSynthesizer`, `SupportPipeline`, `DeclarativeSupportRouter`, `HiringRouter`, `RouterExamples` |
| The Confidence Cascade Pattern | `cascade`: `FaqAgent`, `ExpertAgent`, `SupportCascade`, `CascadeExamples` |
| Testing Decisions | `src/test`: `StubDecisionModel`, `SupportDeskTest`, `EscalationGateTest`, `TransactionGateTest` |

`TriageAndLoopVerificationTest` and `TransferWorkflowTest` are not in the chapter; they pin the behavior of the triage record, the quality loop and the transfer workflow. The Quarkus listings of the chapter, `SupportDeskProducer` and `ClassifierAgent`, are not here because they need the Quarkus LangChain4j release aligned with LangChain4j 1.23.0.
