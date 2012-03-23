package br.usp.gmarques.loginuspnet.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class USPNetLoginDataSource {

	private SQLiteDatabase database;
	private USPNetLoginDBHelper dbHelper;

	public USPNetLoginDataSource(Context context) {
		this.dbHelper = new USPNetLoginDBHelper(context);
	}

	public void open() throws SQLException {
		this.database = this.dbHelper.getWritableDatabase();
	}

	public void close() {
		this.dbHelper.close();
	}

	// update cardapio
	public long setUsernameAndPassword(String username, String password) {

		ContentValues cv = new ContentValues();
		long returnValue = 0;

		this.database.delete(USPNetLoginDBHelper.TABLE_NAME, null, null);
		cv.put(USPNetLoginDBHelper.COLUMN_USERNAME, username);
		cv.put(USPNetLoginDBHelper.COLUMN_PASSWORD, password);
		returnValue = this.database.insert(USPNetLoginDBHelper.TABLE_NAME, null, cv);
		
		return returnValue;
	}

	public String getUsername() {

		Cursor cursor = null;
		String username = null;
		
		cursor = this.database.query(USPNetLoginDBHelper.TABLE_NAME, new String[] {USPNetLoginDBHelper.COLUMN_USERNAME}, null, null, null, null, null);
		if(cursor.moveToFirst())
			username = cursor.getString(0);
		else
			username = "";
		
		cursor.close();

		return username;
	}

	public String getPassword() {

		Cursor cursor = null;
		String password = null;
		
		cursor = this.database.query(USPNetLoginDBHelper.TABLE_NAME, new String[] {USPNetLoginDBHelper.COLUMN_PASSWORD}, null, null, null, null, null);
		if(cursor.moveToFirst())
			password = cursor.getString(0);
		else
			password = "";
		
		cursor.close();
		
		return password;
	}
}
