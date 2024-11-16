package com.example.mapa;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button mapBautton=(Button) findViewById(R.id.buttonGMA);
        mapBautton.setOnClickListener(this);
    }

    public void onClick(View view){
        if(view.getId()==R.id.buttonGMA){
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
            startActivity(intent);
        }
    }
}
