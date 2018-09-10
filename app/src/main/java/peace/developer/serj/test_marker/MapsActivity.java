package peace.developer.serj.test_marker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    final int PERMISSION_ACCESS_COARSE_LOCATION = 1;
    LocationManager mLocationManager;
    Geocoder geocoder;
    List<Address> addresses;
    Button gpsBtn,backToUser;
    TextView alert;
    final int GPS_ENABLED_CODE = 5;
    TextView addressText;
    ProgressBar progressBar;
    boolean GPS_enable_status;
    private final LocationListener mLocationListener = new LocationListener() {


        @Override
        public void onLocationChanged(Location location) {
            getAddressByLatLng(location.getLatitude(),location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(),location.getLongitude())));
            alert.setVisibility(View.GONE);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        alert = findViewById(R.id.alert);
        gpsBtn = findViewById(R.id.gps_enable);
        backToUser = findViewById(R.id.back_to_User);
        addressText = findViewById(R.id.address_txt);
        progressBar = findViewById(R.id.progress);
        geocoder = new Geocoder(this, Locale.getDefault());
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        GPS_enable_status = mLocationManager.isProviderEnabled( LocationManager.GPS_PROVIDER );
        if ( !GPS_enable_status ) {
            askForGPS();
        } else {
            requestMyLocation();
        }




        gpsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), GPS_ENABLED_CODE);
            }
        });
        backToUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestMyLocation();
            }
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng startPoint = new LatLng(-55.750219, 37.620709);
        mMap.moveCamera(CameraUpdateFactory.zoomTo(16));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(startPoint));


        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                LatLng midLatLng = mMap.getCameraPosition().target;
                getAddressByLatLng(midLatLng.latitude,midLatLng.longitude);
            }
        });
        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                if(GPS_enable_status)
                    switchProgress(true);
            }
        });
    }

    private void checkMyPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_ACCESS_COARSE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_ACCESS_COARSE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // All good!
                    Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
                    if (GPS_enable_status)
                        requestMyLocation();
                    else
                        askForGPS();


                } else {

                    Toast.makeText(this, "Give this app location permission!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case GPS_ENABLED_CODE:
                if(resultCode == 0){
                    gpsBtn.setVisibility(View.INVISIBLE);
                    GPS_enable_status = true;
                    requestMyLocation();

                }

        }
    }

    private void askForGPS(){
        Toast.makeText(this,"Please, turn on GPS",Toast.LENGTH_LONG).show();
        gpsBtn.setVisibility(View.VISIBLE);
    }

    private void requestMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkMyPermission();
        } else {
            mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocationListener, null);
            switchProgress(true);
            alert.setVisibility(View.VISIBLE);
        }

    }
    private void getAddressByLatLng(double lat, double lon) {
        try {
            addresses = geocoder.getFromLocation(lat,lon,1);
        } catch (IOException e) {
            e.printStackTrace();
            switchProgress(false);
            addressText.setText("Что то пошло не так");

        }
        if(addresses != null && addresses.size() != 0){
            addressText.setText(addresses.get(0).getAddressLine(0));
            switchProgress(false);

        }
        else {
            addressText.setText("Нет адреса");
        }
    }
    private void switchProgress(boolean showProgress){
        int progress = showProgress ? View.VISIBLE : View.GONE;
        int address = showProgress ? View.GONE : View.VISIBLE;
        addressText.setVisibility(address);
        progressBar.setVisibility(progress);
    }
}
