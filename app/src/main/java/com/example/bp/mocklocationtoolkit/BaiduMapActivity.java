package com.example.bp.mocklocationtoolkit;

import java.util.ArrayList;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMap.OnMapClickListener;
import com.baidu.mapapi.map.BaiduMap.OnMapDoubleClickListener;
import com.baidu.mapapi.map.BaiduMap.OnMapLongClickListener;
import com.baidu.mapapi.map.BaiduMap.OnMapStatusChangeListener;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.WalkingRouteOverlay;
import com.baidu.mapapi.search.core.RouteLine;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.route.*;




public class BaiduMapActivity extends ActionBarActivity {

    private BaiduMockGps myGpsService;


	private MapView mMapView;
	private BaiduMap mBaiduMap;

    // 路线
    private RoutePlanSearch mSearch;
    private WalkingRouteLine route = null;

    private PlanNode stNode;
    private PlanNode enNode;

	private LatLng currentPt;

    private String touchType;

	private TextView mStateBar;
    private EditText etStart;
    private EditText etEnd;
    private EditText etSpeed;

    private Button btnStart;
    private Button btnMock;
    Button btnRotate;
    Button btnStop;
    Button btnMan;

    CheckBox cbRound;

    List listPoints = new ArrayList();
    boolean manRouteFlag = Boolean.FALSE;

    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e("test","connected");
            mStateBar.setText("Service connected");
            //返回一个MsgService对象
            myGpsService = ((BaiduMockGps.MsgBinder)service).getService();

