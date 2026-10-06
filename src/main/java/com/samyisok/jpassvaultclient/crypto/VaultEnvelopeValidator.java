package com.samyisok.jpassvaultclient.crypto;

import java.util.Base64;

/**
 * Validates untrusted envelope metadata before it is allowed to drive key
 * derivation (design D10): a tampered file or hostile sync response must not
 * be able to stall unlocking with an absurd iteration count, a wrong-length
 * salt/IV, or an unknown format version.
 */
final class VaultEnvelopeValidator {

  static final int MAX_ITERATIONS = 5_000_000;
  static final int IV_BYTES = 12;

  private VaultEnvelopeValidator() {
  }

  static void validate(VaultEnvelope envelope) throws EncryptionException {
    if (envelope == null || envelope.getKdf() == null) {
      throw new EncryptionException("missing vault envelope parameters");
    }
    if (envelope.getFormat() != VaultEnvelope.CURRENT_FORMAT) {
      throw new EncryptionException("unsupported vault format: " + envelope.getFormat());
    }
    int iterations = envelope.getKdf().getIterations();
    if (iterations < 1 || iterations > MAX_ITERATIONS) {
      throw new EncryptionException("iteration count out of range: " + iterations);
    }
    if (decodeLength(envelope.getKdf().getSalt()) != VaultKeyDerivation.SALT_BYTES) {
      throw new EncryptionException("invalid salt length");
    }
    if (decodeLength(envelope.getIv()) != IV_BYTES) {
      throw new EncryptionException("invalid IV length");
    }
  }

  private static int decodeLength(String base64) throws EncryptionException {
    if (base64 == null) {
      throw new EncryptionException("missing envelope parameter");
    }
    try {
      return Base64.getDecoder().decode(base64).length;
    } catch (IllegalArgumentException e) {
      throw new EncryptionException("invalid base64 envelope parameter");
    }
  }
}
