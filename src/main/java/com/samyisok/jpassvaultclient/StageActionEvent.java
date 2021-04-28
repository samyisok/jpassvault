package com.samyisok.jpassvaultclient;

import org.springframework.context.ApplicationEvent;

public class StageActionEvent extends ApplicationEvent {
  public static class Payload {
    EventAction action;

    public Payload(EventAction action) {
      this.action = action;
    }

    /**
     * @return the action
     */
    public EventAction getAction() {
      return action;
    }

    /**
     * @param action the action to set
     */
    public void setAction(EventAction action) {
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
