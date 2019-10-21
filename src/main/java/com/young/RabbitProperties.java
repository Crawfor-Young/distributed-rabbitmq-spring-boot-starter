package com.young;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "config.rabbit")
@Data
public class RabbitProperties {

  private String host;
  private Integer port;
  private String username;
  private String password;
  private String virtualHost;
  private int retryAttempts = -1;
  private int retryOpenTimeout = 50000;
  private int retryResetTimeout = 200000;
}