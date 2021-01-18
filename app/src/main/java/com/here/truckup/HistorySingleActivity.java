package com.here.truckup;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
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
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RoutingError;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


public class HistorySingleActivity extends Activity{
    private String rideId, currentUserId, customerId, driverId, userDriverOrCustomer;

    private TextView rideLocation;
    private TextView rideDistance;
    private TextView rideDate;
    private TextView userName;
    private TextView userPhone;

    private ImageView userImage;

    private RatingBar mRatingBar;

    private Button mPay;


    private DatabaseReference historyRideInfoDb;

    private GeoCoordinate destinationLatLng, pickupLatLng;
    private String distance;
    private Double ridePrice;
    private Boolean customerPaid = false;

    // permissions request code
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    // map embedded in the map fragment
    private Map map;

    // map fragment embedded in this activity
    private MapFragment mapFragment;

    // MapRoute for this activity
    private static MapRoute Maproute = null;

    private MapMarker DestinationMarker;
    private MapMarker pickupMarker;

    // permissions that need to be explicitly requested from end user.
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[]{
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);
        initializeMaps();

        Intent intent = new Intent(this, PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        startService(intent);

        rideId = getIntent().getExtras().getString("rideId");
        rideLocation = (TextView) findViewById(R.id.rideLocation);
        rideDistance = (TextView) findViewById(R.id.rideDistance);
        rideDate = (TextView) findViewById(R.id.rideDate);
        userName = (TextView) findViewById(R.id.userName);
        userPhone = (TextView) findViewById(R.id.userPhone);
        userImage = (ImageView) findViewById(R.id.userImage);
        mRatingBar = (RatingBar) findViewById(R.id.ratingBar);
        mPay = findViewById(R.id.pay);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        historyRideInfoDb = FirebaseDatabase.getInstance().getReference().child("history").child(rideId);
        getRideInformation();
    }

