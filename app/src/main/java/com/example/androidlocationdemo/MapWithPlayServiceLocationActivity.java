package com.example.androidlocationdemo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapWithPlayServiceLocationActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, OnMapReadyCallback {

    private static final int PLAY_SERVICE_REQUEST = 9000;
    private static final int PERMISSION_CODE = 101;
    String[] permissions_all={Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION};
    LocationManager locationManager;
    boolean isGpsProvider;
    boolean isNetworkProvider;
    GoogleApiClient googleApiClient;
    Location location;
    GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_with_play_service_location);

        Button show_map_with_location=findViewById(R.id.show_map_with_location);

        show_map_with_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //first checking play service installed
                if(!checkPlayServiceInstalled()){
                    return;
                }

                //now checking permission and request permission

                if(Build.VERSION.SDK_INT>=23){
                    if(checkPermission()){
                        getDeviceLocation();
                    }
                    else{
                        requestPermission();
                    }
                }
                else{
                    getDeviceLocation();
                }


            }
        });

        //now showing marker with current location
        SupportMapFragment supportMapFragment=(SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(MapWithPlayServiceLocationActivity.this);

    }

    private void getDeviceLocation() {
        locationManager=(LocationManager)getSystemService(Service.LOCATION_SERVICE);
        isGpsProvider=locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkProvider=locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if(!isGpsProvider && !isNetworkProvider){
            //showing setting for enable gps
            showSettingAlert();
        }
        else{
            GetLocationData();
        }
    }

    private void GetLocationData() {
        googleApiClient=new GoogleApiClient.Builder(MapWithPlayServiceLocationActivity.this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(MapWithPlayServiceLocationActivity.this)
                .addOnConnectionFailedListener(MapWithPlayServiceLocationActivity.this)
                .build();

        googleApiClient.connect();
    }

    private void showSettingAlert() {
        AlertDialog.Builder al=new AlertDialog.Builder(MapWithPlayServiceLocationActivity.this);
        al.setTitle("Enable GPS");
        al.setMessage("Please Enable GPS");
        al.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Intent intent=new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);

            }
        });
        al.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        al.show();
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(MapWithPlayServiceLocationActivity.this,permissions_all,PERMISSION_CODE);
    }

    private boolean checkPermission() {
        for(int i=0;i<permissions_all.length;i++){
            int result= ContextCompat.checkSelfPermission(MapWithPlayServiceLocationActivity.this,permissions_all[i]);
            if(result== PackageManager.PERMISSION_GRANTED){
                continue;
            }
            else {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSION_CODE:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    getDeviceLocation();
                }
                else{
                    Toast.makeText(this, "Permission Failed", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private boolean checkPlayServiceInstalled() {
        GoogleApiAvailability apiAvailability=GoogleApiAvailability.getInstance();
        int result=apiAvailability.isGooglePlayServicesAvailable(MapWithPlayServiceLocationActivity.this);
        if(result!= ConnectionResult.SUCCESS){
            if(apiAvailability.isUserResolvableError(result)){
                apiAvailability.getErrorDialog(MapWithPlayServiceLocationActivity.this,result,PLAY_SERVICE_REQUEST).show();
                return false;
            }
            else{
                return false;
            }
        }
        else{
            return true;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        location=LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if(location!=null){
            Toast.makeText(MapWithPlayServiceLocationActivity.this, "Lat : "+location.getLatitude()+" Lng "+location.getLongitude(), Toast.LENGTH_SHORT).show();
            if(googleMap!=null){
                LatLng latLng=new LatLng(location.getLatitude(),location.getLongitude());
                googleMap.addMarker(new MarkerOptions().position(latLng).title("Current Location"));
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,10f));
            }
        }
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest=new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //10 sec
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,locationRequest,MapWithPlayServiceLocationActivity.this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        if(googleApiClient!=null){
            if(googleApiClient.isConnected()){
                LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,MapWithPlayServiceLocationActivity.this);
                googleApiClient.disconnect();
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        //Toast.makeText(MapWithPlayServiceLocationActivity.this, "Lat : "+location.getLatitude()+" Lng "+location.getLongitude(), Toast.LENGTH_SHORT).show();
        if(googleMap!=null){
            LatLng latLng=new LatLng(location.getLatitude(),location.getLongitude());
            googleMap.addMarker(new MarkerOptions().position(latLng).title("Current Location"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,10f));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //if you need to diable rotation
        googleMap.getUiSettings().setRotateGesturesEnabled(false);
        //if you need to disable zooming
        googleMap.getUiSettings().setZoomGesturesEnabled(false);

        //now zooming and rotation now working

        //we can also customize map
        //googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        this.googleMap=googleMap;
        //let fixed map loading problem
        // i missed api key


    }
}
