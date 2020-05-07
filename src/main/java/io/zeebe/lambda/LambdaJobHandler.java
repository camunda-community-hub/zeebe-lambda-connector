/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.lambda;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.zeebe.client.api.response.ActivatedJob;
import io.zeebe.client.api.worker.JobClient;
import io.zeebe.client.api.worker.JobHandler;
import io.zeebe.lambda.config.ConfigurationMapFactory;
import io.zeebe.lambda.config.ConfigurationMaps;
import io.zeebe.lambda.config.EnvironmentVariableProvider;
import io.zeebe.lambda.config.PlaceholderProcessor;

@Component
public class LambdaJobHandler implements JobHandler {

  private final Logger logger = LoggerFactory.getLogger(LambdaJobHandler.class);

  private static final String PARAMETER_FUNCTION_NAME = "functionName";
  private static final String PARAMETER_RESULT_NAME = "resultName";
  private static final String PARAMETER_FUNCTION_ERROR_CODE = "functionErrorCode";
  private static final String PARAMETER_BODY = "body";

  @Autowired
  private LambdaInvoicationHelper lambdaInvocation;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private PlaceholderProcessor placeholderProcessor;
  
  @Autowired
  private ConfigurationMapFactory configMapFactory;

  @Override
  public void handle(JobClient jobClient, ActivatedJob job) throws IOException, InterruptedException, ExecutionException, TimeoutException {
    final ConfigurationMaps configMaps = configMapFactory.createConfigurationMap(job);
    final Map<String, Object> resultVariables = new HashMap<String, Object>();

    String functionName = getFunctionName(configMaps);
    String payload = getPayload(configMaps);
    String resultName = getResultName(configMaps);
    
    try {
      
      // Invoke Lambda natively
      logger.info("Invoke Lambda '"+functionName+"' with payload: '" + payload + "'");
      String result = lambdaInvocation.invokeFunction(functionName, payload);      
      logger.info("Lambda function call successful with result: " + result);
      
      // Store result in a workflow variable 
      processResult(resultName, resultVariables, result);
      
    } catch (Throwable e) {
      
      String functionErrorCode = getFunctionErrorCode(configMaps);      
      if (e instanceof LambdaInvocationError && functionErrorCode!=null) { 
        
        // The function returned an error, 
        // and the function error code is configured in the workflow model
        // so use this code to send the error to the BPMN workflow
        logger.info("Lambda function call failed with error, reported as BPMN error code " + functionErrorCode, e);
        jobClient.newThrowErrorCommand(job.getKey()) //
          .errorCode(functionErrorCode) //
          .errorMessage(e.getMessage()) //
          .send().join();
      } else {
        
        // no function error code is set, simply fail the current task
        logger.warn("Lambda function call failed with error, unhandled in BPMN. Retries left: " + (job.getRetries()-1), e);
        jobClient.newFailCommand(job.getKey()) //
          .retries(job.getRetries()-1) //
          .errorMessage(e.getMessage()) //
          .send().join();
      }
    }

    // Move on normally if everything was fine
    jobClient.newCompleteCommand(job.getKey()) //
      .variables(resultVariables) // 
      .send().join();
  }
  
  public static class LambdaResult {
    public int statusCode;
    public String body;
  }

  private void processResult(String resultName, Map<String, Object> resultVariables, String resultString) {
    try {
      LambdaResult result = objectMapper.readValue(resultString, LambdaResult.class);
      resultVariables.put(resultName + "StatusCode", result.statusCode);          
      resultVariables.put(resultName + "JsonString", result.body);
      
      JsonNode resultJsonNode = objectMapper.readTree(result.body);
      resultVariables.put(resultName, resultJsonNode);
    } catch (Exception e) {
      throw new RuntimeException("Could not parse result from Lambda function", e);
    }
  }

  private String getFunctionName(ConfigurationMaps configMaps) {
    return configMaps.getString(PARAMETER_FUNCTION_NAME) //
        .map(config -> placeholderProcessor.process(config, configMaps.getConfig()))
        .orElseThrow(() -> new RuntimeException("Missing required parameter: " + PARAMETER_FUNCTION_NAME));
  }

  private String getResultName(ConfigurationMaps configMaps) {
    return configMaps.getString(PARAMETER_RESULT_NAME) //
        .map(config -> placeholderProcessor.process(config, configMaps.getConfig()))
        .orElse(getFunctionName(configMaps));
  }

  private String getFunctionErrorCode(ConfigurationMaps configMaps) {
    return configMaps.getString(PARAMETER_FUNCTION_ERROR_CODE) //
        .map(config -> placeholderProcessor.process(config, configMaps.getConfig()))
        .orElse(null);
  }

  private String getPayload(ConfigurationMaps configMaps) {
    return configMaps //
        .get(PARAMETER_BODY) //
        .map(body -> {
          if (body instanceof String) { // if a string, replace placeholders
            return placeholderProcessor.process((String) body, configMaps.getConfig());
          } else { // or transform object
            return bodyToJson(body);
          }
        }).orElse(
          // if no body is configured, add all variables            
          bodyToJson(configMaps.getConfig())
        );
  }

  private String bodyToJson(Object body) {
    try {
      return objectMapper.writeValueAsString(body);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize request body to JSON: " + body);
    }
  }
}
