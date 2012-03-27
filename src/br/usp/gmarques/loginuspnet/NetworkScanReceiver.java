package br.usp.gmarques.loginuspnet;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class NetworkScanReceiver extends BroadcastReceiver {
	private static String TAG = NetworkScanReceiver.class.getName();

	private static long lastCalled = -1;

	private static final int MIN_PERIOD_BTW_CALLS = 10 * 1000;// 10 Seconds

	private static SharedPreferences mPreferences;

	@Override
	public void onReceive(Context context, Intent intent) {
		long now = System.currentTimeMillis();
		
		if (lastCalled == -1 || (now - lastCalled > MIN_PERIOD_BTW_CALLS)) {
			lastCalled = now;

			boolean autoConnectEnabled = getPreferences(context).getBoolean(
					context.getString(R.string.pref_connectionAutoEnable), false);

			if (autoConnectEnabled) {
				WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

				if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
					if (!isAlreadyConnected(wm) && !isAnyPreferedNetworkAvailable(wm)) {
						ScanResult scanResult = getUspNetNetwork(wm);
						if (scanResult != null) {
							WifiConfiguration uspNetNetwork = lookupConfigurationByScanResult(wm.getConfiguredNetworks(),
									scanResult);
							if (uspNetNetwork == null) {
								uspNetNetwork = new WifiConfiguration();
								uspNetNetwork.SSID = '"' + scanResult.SSID + '"';
								uspNetNetwork.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
								uspNetNetwork.status = WifiConfiguration.Status.ENABLED;

								uspNetNetwork.networkId = wm.addNetwork(uspNetNetwork);
								wm.saveConfiguration();
								uspNetNetwork.SSID = '"' + scanResult.SSID + '"';
							}
							wm.enableNetwork(uspNetNetwork.networkId, true);
							Log.d(TAG, "Trying to connect");
						}// No UspNet Signal Available
					} else {
						Log.d(TAG, "Not connecting because a prefered network is available OR it's already connected");
					}
					lastCalled = System.currentTimeMillis();
				}// Not Scanning State
			} // Not Active in preferences
		} else {
			Log.v(TAG, "Events to close, ignoring.");
		}
	}

	private SharedPreferences getPreferences(Context context) {
		if (mPreferences == null) {
			mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		}

		return mPreferences;
	}

	private boolean isAlreadyConnected(WifiManager wm) {
		boolean alreadyConnected = false;
		WifiInfo connectionInfo = wm.getConnectionInfo();
		if (connectionInfo != null) {
			SupplicantState supplicantState = connectionInfo.getSupplicantState();
			if (supplicantState != null) {
				alreadyConnected = supplicantState.equals(SupplicantState.ASSOCIATING)
						|| supplicantState.equals(SupplicantState.ASSOCIATED)
						|| supplicantState.equals(SupplicantState.COMPLETED)
						|| supplicantState.equals(SupplicantState.FOUR_WAY_HANDSHAKE)
						|| supplicantState.equals(SupplicantState.GROUP_HANDSHAKE);
			}
		}

		return alreadyConnected;
	}

	private WifiConfiguration lookupConfigurationByScanResult(List<WifiConfiguration> configuredNetworks,
			ScanResult scanResult) {
		boolean found = false;
		WifiConfiguration wifiConfiguration = null;
		Iterator<WifiConfiguration> it = configuredNetworks.iterator();
		while (!found && it.hasNext()) {
			wifiConfiguration = it.next();
			if (wifiConfiguration.SSID != null) {
				found = wifiConfiguration.SSID.equals(scanResult.SSID);
			}
		}

		if (!found) {
			wifiConfiguration = null;
		}

		return wifiConfiguration;
	}

	private ScanResult getUspNetNetwork(WifiManager wm) {
		ScanResult scanResult = null;
		boolean found = false;

		List<ScanResult> scanResults = wm.getScanResults();
		if (scanResults != null) {
			Iterator<ScanResult> it = scanResults.iterator();
			while (!found && it.hasNext()) {
				scanResult = it.next();
				found = scanResult.SSID.toUpperCase().contains("USP") || scanResult.SSID.toUpperCase().contains("ICMC");
			}
			if (!found) {
				scanResult = null;
			}
		}

		return scanResult;
	}

	private boolean isAnyPreferedNetworkAvailable(WifiManager wm) {
		Set<String> scanResultsKeys = new HashSet<String>();
		boolean found = false;

		List<WifiConfiguration> configuredNetworks = wm.getConfiguredNetworks();
		if (configuredNetworks != null && !configuredNetworks.isEmpty()) {
			List<ScanResult> scanResults = wm.getScanResults();
			if (scanResults != null && !scanResults.isEmpty()) {
				// SSID de todas redes disponiveis
				for (ScanResult scanResult : scanResults) {
					scanResultsKeys.add(scanResult.SSID);
				}

				Iterator<WifiConfiguration> it = configuredNetworks.iterator();

				// Olhar as redes conhecidas
				while (!found && it.hasNext()) {
					WifiConfiguration wifiConfiguration = it.next();
					if (wifiConfiguration.SSID == null) {
						wm.removeNetwork(wifiConfiguration.networkId);
					} else if (!wifiConfiguration.SSID.toUpperCase().contains("USP") && !wifiConfiguration.SSID.toUpperCase().contains("ICMC")) {
						found = scanResultsKeys.contains(wifiConfiguration.SSID);
					}
				}
			}
		}

		return found;
	}
}