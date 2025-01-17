package org.triplea.http.client.moderator.toolbox.banned.user;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.triplea.http.client.HttpClientTesting.API_KEY_PASSWORD;
import static org.triplea.http.client.HttpClientTesting.serve200ForToolboxPostWithBody;
import static org.triplea.http.client.HttpClientTesting.serve200ForToolboxPostWithBodyJson;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.http.client.HttpClientTesting;
import org.triplea.http.client.moderator.toolbox.ToolboxHttpHeaders;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import ru.lanwen.wiremock.ext.WiremockResolver;
import ru.lanwen.wiremock.ext.WiremockUriResolver;

@ExtendWith({
    WiremockResolver.class,
    WiremockUriResolver.class
})
class ToolboxUserBanClientTest {

  private static final String BAN_ID = "Halitosis is a weird cannibal.";

  private static final UserBanData BANNED_USER_DATA = UserBanData.builder()
      .banDate(Instant.now())
      .banExpiry(Instant.now().plusSeconds(100))
      .banId("Yarr, sunny freebooter. you won't haul the bikini atoll.")
      .hashedMac("Crush me shark, ye undead dubloon!")
      .ip("Seashells whine with horror!")
      .username("The furner stutters urchin like a black fish.")
      .build();

  private static final UserBanParams BAN_USER_PARAMS = UserBanParams.builder()
      .hashedMac("Why does the skull grow?")
      .hoursToBan(15)
      .ip("Fall loudly like a jolly son.")
      .username("Grace is a scrawny breeze.")
      .build();

  private static ToolboxUserBanClient newClient(final WireMockServer wireMockServer) {
    final URI hostUri = URI.create(wireMockServer.url(""));
    return ToolboxUserBanClient.newClient(hostUri, API_KEY_PASSWORD);
  }


  @Test
  void getUserBans(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.get(ToolboxUserBanClient.GET_USER_BANS_PATH)
            .withHeader(ToolboxHttpHeaders.API_KEY_HEADER, equalTo(API_KEY_PASSWORD.getApiKey()))
            .withHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER, equalTo(API_KEY_PASSWORD.getPassword()))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withBody(
                    HttpClientTesting.toJson(
                        Collections.singletonList(BANNED_USER_DATA)))));

    final List<UserBanData> result = newClient(server).getUserBans();

    assertThat(result, hasSize(1));
    assertThat(result.get(0), is(BANNED_USER_DATA));


  }

  @Test
  void removeUserBan(@WiremockResolver.Wiremock final WireMockServer server) {
    serve200ForToolboxPostWithBody(server, ToolboxUserBanClient.REMOVE_USER_BAN_PATH, BAN_ID);

    newClient(server).removeUserBan(BAN_ID);
  }

  @Test
  void banUser(@WiremockResolver.Wiremock final WireMockServer server) {
    serve200ForToolboxPostWithBodyJson(server, ToolboxUserBanClient.BAN_USER_PATH, BAN_USER_PARAMS);

    newClient(server).banUser(BAN_USER_PARAMS);
  }
}
