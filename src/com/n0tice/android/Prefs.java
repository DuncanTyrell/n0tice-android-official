/**   Copyright 2012 Tyrell Mobile

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. 
 */

package com.n0tice.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

public class Prefs extends SherlockPreferenceActivity {

	private OnSharedPreferenceChangeListener prefsListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		SharedPreferences prefs = 
				PreferenceManager.getDefaultSharedPreferences(this);

		prefsListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
				// Implementation
				Log.i("Preferences", prefs.toString() + key);

				if (key.equals("username")) {
					Log.i("Preferences", "Prefs entry = " + key);
					prefs.edit().putBoolean("verified", false).commit();
				} else if (key.equals("password")) {
					Log.i("Preferences", "Prefs entry = " + key);
					prefs.edit().putBoolean("verified", false).commit();
				}
			}
		};
		prefs.registerOnSharedPreferenceChangeListener(prefsListener);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case android.R.id.home :
			NavUtils.navigateUpTo(this, new Intent(this, com.n0tice.android.TabsPager.class));
			break;
		}
		return false;  
	}
}