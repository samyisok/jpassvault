package com.samyisok.jpassvaultclient.crypto;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.samyisok.jpassvaultclient.domains.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AesCipher {
  private final static String CIPHERMODE = "AES/GCM/NoPadding";
  private final static int GCM_IV_LENGTH = 12;
  private final static int GCM_TAG_LENGTH = 16;


  @Autowired
  Session session;

  public SecretKeySpec getKey() {
    SecretKeySpec secretKey = null;
    String myKey = session.getPasswordVault();
    byte[] key;
    try {
      key = myKey.getBytes("UTF-8");
      MessageDigest sha = MessageDigest.getInstance("SHA3-256");
      key = sha.digest(key);
      key = Arrays.copyOf(key, 32);
      System.out.println(key.toString());
      secretKey = new SecretKeySpec(key, "AES");
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
      System.out.println("Error while encrypting: " + e.toString());
      e.printStackTrace();
    }
    return secretKey;
  }

  public String encrypt(String strToEncrypt) throws Exception {
    byte[] iv = new byte[GCM_IV_LENGTH];
    GCMParameterSpec ivSpec = new GCMParameterSpec(GCM_TAG_LENGTH * Byte.SIZE, iv);
    Cipher cipher = Cipher.getInstance(CIPHERMODE);
    cipher.init(Cipher.ENCRYPT_MODE, getKey(), ivSpec);
    byte[] ciphertext = cipher.doFinal(strToEncrypt.getBytes("UTF-8"));
    byte[] encrypted = new byte[iv.length + ciphertext.length];
    System.arraycopy(iv, 0, encrypted, 0, iv.length);
    System.arraycopy(ciphertext, 0, encrypted, iv.length, ciphertext.length);

    return Base64.getEncoder().encodeToString(encrypted);
  }

  public String decrypt(String strToDecrypt) throws Exception {
    byte[] decoded = Base64.getDecoder().decode(strToDecrypt);
    byte[] iv = Arrays.copyOfRange(decoded, 0, GCM_IV_LENGTH);
    Cipher cipher = Cipher.getInstance(CIPHERMODE);
    GCMParameterSpec ivSpec = new GCMParameterSpec(GCM_TAG_LENGTH * Byte.SIZE, iv);
    cipher.init(Cipher.DECRYPT_MODE, getKey(), ivSpec);

    byte[] ciphertext =
        cipher.doFinal(decoded, GCM_IV_LENGTH, decoded.length - GCM_IV_LENGTH);
    String cryptedString = new String(ciphertext, "UTF8");

    return cryptedString;
  }

}
