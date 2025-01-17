package games.strategy.engine.lobby.moderator.toolbox.tabs.event.log;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.triplea.http.client.moderator.toolbox.PagingParams;
import org.triplea.http.client.moderator.toolbox.event.log.ToolboxEventLogClient;

import lombok.RequiredArgsConstructor;


/**
 * Model to interact with the backend for fetching moderator audit history records. The data is paged
 * so we can avoid fetching too much data all at once. This model keeps some state, specifically it
 * keeps track of the last fetched.
 */
@RequiredArgsConstructor
class EventLogTabModel {

  private final ToolboxEventLogClient toolboxEventLogClient;

  static List<String> fetchTableHeaders() {
    return Arrays.asList("Date", "Moderator", "Action", "Target");
  }

  List<List<String>> fetchTableData(final PagingParams pagingParams) {
    return toolboxEventLogClient.lookupModeratorEvents(pagingParams)
        .stream()
        .map(event -> Arrays.asList(
            event.getDate().toString(),
            event.getModeratorName(),
            event.getModeratorAction(),
            event.getActionTarget()))
        .collect(Collectors.toList());
  }
}
