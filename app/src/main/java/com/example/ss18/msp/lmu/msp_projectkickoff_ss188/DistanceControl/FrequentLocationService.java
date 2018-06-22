package com.example.ss18.msp.lmu.msp_projectkickoff_ss188.DistanceControl;


import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import android.util.Log;

import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Activities.AppLogicActivity;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Utility.MessageFactory;
import com.google.android.gms.nearby.connection.Payload;


public class FrequentLocationService extends AbstractLocationService implements MessageFactory{

    private final String TAG = "FrequentLocationService";

    @Override
    protected void setUpdateDistance() {
        updateDistance = 0;
    }

    @Override
    protected void setUpdateTime() {
        updateTime = 30 * 1000; //TODO
    }

    @Override
    protected void setLocationListener() {
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.i(TAG, "LocationListener::onLocationChanged - new location data available");
                transferFabricatedMessage(location.getLatitude() + "/" + location.getLongitude());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.i(TAG, "LocationListener::onStatusChanged");
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.i(TAG, "LocationListener::onProviderEnabled - Sending of location possible.");
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.i(TAG,"LocationListener::onProviderDisabled - Sending of location not possible.");
                //startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        };
    }

    @Override
    public String fabricateMessage(String message) {
        String fabricatedMessage = "LOCATION:" + message;
        return fabricatedMessage;
        }

    @Override
    public void transferFabricatedMessage(String message) {
        String fabricatedMessage = fabricateMessage(message);
        AppLogicActivity.getInstance().getmService().broadcastMessage(fabricatedMessage);
    }
}
