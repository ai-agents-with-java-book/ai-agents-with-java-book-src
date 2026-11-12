# Chapter 9: Current-time tool

Requires JDK 21 or newer. This example pins LangChain4j 1.20.0 so that
`@P(name = "timeZone", required = false)` gives the optional argument the same
JSON name with or without Java's `-parameters` compiler flag. Both reflective
execution and the low-level JSON parser use `timeZone`.

Run the regression tests from this directory:

```shell
./mvnw clean test -Dmaven.compiler.parameters=false
./mvnw clean test -Dmaven.compiler.parameters=true
```

The two clean builds exercise both compiler modes. The tests check the generated
schema, argument delivery through `DefaultToolExecutor`, and the actual low-level
request/result flow using a stub chat model. They cover supplied and omitted time-zone
arguments, multiple calls in one assistant turn, continued calls, unknown tools, the four-turn limit, and the clock's ISO date/time and time-zone behavior. The error-builder checks use a scripted model to exercise unknown-tool, malformed-argument, and execution-failure paths. They verify that model-facing results omit exception details and that a diagnostic reference matches the application log. No API key or running
model is required; Maven may need to download dependencies on the first run.

The runnable main classes still contact the configured model endpoint. The clock
uses the supplied time-zone ID (for example, `Europe/Berlin`) and the system time
zone when it is omitted. `ZoneId.of` rejects invalid IDs. The tool reads public
time information without looking up a user profile.

`LowLevelToolMainApp.run` keeps the complete conversation and returns the final answer. It appends every result in an assistant turn before asking the model to continue. Four tool turns without a final answer produce an application-side limit error.

`ToolErrorExamples` contains the three separate synchronous builders printed in the chapter. Each shows one error policy. Configure both argument and execution handlers when a service needs both. `ToolFailures` logs the original exception under a generated reference and returns a fixed explanation to the model.
