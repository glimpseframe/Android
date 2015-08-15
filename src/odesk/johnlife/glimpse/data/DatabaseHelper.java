package odesk.johnlife.glimpse.data;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String DB_NAME = "Glimpse.db";
	private static final int SCHEMA_VERSION = 2;
	private static DatabaseHelper instance = null;

	private DatabaseHelper(Context context) {
		super(context, DB_NAME, null, SCHEMA_VERSION);
	}

	public static DatabaseHelper getInstance(Context context) {
		if (instance == null) {
			instance = new DatabaseHelper(context.getApplicationContext());
		}
		return instance;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(PictureData.getCreationSQL()); 
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion == 1 && newVersion == 2) {
			List<String> queries = PictureData.getAddingColumnsSQL();
			for (String query : queries) {
				db.execSQL(query); 
			}
		}
	}

	public PictureData addOrUpdate(PictureData picture) {
		return picture.toDb(getWritableDatabase());
	}
	
	/*package*/ void delete(PictureData pictureData) {
		pictureData.delete(getWritableDatabase());
	}

	/*package*/ List<PictureData> getPictures() {
		Cursor cur = getReadableDatabase().rawQuery(PictureData.getSelectionSQL(), null);
		int size = cur.getCount();
		List<PictureData> value = new ArrayList<PictureData>(size);
		for (int i=0; i<size; i++) {
			cur.moveToNext();
			value.add(new PictureData(cur));
		}
		cur.close();
		return value;
	}
	
}