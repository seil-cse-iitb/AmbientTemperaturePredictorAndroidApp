package com.android.sapantanted.ambienttemperaturepredictor.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.android.sapantanted.ambienttemperaturepredictor.model.TemperatureReading;

public class DBHelper extends SQLiteOpenHelper {
    public DBHelper(Context context) {
        super(context, "temperature_reading_db", null, 1);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS temperature_reading(sm_ph_mac_id varchar(50) not null,sm_ph_ts double not null" +
                " ,battery_temperature double not null,cpu_temperature double ,ram_usage_percent double, adt_sensor_id int not null, " +
                "actual_temperature double not null,primary key(sm_ph_mac_id,ts)) ;");
    }

    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
    }


    public boolean insertTemperatureReading(TemperatureReading tr) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("sm_ph_mac_id", tr.getMacID());
        contentValues.put("sm_ph_ts", tr.getSmphTs());
        contentValues.put("battery_temperature", tr.getBatteryTemperature());
        contentValues.put("cpu_temperature", tr.getCpuTemperature());
        contentValues.put("ram_usage_percent", tr.getRamUsagePercent());
        contentValues.put("adt_sensor_id", tr.getAdtSensorId());
        contentValues.put("actual_temperature", tr.getActualTemperature());
        if (db.insert("temperature_reading", null, contentValues) > 0)
            return true;
        else
            return false;
    }

}