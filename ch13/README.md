# Chapter 13: Security and Compliance

Small runnable Java examples for the chapter. They demonstrate individual controls rather than a complete agent application. There are no fixtures, test harnesses, model stubs, or live business-service integrations.

## Build

Use JDK 21 or later and Maven. Run the commands below from this directory.

```shell
java -version
mvn --version
mvn compile
```

The project targets Java 21. Only JSON serialization needs an external library: Jackson Databind 2.22.2. No model credentials are needed.

## Action policy

```shell
java -cp target/classes org.acme.security.ActionPolicyExample
```

Expected output:

```text
DRAFT_REPLY: ALLOW / DRAFT_ONLY
REQUEST_REPLACEMENT: REQUIRE_APPROVAL / REVIEW_REPLACEMENT
EXPORT_CASES: DENY / OPERATION_NOT_GRANTED
OTHER_CASE: CASE_ACCESS_DENIED
```

`Context` and `CaseRecord` represent trusted inputs. A real request handler must resolve them from authenticated access and current business state. `Proposal` contains the requested operation. The program evaluates policy; it does not send a reply or create a replacement.

## Approval matching and consumption

```shell
java -cp target/classes org.acme.security.ApprovalExample valid
java -cp target/classes org.acme.security.ApprovalExample changed
java -cp target/classes org.acme.security.ApprovalExample replay
```

| Argument | Expected output |
| --- | --- |
| `valid` | `APPROVED` |
| `changed` | `MISMATCH` |
| `expired` | `EXPIRED` |
| `replay` | `APPROVED`, then `UNKNOWN_OR_USED` |
| `revoked` | `POLICY_DENIED` |
| `missing` | `UNKNOWN_OR_USED` |
| `stale` | `MISMATCH` |

The approval is bound to the caller, tenant, complete typed proposal, case version, policy version, and expiry. The example assumes a trusted approval handler already checked the reviewer's authority. It creates the record directly in `main` so you can run the validator independently.

The map only demonstrates consumption within one process. It is not durable storage, an approval endpoint, or a transaction with a business service. A real service must coordinate its accepted command, consumed approval, idempotency record, and case-version check. An expired or unknown approval cannot be restored by the model.

## Context selection

```shell
java -cp target/classes org.acme.security.ContextSelectionExample current
java -cp target/classes org.acme.security.ContextSelectionExample stale
```

| Argument | Expected output |
| --- | --- |
| `current` | The order identifier, promised date, and delivery status below |
| `other-tenant` | `REJECTED: CASE_ACCESS_DENIED` |
| `expired` | `REJECTED: SOURCE_NOT_ELIGIBLE` |
| `stale` | `REJECTED: SOURCE_NOT_ELIGIBLE` |
| `withdrawn` | `REJECTED: SOURCE_NOT_ELIGIBLE` |

```text
Order: ORD-1042
Promised date: 2026-09-08
Delivery status: Carrier check pending
```

The stored email and internal note are excluded from the projection. `usableUntil` is a context-use deadline, not a legal retention period. This example has no database or vector-store dependency; integrate the check after an authorized lookup and before assembling model context.

## Structured security events

Use Maven for these commands so Jackson is on the runtime classpath:

```shell
mvn -q exec:java -Dexec.mainClass=org.acme.security.AuditEventExample -Dexec.args=denied
mvn -q exec:java -Dexec.mainClass=org.acme.security.AuditEventExample -Dexec.args=completed
```

`denied` prints one JSON object with `AUTHORIZATION / DENIED`. `completed` prints two objects sharing an operation identifier: `AUTHORIZATION / APPROVED` and `EXECUTION_RESULT / SUCCEEDED`. Identifiers and timestamps vary on each run.

The completed records are illustrative; no shipment service is called. The output omits prompts, credentials, email addresses, and free-text reasons. References can still be personal data. Standard output is not a durable, tamper-evident, or transactional audit sink.

## Isolated Java execution

This example needs a running Linux container engine, a Unix-style host, and POSIX file permissions. Docker is the default CLI; Podman is selectable. Only use non-sensitive source with this local demonstration: its staged input must be readable by the unprivileged worker and is temporarily readable by other local users.

