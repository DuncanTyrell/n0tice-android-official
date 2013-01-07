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

package com.n0tice.android.content;

import java.io.IOException;
import java.io.Serializable;
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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.n0tice.android.R;
import com.n0tice.android.TabsPager;
import com.n0tice.api.client.model.Noticeboard;

public class LocalBoards extends SherlockListFragment {

	public static String TAG = "LocalBoards";
	private BoardListAdapter boardAdapter;

	private SharedPreferences n0ticeData;
	private Boolean isLocationSelected;

	private ArrayList<HashMap<String, String>> savedResults;
	private ArrayList<HashMap<String, String>> cachedResults;

	private RequestBoards request;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.local_boards, container, false);

		n0ticeData = PreferenceManager.getDefaultSharedPreferences(getSherlockActivity());
		isLocationSelected = n0ticeData.getBoolean("selectLocation", false);

		request = new RequestBoards();

		if (savedInstanceState != null) {
			cachedResults = (ArrayList<HashMap<String, String>>) savedInstanceState.getSerializable("boardList");
			request.execute(cachedResults);
		} else {
			request.execute();
		}

		return rootView;
	}

	@Override
	public void onPause() {
		super.onPause();    
		request.cancel(true);

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("boardList", (Serializable) savedResults);
	}

	private class RequestBoards extends AsyncTask<ArrayList<HashMap<String, String>>, Integer, ArrayList<HashMap<String, String>>> {

		Boolean useFeatured = false;
		String fBoard;
		Boolean haveNetwork = false;
		ArrayList<HashMap<String, String>> boardList;

		@Override
		protected ArrayList<HashMap<String, String>> doInBackground(ArrayList<HashMap<String, String>>... params) {

			if (params.length > 0) {
				haveNetwork = true;
				return params[0];
			}

			//check have network connection   
			final ConnectivityManager conMgr =  (ConnectivityManager) getSherlockActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
			final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();

			if (activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED) {
				//you are online	
				haveNetwork = true;
				boardList = new ArrayList<HashMap<String, String>>();

				try {
					//				
					Location mLoc;

					if (isLocationSelected) {
						mLoc = TabsPager.selectLocation;
					} else {
						mLoc = TabsPager.location;
					}

					JSONArray localBoards = new JSONArray();

					try {
						fBoard = getFeaturedNoticeBoard();
						localBoards.put(fBoard);
					} catch (Exception e) {
						e.printStackTrace();
					}

					try {
						List<String> nearby = TabsPager.api.near(mLoc.getLatitude(), mLoc.getLongitude(), 3).getRefinements(); 
						for (int i = 0; i < nearby.size(); i++) {
							localBoards.put(nearby.get(i));
						}

					} catch (Exception e) {
						e.printStackTrace();
					}

					String boardname;
					Noticeboard nb;


					for (int i=0; i<localBoards.length(); i++) {

						boardname = localBoards.getString(i);

						if (!boardname.equals("n0tice")) {
							Log.i(TAG, boardname);
							nb = TabsPager.api.noticeBoard(boardname);

							HashMap<String, String> map = new HashMap<String, String>();	
							map.put("title",  nb.getName());
							map.put("description",  nb.getDescription());

							if (i == 0) map.put("featured", "true");
							if (nb.getCover() != null) map.put("image",  nb.getCover());
							if (nb.getBackground() != null) map.put("background",  nb.getBackground());

							map.put("domain",  nb.getDomain());
							map.put("type",  "board");

							boardList.add(map);		
						}
					}

					return boardList;

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else{
				//you are not online
				haveNetwork = false;
			}


			return null;
		}

		@Override
		protected void onPostExecute(ArrayList<HashMap<String, String>> results) {

			if (haveNetwork){

				if (results != null) {
					if (savedResults == null) {
						updateViews(results);
						savedResults = results;
					} else {
						if (!results.equals(savedResults)) {
							updateViews(results);
							savedResults = results;
						}
					}
				}
			}
		}
	}

	private String getFeaturedNoticeBoard() {
		String deviceId = "xxxxx";
		String mResult = "";  

		HttpClient mainhttpclient = new DefaultHttpClient();

		String stockURL = "http://n0ticeapis.com/2/noticeboards?featured=true";

		HttpGet httpRequest = new HttpGet(stockURL);  
		httpRequest.addHeader("deviceId", deviceId);

		ResponseHandler<String> handler = new BasicResponseHandler();

		try {  
			mResult = mainhttpclient.execute(httpRequest, handler);  
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
		String featuredBoard = "streetart";

		try {
			featuredBoard = json.getJSONArray("results").getJSONObject(0).getString("domain");

		}catch(JSONException e)        {
			e.printStackTrace();
		}
		return featuredBoard;      

	}
	private void updateViews(ArrayList<HashMap<String, String>> results) {

		boardAdapter = new BoardListAdapter(getSherlockActivity(), results);
		boardAdapter.notifyDataSetChanged();
		setListAdapter(boardAdapter);

		final ListView boardLv = getListView();
		boardLv.setTextFilterEnabled(true);

		boardLv.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {        		
				@SuppressWarnings("unchecked")
				HashMap<String, String> o = (HashMap<String, String>) boardLv.getItemAtPosition(position);	        		

				Intent intent = new Intent(getSherlockActivity(), DisplayBoard.class);
				intent.putExtra("domain", o.get("domain"));
				startActivity(intent);

			}
		});
	}
}
