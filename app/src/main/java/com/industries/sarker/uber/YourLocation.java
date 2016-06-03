package com.industries.sarker.uber;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class YourLocation extends FragmentActivity implements
        OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mCurrentLocation;
    private LocationRequest mLocationRequest;

    private TextView mStatusTextView;
    private Button mRequestUberButton;

    private boolean mRequestActive = false;

    private String mDriverUsername = "";
    ParseGeoPoint mDriverLocation = new ParseGeoPoint(0, 0);

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_your_location);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        createLocationRequest();

        mStatusTextView = (TextView) findViewById(R.id.statusTextView);


        mRequestUberButton = (Button) findViewById(R.id.requestUberButton);
        mRequestUberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRequestActive == false) {
                    ParseObject request = new ParseObject("Requests");
                    request.put("riderUsername", ParseUser.getCurrentUser().getObjectId());

                    ParseACL parseACL = new ParseACL();
                    parseACL.setPublicReadAccess(true);
                    parseACL.setPublicWriteAccess(true);
                    request.setACL(parseACL);

                    request.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                mStatusTextView.setText("Finding Uber driver...");
                                mRequestUberButton.setText("Cancel Uber");
                                mRequestActive = true;
                            }
                        }
                    });

                } else {
                    mStatusTextView.setText("Uber Cancelled");
                    mRequestUberButton.setText("Request Uber");
                    mRequestActive = false;

                    ParseQuery<ParseObject> query = new ParseQuery<>("Requests");

                    query.whereEqualTo("riderUsername", ParseUser.getCurrentUser().getObjectId());

                    query.findInBackground(new FindCallback<ParseObject>() {
                        @Override
                        public void done(List<ParseObject> objects, ParseException e) {
                            if (e == null) {
                                if (objects.size() > 0) {
                                    for (ParseObject object : objects) {
                                        object.deleteInBackground();
                                    }
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onPause() {
        stopLocationUpdates();
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
        super.onResume();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (mCurrentLocation == null) {
            LatLng latLng = new LatLng(37.0902, -95.7129);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 3));
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        startLocationUpdates();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            return;
        }
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setSmallestDisplacement(5);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);

            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;

        mMap.clear();

        updateMap();

        updateUserLocation(location);
    }

    private void updateMap() {
        if (mDriverUsername.equals("")) {
            LatLng currentLocation = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

            mMap.addMarker(new MarkerOptions().position(currentLocation).title("Your Location"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12));
        }
    }

    private void updateUserLocation(Location location) {
        if (mRequestActive == false) {

            ParseQuery<ParseObject> query2 = ParseQuery.getQuery("Requests");
            query2.whereEqualTo("riderUsername", ParseUser.getCurrentUser().getObjectId());
            query2.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        if (objects.size() > 0) {
                            for (ParseObject object : objects) {
                                if (object.get("driverUsername") != null) {
                                    mRequestActive = true;
                                    mDriverUsername = object.getString("driverUsername");
                                    mStatusTextView.setText("Your driver is on their way!");
                                    mRequestUberButton.setVisibility(View.INVISIBLE);
                                }
                            }
                        }
                    }
                }
            });

        }
        if (mRequestActive == true) {

            if (mDriverUsername.equals("")) {
                ParseQuery<ParseObject> query2 = ParseQuery.getQuery("Requests");
                query2.whereEqualTo("riderUsername", ParseUser.getCurrentUser().getObjectId());
                query2.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> objects, ParseException e) {
                        if (e == null) {
                            if (objects.size() > 0) {
                                for (ParseObject object : objects) {
                                    if (object.get("driverUsername") != null) {
                                        mDriverUsername = object.getString("driverUsername");
                                        mStatusTextView.setText("Your driver is on their way!");
                                        mRequestUberButton.setVisibility(View.INVISIBLE);
                                    }
                                }
                            }
                        }
                    }
                });
            }

            if (!mDriverUsername.equals("")) {
                ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
                userQuery.whereEqualTo("objectId", mDriverUsername);
                userQuery.findInBackground(new FindCallback<ParseUser>() {
                    @Override
                    public void done(List<ParseUser> objects, ParseException e) {
                        if (e == null) {
                            if (objects.size() > 0) {
                                for (ParseObject driver : objects) {
                                    mDriverLocation = driver.getParseGeoPoint("location");
                                }
                            }
                        }
                    }
                });

                if (mDriverLocation.getLatitude() != 0 && mDriverLocation.getLongitude() != 0) {
                    double distance = mDriverLocation.distanceInMilesTo(new ParseGeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
                    mStatusTextView.setText("You driver is " + String.format("%.1f", distance) + " miles away!");

                }

                ArrayList<Marker> markers = new ArrayList<>();
                markers.add(mMap.addMarker(new MarkerOptions().position(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude())).title("You")));
                markers.add(mMap.addMarker(new MarkerOptions().position(new LatLng(mDriverLocation.getLatitude(), mDriverLocation.getLongitude())).title("Uber Driver")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))));

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (Marker marker : markers) {
                    builder.include(marker.getPosition());
                }

                LatLngBounds bounds = builder.build();
                int padding = 250;
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                mMap.animateCamera(cameraUpdate);

            }

            final ParseGeoPoint userLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());

            ParseQuery<ParseObject> query = new ParseQuery<>("Requests");

            query.whereEqualTo("riderUsername", ParseUser.getCurrentUser().getObjectId());

            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        if (objects.size() > 0) {
                            for (ParseObject object : objects) {
                                object.put("riderLocation", userLocation);
                                object.saveInBackground();
                            }
                        }
                    }
                }
            });
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateUserLocation(mCurrentLocation);
            }
        }, 10000);
    }
}
