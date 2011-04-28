package org.openbeerdata.library;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Color;
import android.provider.BaseColumns;
import android.util.Log;

public class ObDbAdapter {

	private SQLiteDatabase mDb = null;
	private static ObDbManager sDbManager = null;
	private Application mApplication;

	public static final String LOG_TAG = "OpenBeerDataLibrary";

	public static final class BjcpCategoryColumns implements BaseColumns {
		public static final String TABLE = "bjcp_categories";
		public static final String NAME = "name";
	}

	public static final class BjcpSubcategoryColumns implements BaseColumns {
		public static final String TABLE = "bjcp_subcategories";
		public static final String CATEGORY_ID = "bjcp_category_id";
		public static final String NAME = "name";
		public static final String DISPLAY_ID = "display_id";
		public static final String AROMA = "aroma";
		public static final String APPEARANCE = "appearance";
		public static final String FLAVOR = "flavor";
		public static final String MOUTHFEEL = "mouthfeel";
		public static final String IMPRESSION = "impression";
		public static final String COMMENTS = "comments";
		public static final String INGREDIENTS = "ingredients";
		public static final String OG_LOW = "og_low";
		public static final String OG_HIGH = "og_high";
		public static final String FG_LOW = "fg_low";
		public static final String FG_HIGH = "fg_high";
		public static final String IBU_LOW = "ibu_low";
		public static final String IBU_HIGH = "ibu_high";
		public static final String SRM_LOW = "srm_low";
		public static final String SRM_HIGH = "srm_high";
		public static final String ABV_LOW = "abv_low";
		public static final String ABV_HIGH = "abv_high";
		public static final String EXAMPLES = "examples";
	}

	public static final class SrmColorColumns implements BaseColumns {
		public static final String TABLE = "srm_colors";
		public static final String SRM = "srm";
		public static final String RED = "r";
		public static final String GREEN = "g";
		public static final String BLUE = "b";
	}

	public ObDbAdapter(Application application) {
		mApplication = application;
	}

	public boolean isOpen() {
		return mDb != null && mDb.isOpen();
	}

	public void open() {
		if (sDbManager == null) {
			sDbManager = new ObDbManager(mApplication);
		}
		if (!isOpen()) {
			mDb = sDbManager.getWritableDatabase();
		}
	}

	public void close() {
		if (isOpen()) {
			mDb.close();
			mDb = null;
			if (sDbManager != null) {
				sDbManager.close();
				sDbManager = null;
			}
		}
	}

	public Cursor getCategoryCursor() {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(BjcpCategoryColumns.TABLE);
		String[] asColumnsToReturn = new String[] { BjcpCategoryColumns._ID,
				BjcpCategoryColumns.NAME };

		Cursor cursor = queryBuilder.query(mDb, asColumnsToReturn, null, null,
				null, null, BjcpCategoryColumns._ID + " ASC");

		return cursor;
	}

	public Cursor getSubcategoryCursor(String categoryId) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(BjcpSubcategoryColumns.TABLE);
		String[] columnsToReturn = new String[] { BjcpSubcategoryColumns._ID,
				BjcpSubcategoryColumns.NAME, BjcpSubcategoryColumns.DISPLAY_ID };

		String[] selectionCriteria = new String[] { categoryId };

		Cursor cursor = queryBuilder.query(mDb, columnsToReturn,
				BjcpSubcategoryColumns.CATEGORY_ID + " = ?", selectionCriteria,
				null, null, BjcpSubcategoryColumns._ID + " ASC");

