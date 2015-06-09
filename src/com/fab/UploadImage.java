package com.fab;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.io.CopyStreamAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class UploadImage extends Activity implements Constant{
	private ProgressBar progressBar;
	private String filePath, mobile, compressedFilePath;
	private TextView txtPercentage;
	private ImageView imgPreview;
	long totalSize = 0;
	private FTPClient ftpClient;
	private SQLiteDatabase sqliteDatabase;
	private double lat, lng;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upload_image);
		sqliteDatabase = SqlDbHelper.getInstance(this).getWritableDatabase();

		txtPercentage = (TextView) findViewById(R.id.txtPercentage);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		imgPreview = (ImageView) findViewById(R.id.imgPreview);

		Intent i = getIntent();
		filePath = i.getStringExtra("filePath");
		mobile = i.getStringExtra("mobile");		
		lat = i.getDoubleExtra("latitude", 0.0);
		lng = i.getDoubleExtra("longitude", 0.0);
		
		Toast.makeText(UploadImage.this, lat+" - "+lng, Toast.LENGTH_SHORT).show();
		Log.d("upload", lat+" - "+lng+" $ "+mobile);
		
		sqliteDatabase.execSQL("update fab set IsSync=0 where Mobile='"+mobile+"'");

		CompressBitmap cb = new CompressBitmap(filePath, mobile);
		compressedFilePath = cb.getComressFile();

		if (compressedFilePath != null) {
			previewMedia();
		} else {
			Toast.makeText(getApplicationContext(),"Sorry, file path is missing!", Toast.LENGTH_LONG).show();
		}
		
		File deleteFile = new File(filePath);
		deleteFile.delete();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}	

	private void previewMedia(){
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 8;
		final Bitmap bitmap = BitmapFactory.decodeFile(compressedFilePath, options);
		imgPreview.setImageBitmap(bitmap);
	}

	private class pushLatLong extends AsyncTask<Void, String, String> {
		private JSONObject jsonReceived;
		private JSONArray jsonArray = new JSONArray();
		private ArrayList<String> mobList = new ArrayList<String>();
		private ArrayList<Double> latList = new ArrayList<Double>();
		private ArrayList<Double> lngList = new ArrayList<Double>();

		@Override
		protected String doInBackground(Void... params) {
			JSONObject json = new JSONObject();
			Cursor c = sqliteDatabase.rawQuery("select Mobile,Latitude,Longitude from fab where IsSync=0", null);
			c.moveToFirst();
			while(!c.isAfterLast()){
				mobList.add(c.getString(c.getColumnIndex("Mobile")));
				latList.add(c.getDouble(c.getColumnIndex("Latitude")));
				lngList.add(c.getDouble(c.getColumnIndex("Longitude")));
				c.moveToNext();
			}
			c.close();

			try{
				for(int i=0,len=mobList.size(); i<len; i++){
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("mobile", mobList.get(i));
					jsonObject.put("latitude", latList.get(i));
					jsonObject.put("longitude", lngList.get(i));
					jsonArray.put(jsonObject);
				}
				json.put("details", jsonArray);

				jsonReceived = UploadSyncParser.makePostRequest(PUSH_LAT_LONG, json);
				if(jsonReceived.getInt("success")==1){
					for(String mob: mobList){
						sqliteDatabase.execSQL("update fab set IsSync=1 where Mobile='"+mob+"'");
					}
				}

			}catch(JSONException e){
				e.printStackTrace();
			}catch (ConnectException e) {
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(String result){
			super.onPostExecute(result);
			new UploadFileToServer().execute();
		}
	}

	private class UploadFileToServer extends AsyncTask<Void, Integer, String> {
		String server = "144.76.168.87";
		int port = 21;
		String username = "saurabhdev";
		String password = "saurabhschoolcom123";
		CopyStreamAdapter streamListener;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressBar.setProgress(0);
			progressBar.setVisibility(View.VISIBLE);
			progressBar.setProgress(0);
			txtPercentage.setText("Uploading...");
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			progressBar.setProgress(progress[0]);
			txtPercentage.setText(String.valueOf(progress[0]) + "%");
		}

		@Override
		protected String doInBackground(Void... params) {
			ftpClient = new FTPClient();
			try {
				ftpClient.connect(server, port);
				ftpClient.login(username, password);
			//	ftpClient.enterLocalPassiveMode();
				ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

				BufferedInputStream buffIn = null;
				final File file = new File(compressedFilePath);
				buffIn = new BufferedInputStream(new FileInputStream(file), 8192);
				streamListener = new CopyStreamAdapter(){
					@Override
					public void bytesTransferred(long totalBytesTransferred,
							int bytesTransferred, long streamSize) {

						int percent = (int) (totalBytesTransferred * 100 / file
								.length());
						publishProgress(percent);

						if (totalBytesTransferred == file.length()) {
							removeCopyStreamListener(streamListener);
						}
					}
				};
				ftpClient.setCopyStreamListener(streamListener);

				String remoteFile = "fabfresh/android/"+mobile+".jpg";
				boolean done = ftpClient.storeFile(remoteFile, buffIn);
				if(done){

				}
				buffIn.close();

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

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			Intent i = new Intent(UploadImage.this, Home.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
		}
	}

	public void uploadedImage(View button){
		sqliteDatabase.execSQL("update fab set Latitude="+lat+", Longitude="+lng+" where Mobile='"+mobile+"'");
		button.setVisibility(View.GONE);
		new pushLatLong().execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	//	getMenuInflater().inflate(R.menu.upload_image, menu);
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

}


