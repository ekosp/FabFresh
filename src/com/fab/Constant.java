package com.fab;

public interface Constant {
	public static final String IMAGE_DIRECTORY_NAME = "FabFresh";
	public static final String FILE_DOWNLOAD_URL = "http://144.76.168.87/fabfresh/android/images/";
	public static final String GET_LAT_LONG = "http://144.76.168.87/fabfresh/android/check_mobile.php";
	public static final String PUSH_LAT_LONG = "http://144.76.168.87/fabfresh/android/update_mobile.php";
	public static String DATABASE_NAME = "fab.db";
	public static int DATABASE_VERSION = 1;
	public static String FAB_TABLE = "CREATE TABLE fab(Mobile TEXT UNIQUE, Latitude REAL DEFAULT 0.0, Longitude REAL DEFAULT 0.0, IsPhotoSync INTEGER DEFAULT 0, IsSync INTEGER DEFAULT 0)";
}
