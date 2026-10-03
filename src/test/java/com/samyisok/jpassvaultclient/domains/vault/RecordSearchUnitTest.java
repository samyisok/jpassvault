package com.samyisok.jpassvaultclient.domains.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecordSearchUnitTest {

  private Collection<String> names;

  @BeforeEach
  void setUp() {
    names = List.of("GitHub", "github backup", "Bank");
  }

  @Test
  @DisplayName("filter matches names containing it regardless of case")
  void matchesIgnoringCase() {
    assertEquals(List.of("GitHub", "github backup"), RecordSearch.matches(names, "github"));
  }

  @Test
  @DisplayName("filter matches any substring position in the name")
  void matchesSubstringAnywhere() {
    assertEquals(List.of("Bank"), RecordSearch.matches(names, "ank"));
  }

  @Test
  @DisplayName("empty filter returns every name")
  void emptyFilterReturnsEverything() {
    assertEquals(List.of("Bank", "GitHub", "github backup"), RecordSearch.matches(names, ""));
  }

  @Test
  @DisplayName("filter matching nothing returns no names")
  void noMatchReturnsNothing() {
    assertEquals(List.of(), RecordSearch.matches(names, "zzz"));
  }

  @Test
  @DisplayName("null filter behaves as an empty filter")
  void nullFilterBehavesAsEmpty() {
    assertEquals(List.of("Bank", "GitHub", "github backup"), RecordSearch.matches(names, null));
  }

  @Test
  @DisplayName("matched names come back in case-insensitive alphabetical order")
  void sortsAlphabeticallyIgnoringCase() {
    List<String> unsorted = List.of("bank", "GitHub", "apple");

    assertEquals(List.of("apple", "bank", "GitHub"), RecordSearch.matches(unsorted, ""));
  }

  @Test
  @DisplayName("post-create state carries the filter, the matches and the selection")
  void afterCreateReturnsPrefillState() {
    var state = RecordSearch.afterCreate(names, "GitHub");

    assertEquals("GitHub", state.filterText());
    assertEquals("GitHub", state.selectedName());
    assertEquals(List.of("GitHub", "github backup"), state.visibleNames());
  }
}
