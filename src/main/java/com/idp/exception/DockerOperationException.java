package com.idp.exception;

public class DockerOperationException extends RuntimeException {

  public DockerOperationException(String message) {
    super(message);
  }

  public DockerOperationException(String message, Throwable cause) {
    super(message, cause);
  }
}
