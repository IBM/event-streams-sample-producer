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
package cucumber;

import static org.junit.Assert.*;

import com.ibm.es.producer.Producer;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.tools.ProducerPerformance;

public class Stepdefs {

  private String[] performanceArguments;
  private List<String> argumentsList = new ArrayList<>();

  @Before
  public void beforeEach() {
    ProducerPerformance.reset();
  }

  @Given("an instance of the es-producer")
  public void an_instance_of_the_es_producer() {}

  @When("I provide argument {string} of {string}")
  public void i_provide_a_size_of(String string1, String string2) {
    argumentsList.add(string1);
    argumentsList.add(string2);
  }

  @When("I check the arguments that the Kafka performance script is called with")
  public void i_check_the_arguments_that_the_Kafka_performance_script_is_called_with() {
    String[] arguments = ensureMinimumArgumentsAreProvided(argumentsList);
    Producer.main(arguments);
  }

  @When("I provide the print metrics argument {string}")
  public void i_provide_the_print_metrics_argument(String string) {
    argumentsList.add(string);
  }

  @Then("it was called with {string} of {string}")
  public void i_check_the_arguments_the_Kafka_performance_script_has_been_called_with(
      String string1, String string2) {
    performanceArguments = ProducerPerformance.getArguments();
    assertTrue(Arrays.asList(performanceArguments).contains(string1));
    Integer index = Arrays.asList(performanceArguments).indexOf(string1);
    assertEquals(string2, performanceArguments[index + 1]);
  }

  @Then("it was called with the print metrics argument")
  public void it_was_called_with_the_print_metrics_argument() {
    performanceArguments = ProducerPerformance.getArguments();
    assertTrue(Arrays.asList(performanceArguments).contains("--print-metrics"));
  }

  @Then("it was called {string} times")
  public void it_was_called_times(String expectedCallCount) {
    int expectedValue = Integer.valueOf(expectedCallCount);
    int actualValue = ProducerPerformance.getCallCount();
    assertEquals(expectedValue, actualValue);
  }

  private String[] ensureMinimumArgumentsAreProvided(List<String> testedArgs) {
    List<String> newArgsList = new ArrayList<>(testedArgs);
    Map<String[], String> requiredArgsMap = new HashMap<>();
    requiredArgsMap.put(new String[] {"--producer-config", "-c"}, "config.config");
    requiredArgsMap.put(new String[] {"--topic", "-t"}, "topic");

    for (Map.Entry<String[], String> entry : requiredArgsMap.entrySet()) {
      if (!testedArgs.contains(entry.getKey()[0]) && !testedArgs.contains(entry.getKey()[1])) {
        newArgsList.add(entry.getKey()[0]);
        newArgsList.add(entry.getValue());
      }
    }

    return newArgsList.toArray(new String[0]);
  }
}
