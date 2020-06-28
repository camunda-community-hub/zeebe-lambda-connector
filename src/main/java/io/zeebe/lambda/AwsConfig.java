package io.zeebe.lambda;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * if access key and secret are defined they will be used to call the Lambda api
 * if they are not defined we assume that the container running this app is associated with an IAM role that gives it permission to invoke the lambdas it needs to
 */
@Configuration
@ConfigurationProperties(prefix = "aws")
@Data
public class AwsConfig {
  private String accessKey;
  private String secret;
  private String region;
  private final Logger logger = LoggerFactory.getLogger(AwsConfig.class);

  @Bean
  public AWSLambda buildAwsLambdaClient() {
    AWSLambda client;
    if (accessKey == null) {
      logger.info("Creating Lambda client without credentials the container will use task IAM role ");
      Regions region = Regions.fromName(this.region);
      AWSLambdaClientBuilder builder = AWSLambdaClientBuilder.standard()
              .withRegion(region);
      client = builder.build();
    } else {
      logger.info("Creating Lambda client with provided access and secret key");
      Regions region = Regions.fromName(this.region);
      BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secret);
      AWSLambdaClientBuilder builder = AWSLambdaClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(region);
      client = builder.build();
    }
    return client;
  }

}
