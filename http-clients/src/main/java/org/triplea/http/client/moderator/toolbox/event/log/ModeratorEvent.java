package org.triplea.http.client.moderator.toolbox.event.log;

import java.time.Instant;

import javax.annotation.Nonnull;

import lombok.Builder;
import lombok.Value;

/**
 * Bean class for transport between server and client. Represents one row of the moderator audit history table.
 */
@Builder
@Value
public class ModeratorEvent {
  @Nonnull
  private final Instant date;
  @Nonnull
  private final String moderatorName;
  @Nonnull
  private final String moderatorAction;
  @Nonnull
  private final String actionTarget;
}