            //注册回调接口来接收下载进度的变化
            myGpsService.setCallbackFunc(new ICallback() {
                @Override
                public void updateLocationInfo(Location loc, String info) {

                }
                @Override
                public void updateLatLngInfo(LatLng point, String info) {
                    if(point != null){
                        Log.i("Location", "latitude: "+point.latitude+"\nlongitude: "+point.longitude + " "+info);
                        currentPt = point;
                        updateMapState(info);

                        //居中，打标记
                        //setCenter(currentPt);
                        //markMap(currentPt);
                    }
                    else{
                        if(info != ""){
                            if(info == "stop"){
                                //TODO 结束了
                            }
                            Log.i("has a info: ", info);
                            mStateBar.setText(info);
                        }
                    }
                }
                @Override
                public void updateMockCount(int count) {
                }
            });

        }
    };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i("sys call" , " baidu onCreate");
		super.onCreate(savedInstanceState);
		
		SDKInitializer.initialize(getApplicationContext());
		
		setContentView(R.layout.activity_baidu_map);
		mMapView = (MapView) findViewById(R.id.bmapView);
		mBaiduMap = mMapView.getMap();
		mStateBar = (TextView) findViewById(R.id.state);
		initListener();

        //route search init
        mSearch = RoutePlanSearch.newInstance();
        mSearch.setOnGetRoutePlanResultListener(routListener);

        //edittext get now
        etStart = (EditText)findViewById(R.id.etStart);
        etEnd = (EditText)findViewById(R.id.etEnd);
        etSpeed = (EditText)findViewById(R.id.etSpeed);

        cbRound = (CheckBox)findViewById(R.id.roundCheckBox);

        //myMockGps = new BaiduMockGps(this, new locationUpdatesCallback());
        //bind service

        Intent it = new Intent(this, BaiduMockGps.class);
        startService(it);
        bindService(it, conn, Context.BIND_AUTO_CREATE);
	}


	
	private void initListener() {
		mBaiduMap.setOnMapClickListener(new OnMapClickListener() {
			public void onMapClick(LatLng point) {
				touchType = "single click";
				currentPt = point;
				updateMapState(touchType);

			}
			public boolean onMapPoiClick(MapPoi poi) {
				return false;
			}
		});
		mBaiduMap.setOnMapLongClickListener(new OnMapLongClickListener() {
			public void onMapLongClick(LatLng point) {
                if(manRouteFlag){
                    currentPt = point;
                    updateMapState("手动路线，该点作为路线记录");
                    setCenter(point);
                    markMap(point);
                    listPoints.add(point);
                }
                else {
                    touchType = "long click";
                    currentPt = point;
                    updateMapState(touchType);
                }
			}
		});
		mBaiduMap.setOnMapDoubleClickListener(new OnMapDoubleClickListener() {
			public void onMapDoubleClick(LatLng point) {
				touchType = "double click";
				currentPt = point;
				updateMapState(touchType);

                //set icon marker
                markMap(currentPt);
			}
		});
		mBaiduMap.setOnMapStatusChangeListener(new OnMapStatusChangeListener() {
            public void onMapStatusChangeStart(MapStatus status) {
                updateMapState("onMapStatusChangeStart");
            }

            public void onMapStatusChangeFinish(MapStatus status) {
                updateMapState("onMapStatusChangeFinish");
            }

            public void onMapStatusChange(MapStatus status) {
                updateMapState("onMapStatusChange");
            }
        });

        btnStart = (Button)findViewById(R.id.routebutton);
        btnStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                stNode = PlanNode.withCityNameAndPlaceName("上海", etStart.getText().toString());
                enNode = PlanNode.withCityNameAndPlaceName("上海", etEnd.getText().toString());

                Log.i("Route search", "Start:"+etStart.getText().toString()+" End:"+etEnd.getText().toString());
                mSearch.walkingSearch((new WalkingRoutePlanOption())
                        .from(stNode)
                        .to(enNode));

            }
        });

        btnMock = (Button)findViewById(R.id.mockbutton);
        btnMock.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(manRouteFlag){
                    //手动路线
                    if(listPoints.size() <= 0){
                        mStateBar.setText("请先手动构造路线，长按一个点添加");
                    }
                    else{
                        myGpsService.setRouteLineManual(listPoints, Double.parseDouble(etSpeed.getText().toString()));

                        //检查是不是无休止的来回走下去
                        myGpsService.startMock(cbRound.isChecked());
                    }
                }
                else{
                    //起点，终点路线
                    if(route != null) {
                        myGpsService.setRouteLine(route , Double.parseDouble(etSpeed.getText().toString()));

                        //检查是不是无休止的来回走下去
                        myGpsService.startMock(cbRound.isChecked());
                    }
                    else{
                        mStateBar.setText("请先构造路线");
                    }
                }

            }
        });

        btnRotate = (Button)findViewById(R.id.rotatebutton);
        btnRotate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String tmp = etStart.getText().toString();
                etStart.setText(etEnd.getText().toString());
                etEnd.setText(tmp);
            }
        });

        btnStop = (Button)findViewById(R.id.stopbutton);
        btnStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("btnStop", "stop mock");
                mStateBar.setText("主动停止走路");
                myGpsService.stopMock();
            }
        });

        btnMan = (Button)findViewById(R.id.manbutton);
        btnMan.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("btnMan", "man route");
                manRouteFlag = !manRouteFlag;
                if (manRouteFlag) {
                    mStateBar.setText("手动规划路线，长按作为一个点");
                }
                else{
                    mStateBar.setText("取消手动规划路线，按照起点终点规划路线");
                }
            }
        });
	}


    OnGetRoutePlanResultListener routListener = new OnGetRoutePlanResultListener() {
        public void onGetWalkingRouteResult(WalkingRouteResult result) {
            //获取步行线路规划结果
            if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                Toast.makeText(getApplicationContext(), "抱歉，未找到结果", Toast.LENGTH_SHORT)
                        .show();
                SuggestAddrInfo s = result.getSuggestAddrInfo();
                if(!s.getSuggestStartNode().isEmpty()) {
                    Log.i("SuggestStart", s.getSuggestStartNode().get(0).address);
                    etStart.setText(s.getSuggestStartNode().get(0).address);
                }
                if(!s.getSuggestStartNode().isEmpty()) {
                    Log.i("SuggestEnd", s.getSuggestEndNode().get(0).address);
                    etEnd.setText(s.getSuggestEndNode().get(0).address);
                }
                return;
            }

            //反正是随便走，选首选路线即可
            route = result.getRouteLines().get(0);
            WalkingRouteOverlay walkOverlay = new WalkingRouteOverlay(mBaiduMap);
            mBaiduMap.setOnMarkerClickListener(walkOverlay);

            walkOverlay.setData(route);
            walkOverlay.addToMap();
            walkOverlay.zoomToSpan();

            //tetst
            markMap(route.getAllStep().get(0).getWayPoints().get(0));
        }
        public void onGetTransitRouteResult(TransitRouteResult result) {
            //获取公交换乘路径规划结果
            //pass
        }
        public void onGetDrivingRouteResult(DrivingRouteResult result) {
            //获取驾车线路规划结果
            //pass
        }
    };

    String info;
	private void updateMapState(String extra) {
		if (mStateBar == null) {
			return;
		}

		if (currentPt == null) {
            info = "currentPt is null";
		} else {
            info = String.format("longitude : %f latitude : %f \n %s",
					currentPt.longitude, currentPt.latitude, extra);
		}
		mStateBar.setText(info);

	}

	public void returnResult(){
		Intent it = new Intent();
		Bundle data = new Bundle();
		data.putDouble("Lat", currentPt.latitude);
		data.putDouble("Lng", currentPt.longitude);
		it.putExtras(data);
		this.setResult(ActionBarActivity.RESULT_OK, it);
	}

	
	@Override
    public void onBackPressed() {
        Log.i("sys call", "onBackPressed");

		//returnResult();
        //myMockGps.stopMock();
        super.onBackPressed();
    }

	private void setCenter(LatLng point) {
        //mBaiduMap.clear();
		MapStatus mMapStatus = new MapStatus.Builder()
		.target(point)
		.build();
		MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
		mBaiduMap.setMapStatus(mMapStatusUpdate);
	}
    private void markMap(LatLng point){
        //mBaiduMap.clear();
        //构建Marker图标
        Log.i("test","mark:"+point.latitude+":"+point.longitude);
        BitmapDescriptor bitmap = BitmapDescriptorFactory
                .fromResource(R.drawable.ic_marka);
        //构建MarkerOption，用于在地图上添加Marker
        OverlayOptions option = new MarkerOptions()
                .position(point)
                .icon(bitmap);
        //在地图上添加Marker，并显示
        mBaiduMap.addOverlay(option);
    }


	@Override
	protected void onPause() {
		Log.i("sys call", " baidu onPause");
		mMapView.onPause();
		super.onPause();

		//returnResult();
		this.finish();

	}

	@Override
	protected void onResume() {
		Log.i("sys call", " baidu onResume");
		mMapView.onResume();
		super.onResume();
        setCenter(new LatLng(31.2404440000,121.6709200000));
		/*GET DATA
		Intent it = getIntent();
		Bundle data = it.getExtras();
		LatLng ll = new LatLng(data.getDouble("Lat"), data.getDouble("Lng"));
		currentPt = ll;
		
		updateMapState();
		setCenter(currentPt);
*/
	}

	@Override
	protected void onDestroy() {
		Log.i("sys call", " baidu onDestroy");
		mMapView.onDestroy();
		super.onDestroy();

        unbindService(conn);
        //need destroy
        mSearch.destroy();
	}

}

