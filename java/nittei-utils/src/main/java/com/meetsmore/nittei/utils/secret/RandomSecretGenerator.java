package com.meetsmore.nittei.utils.secret;

import java.security.SecureRandom;

public final class RandomSecretGenerator {

  private static final String CHARSET =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  private static final SecureRandom RANDOM = new SecureRandom();

  private RandomSecretGenerator() {}

  public static String createRandomSecret(int secretLen) {
    StringBuilder value = new StringBuilder(secretLen);
    for (int i = 0; i < secretLen; i++) {
      int idx = RANDOM.nextInt(CHARSET.length());
      value.append(CHARSET.charAt(idx));
    }
    return value.toString();
  }
}
