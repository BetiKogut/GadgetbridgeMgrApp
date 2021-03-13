package nodomain.freeyourgadget.gadgetbridge.database.schema;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.DbManagementActivity;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class myDBHandler extends SQLiteOpenHelper {

    public myDBHandler(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }
public Cursor getAllData() {
        SQLiteDatabase db = SQLiteDatabase.openDatabase("/storage/emulated/0/Android/data/nodomain.freeyourgadget.gadgetbridge/files/Gadgetbridge",null,SQLiteDatabase.OPEN_READONLY);
        Cursor cursor = db.rawQuery("SELECT * FROM MI_BAND_ACTIVITY_SAMPLE", null);
        return  cursor;
}

public Cursor getSumSteps(){
    SQLiteDatabase db = SQLiteDatabase.openDatabase("/storage/emulated/0/Android/data/nodomain.freeyourgadget.gadgetbridge/files/Gadgetbridge", null, SQLiteDatabase.OPEN_READONLY);
    Cursor cursor = db.rawQuery("SELECT SUM(steps) FROM MI_BAND_ACTIVITY_SAMPLE", null);
    return cursor;
    }

    public Cursor getSteps(String lastTimestamp) {
        SQLiteDatabase db = SQLiteDatabase.openDatabase("/storage/emulated/0/Android/data/nodomain.freeyourgadget.gadgetbridge/files/Gadgetbridge", null, SQLiteDatabase.OPEN_READONLY);
        Cursor cursor = db.rawQuery("SELECT datetime(timestamp, 'unixepoch'), steps FROM 'MI_BAND_ACTIVITY_SAMPLE' where steps is not 0 and timestamp > strftime('%s','" + lastTimestamp + "');", null);
        return cursor;
    }
    @Override
    public void onCreate(SQLiteDatabase db) {}
    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {}
}
