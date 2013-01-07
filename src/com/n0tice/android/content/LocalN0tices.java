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
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.n0tice.android.R;
import com.n0tice.android.TabsPager;
import com.n0tice.api.client.model.Content;
import com.n0tice.api.client.model.ResultSet;
import com.n0tice.api.client.model.SearchQuery;

public class LocalN0tices extends SherlockListFragment {

	private n0ticeListAdapter n0ticeAdapter;
	private RequestNotices request;

	private SharedPreferences n0ticeData;
	private Boolean isLocationSelected;

	private List<Content> savedResults;
	private List<Content> cachedResults;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.local_notices, container, false);

		n0ticeData = PreferenceManager.getDefaultSharedPreferences(getSherlockActivity());
		isLocationSelected = n0ticeData.getBoolean("selectLocation", false);

		request = new RequestNotices();

		if (savedInstanceState != null) {
			cachedResults = (List<Content>) savedInstanceState.getSerializable("noticeList");
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
		outState.putSerializable("noticeList", (Serializable) savedResults);
	}

	private class RequestNotices extends AsyncTask<List<Content>, Integer, List<Content>> {

		Boolean useFeatured = false;
		String fedBoard;
		Boolean haveNetwork = false;
		List<Content> localNotices;

		@Override
		protected void onPreExecute() {
			getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true); 
		}

		@Override
		protected List<Content> doInBackground(List<Content>... params) {

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

				try {
					Location mLoc;

					if (isLocationSelected) {
						mLoc = TabsPager.selectLocation;
					} else {
						mLoc = TabsPager.location;
					}

					ResultSet localResults = null;
					Boolean usingLocal = false;

					try {
						localResults = TabsPager.api.near(mLoc.getLatitude(), mLoc.getLongitude(), 3);
						usingLocal = true;
					} catch (Exception e) {
						usingLocal = false;
					}

					if (!usingLocal) {
						try {
							String featuredBoard = getFeaturedNoticeBoard();
							SearchQuery searchQ = new SearchQuery().noticeBoard(featuredBoard);
							localResults = TabsPager.api.search(searchQ);
							useFeatured = true;
							fedBoard = featuredBoard;

						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					localNotices = localResults.getContent();
					return localNotices;

				} catch (Exception e) {
					e.printStackTrace();
				}

			} else {
				//you are not online
				haveNetwork = false;
			}
			return null;
		}


		@Override
		protected void onPostExecute(List<Content> results) {

			if (haveNetwork){

				if (useFeatured) {
					if (results != savedResults) {
						getSherlockActivity().setTitle("Featured: " + fedBoard);
						Toast.makeText(getSherlockActivity(), R.string.no_local_use_featured, Toast.LENGTH_LONG).show();
					}
				}

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
			} else {
				Toast.makeText(getSherlockActivity(), "Could not reach network, please check your connection", Toast.LENGTH_LONG).show();
			}
			getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false); 
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

	private void updateViews(List<Content> results) {

		n0ticeAdapter = new n0ticeListAdapter(getSherlockActivity(), results);
		n0ticeAdapter.notifyDataSetChanged();
		setListAdapter(n0ticeAdapter);

		final ListView n0ticeLv = getListView();
		n0ticeLv.setTextFilterEnabled(true);

		n0ticeLv.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {        		

				@SuppressWarnings("unchecked")
				Content o = (Content) n0ticeLv.getItemAtPosition(position);	        		

				Intent intent = new Intent(getSherlockActivity(), DisplayReport.class);
				intent.putExtra("title", o.getHeadline());

				String description = null;

				try {
					description = o.getUpdates().get(0).getBody();
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (description != null) intent.putExtra("description", description);

				String image = null;

				try {
					image = o.getUpdates().get(0).getImage().getLarge();
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (image != null) intent.putExtra("image", image);

				intent.putExtra("username", o.getUser().getUsername());
				intent.putExtra("board", o.getNoticeBoard());
				intent.putExtra("place", o.getPlace().getName());
				intent.putExtra("source", "local");

				startActivity(intent);

			}
		});
	}
}
