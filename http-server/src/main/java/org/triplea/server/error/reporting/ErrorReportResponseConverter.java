package org.triplea.server.error.reporting;

import java.util.function.Function;

import org.triplea.http.client.error.report.ErrorUploadResponse;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;

import com.google.common.base.Strings;

/**
 * Converts a response from Github.com by our http-server into a response
 * object we can send back to the TripleA game-client.
 */
public class ErrorReportResponseConverter
    implements Function<CreateIssueResponse, ErrorUploadResponse> {

  @Override
  public ErrorUploadResponse apply(final CreateIssueResponse response) {
    return ErrorUploadResponse.builder()
        .githubIssueLink(extractLink(response))
        .build();
  }


  private static String extractLink(final CreateIssueResponse response) {
    final String urlInResponse = response.getHtmlUrl();

    if (Strings.emptyToNull(urlInResponse) == null) {
      throw new CreateErrorReportException("Error report link missing from server response");
    }
    return urlInResponse;

  }
}
