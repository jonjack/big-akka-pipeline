
# Data Pipeline using Akka Streams


## Background

This project provides a usable example of using [Akka HTTP](https://doc.akka.io/docs/akka-http/current/scala/http/) for streaming requests to the OAM API provided by the British Gas Digital platform. However, with further work it could be much more than this one example.

Even though it has been initially designed to address a a requirement to create a _batch_ of OAM records - and therefore behaves in a batching fashion - it does demonstrate a way to process/transform a possibly unbounded source of data using [Akka Streams](https://doc.akka.io/docs/akka/2.5/scala/stream/index.html). The application follows the specific workflow described below, whereby the data source is provided by an input file. The workflow is a sequence of stages, most notable of which is the consumption of the POST operation on the `/users` endpoint to create OAM records. The output stage (`Sink`) is just a set of logs which record the outcomes of the requests. However, at a higher level, the source could be anything (eg. internal application event, message, inbound HTTP request, queue, another stream), the workflow could do anything (eg. call an local/remote API, just transforms the data), and the output could be anything (eg. maybe we write to a file, database, cache, another application, do nothing).

```
                                   /users
                                  ↑      ↓
                                  ↑      ↓
                                  ↑      ↓
+-----------------+         +-----↑------↓------+         +------------+
|     Source      |-------->|  Connection Flow  |-------->|    Sink    |
|      file       |         |   HttpResponses   |         |   Logging  |
+-----------------+         +-------------------+         +------------+
```

Some of the interesting features of Akka Streams (on which Akka HTTP is built) are:-

- It's API is quite abstract - the programmer builds a pipeline/graph of Sources (input), Flows (processing), and Sinks (outputs) which makes it straightforward to reason about what is going on. Low-level mechanisms such as concurrency management and parallelism are kept well hidden.

- It provides a non-blocking, asynchronous execution environment and has built-in [backpressure](https://www.lightbend.com/blog/understanding-akka-streams-back-pressure-and-asynchronous-architectures) management. For any blocking IO tasks (eg. typically Database queries or remoting) you can configure dedicated connection pools in order to keep the blocking tasks (and the host resources they use) separate from the rest of the application.

- With Akka HTTP, all outbound (client) and inbound (server) connections are managed for you by Akka's connection pools. Marshalling an inbound or outbound request/response from/to an interchange format such as JSON is all managed for you - you just need to provide marshallers for custom types.

- The example provided in this initial project is a relatively straightforwrd linear flow of events, but it could be a complex graph containing multiple sources, forks of transformation tasks, and multiple outputs - all handled in a non-blocking, asynchronous, backpressured fashion.



## Building a release

Prerequisites for building a distribution are:-

- [Java 1.8+](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
- [Scala 2.12.4](https://www.scala-lang.org/download/)
- [SBT 1.0+](http://www.scala-sbt.org/download.html)


To build a distribution for Linux.

```bash
sbt universal:packageZipTarball
```


## Deployment & Permisisons

Move the tar (eg. `bg-cargo-1.0.tgz`) to the target host and unpack it.

Check the permissions of unpacked application. The user you run the application under needs rwx permissions on pretty much everything inside the unpacked directory so you may need to switch to that user and run `chmod -Rf 755 bg-cargo-1.0`


## Configuration

The configuration is located at `src/main/resources/application.conf`

For the OAM Client, the following keys will need checking/amending depending on the execution environment.

| Key   | Description    |
|-------|-----------------------|
| source-dir | The absolute path to the directoty containing the data source file. |
| source-file | The name of the data source file (including extension). |
| api-host | The hostname or IP of the API. Can be local or remote. |
| api-port | The port (this should generally always be `443`). |
| api-path | The endpoint path. |
| throttle-rate | The rate at which HTTP requests should be made against the target host. This value is elements/per sec.
| throttle-burst | This should equal the throttle-rate, unless you know that the target API is capable of handling a higher volume of requests. |
| cid | The Client ID header required by the `/users` API. Will depend on target environment. |
| buk | _Back-End-Users-Key_ - A security header which is specific to the `/users` API. Will depend on target environment. |


#### A note of testing environment certificates

TBC...


## Running





## Logs

To set the log level for all loggers amend the log level in `src/main/resources/application.conf`

Log files are generated on a per run basis with the following format.

`log name - day month year - hour:minutes`

#### Summary of the logs generated by the application.

| Log | Description |
|-----|-------------|
| analytics | Records some very coarse data about a single execution of the application. |
| application | Akka and other third-party library logging. | |
| requests | This logs the raw outbound requests - useful in understanding request failures. |
| success | Successful request data. |
| failure | Failed request data. This includes the scenarios where a responses was received but the status code was not what was expected and also failed requests and responses due to either some local application problem (eg. an outbound record could not be marshalled), or some remote issue (could be network problem or remote application problem). |




