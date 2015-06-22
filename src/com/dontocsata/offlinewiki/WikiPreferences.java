package com.dontocsata.offlinewiki;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class WikiPreferences extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.peferences);
	}
}
