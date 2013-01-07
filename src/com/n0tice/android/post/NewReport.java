/**   Copyright 2012 Tony McCrae, Eel Pie Consulting
 * 
 * Including amends by Tyrell Mobile to implement Serializable

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

import java.io.Serializable;

public class NewReport implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String headline;
	private final double latitude;
	private final double longitude;
	private final String body;
	private final byte[] image;
	private final String board;

	public NewReport(String headline, double latitude, double longitude, String body, byte[] image, String board) {
		this.headline = headline;
		this.latitude = latitude;
		this.longitude = longitude;
		this.body = body;
		this.image = image;
		this.board = board;
	}

	public String getHeadline() {
		return headline;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public String getBody() {
		return body;
	}
		
	public byte[] getImage() {
		return image;
	}

	public String getBoard() {
		return board;
	}
	@Override
	public String toString() {
		return "NewReport [headline=" + headline + ", latitude=" + latitude
				+ ", longitude=" + longitude + ", body=" + body + "]";
	}
	
}
