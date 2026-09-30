package com.samyisok.jpassvaultclient;

import javafx.event.Event;

/**
 * Publishes {@link StageActionEvent}s on the application stage. Replaces the
 * former Spring {@code ApplicationContext.publishEvent} calls.
 */
public class EventPublisher {

  private final StageHolder stageHolder;

  public EventPublisher(StageHolder stageHolder) {
    this.stageHolder = stageHolder;
  }

  public void publish(EventAction action) {
    Event.fireEvent(stageHolder.getStage(), new StageActionEvent(action));
  }
}
