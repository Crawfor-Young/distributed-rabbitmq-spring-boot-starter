package com.young.task;

import com.young.RabbitService;
import java.util.Timer;
import java.util.TimerTask;

public class ResendTask extends TimerTask {

  private RabbitService rabbitService;

  public ResendTask(RabbitService rabbitService){
    this.rabbitService = rabbitService;
    Timer timer = new Timer("rabbit.resend.timer", true);
    timer.scheduleAtFixedRate(this,100,10000);
  }

  @Override
  public void run() {
    rabbitService.reSend(0);
  }

}
