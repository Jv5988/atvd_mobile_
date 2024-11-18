package com.example.mapa;

import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.mapa.databinding.ActivityMapsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.text.DecimalFormat;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private GoogleMap mMap;
    private int mapType = GoogleMap.MAP_TYPE_NORMAL;
    private boolean isNorthUp = true;
    private ActivityMapsBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private DatabaseActivity databaseActivity;

    private LatLng previousLocation;
    private double totalDistance = 0.0;
    private long startTime;
    private boolean isTracking = false;

    private TextView distanceTextView;
    private TextView speedTextView;
    private TextView timerTextView;

    private Handler timerHandler = new Handler();
    private Runnable updateTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isTracking) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                updateTimerDisplay(elapsedTime);
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    //Inicializa as comfigurações de banco, api da localização, botões, etc...
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Context context = getApplicationContext();
        File dbFile = context.getDatabasePath("TrilhaDB");
        String dbPath = dbFile.getAbsolutePath();

        Log.d("Database Path", dbPath);

        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupMapAndNavigationOptions();

        databaseActivity = new DatabaseActivity(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupLocationUpdates();

        Button startButton = findViewById(R.id.startButton);
        Button stopButton = findViewById(R.id.stopButton);

        distanceTextView = findViewById(R.id.distanceTextView);
        speedTextView = findViewById(R.id.speedTextView);
        timerTextView = findViewById(R.id.timeTextView);

        startButton.setOnClickListener(v -> startLocationUpdates());
        stopButton.setOnClickListener(v -> stopLocationUpdates());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Button viewTrailButton = findViewById(R.id.viewTrailButton);
        viewTrailButton.setOnClickListener(v -> {
            Intent intent = new Intent(MapsActivity.this, ViewTrailActivity.class);
            startActivity(intent);
        });
    }

    //Tipos de mapas(vetorial ou satélite) e definindo o modo de navegação NorthUp como padrão
    private void setupMapAndNavigationOptions() {
        RadioGroup mapTypeGroup = findViewById(R.id.mapTypeGroup);
        RadioGroup navigationModeGroup = findViewById(R.id.navigationModeGroup);

        mapTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.vectorMap) {
                mapType = GoogleMap.MAP_TYPE_NORMAL;
            } else if (checkedId == R.id.satelliteMap) {
                mapType = GoogleMap.MAP_TYPE_SATELLITE;
            }
            if (mMap != null) mMap.setMapType(mapType);
        });

        navigationModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            isNorthUp = (checkedId == R.id.northUp);
        });
    }

    //Processar as atualizações
    private void setupLocationUpdates() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            private static final double MIN_DISTANCE_UPDATE = 2.0; // Distância mínima para atualizar a localização (em metros)

            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || mMap == null) {
                    return;
                }

                if (isTracking) {
                    LatLng currentLocation = new LatLng(locationResult.getLastLocation().getLatitude(),
                            locationResult.getLastLocation().getLongitude());
                    
                    if (previousLocation == null || calculateDistance(previousLocation, currentLocation) >= MIN_DISTANCE_UPDATE) {
                        saveLocationToDatabase(currentLocation);

                        MarkerOptions markerOptions = new MarkerOptions().position(currentLocation).title("Você está aqui");
                        mMap.clear();
                        mMap.addMarker(markerOptions);

                        if (previousLocation != null) {
                            totalDistance += calculateDistance(previousLocation, currentLocation);
                        }
                        previousLocation = currentLocation;

                        updateDistanceDisplay();

                        float speed = locationResult.getLastLocation().getSpeed();
                        updateSpeedDisplay(speed);

                        long elapsedTime = System.currentTimeMillis() - startTime;
                        updateTimerDisplay(elapsedTime);

                        float zoom = mMap.getCameraPosition().zoom;
                        float bearing = isNorthUp ? 0 : locationResult.getLastLocation().getBearing();

                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                .target(currentLocation)
                                .zoom(zoom)
                                .bearing(bearing)
                                .build();

                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                    }
                }
            }
        };
    }

    //Ações do botão Iniciar
    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            startTime = System.currentTimeMillis();
            isTracking = true;
            previousLocation = null;
            totalDistance = 0.0;
            timerHandler.post(updateTimerRunnable);
            TextView trailSummaryTextView = findViewById(R.id.trailSummaryTextView);
            trailSummaryTextView.setText("");
            databaseActivity.clearAllData();

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }


    //Ações do botão Parar
    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        isTracking = false;
        timerHandler.removeCallbacks(updateTimerRunnable);
        Toast.makeText(this, "Captura de localização interrompida", Toast.LENGTH_SHORT).show();

        long elapsedTime = System.currentTimeMillis() - startTime;

        double averageSpeed = totalDistance / (elapsedTime / 1000.0);
        double averageSpeedKmH = averageSpeed * 3.6;

        long seconds = (elapsedTime / 1000) % 60;
        long minutes = (elapsedTime / (1000 * 60)) % 60;
        long hours = (elapsedTime / (1000 * 60 * 60)) % 24;

        String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        String startTimeFormatted = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                .format(new java.util.Date(startTime));

        String summary = "Início: " + startTimeFormatted + "\n" +
                "Duração: " + time + "\n" +
                "Distância: " + new DecimalFormat("0.0").format(totalDistance) + " m\n" +
                "Velocidade Média: " + new DecimalFormat("0.0").format(averageSpeedKmH) + " km/h";

        TextView trailSummaryTextView = findViewById(R.id.trailSummaryTextView);
        trailSummaryTextView.setText(summary);
        trailSummaryTextView.setTextColor(Color.WHITE);
    }


    //Salva a posição do usuário no banco de dados
    private void saveLocationToDatabase(LatLng location) {
        Log.d("Location", "Latitude: " + location.latitude + ", Longitude: " + location.longitude);
        databaseActivity.addLocation(location.latitude, location.longitude);
        Toast.makeText(this, "Localização salva no banco de dados", Toast.LENGTH_SHORT).show();
    }

    //Calcula a distância do percurso
    private double calculateDistance(LatLng start, LatLng end) {
        double earthRadius = 6371000;
        double lat1 = Math.toRadians(start.latitude);
        double lon1 = Math.toRadians(start.longitude);
        double lat2 = Math.toRadians(end.latitude);
        double lon2 = Math.toRadians(end.longitude);

        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;

        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dlon / 2) * Math.sin(dlon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
    }

    //Atualiza a Velocidade
    private void updateSpeedDisplay(float speed) {
        DecimalFormat df = new DecimalFormat("0.0");
        String speedText = df.format(speed * 3.6) + " km/h";
        speedTextView.setText("Velocidade: " + speedText);
    }

    //Atualiza o cronometro
    private void updateTimerDisplay(long elapsedTime) {
        long seconds = (elapsedTime / 1000) % 60;
        long minutes = (elapsedTime / (1000 * 60)) % 60;
        long hours = (elapsedTime / (1000 * 60 * 60)) % 24;

        String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        timerTextView.setText("Tempo: " + time);
    }

    //Atualiza a distância
    private void updateDistanceDisplay() {
        DecimalFormat df = new DecimalFormat("0.0");
        String distanceText = df.format(totalDistance) + " m";
        distanceTextView.setText("Distância: " + distanceText);
    }

    //Configura o mapa
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(mapType);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 3));

                                mMap.addMarker(new MarkerOptions().position(userLocation).title("Você está aqui"));
                            }
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
}
