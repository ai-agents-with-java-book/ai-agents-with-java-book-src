# Chapter 9: Quarkus tools

This project completes the chapter's dynamic clock provider, tool-error handlers, and city-specific hotel fallback. It uses JDK 21, Maven 3.9.11, Quarkus 3.33.2, Quarkus LangChain4j 1.13.1, and LangChain4j core 1.20.0, matching chapter 4. The Redis client, REST client, and fault-tolerance extension come from that Quarkus platform.

The provider constructs its clock specification and executor once. A request containing `current` offers the clock; other requests offer no tools. Both executor exception flags are enabled so argument and execution failures reach the annotated handlers. Those handlers keep exceptions in application diagnostics and return a fixed explanation with the same diagnostic reference to the model.

## Build and check

The six integration checks use a local WireMock provider and hotel catalog, with an invented model credential. They require an isolated Redis instance but no model account. For example, start Redis bound to loopback with your container engine:

```sh
podman run --rm --name chapter9-redis -p 127.0.0.1:16379:6379 redis:7.4-alpine
```

In another terminal, from this directory:

```sh
REDIS_URL=redis://localhost:16379 ./mvnw package
./mvnw quarkus:dependency-tree
```

The checks delete only the `hotel-suggestions:kyoto`, `hotel-suggestions:berlin`, and `hotel-suggestions:missing` keys in the selected Redis database. Use a disposable Redis instance for them. Stop the container when finished with `podman stop chapter9-redis`.

The checks cover both provider-selection paths, the optional `timeZone` schema, argument and execution errors through the AI Service, city-specific cache results, cache misses, and the original stored timestamp. No test data files or separate test support module are needed.

## Call the clock service

For a live model call, supply `OPENAI_API_KEY` and `OPENAI_MODEL` through your usual environment setup, then run `./mvnw quarkus:dev`. The application binds to `127.0.0.1`.

```sh
curl --fail-with-body http://localhost:8080/tools \
  -H 'Content-Type: text/plain' --data 'What is the current time in Europe/Berlin?'
```

For a compatible local provider, also set `QUARKUS_LANGCHAIN4J_OPENAI_BASE_URL`. Model calls are separate from the stub checks above.

## Hotel fallback

`HotelTools` is the CDI tool bean. Its `HotelController` REST client calls `GET /hotels?location=...` at `HOTEL_CATALOG_URL` (default `http://localhost:8082`) and expects a JSON array such as `[{"name":"Kyoto Inn"}]`. The live path writes that city’s suggestions and retrieval time to Redis at `REDIS_URL` (default `redis://localhost:6379`).

A catalog failure triggers the fault-tolerance fallback. It returns the requested city's entry with `source=cache` and the original `asOf`, or an empty `cache-miss` result with no timestamp. Invalid locations are rejected before the catalog call. The clock service does not register the hotel tool; the integration checks invoke the CDI bean to exercise its actual fallback interceptor and Redis storage.

Suggestions do not establish availability for a booking. Check availability with the booking service before committing a reservation. This example has no reservation implementation or persistent chat memory.