Prepare a JDK image separately. These commands use Podman:

```shell
podman pull docker.io/library/eclipse-temurin:21-jdk
CH13_JDK_IMAGE=$(podman image inspect \
  docker.io/library/eclipse-temurin:21-jdk \
  --format '{{index .RepoDigests 0}}')
```

For Docker, substitute `docker` in the preparation commands and pass `-Dsandbox.runtime=docker` below. The Java launcher requires an image digest and uses `--pull=never`. Review image updates through your normal process.

```shell
java -Dsandbox.runtime=podman -Dsandbox.image="$CH13_JDK_IMAGE" \
  -cp target/classes org.acme.security.IsolatedJavaExample \
  src/main/java/org/acme/security/SandboxProgram.java calculate
```

Run `SandboxProgram` through `IsolatedJavaExample`, not directly on your host. It supplies a few small Java operations to make the worker restrictions visible:

| Final argument | Expected observation |
| --- | --- |
| `calculate` | `EXITED_ZERO`, `sum=42` in worker output |
| `write` | Nonzero exit and a read-only-filesystem error for `/input/unexpected.txt` |
| `network` | Nonzero exit and a network-unreachable error on the verified runtime |
| `secret` | `hostSentinelVisible=false` |
| `timeout` | `TIMED_OUT`, followed by container removal |
| `output` | `outputTruncated=true`; capture is limited to 8 KiB |

The network operation uses the documentation-only address `192.0.2.1`. A failed connection alone would not prove network isolation; inspect the `--network=none` configuration as well.

To check environment isolation, set only this invented sentinel on the host:

```shell
CH13_HOST_ONLY_SENTINEL=example-not-a-secret \
java -Dsandbox.runtime=podman -Dsandbox.image="$CH13_JDK_IMAGE" \
  -cp target/classes org.acme.security.IsolatedJavaExample \
  src/main/java/org/acme/security/SandboxProgram.java secret
```

To shorten the execution allowance:

```shell
java -Dsandbox.runtime=podman -Dsandbox.image="$CH13_JDK_IMAGE" \
  -Dsandbox.seconds=3 \
  -cp target/classes org.acme.security.IsolatedJavaExample \
  src/main/java/org/acme/security/SandboxProgram.java timeout
```

The execution allowance defaults to 10 seconds and may be set from 1 to 60 seconds. Creation and removal each have a separate 20-second control timeout, with bounded local process/output cleanup afterward. The execution deadline is not an end-to-end command deadline.

### What the launcher does

- Stages at most 64 KiB of source, computes its SHA-256, and compiles/runs it inside the JDK container using source-file mode.
- Uses a read-only input mount and root filesystem, a non-root UID, no external network, dropped capabilities, and no privilege escalation.
- Limits CPU, memory, process count, temporary space, shared memory, and retained output. The heap limit is smaller than the container memory limit.
- Disables persistent container logging, drains attached output, and escapes terminal control characters before displaying it.
- Force-removes the named container after execution, including an execution timeout; reports unconfirmed cleanup as an error.

The launcher is trusted local code with container-engine access. The candidate does not receive that access or the host environment. Runtime/image configuration is operator input, never model input. Runtime-managed special filesystems remain; this is not a claim that `/tmp` is the container's only writable path.

This is an execution example, not Chapter 6's acceptance verifier. A zero process exit, printed message, or matching source hash cannot establish that a repair passes trusted acceptance checks. A service also needs independent abandoned-worker cleanup for crashes, interrupted creation, or lost engine connectivity. Containers share a kernel; hostile multi-tenant execution may require a stronger worker boundary.

### Verification record

The documented policy, approval, context-selection, JSON, and six container scenarios were run on 2026-09-11. Host: macOS arm64, Temurin JDK 21.0.8, Maven 3.9.16. Container runtime: Podman client 5.8.2, Linux arm64 server 5.8.3. The resolved image was:

```text
docker.io/library/eclipse-temurin@sha256:1f79c73404fb0cccf9a3459eda22892f368d994b1028d6fb1ae871c1f49749a6
```

All documented outcomes were observed. Docker execution and other architectures were not exercised. These runs establish the demonstrated local behavior, not production integration or resistance to every possible hostile program.
