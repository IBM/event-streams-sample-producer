# event-streams-sample-producer

A sample workload producer for testing your [IBM Event Streams](https://ibm.github.io/event-streams/) instance.
This producer wraps the Kafka class ProducerPerformance, which is provided within the Kafka Tools jar. It will allow you to produce load to a Kafka cluster by specifying either a size, or setting specific values for throughput and total messages.

## Getting Started

There are two options for running this producer. You can either [download](https://github.com/IBM/event-streams-sample-producer/releases) our pre-built es-producer.jar and run it using the following command:

```
java -jar es-producer.jar
```

Alternatively, you can clone this repository and build the project yourself.

## Prerequisites

- [Apache Maven](https://maven.apache.org/download.cgi) (only if you are building yourself)
- [Java](https://www.java.com/en/download/) (1.8 or later)

## Building

- Clone this project
- Navigate to the root directory of the project and run the following command:

   `mvn install`

- This will create an `es-producer.jar` file inside the `target` directory.

## Producer Configuration

We supply a template configuration file, `producer.config`. You will need to fill this in to get the producer working.

The values enter in the Event Streams UI by selecting the topic to produce to, clicking on **Connect to this topic** and viewing the **Resources** tab on the **Topic connection** panel. The exact requirements depend on the version and configuration of the Event Streams instance. For more detailed instructions, see the [Event Streams documentation](https://ibm.github.io/event-streams/getting-started/testing-loads/).

The following configuration options might be required:

| Attribute                             | Description                                                                                                            |
|  ------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| `bootstrap.servers`                     | The addressed used by the producer application to connect to Event Streams. |
| `ssl.truststore.location`       | The location (path and filename) of the Event Streams certificate. |
| `ssl.truststore.password`       | The password of the Event Streams certificate. |
| `security.protocol`             | The security protocol to use for connections. `SSL` if TLS enabled or `SASL_SSL` for Event Streams 2019.4.2 and earlier.   |
| `sasl.mechanism`              | The SASL mechanism to use for connections. `SCRAM-SHA-512` if using SCRAM credentials or `SASL_PLAIN` for Event Streams 2019.4.2 and earlier.   |
| `sasl.jaas.config`            | The SASL configuration details. `org.apache.kafka.common.security.scram.ScramLoginModule required username="<username>" password="<password>";`, with the `<username>` and `<password>` replaced with the SCRAM credentials if using SCRAM. For Event Streams 2019.4.2 and earlier this should be set to `org.apache.kafka.common.security.plain.PlainLoginModule required username="token" password="<password>";` with the `<password>` replaced by an API key.
| `ssl.keystore.location`       | The location (path and filename) of the `user.p12` keystore file if using TLS credentials.   |
| `ssl.keystore.password`       | The password for the keystore if using TLS credentials.   |

## Running

We offer `-s` as a parameter for quick startup. This will automatically set messages per second and num records. You can view a full list of available parameters below.

To run the sample producer from the root of the project:

```java -jar target/es-producer.jar -t <topic-name> -s <size> -r <record-size> -c <config-file>```

Examples:

```java -jar target/es-producer.jar -t myTopic -s small -r 1024 -c producer.config```

```java -jar target/es-producer.jar -t myTopic -T 1000 -n 60000 -r 1024 -c producer.config```

## Testing

To run tests for this producer:

Run the command: ```mvn test``` in the root of the repository.

## Parameters

| Parameter             | Shorthand | Longhand              | Type     | Description                                                                                                                               | Default          |
| --------------------- | --------- | --------------------- | -------- | ----------------------------------------------------------------------------------------------------------------------------------------- | ---------------- |
| Topic                 | -t        | --topic               | `string` | The name of the topic to produce to                                                                                                       | `loadtest`       |
| Num Records           | -n        | --num-records         | `integer`| The total number of messages to be sent (overrides size)                                                                                  | `60000`          |
| Payload File          | -f        | --payload-file        | `string` | File to read the message payloads from. This works only for UTF-8 encoded text files. Payloads will be read from this  file and a payload will be randomly selected when sending messages. |   |
| Payload Delimiter     | -d        | --payload-delimiter   | `string` | Provides delimiter to be used when --payload-file is provided. Note that this parameter will be ignored if --payload-file is not provided | `\n`             |
| Throughput            | -T        | --throughput          | `integer`| Throttle maximum message throughput to *approximately* *THROUGHPUT* messages per second. -1 means as fast as possible                     | `-1`             |
| Producer Config       | -c        | --producer-config     | `string` | Path to producer configuration file                                                                                                       | `producer.config`|
| Print Metrics         | -m        | --print-metrics       | `boolean`| Whether to print out metrics at the end of the test                                                                                       |                  |
| Num Threads           | -x        | --num-threads         | `integer`| The number of producer threads to run                                                                                                     | `1`              |
| Size                  | -s        | --size                | `string` | Pre-defined combinations of message throughput and volume                                                                                 |                  |
| Record Size           | -r        | --record-size         | `integer`| The size of each message to be sent in bytes                                                                                              | `100`            |
| Help                  | -h        | --help                | `N/A`    | Lists the available parameters                                                                                                            |                  |
| Gen Config            | -g        | --gen-config          | `N/A`    | Generates the configuration file required to run the tool                                                                                 |                  |

**Note:** You must **either** supply `payload-file` **or** `record-size`. You cannot supply both.

### Size Options

These are the predefined sizes that are available for quick use.

| Size   | Messages per Second | Total Messages |
| ------ | ------------------- | -------------- |
| small  | 1000                | 60000          |
| medium | 10000               | 600000         |
| large  | 100000              | 6000000        |

## Environment Overrides for Kubernetes

Setting the following environment variables will override the value used for each parameter. This is useful when the jar is Dockerised and you cannot specify parameters on the command line.

| Parameter             | Environment Variable |
| --------------------- | -------------------- |
| Throughput            | ES_THROUGHPUT        |
| Num Records           | ES_NUM_RECORDS       |
| Size                  | ES_SIZE              |
| Record Size           | ES_RECORD_SIZE       |
| Topic                 | ES_TOPIC             |
| Num threads           | ES_NUM_THREADS       |
| Producer Config       | ES_PRODUCER_CONFIG   |
| Payload File          | ES_PAYLOAD_FILE      |
| Payload Delimiter     | ES_PAYLOAD_DELIMITER |

Note: If size has been set in the arguments of the jar, this can only be overridden if both the `ES_NUM_RECORDS` and `ES_THROUGHPUT` environment variables are set, or if `ES_SIZE` is set.

## Built With

- [ProducerPerformance](https://github.com/apache/kafka/blob/trunk/tools/src/main/java/org/apache/kafka/tools/ProducerPerformance.java) - Kafka tool for stress test producing
- [Maven](https://maven.apache.org/) - Dependency management

## License

[Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)
