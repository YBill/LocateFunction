package com.bill.locatefunction;

import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.animation.Animation;
import com.amap.api.maps.model.animation.TranslateAnimation;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.core.SuggestionCity;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;

import java.util.ArrayList;
import java.util.List;

public class LocationActivity extends AppCompatActivity implements AMapLocationListener, AMap.OnMapLoadedListener,
        AMap.OnCameraChangeListener, GeocodeSearch.OnGeocodeSearchListener, LocationSource, PoiSearch.OnPoiSearchListener {

    private MapView mapView;
    private AMap aMap;
    private UiSettings settings;
    private AMapLocationClient locationClient;
    private AMapLocationClientOption locationOption;
    private Marker centerMarker;
    private GeocodeSearch geocodeSearch;
    private PoiSearch.Query query;

    private POIEntity poiEntity;
    private List<POIEntity> poiEntityList = new ArrayList<>();
    private RecyclerView recyclerView;
    private MoreLocateAdapter adapter;
    private int currentPage = 0;

    private boolean isClickItem = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        poiEntity = new POIEntity();
        initList();
        mapView = (MapView) findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);

        initLocation();
        initMaps();
    }

    private void initList() {
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setMotionEventSplittingEnabled(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MoreLocateAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        adapter.setOnItemClickLitener(new MoreLocateAdapter.OnItemClickLitener() {
            @Override
            public void onItemClick(View view, int position) {
                isClickItem = true;
                moveMap(poiEntityList.get(position).longitude, poiEntityList.get(position).latitude);
                int tag = 0;
                for (int i = 0; i < poiEntityList.size(); i++) {
                    if (poiEntityList.get(i).isSelect) {
                        tag = i;
                        break;
                    }
                }
                poiEntityList.get(tag).isSelect = false;
                poiEntityList.get(position).isSelect = true;
                adapter.setLocateList(poiEntityList);

            }
        });
    }

    /**
     * 初始化地图
     */
    private void initMaps() {
        aMap = mapView.getMap();
        settings = aMap.getUiSettings();
        settings.setZoomControlsEnabled(true); // 缩放按钮
        settings.setCompassEnabled(true); // 指针
        settings.setScaleControlsEnabled(true); // 比例尺寸
        settings.setMyLocationButtonEnabled(true); // 定位按钮

        aMap.setOnMapLoadedListener(this);
        aMap.setOnCameraChangeListener(this);
        aMap.setLocationSource(this);

        // 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.setMyLocationEnabled(true);
        // 设置定位的类型为定位模式 ，可以由定位、跟随或地图根据面向方向旋转几种
        aMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);

        geocodeSearch = new GeocodeSearch(this);
        geocodeSearch.setOnGeocodeSearchListener(this);
    }

    /**
     * 初始化定位功能
     */
    private void initLocation() {
        locationClient = new AMapLocationClient(this);
        locationOption = new AMapLocationClientOption();
        // 设置定位监听
        locationClient.setLocationListener(this);
        // 设置定位模式为低功耗模式
        locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Battery_Saving);
        //设置为单次定位
        locationOption.setOnceLocation(true);
        // 设置是否需要显示地址信息
        locationOption.setNeedAddress(true);
        // 设置是否开启缓存
//        locationOption.setLocationCacheEnable(true);
        //设置是否强制刷新WIFI，默认为强制刷新
//        locationOption.setOnceLocationLatest(true);
        //设置是否等待设备wifi刷新，如果设置为true,会自动变为单次定位，持续定位时不要使用
//        locationOption.setWifiActiveScan(true);
        // 设置发送定位请求的时间间隔,最小值为1000，如果小于1000，按照1000算.只有持续定位设置定位间隔才有效，单次定位无效
//        locationOption.setInterval(2000);
        // 设置定位参数
        locationClient.setLocationOption(locationOption);
        // 开始定位
