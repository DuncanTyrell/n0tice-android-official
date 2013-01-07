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

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.n0tice.android.R;

public class BoardListAdapter extends BaseAdapter {

	private Activity activity;
	private ArrayList<HashMap<String, String>> data;
	private static LayoutInflater inflater=null;
	public static ImageLoader imageLoader;
	private int featuredBlue;

	public BoardListAdapter(Activity a, ArrayList<HashMap<String, String>> d){
		activity = a;
		featuredBlue = a.getResources().getColor(R.color.dark_blue);
		data=d;
		inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		imageLoader=new ImageLoader(activity.getApplicationContext());
	}

	@Override
	public int getCount() {
		int count = 0;
		if (data != null) count = data.size();
		return count;
	}

	@Override
	public Object getItem(int position) {
		return data.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View vi=convertView;
		if (convertView==null)
			vi = inflater.inflate(R.layout.list_row, null);

		TextView title = (TextView)vi.findViewById(R.id.item_title);
		TextView description = (TextView)vi.findViewById(R.id.item_subtitle);
		TextView location = (TextView)vi.findViewById(R.id.list_loc); 
		location.setVisibility(View.GONE);
		TextView user = (TextView)vi.findViewById(R.id.post_user);
		TextView votes = (TextView)vi.findViewById(R.id.interesting_votes);
		ImageView thumb_image= (ImageView)vi.findViewById(R.id.thumbnail);

		HashMap<String, String> listContent = new HashMap<String, String>();
		listContent = data.get(position);

		// Setting all values in listview
		title.setText(listContent.get("title"));

		String shortBody = listContent.get("description");

		if (shortBody != null) {
			if (shortBody.length()>140)	shortBody = new String(shortBody.substring(0,140) +"...");
			description.setText(shortBody);
		}

		String featured = listContent.get("featured"); 

		if (featured != null) {
			if (featured.equals("true"))
			location.setVisibility(View.VISIBLE);
			location.setText("FEATURED BOARD");
			location.setTextColor(featuredBlue);
			featured = null;
		} 

		String type = listContent.get("type");

		if (type.equals("board")) {
			thumb_image.setImageResource(R.drawable.ic_board);
		} 
		
		String image = null;
		
		if (listContent.get("image") != null) {
			image = listContent.get("image");
		} else if (listContent.get("background") != null) {
			image = listContent.get("background");
			thumb_image.setScaleType(ImageView.ScaleType.CENTER_CROP);
		}

		if (image != null) {
			thumb_image.setVisibility(View.VISIBLE);
			imageLoader.DisplayImage(image, thumb_image);
		} else {
			thumb_image.setVisibility(View.GONE);
		}
		
		return vi;
	}

}