/**   Copyright 2012 Tyrell Mobile
 * 
 * Some elements derived from code by Tony McCrae, Eel Pie Consulting

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.n0tice.android.R;
import com.n0tice.android.TabsPager;
import com.n0tice.api.client.exceptions.HttpFetchException;
import com.n0tice.api.client.exceptions.NotFoundException;
import com.n0tice.api.client.exceptions.ParsingException;
import com.n0tice.api.client.model.Noticeboard;

public class ReportActivity extends SherlockActivity implements OnItemSelectedListener {

	private byte[] resizedImage = null;	
	private Location location = null;
	private Uri imageUri;

	private Bitmap mBitmap;
	private ImageView imagePreview;
	private Bitmap resizedBitmap;
	private TextView imageText;
	private InputMethodManager imm;
	private Spinner boardSpinner;
	private ArrayAdapter<CharSequence> boardAdapter;
	
	private String boardToPost;
	
	// variables for geocoder
	private List<Address> addresses = null;
	private List<String> addressArray;	
	private String currentLocation;

	private GetUserBoards getBoards;
	
	// OnActivityResult codes
	private static final int TAKE_IMAGE = 0;
	private static final int PICK_IMAGE = 1;
	private static final int SET_LOCATION = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.report);
		setTitle(null);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// soft keyboard control
		imm = (InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE);
		
		location = TabsPager.location;

		try {
			geoCodeLoc gcl = new geoCodeLoc(location.getLatitude(), location.getLongitude());
			gcl.execute();
		} catch (Exception e) {
			e.printStackTrace();
		}

		imagePreview = (ImageView)findViewById(R.id.imageView1);
		imageText = (TextView)findViewById(R.id.image_text);

		boardSpinner = (Spinner)findViewById(R.id.board_spinner);
		boardSpinner.setVisibility(View.GONE);

		boardAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
		
		// Specify the layout to use when the list of choices appears
		boardAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		getBoards = new GetUserBoards();
		getBoards.execute();

		// set up camera button
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		String action = intent.getAction();

		if (Intent.ACTION_SEND.equals(action)) {
			if (extras.containsKey(Intent.EXTRA_STREAM)) {
				Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
				if (uri != null) setPreviewImage(uri, true);
			}
		}		
	}

	@Override
	public void onPause() {
		super.onPause();
		getBoards.cancel(true);
		resizedImage = null;
		if (mBitmap != null) mBitmap.recycle();
	}

	@Override
	public void onResume() {
		super.onResume();

		location = TabsPager.location;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();    
		imagePreview = null;
		if (resizedBitmap != null) resizedBitmap.recycle();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.report_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case android.R.id.home :
			NavUtils.navigateUpTo(this, new Intent(this, com.n0tice.android.TabsPager.class));
			break;

		case R.id.change_post_location :
			Intent intent = new Intent(ReportActivity.this, com.n0tice.android.map.MapControl.class);
			startActivityForResult(intent, SET_LOCATION);
			break;

		case R.id.add_image :

			AlertDialog.Builder builder = new AlertDialog.Builder(ReportActivity.this);
			builder.setMessage(R.string.image_picker_dialog)
			.setPositiveButton("Take picture", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					imageUri = null;
					Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
					imageUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory() + File.separator +
							"N0TICE_IMG" + ".jpg"));

					intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
					startActivityForResult(intent, TAKE_IMAGE);
				}
			})
			.setNegativeButton("Choose picture", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					Intent intent = new Intent();
					intent.setType("image/*");
					intent.setAction(Intent.ACTION_GET_CONTENT);
					startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
			break;

		case R.id.post_n0tice :
			postReport();
			break;
		}
		return false;  
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {  
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == TAKE_IMAGE) {
			if (resultCode == Activity.RESULT_OK) {
				if (imageUri != null) setPreviewImage(imageUri, false);
			}

		} else if (requestCode == PICK_IMAGE && data != null && data.getData() != null) {
			Uri mUri = data.getData();
			if (mUri != null) setPreviewImage(mUri, true);

		} else if (requestCode == SET_LOCATION) {
			if (resultCode == Activity.RESULT_OK) {

				Bundle extras = data.getExtras();
				double rLat = extras.getInt("mLat") / 1E6;
				double rLon = extras.getInt("mLon") / 1E6;

				location.setLatitude(rLat);
				location.setLongitude(rLon);

				geoCodeLoc gcSelectLoc = new geoCodeLoc(rLat, rLon);
				gcSelectLoc.execute();
			}
		}
	}

	private void setPreviewImage(Uri uri, boolean share) {

		Log.i("Image post", "bytecount = ");

		try {
			File image;

			if (share) {
				image = new File(getRealPathFromURI(uri));
			} else {
				image = new File(uri.getPath());
			}

			int rotation = getImageRotation(image);

			mBitmap = decodeSampledBitmapFromInputStream(uri, 800, 800);
//			mBitmap = BitmapFactory.decodeStream(is);

/*			is.close();
			cr = null;
*/
			Matrix matrix = new Matrix();
			matrix.setRotate(rotation);
