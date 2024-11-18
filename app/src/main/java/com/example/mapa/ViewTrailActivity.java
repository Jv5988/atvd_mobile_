package com.example.mapa;

import static com.example.mapa.DatabaseActivity.COLUMN_LATITUDE;
import static com.example.mapa.DatabaseActivity.COLUMN_LONGITUDE;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ViewTrailActivity extends AppCompatActivity {

    private DatabaseActivity databaseActivity;
    private ListView locationsListView;
    private ArrayAdapter<String> adapter;
    private List<String> locationList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_trail);

        databaseActivity = new DatabaseActivity(this);

        locationsListView = findViewById(R.id.locationsListView);

        locationList = getLocationsFromDatabase();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, locationList);
        locationsListView.setAdapter(adapter);
    }

    // Método para obter as localizações do banco de dados
    public List<String> getLocationsFromDatabase() {
        List<String> locations = new ArrayList<>();
        Cursor cursor = databaseActivity.getAllLocations();

        if (cursor != null && cursor.moveToFirst()) {
            int latitudeIndex = cursor.getColumnIndex(COLUMN_LATITUDE);
            int longitudeIndex = cursor.getColumnIndex(COLUMN_LONGITUDE);

            if (latitudeIndex != -1 && longitudeIndex != -1) {
                do {
                    double latitude = cursor.getDouble(latitudeIndex);
                    double longitude = cursor.getDouble(longitudeIndex);
                    String location = "Lat: " + latitude + ", Lon: " + longitude;
                    locations.add(location);
                } while (cursor.moveToNext());
            } else {
                Log.e("Database Error", "Colunas latitude ou longitude não encontradas.");
            }
            cursor.close();
        }
        return locations;
    }
}

