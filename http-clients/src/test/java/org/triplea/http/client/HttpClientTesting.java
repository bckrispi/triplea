package org.triplea.http.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.triplea.http.client.moderator.toolbox.ApiKeyPassword;
import org.triplea.http.client.moderator.toolbox.PagingParams;
import org.triplea.http.client.moderator.toolbox.ToolboxHttpHeaders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

import feign.FeignException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * Utility class with tests for common http client error scenarios.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpClientTesting {

  public static final ApiKeyPassword API_KEY_PASSWORD = ApiKeyPassword.builder()
      .apiKey("api-key")
      .password("key-password")
      .build();

  public static final PagingParams PAGING_PARAMS = PagingParams.builder()
      .pageSize(10)
      .build();

  private static final String CONTENT_TYPE_JSON = "application/json";
  private static final String FAILURE_MESSAGE_FROM_SERVER = "simulated failure message from server";
  private static final ObjectMapper objectMapper;

  static {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
  }

  /**
   * Utility method where we send a post request with an expected string as the request body, server
   * responds only with a status code.
   */
  public static void serve200ForToolboxPostWithBody(
      final WireMockServer server, final String path, final String body) {
    server.stubFor(
        WireMock.post(path)
            .withHeader(ToolboxHttpHeaders.API_KEY_HEADER, equalTo(API_KEY_PASSWORD.getApiKey()))
            .withHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER, equalTo(API_KEY_PASSWORD.getPassword()))
            .withRequestBody(equalTo(body))
            .willReturn(WireMock.aResponse()
                .withStatus(200)));
  }


  /**
   * Utility method where we send a post request with an expected JSON as the request body, server
   * responds only with a status code.
   */
  public static <T> void serve200ForToolboxPostWithBodyJson(
      final WireMockServer server, final String path, final T jsonObject) {
    server.stubFor(
        WireMock.post(path)
            .withHeader(ToolboxHttpHeaders.API_KEY_HEADER, equalTo(API_KEY_PASSWORD.getApiKey()))
            .withHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER, equalTo(API_KEY_PASSWORD.getPassword()))
            .withRequestBody(equalToJson(toJson(jsonObject)))
            .willReturn(WireMock.aResponse()
                .withStatus(200)));
  }

  /**
   * Utility method to convert objects to JSON. Serialization of Instant classes is customized so that
   * instant is serialized as "epoch_second.nanos". Without this default Instants are serialized to be
   * JSON objects (example of what we do not want: Instant: {"second":value, "nano":value"})
   */
  public static <T> String toJson(final T object) {
    try {
      return objectMapper.writeValueAsString(object);
    } catch (final JsonProcessingException e) {
      throw new RuntimeException("Failed to convert to JSON string: " + object, e);
    }
  }

  /**
   * Sends a service call and simulates a 500 response coming back.
   */
  public static <T> T sendServiceCallToWireMockRespondWith500(final ServiceCallArgs<T> args) {
    return sendServiceCallToWireMock(args, HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  /**
   * Parameter object for 'setupServer' method, contains values needed to specify server
   * call expectations and stubbed return value.
   *
   * @param <T> Http service client response object type.
   */
  @Builder
  public static class ServiceCallArgs<T> {
    @Nonnull
    private final WireMockServer wireMockServer;
    @Nonnull
    private final String expectedRequestPath;
    @Nonnull
    private final List<String> expectedBodyContents;
    @Nonnull
    private final String serverReturnValue;
    @Nonnull
    private final Function<URI, T> serviceCall;
  }

  /**
   * Helper method to set up wiremock server behavior for a 'success' case where the
   * server returns back a JSON response with HTTP 200.
   */
  public static <T> T sendServiceCallToWireMock(final ServiceCallArgs<T> args) {
    return sendServiceCallToWireMock(args, HttpStatus.SC_OK);
  }

  private static <T> T sendServiceCallToWireMock(
      final ServiceCallArgs<T> args,
      final int returnCode) {

    args.wireMockServer.stubFor(
        post(urlEqualTo(args.expectedRequestPath))
            .withHeader(HttpHeaders.ACCEPT, equalTo(HttpClientTesting.CONTENT_TYPE_JSON))
            .willReturn(aResponse()
                .withStatus(returnCode)
                .withHeader(HttpHeaders.CONTENT_TYPE, HttpClientTesting.CONTENT_TYPE_JSON)
                .withBody(args.serverReturnValue)));

    WireMock.configureFor("localhost", args.wireMockServer.port());
    final URI hostUri = URI.create(args.wireMockServer.url(""));

    final T response = args.serviceCall.apply(hostUri);


    RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlMatching(args.expectedRequestPath));

    for (final String content : args.expectedBodyContents) {
      requestPatternBuilder = requestPatternBuilder.withRequestBody(containing(content));
    }

    verify(
        requestPatternBuilder.withHeader(HttpHeaders.CONTENT_TYPE, matching(HttpClientTesting.CONTENT_TYPE_JSON)));

    return response;
  }

  @Builder
  private static final class ErrorHandlingArg<T> {
    @Nonnull
    private final String path;
    @Nonnull
    private final Function<URI, T> serviceCall;
  }


  /**
   * Verifies http client behavior on error cases, eg: communication error, server 500.
   */
  public static <T> void verifyErrorHandling(
      final WireMockServer wireMockServer,
      final String expectedRequestPath,
      final RequestType requestType,
      final Function<URI, T> serviceCall) {
    server500(wireMockServer, expectedRequestPath, requestType, serviceCall);
    faultCases(wireMockServer, expectedRequestPath, requestType, serviceCall);
  }

  /**
   * Enum indicating whether we expect an HTTP POST or GET request.
   */
  public enum RequestType {
    POST, GET;

    private MappingBuilder verifyPath(final String expectedPath) {
      return this == POST ? post(urlEqualTo(expectedPath))
          : get(urlEqualTo(expectedPath));
    }
  }

  private static <T> void server500(
      final WireMockServer wireMockServer,
      final String expectedRequestPath,
      final RequestType requestType,
      final Function<URI, T> serviceCall) {
    givenServer500(wireMockServer, expectedRequestPath, requestType);
    final URI hostUri = configureWireMock(wireMockServer);

    assertThrows(HttpInteractionException.class, () -> serviceCall.apply(hostUri));
  }

  private static URI configureWireMock(final WireMockServer wireMockServer) {
    WireMock.configureFor("localhost", wireMockServer.port());
    return URI.create(wireMockServer.url(""));
  }

  private static void givenServer500(
      final WireMockServer wireMockServer,
      final String expectedRequestPath,
      final RequestType requestType) {
    wireMockServer.stubFor(requestType.verifyPath(expectedRequestPath)
        .withHeader(HttpHeaders.ACCEPT, equalTo(CONTENT_TYPE_JSON))
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)
            .withHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
            .withBody(FAILURE_MESSAGE_FROM_SERVER)));
  }

  /**
   * Verifies http client behavior when communication problems happen.
   */
  private static <T> void faultCases(
      final WireMockServer wireMockServer,
      final String expectedRequestPath,
      final RequestType requestType,
      final Function<URI, T> serviceCall) {
    Arrays.asList(
        // caution, one of the wiremock faults is known to cause a hang in windows, so to aviod that
        // problem do not use the full available list of of wiremock faults
        Fault.EMPTY_RESPONSE,
        Fault.RANDOM_DATA_THEN_CLOSE)
        .forEach(fault -> testFaultHandling(
            wireMockServer, expectedRequestPath, requestType, serviceCall, fault));
  }

  private static <T> void testFaultHandling(
      final WireMockServer wireMockServer,
      final String expectedRequestPath,
      final RequestType requestType,
      final Function<URI, T> serviceCall,
      final Fault fault) {
    givenFaultyConnection(wireMockServer, expectedRequestPath, requestType, fault);
    final URI hostUri = configureWireMock(wireMockServer);

    assertThrows(FeignException.class, () -> serviceCall.apply(hostUri));
  }

  private static void givenFaultyConnection(
      final WireMockServer wireMockServer,
      final String expectedRequestPath,
      final RequestType requestType,
      final Fault fault) {
    wireMockServer.stubFor(requestType.verifyPath(expectedRequestPath)
        .withHeader(HttpHeaders.ACCEPT, equalTo(CONTENT_TYPE_JSON))
        .willReturn(aResponse()
            .withFault(fault)
            .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)
            .withHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
            .withBody("a simulated error occurred")));
  }
}