//			matrix.postScale(0.2f, 0.2f);

			resizedBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, false);

			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
			resizedImage = stream.toByteArray();
			stream.flush();
			stream.close();
			matrix = null;
			
			imagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
			imagePreview.setImageBitmap(resizedBitmap);
			imageText.setVisibility(View.GONE);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
    /**
     * Decode and sample down a bitmap from a file input stream to the requested width and height.
     * NOTE: once an inputstream is used once, it cannot be reused and must be recreated
     * 
     * @param fileDescriptor The file descriptor to read from
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     *         that are equal to or greater than the requested width and height
     */
    public Bitmap decodeSampledBitmapFromInputStream(
            Uri uri, int reqWidth, int reqHeight) {

		ContentResolver cr = getContentResolver();
		InputStream is = null;
		try {
			is = cr.openInputStream(uri);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        Bitmap sampleBM = null;
        if (is != null) sampleBM = BitmapFactory.decodeStream(is, null, options);


        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

		try {
			is = cr.openInputStream(uri);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return BitmapFactory.decodeStream(is, null, options);
    }

    /**
     * Calculate an inSampleSize for use in a {@link BitmapFactory.Options} object when decoding
     * bitmaps using the decode* methods from {@link BitmapFactory}. This implementation calculates
     * the closest inSampleSize that will result in the final decoded bitmap having a width and
     * height equal to or larger than the requested width and height. This implementation does not
     * ensure a power of 2 is returned for inSampleSize which can be faster when decoding but
     * results in a larger bitmap which isn't as useful for caching purposes.
     *
     * @param options An options object with out* params already populated (run through a decode*
     *            method with inJustDecodeBounds==true
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    public static int calculateInSampleSize(BitmapFactory.Options options,
            int reqWidth, int reqHeight) {
        // Raw height and width of image
        Log.i("ReportActivity", "launched insamplesize");
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
                Log.i("ReportActivity", "sample size = " + inSampleSize);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
                Log.i("ReportActivity", "sample size = " + inSampleSize);
            }

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger
            // inSampleSize).

            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down
            // further.
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }


	private String getRealPathFromURI(Uri contentUri) {
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = this.managedQuery(contentUri, proj, null, null, null);
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}

	private int getImageRotation(File imageFile) {
		ExifInterface exif = null;

		try {
			exif = new ExifInterface(imageFile.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
		int rotate = 0;

		switch(orientation) {
		case ExifInterface.ORIENTATION_ROTATE_270:
			rotate = 270;
			break;
		case ExifInterface.ORIENTATION_ROTATE_180:
			rotate = 180;
			break;
		case ExifInterface.ORIENTATION_ROTATE_90:
			rotate = 90;
			break;
		case ExifInterface.ORIENTATION_NORMAL:
			rotate = 0;
			break;
		}
		return rotate;		
	}

	private void postReport() {		
		EditText headlineInput = (EditText)findViewById(R.id.headline);
		final String headline = headlineInput.getText().toString().trim();

		if (headline.equals("")) {
			Toast.makeText(this, "Please add a title for this n0tice", Toast.LENGTH_LONG).show();
					return;
		}

		EditText bodyInput = (EditText)findViewById(R.id.body);
		final String body = bodyInput.getText().toString();	

		location = TabsPager.location;

		if (location == null) {
			location = new Location("Made up");
			location.setLatitude(51.0);
			location.setLongitude(-0.3);
		}

		
		final Intent intent = new Intent(this, ReportPostingService.class);
		final Bundle extras = new Bundle();		
		extras.putString("headline", headline);
		extras.putDouble("latitude", location.getLatitude());
		extras.putDouble("longitude", location.getLongitude());
		extras.putString("body", body);

		if (boardToPost != null) extras.putString("board", boardToPost);

		if (resizedImage != null) {
			extras.putByteArray("image", resizedImage);
		}
		intent.putExtras(extras);

		startService(intent);

		Toast.makeText(this, "Uploading new report", Toast.LENGTH_LONG).show();

		// Set everything to null
		imagePreview.setImageBitmap(null);
		resizedImage = null;
		headlineInput.setText(null);
		bodyInput.setText(null);

		// hide keyboard and call finish()
		imm.hideSoftInputFromWindow(((EditText) this.findViewById(R.id.headline)).getWindowToken(), 0);
		finish();

	}


	/** Geocoder takes lat and lon values, outputs a simple name string with no
	 * empty values. Uses an AST to avoid NetworkOnMainThread error.
	 */

	private class geoCodeLoc extends AsyncTask<URL, Integer, String > {

		Double myLat;
		Double myLon;
		Geocoder gcd = new Geocoder(ReportActivity.this);

		public geoCodeLoc(Double myLat, Double myLon) {
			this.myLat = myLat;
			this.myLon = myLon;
		}

		@Override
		protected String doInBackground(URL... urls) {
			//check have network connection   
			final ConnectivityManager conMgr =  (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();

			if (activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED) {
				//you are online	

				try {
					addresses = gcd.getFromLocation(myLat, myLon, 1);
				} catch (IOException e) {
					e.printStackTrace();
				}

				if (addresses != null && addresses.size() > 0) {
					addressArray = crunchGeoCode(addresses);
					currentLocation = addressArray.get(0) + ", " + addressArray.get(1);
				}		
			} 
			return currentLocation;
		}

		@Override
		protected void onPostExecute(String result) {
			setTitle(result);
		}

		//takes a list of addresses and generates a place name with no null fields
		private List<String> crunchGeoCode(List <Address> addresses){

			List<String> crunchStr = new ArrayList<String>();
			String loc1 = null;
			String loc2 = null;
			String locCountry = null;

			if (loc1 == null && addresses.get(0).getAddressLine(0) != null)
				loc1 = addresses.get(0).getAddressLine(0);

			if (loc1 == null)
				loc1 = "Failed to load";

			if (loc2 == null && addresses.get(0).getAddressLine(1) != null)
				loc2 = addresses.get(0).getAddressLine(1);

			if (loc2 == null)
				loc2 = "Failed to load";

			if (addresses.get(0).getCountryCode() != null)
				locCountry = addresses.get(0).getCountryCode();

			if (locCountry == null && addresses.get(0).getCountryName() != null)
				locCountry = addresses.get(0).getCountryName();

			if (locCountry == null)
				locCountry = "Failed to load";

			crunchStr.add(0, loc1);
			crunchStr.add(1, loc2);
			crunchStr.add(2, locCountry);

			return crunchStr;		
		}
	}

	private class GetUserBoards extends AsyncTask<String, Integer, List<Noticeboard>> {

		List<Noticeboard> myBoards;
		List<Noticeboard> followedBoards;

		@Override
		protected List<Noticeboard> doInBackground(String... params) {

			try {
//				Noticeboard notice = TabsPager.api.noticeBoard("northerner");
//				myBoards.add(notice);
				String username = Config.getUsername(ReportActivity.this);
				if (username != null && !username.equals("") && TabsPager.api != null) {
					myBoards = TabsPager.api.noticeboards(username);
					followedBoards = TabsPager.api.followedNoticeboards(Config.getUsername(ReportActivity.this));
				}
				myBoards.addAll(followedBoards);
				return myBoards;

			} catch (NullPointerException e) {
				e.printStackTrace();
			} catch (NotFoundException e) {
				e.printStackTrace();
			} catch (ParsingException e) {
				e.printStackTrace();
			} catch (HttpFetchException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(List<Noticeboard> results) {
			
			if (results != null) {
				if (results.size() > 0) {
					boardAdapter.add("Choose a board to post to...");
					for (int i = 0 ; i < results.size(); i++) {
						boardAdapter.add(results.get(i).getDomain());
					}
				} else {
					boardAdapter.add("Follow a board in order to post");
				}
				boardAdapter.notifyDataSetChanged();
				boardSpinner.setAdapter(boardAdapter);	
				boardSpinner.setOnItemSelectedListener(ReportActivity.this);
				boardSpinner.setVisibility(View.VISIBLE);
			} 
		}
	}
	
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
        if (pos != 0) boardToPost = parent.getItemAtPosition(pos).toString();
        Log.i("Spinner selection","Selected board = " + boardToPost);
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
}