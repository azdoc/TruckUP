package com.here.truckup;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
 import android.support.v7.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.LocationDataSourceHERE;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.mapping.MapState;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RoutingError;
import com.here.android.positioning.StatusListener;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class DriverMapActivity extends AppCompatActivity implements Map.OnTransformListener {

    private Button mSettings, mRideStatus;

    private Switch mWorkingSwitch;

    private float rideDistance;

    private Boolean isLoggingOut = false;
    // permissions request code
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    // map embedded in the map fragment
    private Map map;

    // map fragment embedded in this activity
    private MapFragment mapFragment;

    // positioning manager instance
    private PositioningManager mPositioningManager;

    // HERE location data source instance
    private LocationDataSourceHERE mHereLocation;

    // flag that indicates whether maps is being transformed
    private boolean mTransforming;

    // callback that is called when transforming ends
    private Runnable mPendingUpdate;

    private MapMarker m_map_marker;

    private String customerId = "", destination,DriverName;

    private LinearLayout mCustomerInfo;

    private ImageView mCustomerProfileImage;

    private TextView mCustomerName, mCustomerPhone, mCustomerDestination;

    private TextView mDriverName, mDriverPhone, mDriverCar;

    private List<RouteResult> m_mapObjectList = new ArrayList<>();

    private int status = 0;

    private GeoCoordinate pickupLatLng;

    // MapRoute for this activity
    private static MapRoute Maproute = null;

    private GeoCoordinate destinationLatLng;
    private MapMarker DestinationMarker;
    private MapMarker pickupMarker;
    DrawerLayout mDrawerLayout;
    ActionBarDrawerToggle actionBarDrawerToggle;

    // permissions that need to be explicitly requested from end user.
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mDrawerLayout =(DrawerLayout) findViewById(R.id.drawer_layout);
        actionBarDrawerToggle=new ActionBarDrawerToggle(this,mDrawerLayout,toolbar,R.string.open_drawer,R.string.close_drawer);
        mDrawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();


        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_view_drawer_24dp);

        final NavigationView navigationView =(NavigationView) findViewById(R.id.nav_view);

        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference navdrawervaluesref = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId);
        navdrawervaluesref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    java.util.Map<String, Object> map = (java.util.Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        DriverName = map.get("name").toString();
                        View v = navigationView.getHeaderView(0);
                        TextView Emailtxtview = (TextView ) v.findViewById(R.id.navEmailid);
                        Emailtxtview.setText(DriverName);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                       // menuItem.setChecked(true);
                        // close drawer when item is tapped
                        switch (menuItem.getItemId())
                        {
                            case R.id.Settings:
                                Intent intent = new Intent(DriverMapActivity.this, DriverSettingsActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                break;

                            case R.id.Logout:
                                isLoggingOut = true;
                                disconnectDriver();
                                FirebaseAuth.getInstance().signOut();
                                mPositioningManager.removeListener(positionListener);
                                Intent intent1 = new Intent(getApplicationContext(), MainActivity.class);
                                intent1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent1);
                                finish();
                                break;
                            case R.id.History:
                                Intent intent2 = new Intent(DriverMapActivity.this, HistoryActivity.class);
                                intent2.putExtra("customerOrDriver", "Drivers");
                                startActivity(intent2);
                                break;
                        }
                        // Add code here to update the UI based on the item selected
                        // For example, swap UI fragments here
                        mDrawerLayout =(DrawerLayout) findViewById(R.id.drawer_layout);
                        mDrawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    }
                });

        checkPermissions();

        mCustomerInfo = (LinearLayout) findViewById(R.id.customerInfo);
        mCustomerProfileImage = (ImageView) findViewById(R.id.customerProfileImage);
        mCustomerName = (TextView) findViewById(R.id.customerName);
        mCustomerPhone = (TextView) findViewById(R.id.customerPhone);
        mCustomerDestination = (TextView) findViewById(R.id.customerDestination);
        mRideStatus = (Button) findViewById(R.id.rideStatus);
        mWorkingSwitch = (Switch) findViewById(R.id.workingSwitch);
        mWorkingSwitch.setChecked(true);

        mWorkingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    connectDriver();
                }else{
                    disconnectDriver();
                }
            }
        });

        mRideStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (status) {
                    case 1:
                        status = 2;
                        erasePolylines();
                        if (destinationLatLng.getLatitude() != 0.0 && destinationLatLng.getLongitude() != 0.0) {
                            removePickupmarker();
                            getRouteToMarker(destinationLatLng);
                            DestinationMarker = new MapMarker();
                            DestinationMarker.setCoordinate(destinationLatLng);
                            map.addMapObject(DestinationMarker);
                        }
                        mRideStatus.setText("drive completed");
                        break;
                    case 2:
                        recordRide();                    //   addded by Arjun
                        if (DestinationMarker != null)
                        {
                            map.removeMapObject(DestinationMarker);
                            DestinationMarker = null;
                        }
                        endRide();
                        break;
                }
            }
        });

        getAssignedCustomer();
    }

    @Override
    public void onBackPressed() {
        if(mDrawerLayout.isDrawerOpen(GravityCompat.START))
        {
            mDrawerLayout.closeDrawer((GravityCompat.START));
        }
        this.moveTaskToBack(true);
    }


    private void getAssignedCustomer() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest").child("customerRideId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    status = 1;
                    customerId = dataSnapshot.getValue().toString();
                    getAssignedCustomerPickupLocation();
                    getAssignedCustomerDestination();
                    getAssignedCustomerInfo();
                } else {
                    if (map != null && Maproute != null) {
                        map.removeMapObject(Maproute);
                        Maproute = null;
                    }
                 endRide();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void getAssignedCustomerDestination()
    {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest");
        assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    java.util.Map<String, Object> map = (java.util.Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("destination")!=null){
                        destination = map.get("destination").toString();
                        mCustomerDestination.setText("Destination: " + destination);
                    }
                    else{
                        mCustomerDestination.setText("Destination: --");
                    }

                    Double destinationLat = 0.0;
                    Double destinationLng = 0.0;
                    if(map.get("destinationLat") != null){
                        destinationLat = Double.valueOf(map.get("destinationLat").toString());
                    }
                    if(map.get("destinationLng") != null){
                        destinationLng = Double.valueOf(map.get("destinationLng").toString());
                        destinationLatLng = new GeoCoordinate(destinationLat, destinationLng);
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void getAssignedCustomerInfo() {
        mCustomerInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    java.util.Map<String, Object> map = (java.util.Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name")  != null) {
                        mCustomerName.setText(map.get("name").toString());
                    }
                    if (map.get("phone") != null) {
                        mCustomerPhone.setText(map.get("phone").toString());
                    }
                    if (map.get("profileImageUrl") != null) {
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mCustomerProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(DriverMapActivity.this, "Happy driving", Toast.LENGTH_SHORT).show();
            }
        });
    }


    DatabaseReference assignedCustomerPickupLocationRef;
    ValueEventListener assignedCustomerPickupLocationRefListener;

    private void getAssignedCustomerPickupLocation() {
        assignedCustomerPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerId).child("l");
        assignedCustomerPickupLocationRefListener = assignedCustomerPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !customerId.equals("")) {
                    List<Object> mapvalues = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if (mapvalues.get(0) != null) {
                        locationLat = Double.parseDouble(mapvalues.get(0).toString());
                    }
                    if (mapvalues.get(1) != null) {
                        locationLng = Double.parseDouble(mapvalues.get(1).toString());
                    }
                     pickupLatLng = new GeoCoordinate(locationLat, locationLng);

                    removePickupmarker();
                    addPickupmarker();
                    getRouteToMarker(pickupLatLng);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void getRouteToMarker(GeoCoordinate pickupOrDestination) {
        GeoPosition geoPosition;
        GeoCoordinate coordinate;
        erasePolylines();
        if (mPositioningManager != null) {
            geoPosition = mPositioningManager.getPosition();
            coordinate = geoPosition.getCoordinate();
// 2. Initialize RouteManager
            CoreRouter router = new CoreRouter();

            // 3. Select routing options
            RoutePlan routePlan = new RoutePlan();
            routePlan.addWaypoint(new RouteWaypoint(new GeoCoordinate(coordinate.getLatitude(), coordinate.getLongitude())));
            routePlan.addWaypoint(new RouteWaypoint(new GeoCoordinate(pickupOrDestination)));
            // Create the RouteOptions and set its transport mode & routing type
            RouteOptions routeOptions = new RouteOptions();
            routeOptions.setTransportMode(RouteOptions.TransportMode.TRUCK);
            routeOptions.setRouteType(RouteOptions.Type.FASTEST);
            routePlan.setRouteOptions(routeOptions);
            router.calculateRoute(routePlan, routeManagerListener);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPositioningManager != null) {
            mPositioningManager.stop();
        }
    }

    public void connectDriver(){
        if (mPositioningManager != null) {
            mPositioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK);
            mPositioningManager.addListener(new WeakReference<PositioningManager.OnPositionChangedListener>(positionListener));
        }
    }

    private void disconnectDriver() {
        if (mPositioningManager != null) {
            mPositioningManager.removeListener(positionListener);
        }
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("DriversAvailable");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPositioningManager != null) {
            mPositioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK);
            mPositioningManager.addListener(new WeakReference<PositioningManager.OnPositionChangedListener>(positionListener));
        }
    }

    private PositioningManager.OnPositionChangedListener positionListener = new
            PositioningManager.OnPositionChangedListener() {
                @Override
                public void onPositionUpdated(final PositioningManager.LocationMethod locationMethod, final GeoPosition geoPosition, final boolean mapMatched) {
                    final GeoCoordinate coordinate = geoPosition.getCoordinate();

                    if (mTransforming) {
                        mPendingUpdate = new Runnable() {
                            @Override
                            public void run() {
                                onPositionUpdated(locationMethod, geoPosition, mapMatched);

                            }
                        };
                    } else {
                        if(!customerId.equals("")){
                            rideDistance += coordinate.distanceTo(coordinate)/1000;
                        }

                        map.setCenter(coordinate, Map.Animation.BOW);

                        if (map != null && m_map_marker != null) {
                            map.removeMapObject(m_map_marker);
                            m_map_marker = null;
                        }
                        Image img = new Image();
                        try {
                            img.setImageResource(R.mipmap.ic_pinlocation); // this is the current location marker of driver
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        m_map_marker = new MapMarker();
                        m_map_marker.setIcon(img);
                        m_map_marker.setCoordinate(coordinate);
                        map.addMapObject(m_map_marker);

                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("DriversAvailable");
                        DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("DriversWorking");
                        GeoFire geoFireAvailable = new GeoFire(refAvailable);
                        GeoFire geoFireWorking = new GeoFire(refWorking);

                        switch (customerId) {
                            case "":
                                geoFireWorking.removeLocation(userId);
                                geoFireAvailable.setLocation(userId, new GeoLocation(coordinate.getLatitude(), coordinate.getLongitude()));
                                break;

                            default:
                                geoFireAvailable.removeLocation(userId);
                                geoFireWorking.setLocation(userId, new GeoLocation(coordinate.getLatitude(), coordinate.getLongitude()));
                                break;
                        }
                    }


                }

                @Override
                public void onPositionFixChanged(PositioningManager.LocationMethod locationMethod, PositioningManager.LocationStatus locationStatus) {
                    // ignored
                }
            };

    @Override
    public void onMapTransformStart() {
        mTransforming = true;
    }

    @Override
    public void onMapTransformEnd(MapState mapState) {
        mTransforming = false;
        if (mPendingUpdate != null) {
            mPendingUpdate.run();
            mPendingUpdate = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Required permission '" + permissions[index] + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                initializeMapsAndPositioning();
                break;
        }
    }

    /**
     * Checks the dynamically controlled permissions and requests missing
     * permissions from end user.
     */
    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<>();
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (missingPermissions.isEmpty()) {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        } else {
            final String[] permissions = missingPermissions.toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        }
    }

    // Google has deprecated android.app.Fragment class. It is used in current SDK implementation.
    // Will be fixed in future SDK version.

