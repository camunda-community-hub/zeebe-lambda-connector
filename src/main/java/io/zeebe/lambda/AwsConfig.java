package io.zeebe.lambda;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Simplified version for now - just use a pre-configured access key and secret for the instance
 */
@Configuration
public class AwsConfig {
  
  @Value("${aws.accessKey}")
  private String awsAccessKey = "Your AWS ID goes here";
  
  @Value("${aws.secret}")
  private String awsSecretAccessKey = "Your AWS secret key goes here";

  @Value("${aws.region}")
  private String awsRegionName;

  public String getAwsAccessKey() {
    return awsAccessKey;
  }

  public String getAwsSecretAccessKey() {
    return awsSecretAccessKey;
  }

  public String getAwsRegionName() {
    return awsRegionName;
  }

}
