package org.triplea.lobby.server.db.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserBanDaoDataTest {
  private static final Instant NOW = Instant.now();
  private static final Timestamp timestamp = Timestamp.from(NOW);

  private static final Instant YESTERDAY = Instant.now().minus(1, ChronoUnit.DAYS);
  private static final Timestamp yesterdayTimestamp = Timestamp.from(YESTERDAY);

  private static final String MAC = "Parrots fall on beauty at port royal!";
  private static final String IP = "Yo-ho-ho! Pieces o' amnesty are forever coal-black.";
  private static final String PUBLIC_ID = "Punishment is a black sun.";
  private static final String USERNAME = "Malaria is a small anchor.";

  @Mock
  private ResultSet resultSet;

  @Test
  void buildResultMapper() throws Exception {
    when(resultSet.getTimestamp(eq(UserBanDaoData.BAN_EXPIRY_COLUMN), any(Calendar.class)))
        .thenReturn(timestamp);
    when(resultSet.getTimestamp(eq(UserBanDaoData.DATE_CREATED_COLUMN), any(Calendar.class)))
        .thenReturn(yesterdayTimestamp);
    when(resultSet.getString(UserBanDaoData.HASHED_MAC_COLUMN)).thenReturn(MAC);
    when(resultSet.getString(UserBanDaoData.IP_COLUMN)).thenReturn(IP);
    when(resultSet.getString(UserBanDaoData.PUBLIC_ID_COLUMN)).thenReturn(PUBLIC_ID);
    when(resultSet.getString(UserBanDaoData.USERNAME_COLUMN)).thenReturn(USERNAME);

    final UserBanDaoData result = UserBanDaoData.buildResultMapper().map(resultSet, null);

    assertThat(result.getBanExpiry(), is(NOW));
    assertThat(result.getDateCreated(), is(YESTERDAY));
    assertThat(result.getHashedMac(), is(MAC));
    assertThat(result.getIp(), is(IP));
    assertThat(result.getPublicBanId(), is(PUBLIC_ID));
    assertThat(result.getUsername(), is(USERNAME));
  }
}
