package io.zeebe.lambda;

public class LambdaInvocationError extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public LambdaInvocationError(String message) {
    super(message);
  }

}
