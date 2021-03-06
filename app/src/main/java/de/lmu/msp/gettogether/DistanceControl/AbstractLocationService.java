package de.lmu.msp.gettogether.DistanceControl;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import de.lmu.msp.gettogether.Connection.PayloadSender;

public abstract class AbstractLocationService extends Service {

    private static String TAG = "AbstractLocationService";
    protected Intent intent;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG,"onStartCommand()");
        if(payloadSender == null)
            payloadSender = new PayloadSender();
        this.intent = intent;
        start();
        return super.onStartCommand(intent, flags, startId);
    }

    private void start(){
        Log.i(TAG,"start()");
        setLocationListener();
        setUpdateDistance();
        setUpdateTime();
        checkLocation();
    }

    protected LocationManager locationManager = null;
    protected PayloadSender payloadSender;
    protected Location myLocation;
    protected LocationListener listener;

    protected int updateTime;
    protected int updateDistance;

    protected abstract void setLocationListener();
    protected abstract void setUpdateTime();
    protected abstract void setUpdateDistance();

    protected void checkLocation(){
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            // should not be possible to get here, since permission is checked on startup
            // though needed for compiling
            Log.i(TAG,"Checking Location not possible - Permission missing");
            return;
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            myLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            locationManager.requestLocationUpdates(updateTime,updateDistance,new Criteria(),listener,null);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    updateTime, updateDistance, listener);
        }
    }

}
