package com.fab;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SqlDbHelper extends SQLiteOpenHelper implements Constant {
	private static SqlDbHelper dbHelper;
	public SQLiteDatabase sqliteDatabase;

	private SqlDbHelper(Context context){
		super(context, DATABASE_NAME, null, DATABASE_VERSION);	
	}

	public static SqlDbHelper getInstance(Context context){
		if(dbHelper==null){
			dbHelper = new SqlDbHelper(context.getApplicationContext());
		}
		return dbHelper;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(FAB_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS fab");
	}

}
