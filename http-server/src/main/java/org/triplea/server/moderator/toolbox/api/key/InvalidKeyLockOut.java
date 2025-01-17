package org.triplea.server.moderator.toolbox.api.key;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Preconditions;

import lombok.Builder;
import lombok.extern.java.Log;

/**
 * Class to determine if API key validation is in a 'lock-out' mode where we will refuse to validate
 * API keys and will refuse the request. This is intended to prevent brute-force attacks from attempting
 * to guess an API key.
 */
@Log
public class InvalidKeyLockOut {

  @Nonnull
  private final InvalidKeyCache invalidKeyCache;
  @Nonnull
  private final Integer maxTotalFails;
  @Nonnull
  private final Integer maxFailsByIpAddress;
  @Nonnull
  private final Boolean production;

  @Builder
  public InvalidKeyLockOut(
      @Nonnull final InvalidKeyCache invalidKeyCache,
      final int maxTotalFails,
      final int maxFailsByIpAddress,
      final boolean production) {
    Preconditions.checkNotNull(invalidKeyCache);
    Preconditions.checkArgument(maxTotalFails >= maxFailsByIpAddress);
    Preconditions.checkArgument(maxFailsByIpAddress > 0);
    this.invalidKeyCache = invalidKeyCache;
    this.maxTotalFails = maxTotalFails;
    this.maxFailsByIpAddress = maxFailsByIpAddress;
    this.production = production;
  }

  /**
   * Returns true if no attempts should be made to validate the API key in a given request.
   * Returns false to indicate there is no lockout and if we can continue further
   * with a database lookup to validate an API key.
   *
   * @param request Request containing moderator api key as a header.
   */
  public boolean isLockedOut(final HttpServletRequest request) {
    if (invalidKeyCache.getCount(request) >= maxFailsByIpAddress
        || invalidKeyCache.totalSum() >= maxTotalFails) {
      log.warning("Request for API key validation by: " + request.getRemoteHost() + " is locked out");
      return true;
    }
    return false;
  }

  public void recordInvalid(final HttpServletRequest request) {
    invalidKeyCache.increment(request);
  }

  /**
   * May only be used in non-prod, when called resets the lockout cache. This is to enable testing.
   */
  public synchronized void clearLockouts(final HttpServletRequest request) {
    if (production) {
      // if accessed in production, not allowed, immediately lock out.
      for (int i = 0; i < maxFailsByIpAddress; i++) {
        invalidKeyCache.increment(request);
      }
    }
    Preconditions.checkArgument(!production);
    invalidKeyCache.clear();
  }
}
