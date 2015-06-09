package com.fab;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

public class Home extends Activity implements Constant,LocationListener {
	private Uri fileUri;
	private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
	public static final int MEDIA_TYPE_IMAGE = 1;
	private static AutoCompleteTextView mobNumber;
	private SQLiteDatabase sqliteDatabase;
	private ArrayList<String> mob = new ArrayList<String>();
	private String mobile, provider=LocationManager.GPS_PROVIDER;
	private Double latitude,longitude;
	private ProgressDialog pDialog;
	private FTPClient ftpClient;
	private Button butNewMob, butSearchMob, showMap;
	private double slat, slng;
	private LocationManager locationManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		mobNumber = (AutoCompleteTextView)findViewById(R.id.mobNumber);
		sqliteDatabase = SqlDbHelper.getInstance(this).getWritableDatabase();
		pDialog = new ProgressDialog(this);
		butNewMob = (Button)findViewById(R.id.newPic);
		butSearchMob = (Button)findViewById(R.id.searchPic);
		showMap = (Button)findViewById(R.id.showMap);

		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(provider, 1000, 1, this);

		Cursor c = sqliteDatabase.rawQuery("select Mobile from fab", null);
		c.moveToFirst();
		while(!c.isAfterLast()){
			mob.add(c.getString(c.getColumnIndex("Mobile")));
			c.moveToNext();
		}
		c.close();

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line, mob);
		mobNumber.setAdapter(adapter);

	}

	public void showMeDirection(View button){
		sqliteDatabase.execSQL("update fab set Latitude="+latitude+", Longitude="+longitude+",IsSync=1 where Mobile='"+mobile+"'");
		String uri = String.format(Locale.ENGLISH,"http://maps.google.com/maps?saddr=%f,%f&daddr=%f,%f", slat, slng, latitude, longitude);
		Intent i = new Intent(android.content.Intent.ACTION_VIEW,Uri.parse(uri));
		i.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
		startActivity(i);
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

	public void newPic(View button){
		//	mobile = mobNumber.getText().toString();
		captureImage();
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
		Intent i = new Intent(Home.this, UploadImage.class);
		i.putExtra("filePath", fileUri.getPath());
		i.putExtra("mobile", mobNumber.getText().toString());
		i.putExtra("latitude", slat);
		i.putExtra("longitude", slng);
		startActivity(i);
	}

	public void searchPic(View button){
		mobile = mobNumber.getText().toString();
		try{
			sqliteDatabase.execSQL("insert into fab(Mobile) values('"+mobile+"')");
		}catch(SQLException e){
			e.printStackTrace();
		}

		InputMethodManager imm = (InputMethodManager)getSystemService(
				Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mobNumber.getWindowToken(), 0);

		if(mobile.length()!=10 || mobile.equals("")){
			Alert alert = new Alert(Home.this);
			alert.showAlert("Please enter valid number.");
		}else{
			new getLatLong().execute();
		}
	}

	private class getLatLong extends AsyncTask<Void, String, String> {
		JSONObject jsonReceived;
		String server = "144.76.168.87";
		int port = 21;
		String username = "saurabhdev";
		String password = "saurabhschoolcom123";

		boolean latlngFlag = false;
		boolean imageFlag = false;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog.setMessage("Searching web...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}

		@Override
		protected String doInBackground(Void... params) {
			ftpClient = new FTPClient();
			JSONObject ack_json = new JSONObject();
			try {
				ack_json.put("mobile", mobile);
				jsonReceived = UploadSyncParser.makePostRequest(GET_LAT_LONG, ack_json);

				if(jsonReceived.getInt("success")==1){
					latlngFlag = true;
					try{
						latitude = Double.parseDouble(jsonReceived.getString("latitude"));
						longitude = Double.parseDouble(jsonReceived.getString("longitude"));
					}catch(NumberFormatException e){
						latlngFlag = false;
					}
					//	sqliteDatabase.execSQL("insert into fab(Mobile,Latitude,Longitude) values('"+mobile+"',"+sArray[0]+","+sArray[1]+")");
				}else{
					latlngFlag = false;
				}

				try {
					ftpClient.connect(server, port);
					ftpClient.login(username, password);
				//	ftpClient.enterLocalPassiveMode();
					ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

					String filePath = "fabfresh/android/"+mobile+".jpg";
					InputStream inputStream = ftpClient.retrieveFileStream(filePath);
					int returnCode = ftpClient.getReplyCode();
					if (inputStream == null || returnCode == 550) {
						imageFlag = false;
					}else{
						
						File dir = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/FabFresh/");
						dir.mkdirs();
						
						File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/FabFresh/"+mobile+".jpg");
						/*OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
						boolean success = ftpClient.retrieveFile(filePath, outputStream);
						outputStream.close();*/
						OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
						byte[] bytesArray = new byte[4096];
						int bytesRead = -1;
						while ((bytesRead = inputStream.read(bytesArray)) != -1) {
							outputStream.write(bytesArray, 0, bytesRead);
						}

						boolean success = ftpClient.completePendingCommand();
						if (success) {
							imageFlag = true;
						}
						outputStream.close();
						inputStream.close();
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				} finally {
					try {
						if (ftpClient.isConnected()) {
							ftpClient.logout();
							ftpClient.disconnect();
						}
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			} catch (ConnectException e) {
				e.printStackTrace();
			} catch (JSONException e){
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			try {
				if ((pDialog != null) &&pDialog.isShowing()) {
					pDialog.dismiss();
				}
			} catch (final IllegalArgumentException e) {
			} catch (final Exception e) {
			} finally {
				pDialog = null;
			}

			if(latlngFlag && !imageFlag){
				butSearchMob.setVisibility(View.GONE);
				butNewMob.setVisibility(View.VISIBLE);
				if(latitude.intValue()!=0 && longitude.intValue()!=0){
					showMap.setVisibility(View.VISIBLE);
				}				
				mobNumber.setKeyListener(null);
				//	mobNumber.setEnabled(false);
			}else if(latlngFlag && imageFlag && latitude.intValue()!=0 && longitude.intValue()!=0 && !latitude.toString().equals("") &&  !longitude.toString().equals("")){
				Intent i = new Intent(Home.this, DownloadImage.class);
				i.putExtra("mobile", mobile);
				i.putExtra("latitude", latitude);
				i.putExtra("longitude", longitude);
				startActivity(i);
			}else{
				Intent intent = getIntent();
				finish();
				startActivity(intent);
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable("file_uri", fileUri);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		fileUri = savedInstanceState.getParcelable("file_uri");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.home, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.refresh) {
			Intent intent = getIntent();
			finish();
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onPause() {
		super.onPause();
		locationManager.removeUpdates(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		pDialog = new ProgressDialog(this);
		locationManager.requestLocationUpdates(provider, 1000, 1, this);
	}

	@Override
	public void onLocationChanged(Location location) {
		if(location!=null){
			slat = location.getLatitude();
			slng = location.getLongitude();
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}

	@Override
	public void onProviderEnabled(String provider) {}

	@Override
	public void onProviderDisabled(String provider) {}
}

