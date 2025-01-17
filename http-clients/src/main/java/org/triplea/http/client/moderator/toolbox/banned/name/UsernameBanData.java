package org.triplea.http.client.moderator.toolbox.banned.name;

import java.time.Instant;

import javax.annotation.Nonnull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Data object for JSON encoding between server/client. Contains
 * data to view the username bans.
 */
@Builder
@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class UsernameBanData {
  @Nonnull
  final String bannedName;
  @Nonnull
  final Instant banDate;
}
