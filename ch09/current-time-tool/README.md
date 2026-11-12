# Chapter 9: Current-time tool

Requires JDK 21 or newer. This example pins LangChain4j 1.20.0 so that
`@P(name = "userId", required = false)` gives the optional argument the same
JSON name with or without Java's `-parameters` compiler flag. Both reflective
execution and the low-level JSON parser use `userId`.

Run the regression tests from this directory:

```shell
./mvnw clean test -Dmaven.compiler.parameters=false
./mvnw clean test -Dmaven.compiler.parameters=true
```

The two clean builds exercise both compiler modes. The tests check the generated
schema, argument delivery through `DefaultToolExecutor`, and the actual low-level
request/result flow using a stub chat model. They cover supplied and omitted user
IDs, plus the clock's ISO date/time and time-zone behavior. No API key or running
model is required; Maven may need to download dependencies on the first run.

The runnable main classes still contact the configured model endpoint. The clock
uses CET for any supplied user ID and the system time zone when it is omitted;
the user lookup is deliberately simplified for the example.
