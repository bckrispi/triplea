package games.strategy.engine.lobby.moderator.toolbox.tabs.banned.names;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.banned.name.ToolboxUsernameBanClient;
import org.triplea.http.client.moderator.toolbox.banned.name.UsernameBanData;

import games.strategy.engine.lobby.moderator.toolbox.tabs.ToolboxTabModelTestUtil;

@ExtendWith(MockitoExtension.class)
class BannedUsernamesTabModelTest {

  private static final String USERNAME = "Belay, yer not desiring me without a pestilence!";
  private static final UsernameBanData BANNED_USERNAME_DATA =
      UsernameBanData.builder()
          .banDate(Instant.now())
          .bannedName("Fear the pacific ocean until it falls.")
          .build();

  @Mock
  private ToolboxUsernameBanClient toolboxUsernameBanClient;

  @InjectMocks
  private BannedUsernamesTabModel bannedUsernamesTabModel;


  @Test
  void fetchTableData() {
    when(toolboxUsernameBanClient.getUsernameBans())
        .thenReturn(Collections.singletonList(BANNED_USERNAME_DATA));

    final List<List<String>> tabledata = bannedUsernamesTabModel.fetchTableData();

    ToolboxTabModelTestUtil.verifyTableDimensions(tabledata, BannedUsernamesTabModel.fetchTableHeaders());
    ToolboxTabModelTestUtil.verifyTableDataAtRow(tabledata, 0,
        BANNED_USERNAME_DATA.getBannedName(),
        BANNED_USERNAME_DATA.getBanDate().toString(),
        BannedUsernamesTabModel.REMOVE_BUTTON_TEXT);
  }

  @Test
  void removeUsernameBan() {
    bannedUsernamesTabModel.removeUsernameBan(USERNAME);

    verify(toolboxUsernameBanClient).removeUsernameBan(USERNAME);
  }

  @Test
  void addUsernameBan() {
    bannedUsernamesTabModel.addUsernameBan(USERNAME);

    verify(toolboxUsernameBanClient).addUsernameBan(USERNAME);
  }
}
