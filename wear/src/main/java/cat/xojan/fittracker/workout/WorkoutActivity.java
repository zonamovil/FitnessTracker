package cat.xojan.fittracker.workout;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.wearable.Wearable;

import cat.xojan.fittracker.R;
import cat.xojan.fittracker.workout.controller.DistanceController;
import cat.xojan.fittracker.workout.controller.FitnessController;

public class WorkoutActivity extends WearableActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        WorkoutFragment.TrackingStateListener,
        FragmentStartWorkout.WorkoutStartListener {

    private static final String TAG = "WorkoutActivity";

    private static final long UPDATE_INTERVAL_MS = 3 * 1000;
    private static final long FASTEST_INTERVAL_MS = 3 * 1000;

    private GoogleApiClient mGoogleApiClient;
    private TextView mTextView;
    private boolean isFirstLocation = true;
    private boolean mIsTracking = false;
    private DistanceController mDistanceController;
    private FitnessController mFitnessController;
    private Location mOldLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAmbientEnabled();
        setContentView(R.layout.activity_start);
        if (!hasGps()) {
            Log.d(TAG, "This hardware doesn't have GPS.");
            // Fall back to functionality that does not use location or
            // warn the user that location function is not available.
            finish();//TODO
        }
        String activityType = getIntent().getStringExtra("EXTRA_ACTIVITY_TYPE");
        FitnessController.getInstance().setFitnessActivity(activityType);

        // Build a new GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Wearable.API)  // used for data layer API
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mTextView = (TextView) findViewById(R.id.text);

        mDistanceController = DistanceController.getInstance();
        mFitnessController = FitnessController.getInstance();

        mFitnessController = FitnessController.getInstance();
        mFitnessController.init(this);
    }

    private boolean hasGps() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi
                    .removeLocationUpdates(mGoogleApiClient, this);
        }
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL_MS)
                .setFastestInterval(FASTEST_INTERVAL_MS);

        LocationServices.FusedLocationApi
                .requestLocationUpdates(mGoogleApiClient, locationRequest, this)
                .setResultCallback(status -> {
                    if (status.getStatus().isSuccess()) {
                        Log.d(TAG, "Successfully requested location updates");
                    } else {
                        Log.e(TAG,
                                "Failed in requesting location updates, "
                                        + "status code: "
                                        + status.getStatusCode()
                                        + ", message: "
                                        + status.getStatusMessage());
                    }
                });
    }

    // Disconnect from Google Play Services when the Activity stops
    @Override
    protected void onStop() {

        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "connection to location client suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
    }

    @Override
    public void onLocationChanged(Location location) {
        if (isFirstLocation) {
            mTextView.setVisibility(View.GONE);
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new FragmentStartWorkout())
                    .commit();
            isFirstLocation = false;
        } else {
            updateTrack(location);
        }

        mOldLocation = location;
        // Display the latitude and longitude in the UI
        //mTextView.setText("Latitude:  " + String.valueOf( location.getLatitude()) + "\nLongitude:  " + String.valueOf( location.getLongitude()));
        //addLocationEntry(location.getLatitude(), location.getLongitude());
    }

    public void updateTrack(Location location) {
        LatLng currentPosition = new LatLng(location.getLatitude(), location.getLongitude());
        if (mIsTracking) {
            mDistanceController.updateDistance(getOldPosition(mOldLocation), currentPosition);
            mFitnessController.storeLocation(location);
        }
    }

    private LatLng getOldPosition(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    @Override
    public void isTracking(boolean isTracking) {
         mIsTracking = isTracking;
    }

    @Override
    public void removeLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi
                    .removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void notifyWorkoutStart() {
        //first location
        mFitnessController.storeLocation(mOldLocation);
        //start tracking
        mIsTracking = true;
    }

    /*private void addLocationEntry(double latitude, double longitude) {
        if (!mSaveGpsLocation || !mGoogleApiClient.isConnected()) {
            return;
        }

        mCalendar.setTimeInMillis(System.currentTimeMillis());

        // Set the path of the data map
        String path = Constants.PATH + "/" + mCalendar.getTimeInMillis();
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(path);

        // Set the location values in the data map
        putDataMapRequest.getDataMap()
                .putDouble(Constants.KEY_LATITUDE, latitude);
        putDataMapRequest.getDataMap()
                .putDouble(Constants.KEY_LONGITUDE, longitude);
        putDataMapRequest.getDataMap()
                .putLong(Constants.KEY_TIME, mCalendar.getTimeInMillis());

        // Prepare the data map for the request
        PutDataRequest request = putDataMapRequest.asPutDataRequest();

        // Request the system to create the data item
        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        if (!dataItemResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Failed to set the data, "
                                    + "status: " + dataItemResult.getStatus()
                                    .getStatusCode());
                        }
                    }
                });
    }*/
}
