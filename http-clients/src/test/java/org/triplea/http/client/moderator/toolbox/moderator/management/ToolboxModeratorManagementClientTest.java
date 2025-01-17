package org.triplea.http.client.moderator.toolbox.moderator.management;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.triplea.http.client.HttpClientTesting.API_KEY_PASSWORD;
import static org.triplea.http.client.HttpClientTesting.serve200ForToolboxPostWithBody;
import static org.triplea.http.client.HttpClientTesting.toJson;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.http.client.HttpClientTesting;
import org.triplea.http.client.moderator.toolbox.NewApiKey;
import org.triplea.http.client.moderator.toolbox.ToolboxHttpHeaders;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import ru.lanwen.wiremock.ext.WiremockResolver;
import ru.lanwen.wiremock.ext.WiremockUriResolver;

@ExtendWith({
    WiremockResolver.class,
    WiremockUriResolver.class
})
class ToolboxModeratorManagementClientTest {

  private static final String MODERATOR_NAME = "Ooh! Pieces o' urchin are forever coal-black.";
  private static final NewApiKey NEW_API_KEY = new NewApiKey("Tobaccos sing from fortunes like undead shipmates.");
  private static final ModeratorInfo MODERATOR_INFO = ModeratorInfo.builder()
      .name("Oh, power!")
      .lastLogin(Instant.now())
      .build();

  private static ToolboxModeratorManagementClient newClient(final WireMockServer wireMockServer) {
    final URI hostUri = URI.create(wireMockServer.url(""));
    return ToolboxModeratorManagementClient.newClient(hostUri, API_KEY_PASSWORD);
  }

  @Test
  void fetchModeratorList(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.get(ToolboxModeratorManagementClient.FETCH_MODERATORS_PATH)
            .withHeader(ToolboxHttpHeaders.API_KEY_HEADER, equalTo(API_KEY_PASSWORD.getApiKey()))
            .withHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER, equalTo(API_KEY_PASSWORD.getPassword()))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(
                        toJson(Collections.singletonList(MODERATOR_INFO)))));

    final List<ModeratorInfo> results = newClient(server).fetchModeratorList();

    assertThat(results, hasSize(1));
    assertThat(results.get(0), is(MODERATOR_INFO));
  }

  @Nested
  final class IsCurrentUserSuperMod {
    @Test
    void positiveCase(@WiremockResolver.Wiremock final WireMockServer server) {
      expectIsSuperModAndReturn(server, true);

      assertThat(
          newClient(server).isCurrentUserSuperMod(),
          is(true));
    }

    void expectIsSuperModAndReturn(final WireMockServer server, final boolean value) {
      server.stubFor(
          WireMock.get(ToolboxModeratorManagementClient.IS_SUPER_MOD_PATH)
              .withHeader(ToolboxHttpHeaders.API_KEY_HEADER, equalTo(API_KEY_PASSWORD.getApiKey()))
              .withHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER, equalTo(API_KEY_PASSWORD.getPassword()))
              .willReturn(
                  WireMock.aResponse()
                      .withStatus(200)
                      .withBody(String.valueOf(value))));
    }

    @Test
    void negativeCase(@WiremockResolver.Wiremock final WireMockServer server) {
      expectIsSuperModAndReturn(server, false);

      assertThat(
          newClient(server).isCurrentUserSuperMod(),
          is(false));
    }
  }

  @Test
  void removeMod(@WiremockResolver.Wiremock final WireMockServer server) {
    serve200ForToolboxPostWithBody(server, ToolboxModeratorManagementClient.REMOVE_MOD_PATH, MODERATOR_NAME);

    newClient(server).removeMod(MODERATOR_NAME);
  }

  @Test
  void addSuperMod(@WiremockResolver.Wiremock final WireMockServer server) {
    serve200ForToolboxPostWithBody(server, ToolboxModeratorManagementClient.ADD_SUPER_MOD_PATH, MODERATOR_NAME);

    newClient(server).addSuperMod(MODERATOR_NAME);
  }

  @Test
  void checkUserExists(@WiremockResolver.Wiremock final WireMockServer server) {
    serve200ForToolboxPostWithBody(server, ToolboxModeratorManagementClient.ADD_SUPER_MOD_PATH, MODERATOR_NAME);

    newClient(server).addSuperMod(MODERATOR_NAME);
  }

  @Test
  void generateSingleUseKey(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.post(ToolboxModeratorManagementClient.SUPER_MOD_GENERATE_SINGLE_USE_KEY_PATH)
            .withHeader(ToolboxHttpHeaders.API_KEY_HEADER, equalTo(API_KEY_PASSWORD.getApiKey()))
            .withHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER, equalTo(API_KEY_PASSWORD.getPassword()))
            .withRequestBody(equalTo(MODERATOR_NAME))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(HttpClientTesting.toJson(NEW_API_KEY))));


    final String result = newClient(server).generateSingleUseKey(MODERATOR_NAME);

    assertThat(result, is(NEW_API_KEY.getApiKey()));
  }


  @Test
  void addModerator(@WiremockResolver.Wiremock final WireMockServer server) {
    serve200ForToolboxPostWithBody(server, ToolboxModeratorManagementClient.ADD_MODERATOR_PATH, MODERATOR_NAME);

    newClient(server).addModerator(MODERATOR_NAME);
  }
}