/*    private MapFragment getMapFragment() {
        return (MapFragment) getFragmentManager().findFragmentById(R.id.mapfragment);
    }*/

    /**
     * Initializes HERE Maps and HERE Positioning. Called after permission check.
     */
    private void initializeMapsAndPositioning() {

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapfragment);
        mapFragment.setRetainInstance(false);

        // Set path of isolated disk cache
        String diskCacheRoot = Environment.getExternalStorageDirectory().getPath()
                + File.separator + ".isolated-here-maps";
        // Retrieve intent name from manifest
        String intentName = "";
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(this.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            intentName = bundle.getString("INTENT_NAME");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(this.getClass().toString(), "Failed to find intent name, NameNotFound: " + e.getMessage());
        }

        boolean success = com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(diskCacheRoot, intentName);
        if (!success) {
            // Setting the isolated disk cache was not successful, please check if the path is valid and
            // ensure that it does not match the default location
            // (getExternalStorageDirectory()/.here-maps).
            // Also, ensure the provided intent name does not match the default intent name.
        } else {
            mapFragment.init(new OnEngineInitListener() {
                @Override
                public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                    if (error == OnEngineInitListener.Error.NONE) {
                        map = mapFragment.getMap();
                        map.setCenter(new GeoCoordinate(61.497961, 23.763606, 0.0), Map.Animation.NONE);
                        map.setZoomLevel(map.getMaxZoomLevel() - 1);
                        map.addTransformListener(DriverMapActivity.this);
                        mPositioningManager = PositioningManager.getInstance();
                        mHereLocation = LocationDataSourceHERE.getInstance(
                                new StatusListener() {
                                    @Override
                                    public void onOfflineModeChanged(boolean offline) {
                                        // called when offline mode changes
                                    }

                                    @Override
                                    public void onAirplaneModeEnabled() {
                                        // called when airplane mode is enabled
                                    }

                                    @Override
                                    public void onWifiScansDisabled() {
                                        // called when Wi-Fi scans are disabled
                                    }

                                    @Override
                                    public void onBluetoothDisabled() {
                                        // called when Bluetooth is disabled
                                    }

                                    @Override
                                    public void onCellDisabled() {
                                        // called when Cell radios are switch off
                                    }

                                    @Override
                                    public void onGnssLocationDisabled() {
                                        // called when GPS positioning is disabled
                                    }

                                    @Override
                                    public void onNetworkLocationDisabled() {
                                        // called when network positioning is disabled
                                    }

                                    @Override
                                    public void onServiceError(ServiceError serviceError) {
                                        // called on HERE service error
                                    }

                                    @Override
                                    public void onPositioningError(PositioningError positioningError) {
                                        // called when positioning fails
                                    }
                                });
                        if (mHereLocation == null) {
                            Toast.makeText(DriverMapActivity.this, "LocationDataSourceHERE.getInstance(): failed, exiting", Toast.LENGTH_LONG).show();
                            finish();
                        }
                        mPositioningManager.setDataSource(mHereLocation);
                        mPositioningManager.addListener(new WeakReference<PositioningManager.OnPositionChangedListener>(positionListener));
                        // start position updates, accepting GPS, network or indoor positions
                        if (mPositioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK)) {
                            mapFragment.getPositionIndicator().setVisible(true);
                        } else {
                            Toast.makeText(DriverMapActivity.this, "PositioningManager.start: failed, exiting", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(DriverMapActivity.this, "onEngineInitializationCompleted: error: " + error + ", exiting", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
            });
        }
    }

    private CoreRouter.Listener routeManagerListener = new CoreRouter.Listener() {
        @Override
        public void onCalculateRouteFinished(List<RouteResult> list, RoutingError routingError) {
            if (routingError == RoutingError.NONE && list.get(0).getRoute() != null) {
// Render the route on the map
                Maproute = new MapRoute(list.get(0).getRoute());
                map.addMapObject(Maproute);
                // Get the bounding box containing the route and zoom in (no animation)
                GeoBoundingBox gbb = list.get(0).getRoute().getBoundingBox();
                map.zoomTo(gbb, Map.Animation.NONE, Map.MOVE_PRESERVE_ORIENTATION);
            } else {
// Display a message indicating route calculation failure
                Toast.makeText(DriverMapActivity.this, "Route calculation failed: %s" + routingError.toString(), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onProgress(int i) {

        }
    };

    private void endRide() {
        mRideStatus.setText("picked customer");
        erasePolylines();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("customerRequest");
        driverRef.removeValue();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(customerId);
        customerId = "";
        rideDistance = 0;

            removePickupmarker();
            if (assignedCustomerPickupLocationRefListener != null) {
                assignedCustomerPickupLocationRef.removeEventListener(assignedCustomerPickupLocationRefListener);
            }
            mCustomerInfo.setVisibility(View.GONE);
            mCustomerName.setText("");
            mCustomerPhone.setText("");
            mCustomerDestination.setText("Destination: --");
            mCustomerProfileImage.setImageResource(R.drawable.ic_default_user); //change default profile image

    }

    private void recordRide(){
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("history");
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("history");
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("history");
        String requestId = historyRef.push().getKey();
        driverRef.child(requestId).setValue(true);
        customerRef.child(requestId).setValue(true);

        HashMap map = new HashMap();
        map.put("driver", userId);
        map.put("customer", customerId);
        map.put("rating", 0);
        map.put("timestamp", getCurrentTimestamp());
        map.put("destination", destination);
        map.put("location/from/lat", pickupLatLng.getLatitude());
        map.put("location/from/lng", pickupLatLng.getLongitude());
        map.put("location/to/lat", destinationLatLng.getLatitude());
        map.put("location/to/lng", destinationLatLng.getLongitude());
        map.put("distance", rideDistance);
        historyRef.child(requestId).updateChildren(map);

    }

    private Long getCurrentTimestamp() {
        Long timestamp = System.currentTimeMillis()/1000;
        return timestamp;
    }

    private void removePickupmarker()
    {
        if (pickupMarker != null) {
            map.removeMapObject(pickupMarker);
            pickupMarker = null;
        }
    }
    private void addPickupmarker()
    {
        Image img = new Image();
        try {
            img.setImageResource(R.mipmap.ic_pinlocation); // this is the customers pickup location
        } catch (IOException e) {
            e.printStackTrace();
        }
        pickupMarker = new MapMarker();
        pickupMarker.setIcon(img);
        pickupMarker.setCoordinate(pickupLatLng);
        map.addMapObject(pickupMarker);//set the title to this marker pickup location
    }
    private void erasePolylines()
    {
        if (map != null && Maproute != null) {
            map.removeMapObject(Maproute);
            Maproute = null;
        }
    }
}
