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

package com.n0tice.android.map;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockMapActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.n0tice.android.R;
import com.n0tice.android.TabsPager;

public class MapControl extends SherlockMapActivity implements OnClickListener, TextWatcher, OnItemClickListener {

	public static String TAG = "MapControl";
	Integer[] geolat = null;
	Integer[] geolon = null;

	MapView mapView;
	String terms = null;
	String queryType = null;
	String username = null;
	String searchParam = null;  
	String distanceParam;
	String radiusParam;
	String userLat;
	String userLon;

	Button setLocation;
	Button getNotices;

	int mapSelectLocLat;
	int mapSelectLocLon;

	double mapDisplayLat;
	double mapDisplayLon;
	Location mLoc;

	RequestN0ticeObject request;

	//address list for geocoder
	private List<Address> addresses = null;
	private List<String> addressArray;	
	public static String textSearchLocation;
	private String currentLocation;

	private LinearLayout searchMapDash;
	private AutoCompleteTextView searchMapText;
	private Button searchMapBtn;
	private ArrayAdapterNoFilter searchAdapter;
	int mGeoLat;
	int mGeoLon;
	GeoPoint gLoc;

	private MapController mapController;
	private Handler messageHandler;
	private static final int NEW_SEARCH_TERM = 0;

	private InputMethodManager imm;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.maplayout);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

		messageHandler = new MyMessageHandler();

		searchMapDash = (LinearLayout)findViewById(R.id.search_map_dash);
		searchMapDash.setVisibility(View.GONE);

		// array adapter to back the auto complete function
		searchAdapter = new ArrayAdapterNoFilter(this, android.R.layout.simple_dropdown_item_1line);
		searchAdapter.setNotifyOnChange(true); 

		// Autocomplete text for searching the map
		searchMapText = (AutoCompleteTextView)findViewById(R.id.search_map_edit);
		searchMapText.setAdapter(searchAdapter);
		searchMapText.addTextChangedListener(this);
		searchMapText.setOnItemClickListener(this);

		searchMapBtn = (Button)findViewById(R.id.search_map_btn);
		searchMapBtn.setOnClickListener(this);

		mapView = (MapView) findViewById(R.id.maplayout);
		mapView.setBuiltInZoomControls(false);
		//		 	
		mapController = mapView.getController();
		mapController.setZoom(14);

		mLoc = new Location("mapDisplay");

		Log.i(TAG, "TP.mHaveLoc = " + TabsPager.mHaveLocation);

		if (TabsPager.mHaveLocation){

			mapDisplayLat = TabsPager.location.getLatitude();
			mapDisplayLon = TabsPager.location.getLongitude();
			Log.i(TAG, "TP.location = " + mapDisplayLat);

			mGeoLat = (int) (mapDisplayLat * 1000000);
			mGeoLon = (int) (mapDisplayLon * 1000000);

			mLoc.setLatitude(mapDisplayLat);
			mLoc.setLongitude(mapDisplayLon);

			gLoc = new GeoPoint(mGeoLat, mGeoLon);

			GeoCodeLoc gcl = new GeoCodeLoc(mLoc.getLatitude(), mLoc.getLongitude());
			gcl.execute();

			mapController.animateTo(gLoc);
		}

		setTitle(null);

		request = new RequestN0ticeObject();
		request.execute(mLoc);

	}

	@Override		        
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	protected void onPause() {
		super.onPause();
		//		gcl.cancel();
		//		request.cancel();
		//		gcSearch.cancel();
	}
	/**
	 * requests a notice feed object
	 */
	class RequestN0ticeObject extends AsyncTask<Location, Integer, JSONObject> {

		ProgressDialog progress;
		boolean haveNetwork = false;

		@Override
		protected void onPreExecute() {

			/** Fire off progress dialog */
			progress = new ProgressDialog(MapControl.this);
			progress.setMessage("Fetching n0tices...");
			progress.setIndeterminate(true);
			progress.setCancelable(true);
			progress.show();
		}

		@Override
		protected JSONObject doInBackground(Location... params) {

			// TODO this all needs rebuilding using API client calls instead

			//check have network connection   
			final ConnectivityManager conMgr =  (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();

			if (activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED) {
				//you are online	
				haveNetwork = true;


				Location mLocation = params[0];

				String URL = "http://n0ticeapis.com/2/search?type=report&location="
						+ mLocation.getLatitude() + "," + mLocation.getLongitude();

				Log.i("api request location",
						"lat:  " + String.valueOf(mapDisplayLat)
						+ "   " + "long:  " + String.valueOf(mapDisplayLon));

				Log.i("api URL", URL);

				String deviceId = "xxxxx" ;
				String mResult = "";  

				HttpClient mainhttpclient = new DefaultHttpClient();  
				// 		        HttpGet request = new HttpGet(URL + terms + distanceParam + "10");
				HttpGet request = new HttpGet(URL);
				request.addHeader("deviceId", deviceId);  
				ResponseHandler<String> handler = new BasicResponseHandler();  

				try {  
					mResult = mainhttpclient.execute(request, handler);  
				} catch (ClientProtocolException e) {  
					e.printStackTrace();  
				} catch (IOException e) {  
					e.printStackTrace();
				}  
				mainhttpclient.getConnectionManager().shutdown();  

				JSONObject json = null;
				try {
					json = new JSONObject(mResult);
				} catch (JSONException e1) {
					e1.printStackTrace();
				}
				return json;
			}
			else{
				haveNetwork = false;

				return null;
			}

		}

		@Override
		protected void onPostExecute(JSONObject json) {

			if (haveNetwork){
				if (json != null)updateViews(json);
			}
			else Toast.makeText(MapControl.this, "could not reach network, please check your connection", Toast.LENGTH_LONG).show();
			if(progress.isShowing()) progress.dismiss();
		}
	}

	protected void updateViews(JSONObject mJson){


		ArrayList<HashMap<String, String>> mylist = new ArrayList<HashMap<String, String>>();

		try{
			JSONArray n0ticeResults = mJson.getJSONArray("results");

			mylist.clear();

			for(int i=0;i<n0ticeResults.length();i++) {

				JSONObject e = n0ticeResults.getJSONObject(i);
				JSONObject place = e.getJSONObject("place");

				JSONArray updates = e.getJSONArray("updates");

				HashMap<String, String> map = new HashMap<String, String>();	

				String description = updates.getJSONObject(0).getString("body");
				String shortDescription = null;
				if (description.length() > 160) {
					shortDescription = new String(description.substring(0,160) +"...");
				} else {
					shortDescription = description;
				}

				map.put("name", e.getString("headline"));
				map.put("id", e.getString("id"));
				map.put("shortDesc", shortDescription);
				map.put("description", description);
				map.put("place", place.getString("name"));
				map.put("path", e.getString("webUrl"));

				map.put("lat",  geoizeString(place.getString("latitude")));
				map.put("lon",  geoizeString(place.getString("longitude")));

				mylist.add(map);	 		        	
			}		
		}catch(JSONException e)        {
			e.printStackTrace();
		}

		List<Overlay> mapOverlays = mapView.getOverlays();
		Drawable drawable = this.getResources().getDrawable(R.drawable.map_pin);
		NoticeOverlay itemizedoverlay = new NoticeOverlay(drawable,this);

		GeoPoint point = null;
		CustomOverlayItem customoverlayitem = null;
		String headline = null;
		String snippet = null;
		String body = null;
		String webUrl = null;

		for(int i = 0; i < mylist.size(); i++) {

			point = new GeoPoint(Integer.parseInt(mylist.get(i).get("lat")), Integer.parseInt(mylist.get(i).get("lon")));
			headline = mylist.get(i).get("name");
			snippet = mylist.get(i).get("shortDesc");
			body = mylist.get(i).get("description");
			webUrl = mylist.get(i).get("id");
			customoverlayitem = new CustomOverlayItem(point, headline, snippet, body, null, webUrl);
			itemizedoverlay.addOverlay(customoverlayitem);
		}

		//		 	GeoPoint startPoint = new GeoPoint(Integer.parseInt(userLat), Integer.parseInt(userLon));
		//		 	mapController.setCenter(startPoint);
		if(itemizedoverlay.size()>0)mapOverlays.add(itemizedoverlay);
		mapView.invalidate();

	}


	// method to convert latitude or longitude strings to GeoPoints.

	public String geoizeString(String loc)
	{
		loc = loc.replace(".","");
		while(loc.length() < 9)
			loc = loc.concat("0");
		if(loc.contains("-"))
			loc = loc.substring(0,8);
		else
			loc = loc.substring(0,8);
		return loc;
	}

	private void returnResults()
	{
		MapActivity ma = (MapActivity) this;
		Intent i = new Intent();
		Bundle b = new Bundle();
		b.putInt("mLat", mapSelectLocLat);
		b.putInt("mLon", mapSelectLocLon);
		i.putExtras(b);
		ma.setResult(Activity.RESULT_OK, i);
		ma.finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.map_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case android.R.id.home :
			NavUtils.navigateUpTo(this, new Intent(this, com.n0tice.android.TabsPager.class));
			break;

		case R.id.set_location :
			GeoPoint newLoc = mapView.getMapCenter();
			mapSelectLocLat = newLoc.getLatitudeE6();
			mapSelectLocLon = newLoc.getLongitudeE6();

			returnResults();

			break;


		case R.id.search_map_menu :

			int dashVis = searchMapDash.getVisibility();

			if (dashVis == View.VISIBLE) {
				imm.hideSoftInputFromWindow(searchMapText.getWindowToken(), 0);
				searchMapDash.setVisibility(View.GONE);
			} else if (dashVis == View.GONE) {
				imm.showSoftInput(searchMapText, 0);
				searchMapDash.setVisibility(View.VISIBLE);
			}

			break;


		case R.id.refresh_n0tices :

			refreshNotices();

			break;

		}
		return false;  
	}

	private void refreshNotices() {
		GeoPoint noticeLoc = mapView.getMapCenter();
		double latdoub = noticeLoc.getLatitudeE6() / 1E6;
		double londoub = noticeLoc.getLongitudeE6() / 1E6;

		Location newMapLoc = new Location("mapDisplayLastKnown");
		newMapLoc.setLatitude(latdoub);
		newMapLoc.setLongitude(londoub);

		GeoCodeLoc gcl = new GeoCodeLoc(newMapLoc.getLatitude(), newMapLoc.getLongitude());
		gcl.execute();

		request.cancel(true);
		request = new RequestN0ticeObject();
		request.execute(newMapLoc);

	}
	// Geocoder takes lat and lon values, outputs a name string

	private class GeoCodeLoc extends AsyncTask<URL, Integer, String > {

		Double myLat;
		Double myLon;
		Geocoder gcd = new Geocoder(MapControl.this);

		public GeoCodeLoc(Double myLat, Double myLon) {
			this.myLat = myLat;
			this.myLon = myLon;
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

	//this one takes a string (from text search) and produces lat and lon figures - also a normalised name
	private class GeoCodeString extends AsyncTask<String, Integer, List<Address>> {

		Geocoder gcd = new Geocoder(MapControl.this);

		@Override
		protected List<Address> doInBackground(String... params) {

			//check have network connection   
			final ConnectivityManager conMgr =  (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();

			double cityLat = -1;
			double cityLon = -1;

			if (activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED) {
				//you are online	

				try {
					addresses = gcd.getFromLocationName(params[0], 5);
				}
				catch (Exception e) {
					e.printStackTrace();
				}

			}
			return addresses;
		}

		@Override
		protected void onPostExecute(List<Address> results) {
			if (results != null && results.size() > 0) {

				searchAdapter.clear();
				for (Address a : results) {

					String add1 = a.getAddressLine(0); 
					String add2 = a.getAddressLine(1); 
					String addString = "";
					if (add1 != null) addString = addString.concat(add1);
					if (add2 != null) addString = addString.concat(", " + add2);

					searchAdapter.add(addString);
				}
				searchAdapter.notifyDataSetChanged();

			}
			else {
				Toast.makeText(MapControl.this, "No location found, please try again", Toast.LENGTH_LONG).show();
			}
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

	@Override
	public void onClick(View arg0) {

		int id = arg0.getId();
		switch (id) {
		case R.id.search_map_btn :
			refreshNotices();
			imm.hideSoftInputFromWindow(searchMapText.getWindowToken(), 0);

			break;

		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

		Log.e(MapControl.class.getName(), "OnItemSelected");
		imm.hideSoftInputFromWindow(searchMapText.getWindowToken(), 0);

		if (arg2 < addresses.size()) {

			Log.e(MapControl.class.getName(), "OnItemSelected > arg2 <");

			Address selected = addresses.get(arg2);

			int thisLat = (int) (selected.getLatitude() * 1E6);
			int thisLon = (int) (selected.getLongitude() * 1E6);

			gLoc = new GeoPoint(thisLat, thisLon);
			mapController.animateTo(gLoc);


		}
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

		Message msg = Message.obtain(messageHandler, NEW_SEARCH_TERM, s.toString());
		messageHandler.removeMessages(NEW_SEARCH_TERM);
		messageHandler.sendMessageDelayed(msg, 1000);

	}

	@Override
	public void afterTextChanged(Editable s) {		
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}


	private class MyMessageHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == NEW_SEARCH_TERM) {
				String enteredText = (String) msg.obj;

				try {
					GeoCodeString gcSearch = new GeoCodeString();
					gcSearch.execute(enteredText);

				} catch (Exception ex) {
					Log.e(MapControl.class.getName(), "Failed to get autocomplete suggestions", ex);
				}
			}
		}
	}
}