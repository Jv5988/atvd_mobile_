package com.example.mapa;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseActivity extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "TrilhaDB";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_TRILHA = "Trilha";
    private static final String COLUMN_ID = "Id";
    static final String COLUMN_LATITUDE = "Latitude";
    static final String COLUMN_LONGITUDE = "Longitude";
    private static final String COLUMN_TIMESTAMP = "Timestamp";

    public DatabaseActivity(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Criação da tabela Trilha
        String CREATE_TRILHA_TABLE = "CREATE TABLE " + TABLE_TRILHA + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_LATITUDE + " REAL,"
                + COLUMN_LONGITUDE + " REAL,"
                + COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP" + ")";
        db.execSQL(CREATE_TRILHA_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRILHA);
        onCreate(db);
    }

    // Método para inserir uma posição no banco de dados
    public void addLocation(double latitude, double longitude) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LATITUDE, latitude);
        values.put(COLUMN_LONGITUDE, longitude);

        db.insert(TABLE_TRILHA, null, values);
        db.close();
    }

    // Método para obter todas as posições registradas
    public Cursor getAllLocations() {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_TRILHA;
        return db.rawQuery(selectQuery, null);
    }

    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRILHA, null, null);
        db.close();
    }
}
