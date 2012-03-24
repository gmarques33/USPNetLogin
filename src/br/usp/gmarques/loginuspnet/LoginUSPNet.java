package br.usp.gmarques.loginuspnet;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class LoginUSPNet extends PreferenceActivity implements OnSharedPreferenceChangeListener{
		
	private EditTextPreference username = null;
	private SharedPreferences preferences = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.preferences);
		
		this.preferences = PreferenceManager.getDefaultSharedPreferences(this);
		this.username = (EditTextPreference)getPreferenceScreen().findPreference(this.getString(R.string.pref_username));
		
	}
	
	@Override
	protected void onResume(){
		super.onResume();

		this.username.setSummary(this.preferences.getString(this.getString(R.string.pref_username), ""));
		
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		
	}
	
	@Override
	protected void onPause(){
		super.onPause();

		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);    
	}
	
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    	if (key.equals(this.getString(R.string.pref_username))) {
    		this.username.setSummary(sharedPreferences.getString(this.getString(R.string.pref_username), ""));
        }
    }
    
}
