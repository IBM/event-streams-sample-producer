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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.kafka.tools.ProducerPerformance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProducerThread extends Thread {

  private Thread thread;
  private String threadName;
  private Producer producer;

  private static Logger logger = LoggerFactory.getLogger(ProducerThread.class);

  ProducerThread(ThreadGroup threadGroup, String threadName, Producer producer) {
    super(threadGroup, threadName);
    this.threadName = threadName;
    this.producer = producer;
  }

  @Override
  public void run() {
    List<String> argumentsList =
        new ArrayList<>(
            Arrays.asList(
                "--topic", producer.getTopic(),
                "--num-records", String.valueOf(producer.getNumRecords()),
                "--throughput", String.valueOf(producer.getThroughput()),
                "--producer.config", producer.getConfigFilePath()));

    if (producer.shouldPrintMetrics()) {
      argumentsList.add("--print-metrics");
    }

    if ("".equals(producer.getPayloadFilePath())) {
      argumentsList.add("--record-size");
      argumentsList.add(String.valueOf(producer.getRecordSize()));
    } else {
      argumentsList.add("--payload-file");
      argumentsList.add(producer.getPayloadFilePath());

      argumentsList.add("--payload-delimiter");
      argumentsList.add(producer.getPayloadDelimiter());
    }

    try {
      String[] arguments = new String[argumentsList.size()];
      arguments = argumentsList.toArray(arguments);
      ProducerPerformance.main(arguments);
    } catch (Exception error) {
      logger.error("Failed to execute", error);
    }
  }

  @Override
  public void start() {
    if (thread == null) {
      thread = new Thread(this, threadName);
      thread.start();
    }
  }
}
