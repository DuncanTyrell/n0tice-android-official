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

import java.util.ArrayList;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.n0tice.android.content.DisplayReport;


public class NoticeOverlay extends ItemizedOverlay<CustomOverlayItem> {

	private ArrayList<CustomOverlayItem> mOverlays = new ArrayList<CustomOverlayItem>();
	private Context mContext;
	private CustomOverlayItem item;
	private CustomDialog.Builder customBuilder;

	public NoticeOverlay(Drawable defaultMarker, Context context)
	{
		super(boundCenterBottom(defaultMarker));
		mContext = context;
	}

	public void addOverlay(CustomOverlayItem overlay)
	{
		mOverlays.add(overlay);
		populate();
	}
	@Override
	protected CustomOverlayItem createItem(int i)
	{
		return mOverlays.get(i);
	}
	@Override
	public int size()
	{
		return mOverlays.size();
	}
	@Override
	protected boolean onTap(int index)
	{
		Dialog dialog;

		item = mOverlays.get(index);
		customBuilder = new CustomDialog.Builder(mContext);

		customBuilder.setTitle(item.getTitle());
		customBuilder.setMessage(item.getSnippet());
		customBuilder.setTarget(item.getTarget());

		customBuilder.setPositiveButton("Read full n0tice...", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {

				Intent intent = new Intent(mContext, DisplayReport.class);
				intent.putExtra("id", customBuilder.getTarget());
				Log.i("clickurl", "" + customBuilder.getTarget());
				mContext.startActivity(intent);	
				dialog.dismiss();

			}; 	

		} );
		dialog = customBuilder.create();
		dialog.show();
		return false;

	}

	@Override
	public boolean onTouchEvent(MotionEvent event, MapView mapView) 
	{   
		return false;
	}
}

