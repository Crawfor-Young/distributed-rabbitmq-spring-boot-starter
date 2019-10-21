package com.young.core;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageInfo {

  public MessageInfo(String exchange, String routingKey, Object message){
    this.exchange = exchange;
    this.routingKey = routingKey;
    this.message = message;
  }

  String exchange;
  String routingKey;
  Object message;
  int retry = 0;

}
