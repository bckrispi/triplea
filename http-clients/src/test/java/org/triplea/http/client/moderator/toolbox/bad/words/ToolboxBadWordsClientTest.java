package org.triplea.http.client.moderator.toolbox.bad.words;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.triplea.http.client.HttpClientTesting.API_KEY_PASSWORD;
import static org.triplea.http.client.HttpClientTesting.serve200ForToolboxPostWithBody;

import java.net.URI;
import java.util.Arrays;
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
class ToolboxBadWordsClientTest {
  private static final String BAD_WORD = "Damn yer bilge rat, feed the corsair.";
  private static final List<String> badWords = Arrays.asList("one", "two", "three");

  private static ToolboxBadWordsClient newClient(final WireMockServer wireMockServer) {
    final URI hostUri = URI.create(wireMockServer.url(""));
    return ToolboxBadWordsClient.newClient(hostUri, API_KEY_PASSWORD);
  }

  @Test
  void removeBadWord(@WiremockResolver.Wiremock final WireMockServer server) {
    serve200ForToolboxPostWithBody(server, ToolboxBadWordsClient.BAD_WORD_REMOVE_PATH, BAD_WORD);

    newClient(server).removeBadWord(BAD_WORD);
  }

  @Test
  void addBadWord(@WiremockResolver.Wiremock final WireMockServer server) {
    serve200ForToolboxPostWithBody(server, ToolboxBadWordsClient.BAD_WORD_ADD_PATH, BAD_WORD);

    newClient(server).addBadWord(BAD_WORD);
  }

  @Test
  void getBadWords(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.get(ToolboxBadWordsClient.BAD_WORD_GET_PATH)
            .withHeader(ToolboxHttpHeaders.API_KEY_HEADER, equalTo(API_KEY_PASSWORD.getApiKey()))
            .withHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER, equalTo(API_KEY_PASSWORD.getPassword()))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(HttpClientTesting.toJson(badWords))));

    final List<String> result = newClient(server).getBadWords();

    assertThat(result, is(badWords));
  }
}
