package com.industries.sarker.uber;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class ViewRequests extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;


    private ListView mRequestsListView;
    private ArrayList<String> mArrayList;
    private ArrayAdapter mArrayAdapter;
    private ArrayList<String> mUsernames;
    private ArrayList<Double> mLatitudes;
    private ArrayList<Double> mLongitudes;

    private Location mCurrentLocation;

    private ProgressDialog dlg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_requests);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        createLocationRequest();

        mRequestsListView = (ListView) findViewById(R.id.requestsListView);
        mArrayList = new ArrayList<>();

        mUsernames = new ArrayList<>();
        mLatitudes = new ArrayList<>();
        mLongitudes = new ArrayList<>();

        mArrayList.add("Finding Nearby Requests...");
        mArrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, mArrayList);

        mRequestsListView.setAdapter(mArrayAdapter);

        mRequestsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(ViewRequests.this, ViewRiderLocation.class);
                intent.putExtra("RIDER_USERNAME", mUsernames.get(position));
                intent.putExtra("RIDER_LATITUDE", mLatitudes.get(position));
                intent.putExtra("RIDER_LONGITUDE", mLongitudes.get(position));
                intent.putExtra("DRIVER_LATITUDE", mCurrentLocation.getLatitude());
                intent.putExtra("DRIVER_LONGITUDE", mCurrentLocation.getLongitude());

                startActivity(intent);
            }
        });

        dlg = new ProgressDialog(ViewRequests.this);

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
        dlg.setMessage("Finding Nearby Requests. Please wait.");
        dlg.show();
        super.onResume();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 0);

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
        updateLocation();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdates();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 0);

            return;
        }

        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setSmallestDisplacement(5);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void updateLocation() {
        final ParseGeoPoint userLocation = new ParseGeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

        ParseUser.getCurrentUser().put("location", userLocation);
        ParseUser.getCurrentUser().saveInBackground();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Requests");
        query.whereDoesNotExist("driverUsername");
        query.whereNear("riderLocation", userLocation);
        query.setLimit(100);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    if (objects.size() > 0) {
                        mArrayList.clear();
                        mUsernames.clear();
                        mLatitudes.clear();
                        mLongitudes.clear();

                        for (ParseObject object : objects) {
                            double distance = (userLocation.distanceInMilesTo((ParseGeoPoint) object.get("riderLocation")));
                            mArrayList.add(String.format("%.1f", distance) + " miles");

                            mUsernames.add(object.getString("riderUsername"));
                            mLatitudes.add(object.getParseGeoPoint("riderLocation").getLatitude());
                            mLongitudes.add(object.getParseGeoPoint("riderLocation").getLongitude());
                        }

                        mArrayAdapter.notifyDataSetChanged();
                        dlg.dismiss();
                    }
                }
            }
        });
    }
}
