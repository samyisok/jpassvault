package com.samyisok.jpassvaultclient.domains.vault;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Decides which record names a Search filter shows, and what the vault screen
 * should display immediately after a record is created.
 *
 * <p>Pure: no JavaFX, no I/O, no state. That is what lets every rule here be
 * tested without a display, which no test of the controls themselves can be.
 */
public final class RecordSearch {

  private RecordSearch() {
  }

  /**
   * Names containing {@code filter}, ignoring case, ordered case-insensitively
   * so the list does not reshuffle between refreshes. A null filter behaves as
   * empty, so an untouched Search box shows everything.
   */
  public static List<String> matches(Collection<String> names, String filter) {
    String needle = filter == null ? "" : filter.toLowerCase(Locale.ROOT);
    return names.stream()
        .filter(name -> name.toLowerCase(Locale.ROOT).contains(needle))
        .sorted(Comparator.comparing(name -> name.toLowerCase(Locale.ROOT)))
        .collect(Collectors.toList());
  }

  /**
   * The screen state a successful creation produces: the created name becomes
   * the filter text (replacing a stale filter), the list is refreshed from it,
   * and the created record is the selection.
   */
  public static PostCreateState afterCreate(Collection<String> names, String createdName) {
    return new PostCreateState(createdName, matches(names, createdName), createdName);
  }

  /** Filter text to show, names that stay visible, and the record to select. */
  public record PostCreateState(String filterText, List<String> visibleNames,
      String selectedName) {
  }
}
