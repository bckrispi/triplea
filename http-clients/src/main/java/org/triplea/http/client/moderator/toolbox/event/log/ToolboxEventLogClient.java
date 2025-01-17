package org.triplea.http.client.moderator.toolbox.event.log;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.URI;
import java.util.List;

import org.triplea.http.client.HttpClient;
import org.triplea.http.client.moderator.toolbox.ApiKeyPassword;
import org.triplea.http.client.moderator.toolbox.PagingParams;
import org.triplea.http.client.moderator.toolbox.ToolboxHttpHeaders;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Client object to query server for the moderator event log. The event log is an audit trail of
 * moderator actions.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolboxEventLogClient {
  public static final String AUDIT_HISTORY_PATH = "/moderator-toolbox/audit-history/lookup";

  private final ToolboxHttpHeaders toolboxHttpHeaders;
  private final ToolboxEventLogFeignClient client;

  public static ToolboxEventLogClient newClient(final URI serverUri, final ApiKeyPassword apiKeyPassword) {
    return new ToolboxEventLogClient(
        new ToolboxHttpHeaders(apiKeyPassword),
        new HttpClient<>(ToolboxEventLogFeignClient.class, serverUri).get());
  }

  /**
   * Method to lookup moderator audit history events with paging.
   */
  public List<ModeratorEvent> lookupModeratorEvents(final PagingParams pagingParams) {
    checkArgument(pagingParams.getRowNumber() >= 0);
    checkArgument(pagingParams.getPageSize() > 0);

    return client.lookupModeratorEvents(
        toolboxHttpHeaders.createHeaders(), pagingParams);
  }
}