		return cursor;
	}

	public Cursor getSubcategoryDetailCursor(String subcategoryId) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(BjcpSubcategoryColumns.TABLE);
		String[] columnsToReturn = new String[] { BjcpSubcategoryColumns._ID,
				BjcpSubcategoryColumns.NAME, BjcpSubcategoryColumns.DISPLAY_ID,
				BjcpSubcategoryColumns.AROMA,
				BjcpSubcategoryColumns.APPEARANCE,
				BjcpSubcategoryColumns.FLAVOR,
				BjcpSubcategoryColumns.MOUTHFEEL,
				BjcpSubcategoryColumns.IMPRESSION,
				BjcpSubcategoryColumns.COMMENTS,
				BjcpSubcategoryColumns.INGREDIENTS,
				BjcpSubcategoryColumns.OG_LOW, BjcpSubcategoryColumns.OG_HIGH,
				BjcpSubcategoryColumns.FG_LOW, BjcpSubcategoryColumns.FG_HIGH,
				BjcpSubcategoryColumns.IBU_LOW,
				BjcpSubcategoryColumns.IBU_HIGH,
				BjcpSubcategoryColumns.SRM_LOW,
				BjcpSubcategoryColumns.SRM_HIGH,
				BjcpSubcategoryColumns.ABV_LOW,
				BjcpSubcategoryColumns.ABV_HIGH,
				BjcpSubcategoryColumns.EXAMPLES };

		String[] selectionCriteria = new String[] { subcategoryId };

		Cursor cursor = queryBuilder.query(mDb, columnsToReturn,
				BjcpSubcategoryColumns._ID + " = ?", selectionCriteria, null,
				null, null);

		return cursor;
	}

	public ColorPair getSrmColor(String srm) {
		ColorPair colors = new ColorPair();

		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(SrmColorColumns.TABLE);
		String[] columnsToReturn = new String[] { SrmColorColumns.RED,
				SrmColorColumns.GREEN, SrmColorColumns.BLUE };

		String[] selectionCriteria = new String[] { srm };

		Cursor cursor = queryBuilder.query(mDb, columnsToReturn,
				SrmColorColumns.SRM + " = ?", selectionCriteria, null, null,
				null);

		if (cursor.moveToFirst()) {
			colors.backgroundColor = Color
					.rgb(cursor.getInt(cursor
							.getColumnIndex(SrmColorColumns.RED)), cursor
							.getInt(cursor
									.getColumnIndex(SrmColorColumns.GREEN)),
							cursor.getInt(cursor
									.getColumnIndex(SrmColorColumns.BLUE)));
			if (Float.parseFloat(srm) < 12.7) {
				colors.textColor = Color.BLACK;
			} else {
				colors.textColor = Color.WHITE;
			}
		} else {
			colors.backgroundColor = Color.rgb(6, 2, 1);
			colors.textColor = Color.WHITE;
		}

		cursor.close();

		return colors;
	}

	private static class ObDbManager extends SQLiteOpenHelper {
		private static final String DATABASE_NAME = "openbeerdata.db";
		private static final int DATABASE_VERSION = 1;

		private Context mContext;
		private String mDatabasePath;

		public ObDbManager(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);

			mContext = context;
			mDatabasePath = context.getDatabasePath(DATABASE_NAME)
					.getAbsolutePath();
		}

		@Override
		public void onCreate(SQLiteDatabase db) {

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		}

		@Override
		public synchronized SQLiteDatabase getReadableDatabase() {
			try {
				createDatabase();
			} catch (IOException e) {
				Log.e(LOG_TAG, e.toString());
			}
			return super.getReadableDatabase();
		}

		@Override
		public synchronized SQLiteDatabase getWritableDatabase() {
			try {
				createDatabase();
			} catch (IOException e) {
				Log.e(LOG_TAG, e.toString());
			}
			return super.getWritableDatabase();
		}

		public void createDatabase() throws IOException {
			boolean dbExist = checkDatabase();
			if (!dbExist) {
				try {
					copyDatabase();
				} catch (IOException e) {
					throw new Error("Error copying database");
				}
			}
		}

		private boolean checkDatabase() {
			SQLiteDatabase checkDb = null;

			try {
				checkDb = SQLiteDatabase.openDatabase(mDatabasePath, null,
						SQLiteDatabase.OPEN_READONLY);
			} catch (SQLiteException e) {

			}

			if (checkDb != null) {
				checkDb.close();
			}

			return checkDb != null ? true : false;
		}

		private void copyDatabase() throws IOException {
			try {
				InputStream input = mContext.getAssets().open(DATABASE_NAME);
				
				File path = mContext.getDatabasePath("");
				if (!path.exists()) {
					path.mkdirs();
				}

				OutputStream output = new FileOutputStream(mDatabasePath);

				byte[] buffer = new byte[1024];
				int length;
				while ((length = input.read(buffer)) > 0) {
					output.write(buffer, 0, length);
				}

				output.flush();
				output.close();
				input.close();
			} catch (Exception e) {
				Log.e(LOG_TAG, e.toString());
				throw new IOException();
			}
		}
	}
}
