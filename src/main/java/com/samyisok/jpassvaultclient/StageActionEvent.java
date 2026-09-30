package com.samyisok.jpassvaultclient;

import java.util.EnumMap;
import java.util.Map;
import javafx.event.Event;
import javafx.event.EventType;

/**
 * Scene-switching event fired on the application {@link javafx.stage.Stage}.
 * Each {@link EventAction} has a dedicated {@link EventType} registered as a
 * child of {@link #STAGE_ACTION}, so a single handler on the parent type
 * receives every action.
 */
public class StageActionEvent extends Event {

  public static final EventType<StageActionEvent> STAGE_ACTION =
      new EventType<>("STAGE_ACTION");

  private static final Map<EventAction, EventType<StageActionEvent>> ACTION_TYPES =
      new EnumMap<>(EventAction.class);

  static {
    for (EventAction action : EventAction.values()) {
      ACTION_TYPES.put(action, new EventType<>(STAGE_ACTION, action.name()));
    }
  }

  private final EventAction action;

  public StageActionEvent(EventAction action) {
    super(ACTION_TYPES.get(action));
    this.action = action;
  }

  public EventAction getAction() {
    return action;
  }
}
