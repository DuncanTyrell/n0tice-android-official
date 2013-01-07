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

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.SystemClock;

public class LocProvider implements LocationListener{

	public static double mMyLocationLat;
	public static double mMyLocationLon;
	public static float mOrientation;
	private final long RETAIN_GPS_MILLIS = 60000L;

	/**
	 * True when we have own location
	 */     
	static boolean mHaveLocation = false;

	/**
	 * True when using GPS for location
	 */
	boolean usingGPS = false;

	/**
	 * Time in millis for the last time GPS reported a location
	 */
	private long mLastGpsFixTime = 0L;

	/**
	 * The last location reported by the network provider. Use this if we can't get a location from
	 * GPS
	 */
	private Location mNetworkLocation;

	/**
	 * True if GPS is reporting a location
	 */
	private boolean mGpsAvailable;

	/**
	 * True if the network provider is reporting a location
	 */
	private boolean mNetworkAvailable;

	public static float[] mValues;

	int center;
	int radius;

	public void onLocationChanged(Location location) {
		if (!mHaveLocation) {
			mHaveLocation = true;
		}

		final long now = SystemClock.uptimeMillis();
		boolean useLocation = false;
		final String provider = location.getProvider();
		if (LocationManager.GPS_PROVIDER.equals(provider)) {
			// Use GPS if available
			usingGPS = true;
			mLastGpsFixTime = SystemClock.uptimeMillis();
			useLocation = true;
		} else if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
			// Use network provider if GPS is getting stale

			useLocation = now - mLastGpsFixTime > RETAIN_GPS_MILLIS;
			if (mNetworkLocation == null) {
				mNetworkLocation = new Location(location);
			} else {
				mNetworkLocation.set(location);
				usingGPS = false;
			}

			mLastGpsFixTime = 0L;
		}
		if (useLocation) {
			mMyLocationLat = location.getLatitude();
			mMyLocationLon = location.getLongitude();
		}
	}

	public void onProviderDisabled(String provider) {        
	}

	public void onProviderEnabled(String provider) { 
	}

	/**
	 * Called when a location provider has changed its availability.
	 * 
	 * @see android.location.LocationListener#onStatusChanged(java.lang.String, int, android.os.Bundle)
	 */
	public void onStatusChanged(String provider, int status, Bundle extras) { 

		if (LocationManager.GPS_PROVIDER.equals(provider)) {
			switch (status) {
			case LocationProvider.AVAILABLE:
				mGpsAvailable = true;
				break;
			case LocationProvider.OUT_OF_SERVICE:
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				mGpsAvailable = false;

				if (mNetworkLocation != null && mNetworkAvailable) {
					// Fallback to network location
					mLastGpsFixTime = 0L;
					onLocationChanged(mNetworkLocation);
				} else {
					handleUnknownLocation();
				}

				break;
			}

		} else if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
			switch (status) {
			case LocationProvider.AVAILABLE:
				mNetworkAvailable = true;
				break;
			case LocationProvider.OUT_OF_SERVICE:
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				mNetworkAvailable = false;

				if (!mGpsAvailable) {
					handleUnknownLocation();
				}
				break;
			}
		}
	}

	/**
	 * Called when we no longer have a valid location.
	 */
	public void handleUnknownLocation() {
		mHaveLocation = false;

	}

	public LocProvider(){

	}

	public Location getLocation() {
		return mNetworkLocation;

	}
}
