Feature: Arguments are correctly interpreted

    Scenario Outline: Provided parameters are correctly interpreted
        Given an instance of the es-producer
        When I provide argument "<argument>" of "<value>"
        And I check the arguments that the Kafka performance script is called with
        Then it was called with "<producerArgument>" of "<value>"

        Examples:
            | argument          | value         | producerArgument  |
            | --topic           | topic         | --topic           |
            | --payload-file    | file.txt      | --payload-file    |
            | --producer-config | config.config | --producer.config |
            | --record-size     | 1             | --record-size     |
            | -t                | topic         | --topic           |
            | -f                | file,txt      | --payload-file    |
            | -c                | config.config | --producer.config |
            | -r                | 1             | --record-size     |

    Scenario Outline: Print metrics is correctly interpreted
         Given an instance of the es-producer
         When I provide the print metrics argument "<argument>"
         And I check the arguments that the Kafka performance script is called with
         Then it was called with the print metrics argument

         Examples:
            | argument        |
            | --print-metrics |
            | -m              |

    Scenario: Payload delimiter is correctly interpreted:
        Given an instance of the es-producer
        When I provide argument "--payload-file" of "file.txt"
        When I provide argument "--payload-delimiter" of ";"
        And I check the arguments that the Kafka performance script is called with
        Then it was called with "--payload-file" of "file.txt"
        And it was called with "--payload-delimiter" of ";"

    Scenario Outline: Size is correctly translated to number of records and throughput
        Given an instance of the es-producer
        When I provide argument "--size" of "<size>"
        And I check the arguments that the Kafka performance script is called with
        Then it was called with "--num-records" of "<records>"
        And it was called with "--throughput" of "<throughput>"

        Examples:
            | size   | records | throughput |
            | small  | 60000   | 1000       |
            | medium | 600000  | 10000      |
            | large  | 6000000 | 100000     |

    Scenario: Size is not overridden by number of records and throughput
         Given an instance of the es-producer
         When I provide argument "--size" of "small"
         When I provide argument "--num-records" of "1"
         When I provide argument "--throughput" of "1"
         And I check the arguments that the Kafka performance script is called with
         Then it was called with "--num-records" of "60000"
         And it was called with "--throughput" of "1000"

    Scenario: If size is not provided but number of records and throughput are, then they are used
        Given an instance of the es-producer
        When I provide argument "--num-records" of "1"
        When I provide argument "--throughput" of "1"
        And I check the arguments that the Kafka performance script is called with
        Then it was called with "--num-records" of "1"
        And it was called with "--throughput" of "1"

    Scenario: If --num-threads is specified, that many threads are spawned, and workload is split between them
         Given an instance of the es-producer
         When I provide argument "--num-threads" of "2"
         When I provide argument "--size" of "small"
         And I check the arguments that the Kafka performance script is called with
         Then it was called "2" times
         Then it was called with "--num-records" of "30000"
         And it was called with "--throughput" of "1000"