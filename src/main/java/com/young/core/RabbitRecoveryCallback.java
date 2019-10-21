package com.young.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryContext;

public class RabbitRecoveryCallback implements RecoveryCallback<Void> {

  private Logger logger = LoggerFactory.getLogger("com.young.rabbit");

  @Override
  public Void recover(RetryContext context) throws Exception {
    if (logger.isErrorEnabled()) {
      logger.error(context.getLastThrowable().getMessage());
    }
    throw new Exception(context.getLastThrowable());
  }
}
