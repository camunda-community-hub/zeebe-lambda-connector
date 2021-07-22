package io.zeebe.lambda.config;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.zeebe.lambda.LambdaInvoicationHelper;

/**
 * Generates a Map that overlays different sources for objects used in expressions, namely
 * 
 * <ul>
 *   <li>Custom Headers of a Task in a BPMN workflow</li>
 *   <li>Workflow variables of the current instance</li>
 *   <li>Environment variables loaded from a given URL</li>
 * 
 * Additionally some constants can also be resolved, namely
 * 
 * <ul>
 *  <li>jobKey</li>
 *  <li>workflowInstanceKey</li>
 * </ul>
 */
@Component
public class ConfigurationMapFactory {
  
  private final Logger logger = LoggerFactory.getLogger(LambdaInvoicationHelper.class);

  @Autowired
  private EnvironmentVariableProvider environmentVariableProvider;

  @Autowired
  private ObjectMapper objectMapper;

  public ConfigurationMaps createConfigurationMap(ActivatedJob job) {

    Map<String, String> customHeaders = job.getCustomHeaders();
    Map<String, Object> variables = job.getVariablesAsMap();

    HashMap<String, Object> config = new HashMap<>();
    config.putAll(customHeaders);
    config.putAll(variables);
    config.putAll(environmentVariableProvider.getVariables());

    config.put("jobKey", job.getKey());
    config.put("processInstanceKey", job.getProcessInstanceKey());

    try {      
      String jsonString = objectMapper.writeValueAsString(variables);
      config.put("variablesJson", jsonString);
      config.put("variablesJsonEscaped", new String(JsonStringEncoder.getInstance().quoteAsUTF8(jsonString), "UTF-8"));
    } catch (Exception e) {
      logger.error("Could not transform workflow variables to Json", e);
    }
    
    return new ConfigurationMaps(config);
  }
  
  

}
