/**
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.n0tice.android;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.google.android.maps.GeoPoint;
import com.n0tice.android.content.LocalBoards;
import com.n0tice.android.content.LocalN0tices;
import com.n0tice.android.post.ApiFactory;
import com.n0tice.android.post.Config;
import com.n0tice.api.client.N0ticeApi;
import com.n0tice.api.client.exceptions.AuthorisationException;
import com.n0tice.api.client.exceptions.BadRequestException;
import com.n0tice.api.client.exceptions.NotAllowedException;
import com.n0tice.api.client.exceptions.NotFoundException;
import com.n0tice.api.client.exceptions.ParsingException;

/**
 * Demonstrates combining a TabHost with a ViewPager to implement a tab UI
 * that switches between tabs and also allows the user to perform horizontal
 * flicks to move between the tabs.
 */
public class TabsPager extends SherlockFragmentActivity {

	public static String TAG = "TabsPager";
	TabHost mTabHost;
	ViewPager  mViewPager;
	TabsAdapter mTabsAdapter;

	public static Location location = null;
	public static Location selectLocation = null;

	//address list for geocoder
	private List<Address> addresses = null;
	private List<String> addressArray;	
	public static String textSearchLocation;
	private String currentLocation;

	public static SharedPreferences n0ticeData;
	private Editor editor;

	LocationManager locMan;

	Boolean swapFragment;

	//location
	private LocationManager locManager;
	private LocationListener locListener;

	private final int LOCATION_UPDATE_INTERVAL_MILLIS = 500;

	public  double geodouble;
	public  int geoint;
	public  GeoPoint myGeoLoc;

	public  double mMyLocationLat;
	public  int mMyLocationGeoLat;

	public  double mMyLocationLon;
	public  int mMyLocationGeoLon;

	private final long RETAIN_GPS_MILLIS = 10000L;

	// True when we have know own location
	public static boolean mHaveLocation = false;

	public static Location lastKnownLoc;

	// True when using GPS for location
	boolean usingGPS = false;

	///Time in millis for the last time GPS reported a location
	private long mLastGpsFixTime = 0L;

	// The last location reported by the network provider. Use this if we can't get a location from GPS	    */
	private Location mNetworkLocation;

	//True if GPS is reporting a location
	private boolean mGpsAvailable;

	//True if the network provider is reporting a location
	private boolean mNetworkAvailable;

