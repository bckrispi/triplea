package games.strategy.engine.lobby.moderator.toolbox.tabs.access.log;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.triplea.http.client.moderator.toolbox.PagingParams;
import org.triplea.http.client.moderator.toolbox.access.log.ToolboxAccessLogClient;
import org.triplea.http.client.moderator.toolbox.banned.name.ToolboxUsernameBanClient;
import org.triplea.http.client.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.moderator.toolbox.banned.user.UserBanParams;

import com.google.common.annotations.VisibleForTesting;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class AccessLogTabModel {

  @VisibleForTesting
  static final String BAN_NAME_BUTTON_TEXT = "Ban Name";
  @VisibleForTesting
  static final String BAN_USER_BUTTON_TEXT = "Ban User";

  private final ToolboxAccessLogClient toolboxAccessLogClient;
  private final ToolboxUserBanClient toolboxUserBanClient;
  private final ToolboxUsernameBanClient toolboxUsernameBanClient;

  static List<String> fetchTableHeaders() {
    return Arrays.asList(
        "Access Date",
        "Username",
        "IP",
        "Hashed Mac",
        "Registered",
        "",
        "");
  }

  List<List<String>> fetchTableData(final PagingParams pagingParams) {
    return toolboxAccessLogClient.getAccessLog(pagingParams)
        .stream()
        .map(accessLogData -> Arrays.asList(
            accessLogData.getAccessDate().toString(),
            accessLogData.getUsername(),
            accessLogData.getIp(),
            accessLogData.getHashedMac(),
            accessLogData.isRegistered() ? "Y" : "",
            "Ban Name",
            "Ban User"))
        .collect(Collectors.toList());
  }

  void banUserName(final String username) {
    toolboxUsernameBanClient.addUsernameBan(username);
  }

  void banUser(final UserBanParams banUserParams) {
    toolboxUserBanClient.banUser(banUserParams);
  }
}
