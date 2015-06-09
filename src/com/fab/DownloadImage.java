package com.fab;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class DownloadImage extends Activity implements Constant,LocationListener{
	private Uri fileUri;
	private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
	public static final int MEDIA_TYPE_IMAGE = 1;
	private ImageView my_image;
	static boolean active = false;
	private String mobile,provider=LocationManager.GPS_PROVIDER;
	private double slat, slng, dlat, dlng;
	private LocationManager locationManager;
	private File file;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_download_image);
		my_image = (ImageView) findViewById(R.id.my_image);
		
		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(provider, 1000, 1, this);
		
		Intent i = getIntent();
		mobile = i.getStringExtra("mobile");
		dlat = i.getDoubleExtra("latitude", 0);
		dlng = i.getDoubleExtra("longitude", 0);
		
		file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/FabFresh/"+mobile+".jpg");
		if(file.exists()){
			String imagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/FabFresh/"+mobile+".jpg";
			my_image.setImageDrawable(Drawable.createFromPath(imagePath));
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				launchUploadActivity();         	
			} else if (resultCode == RESULT_CANCELED) {
				Toast.makeText(getApplicationContext(),"User cancelled image capture", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getApplicationContext(),"Sorry! Failed to capture image", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	private void launchUploadActivity(){
		file.delete();
		if(slat!=0 && slng!=0){
			Intent i = new Intent(DownloadImage.this, UploadImage.class);
			i.putExtra("filePath", fileUri.getPath());
			i.putExtra("mobile", mobile);
			i.putExtra("latitude", slat);
			i.putExtra("longitude", slng);
			startActivity(i);
		}else{
			Alert alert = new Alert(this);
			alert.showAlert("latitude & longitude are not locked");
		}
		
	}
	
	private void captureImage() {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
		startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
	}

	public Uri getOutputMediaFileUri(int type) {
		return Uri.fromFile(getOutputMediaFile(type));
	}

	private static File getOutputMediaFile(int type) {
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.getDefault()).format(new Date());
		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				IMAGE_DIRECTORY_NAME);

		if (!mediaStorageDir.exists()) {
			mediaStorageDir.mkdirs();
		}
		File mediaFile;
		if (type == MEDIA_TYPE_IMAGE) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator+ timeStamp + ".jpg");
		}else{
			return null;
		}
		return mediaFile;
	}

	class FileDownload extends AsyncTask<String, String, String>{
		@Override
		protected String doInBackground(String... arg0) {
			File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"FabFresh");
			if (!file.exists()) {
				file.mkdirs();
			}

			DownloadManager mgr = (DownloadManager) DownloadImage.this.getSystemService(Context.DOWNLOAD_SERVICE);
			Uri downloadUri = Uri.parse(FILE_DOWNLOAD_URL+mobile+".jpg");
			DownloadManager.Request request = new DownloadManager.Request(
					downloadUri);

			request.setAllowedNetworkTypes(
					DownloadManager.Request.NETWORK_WIFI
					| DownloadManager.Request.NETWORK_MOBILE)
					.setAllowedOverRoaming(false).setTitle("FabFresh")
					.setDescription("Downloading photo of house.")
					.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES+"/FabFresh", mobile+".jpg");

			mgr.enqueue(request);

			return null;
		}

		protected void onPostExecute(String s){
			super.onPostExecute(s);
		}
	}

	public void showMeDirection(View button){
	//	String uri = String.format(Locale.ENGLISH,"http://maps.google.com/maps?saddr=%f,%f&daddr=%f,%f", slat, slng, dlat, dlng);
	//	Intent i = new Intent(android.content.Intent.ACTION_VIEW,Uri.parse(uri));
		Intent i = new Intent(android.content.Intent.ACTION_VIEW,Uri.parse("http://maps.google.com/maps?daddr="+dlat+","+dlng));
		i.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
		startActivity(i);
	}
	
	public void updateImage(View button){
		captureImage();
	}

	@Override
	public void onStart() {
		super.onStart();
		active = true;
	} 

	@Override
	public void onStop() {
		super.onStop();
		active = false;
	}
	
	 @Override
	    protected void onPause() {
	        super.onPause();
	        locationManager.removeUpdates(this);
	    }
	    
	    @Override
	    public void onResume() {
	        super.onResume();
	    	locationManager.requestLocationUpdates(provider, 1000, 1, this);
	    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	//	getMenuInflater().inflate(R.menu.download_image, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.refresh) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onLocationChanged(Location location) {
		if(location!=null){
			slat = location.getLatitude();
			slng = location.getLongitude();
			Toast.makeText(DownloadImage.this, slat +" - "+slng, Toast.LENGTH_SHORT).show();
			Log.d("upload", slat+" - "+slng);
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}

	@Override
	public void onProviderEnabled(String provider) {}

	@Override
	public void onProviderDisabled(String provider) {}

}

