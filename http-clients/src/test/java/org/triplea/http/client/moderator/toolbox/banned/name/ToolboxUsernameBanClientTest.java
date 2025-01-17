package org.triplea.http.client.moderator.toolbox.banned.name;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.triplea.http.client.HttpClientTesting.API_KEY_PASSWORD;
import static org.triplea.http.client.HttpClientTesting.serve200ForToolboxPostWithBody;

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
class ToolboxUsernameBanClientTest {
  private static final String USERNAME = "Faith ho! pull to be robed.";

  private static final UsernameBanData BANNED_USERNAME_DATA = UsernameBanData.builder()
      .banDate(Instant.now())
      .bannedName("Cannons grow with halitosis!")
      .build();

  private static ToolboxUsernameBanClient newClient(final WireMockServer wireMockServer) {
    final URI hostUri = URI.create(wireMockServer.url(""));
    return ToolboxUsernameBanClient.newClient(hostUri, API_KEY_PASSWORD);
  }

  @Test
  void removeUsernameBan(@WiremockResolver.Wiremock final WireMockServer server) {
    serve200ForToolboxPostWithBody(server, ToolboxUsernameBanClient.REMOVE_BANNED_USER_NAME_PATH, USERNAME);

    newClient(server).removeUsernameBan(USERNAME);
  }

  @Test
  void addUsernameBan(@WiremockResolver.Wiremock final WireMockServer server) {
    serve200ForToolboxPostWithBody(server, ToolboxUsernameBanClient.ADD_BANNED_USER_NAME_PATH, USERNAME);

    newClient(server).addUsernameBan(USERNAME);
  }

  @Test
  void getUsernameBans(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.get(ToolboxUsernameBanClient.GET_BANNED_USER_NAMES_PATH)
            .withHeader(ToolboxHttpHeaders.API_KEY_HEADER, equalTo(API_KEY_PASSWORD.getApiKey()))
            .withHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER, equalTo(API_KEY_PASSWORD.getPassword()))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withBody(
                    HttpClientTesting.toJson(
                        Collections.singletonList(BANNED_USERNAME_DATA)))));

    final List<UsernameBanData> results = newClient(server).getUsernameBans();

    assertThat(results, hasSize(1));
    assertThat(results.get(0), is(BANNED_USERNAME_DATA));
  }
}
