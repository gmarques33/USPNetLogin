package br.usp.gmarques.loginuspnet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.xml.sax.InputSource;

import br.usp.gmarques.loginuspnet.db.USPNetLoginDataSource;
import br.usp.gmarques.loginuspnet.http.HttpUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WifiChangeReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();

		Log.v("LoginUSPNet", "Action: " + action);

		if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
			NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			if (info.getDetailedState() == DetailedState.CONNECTED) {
				Log.v("LoginUSPNet", "Conectado");

				WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				WifiInfo wifiInfo = wifiManager.getConnectionInfo();
				if (wifiInfo.getSSID().toUpperCase().contains("ICMC")) {

					Log.d("LoginUSPNet", "Rede ICMC detectada.");

					USPNetLoginDataSource uspNetLoginDataSource = new USPNetLoginDataSource(context);
					uspNetLoginDataSource.open();

					final String httpsURL = "https://1.1.1.1/login.html?redirect=https://www.google.com";

					final List<BasicNameValuePair> nvps = new ArrayList<BasicNameValuePair>();
					nvps.add(new BasicNameValuePair("buttonClicked", "4"));
					nvps.add(new BasicNameValuePair("err_flag", "0"));
					nvps.add(new BasicNameValuePair("err_msg", ""));
					nvps.add(new BasicNameValuePair("info_flag", "0"));
					nvps.add(new BasicNameValuePair("info_msg", ""));
					nvps.add(new BasicNameValuePair("redirect_url",	"https://www.google.com"));
					nvps.add(new BasicNameValuePair("username",	uspNetLoginDataSource.getUsername()));
					nvps.add(new BasicNameValuePair("password",	uspNetLoginDataSource.getPassword()));
					uspNetLoginDataSource.close();

					try {
						sendRequest(httpsURL, nvps);
					} catch (ClientProtocolException e) {
						Log.e("LoginUSPNet",
								"ClientProtocolException: " + e.getMessage());
						Log.e("LoginUSPNet", " " + Log.getStackTraceString(e));
					} catch (IOException e) {
						Log.e("LoginUSPNet", "IOException: " + e.getMessage());
						Log.e("LoginUSPNet", " " + Log.getStackTraceString(e));
					}

				}
			}

		}
	}

	private void sendRequest(String httpsURL, List<BasicNameValuePair> nvps)
			throws ClientProtocolException, IOException {

		HttpClient client = HttpUtils.getNewHttpClient();

		HttpPost httppost = new HttpPost(httpsURL);

		// Bloco de autenticacao
		UrlEncodedFormEntity p_entity;

		p_entity = new UrlEncodedFormEntity(nvps, HTTP.UTF_8);
		httppost.setEntity(p_entity);
		// Enviando a requisição e recebendo a resposta
		HttpResponse response = client.execute(httppost);
		HttpEntity responseEntity = response.getEntity();

		// Tratando a resposta
		InputSource inputSource = new InputSource(responseEntity.getContent());
		BufferedReader in = new BufferedReader(new InputStreamReader(
				inputSource.getByteStream()));

		@SuppressWarnings("unused")
		String inputLine;
		while ((inputLine = in.readLine()) != null) {
			// Recebe a pagina de retorno. Pode ser usado para verificar se
			// obteve sucesso na autenticaco
			// Log.d("LoginUSPNet:", " " + inputLine);
		}
	}
}

/*
 * //Dados USPNet
 * 
 * String httpsURL = "https://gwsc.semfio.usp.br:8001"; nvps.add(new
 * BasicNameValuePair("redirurl", "https://www.google.com")); nvps.add(new
 * BasicNameValuePair("auth_user", uspNetLoginDataSource.getUsername())); nvps.add(new
 * BasicNameValuePair("auth_pass", uspNetLoginDataSource.getPassword())); nvps.add(new
 * BasicNameValuePair("accept", "Continue"));
 */

