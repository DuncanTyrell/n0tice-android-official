/**   Copyright 2012 Tony McCrae, Eel Pie Consulting
 * 
 * Including minor amends by Tyrell Mobile

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

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import com.n0tice.android.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.n0tice.android.TabsPager;
//import com.n0tice.api.client.N0ticeApi;
import com.n0tice.api.client.exceptions.AuthorisationException;
import com.n0tice.api.client.exceptions.BadRequestException;
import com.n0tice.api.client.exceptions.NotAllowedException;
import com.n0tice.api.client.exceptions.NotFoundException;
import com.n0tice.api.client.exceptions.ParsingException;
import com.n0tice.api.client.model.ImageFile;

public class ReportPostingService extends Service {

	private static final String TAG = "ReportPostingService";
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.i(TAG, "Started");
		
		final Bundle extras = intent.getExtras();			
		final String headline = extras.getString("headline");
		final double latitude = extras.getDouble("latitude");
		final double longitude = extras.getDouble("longitude");			
		final String body = extras.getString("body");
		final byte[] image = extras.getByteArray("image");
		final String board = extras.getString("board");
		
		final NewReport newReport = new NewReport(headline, latitude, longitude, body, image, board);
		
		final PostReportTask task = new PostReportTask();
		task.execute(newReport);
		
		return START_STICKY;
	}

	private void afterPost(NewReport newReport) {
		sendSuccessNotification(newReport);
		Log.i(TAG, "Finished - closing down service");
		stopSelf();
	}
	
	private void sendSuccessNotification(NewReport newReport) {		
		final Context context = getApplicationContext();
		final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		final String text = "New n0tice report uploaded";
		final String headline_text = newReport.getHeadline();
		final Notification notification = new Notification(R.drawable.n0tice_n, text, new Date().getTime());
		Intent notificationIntent = new Intent(context, TabsPager.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);		
		notification.setLatestEventInfo(context, text, headline_text, contentIntent);
		notificationManager.notify(1, notification);		
	}
	
	private class PostReportTask extends AsyncTask<NewReport, Integer, Boolean> {

		private NewReport newReport;

		@Override
		protected Boolean doInBackground(NewReport... params) {
			newReport = params[0];
			Log.i(TAG, "Posting report in background: " + newReport);
						
			try {
				TabsPager.api = ApiFactory.getAuthenticatedApi(getApplicationContext());
				final ImageFile imageFile = newReport.getImage() != null ? new ImageFile(newReport.getImage(), "image.jpg") : null;
				TabsPager.api.postReport(newReport.getHeadline(), newReport.getLatitude(), newReport.getLongitude(), newReport.getBody(), null, imageFile, newReport.getBoard());
				return true;
				
			} catch (NotFoundException e) {
				e.printStackTrace();
			} catch (ParsingException e) {
				e.printStackTrace();
			} catch (NotAllowedException e) {
				e.printStackTrace();
			} catch (AuthorisationException e) {
				e.printStackTrace();
			} catch (BadRequestException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
			return false;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			afterPost(newReport);
		}		
	}
	
}
