package com.here.truckup;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
import com.here.android.mpa.mapping.MapState;
import com.here.android.mpa.search.DiscoveryResult;
import com.here.android.mpa.search.DiscoveryResultPage;
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.ResultListener;
import com.here.android.mpa.search.SearchRequest;
import com.here.android.positioning.StatusListener;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class CustomerMapActivity extends AppCompatActivity implements Map.OnTransformListener{

    private Boolean requestBol=false;
    private Button mRequest,m_placeDetailButton;;

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

    private MapMarker pickupMarker;

    private LinearLayout mDriverInfo;

    private ImageView mDriverProfileImage;

    private TextView mDriverName, mDriverPhone, mDriverCar;

    private RadioGroup mRadioGroup;
    private int ToggleCardView =0;
    private CardView TruckDetailsCardView;
    private RatingBar mRatingBar;

    private String destination, requestService,CustomerName;
    DrawerLayout mDrawerLayout;
    ActionBarDrawerToggle actionBarDrawerToggle;
    double destinationlat;
    double destinationlng;

    EditText destinationEdittext;
    public static List<DiscoveryResult> s_ResultList;

    private GeoCoordinate destinationLatLng;

    // permissions that need to be explicitly requested from end user.
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[]{
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        checkPermissions();
        mDriverInfo = (LinearLayout) findViewById(R.id.driverInfo);
        mDriverProfileImage = (ImageView) findViewById(R.id.driverProfileImage);
        mDriverName = (TextView) findViewById(R.id.driverName);
        mDriverPhone = (TextView) findViewById(R.id.driverPhone);
        mDriverCar = (TextView) findViewById(R.id.driverCar);
        mRatingBar = (RatingBar) findViewById(R.id.ratingBar);
        mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        mRadioGroup.check(R.id.TataAce);
        mRequest = (Button) findViewById(R.id.request);
        TruckDetailsCardView=(CardView)findViewById(R.id.card_view);
        TruckDetailsCardView.setVisibility(View.INVISIBLE);
        Toolbar toolbar = findViewById(R.id.toolbar);
        destinationEdittext=(EditText)findViewById(R.id.destinationsearch);
        setSupportActionBar(toolbar);

        Intent intent=getIntent();
        destinationlat=intent.getDoubleExtra(ResultListActivity.EXTRA_LATITUDE,0.0);
        destinationlng=intent.getDoubleExtra(ResultListActivity.EXTRA_LONGITUDE,0.0);

        destination =intent.getStringExtra(ResultListActivity.EXTRA_DESTINATIONPLACE);
        destinationEdittext.setText(destination);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.open_drawer, R.string.close_drawer);
        mDrawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();


        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_view_drawer_24dp);

        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        String customerid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference navdrawervaluesref = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerid);
        navdrawervaluesref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    java.util.Map<String, Object> map = (java.util.Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        CustomerName = map.get("name").toString();
                        View v = navigationView.getHeaderView(0);
                        TextView Emailtxtview = (TextView ) v.findViewById(R.id.navEmailid);
                        Emailtxtview.setText(CustomerName);
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
                        switch (menuItem.getItemId()) {
                            case R.id.Settings:
                                Intent intent = new Intent(CustomerMapActivity.this, CustomerSettingsActivity.class);
                                startActivity(intent);
                                break;

                            case R.id.Logout:
                                FirebaseAuth.getInstance().signOut();
                                mPositioningManager.removeListener(positionListener);
                                Intent intent1 = new Intent(getApplicationContext(), MainActivity.class);
                                intent1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent1);
                                finish();
                                break;
                            case R.id.History:
                                Intent intent2 = new Intent(CustomerMapActivity.this, HistoryActivity.class);
                                intent2.putExtra("customerOrDriver", "Customers");
                                startActivity(intent2);
                                break;
                        }

                        // Add code here to update the UI based on the item selected
                        // For example, swap UI fragments here
                        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
                        mDrawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    }
                });

        //Takes the current position of the user and creates a CustomerRequest under which contains current users id against its LAT,LNG
        mRequest = (Button) findViewById(R.id.request);

        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (destination != "" && destinationLatLng != null && !destinationEdittext.getText().toString().isEmpty()) {
                    try {
                        if (mPositioningManager != null) {
                            final GeoCoordinate pickupLocation = mPositioningManager.getPosition().getCoordinate();
                            if (requestBol) {
                                endRide();
                            } else {
                                int selectId = mRadioGroup.getCheckedRadioButtonId();

                                final RadioButton radioButton = (RadioButton) findViewById(selectId);

                                if (radioButton.getText() == null) {
                                    return;
                                }

                                requestService = radioButton.getText().toString();

                                requestBol = true;
                                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
                                GeoFire geoFire = new GeoFire(ref);
                                geoFire.setLocation(userId, new GeoLocation(pickupLocation.getLatitude(), pickupLocation.getLongitude()));

                                removePickupmarker();
                                addPickupmarker(pickupLocation);
                                mRequest.setText("Getting your Truck...");
                                getClosestDriver(pickupLocation);
                            }
                        } else {
                            Toast.makeText(CustomerMapActivity.this, "Please wait for location fix", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(CustomerMapActivity.this, "Please wait for location fix", Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    Toast.makeText(CustomerMapActivity.this, "Please Select a destination", Toast.LENGTH_SHORT).show();
                }
            }
        });
// This will get the radiobutton in the radiogroup that is checked

        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton checkedRadioButton = (RadioButton) mRadioGroup.findViewById(mRadioGroup.getCheckedRadioButtonId());
                RadioButton TataAce = (RadioButton) findViewById(R.id.TataAce);
                RadioButton TataOpen = (RadioButton) findViewById(R.id.TataOpen);
                RadioButton Tata407 = (RadioButton) findViewById(R.id.Tata407);
                // This will get the radiobutton that has changed in its check state
                // This puts the value (true/false) into the variable

                mRadioGroup.findViewById(mRadioGroup.getCheckedRadioButtonId()).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (ToggleCardView == 0) {
                            TruckDetailsCardView.setVisibility(View.VISIBLE);
                            ToggleCardView=1;
                        }else if (ToggleCardView == 1) {
                            TruckDetailsCardView.setVisibility(View.INVISIBLE);
                            ToggleCardView=0;
                        }
                    }
                });

                boolean isChecked = checkedRadioButton.isChecked();
                // If the radiobutton that has changed in check state is now checked...
                if (TataAce.isChecked() == isChecked) {
                    TextView PayLoadTxtInCardview = (TextView) TruckDetailsCardView.findViewById(R.id.payload);
                    TextView DimensionTxt = (TextView) TruckDetailsCardView.findViewById(R.id.dimension);
                    PayLoadTxtInCardview.setText("750 kg\nPayload");
                    DimensionTxt.setText("7ft x 4.5ft x 5.5ft\nLxBxH");
                } else if (TataOpen.isChecked() == isChecked) {
                    TextView PayLoadTxtInCardview = (TextView) TruckDetailsCardView.findViewById(R.id.payload);
                    TextView DimensionTxt = (TextView) TruckDetailsCardView.findViewById(R.id.dimension);
                    PayLoadTxtInCardview.setText("1000 kg\nPayload");
                    DimensionTxt.setText("8ft x 4.5ft x 5.5ft\nLxBxH");
                } else if (Tata407.isChecked() == isChecked) {
                    TextView PayLoadTxtInCardview = (TextView) TruckDetailsCardView.findViewById(R.id.payload);
                    TextView DimensionTxt = (TextView) TruckDetailsCardView.findViewById(R.id.dimension);
                    PayLoadTxtInCardview.setText("2500 kg\nPayload");
                    DimensionTxt.setText("9ft x 5.5ft x 6ft\nLxBxH");
                }

            }
        });
        mRadioGroup.findViewById(mRadioGroup.getCheckedRadioButtonId()).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ToggleCardView == 0) {
                    TruckDetailsCardView.setVisibility(View.VISIBLE);
                    ToggleCardView=1;
                } else if (ToggleCardView == 1) {
                    TruckDetailsCardView.setVisibility(View.INVISIBLE);
                    ToggleCardView=0;
                }

            }
        });
        initResultListButton();
        initSearchControlButtons();
    }
    private void initResultListButton() {
        m_placeDetailButton = (Button) findViewById(R.id.resultListBtn);
        m_placeDetailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Open the ResultListActivity */
                Intent intent = new Intent(CustomerMapActivity.this, ResultListActivity.class);
                startActivity(intent);
            }
        });
    }
    private void initSearchControlButtons() {
        Button searchRequestButton = (Button) findViewById(R.id.searchRequestBtn);
        searchRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Trigger a SearchRequest based on the current map center and search query
                 * "Hotel".Please refer to HERE Android SDK API doc for other supported location
                 * parameters and categories.
                 */
                if (!destinationEdittext.getText().toString().isEmpty()) {
                    m_placeDetailButton.setVisibility(View.GONE);
                    SearchRequest searchRequest = new SearchRequest(destinationEdittext.getText().toString());
                    searchRequest.setSearchCenter(map.getCenter());
                    searchRequest.execute(discoveryResultPageListener);
                }
                else {
                    Toast.makeText(CustomerMapActivity.this, "Please enter your destination", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private ResultListener<DiscoveryResultPage> discoveryResultPageListener = new ResultListener<DiscoveryResultPage>() {
        @Override
        public void onCompleted(DiscoveryResultPage discoveryResultPage, ErrorCode errorCode) {
            if (errorCode == ErrorCode.NONE) {

                m_placeDetailButton.setVisibility(View.VISIBLE);

                s_ResultList = discoveryResultPage.getItems();

            } else {
                Toast.makeText(CustomerMapActivity.this,
                        "ERROR:Discovery search request returned return error code+ " + errorCode,
                        Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void getDriverInfo(){
        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    java.util.Map<String, Object> map = (java.util.Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        mDriverName.setText(map.get("name").toString());
                    }
                    if(map.get("phone")!=null){
                        mDriverPhone.setText(map.get("phone").toString());
                    }
                    if(map.get("car")!=null) {
                        mDriverCar.setText(map.get("car").toString());
                    }
                    if(map.get("profileImageUrl")!=null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mDriverProfileImage);
                    }
                    int ratingSum = 0;
                    float ratingsTotal = 0;
                    float ratingsAvg = 0;
                    for (DataSnapshot child : dataSnapshot.child("rating").getChildren()){
                        ratingSum = ratingSum + Integer.valueOf(child.getValue().toString());
                        ratingsTotal++;
                    }
                    if(ratingsTotal!= 0){
                        ratingsAvg = ratingSum/ratingsTotal;
                        mRatingBar.setRating(ratingsAvg);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private DatabaseReference driveHasEndedRef;
    private ValueEventListener driveHasEndedRefListener;
    private void getHasRideEnded(){
        driveHasEndedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("customerRequest").child("customerRideId");
        driveHasEndedRefListener = driveHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                }else{
                    endRide();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void endRide() {
        requestBol = false;
        geoQuery.removeAllListeners();
        if (driverLocationRef != null) {
            driverLocationRef.removeEventListener(driverLocationRefListener);
        }
        if (driveHasEndedRef != null)
        {
            driveHasEndedRef.removeEventListener(driveHasEndedRefListener);
        }

        if (driverFoundID != null){
            DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("customerRequest");
            driverRef.removeValue();
            driverFoundID = null;
        }
        driverFound = false;
        radius = 1;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);

        removePickupmarker();
        removeDriversLocationMarker();
        mRequest.setText("call Truck");
        mDriverInfo.setVisibility(View.GONE);
        mDriverName.setText("");
        mDriverPhone.setText("");
        mDriverCar.setText("");
        mDriverProfileImage.setImageResource(R.drawable.ic_default_user);
    }

    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundID;
    GeoQuery geoQuery;

    private void getClosestDriver(final GeoCoordinate pickupLocation)
    {

        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("DriversAvailable");
        GeoFire geoFire = new GeoFire(driverLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.getLatitude(), pickupLocation.getLongitude()), radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound && requestBol){
                    DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);
                    mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                                java.util.Map<String, Object> driverMap = (java.util.Map<String, Object>) dataSnapshot.getValue();
                                if (driverFound){
                                    return;
                                }

                                if(driverMap.get("service").equals(requestService)){
                                    driverFound = true;
                                    driverFoundID = dataSnapshot.getKey();

                                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("customerRequest");
                                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    HashMap map = new HashMap();
                                    map.put("customerRideId", customerId);
                                    map.put("destination", destination);
                                    map.put("destinationLat", destinationLatLng.getLatitude());
                                    map.put("destinationLng", destinationLatLng.getLongitude());
                                    driverRef.updateChildren(map);

                                    getDriversLocation(pickupLocation);
                                    getDriverInfo();
                                    getHasRideEnded();
                                    mRequest.setText("Looking for Driver Location....");
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound) {
                        radius++;
                    getClosestDriver(pickupLocation);
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }
    private MapMarker mDriverMarker;
    private ValueEventListener driverLocationRefListener;
    private DatabaseReference driverLocationRef;

    private void getDriversLocation(final GeoCoordinate pickupLocation)
    {
        driverLocationRef= FirebaseDatabase.getInstance().getReference().child("DriversWorking").child(driverFoundID).child("l");
        driverLocationRefListener=driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestBol) {
                    List<Object> mapvalues = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if (mapvalues.get(0) != null) {
                        locationLat = Double.parseDouble(mapvalues.get(0).toString());
                    }
                    if (mapvalues.get(1) != null) {
                        locationLng = Double.parseDouble(mapvalues.get(1).toString());
                    }
                    GeoCoordinate driverLatLng=new GeoCoordinate(locationLat,locationLng);
                    Location loc1 = new Location("");
                    loc1.setLatitude(pickupLocation.getLatitude());
                    loc1.setLongitude(pickupLocation.getLongitude());

                    Location loc2 = new Location("");
                    loc2.setLatitude(driverLatLng.getLatitude());
                    loc2.setLongitude(driverLatLng.getLongitude());

                    float distance = loc1.distanceTo(loc2);

                    if (distance<150)//I Have increased it to 150
                    {
                        mRequest.setText("Cancel Ride");
                    }else{
                        mRequest.setText("Driver Found: " + String.valueOf(distance));
                    }
                    removeDriversLocationMarker();
                    Image img = new Image();
                    try {
                        img.setImageResource(R.mipmap.ic_drivers_location); // Marker for the drivers location
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mDriverMarker = new MapMarker();
                    mDriverMarker.setIcon(img);
                    mDriverMarker.setCoordinate(driverLatLng);
                    map.addMapObject(mDriverMarker);//set the title to this marker pickup here
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPositioningManager != null) {
            mPositioningManager.stop();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPositioningManager != null) {
            mPositioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK);
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
                        map.setCenter(coordinate, Map.Animation.BOW);
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
    @SuppressWarnings("deprecation")
    private MapFragment getMapFragment() {
        return (MapFragment) getFragmentManager().findFragmentById(R.id.mapfragment);
    }

    /**
     * Initializes HERE Maps and HERE Positioning. Called after permission check.
     */
    private void initializeMapsAndPositioning() {

        mapFragment = getMapFragment();
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
                        map.setCenter(new GeoCoordinate(19.1503, 72.8530, 0.0), Map.Animation.LINEAR);
                        map.setZoomLevel(map.getMaxZoomLevel() - 1);
                        destinationLatLng=new GeoCoordinate(destinationlat,destinationlng);
                        map.addTransformListener(CustomerMapActivity.this);
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
                            Toast.makeText(CustomerMapActivity.this, "LocationDataSourceHERE.getInstance(): failed, exiting", Toast.LENGTH_LONG).show();
                            finish();
                        }
                        mPositioningManager.setDataSource(mHereLocation);
                        mPositioningManager.addListener(new WeakReference<PositioningManager.OnPositionChangedListener>(positionListener));
                        // start position updates, accepting GPS, network or indoor positions
                        if (mPositioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK)) {
                            mapFragment.getPositionIndicator().setVisible(true);
                        } else {
                            Toast.makeText(CustomerMapActivity.this, "PositioningManager.start: failed, exiting", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(CustomerMapActivity.this, "onEngineInitializationCompleted: error: " + error + ", exiting", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
            });
        }
    }
    private void removePickupmarker()
    {
        if (pickupMarker != null) {
            map.removeMapObject(pickupMarker);
            pickupMarker = null;
        }
    }
    private void removeDriversLocationMarker() {
        if(map != null && mDriverMarker!= null) {
            map.removeMapObject(mDriverMarker);
            mDriverMarker = null;
        }
    }
    private void addPickupmarker(GeoCoordinate coordinate)
    {
        Image img = new Image();
        try {
            img.setImageResource(R.mipmap.ic_pinlocation);
        } catch (IOException e) {
            e.printStackTrace();
        }
        pickupMarker = new MapMarker();
        pickupMarker.setIcon(img);
        pickupMarker.setCoordinate(coordinate);
        map.addMapObject(pickupMarker);//set the title to this marker pickup location
    }
    @Override
    public void onBackPressed() {
        if(mDrawerLayout.isDrawerOpen(GravityCompat.START))
        {
            mDrawerLayout.closeDrawer((GravityCompat.START));
        }
        this.moveTaskToBack(true);
    }
}
