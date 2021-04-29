package com.samyisok.jpassvaultclient.password;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class PasswordGenerator {
  private int maxChars = 14;
  private String lowerCharSet = "abcdefghijkmnpqrstuvwxyz";
  private String upperCharSet = "ABCDEFGHKLMNOPQRSTUVWXYZ";
  private String numberCharSet = "0123456789";
  private String specialCharSet = "#$%@";
  private ArrayList<String> setList =
      new ArrayList<String>(
          Arrays.asList(lowerCharSet, upperCharSet, numberCharSet, specialCharSet));

  public String getPassword() {
    SecureRandom random = new SecureRandom();
    StringBuilder sb = new StringBuilder();

    String charSet = setList.stream().collect(Collectors.joining());

    while (sb.length() < maxChars + 1) {
      int randomIndex = random.nextInt(charSet.length());
      sb.append(charSet.charAt(randomIndex));
      if (sb.length() == maxChars) {
        if (!containsAllCharSets(sb.toString())) {
          sb.delete(0, sb.length());
        }
      }
    }

    return sb.toString();
  }

  boolean containsAny(String password, String set) {
    return Stream.of(set.split("")).anyMatch(c -> password.contains(c));
  }

  boolean containsAllCharSets(String password) {
    return setList.stream().allMatch(set -> containsAny(password, set));
  }
}
