package com.example.bp.mocklocationtoolkit;

import android.location.Location;

import com.baidu.mapapi.model.LatLng;

public interface ICallback {   
	public void  updateLocationInfo(Location loc, String info);
    public void  updateLatLngInfo(LatLng point, String info);
	public void  updateMockCount(int count);
}  
