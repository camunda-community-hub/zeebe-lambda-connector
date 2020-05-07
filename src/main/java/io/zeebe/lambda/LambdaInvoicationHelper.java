package io.zeebe.lambda;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;

@Component
public class LambdaInvoicationHelper {

  private final Logger logger = LoggerFactory.getLogger(LambdaInvoicationHelper.class);

  @Autowired
  private AwsConfig awsConfig;

  public String invokeFunction(String functionName, String payloadAsJson) {
    AWSLambda lambdaClient = buildAwsLambdaClient();
    
    InvokeRequest req = new InvokeRequest() //
        .withFunctionName(functionName) //
        .withPayload(payloadAsJson);
    
    InvokeResult requestResult = lambdaClient.invoke(req);

    String result = null;
    if (requestResult.getPayload() != null) {
      result = StandardCharsets.UTF_8.decode(requestResult.getPayload()).toString();
    }
    
    if (requestResult.getFunctionError()!=null) {
      logger.info("Failure invoking Lambda '"+functionName+"': '" + requestResult.getFunctionError()+ "': " + result);
      throw new LambdaInvocationError("Failure invoking Lambda '"+functionName+"':" + requestResult.getFunctionError()+ "': " + result);
    } else {    
      logger.info("Result of Lambda '"+functionName+"': " + result);
      return result;
    }
  }

  private AWSLambda buildAwsLambdaClient() {
    // TODO: use data from Resource String?
    // arn:aws:lambda:#{AWS::Region}:#{AWS::AccountId}:function:${self:service}-${opt:stage}-book-hotel
    Regions region = Regions.fromName(awsConfig.getAwsRegionName());
    BasicAWSCredentials credentials = new BasicAWSCredentials(awsConfig.getAwsAccessKey(), awsConfig.getAwsSecretAccessKey());
    AWSLambdaClientBuilder builder = AWSLambdaClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(region);
    AWSLambda client = builder.build();
    return client;
  }

}
