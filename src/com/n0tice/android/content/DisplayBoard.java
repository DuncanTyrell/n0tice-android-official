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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.MenuItem;
import com.n0tice.android.R;
import com.n0tice.android.TabsPager;
import com.n0tice.android.post.Config;
import com.n0tice.api.client.exceptions.AuthorisationException;
import com.n0tice.api.client.exceptions.BadRequestException;
import com.n0tice.api.client.exceptions.HttpFetchException;
import com.n0tice.api.client.exceptions.NotAllowedException;
import com.n0tice.api.client.exceptions.NotFoundException;
import com.n0tice.api.client.exceptions.ParsingException;
import com.n0tice.api.client.model.Content;
import com.n0tice.api.client.model.Noticeboard;
import com.n0tice.api.client.model.SearchQuery;

public class DisplayBoard extends SherlockListActivity implements OnClickListener {

	String domain = null;
	String background = null;

	SearchQuery searchQ;
	ImageView mImage;
	ListView lv = null;

	public static final String ARG_ITEM_ID = "item_id";

	ArrayList<HashMap<String, String>> mylist;
	private n0ticeListAdapter adapter;

	private List<Content> savedResults;

	private GetBoard request;
	private Intent intent;
	private Bundle extras;

	private Boolean isFollowing = null;

