package com.samyisok.jpassvaultclient.models;

import java.util.HashMap;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class Vault extends HashMap<String, VaultContainer> {


  public void save() {
    // TODO Auto-generated method stub
  }

  @PostConstruct
  public void init() {
    // Read file or something
    VaultContainer v1 = new VaultContainer("password", "login");
    VaultContainer v2 = new VaultContainer("password2", "login2");
    this.put("Site1", v1);
    this.put("Site2", v2);
  }
}
