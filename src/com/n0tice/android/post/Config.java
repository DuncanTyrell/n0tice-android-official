/**   Copyright 2012 Tony McCrae, Eel Pie Consulting
 * 
 * Including amends by Tyrell Mobile, as marked

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

package com.n0tice.android.post;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Config {
	
	public static String getUsername(Context context) {
		PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString("username", null);
	}
	
	public static String getPassword(Context context) {
		PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString("password", null);
	}

	/** Added by Tyrell Mobile
	 * 
	 * @param context
	 * @return
	 */
	public static Boolean getVerified(Context context) {
		PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getBoolean("verified", false);
	}

	public static void setVerified(Context context, Boolean value) {
		PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.edit().putBoolean("verified", value);
	}

	public static String getApiUrl() {
		return "https://n0ticeapis.com/2";
	}
	
	/**	The real site */
	public static String getConsumerKey() {
		return "b9c64d247cee72ed06b7bed4dd6d6608";
	}
	
	public static String getConsumerSecret() {
		return "6fac6be141e7303d69dbffbd8d2afd38";
	}
	
	/**	The dev site */
//	public static String getConsumerKey() {
//		return "dbb3ff6e969fcec70929f08404a52138";
//	}
//	
//	public static String getConsumerSecret() {
//		return "c783cb84b1368e986d6f6552c6255f09";
//	}
	
}
