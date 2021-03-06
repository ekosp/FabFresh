package com.fab;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class UploadSyncParser {
	private static InputStream is = null;
	private static JSONObject jObj = null;
	private static String json = "";
	private static boolean flag;

	public static JSONObject makePostRequest(String urlUlAnswered,JSONObject jsonn) throws ConnectException {
		Log.d("req", jsonn+"");
		try{
			flag = false;
			HttpPost httpPostRequest = new HttpPost(urlUlAnswered);
			HttpParams param = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(param, 10000);
			HttpConnectionParams.setSoTimeout(param, 10000);
			DefaultHttpClient httpclient = new DefaultHttpClient(param);	
			StringEntity se = new StringEntity(jsonn.toString());
			httpPostRequest.setEntity(se);
			httpPostRequest.setHeader("Accept", "application/json");
			httpPostRequest.setHeader("content-type", "application/json");
			HttpResponse response = (HttpResponse) httpclient.execute(httpPostRequest);
			HttpEntity entity = response.getEntity();
			is = entity.getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					is, "iso-8859-1"), 8);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			is.close();
			json = sb.toString();
			Log.d("Received data", sb+"");
			jObj = new JSONObject(json);
		}catch (ConnectTimeoutException e){
			e.printStackTrace();
			flag = true;
		}catch(ConnectException e){
			e.printStackTrace();
			flag = true;
		}catch(SocketException e){
			e.printStackTrace();
			flag = true;
		}catch(UnknownHostException e){
			e.printStackTrace();
			flag = true;
		}catch (ClientProtocolException e) {
			e.printStackTrace();
			flag = true;
		}catch (IOException e) {
			e.printStackTrace();
			flag = true;
		}catch (IllegalStateException e) {
			e.printStackTrace();
			flag = true;
		}catch (JSONException e) {
			e.printStackTrace();
			flag = true;
		}finally{
			if(flag){
				try{
					jObj = new JSONObject();
					jObj.put("success", 0);
				}catch(JSONException e){
					e.printStackTrace();
				}
			}
		}
		return jObj;
	}

}
