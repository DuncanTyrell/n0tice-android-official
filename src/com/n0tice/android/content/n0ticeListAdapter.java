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

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.n0tice.android.R;
import com.n0tice.api.client.model.Content;
import com.n0tice.api.client.model.Place;
import com.n0tice.api.client.model.Update;
import com.n0tice.api.client.model.User;

public class n0ticeListAdapter extends BaseAdapter {

	private Activity activity;
	private List<Content> data;
	private static LayoutInflater inflater=null;
	private static ImageLoader imageLoader;
		
	public n0ticeListAdapter(Activity a, List<Content> d){
		activity = a;
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
		if (convertView==null) vi = inflater.inflate(R.layout.new_list_row, null);
		
		TextView title = (TextView)vi.findViewById(R.id.item_title); // title
		TextView userText = (TextView)vi.findViewById(R.id.post_user); // duration
		TextView votes = (TextView)vi.findViewById(R.id.interesting_votes); // duration
		ImageView thumb_image= (ImageView)vi.findViewById(R.id.list_image); // thumb image
		Content listContent = data.get(position);
		
		// Setting all values in listview
		title.setText(listContent.getHeadline());

		User mUser = listContent.getUser();
		List<Update> mUpdates = listContent.getUpdates(); 
				
		String mBoard = listContent.getNoticeBoard();
		Place mPlace = listContent.getPlace();
		
		int mVotes = listContent.getInterestingVotes();
		
		userText.setText("n0ticed by " + mUser.getUsername() + " to "
		+ mBoard + ".n0tice.com near " + mPlace.getName());

		if (mVotes > 0) {
			votes.setVisibility(View.VISIBLE);
			votes.setText("+" + mVotes);
		} else {
			votes.setVisibility(View.GONE);
		}

		String image = null;
		
		if (mUpdates.size() > 0) {
			if (listContent.getUpdates().get(0).getImage() != null) {
				image = listContent.getUpdates().get(0).getImage().getLarge();
			}
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