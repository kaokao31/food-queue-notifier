package com.kku.queuenotify.exception;

import org.springframework.http.HttpStatus;

/** A business conflict, rather than an unexpected server failure. */
public class IllegalStateTransitionException extends ApiException {
  public IllegalStateTransitionException(String message){super(HttpStatus.CONFLICT,message);}
}
