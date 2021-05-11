package com.samyisok.jpassvaultclient.remote;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import com.google.gson.Gson;
import com.samyisok.jpassvaultclient.crypto.EncryptionException;
import com.samyisok.jpassvaultclient.domains.options.Options;
import com.samyisok.jpassvaultclient.domains.session.Session;
import com.samyisok.jpassvaultclient.domains.vault.MergeVaultException;
import com.samyisok.jpassvaultclient.domains.vault.VaultLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RemoteVault implements RemotableVault {
  final static String FILES_PATH = "files";
  final static String LAST_PATH = "files/last";
  final static String CHECK_PATH = "check";
  final static String HASH_PATH = "files/last/checksum";


  @Autowired
  Options options;

  @Autowired
  Session Session;

  @Autowired
  VaultLoader vaultLoader;

  @Override
  public void load()
      throws URISyntaxException, RemoteException, MergeVaultException {
    try {
      if (ifRemoteIsEmptyOrMatchesCurrentVault()) {
        return;
      }
    } catch (IOException | InterruptedException e1) {
      throw new RemoteException("Error when check remote db");
    } catch (EncryptionException e) {
      // TODO log it
      e.printStackTrace();
    }

    URI url = new URI((options.getApiUrl() + LAST_PATH));
    try {
      String body = getRequest(url.toString(), options.getTokenApi());
      Gson g = new Gson();
      RemoteVaultResponseScheme vaultResponse =
          g.fromJson(body, RemoteVaultResponseScheme.class);
      String file = vaultResponse.getFile();

      vaultLoader.merge(file);
    } catch (IOException | InterruptedException e) {
      throw new RemoteException("Error when loading remote db");
    }
  }

  @Override
  public void save() throws URISyntaxException, RemoteException, EncryptionException {
    if( ifChecksumMatches() ){
      return;
    }

    try {
      URI url = new URI((options.getApiUrl() + FILES_PATH));
      Gson g = new Gson();
      String encryptedDb = vaultLoader.getCurrentEncryptedJsonDb();
      Map<String, String> singletonMap = Map.of("file", encryptedDb);
      String payload = g.toJson(singletonMap);

      postRequest(url.toString(), options.getTokenApi(), payload);
    } catch (IOException | InterruptedException e) {
      throw new RemoteException("Error when uploading remote db");
    }
  }

  URI getUri() throws URISyntaxException {
    return new URI((options.getApiUrl() + LAST_PATH));
  }

  boolean ifRemoteIsEmptyOrMatchesCurrentVault() throws EncryptionException,
      RemoteException, IOException, InterruptedException, URISyntaxException {
    String remoteChecksum = getLastCheckSumHash();

    if (remoteChecksum.isEmpty()){
      return true;
    }

    if (remoteChecksum.equals(vaultLoader.getVaultEncryptCheckSum())){
      return true;
    }

    return false;
  }

  boolean ifChecksumMatches() {
    String remoteChecksum;
    String localChecksum;

    try {
      remoteChecksum = getLastCheckSumHash();
      localChecksum = vaultLoader.getVaultEncryptCheckSum();
    } catch (RemoteException | IOException | InterruptedException
        | URISyntaxException | EncryptionException e) {
      return false;
    }

    return localChecksum.equals(remoteChecksum);
  }

  String getLastCheckSumHash()
      throws RemoteException, IOException, InterruptedException, URISyntaxException {
    URI url = new URI((options.getApiUrl() + HASH_PATH));
    String body = getRequest(url.toString(), options.getTokenApi());
    Gson g = new Gson();
    String hash;
    try {
      HashScheme hashResponse = g.fromJson(body, HashScheme.class);
      hash = hashResponse.getHash();
    } catch (Exception e) {
      throw new RemoteException("invalid response");
    }

    return hash;
  }

  @Override
  public boolean isAvailible() {
    try {
      return checkHostAndToken(options.getApiUrl(), options.getTokenApi());
    } catch (RemoteException | IOException | InterruptedException
        | URISyntaxException e) {
      return false;
    }
  }

  @Override
  public boolean checkHostAndToken(String host, String token)
      throws RemoteException, IOException, InterruptedException, URISyntaxException {
    URI url = new URI((host + CHECK_PATH));
    String body = getRequest(url.toString(), token);
    Gson g = new Gson();
    boolean status = false;
    try {
      CheckScheme checkResponse = g.fromJson(body, CheckScheme.class);
      status = checkResponse.isValid();
    } catch (Exception e) {
      throw new RemoteException("invalid response");
    }

    return status;
  }

  String postRequest(String host, String token, String payload) throws RemoteException,
      IOException, InterruptedException {


    HttpClient client = HttpClient.newHttpClient();

    if (host.isEmpty() || token.isEmpty()) {
      throw new RemoteException("Api or token does not init");
    }

    HttpRequest request = HttpRequest.newBuilder(URI.create(host))
        .header("accept", "application/json")
        .header("content-type", "application/json")
        .header("token", token)
        .POST(HttpRequest.BodyPublishers.ofString(payload))
        .build();

    //TODO log it

    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());


    return response.body().toString();
  }

  String getRequest(String host, String token)
      throws RemoteException, IOException, InterruptedException,
      URISyntaxException {

    HttpClient client = HttpClient.newHttpClient();

    if (host.isEmpty() || token.isEmpty()) {
      throw new RemoteException("Api or token does not init");
    }

    HttpRequest request = HttpRequest.newBuilder(URI.create(host))
        .header("accept", "application/json")
        .header("token", token)
        .build();

    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
    //TODO log it

    return response.body().toString();
  }
}
