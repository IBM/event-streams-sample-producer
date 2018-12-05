/*
 * Copyright 2018 IBM Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.es.producer;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import net.sourceforge.argparse4j.*;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentGroup;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.common.utils.Exit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Producer {

  private static Logger logger = LoggerFactory.getLogger(Producer.class);
  private static ResourceBundle producerTranslations =
      ResourceBundle.getBundle("MessageBundle", Locale.getDefault());
  private static ResourceBundle configTranslations =
      ResourceBundle.getBundle("ConfigBundle", Locale.getDefault());

  // default values used by the tool
  private static final String DEFAULT_PRODUCER_CONFIG = "producer.config";
  private static final String DEFAULT_SIZE = "";
  private static final Long DEFAULT_NUM_RECORDS = 60000L;
  private static final Integer DEFAULT_THROUGHPUT = -1;
  private static final String DEFAULT_PAYLOAD_DELIMITER = "\\n";
  private static final Boolean DEFAULT_PRINT_METRICS = false;
  private static final Integer DEFAULT_NUMBER_THREADS = 1;
  private static final Integer DEFAULT_RECORD_SIZE = 100;
  private static final String DEFAULT_PAYLOAD_FILE = "";

  private String size;
  private String topic;
  private Integer numThreads;
  private Long numRecords;
  private Integer throughput;
  private String configFilePath;
  private Integer recordSize;
  private String payloadFilePath;
  private Boolean shouldPrintMetrics;
  private String payloadDelimiter;

  public static void main(String[] args) {
    Producer producer = new Producer();

    ArgumentParser parser = argParser();

    try {
      Namespace res = parser.parseArgs(args);

      producer.setTopic(res.getString("topic"));
      producer.setNumRecords(res.getLong("numRecords"));
      producer.setRecordSize(res.getInt("recordSize"));
      producer.setThroughput(res.getInt("throughput"));
      producer.setConfigFilePath(res.getString("producerConfigFile"));
      producer.setPayloadFilePath(res.getString("payloadFile"));
      producer.setShouldPrintMetrics(res.getBoolean("printMetrics"));
      producer.setNumThreads(res.getInt("numThreads"));
      producer.setSize(res.getString("size"));

      producer.setPayloadDelimiter(
          res.getString("payloadDelimiter").equals("\\n")
              ? "\n"
              : res.getString("payloadDelimiter"));

      overrideArgumentsWithEnvVars(producer);

      // if generate config, load the template, and insert translated strings
      if (res.getBoolean("genConfig")) {
        try {
          StringBuilder template =
              new StringBuilder(IOUtils.resourceToString("/producer.config.template", null));
          Enumeration<String> configKeys = configTranslations.getKeys();

          while (configKeys.hasMoreElements()) {
            String key = configKeys.nextElement();
            String translatedText = configTranslations.getString(key);
            int startOfInsert = template.indexOf(key);
            int endOfInsert = startOfInsert + key.length();
            template.replace(startOfInsert, endOfInsert, translatedText);
          }

          FileUtils.writeStringToFile(new File("producer.config"), template.toString(), "UTF-8");
          System.out.println(producerTranslations.getString("producer.fileGenerated"));
        } catch (IOException exception) {
          System.err.println(producerTranslations.getString("producer.fileGenerationFail"));
          logger.error(producerTranslations.getString("producer.envar.warning"), exception);
        }

      } else {
        // if one of the required arguments is missing, log and exit.
        if (producer.getTopic() == null
            || producer.getConfigFilePath() == null
            || (producer.getRecordSize() == null && producer.getPayloadFilePath() == null)) {
          System.out.println(producerTranslations.getString("producer.argsMissing"));
          parser.printHelp();
          Exit.exit(0);
          // check the inputs for the fields we can - if invalid log and exit
        } else if (producer.numThreads < 1) {
          System.out.println(producerTranslations.getString("producer.invalidThreads"));
          parser.printHelp();
          Exit.exit(0);
        } else if (producer.throughput == 0 || producer.throughput < -1) {
          System.out.println(producerTranslations.getString("producer.invalidThroughput"));
          parser.printHelp();
          Exit.exit(0);
        } else {
          ThreadGroup producers = new ThreadGroup("Producers");
          for (int i = 0; i < producer.getNumThreads(); i++) {
            // if size set, determine the values to use
            if (!producer.getSize().isEmpty()) {
              switch (producer.getSize()) {
                case "small":
                  producer.setNumRecords(60000L);
                  producer.setThroughput(1000);
                  break;
                case "medium":
                  producer.setNumRecords(600000L);
                  producer.setThroughput(10000);
                  break;
                case "large":
                  producer.setNumRecords(6000000L);
                  producer.setThroughput(100000);
                  break;
              }
            }
            // if we have a number of threads to run over, divide the work between them
            producer.numRecords = producer.numRecords / producer.numThreads;

            ProducerThread producerThread =
                new ProducerThread(producers, String.format("producer%d", i), producer);
            producerThread.start();
          }
        }
      }
    } catch (ArgumentParserException error) {
      if (args.length == 0) {
        parser.printHelp();
        Exit.exit(0);
      } else {
        parser.handleError(error);
        Exit.exit(1);
      }
    }
  }

  private static ArgumentParser argParser() {
    ArgumentParser parser =
        ArgumentParsers.newFor("es-producer")
            .singleMetavar(true)
            .build()
            .defaultHelp(true)
            .description(producerTranslations.getString("producer.summary"));

    ArgumentGroup propertiesFileSection =
        parser.addArgumentGroup(producerTranslations.getString("producer.configSection"));

    propertiesFileSection
        .addArgument("-g", "--gen-config")
        .action(Arguments.storeTrue())
        .required(false)
        .type(Arguments.booleanType())
        .dest("genConfig")
        .help(producerTranslations.getString("producer.genConfig.help"));

    ArgumentGroup requiredParams =
        parser.addArgumentGroup(producerTranslations.getString("producer.requiredConfigSection"));

    requiredParams
        .addArgument("-t", "--topic")
        .action(Arguments.store())
        .required(false)
        .type(String.class)
        .metavar("TOPIC")
        .help(producerTranslations.getString("producer.topic.help"));

    requiredParams
        .addArgument("-c", "--producer-config")
        .action(Arguments.store())
        .required(false)
        .type(String.class)
        .metavar("CONFIG-FILE")
        .dest("producerConfigFile")
        .setDefault(DEFAULT_PRODUCER_CONFIG)
        .help(producerTranslations.getString("producer.producerConfigFile.help"));

    ArgumentGroup generalConfig =
        parser.addArgumentGroup(producerTranslations.getString("producer.generalConfigSection"));

    generalConfig
        .addArgument("-s", "--size")
        .action(Arguments.store())
        .required(false)
        .type(String.class)
        .metavar("SIZE")
        .dest("size")
        .choices("small", "medium", "large")
        .setDefault(DEFAULT_SIZE)
        .help(producerTranslations.getString("producer.size.help"));

    generalConfig
        .addArgument("-n", "--num-records")
        .action(Arguments.store())
        .required(false)
        .type(Long.class)
        .setDefault(DEFAULT_NUM_RECORDS)
        .metavar("NUM-RECORDS")
        .dest("numRecords")
        .help(producerTranslations.getString("producer.numrecords.help"));

    generalConfig
        .addArgument("-T", "--throughput")
        .action(Arguments.store())
        .required(false)
        .type(Integer.class)
        .metavar("THROUGHPUT")
        .setDefault(DEFAULT_THROUGHPUT)
        .help(producerTranslations.getString("producer.throughput.help"));

    generalConfig
        .addArgument("-d", "--payload-delimiter")
        .action(Arguments.store())
        .required(false)
        .type(String.class)
        .metavar("PAYLOAD-DELIMITER")
        .dest("payloadDelimiter")
        .setDefault(DEFAULT_PAYLOAD_DELIMITER)
        .help(producerTranslations.getString("producer.payloadDelimeter.help"));

    generalConfig
        .addArgument("-m", "--print-metrics")
        .action(Arguments.storeTrue())
        .required(false)
        .type(Arguments.booleanType())
        .dest("printMetrics")
        .setDefault(DEFAULT_PRINT_METRICS)
        .help(producerTranslations.getString("producer.printMetrics.help"));

    generalConfig
        .addArgument("-x", "--num-threads")
        .action(Arguments.store())
        .required(false)
        .type(Integer.class)
        .metavar("NUM_THREADS")
        .dest("numThreads")
        .setDefault(DEFAULT_NUMBER_THREADS)
        .help(producerTranslations.getString("producer.numThreads.help"));

    MutuallyExclusiveGroup payloadOptions =
        parser
            .addMutuallyExclusiveGroup()
            .description(producerTranslations.getString("producer.payload.options"));

    payloadOptions
        .addArgument("-r", "--record-size")
        .action(Arguments.store())
        .required(false)
        .type(Integer.class)
        .metavar("RECORD-SIZE")
        .dest("recordSize")
        .setDefault(DEFAULT_RECORD_SIZE)
        .help(producerTranslations.getString("producer.recordSize.help"));

    payloadOptions
        .addArgument("-f", "--payload-file")
        .action(Arguments.store())
        .required(false)
        .type(String.class)
        .metavar("PAYLOAD-FILE")
        .dest("payloadFile")
        .setDefault(DEFAULT_PAYLOAD_FILE)
        .help(producerTranslations.getString("producer.payloadFile.help"));

    return parser;
  }

  private static void overrideArgumentsWithEnvVars(Producer producer) {
    Map<String, String> env = System.getenv();
    boolean throughputOverridden = false;
    boolean numRecordsOverridden = false;

    if (env.containsKey("ES_TOPIC")) {
      producer.setTopic(env.get("ES_TOPIC"));
    }
    if (env.containsKey("ES_RECORD_SIZE")) {
      producer.setRecordSize(Integer.parseInt(env.get("ES_RECORD_SIZE")));
    }
    if (env.containsKey("ES_NUM_THREADS")) {
      producer.setNumThreads(Integer.parseInt(env.get("ES_NUM_THREADS")));
    }
    if (env.containsKey("ES_PRODUCER_CONFIG")) {
      producer.setConfigFilePath(env.get("ES_PRODUCER_CONFIG"));
    }
    if (env.containsKey("ES_PAYLOAD_FILE")) {
      producer.setPayloadFilePath(env.get("ES_PAYLOAD_FILE"));
    }
    if (env.containsKey("ES_PAYLOAD_DELIMITER")) {
      producer.setPayloadDelimiter(env.get("ES_PAYLOAD_DELIMITER"));
    }

    if (env.containsKey("ES_THROUGHPUT")) {
      producer.setThroughput(Integer.parseInt(env.get("ES_THROUGHPUT")));
      throughputOverridden = true;
    }
    if (env.containsKey("ES_NUM_RECORDS")) {
      producer.setNumRecords(Long.parseLong(env.get("ES_NUM_RECORDS")));
      numRecordsOverridden = true;
    }
    if (throughputOverridden && numRecordsOverridden) {
      producer.setSize("");
    }
    if ((throughputOverridden && !numRecordsOverridden)
        || (!throughputOverridden && numRecordsOverridden)) {
      logger.warn(producerTranslations.getString("producer.envar.warning"));
    }

    if (env.containsKey("ES_SIZE")) producer.size = env.get("ES_SIZE");
  }

  public void setSize(String size) {
    this.size = size;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public void setNumThreads(Integer numThreads) {
    this.numThreads = numThreads;
  }

  public void setNumRecords(Long numRecords) {
    this.numRecords = numRecords;
  }

  public void setThroughput(Integer throughput) {
    this.throughput = throughput;
  }

  public void setConfigFilePath(String configFilePath) {
    this.configFilePath = configFilePath;
  }

  public void setRecordSize(Integer recordSize) {
    this.recordSize = recordSize;
  }

  public void setPayloadFilePath(String payloadFilePath) {
    this.payloadFilePath = payloadFilePath;
  }

  public void setShouldPrintMetrics(Boolean shouldPrintMetrics) {
    this.shouldPrintMetrics = shouldPrintMetrics;
  }

  public void setPayloadDelimiter(String payloadDelimiter) {
    this.payloadDelimiter = payloadDelimiter;
  }

  public String getSize() {
    return size;
  }

  public String getTopic() {
    return topic;
  }

  public Integer getNumThreads() {
    return numThreads;
  }

  public Long getNumRecords() {
    return numRecords;
  }

  public Integer getThroughput() {
    return throughput;
  }

  public String getConfigFilePath() {
    return configFilePath;
  }

  public Integer getRecordSize() {
    return recordSize;
  }

  public String getPayloadFilePath() {
    return payloadFilePath;
  }

  public Boolean shouldPrintMetrics() {
    return shouldPrintMetrics;
  }

  public String getPayloadDelimiter() {
    return payloadDelimiter;
  }
}
