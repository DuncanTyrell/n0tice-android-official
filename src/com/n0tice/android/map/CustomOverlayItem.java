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

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class CustomOverlayItem extends OverlayItem {

	private String mTarget;

	public CustomOverlayItem(GeoPoint point, String title, String snippet, String body, String image, String path) {
		
		super(point, title, snippet);
		this.mTarget = path;
	}
	
		public String getTarget() {
			return mTarget;
		}
}