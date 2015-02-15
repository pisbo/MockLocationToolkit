package com.example.bp.mocklocationtoolkit;

import android.support.v7.app.ActionBarActivity;
import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;


import android.util.Log;

public class MyMockGps implements LocationListener, Runnable{
	
	double startLat = 31.2404440000;
	double startLng = 121.6709200000;
	double nowLat = 31.2404440000;
	double nowLng = 121.6709200000;
	
	double divider = 3000;
	double fixLat = 0.002933;
	double fixLng = 0.008956;
	
	int interval = 10000;
	int mockCount = 0;
	
	LocationManager locationMgr = null;
	ActionBarActivity activity = null;
	
    //timer handle
	final Handler taskHandle = new Handler();

	//Ĭ��ΪGPSprovider
	String strProvider = LocationManager.GPS_PROVIDER;

	//location updates, callback to do sth.
	public ICallback callback;	
	public void setCallbackFunc(ICallback callback){
		this.callback = callback;
	}
	
	public MyMockGps(ActionBarActivity activity, ICallback callback){
		
		setCallbackFunc(callback);
		this.activity = activity;	
		
		locationMgr = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
	}
	
	public void mockLocation(Location loc){
	    locationMgr.setTestProviderEnabled(strProvider, true);
	    locationMgr.setTestProviderStatus(strProvider, LocationProvider.AVAILABLE,
	    									null, System.currentTimeMillis());      
	    //set location at last
	    locationMgr.setTestProviderLocation(strProvider, loc); 
	}
	
	@SuppressLint("NewApi") public void mockLocation(double lng, double lat){
		
	    Location newLocation = new Location(strProvider);

	    //set the mockLocation latitude and longtitude
	    newLocation.setLatitude(lat - fixLat);
	    newLocation.setLongitude(lng - fixLng);
	    newLocation.setAccuracy(500);
	    newLocation.setTime(System.currentTimeMillis());
	    newLocation.setElapsedRealtimeNanos(9);
	    
	    mockLocation(newLocation);
	}
	
	public void startMock(double lng, double lat, double div){
		try{
			if(locationMgr == null){
				locationMgr = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
			}		
			
			//if has , remove first
			if (locationMgr.getProvider(strProvider) != null) {
				
				//locationMgr.removeTestProvider(strProvider);
			}
			
			locationMgr.addTestProvider
			(
				strProvider,
			    "requiresNetwork" == "",
			    "requiresSatellite" == "",
			    "requiresCell" == "",
			    "hasMonetaryCost" == "",
			    "supportsAltitude" == "",
			    "supportsSpeed" == "",
			    "supportsBearing" == "",
			    android.location.Criteria.POWER_LOW,
			    android.location.Criteria.ACCURACY_FINE
			 );  
			
			doLocationUpdates();
			
			//mock location
			nowLat = startLat = lat;
			nowLng = startLng = lng;
			divider = div;
			
			//start handle runable
			taskHandle.postDelayed(this, 2);
		}
		catch(Exception ex){
			Log.e("exception" , ex.getMessage());
			this.callback.updateLocationInfo(null, ex.getMessage());
		}
	}
	
	public void stopMock(){
		try{
	        if(locationMgr.getProvider(strProvider)!=null) {
	
	        	taskHandle.removeCallbacks(this);
	        	
	            //����provider
	        	locationMgr.clearTestProviderLocation(strProvider);
	        	locationMgr.clearTestProviderEnabled(strProvider);
	        	locationMgr.removeTestProvider(strProvider);
	
	            this.callback.updateLocationInfo(null, "Stop mocking location!");
	
	            
	            locationMgr.removeUpdates(this);
	        }
	        
	        locationMgr = null;
		}
		catch(Exception ex){
			Log.e("exception" , ex.getMessage());
			this.callback.updateLocationInfo(null, ex.getMessage());
		}
    }

	
	public Location getLocation(){
		try{
	        if(locationMgr != null &&
	           locationMgr.getProvider(strProvider)!=null) {
	        	return locationMgr.getLastKnownLocation(strProvider);
	        }
		}
		catch(Exception ex){
			Log.e("exception" , ex.getMessage());
			this.callback.updateLocationInfo(null, ex.getMessage());
			return null;
		}
		return null;
	}

	
    public void doLocationUpdates(){		
		locationMgr.requestLocationUpdates(strProvider, 0, 0, this);
	}

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
        // log it when the location changes
        if (location != null) {
        	callback.updateLocationInfo(location, "");
        }
	}

	@Override
	public void onProviderDisabled(String location) {
        // Provider��disableʱ�����˺���������GPS���ر�
    	callback.updateLocationInfo(null, "provider[" + strProvider + "] Disabled !");
	}

	@Override
	public void onProviderEnabled(String location) {
        //  Provider��enableʱ�����˺���������GPS����
    	callback.updateLocationInfo(null, "provider[" + strProvider + "] Enabled !");
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
        // Provider��ת̬�ڿ��á���ʱ�����ú��޷�������״ֱ̬���л�ʱ�����˺���
    	callback.updateLocationInfo(null, "Provider:[" + strProvider + "] is : %s" + status);
		
	}
	
	@Override
	public void run() {
		double random;
        random = Math.random() * 10;
        if (random >= 5) {
            random = Math.random()/divider;
            nowLng = nowLng + random;
        }else {
            random = Math.random()/divider;
            nowLng = nowLng - random;
        }
        random = Math.random() * 10;
        if (random >= 5) {
            random = Math.random()/divider;
            nowLat = nowLat + random;
        }else {
            random = Math.random()/divider;
            nowLat = nowLat - random;
        }

        taskHandle.postDelayed(this, interval);
        
        mockCount = mockCount + 1;
        callback.updateMockCount(mockCount);
        
        mockLocation(nowLng, nowLat);
	}
}