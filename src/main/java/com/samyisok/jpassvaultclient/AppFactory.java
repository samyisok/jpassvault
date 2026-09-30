package com.samyisok.jpassvaultclient;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import com.samyisok.jpassvaultclient.controllers.MainController;
import com.samyisok.jpassvaultclient.controllers.OptionsController;
import com.samyisok.jpassvaultclient.controllers.SetupController;
import com.samyisok.jpassvaultclient.controllers.VaultController;
import com.samyisok.jpassvaultclient.crypto.AesCipher;
import com.samyisok.jpassvaultclient.domains.options.Options;
import com.samyisok.jpassvaultclient.domains.options.OptionsLoader;
import com.samyisok.jpassvaultclient.domains.session.Session;
import com.samyisok.jpassvaultclient.domains.vault.Vault;
import com.samyisok.jpassvaultclient.domains.vault.VaultLoader;
import com.samyisok.jpassvaultclient.password.PasswordGenerator;
import com.samyisok.jpassvaultclient.remote.RemoteVault;

/**
 * Composition root: constructs every shared singleton once and hands
 * controllers their dependencies. Replaces the former Spring application
 * context and javafx-weaver controller instantiation.
 */
public class AppFactory {

  private final Options options = new Options();
  private final OptionsLoader optionsLoader = new OptionsLoader(options);
  private final Session session = new Session();
  private final AesCipher aesCipher = new AesCipher(session);
  private final Vault vault = new Vault();
  private final VaultLoader vaultLoader = new VaultLoader(options, vault, aesCipher);
  private final PasswordGenerator passwordGenerator = new PasswordGenerator();
  private final RemoteVault remoteVault = new RemoteVault(options, session, vaultLoader);
  private final StageHolder stageHolder = new StageHolder();
  private final EventPublisher eventPublisher = new EventPublisher(stageHolder);
  private final ViewLoader viewLoader = new ViewLoader(this::getController);
  private final MainListener mainListener =
      new MainListener(stageHolder, vaultLoader, viewLoader, remoteVault, options);
  private final StageInit stageInit = new StageInit(stageHolder, vaultLoader,
      optionsLoader, options, viewLoader, loadApplicationTitle());

  private final Map<Class<?>, Supplier<Object>> controllerFactories = new HashMap<>();
  private final Map<Class<?>, Object> controllers = new HashMap<>();

  public AppFactory() {
    controllerFactories.put(MainController.class,
        () -> new MainController(eventPublisher, vaultLoader, session, remoteVault,
            options));
    controllerFactories.put(OptionsController.class,
        () -> new OptionsController(eventPublisher, options, optionsLoader, stageHolder,
            remoteVault));
    controllerFactories.put(SetupController.class,
        () -> new SetupController(eventPublisher, vaultLoader, session));
    controllerFactories.put(VaultController.class,
        () -> new VaultController(eventPublisher, vault, vaultLoader, passwordGenerator));
  }

  public Object getController(Class<?> type) {
    return controllers.computeIfAbsent(type, this::instantiateController);
  }

  private Object instantiateController(Class<?> type) {
    Supplier<Object> factory = controllerFactories.get(type);
    if (factory == null) {
      throw new IllegalArgumentException("Unknown controller: " + type);
    }
    return factory.get();
  }

  public MainListener mainListener() {
    return mainListener;
  }

  public StageInit stageInit() {
    return stageInit;
  }

  public StageHolder stageHolder() {
    return stageHolder;
  }

  private static String loadApplicationTitle() {
    Properties props = new Properties();
    try (InputStream in =
        AppFactory.class.getResourceAsStream("/application.properties")) {
      if (in != null) {
        props.load(in);
      }
    } catch (IOException e) {
      System.out.println("cant read application.properties: " + e.getMessage());
    }
    return props.getProperty("app.ui.title", "jpassvault");
  }
}
