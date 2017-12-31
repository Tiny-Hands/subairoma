package com.vysh.subairoma.services;

import android.app.Service;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.vysh.subairoma.R;

import java.util.List;

/**
 * Created by Vishal on 12/28/2017.
 */

public class LocationChecker extends Service {
    private final int LOCATION_VALIDITY_DURATION_MS = 2000;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        requestLocation();
        return super.onStartCommand(intent, flags, startId);
    }

    private void requestLocation() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_LOW);
        criteria.setPowerRequirement(Criteria.POWER_LOW);

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        String provider = locationManager.getBestProvider(criteria, true);
        Location location = locationManager.getLastKnownLocation(provider);
        Log.d("mylog", "In Requesting Location");
        if (location != null && (System.currentTimeMillis() - location.getTime()) <= LOCATION_VALIDITY_DURATION_MS) {
            Log.d("mylog", "Last known location: " + location.getLatitude() + ":" + location.getLongitude());
            String countryName = getCountryName(location);
            saveToServer(countryName);
        } else {
            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setNumUpdates(1);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            Log.d("mylog", "Last location too old getting new location!");
            FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            LocationCallback mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    Location loc = locationResult.getLastLocation();
                    String countryName = getCountryName(loc);
                    saveToServer(countryName);
                }
            };
            mFusedLocationClient.requestLocationUpdates(locationRequest,
                    mLocationCallback, Looper.myLooper());
            //locationManager.requestLocationUpdates();
        }
        //locationManager.requestSingleUpdate(provider, this);
    }

    private void saveToServer(String countryName) {
        Log.d("mylog", "Save to server: " + countryName);
    }

    private String getCountryName(Location location) {
        String cName = "";
        try {
            Geocoder geocoder = new Geocoder(getApplicationContext());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            cName = addresses.get(0).getCountryName();
            Log.d("mylog", "Complete address: " + addresses.toString());
            Log.d("mylog", "country name:" + cName);
        } catch (Exception ex) {
            Log.d("mylog", "Exception geocoding: " + ex.toString());
        }
        return cName;
    }
}