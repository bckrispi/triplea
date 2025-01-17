package games.strategy.engine.lobby.moderator.toolbox.tabs.bad.words;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.triplea.http.client.moderator.toolbox.bad.words.ToolboxBadWordsClient;

import lombok.Builder;

/**
 * Model object interacts with backend, does not hold state and is not aware of UI components.
 */
@Builder
class BadWordsTabModel {

  @Nonnull
  private final ToolboxBadWordsClient toolboxBadWordsClient;

  static List<String> fetchTableHeaders() {
    return Arrays.asList("Bad Word", "");
  }

  /**
   * Returns a "2 x N " list of lists. The first column value is the bad word value from the bad words table,
   * the second column value is the "remove" button.
   */
  List<List<String>> fetchTableData() {
    try {
      return toolboxBadWordsClient
          .getBadWords()
          .stream()
          .map(word -> Arrays.asList(word, BadWordsTabActions.REMOVE_BUTTON_TEXT))
          .collect(Collectors.toList());
    } catch (final RuntimeException e) {
      return Collections.emptyList();
    }
  }

  void removeBadWord(final String wordToRemove) {
    toolboxBadWordsClient.removeBadWord(wordToRemove);
  }

  void addBadWord(final String newBadWord) {
    toolboxBadWordsClient.addBadWord(newBadWord);
  }
}
