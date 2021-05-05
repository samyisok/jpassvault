package com.samyisok.jpassvaultclient.remote;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;
import com.google.gson.Gson;
import com.samyisok.jpassvaultclient.domains.options.Options;
import com.samyisok.jpassvaultclient.domains.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RemoteVault implements RemotableVault {
  final String filesPath = "files";
  final String lastPath = "files/last";
  final String checkPath = "check";


  @Autowired
  Options options;

  @Autowired
  Session Session;

  @Override
  public void load() {
    // TODO Auto-generated method stub
    // try {
    // getRequest();
    // } catch (RemoteException | IOException | InterruptedException
    // | URISyntaxException e) {
    // // TODO Auto-generated catch block
    // System.out.println("Catch error" + e.getMessage());
    // e.printStackTrace();
    // }

  }

  @Override
  public void save() {
    // TODO Auto-generated method stub

  }

  URI getUri() throws URISyntaxException {
    return new URI((options.getApiUrl() + lastPath));
  }


  String encodeBase64(String string) {
    return Base64.getEncoder().encodeToString(string.getBytes());
  }

  String decodeBase64(String string) {
    return Base64.getDecoder().decode(string).toString();
  }


  public boolean checkHostAndToken(String host, String token)
      throws RemoteException, IOException, InterruptedException, URISyntaxException {
    URI url = new URI((host + checkPath));
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


    // use the client to send the request
    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
    System.out
        .println("RESPONSE: " + response.toString() + ":" + response.body().toString());
    return response.body().toString();
  }


}
