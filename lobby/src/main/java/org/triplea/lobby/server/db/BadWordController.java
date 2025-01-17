package org.triplea.lobby.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;

import lombok.AllArgsConstructor;

/**
 * Utility class to create/read/delete bad words (there is no update).
 */
@AllArgsConstructor
final class BadWordController implements BadWordDao {
  private final Supplier<Connection> connection;

  @Override
  public boolean containsBadWord(final String testString) {
    if (testString.isEmpty()) {
      return false;
    }

    // Query to count if at least one bad word value is contained in the testString.
    final String sql =
        "select count(bw.word) "
            + "from bad_word bw "
            + "where lower(?) like '%' || lower(bw.word) || '%'";

    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement(sql)) {

      ps.setString(1, testString);

      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getInt(1) > 0;
      }
    } catch (final SQLException e) {
      throw new DatabaseException("Error reading bad words", e);
    }
  }
}
