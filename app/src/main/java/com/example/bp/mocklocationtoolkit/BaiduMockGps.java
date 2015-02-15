package com.example.bp.mocklocationtoolkit;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
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

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.route.WalkingRouteLine;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.baidu.mapapi.utils.DistanceUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BaiduMockGps extends Service implements  Runnable {


    boolean stopFlag = Boolean.TRUE;
    boolean mockForever = Boolean.FALSE;
    double interval = 10000;
    //4公里每小时，不做配置了
    double speed = 4;

    LocationManager locationMgr = null;
    ActionBarActivity activity = null;

    //baidu api
    WalkingRouteLine route = null;
    CoordinateConverter converter = new CoordinateConverter();
    List listPoints = new ArrayList();
    List listPointsRevers = new ArrayList();
    double totalDistance = 0;
    int currIndex = 0;
    int totalCount = 0;
    double curDistance = 0;

    //timer handle
    final Handler taskHandle = new Handler();

    String strProvider = LocationManager.GPS_PROVIDER;

    //notification
    private static int MOOD_NOTIFICATIONS = 1;

    private NotificationManager mNm;


    //location updates, callback to do sth.
    public ICallback callback;

    public void setCallbackFunc(ICallback callback) {
        this.callback = callback;
    }

    public boolean getWorkFlag() {
        return !stopFlag;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub

        mNm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about us starting.    We put an icon in the status bar.
        showNotification();
        locationMgr = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        super.onCreate();
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = "mockLocation";

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.ic_marka, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, BaiduMapActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, "mockLocationNotification Event",
                text, contentIntent);

        // Send the notification.
        // We use a layout id because it is a unique number.    We use it later to cancel.
        mNm.notify(244, notification);
    }

    @Override
    public boolean onUnbind(Intent intent) {

        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        Log.e("service", "onDestroy");
        mNm.cancelAll();
        if (stopFlag != Boolean.TRUE) {
            this.stopMock();
        }
        super.onDestroy();
    }

	/*public BaiduMockGps(ActionBarActivity activity, ICallback callback){
		
		setCallbackFunc(callback);
		this.activity = activity;	
		
		locationMgr = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
	}*/

    public void setRouteLine(WalkingRouteLine routeline, double sp) {
        route = routeline;
        speed = sp;
        totalDistance = routeline.getDistance();
        this.callback.updateLocationInfo(null, "路程全程 " + totalDistance + " 米");
        for (WalkingRouteLine.WalkingStep i : route.getAllStep()) {
            //Log.i("WalkingStep","^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
            for (LatLng j : i.getWayPoints()) {
                //Log.i("latLng", "lat:"+Double.toString(j.latitude)+" lng:"+Double.toString(j.longitude));
                //add
                listPoints.add(j);
                totalCount += 1;
            }
        }
    }

    public void setRouteLineManual(List routeList, double sp){
        listPoints.addAll(routeList);
        totalCount = listPoints.size();
        speed = sp;
        this.callback.updateLocationInfo(null, "手工路程全程 " + totalCount + " 个点");
    }


    public LatLng convertBaiduToGps(LatLng point) {
        //百度不公开算法，从网上找到一个方法
        //假设你有百度坐标：x1=116.397428，y1=39.90923
        //把这个坐标当成GPS坐标，通过接口获得他的百度坐标：x2=116.41004950566，y2=39.916979519873
        //通过计算就可以得到GPS的坐标：
        //x = 2*x1-x2，y = 2*y1-y2

        // 将GPS设备采集的原始GPS坐标转换成百度坐标
        converter.from(CoordinateConverter.CoordType.GPS);
        // sourceLatLng待转换坐标
        converter.coord(point);
        LatLng baiduLatLng = converter.convert();
        double lat, lng;
        lat = 2 * point.latitude - baiduLatLng.latitude;
        lng = 2 * point.longitude - baiduLatLng.longitude;

        return new LatLng(lat, lng);
    }

    public void mockLocation(Location loc) {
        locationMgr.setTestProviderEnabled(strProvider, true);
        locationMgr.setTestProviderStatus(strProvider, LocationProvider.AVAILABLE,
                null, System.currentTimeMillis());
        //set location at last
        locationMgr.setTestProviderLocation(strProvider, loc);
    }

    @SuppressLint("NewApi")
    public void mockLocation(LatLng point) {

        Location newLocation = new Location(strProvider);

        //convert baidu location to gsp location
        LatLng dstPoint = convertBaiduToGps(point);

        newLocation.setLatitude(dstPoint.latitude);
        newLocation.setLongitude(dstPoint.longitude);
        newLocation.setAccuracy(500);
        newLocation.setTime(System.currentTimeMillis());
        newLocation.setElapsedRealtimeNanos(9);

        mockLocation(newLocation);
    }

    public void startMock(boolean foreverFlag) {
        try {
            if (locationMgr == null) {
                locationMgr = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
            }

            //if has , remove first
            if (locationMgr.getProvider(strProvider) != null) {

                //locationMgr.removeTestProvider(strProvider);
            }

            if (listPoints.size()<=0) {
                //无路径，不走路
                callback.updateLatLngInfo(null, "无路径，不走路");
                return;
            }

            //赋值，是不是一直走下去
            mockForever = foreverFlag;

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
            stopFlag = Boolean.FALSE;
            //start handle runable
            taskHandle.postDelayed(this, 2);
        } catch (Exception ex) {
            Log.e("exception", ex.getMessage());
            this.callback.updateLocationInfo(null, "ERROR: " + ex.getMessage());
            stopFlag = Boolean.TRUE;
        }
    }

    public void stopMock() {
        try {
            stopFlag = Boolean.TRUE;

            callback.updateLatLngInfo(null, "stop");
            if (locationMgr.getProvider(strProvider) != null) {

                taskHandle.removeCallbacks(this);

                locationMgr.clearTestProviderLocation(strProvider);
                locationMgr.clearTestProviderEnabled(strProvider);
                locationMgr.removeTestProvider(strProvider);

                this.callback.updateLocationInfo(null, "Stop mocking location!");


            }

            locationMgr = null;
        } catch (Exception ex) {
            Log.e("exception", ex.getMessage());
            this.callback.updateLocationInfo(null, "ERROR:" + ex.getMessage());
            stopFlag = Boolean.TRUE;
        }
    }


    public Location getLocation() {
        try {
            if (locationMgr != null &&
                    locationMgr.getProvider(strProvider) != null) {
                return locationMgr.getLastKnownLocation(strProvider);
            }
        } catch (Exception ex) {
            Log.e("exception", ex.getMessage());
            this.callback.updateLocationInfo(null, "ERROR:" + ex.getMessage());
            return null;
        }
        return null;
    }


    @Override
    public void run() {
        //计算当前点到下一点的距离
        if (currIndex < totalCount - 1) {
            //计算p1、p2两点之间的直线距离，单位：米
            curDistance = DistanceUtil.getDistance((LatLng) listPoints.get(currIndex), (LatLng) listPoints.get(currIndex + 1));
            interval = curDistance / speed * 3600;//米，公里每小时, 微秒
            //Log.i("sleep","下一点距离现在"+curDistance+"米，休眠"+interval+"微秒");
            callback.updateLatLngInfo((LatLng) listPoints.get(currIndex), "下一点距离现在"+curDistance+"米，休眠"+interval+"微秒");

            //TODO
            //interval = 3000;
        } else if (currIndex == totalCount - 1) {
            //最后一个了，随便休眠了10秒
            interval = 10000;
            callback.updateLatLngInfo(null, "目的马上到了，休眠最后一次10秒");
        } else {
            //如果一直走，就往回走
            if(mockForever){
                //翻转集合，往回走
                Collections.reverse(listPoints);
                currIndex = 0;
                callback.updateLatLngInfo(null, "回头走，开始");
            }
            else {
                //结束了，stop
                this.stopMock();
                callback.updateLatLngInfo(null, "走路结束");
            }
        }

        taskHandle.postDelayed(this, Math.round(interval));


        //callback.updatecurrIndex(currIndex);

        mockLocation((LatLng) listPoints.get(currIndex));

        currIndex = currIndex + 1;
     }

    @Override
    public IBinder onBind(Intent it) {

        return new MsgBinder();
    }

    public class MsgBinder extends Binder{
        //获取当前实例
        public BaiduMockGps getService(){
            return BaiduMockGps.this;
        }
    }

}