package br.usp.gmarques.loginuspnet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.xml.sax.InputSource;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import br.usp.gmarques.loginuspnet.http.HttpUtils;

public class WifiChangeReceiver extends BroadcastReceiver {

	Context context = null;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		this.context = context;
		
		Log.v("LoginUSPNet", "Action: " + action);

		if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
			NetworkInfo info = (NetworkInfo) intent
					.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			if (info.getDetailedState() == DetailedState.CONNECTED) {
				Log.v("LoginUSPNet", "Conectado");

				WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				WifiInfo wifiInfo = wifiManager.getConnectionInfo();
				if (wifiInfo.getSSID().toUpperCase().contains("USP")) {
					Log.d("LoginUSPNet", "Rede USPNet detectada.");
					new loginThread().execute("USP");
				} else if (wifiInfo.getSSID().toUpperCase().contains("ICMC")) {
					Log.d("LoginUSPNet", "Rede ICMC detectada.");
					new loginThread().execute("ICMC");					
				}
			}

		}
	}

	/**
	 * Envia um POST para a pagina passada no httpsURL com os argumentos passados em nvps
	 * @param httpsURL
	 * @param nvps
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private void sendRequest(String httpsURL, List<BasicNameValuePair> nvps)
			throws ClientProtocolException, IOException {

		HttpClient client = HttpUtils.getNewHttpClient();
		HttpPost httppost = new HttpPost(httpsURL);

		// Bloco de autenticacao
		UrlEncodedFormEntity p_entity;

		p_entity = new UrlEncodedFormEntity(nvps, HTTP.UTF_8);
		httppost.setEntity(p_entity);
		// Enviando a requisicao e recebendo a resposta
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
	
	/**
	 * 
	 * @return A URL para fazer login
	 */
	private String findPostURL(){
		
		String path = "http://www.usp.br"; //Pagina aleatoria para verificar o redirecionamento
    	String resultPage;
    	String resultURL = "";
    	URL url;
    	URLConnection con;
    	final char[] buffer;
    	StringBuilder out;
    	Reader in;
    	int urlIndex;
    	
    	try {
			url = new URL(path);
			con = url.openConnection();
			trustAllCertificates();
			
			//Tenta conectar em alguma pagina e guarda o retorno em uma string.
			//Se a pagina de retorno for uma pagina de login da USPNet procura a url do POST
			
			buffer = new char[0x10000];
			out = new StringBuilder();
			in = new InputStreamReader(openConnectionCheckRedirects(con), "UTF-8");
			int read;
			do {
			  read = in.read(buffer, 0, buffer.length);
			  if (read>0) {
			    out.append(buffer, 0, read);
			  }
			} while (read>=0);
			resultPage = out.toString();
			
			urlIndex = resultPage.toUpperCase().lastIndexOf("ACTION=\"");
			if(urlIndex != -1){
				urlIndex += 8;
				for(;urlIndex < resultPage.length() && resultPage.charAt(urlIndex) != '"'; urlIndex++){
					resultURL += resultPage.charAt(urlIndex);
				}
			}else{
				resultURL = "";
			}
		} catch (MalformedURLException e) {
			Log.e("MalformedURLException", "Message: " + e.getMessage());
		} catch (SocketTimeoutException e) {
			Log.e("SocketTimeoutException", "Message: " + e.getMessage());
		} catch (IOException e) {
			Log.e("IOExceptionNet", "Message: " + e.getMessage());
		} catch (KeyManagementException e) {
			Log.e("KeyManagementException", "Message: " + e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			Log.e("NoSuchAlgorithmException", "Message: " + e.getMessage());
		}
    	Log.d("LoginUSPNet", "URL: " + resultURL);
		return resultURL;
	}

	/**
	 * Abre uma conexão e aceita redirecionar http para https
	 * Usado para obter a pagina de login da uspnet
	 * @param c
	 * @return
	 * @throws IOException
	 */
	private InputStream openConnectionCheckRedirects(URLConnection c) throws IOException 
	{
	   boolean redir;
	   int redirects = 0;
	   InputStream in = null;
	   do 
	   {
	      if (c instanceof HttpURLConnection) 
	      {
	         ((HttpURLConnection) c).setInstanceFollowRedirects(false);
	      }
	      // We want to open the input stream before getting headers
	      // because getHeaderField() et al swallow IOExceptions.
	      in = c.getInputStream(); 
	      redir = false; 
	      if (c instanceof HttpURLConnection) 
	      {
	         HttpURLConnection http = (HttpURLConnection) c;
	         int stat = http.getResponseCode();
	         if (stat >= 300 && stat <= 307 && stat != 306 &&
	            stat != HttpURLConnection.HTTP_NOT_MODIFIED) 
	         {
	            URL base = http.getURL();
	            String loc = http.getHeaderField("Location");
	            URL target = null;
	            if (loc != null) 
	            {
	               target = new URL(base, loc);
	            }
	            http.disconnect();
	            // Redirection should be allowed only for HTTP and HTTPS
	            // and should be limited to 5 redirections at most.
	            if (target == null || !(target.getProtocol().equals("http")
	               || target.getProtocol().equals("https"))
	               || redirects >= 5)
	            {
	               throw new SecurityException("illegal URL redirect");
	            }
	            redir = true;
	            c = target.openConnection();
	            redirects++;
	         }
	      }
	   } 
	   while (redir);
	   return in;
	}

	/**
	 * Desabilita a checagem de certificado para conexoes ssl.
	 * A pagina da uspnet não tem um certificado valido.
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	private void trustAllCertificates() throws NoSuchAlgorithmException, KeyManagementException{
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}
				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}
				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			}
		};

		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};

		// Install the all-trusting host verifier
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	}

	/**
	 * Faz o login em uma thread separada para nao bloquear a thread principal com conexao de rede.
	 * @author Giovanni
	 *
	 */
	private class loginThread extends AsyncTask<String, Void, Void> {

		protected Void doInBackground(String... id) {
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			final List<BasicNameValuePair> nvps = new ArrayList<BasicNameValuePair>();
			
			if(id[0].toUpperCase().equals("USP")){

				String httpsURL = "";
				//Procura a URL de login na pagina e envia os dados.
				//Deve funcionar em todos os campi da usp.
				
				httpsURL = findPostURL();

				if(!httpsURL.equals("")){
					nvps.add(new BasicNameValuePair("redirurl",	"https://www.google.com"));
					nvps.add(new BasicNameValuePair("auth_user", preferences.getString(context.getString(R.string.pref_username),"")));
					nvps.add(new BasicNameValuePair("auth_pass", preferences.getString(context.getString(R.string.pref_password),"")));
					nvps.add(new BasicNameValuePair("accept", "Continue"));
	
					try {
						sendRequest(httpsURL, nvps);
					} catch (ClientProtocolException e) {
						Log.e("LoginUSPNet", "ClientProtocolException while connecting to " + id[0] + " Message: "+ e.getMessage());
						Log.e("LoginUSPNet", " " + Log.getStackTraceString(e));
					} catch (IOException e) {
						Log.e("LoginUSPNet", "IOException while connecting to " + id[0] + " Message: "+ e.getMessage());
						Log.e("LoginUSPNet", " " + Log.getStackTraceString(e));
					}
				}

			} else if(id[0].toUpperCase().equals("ICMC")){
				final String httpsURL = "https://1.1.1.1/login.html?redirect=https://www.google.com";

				nvps.add(new BasicNameValuePair("buttonClicked", "4"));
				nvps.add(new BasicNameValuePair("err_flag", "0"));
				nvps.add(new BasicNameValuePair("err_msg", ""));
				nvps.add(new BasicNameValuePair("info_flag", "0"));
				nvps.add(new BasicNameValuePair("info_msg", ""));
				nvps.add(new BasicNameValuePair("redirect_url",	"https://www.google.com"));
				nvps.add(new BasicNameValuePair("username", preferences.getString(context.getString(R.string.pref_username),"")));
				nvps.add(new BasicNameValuePair("password", preferences.getString(context.getString(R.string.pref_password),"")));

				try {
					sendRequest(httpsURL, nvps);
				} catch (ClientProtocolException e) {
					Log.e("LoginUSPNet", "ClientProtocolException while connecting to " + id[0] + " Message: "+ e.getMessage());
					Log.e("LoginUSPNet", " " + Log.getStackTraceString(e));
				} catch (IOException e) {
					Log.e("LoginUSPNet", "IOException while connecting to " + id[0] + " Message: "+ e.getMessage());
					Log.e("LoginUSPNet", " " + Log.getStackTraceString(e));
				}
				
			}
			
			return null;
		}
	}

}