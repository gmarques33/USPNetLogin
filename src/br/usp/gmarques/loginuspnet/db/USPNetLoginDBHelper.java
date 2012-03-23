package br.usp.gmarques.loginuspnet.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class USPNetLoginDBHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "uspnet.db";
	private static final int DATABASE_VERSION = 1;

	public static final String TABLE_NAME = "USPNET";

	public static final String COLUMN_USERNAME = "username";
	public static final String COLUMN_PASSWORD = "password";

	public static final String[] COLUMNS = new String[] { COLUMN_USERNAME, COLUMN_PASSWORD };

	public USPNetLoginDBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE_NAME + "( " + COLUMN_USERNAME + " TEXT, " + COLUMN_PASSWORD + " TEXT);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.v(USPNetLoginDBHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");

		db.execSQL("DROP TABLE IF EXISTS" + TABLE_NAME);
		onCreate(db);
	}

}
