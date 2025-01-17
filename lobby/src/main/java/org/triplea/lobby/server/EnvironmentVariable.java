package org.triplea.lobby.server;

import java.util.Optional;

import lombok.AllArgsConstructor;

/** Class that represent OS environment variable keys with default values. */
@AllArgsConstructor
public enum EnvironmentVariable {
  PORT("3304");

  private final String defaultValue;

  public String getValue() {
    return Optional.ofNullable(System.getenv(name())).orElse(defaultValue);
  }
}
