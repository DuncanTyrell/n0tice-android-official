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

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.n0tice.android.R;
import com.n0tice.android.TabsPager;
import com.n0tice.android.post.NewReport;
import com.n0tice.api.client.exceptions.HttpFetchException;
import com.n0tice.api.client.exceptions.NotFoundException;
import com.n0tice.api.client.exceptions.ParsingException;
import com.n0tice.api.client.model.Content;
import com.n0tice.api.client.model.Image;
import com.n0tice.api.client.model.Update;

public class DisplayReport extends SherlockFragmentActivity {

	ImageView mImageView;
	LoadBitmap loadBit;
	NewReport report;

	String title;
	String description;
	String image;
	String username;
	String board;
	String place;
	String source;
	String id;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS); 
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.display_report);

		Bundle extras = getIntent().getExtras();

		title = null;
		description = null;
		image = null;
		username = null;
		board = null;
		place = null;
		source = null;

		id = null;

		// get bundled extras
		if (extras != null) {
			title = extras.getString("title");
			description = extras.getString("description");
			image = extras.getString("image");
			username = extras.getString("username");
			board = extras.getString("board");
			place = extras.getString("place");
			source = extras.getString("source");
			id = extras.getString("id");
		}

		/** id is only set as an extra from MapControl.class, so if it's not null
		 * then load the report details from that (can't put the rest of the details
		 * as extras from the CustomDialog class without a lot of messing about).
		 */
		if (id != null) {
			LoadReport request = new LoadReport();
			request.execute(id);

		} else {
			updateViews();
		}
	}

	private void updateViews() {
		setTitle(title);

		// set title txt
		TextView headText = (TextView)findViewById(R.id.report_title);
		headText.setText(Html.fromHtml(title));
		headText.setAutoLinkMask(Linkify.WEB_URLS);
		headText.setMovementMethod(LinkMovementMethod.getInstance());

		// setup imageview
		mImageView = (ImageView)findViewById(R.id.report_image);

		// set description text
		TextView bodyText = (TextView)findViewById(R.id.report_description);
		if (description != null) bodyText.setText(Html.fromHtml(String.valueOf(description)));
		bodyText.setAutoLinkMask(Linkify.WEB_URLS);
		bodyText.setMovementMethod(LinkMovementMethod.getInstance());

		// set user text
		Button userBtn = (Button)findViewById(R.id.report_user);
		userBtn.setText(username);

		// set board text
		Button boardBtn = (Button)findViewById(R.id.report_board);
		if (!board.equals("n0tice")) {
			boardBtn.setText(board);
		} else {
			boardBtn.setVisibility(View.INVISIBLE);
		}

		// set place text
		TextView placeText = (TextView)findViewById(R.id.report_place);
		placeText.setText(getString(R.string.n0ticed_at) + " " 
				+ place);

		// load image
		loadBit = new LoadBitmap();
		if (image != null) {
			loadBit.execute(image);
		} 
	}


	@Override
	public void onPause() {
		super.onPause();    
		loadBit.cancel(true);

	}
	private class LoadBitmap extends AsyncTask<String, Integer, Bitmap> {

		@Override
		protected void onPreExecute() {
			DisplayReport.this.setProgressBarIndeterminateVisibility(true); 
		}

		@Override
		protected Bitmap doInBackground(String... params) {

			Log.i("DisplayReport", "AST started");

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

			Log.i("DisplayReport", "OPE started");

			if (result != null) {
				Log.i("DisplayReport", "result != null");

				try {
					mImageView.setImageBitmap(result);
				} catch (Exception e1) {
					e1.printStackTrace();
					Log.i("DisplayReport", "AST Failure Will Robinsion");
				}
			}
			DisplayReport.this.setProgressBarIndeterminateVisibility(false); 
		}
	}


	private class LoadReport extends AsyncTask<String, Integer, Content> {

		@Override
		protected void onPreExecute() {
			DisplayReport.this.setProgressBarIndeterminateVisibility(true); 

		}

		@Override
		protected Content doInBackground(String... params) {

			Content mContent = null;

			try {
				mContent = TabsPager.api.get(params[0]);
				return mContent;
			} catch (NotFoundException e) {
				e.printStackTrace();
			} catch (HttpFetchException e) {
				e.printStackTrace();
			} catch (ParsingException e) {
				e.printStackTrace();
			}

			return null;
		}

		protected void onPostExecute(Content result) {

			Log.i("DisplayReport", "OPE started");

			if (result != null) {
				title = result.getHeadline();
				List<Update> mUpdates = result.getUpdates();
				Image mImage = null;
				if (mUpdates.size() > 0) {
					description = mUpdates.get(0).getBody();
					if (mUpdates.get(0).getImage() != null) mImage = mUpdates.get(0).getImage(); 
				}

				if (mImage != null) image = mImage.getLarge();

				username = result.getUser().getUsername();
				board = result.getNoticeBoard();
				place = result.getPlace().getName();

				updateViews();
			}
			DisplayReport.this.setProgressBarIndeterminateVisibility(false); 
		}
	}

	public void onItemClicked(View v) {

		int id = v.getId ();

		switch (id) {
		case R.id.report_user :

			Intent showUser = new Intent(DisplayReport.this, DisplayUser.class);
			showUser.putExtra("username", username);
			startActivity(showUser);
			break;
		case R.id.report_board :
			Intent showBoard = new Intent(DisplayReport.this, DisplayBoard.class);
			showBoard.putExtra("domain", board);
			startActivity(showBoard);
			break;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case android.R.id.home: 
			NavUtils.navigateUpTo(this, new Intent(this, com.n0tice.android.TabsPager.class));
			return true;
		}
		return super.onOptionsItemSelected(item);  
	}
}