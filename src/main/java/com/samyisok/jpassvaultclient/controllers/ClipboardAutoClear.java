package com.samyisok.jpassvaultclient.controllers;

import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.animation.PauseTransition;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;

/**
 * Copies a secret to the system clipboard and clears it after
 * {@value #CLEAR_DELAY_MS} milliseconds, when the vault locks, and on
 * application exit (design D7/D14). The clear only happens if the clipboard
 * still holds what we copied — foreign content is left alone and the retained
 * copy is dropped.
 */
public class ClipboardAutoClear {

  /** Clear delay in milliseconds — the 30-second policy value. */
  public static final long CLEAR_DELAY_MS = 30_000L;

  /** One-shot scheduler seam; production uses a JavaFX {@link PauseTransition}. */
  public interface Scheduler {
    void schedule(Runnable task, long delayMs);
  }

  private final Supplier<Clipboard> clipboardSupplier;
  private final Scheduler scheduler;
  private String copiedText;

  public ClipboardAutoClear(Clipboard clipboard, Scheduler scheduler) {
    this(clipboard, scheduler, ClipboardAutoClear::registerShutdownHook);
  }

  ClipboardAutoClear(Clipboard clipboard, Scheduler scheduler, Consumer<Runnable> shutdownHooks) {
    this(() -> clipboard, scheduler, shutdownHooks);
  }

  private ClipboardAutoClear(Supplier<Clipboard> clipboardSupplier, Scheduler scheduler,
      Consumer<Runnable> shutdownHooks) {
    this.clipboardSupplier = clipboardSupplier;
    this.scheduler = scheduler;
    shutdownHooks.accept(this::clearNow);
  }

  /**
   * Production wiring: the system clipboard fetched lazily (safe to construct
   * before the toolkit is ready) and a JVM shutdown hook for exit clearing.
   */
  public static ClipboardAutoClear forSystemClipboard() {
    return new ClipboardAutoClear(Clipboard::getSystemClipboard,
        ClipboardAutoClear::pauseTransition, ClipboardAutoClear::registerShutdownHook);
  }

  private static void registerShutdownHook(Runnable runnable) {
    Runtime.getRuntime().addShutdownHook(new Thread(runnable, "clipboard-clear"));
  }

  public void copy(String text) {
    ClipboardContent content = new ClipboardContent();
    content.putString(text);
    clipboardSupplier.get().setContent(content);
    copiedText = text;
    scheduler.schedule(this::clearNow, CLEAR_DELAY_MS);
  }

  /** Clears the clipboard when it still holds our copy; used on LOCK and exit. */
  public void clearNow() {
    if (copiedText == null) {
      return;
    }
    Clipboard clipboard = clipboardSupplier.get();
    if (!copiedText.equals(clipboard.getString())) {
      copiedText = null; // foreign content: do not retain the secret
      return;
    }
    ClipboardContent empty = new ClipboardContent();
    empty.putString("");
    clipboard.setContent(empty);
    copiedText = null;
  }

  private static void pauseTransition(Runnable task, long delayMs) {
    PauseTransition pause = new PauseTransition(Duration.millis(delayMs));
    pause.setOnFinished(event -> task.run());
    pause.playFromStart();
  }
}
