package com.samyisok.jpassvaultclient;

import org.springframework.context.ApplicationEvent;

public class StageActionEvent extends ApplicationEvent {
  public static class Payload {
    String action;

    public Payload(String action) {
      this.action = action;
    }

    /**
     * @return the action
     */
    public String getAction() {
      return action;
    }

    /**
     * @param action the action to set
     */
    public void setAction(String action) {
      this.action = action;
    }
  }

  private Payload payload;

  public StageActionEvent(Payload payload) {
    super(payload);
    this.payload = payload;
  }

  public Payload getEvent() {
    return ((Payload) getSource());
  }

  /**
   * @return the payload
   */
  public Payload getPayload() {
    return payload;
  }
}
