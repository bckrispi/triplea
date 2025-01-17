package org.triplea.lobby.server.db;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.triplea.lobby.server.config.TestLobbyConfigurations;

import com.github.npathai.hamcrestopt.OptionalMatchers;

final class BannedMacControllerIntegrationTest extends AbstractModeratorServiceControllerTestCase {

  private final BannedMacDao controller =
      TestLobbyConfigurations.INTEGRATION_TEST.getDatabaseDao().getBannedMacDao();

  @Test
  void testBanMac() throws Exception {
    assertThat(isMacBanned(), OptionalMatchers.isEmpty());
    banUser();
    assertThat(isMacBanned(), isPresent());
  }

  private void banUser() {
    controller.addBannedMac(user, null, moderator);
  }

  private Optional<Timestamp> isMacBanned() throws Exception {
    return controller.isMacBanned(InetAddress.getLocalHost(), user.getHashedMacAddress());
  }
}
