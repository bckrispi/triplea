package org.triplea.lobby.server.db.data;

import java.time.Instant;

import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.lobby.server.db.TimestampMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Return data when selecting lobby access history.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AccessLogDaoData {
  public static final String ACCESS_TIME_COLUMN = "access_time";
  public static final String USERNAME_COLUMN = "username";
  public static final String IP_COLUMN = "ip";
  public static final String MAC_COLUMN = "mac";
  public static final String REGISTERED_COLUMN = "registered";

  private Instant accessTime;
  private String username;
  private String ip;
  private String mac;
  private boolean registered;

  /**
   * Returns a JDBI row mapper used to convert results into an instance of this bean object.
   */
  public static RowMapper<AccessLogDaoData> buildResultMapper() {
    return (rs, ctx) -> AccessLogDaoData.builder()
        .accessTime(TimestampMapper.map(rs, ACCESS_TIME_COLUMN))
        .username(rs.getString(USERNAME_COLUMN))
        .ip(rs.getString(IP_COLUMN))
        .mac(rs.getString(MAC_COLUMN))
        .registered(rs.getBoolean(REGISTERED_COLUMN))
        .build();
  }
}
