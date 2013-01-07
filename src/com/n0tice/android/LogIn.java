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

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.n0tice.android.post.ApiFactory;
import com.n0tice.android.post.Config;
import com.n0tice.api.client.exceptions.AuthorisationException;
import com.n0tice.api.client.exceptions.BadRequestException;
import com.n0tice.api.client.exceptions.HttpFetchException;
import com.n0tice.api.client.exceptions.NotAllowedException;
import com.n0tice.api.client.exceptions.NotFoundException;
import com.n0tice.api.client.exceptions.ParsingException;

public class LogIn extends Activity implements OnClickListener {

	private EditText userEdit;
	private EditText passEdit;
	private Button okButton;
	private Button setupAcct;
	private SharedPreferences n0ticeData;
	private Editor editor;

	private InputMethodManager imm;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);

		userEdit = (EditText)findViewById(R.id.user_edit);
		if (Config.getUsername(this) != null) userEdit.setText(Config.getUsername(this));
		passEdit = (EditText)findViewById(R.id.pass_edit);
		if (Config.getPassword(this) != null) passEdit.setText(Config.getPassword(this));

		okButton = (Button)findViewById(R.id.user_ok);
		setupAcct = (Button)findViewById(R.id.setup_account);
		okButton.setOnClickListener(this);
		setupAcct.setOnClickListener(this);

		imm = (InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE);

		n0ticeData = PreferenceManager.getDefaultSharedPreferences(LogIn.this);
		editor = n0ticeData.edit();
	}

	public void onClick(View v) {

		if (v.getId() == R.id.user_ok) {
			String username = userEdit.getText().toString().trim();
			String password = passEdit.getText().toString().trim();

			if (!username.isEmpty() && !password.isEmpty()) {

				editor.putString("username", username);
				editor.putString("password", password);
				editor.commit();

				VerifyUser verify = new VerifyUser();
				verify.execute(username);

				imm.hideSoftInputFromWindow(((EditText) this.findViewById(R.id.pass_edit)).getWindowToken(), 0);

			} else {
				Toast.makeText(this, "Please enter your login details or click below to setup a new account",
						Toast.LENGTH_LONG).show();
			}

		} else if (v.getId() == R.id.setup_account) {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://n0tice.com/signup"));
			startActivity(browserIntent);
		}
	}



	private class VerifyUser extends AsyncTask<String, Integer, String> {

		Boolean noNetwork;
		@Override
		protected String doInBackground(String... params) {

			String userName = params[0];
			String verifiedName = null;

			noNetwork = true;

			final ConnectivityManager conMgr =  (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();

			if (activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED) {

				noNetwork = false;

				try {
					// TODO: use the API's verify function instead of this bit
					TabsPager.api = ApiFactory.getAuthenticatedApi(getApplicationContext());
					verifiedName = TabsPager.api.userProfile(userName).getDisplayName();
					return verifiedName;
				} catch (NotFoundException e) {
					e.printStackTrace();
				} catch (InvalidKeyException e) {
					e.printStackTrace();
				} catch (ParsingException e) {
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} catch (NotAllowedException e) {
					e.printStackTrace();
				} catch (AuthorisationException e) {
					e.printStackTrace();
				} catch (BadRequestException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (HttpFetchException e) {
					e.printStackTrace();
				}

			} else {
				//no network connection
				noNetwork = true;

			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {

			Log.i("OPE", "name = " + result);
			if (result != null) {
				if (result.length() > 0) {
					Toast.makeText(LogIn.this, "Successfully logged in as " + result, Toast.LENGTH_LONG).show();
					editor.putBoolean("verified", true).commit();
					finish();
				}
			}
			else if (noNetwork == true) {
				Toast.makeText(LogIn.this, "Could not reach network, please check your connection", Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(LogIn.this, "Login failed, please check your details and try again", Toast.LENGTH_LONG).show();
				userEdit.setText(null);
				passEdit.setText(null);
				editor.putString("username", null);
				editor.putString("password", null);
				editor.putBoolean("verified", false);
				editor.commit();

			}		
		}
	}
}