	private Button followBoardBtn;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.display_noticeboard);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		intent = getIntent();
		extras = intent.getExtras();
		if (extras != null) {
			//			title = extras.getString("title");
			domain = extras.getString("domain");
			//			type = extras.getString("type");
			//			background = extras.getString("background");
		}

		mImage = new ImageView(this);
		mImage = (ImageView)findViewById(R.id.board_image);

		followBoardBtn = (Button)findViewById(R.id.follow_board_btn);
		followBoardBtn.setVisibility(View.GONE);
		followBoardBtn.setOnClickListener(this);

		request = new GetBoard();
		request.execute(domain);
	}

	@Override
	public void onPause() {
		super.onPause();

		request.cancel(true);
	}


	/**
	 * requests the List<Content> for a given noticeboard
	 */
	private class GetBoard extends AsyncTask<String, Integer, List<Content>> {

		Noticeboard noticeboard;
		List<Noticeboard> followedBoards;

		@Override
		protected List<Content> doInBackground(String... params) {

			String boardParam = params[0];

			searchQ = new SearchQuery().noticeBoard(boardParam);

			List<Content> boardNotices;	
			try {
				noticeboard = TabsPager.api.noticeBoard(boardParam);

				boardNotices = TabsPager.api.search(searchQ).getContent();

				if (Config.getVerified(DisplayBoard.this)) {

					followedBoards = TabsPager.api.followedNoticeboards(Config.getUsername(DisplayBoard.this));

					/** this doesn't work elegantly - still a bit of a hack,
					 * but .contains() method doesn't work right for these, they don't equate
					 */

					for (int i = 0; i < followedBoards.size(); i++) {
						if (followedBoards.get(i).getDomain().equals(noticeboard.getDomain())) {
							isFollowing = true;
							break;
						} else {
							isFollowing = false;
						}
					}
				}

				return boardNotices;

			} catch (NotFoundException e) {
				e.printStackTrace();
			} catch (ParsingException e) {
				e.printStackTrace();
			} catch (HttpFetchException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(List<Content> results) {

			if (results != null) {

				LoadBitmap loader = new LoadBitmap();

				if (savedResults == null) {

					updateViews(results);
					setTitle(noticeboard.getName());

					if (noticeboard.getCover() != null) loader.execute(noticeboard.getCover());
					else if (noticeboard.getBackground() != null) loader.execute(noticeboard.getBackground());

					savedResults = results;

				} else {
					if (!results.equals(savedResults)) {

						updateViews(results);
						savedResults = results;
						if (noticeboard.getCover() != null) loader.execute(noticeboard.getCover());
						else if (noticeboard.getBackground() != null) loader.execute(noticeboard.getBackground());

					}
				}

			} else {
				Toast.makeText(DisplayBoard.this, "results == null", Toast.LENGTH_LONG).show();
			}

			if (isFollowing == null) {
				followBoardBtn.setVisibility(View.GONE);
			} else if (isFollowing) {
				followBoardBtn.setVisibility(View.VISIBLE);
				followBoardBtn.setText("Unfollow");
			} else {
				followBoardBtn.setVisibility(View.VISIBLE);
				followBoardBtn.setText("Follow");
			}
		}
	}

	protected void updateViews(List<Content> results){

		mylist = new ArrayList<HashMap<String, String>>();

		adapter = new n0ticeListAdapter(this, results);
		adapter.notifyDataSetChanged();
		setListAdapter(adapter);

		lv = getListView();
		lv.setTextFilterEnabled(true);	
		lv.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {        		

				Content o = (Content) lv.getItemAtPosition(position);	        		
				Intent intent = new Intent(DisplayBoard.this, DisplayReport.class);

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



	private class FollowBoard extends AsyncTask<Boolean, Integer, Integer> {

		/** @param 	-1 = not set
		 * @param	0 = followTask success
		 * @param	1 = followTask failed
		 * @param	2 = unfollowTask success
		 * @param	3 = unfollowTask failed
		 * @param 	4 = fucked up for some other reason
		 */
		int result;

		@Override
		protected Integer doInBackground(Boolean... params) {

			final ConnectivityManager conMgr =  (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();

			if (activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED) {
				//you are online

				result = -1;

				Boolean success = false;

				if (params[0]) {
					// follow board
					try {
						success = TabsPager.api.followNoticeboard(domain);
						result = 0;
						return result;
					} catch (NotFoundException e) {
						result = 1;
						e.printStackTrace();
					} catch (AuthorisationException e) {
						result = 1;
						e.printStackTrace();
					} catch (NotAllowedException e) {
						result = 1;
						e.printStackTrace();
					} catch (BadRequestException e) {
						result = 1;
						e.printStackTrace();
					}
				} else {
					// already following, unfollow this board
					try {
						success = TabsPager.api.unfollowNoticeboard(domain);
						result = 2;
						return result;
					} catch (NotFoundException e) {
						result = 3;
						e.printStackTrace();
					} catch (AuthorisationException e) {
						result = 3;
						e.printStackTrace();
					} catch (NotAllowedException e) {
						result = 3;
						e.printStackTrace();
					} catch (BadRequestException e) {
						result = 3;
						e.printStackTrace();
					} 

				}
				return result;

			} else {
				//no network connection
				Toast.makeText(DisplayBoard.this, "Could not reach network, please check your connection", Toast.LENGTH_LONG).show();
				result = 4;
				return result;
			}
		}

		@Override
		protected void onPostExecute(Integer result) {

			switch (result) {
			case (-1) :

				break;

			case (0) :
				Toast.makeText(DisplayBoard.this, "Now following " + domain, Toast.LENGTH_LONG).show();
			isFollowing = true;
			followBoardBtn.setText("Unfollow");
			break;

			case (1) :
				Toast.makeText(DisplayBoard.this, "Tried to follow " + domain + "but failed", Toast.LENGTH_LONG).show();
			break;

			case (2) :
				Toast.makeText(DisplayBoard.this, "No longer following " + domain, Toast.LENGTH_LONG).show();
			isFollowing = false;
			followBoardBtn.setText("Follow");
			break;

			case (3) :
				Toast.makeText(DisplayBoard.this, "Tried to unfollow " + domain + "but failed", Toast.LENGTH_LONG).show();

			break;

			case (4) :
				Toast.makeText(DisplayBoard.this, "Failed for other reasons - network, api, etc", Toast.LENGTH_LONG).show();

			break;

			}
		}
	}

	private class LoadBitmap extends AsyncTask<String, Integer, Bitmap> {
	
		@Override
		protected void onPreExecute() {
			DisplayBoard.this.setProgressBarIndeterminateVisibility(true); 
		}

		@Override
		protected Bitmap doInBackground(String... params) {

			Bitmap mIcon11 = null;
			try {
				String urldisplay = params[0];
				InputStream in = new java.net.URL(urldisplay).openStream();
				mIcon11 = BitmapFactory.decodeStream(in);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return mIcon11;
		}

		protected void onPostExecute(Bitmap result) {

			if (result != null) {
				try {
					mImage.setImageBitmap(result);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			} else {
				mImage.setVisibility(View.GONE);
			}
			DisplayBoard.this.setProgressBarIndeterminateVisibility(false); 

		}
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

	@Override
	public void onClick(View v) {
		int id = v.getId();

		switch (id) {
		case R.id.follow_board_btn :

			// Check whether we're logged in then attempt follow/unfollow
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(DisplayBoard.this);
			Boolean loggedIn = prefs.getBoolean("verified", false);

			if (loggedIn) {
				FollowBoard follow = new FollowBoard();

				if (isFollowing) {
					follow.execute(false);
				} else {
					follow.execute(true);
				}

			} else {
				Toast.makeText(this, "Please log in to your account", Toast.LENGTH_LONG).show();
				startActivity(new Intent(this, com.n0tice.android.LogIn.class));
			}
			break;
		}
	}
}