    private void initializeMaps() {

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
                    } else {
                        Toast.makeText(HistorySingleActivity.this, "onEngineInitializationCompleted: error: " + error + ", exiting", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
            });
        }
    }
    private void getRideInformation() {
        historyRideInfoDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for (DataSnapshot child:dataSnapshot.getChildren()){
                        if (child.getKey().equals("customer")){
                            customerId = child.getValue().toString();
                            if(!customerId.equals(currentUserId)){
                                userDriverOrCustomer = "Drivers";
                                getUserInformation("Customers", customerId);
                            }
                        }
                        if (child.getKey().equals("driver")){
                            driverId = child.getValue().toString();
                            if(!driverId.equals(currentUserId)){
                                userDriverOrCustomer = "Customers";
                                getUserInformation("Drivers", driverId);
                                displayCustomerRelatedObjects();
                            }
                        }
                        if (child.getKey().equals("timestamp")){
                            rideDate.setText(getDate(Long.valueOf(child.getValue().toString())));
                        }
                        if (child.getKey().equals("rating")){
                            mRatingBar.setRating(Integer.valueOf(child.getValue().toString()));

                        }
                        if (child.getKey().equals("customerPaid")){
                            customerPaid =true;
                        }
                        if (child.getKey().equals("distance")){
                            distance = child.getValue().toString();
                            rideDistance.setText(distance.substring(0, Math.min(distance.length(), 5)) + " km");
                            ridePrice = Double.valueOf(distance) * 0.5;

                        }
                        if (child.getKey().equals("destination")){
                            rideLocation.setText(child.getValue().toString());
                        }
                        if (child.getKey().equals("location")){
                            pickupLatLng = new GeoCoordinate(Double.valueOf(child.child("from").child("lat").getValue().toString()), Double.valueOf(child.child("from").child("lng").getValue().toString()));
                            destinationLatLng = new GeoCoordinate(Double.valueOf(child.child("to").child("lat").getValue().toString()), Double.valueOf(child.child("to").child("lng").getValue().toString()));
                            if(destinationLatLng != new GeoCoordinate(0,0)){
                                getRouteToMarker();
                            }
                        }
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void displayCustomerRelatedObjects() {
        mRatingBar.setVisibility(View.VISIBLE);
        mPay.setVisibility(View.VISIBLE);
        mRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                historyRideInfoDb.child("rating").setValue(rating);
                DatabaseReference mDriverRatingDb = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("rating");
                mDriverRatingDb.child(rideId).setValue(rating);
            }
        });

        if(customerPaid){
            mPay.setEnabled(false);
        }else{
            mPay.setEnabled(true);
        }
        mPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                payPalPayment();
            }
        });
    }
    private int PAYPAL_REQUEST_CODE = 1;
    private static PayPalConfiguration config = new PayPalConfiguration()
            .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
            .clientId(PayPalConfig.PAYPAL_CLIENT_ID);

    private void payPalPayment(){
        PayPalPayment payment = new PayPalPayment(new BigDecimal(ridePrice), "USD", "Truck Up",
                PayPalPayment.PAYMENT_INTENT_SALE);

        Intent intent = new Intent(HistorySingleActivity.this, PaymentActivity.class);

        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);

        startActivityForResult(intent, PAYPAL_REQUEST_CODE);

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PAYPAL_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){
               PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
                if(confirm != null){
                    try{
                        JSONObject jsonObj = new JSONObject(confirm.toJSONObject().toString());

                        String paymentResponse = jsonObj.getJSONObject("response").getString("state");

                        if(paymentResponse.equals("approved")){
                            Toast.makeText(getApplicationContext(), "Payment successful", Toast.LENGTH_LONG).show();
                            historyRideInfoDb.child("customerPaid").setValue(true);
                            mPay.setEnabled(false);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }else{
                Toast.makeText(getApplicationContext(), "Payment unsuccessful", Toast.LENGTH_LONG).show();
            }
        }
    }
    @Override
    protected void onDestroy() {
        stopService(new Intent(this, PayPalService.class));
        super.onDestroy();
    }

    private void getUserInformation(String otherUserDriverOrCustomer, String otherUserId) {
        DatabaseReference mOtherUserDB = FirebaseDatabase.getInstance().getReference().child("Users").child(otherUserDriverOrCustomer).child(otherUserId);
        mOtherUserDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    java.util.Map<String, Object> map = (java.util.Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name") != null){
                        userName.setText(map.get("name").toString());
                    }
                    if(map.get("phone") != null){
                        userPhone.setText(map.get("phone").toString());
                    }
                    if(map.get("profileImageUrl") != null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(userImage);
                    }
                }

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    private String getDate(Long time) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(time*1000);
        String date = DateFormat.format("MM-dd-yyyy hh:mm", cal).toString();
        return date;
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

                Image ridestartpoint = new Image();
                Image rideendpoint = new Image();
                try {
                    ridestartpoint.setImageResource(R.drawable.ic_ridestartpoint);        //need changes in image
                    rideendpoint.setImageResource(R.drawable.ic_rideendpoint);          //need changes in image
                    // this is the customers pickup location
                } catch (IOException e) {
                    e.printStackTrace();
                }
                pickupMarker = new MapMarker();
                pickupMarker.setIcon(ridestartpoint);
                pickupMarker.setCoordinate(pickupLatLng);
                map.addMapObject(pickupMarker);

                DestinationMarker= new MapMarker();
                DestinationMarker.setIcon(rideendpoint);
                DestinationMarker.setCoordinate(destinationLatLng);
                map.addMapObject(DestinationMarker);

                map.zoomTo(gbb, Map.Animation.BOW, Map.MOVE_PRESERVE_ORIENTATION);
            } else {
// Display a message indicating route calculation failure
                Toast.makeText(HistorySingleActivity.this, "Route calculation failed: %s" + routingError.toString(), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onProgress(int i) {

        }
    };

    private void getRouteToMarker() {
        erasePolylines();
// 2. Initialize RouteManager
        CoreRouter router = new CoreRouter();

        // 3. Select routing options
        RoutePlan routePlan = new RoutePlan();
        routePlan.addWaypoint(new RouteWaypoint(new GeoCoordinate(pickupLatLng.getLatitude(), pickupLatLng.getLongitude())));
        routePlan.addWaypoint(new RouteWaypoint(new GeoCoordinate(destinationLatLng.getLatitude(), destinationLatLng.getLongitude())));
        // Create the RouteOptions and set its transport mode & routing type
        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(RouteOptions.TransportMode.TRUCK);
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);
        routePlan.setRouteOptions(routeOptions);
        router.calculateRoute(routePlan, routeManagerListener);
    }
    private void erasePolylines()
    {
        if (map != null && Maproute != null) {
            map.removeMapObject(Maproute);
            Maproute = null;
        }
    }
}