	public static N0ticeApi api;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS); 

		setContentView(R.layout.fragment_tabs_pager);
		setTitle(null);

		swapFragment = false;

		//get the location and compass stuff
		locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		selectLocation = new Location("user selection");

		n0ticeData = PreferenceManager.getDefaultSharedPreferences(TabsPager.this);

		Boolean verified = n0ticeData.getBoolean("verified", false);

		if (verified) {
			GetAuthenticatedApi getAuth = new GetAuthenticatedApi();
			getAuth.execute();
		} else {
			api = ApiFactory.getUnauthenticatedApi();
		}

		editor = n0ticeData.edit();
		editor.putBoolean("selectLocation", false);
		editor.commit();

		locMan = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		location = locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Log.i(TAG, "latitude = " + location.getLatitude());
		if (location != null) {
			/** had to set mHaveLocation because the map stopped
			 * working - may have deleted a line accidentally whilst cleaning up?
			 */ 
			mHaveLocation = true;
			GeoCodeLoc gcl = new GeoCodeLoc(location.getLatitude(), location.getLongitude());
			gcl.execute();
		}

		mTabHost = (TabHost)findViewById(android.R.id.tabhost);
		mTabHost.setup();

		mViewPager = (ViewPager)findViewById(R.id.pager);

		mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);
		mTabsAdapter.addTab(mTabHost.newTabSpec("n0tices").setIndicator("N0TICES"),
				LocalN0tices.class, null);
		mTabsAdapter.addTab(mTabHost.newTabSpec("Boards").setIndicator("BOARDS"),
				LocalBoards.class, null);

		if (savedInstanceState != null) {
			mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
		}

		/**
		 * the location listener - uses gps and network location
		 */

		locListener = new LocationListener() {

			public void onLocationChanged(Location location) {

				Log.i(TAG, "OnLocaChanged = " + location.getLatitude());

				if (!n0ticeData.getBoolean("selectLocation", false)) {
					GeoCodeLoc gcl = new GeoCodeLoc(location.getLatitude(), location.getLongitude());
					gcl.execute();
				}
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
					TabsPager.this.location = location;

					mMyLocationLat = location.getLatitude();
					mMyLocationLon = location.getLongitude();

					mMyLocationGeoLat = (int) (mMyLocationLat * 1E6);
					mMyLocationGeoLon = (int) (mMyLocationLon * 1E6);

					myGeoLoc = new GeoPoint(mMyLocationGeoLat, mMyLocationGeoLon);

				}
			}

			public void onProviderDisabled(String arg0) {
			}

			public void onProviderEnabled(String arg0) {
			}

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
		};
	}    

	private class GetAuthenticatedApi extends AsyncTask<Void, Integer, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {
			//check have network connection   
			final ConnectivityManager conMgr =  (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();

			if (activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED) {
				//you are online	

				try {
					Log.i("GetAuthneticated API", "Fetching...");
					api = ApiFactory.getAuthenticatedApi(getApplicationContext());
				} catch (NullPointerException e) {
					Log.i(TAG, "NPE onn DIB");
					e.printStackTrace();
				} catch (NotFoundException e) {
					e.printStackTrace();
				} catch (InvalidKeyException e) {
					e.printStackTrace();
				} catch (ParsingException e) {
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} catch (NotAllowedException e) {
					e.printStackTrace();
				} catch (AuthorisationException e) {
					e.printStackTrace();
				} catch (BadRequestException e) {
					e.printStackTrace();
				}
				Log.i("GetAuthneticated API", "Retrieved");

				return true;
			} else {
				//no network connection
				Toast.makeText(TabsPager.this, "Could not reach network, please check your connection", Toast.LENGTH_LONG).show();
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean success) {

			if (success) {
				Toast.makeText(TabsPager.this, "Logged in", Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(TabsPager.this, "Log in failed", Toast.LENGTH_LONG).show();

			}
		}

	}

	/**
	 * Called when we no longer have a valid location.
	 */
	public void handleUnknownLocation() {
		mHaveLocation = false;

	}


	@Override
	public void onResume() {
		super.onResume();

		Log.i("FTP OnResume", "Resuming");

		// Register for location updates
		locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				LOCATION_UPDATE_INTERVAL_MILLIS, 1, locListener);
		locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
				LOCATION_UPDATE_INTERVAL_MILLIS, 1, locListener);


		if (swapFragment) {
			swapFragments();
			swapFragment = false;
		}

		if (!n0ticeData.getBoolean("selectLocation", false) && location != null) {
			GeoCodeLoc gcl = new GeoCodeLoc(location.getLatitude(), location.getLongitude());
			gcl.execute();
		}
	}

	private void swapFragments() {
		Fragment newFragment1 = new LocalN0tices();
		Fragment newFragment2 = new LocalBoards();
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.replace(R.id.local_notices, newFragment1);
		transaction.replace(R.id.local_boards, newFragment2);
		transaction.commit();

	}

	@Override
	public void onPause() {
		super.onPause();

		Log.i("FTP OnPause", "Pausing");

		//remove the location and compass listeners
		locManager.removeUpdates(locListener);
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();    

		//remove the location and compass listeners
		locManager.removeUpdates(locListener);
	}

	//	@Override
	//	public void onSaveInstanceState(Bundle outState) {
	//		super.onSaveInstanceState(outState);
	//		outState.putSerializable("noticeList", api);
	//	}
	/**
	 * This is a helper class that implements the management of tabs and all
	 * details of connecting a ViewPager with associated TabHost.  It relies on a
	 * trick.  Normally a tab host has a simple API for supplying a View or
	 * Intent that each tab will show.  This is not sufficient for switching
	 * between pages.  So instead we make the content part of the tab host
	 * 0dp high (it is not shown) and the TabsAdapter supplies its own dummy
	 * view to show as the tab content.  It listens to changes in tabs, and takes
	 * care of switch to the correct paged in the ViewPager whenever the selected
	 * tab changes.
	 */
	public static class TabsAdapter extends FragmentPagerAdapter
	implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
		private final Context mContext;
		private final TabHost mTabHost;
		private final ViewPager mViewPager;
		private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

		static final class TabInfo {
			private final String tag;
			private final Class<?> clss;
			private final Bundle args;

			TabInfo(String _tag, Class<?> _class, Bundle _args) {
				tag = _tag;
				clss = _class;
				args = _args;
			}
		}

		static class DummyTabFactory implements TabHost.TabContentFactory {
			private final Context mContext;

			public DummyTabFactory(Context context) {
				mContext = context;
			}

			@Override
			public View createTabContent(String tag) {
				View v = new View(mContext);
				v.setMinimumWidth(0);
				v.setMinimumHeight(0);
				return v;
			}

		}

		public TabsAdapter(FragmentActivity activity, TabHost tabHost, ViewPager pager) {
			super(activity.getSupportFragmentManager());
			mContext = activity;
			mTabHost = tabHost;
			mViewPager = pager;
			mTabHost.setOnTabChangedListener(this);
			mViewPager.setAdapter(this);
			mViewPager.setOnPageChangeListener(this);
		}

		public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {

			tabSpec.setContent(new DummyTabFactory(mContext));

			String tag = tabSpec.getTag();

			View view = prepareTabView(mTabHost.getContext(), R.id.tab_title, R.drawable.n0tice_logo, tag);
			TabInfo info = new TabInfo(tag, clss, args);
			mTabs.add(info);
			tabSpec.setIndicator(view);
			mTabHost.addTab(tabSpec);
			notifyDataSetChanged();

		}

		private static View prepareTabView(Context context, int textId, int drawable, String tag) {
			View view = LayoutInflater.from(context).inflate(R.layout.tab_layout, null);
			TextView tabTitle = (TextView)view.findViewById(textId);
			tabTitle.setText(tag);


			// setting text and image
			// ...
			// Write your own code here
			return view;
		}

		@Override
		public int getCount() {
			return mTabs.size();
		}

		@Override
		public Fragment getItem(int position) {
			TabInfo info = mTabs.get(position);
			return Fragment.instantiate(mContext, info.clss.getName(), info.args);
		}

		@Override
		public void onTabChanged(String tabId) {
			int position = mTabHost.getCurrentTab();
			mViewPager.setCurrentItem(position);
		}

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		}

		@Override
		public void onPageSelected(int position) {
			// Unfortunately when TabHost changes the current tab, it kindly
			// also takes care of putting focus on it when not in touch mode.
			// The jerk.
			// This hack tries to prevent this from pulling focus out of our
			// ViewPager.
			TabWidget widget = mTabHost.getTabWidget();
			int oldFocusability = widget.getDescendantFocusability();
			widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
			mTabHost.setCurrentTab(position);
			widget.setDescendantFocusability(oldFocusability);
		}

		@Override
		public void onPageScrollStateChanged(int state) {
		}
	}


	// Geocoder takes lat and lon values, outputs a name string

	private class GeoCodeLoc extends AsyncTask<URL, Integer, String > {

		Double myLat;
		Double myLon;
		Geocoder gcd = new Geocoder(TabsPager.this);

		public GeoCodeLoc(Double myLat, Double myLon) {
			this.myLat = myLat;
			this.myLon = myLon;
			Log.i(TAG, "GCL latitude = " + myLat);
		}

		@Override
		protected String doInBackground(URL... urls) {
			//check have network connection   
			final ConnectivityManager conMgr =  (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();

			if (activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED) {
				//you are online	

				try {
					addresses = gcd.getFromLocation(myLat, myLon, 1);
				} catch (IOException e) {
					e.printStackTrace();
				}

				if (addresses != null && addresses.size() > 0) {
					addressArray = crunchGeoCode(addresses);
					currentLocation = addressArray.get(0) + ", " + addressArray.get(1);
					textSearchLocation = addressArray.get(0) + ", " + addressArray.get(2);
				}		
			} 
			return currentLocation;
		}

		@Override
		protected void onPostExecute(String result) {

			setTitle(result);
		}

	}



	//takes a list of addresses and generates a place name with no null fields

	private List<String> crunchGeoCode(List <Address> addresses){

		List<String> crunchStr = new ArrayList<String>();
		String loc1 = null;
		String loc2 = null;
		String locCountry = null;

		if (loc1 == null && addresses.get(0).getAddressLine(0) != null)
			loc1 = addresses.get(0).getAddressLine(0);

		if (loc1 == null)
			loc1 = "Failed to load";

		if (loc2 == null && addresses.get(0).getAddressLine(1) != null)
			loc2 = addresses.get(0).getAddressLine(1);

		if (loc2 == null)
			loc2 = "Failed to load";

		if (addresses.get(0).getCountryCode() != null)
			locCountry = addresses.get(0).getCountryCode();

		if (locCountry == null && addresses.get(0).getCountryName() != null)
			locCountry = addresses.get(0).getCountryName();

		if (locCountry == null)
			locCountry = "Failed to load";

		crunchStr.add(0, loc1);
		crunchStr.add(1, loc2);
		crunchStr.add(2, locCountry);

		return crunchStr;		
	}
	// Implement menu button

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case R.id.pick_loc :

			Intent intent = new Intent(TabsPager.this, com.n0tice.android.map.MapControl.class);
			startActivityForResult(intent, 82);
			break;

		case R.id.new_post :
			String userCheck = n0ticeData.getString("username", null);
			String passCheck = n0ticeData.getString("password", null);
			Log.i("FTP user details", "details = " + userCheck + passCheck);

			if (Config.getVerified(this) == false) {
				startActivity(new Intent(this, com.n0tice.android.LogIn.class));
			} else {
				startActivity(new Intent(this, com.n0tice.android.post.ReportActivity.class));
			}

			break;

		case R.id.refresh :
			swapFragments();
			break;

		case R.id.settings :
			startActivityForResult(new Intent(this, com.n0tice.android.Prefs.class), 84);
			break;
		}
		return false;  
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {  
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 82) {
			if (resultCode == Activity.RESULT_OK) {

				Bundle extras = data.getExtras();
				int rLat = extras.getInt("mLat");
				int rLon = extras.getInt("mLon");

				selectLocation.setLatitude(rLat / 1E6);
				selectLocation.setLongitude(rLon / 1E6);
				editor.putBoolean("selectLocation", true);
				editor.commit();

				GeoCodeLoc gcl = new GeoCodeLoc(selectLocation.getLatitude(), selectLocation.getLongitude());
				gcl.execute();

				Log.i("OAR", "QPLAR");

				swapFragment = true;
			}
		} else if (requestCode == 84) {
			startActivity(new Intent(this, com.n0tice.android.LogIn.class));
		}

	}


}