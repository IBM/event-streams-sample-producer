# event-streams-sample-producer

A sample workload producer for testing your Event Streams instance.
This producer wraps the Kafka class ProducerPerformance, which is provided within the Kafka Tools jar. It will allow you to produce load to a Kafka cluster, by specifying either a size, or setting specific values for throughput and total messages.

## Getting Started

These instructions will get you a copy of this sample producer up and running on your local machine. There are two options for running this producer. You can either [download](https://raw.githubusercontent.com/IBM/event-streams-sample-producer/blob/master/es-producer.jar) and run our pre-built es-producer.jar:

```
java -jar es-producer.jar
```

Alternatively, you can clone this repository and build the project yourself. 

## Prerequisites

- Apache Maven (only if you are building yourself) - available [here](https://maven.apache.org/download.cgi)
- Java (1.8 or later) - available [here](https://www.java.com/en/download/)

## Building

- Clone this project
- Navigate to the root directory of the project and run `mvn install`
- This will create `es-producer.jar` inside of the `target` directory

## Producer Configuration

We supply a template configuration file, `producer.config`. You will need to fill this in to get the producer working. See the table below for details.

| Attribute                             | Description                                                                                                            |
| ------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| bootstrap.servers                     | The URL used for bootstrapping knowledge about the rest of the cluster. This address can be found in the 'Connection information' page for your topic in the Event Streams UI. |                                              |
| ssl.truststore.location               | The location of the JKS keystore used to securley communicate with your Event Streams instance. This can be downloaded from the 'Connection information' page for your topic in the Event Streams UI.       |
| sasl.jaas.config                      | SASL config options, an API key which authorizes production to your topic must be added to the 'password' string. API keys can be set up via the 'Connection information' page for your topic in the Event Streams UI. We recommend naming this key something memorable for future reference.      |

## Running

We offer `-s` as a parameter for quick startup. This will automatically set messages per second and num records. You can view a full list of available parameters below.

To run the sample producer from the root of the project:

```java -jar target/es-producer.jar -t <TOPIC> -s <SIZE> -r <RECORD_SIZE> -c <CONFIG_FILE>```

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
| Num Records           | -n        | --num-records         | `int`    | The total number of messages to be sent (overrides size)                                                                                  | `60000`          |
| Payload File          | -f        | --payload-file        | `string` | File to read the message payloads from. This works only for UTF-8 encoded text files. Payloads will be read from this  file and a payload will be randomly selected when sending messages. |   | 
| Payload Delimiter     | -d        | --payload-delimiter   | `string` | Provides delimiter to be used when --payload-file is provided. Note that this parameter will be ignored if --payload-file is not provided | `\n`             |
| Throughput            | -T        | --throughput          | `int`    | Throttle maximum message throughput to *approximately* *THROUGHPUT* messages per second. -1 means as fast as possible                     | `-1`             |
| Producer Config       | -c        | --producer-config     | `string` | Path to producer configuration file                                                                                                       | `producer.config`|
| Print Metrics         | -m        | --print-metrics       | `bool`   | Whether to print out metrics at the end of the test                                                                                       |                  |
| Num Threads           | -x        | --num-threads         | `int`    | The number of producer threads to run                                                                                                     | `1`              |
| Size                  | -s        | --size                | `string` | Pre-defined combinations of message throughput and volume                                                                                 |                  |
| Record Size           | -r        | --record-size         | `int`    | The size of each message to be sent in bytes                                                                                              | `100`            |
| Help                  | -h        | --help                | `N/A`    | Lists the available parameters                                                                                                            |                  |
| Gen Config            | -g        | --gen-config          | `N/A`    | Generates the configuration file required to run the tool                                                                                 |                  |


### Note

You must **either** supply payload-file **or** record-size. You cannot supply both.

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