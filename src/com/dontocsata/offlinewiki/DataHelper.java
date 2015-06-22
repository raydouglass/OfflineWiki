package com.dontocsata.offlinewiki;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.SQLException;
import java.util.Random;
import java.util.zip.GZIPInputStream;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

/**
 * Helps retrieve the wiki data from the sdcard<br>
 * There are 2 sqlite databases and a series of files.
 *
 * @author ray
 */
public class DataHelper {

	public static final String DIRECTORY = Environment.getExternalStorageDirectory().getAbsolutePath() + "/wiki/";

	/**
	 * Database containing the FTS table of titles
	 */
	private SQLiteDatabase searchDataBase;
	/**
	 * Database containing the location of articles and redirects
	 */
	private SQLiteDatabase lookupDatabase;
	/**
	 * Whether the data in the flat files is compressed or not. This is specified in the searchDatabase
	 */
	private boolean compressValues;

	public DataHelper() throws SQLException {
		openDataBase();
	}

	/**
	 * Get the ID of an article with the given title, following redirects up to count times
	 *
	 * @return the article ID or -1 if no article was found
	 */
	public int getId(String title, int count) {
		if (count == 0) {
			return -1;
		}
		title = title.replaceAll("_", " ");
		title = DatabaseUtils.sqlEscapeString(title);
		Cursor cursor = lookupDatabase.rawQuery("select _id, redirect from wiki where title=" + title, null);
		int id = -1;
		String redirect = null;
		while (cursor.moveToNext()) {
			id = cursor.getInt(cursor.getColumnIndex("_id"));
			redirect = cursor.getString(cursor.getColumnIndex("redirect"));
		}
		cursor.close();
		if (redirect != null) {
			return getId(redirect, --count);
		}
		return id;
	}

	public int getId(String title) {
		return getId(title, 3);
	}

	/**
	 * Get the text of the article given it's ID
	 */
	public String get(int id) throws IOException {
		if (id == -1) {
			return "Invalid ID=" + id;
		}
		String toRet = null;
		Cursor cursor = lookupDatabase.rawQuery("select position, length, file, redirect from wiki where _id=" + id,
				null);
		long position = 0;
		long length = 0;
		int file = 0;
		String redirect = null;
		if (cursor.moveToFirst()) {
			position = cursor.getLong(cursor.getColumnIndex("position"));
			length = cursor.getLong(cursor.getColumnIndex("length"));
			file = cursor.getInt(cursor.getColumnIndex("file"));
			redirect = cursor.getString(cursor.getColumnIndex("redirect"));
		}
		cursor.close();
		if (redirect != null) {
			return get(getId(redirect));
		}
		TimedLoad tl = OfflineWikiApplication.getMostRecent();
		if (tl != null) {
			tl.doneQuery();
		}
		if (length == 0) {
			return null;
		}
		@SuppressWarnings("resource")
		FileChannel channel = new RandomAccessFile(DIRECTORY + "dat." + file, "r").getChannel();
		ByteBuffer buffer = ByteBuffer.allocate((int) length);
		channel.read(buffer, position);
		channel.close();
		if (tl != null) {
			tl.doneRead();
		}
		if (compressValues) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(buffer.array()));
			byte[] buf = new byte[4096];
			int n = -1;
			while ((n = in.read(buf)) >= 0) {
				baos.write(buf, 0, n);
			}
			toRet = new String(baos.toByteArray());
		} else {
			toRet = new String(buffer.array());
		}
		if (tl != null) {
			tl.doneCompression();
		}
		return toRet;
	}

	/**
	 * Search for a particular FTS term
	 *
	 * @param term
	 * @return
	 */
	public Cursor search(String term) {
		String[] split = term.split("\\s");
		StringBuilder sb = new StringBuilder();
		for (String s : split) {
			sb.append(s + "*" + " ");
		}
		return searchDataBase.query("titles", new String[] { "_id", "title" }, "title match ?",
				new String[] { sb.toString() }, null, null, null);
	}

	public String getRandomArticles(int number) {
		int min = 10;
		int max = 46719870;
		int[] ids = new int[number * 5];
		Random r = new Random();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ids.length; i++) {
			sb.append(r.nextInt(max) + min);
			if (i < ids.length - 1) {
				sb.append(",");
			}
		}
		Cursor cursor = lookupDatabase.rawQuery("select title from wiki where _id in (" + sb.toString() + ")", null);
		sb = new StringBuilder();
		while (cursor.moveToNext()) {
			String title = cursor.getString(cursor.getColumnIndex("title"));
			sb.append("<a href=\"/").append(title.replaceAll(" ", "_"));
			sb.append("\">").append(title).append("</a><br>");
		}
		cursor.close();
		return sb.toString();
	}

	private void openDataBase() throws SQLException {
		String path = DIRECTORY + "meta.db";
		searchDataBase = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY
				+ SQLiteDatabase.NO_LOCALIZED_COLLATORS);
		Cursor cursor = searchDataBase.rawQuery("select value from meta where key='compressed_values'", null);
		while (cursor.moveToNext()) {
			compressValues = Boolean.parseBoolean(cursor.getString(0));
		}
		cursor.close();

		lookupDatabase = SQLiteDatabase.openDatabase(DIRECTORY + "wiki.db", null, SQLiteDatabase.OPEN_READONLY
				+ SQLiteDatabase.NO_LOCALIZED_COLLATORS);
	}

	public void close() {
		searchDataBase.close();
		lookupDatabase.close();
	}

}
