package com.samyisok.jpassvaultclient.remote;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import com.samyisok.jpassvaultclient.domains.options.Options;
import com.samyisok.jpassvaultclient.domains.session.Session;
import com.samyisok.jpassvaultclient.domains.vault.VaultLoader;

class RemoteResponseRobustnessUnitTest {

  private RemoteVault remoteVault;

  @BeforeEach
  void setUp() {
    Options options = mock(Options.class);
    when(options.getApiUrl()).thenReturn("https://sync.example.com/");
    when(options.getTokenApi()).thenReturn("token");
    remoteVault = new RemoteVault(options, new Session(), mock(VaultLoader.class));
  }

  @Test
  @DisplayName("a checksum response without a hash is reported as a sync failure")
  void noHashIsSyncFailure() throws Exception {
    withStubbedResponse("{}",
        () -> assertThrows(RemoteException.class, () -> remoteVault.getLastCheckSumHash()));
  }

  @Test
  @DisplayName("a malformed checksum response is not surfaced as an unchecked error")
  void malformedChecksumDoesNotThrowUnchecked() throws Exception {
    withStubbedResponse("{}", () -> assertThrows(RemoteException.class,
        () -> remoteVault.ifRemoteIsEmptyOrMatchesCurrentVault()));
  }

  /** Runs the assertion while a stubbed HttpClient is installed for this call. */
  private void withStubbedResponse(String body, CheckedRunnable assertion) throws Exception {
    try (MockedStatic<HttpClient> http = mockStatic(HttpClient.class)) {
      HttpClient client = mock(HttpClient.class);
      @SuppressWarnings("unchecked")
      HttpResponse<String> response = mock(HttpResponse.class);
      when(response.body()).thenReturn(body);
      http.when(HttpClient::newHttpClient).thenReturn(client);
      doReturn(response).when(client).send(any(), any());
      assertion.run();
    }
  }

  private interface CheckedRunnable {
    void run() throws Exception;
  }
}