//        locationClient.startLocation();
    }

    /**
     * 根据经纬度搜索
     *
     * @param longitude
     * @param latitude
     */
    public void searchByLocation(double longitude, double latitude) {
        // 第一个参数表示搜索字符串
        // 第二个参数表示POI 类型的组合，比如定义如下组合：餐馆|电影院|景点
        // 第三个参数表示poi搜索区域（空字符串代表全国)
        query = new PoiSearch.Query("", "", "");
        // 设置查第一页
        query.setPageNum(currentPage);
        // 设置每页最多返回多少条poiitem
        query.setPageSize(19);
        Log.d("Bill", latitude + ":" + longitude);
        LatLonPoint latLonPoint = new LatLonPoint(latitude, longitude);
        PoiSearch poiSearch = new PoiSearch(this, query);
        // 设置搜索区域为以latLonPoint点为圆心，其周围5000米范围
        poiSearch.setBound(new PoiSearch.SearchBound(latLonPoint, 5000, true));
        // 异步搜索
        poiSearch.searchPOIAsyn();
        poiSearch.setOnPoiSearchListener(this);
    }

    /**
     * @param longitude 经度
     * @param latitude  纬度
     */
    private void moveMap(double longitude, double latitude) {
        LatLng latLng = new LatLng(latitude, longitude);
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f));
    }

    /**
     * 屏幕中心marker 跳动
     */
    public void startJumpAnimation() {
        if (centerMarker != null) {
            //根据屏幕距离计算需要移动的目标点
            final LatLng latLng = centerMarker.getPosition();
            Point point = aMap.getProjection().toScreenLocation(latLng);
            point.y -= dip2px(125);
            LatLng target = aMap.getProjection().fromScreenLocation(point);
            //使用TranslateAnimation,填写一个需要移动的目标点
            Animation animation = new TranslateAnimation(target);
            animation.setInterpolator(new Interpolator() {
                @Override
                public float getInterpolation(float input) {
                    // 模拟重加速度的interpolator
                    if (input <= 0.5) {
                        return (float) (0.5f - 2 * (0.5 - input) * (0.5 - input));
                    } else {
                        return (float) (0.5f - Math.sqrt((input - 0.5f) * (1.5f - input)));
                    }
                }
            });
            //整个移动所需要的时间
            animation.setDuration(600);
            //设置动画
            centerMarker.setAnimation(animation);
            //开始动画
            centerMarker.startAnimation();

        } else {
            Log.d("Bill", "screenMarker is null");
        }
    }

    /**
     * 在屏幕中心添加一个Marker
     */
    private void addMarkerInScreenCenter() {
        LatLng latLng = aMap.getCameraPosition().target;
        Point screenPosition = aMap.getProjection().toScreenLocation(latLng);
        MarkerOptions markerOptions = new MarkerOptions()
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.purple_pin));
        centerMarker = aMap.addMarker(markerOptions);
        //设置Marker在屏幕上,不跟随地图移动
        centerMarker.setPositionByPixels(screenPosition.x, screenPosition.y);
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public int dip2px(float dpValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private Toast toast;

    private void toast(String msg) {
        if (toast == null)
            toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        else
            toast.setText(msg);
        toast.show();
    }

    /**
     * 定位结果回调
     *
     * @param aMapLocation
     */
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        Log.d("Bill", aMapLocation.getLongitude() + ":" + aMapLocation.getLatitude());
        Log.e("Bill", LocationUtils.getLocationStr(aMapLocation));
        locationClient.stopLocation();
        moveMap(aMapLocation.getLongitude(), aMapLocation.getLatitude());
    }

    /**
     * map加载完成
     */
    @Override
    public void onMapLoaded() {
        addMarkerInScreenCenter();
    }

    /**
     * map移动回调
     *
     * @param cameraPosition
     */
    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        // 拖动中实时回调
    }

    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        // 拖动结束
        if (isClickItem) {
            isClickItem = false;
        } else {
            startJumpAnimation();
            LatLng latLng = aMap.getCameraPosition().target;
            poiEntity.longitude = latLng.longitude;
            poiEntity.latitude = latLng.latitude;
            LatLonPoint point = new LatLonPoint(latLng.latitude, latLng.longitude);
            RegeocodeQuery query = new RegeocodeQuery(point, 200, GeocodeSearch.AMAP);
            geocodeSearch.getFromLocationAsyn(query);
            Log.e("Bill", latLng.longitude + "=----==" + latLng.latitude);
        }
    }

    /**
     *
     */
    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int rCode) {
        // 逆编码
        if (rCode == 1000) {
            if (regeocodeResult != null) {
                RegeocodeAddress regeocodeAddress = regeocodeResult.getRegeocodeAddress();
                poiEntity.title = regeocodeAddress.getFormatAddress();
                poiEntity.isSelect = true;
                searchByLocation(regeocodeResult.getRegeocodeQuery().getPoint().getLongitude(), regeocodeResult.getRegeocodeQuery().getPoint().getLatitude());

                // 下面的pois固定条数，没发分页
                /*poiEntityList.clear();
                POIEntity entity1 = new POIEntity();
                entity1.isSelect = true;
                entity1.content = regeocodeAddress.getFormatAddress();
                entity1.longitude = regeocodeResult.getRegeocodeQuery().getPoint().getLongitude();
                entity1.latitude = regeocodeResult.getRegeocodeQuery().getPoint().getLatitude();
                poiEntityList.add(entity1);
                Log.e("Bill", regeocodeResult.getRegeocodeQuery().getPoint().getLongitude() + "===" + regeocodeResult.getRegeocodeQuery().getPoint().getLatitude());

                List<PoiItem> poiItems = regeocodeAddress.getPois();
                Log.e("Bill", "size:" + poiItems.size());
                if (poiItems != null && poiItems.size() > 0) {
                    for (int i = 0; i < poiItems.size(); i++) {
                        POIEntity entity = new POIEntity();
                        entity.title = poiItems.get(i).getTitle();
                        entity.content = poiItems.get(i).getSnippet();
                        entity.longitude = poiItems.get(i).getLatLonPoint().getLongitude();
                        entity.latitude = poiItems.get(i).getLatLonPoint().getLatitude();
                        poiEntityList.add(entity);
                    }
                }
                adapter.setLocateList(poiEntityList);*/
            }
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {
        // 编码
    }

    /**
     * 我的位置回调
     */
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        locationClient.startLocation();
    }

    @Override
    public void deactivate() {
        locationClient.stopLocation();
    }

    @Override
    public void onPoiSearched(PoiResult poiResult, int rCode) {
        if (rCode == 1000) {
            if (poiResult != null && poiResult.getQuery() != null) {// 搜索poi的结果
                if (poiResult.getQuery().equals(query)) {// 是否是同一条
                    // 取得第一页的poiitem数据，页数从数字0开始
                    List<PoiItem> poiItems = poiResult.getPois();
                    // 当搜索不到poiitem数据时，会返回含有搜索关键字的城市信息
                    List<SuggestionCity> suggestionCities = poiResult.getSearchSuggestionCitys();

                    poiEntityList.clear();
                    poiEntityList.add(poiEntity);

                    if (poiItems != null && poiItems.size() > 0) {
                        Log.e("Bill", "poiItems size:" + poiItems.size());
                        if (poiItems != null && poiItems.size() > 0) {
                            for (int i = 0; i < poiItems.size(); i++) {
                                POIEntity entity = new POIEntity();
                                entity.title = poiItems.get(i).getTitle();
                                entity.content = poiItems.get(i).getSnippet();
                                entity.longitude = poiItems.get(i).getLatLonPoint().getLongitude();
                                entity.latitude = poiItems.get(i).getLatLonPoint().getLatitude();
                                poiEntityList.add(entity);
                            }
                        }
                    } else if (suggestionCities != null && suggestionCities.size() > 0) {
                        Log.e("Bill", "suggestionCities size:" + suggestionCities.size());
                    } else {
                        Log.e("Bill", "对不起，没有搜索到相关数据！");
                    }
                    adapter.setLocateList(poiEntityList);
                }
            } else {
                Log.e("Bill", "对不起，没有搜索到相关数据！");
            }
        } else {
            Log.e("Bill", "rCode:" + rCode);
        }
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (null != locationClient) {
            locationClient.onDestroy();
            locationClient = null;
            locationOption = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

}
