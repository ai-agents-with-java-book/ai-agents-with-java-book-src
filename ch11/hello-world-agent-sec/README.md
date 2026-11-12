# Chapter 11: Authenticated task ownership

`CustomTaskAuthorizationProvider` requires an authenticated SDK `User` with a
nonblank user ID before allowing task creation or recording ownership. Reads and
writes additionally require the caller to own the task. In particular, the SDK's
`UnauthenticatedUser` must not become an owner through its empty username.

With JDK 25, run the focused regression suite from this directory:

```shell
./mvnw test -Dtest=CustomTaskAuthorizationProviderTest
```

These are plain JUnit tests of the authorization provider: no running Quarkus
server, Keycloak, Docker, or model endpoint is needed. They cover missing and
anonymous users, blank IDs, owner/non-owner access, unrecorded tasks, and ownership
that cannot be reassigned by a later request. They do not test token validation
or the HTTP authentication integration. Maven may download dependencies on the
first run.

# hello-world-agent

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/hello-world-agent-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
