
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


To build a distribution for Linux run the following SBT command whilst located in the root of the application.

```bash
sbt universal:packageZipTarball
```

The package will be located in the following path of the source project.

```scala
target/universal/bg-cargo-[RELEASE].tgz
```


## Deployment & Permisisons

Move the release file - eg. `bg-cargo-1.0.tgz` - to the target host and unpack it.

```scala
tar -xzvf bg-cargo-0.3.tgz
```

The layout of the application.

```scala
  |
  |--bin    // startup scripts
  |   
  |--lib    // compiled app + thrid-party dependencies
  |   |
  |  bg-cago-1.0.jar  - the application is packaged within this jar
  |  ... 
  |  ...
  |
```

Check the permissions of the unpacked application directories and files. The user you run the application under needs rwx permissions on pretty much everything inside the unpacked directory so you may need to switch to that user and run 

```scala
chmod -Rf 755 bg-cargo-1.0
```


## Configuration

The default configuration is located at `src/main/resources/application.conf` and is packaged within the application Jar file when building/deploying a binary distribution. ie. you cannot conveniently amend it. The default configuration can easily be complemented, and overridden, however, by specifying an additional configuration source as a system property at runtime. 


#### Sensitive configuration properties

For sensitive configuration properties - such as production environment security headers - you must use an additional configuration source as instructed below. You should not check such properties into this source code repository otherwise you will risk compromising the security of the API platform.


#### Using an additional configuration source

To specify a configuration source (to add to, and override, the defaults) when running the application, specify it as a system property as follows.

```bash
bin/bg-cargo -Dconfig.file=./configuration.conf
```

An example configuration file for the OAM client configuration is provided in the root of the project (`configuration.conf`) and looks like this.

```scala
include "application"

# Override default OAM Client Configuration
# -----------------------------------------

# source-dir=/path/to/source/directory
# source-file=datasource.txt

# api-host=digital1.3utilities.com
# api-port=443

# cid=4d85090c-8b6d-4cba-9327-c024643be989
# buk=ABC

# throttle-elements=1
# throttle-per=1
# throttle-burst=1
```

Note that the `include "application"` statement imports the default configuration packaged within the application Jar, and is necessary if you are only overriding a subset of properties (which will always likely be the case). You need to either copy this example configuration file to your remote host or create your own.

For the OAM Client, the following keys will need checking/amending depending on the execution environment.

| Key   | Description    |
|-------|-----------------------|
| source-dir | The absolute path to the directoty containing the data source file. |
| source-file | The name of the data source file (including extension). |
| api-host | The hostname or IP of the API. Can be local or remote. |
| api-port | The port (this should generally always be `443`). |
| api-path | The endpoint path. |
| throttle-elements | The number of elements that can enter the throttle's token bucket per `throttle-per`. |
| throttle-per | The timeframe in which the number of `elements` can enter the throttle's token bucket. |
| throttle-burst | This should equal the throttle-rate, unless you know that the target API is capable of handling a higher volume of requests. |
| cid | The Client ID header required by the `/users` API. Will depend on target environment. |
| buk | _Back-End-Users-Key_ - A security header which is specific to the `/users` API. Will depend on target environment. |


#### A note on Throttling

The flow incorporates a throttle to control the rate at which the stream of elements is emitted through the flow, otherwise we will overwhelm the API with too many requests which will likely result in failures. The `throttle` provided by Akka streams is highly configurable.

```scala
throttle(elements: Int, per: FiniteDuration, maximumBurst: Int, mode: ThrottleMode)
```

- `elements` - The number of elements that can enter the throttle's token bucket per `FiniteDuration`.  
- `per` - The timeframe in which the number of `elements` can enter the throttle's token bucket.
- `burst` - The number of elements that can be emmitted downstream in a _burst_. If you do not want elements emiiteed downstream faster than `elements / sec` then set `burst` to be the same as `elements`.
- `mode` - Manages behaviour when upstream is faster than throttle rate. This should be "shaping" since this does not throw exceptions in the event of backpressure. This is not configurable.

The default configuration (below) can be overridden at runtime using external configuration (see [Running](#running) below).

```scala
throttle-elements=1
throttle-per=1
throttle-burst=1
```

## Running

An unpacked distribution will include a startup script which sets the classpath to the dependant libraries and invokes the entrypoint to the application. Running the application is as straightforward as invoking this startup script.

```bash

bin/bg-cargo

// or if using an external configuration file to override some default configuration values
bin/bg-cargo -Dconfig.file=/path/to/external/configuration/file
```


## Logs

To set the log level for all loggers amend the log level in `src/main/resources/application.conf`

```scala
akka {
  loglevel = "DEBUG"
  stdout-loglevel = "DEBUG"
}
```

Log files are generated on a per run basis with the following format.

`log name - day month year - hour:minutes`


#### Summary of the logs generated by the application.

| Log | Description |
|-----|-------------|
| application | Akka and other third-party library logging. |
| analytics | Records some very coarse data about a single execution of the application. |
| requests | Logs the raw outbound requests - useful in understanding request failures. |
| successes | Logs Responses where the status was what was expected eg. `201 Created` for a successful POST. |
| failures | Logs Responses where the status was not what was expected eg. `409 Conflict` or `500 Error`. |
| exceptions | This includes:- marshalling exceptions, network exceptions (no response), etc. |


