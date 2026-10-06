package com.samyisok.jpassvaultclient.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;

class ClipboardAutoClearUnitTest {

  private Clipboard clipboard;
  private FakeScheduler scheduler;
  private ClipboardAutoClear autoClear;

  @BeforeEach
  void setUp() {
    clipboard = mock(Clipboard.class);
    scheduler = new FakeScheduler();
    autoClear = new ClipboardAutoClear(clipboard, scheduler);
  }

  @Test
  @DisplayName("copy schedules a one-shot clear 30 seconds out")
  void schedulesOneClear() {
    autoClear.copy("secret");
    assertEquals(ClipboardAutoClear.CLEAR_DELAY_MS, scheduler.delayMs);
    assertEquals(1, scheduler.tasks.size());
  }

  @Test
  @DisplayName("the scheduled clear removes our copied password")
  void clearsOurCopiedPassword() {
    autoClear.copy("secret");
    when(clipboard.getString()).thenReturn("secret");
    ArgumentCaptor<ClipboardContent> captor = ArgumentCaptor.forClass(ClipboardContent.class);
    scheduler.runNext();
    verify(clipboard, times(2)).setContent(captor.capture());
    assertEquals("", captor.getAllValues().get(1).get(DataFormat.PLAIN_TEXT));
  }

  @Test
  @DisplayName("a foreign clipboard value is left alone")
  void leavesForeignContentAlone() {
    autoClear.copy("secret");
    when(clipboard.getString()).thenReturn("someone-elses-data");
    scheduler.runNext();
    verify(clipboard, times(1)).setContent(any(ClipboardContent.class));
  }

  @Test
  @DisplayName("clearNow removes a pending copied password (used on LOCK)")
  void clearNowRemovesPendingCopy() {
    autoClear.copy("secret");
    when(clipboard.getString()).thenReturn("secret");
    autoClear.clearNow();
    ArgumentCaptor<ClipboardContent> captor = ArgumentCaptor.forClass(ClipboardContent.class);
    verify(clipboard, times(2)).setContent(captor.capture());
    assertEquals("", captor.getAllValues().get(1).get(DataFormat.PLAIN_TEXT));
  }

  @Test
  @DisplayName("the shutdown hook clears a pending copied password (used on exit)")
  void shutdownHookRemovesPendingCopy() {
    RecordingHooks hooks = new RecordingHooks();
    ClipboardAutoClear helper = new ClipboardAutoClear(clipboard, scheduler, hooks);
    helper.copy("secret");
    when(clipboard.getString()).thenReturn("secret");
    hooks.registered.run();
    verify(clipboard, times(2)).setContent(any(ClipboardContent.class));
  }

  @Test
  @DisplayName("the copied value is not retained once foreign content is seen")
  void copiedValueIsNotRetained() {
    autoClear.copy("secret");
    when(clipboard.getString()).thenReturn("someone-elses-data");
    autoClear.clearNow();
    when(clipboard.getString()).thenReturn("secret");
    autoClear.clearNow();
    verify(clipboard, times(1)).setContent(any(ClipboardContent.class));
  }

  /** Captures the registered shutdown hook instead of registering it with the JVM. */
  static class RecordingHooks implements java.util.function.Consumer<Runnable> {

    Runnable registered;

    @Override
    public void accept(Runnable runnable) {
      this.registered = runnable;
    }
  }

  /** Records scheduled tasks so tests never wait on real time. */
  static class FakeScheduler implements ClipboardAutoClear.Scheduler {

    final List<Runnable> tasks = new ArrayList<>();
    long delayMs;

    @Override
    public void schedule(Runnable task, long delayMs) {
      tasks.add(task);
      this.delayMs = delayMs;
    }

    void runNext() {
      tasks.remove(0).run();
    }
  }
}
