/**   Copyright 2012 Tony McCrae, Eel Pie Consulting
 * 
 * Including amends by Tyrell Mobile, as marked

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.util.Log;

import com.n0tice.api.client.N0ticeApi;
import com.n0tice.api.client.exceptions.AuthorisationException;
import com.n0tice.api.client.exceptions.BadRequestException;
import com.n0tice.api.client.exceptions.NotAllowedException;
import com.n0tice.api.client.exceptions.NotFoundException;
import com.n0tice.api.client.exceptions.ParsingException;
import com.n0tice.api.client.model.AccessToken;


public class ApiFactory {
	
	private static final String TAG = "ApiFactory";
	
	private static final String API_URL = Config.getApiUrl();
	private static final String CONSUMER_SECRET = Config.getConsumerSecret();
	private static final String CONSUMER_KEY = Config.getConsumerKey();
	
	private static String TOKEN_FILENAME = "n0tice_access_token";

	/**
	 * Returns an unauthenticated read only instance of the n0tice API
	 */
	public static N0ticeApi getUnauthenticatedApi() {
		return new N0ticeApi(Config.getApiUrl());
	}
	
	/**
	 * Returns an authenticated read/write instance of the n0tice API
	 * @throws NoSuchAlgorithmException 
	 * @throws UnsupportedEncodingException 
	 * @throws InvalidKeyException 
	 */
	public static N0ticeApi getAuthenticatedApi(Context context) throws NotFoundException, ParsingException, NotAllowedException, AuthorisationException, BadRequestException, InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException {		
		final String username = Config.getUsername(context);
		final String password = Config.getPassword(context);
		final Boolean verified = Config.getVerified(context);
		
		Log.d(TAG, "Username: " + username + ", password: " + password);
		
		final N0ticeApi n0ticeApi = getUnauthenticatedApi();
		
		/** ADDED BY TYRELL: tries to persist access token between sessions...
		*/
		
		if (getAccessTokenFromCache(context) != null) {
			AccessToken cachedToken = getAccessTokenFromCache(context);
			return new N0ticeApi(API_URL, CONSUMER_KEY, CONSUMER_SECRET, cachedToken);

		} else {
			if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
				// Exchange the users username and password for an authorised oauth access token.
				// Because the oauth web browers authorisation flow is not suited to mobile the API provides a method
				// for selected apps to directly swap username and passwords for tokens
				final AccessToken accessToken = exchangeCredentialsForOauthAccessToken(username, password, n0ticeApi);
	
				saveAccessTokenToCache(context, accessToken);
				
				// At this point an ideal client implementation would store the access token locally
				// only attempting to retrieve another if there was a problem (ie. it was revoked) with the current one. 			
				return new N0ticeApi(API_URL, CONSUMER_KEY, CONSUMER_SECRET, accessToken);
	
			} else {
				throw new AuthorisationException();
			}
			
		}
	}
/** The following two methods were added by Tyrell Mobile to original code by Tony McCrae
 * 
 * @param ctx
 * @param accessToken
 */
	private static void saveAccessTokenToCache(Context ctx, AccessToken accessToken) {

		/** This saves an object to a file in the protected cache storage for this app.
		 */
		File file = new File(ctx.getCacheDir(), TOKEN_FILENAME);

		FileOutputStream fos;
		ObjectOutputStream oos;

		try {
			fos = new FileOutputStream(file);
			oos = new ObjectOutputStream(fos);

			oos.writeObject(accessToken);
			oos.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static AccessToken getAccessTokenFromCache(Context ctx) {
		
		AccessToken savedToken = null;
		
		/** This gets an object from a file in the protected cache storage for this app
		 */
		File file = new File(ctx.getCacheDir(), TOKEN_FILENAME);

		FileInputStream fis;
		ObjectInputStream ois;

		try {
			fis = new FileInputStream(file);
			ois = new ObjectInputStream(fis);

			savedToken = (AccessToken) ois.readObject();
			ois.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		return savedToken;
	}

	
	private static AccessToken exchangeCredentialsForOauthAccessToken(
			final String username, final String password,
			final N0ticeApi n0ticeApi) throws ParsingException,
			NotFoundException, NotAllowedException, AuthorisationException,
			BadRequestException, InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException {

		final AccessToken accessToken = n0ticeApi.authUser(CONSUMER_KEY, username, password, CONSUMER_SECRET);
		Log.d(TAG, "Access token: " + accessToken.getToken() + ", access secret: " + accessToken.getSecret());
		return accessToken;
	}
	
}
