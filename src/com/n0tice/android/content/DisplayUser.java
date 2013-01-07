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
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.n0tice.android.R;
import com.n0tice.android.TabsPager;
import com.n0tice.android.post.Config;
import com.n0tice.api.client.exceptions.AuthorisationException;
import com.n0tice.api.client.exceptions.BadRequestException;
import com.n0tice.api.client.exceptions.HttpFetchException;
import com.n0tice.api.client.exceptions.NotAllowedException;
import com.n0tice.api.client.exceptions.NotFoundException;
import com.n0tice.api.client.exceptions.ParsingException;
import com.n0tice.api.client.model.Noticeboard;
import com.n0tice.api.client.model.User;

public class DisplayUser extends SherlockFragmentActivity implements OnClickListener {

	ImageView mImage;
	LoadBitmap loadBit;

	GetUserProfile getProfile;
	Button followUserBtn;

	String username;

	/** @param int 0 = follow
	 * @param int 1 = unfollow
	 */
	int followOrUnfollow;

	private Boolean isFollowing;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS); 
		setContentView(R.layout.display_user);

		String mUsername = getIntent().getStringExtra("username");

		followUserBtn = (Button)findViewById(R.id.follow_user_btn);
		followUserBtn.setVisibility(View.GONE);
		followUserBtn.setOnClickListener(this);

		getProfile = new GetUserProfile();
		getProfile.execute(mUsername);
	}


	@Override
	public void onPause() {
		super.onPause();    
		getProfile.cancel(true);
		loadBit.cancel(true);
	}

	public class GetUserProfile extends AsyncTask<String, Integer, User> {

		User user = null;
		List<User> followedUsers;
		List<Noticeboard> followedBoards;
		
		@Override
		protected void onPreExecute() {
			DisplayUser.this.setProgressBarIndeterminateVisibility(true); 
		}

		@Override
		protected User doInBackground(String... params) {

			try {
				user = TabsPager.api.userProfile(params[0]);
				
				if (Config.getVerified(DisplayUser.this) == true) {
					followedUsers = TabsPager.api.followedUsers(Config.getUsername(DisplayUser.this));

					// set Follow button text
					isFollowing = followedUsers.contains(user);
					
					followedBoards = TabsPager.api.followedNoticeboards(Config.getUsername(DisplayUser.this));
				}
			
			} catch (NotFoundException e) {
				e.printStackTrace();
			} catch (ParsingException e) {
				e.printStackTrace();
			} catch (HttpFetchException e) {
				e.printStackTrace();
			}
			return user;
		}

		@Override
		protected void onPostExecute(User result) {

			if (result != null) {
				username = result.getUsername(); 

				String displayName = result.getDisplayName().trim();

				// set title txt
				TextView headText = (TextView)findViewById(R.id.user_title);

				if (displayName != "") {
					//					setTitle(displayName);
					headText.setText(Html.fromHtml(displayName));
				} else {
					//					setTitle(username);
					headText.setText(Html.fromHtml(username));
				}

				// set description text
				TextView bodyText = (TextView)findViewById(R.id.user_bio);
				bodyText.setText(result.getBio());
				bodyText.setAutoLinkMask(Linkify.WEB_URLS);
				bodyText.setMovementMethod(LinkMovementMethod.getInstance());


				if (isFollowing == null) {
					followUserBtn.setVisibility(View.GONE);
				} else if (isFollowing == true) {
					followUserBtn.setVisibility(View.VISIBLE);
					followUserBtn.setText("Unfollow " + username);
				} else {
					followUserBtn.setVisibility(View.VISIBLE);
					followUserBtn.setText("Follow " + username);
				}

				String imageUrl = null;
				if (result.getProfileImage() != null) imageUrl = result.getProfileImage().getSmall(); 
				
				if (imageUrl != null && !imageUrl.equals("")) {
					// load image
					loadBit = new LoadBitmap();
					loadBit.execute(result.getProfileImage().getSmall());
				} else {
					DisplayUser.this.setProgressBarIndeterminateVisibility(false); 
				}
			}

		}
	}

	private class LoadBitmap extends AsyncTask<String, Integer, Bitmap> {

		@Override
		protected Bitmap doInBackground(String... params) {

			// setup imageview
			mImage = (ImageView)findViewById(R.id.user_image);

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

		@Override
		protected void onPostExecute(Bitmap result) {

			if (result != null) {

				try {
					mImage.setImageBitmap(result);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			DisplayUser.this.setProgressBarIndeterminateVisibility(false); 
		}
	}

	@Override
	public void onClick(View v) {

		int id = v.getId();

		switch (id) {
		case R.id.follow_user_btn :

			// Check whether we're logged in then attempt follow/unfollow
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(DisplayUser.this);
			Boolean loggedIn = prefs.getBoolean("verified", false);

			if (loggedIn) {
				FollowUser follow = new FollowUser();

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

	private class FollowUser extends AsyncTask<Boolean, Integer, Integer> {

		/** @param 	-1 = not set
		 * @param	0 = followTask success
		 * @param	1 = followTask failed
		 * @param	2 = unfollowTask success
		 * @param	3 = unfollowTask failed
		 * @param 	4 = failed for some other reason
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
					// follow user
					try {
						success = TabsPager.api.followUser(username);
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
					// already following, unfollow this user
					try {
						success = TabsPager.api.unfollowUser(username);
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
				Toast.makeText(DisplayUser.this, "Could not reach network, please check your connection", Toast.LENGTH_LONG).show();
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
				Toast.makeText(DisplayUser.this, "Now following " + username, Toast.LENGTH_LONG).show();
			isFollowing = true;
			followUserBtn.setText("Unfollow " + username);
			break;

			case (1) :
				Toast.makeText(DisplayUser.this, "Tried to follow " + username + "but failed", Toast.LENGTH_LONG).show();
			break;

			case (2) :
				Toast.makeText(DisplayUser.this, "No longer following " + username, Toast.LENGTH_LONG).show();
			isFollowing = false;
			followUserBtn.setText("Follow " + username);

				break;

			case (3) :
				Toast.makeText(DisplayUser.this, "Tried to unfollow " + username + "but failed", Toast.LENGTH_LONG).show();

				break;

			case (4) :
				Toast.makeText(DisplayUser.this, "Failed for other reasons - network, api, etc", Toast.LENGTH_LONG).show();

				break;

			}
		}
	}
}

