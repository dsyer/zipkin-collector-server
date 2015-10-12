# Running Zipkin on Cloud Foundry

We have a running instance on PWS: http://zipkin-web.cfapps.io. It is backed by a `zipkin-java-server` with a MySQL backend and RabbitMQ (Spring Cloud Stream) for span transport.

## Instrumenting Apps

Depend on [Spring Cloud Sleuth Stream](https://github.com/spring-cloud-spring-cloud-sleuth). Bind to a rabbit service (or redis if you prefer - normal Spring Cloud Stream process).

## Collector and Query Server

Code is [here](https://github.com/dsyer/zipkin-collector-server).

Bind to MySQL and the same Stream service that you did in the apps (rabbit, redis, kafka). Set `spring.datasource.initialize=true` the first time you start to initialize the database. Alternatively, curl it:

```
$ curl  zipkin-server.cfapps.io/env -d endpoints.restart.enabled=true
$ curl  zipkin-server.cfapps.io/env -d spring.datasource.initialize=true
$ curl  zipkin-server.cfapps.io/restart -d {}
```

Uses the `zipkin-server` jar from the [OSS](https://github.com/openzipkin/zipkin-java) as well as `spring-cloud-sleuth-stream`.

> NOTE: running locally you don't need MySQL (the default span store is in memory). You could even run in PWS without MySQL just by removing the `zipkin.store.type` setting.

## Web UI

Get the jar from the [OSS](https://github.com/openzipkin/zipkin) and push it:

```
$ cf push zipkin-web -p zipkin-web/build/libs/zipkin-web*all.jar
```

It needs an environment variable to set the command line args:

```
$ cf set-env zipkin-web JBP_CONFIG_JAVA_MAIN '{arguments: "-zipkin.web.port=:\$PORT -zipkin.web.rootUrl=/ -zipkin.web.query.dest=zipkin-server.cfapps.io:80 -zipkin.web.resourcesRoot=."}'
```

NOTE: `JBP_CONFIG_JAVA_MAIN` only works with Java buildpack v3.2 and above (so not in PEZ Heritage right now).
