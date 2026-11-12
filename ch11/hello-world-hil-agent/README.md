# Human-in-the-loop greeting

Use the [chapter 11 JDK and dependency baseline](../README.md). This local, single-user server keeps `a2a.authorization.required=false`; use the [replica example](../hello-world-hil-agent-replica/README.md) for authenticated, persistent ownership.

```sh
./mvnw package
java -Dquarkus.http.host=127.0.0.1 -jar target/quarkus-app/quarkus-run.jar
```

In another terminal, from this directory:

```sh
./mvnw dependency:build-classpath -Dmdep.outputFile=target/client-classpath.txt
java -cp "target/classes:$(cat target/client-classpath.txt)" org.acme.A2AClient
```

Choose `upper` or `lower`. `HilSession` observes full task snapshots, task updates and direct message responses. The first network wait ends when input is requested or the task finishes. The second waits for completion after the reply. Both use a 30-second deadline. Failed/canceled tasks and stream errors release the caller; interruption restores the interrupt flag. EOF exits without a reply. The client closes on every exit through the `try` block.

Keyboard input runs on a virtual thread so a terminal stream outcome can release the caller even while the console is waiting. `System.in` stays open; a blocked console read is a daemon task and cannot keep the process alive. The example accepts one human reply. Duplicate input/terminal events cannot produce another reply; a further input request after that reply eventually reaches the final-wait deadline.

```sh
./mvnw test -Dtest=HilSessionTest
```

The focused tests cover input-required/completion, early completion, direct messages, failed/canceled/rejected tasks, stream errors before input and during keyboard input, both deadlines, interruption, duplicate events and EOF. The same coordinator source is used by the replica client. The existing Agent Card check runs with `./mvnw test`.
