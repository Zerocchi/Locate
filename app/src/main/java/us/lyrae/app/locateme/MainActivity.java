package us.lyrae.app.locateme;

import android.content.Intent;
import android.content.res.Resources;
import android.icu.text.DateFormat;
import android.icu.util.TimeZone;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static us.lyrae.app.locateme.R.id.map;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        OnMapReadyCallback {

    public static final int INTERVAL = 2000; // ms
    public static final int FASTEST_INTERVAL = 1000; // ms
    public static final int ZOOM_DURATION = 1500; // ms
    private static final float INITIAL_ZOOM = 16;

    public TextView latitudeTv;
    public TextView longitudeTv;
    public TextView addressTv;
    public TextView headerLocationTv;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private SupportMapFragment mapFragment;
    private GoogleMap map;
    private Handler handler = new Handler();
    Marker mCurrLocationMarker;

    private float zoom;

    private double latitude;
    private double longitude;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitudeTv = (TextView) findViewById(R.id.latitude);
        longitudeTv = (TextView) findViewById(R.id.longitude);
        addressTv = (TextView) findViewById(R.id.address);
        headerLocationTv = (TextView) findViewById(R.id.header_location);

        buildClient();
        //mGoogleApiClient.connect();

        if(mapFragment == null) {
            mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }
    }

    private synchronized void buildClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }


    protected void onStart() {
        // initial values
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        createLocationRequest();
        startLocationUpdates();
    }


    @Override
    public void onConnectionSuspended(int i) {
        stopLocationUpdates();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        if (location != null) {

            mLastLocation = location;

            if (mCurrLocationMarker != null) {
                mCurrLocationMarker.remove();
            }

            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();

            Resources res = getResources();
            ArrayList<String> coordinatesDMS = Helper.getFormattedLocationInDegree(latitude, longitude);

            // set text values
            latitudeTv.setText(coordinatesDMS.get(0));
            longitudeTv.setText(coordinatesDMS.get(1));
            String headerLocationText = String.format(res.getString(R.string.your_location_header)
                    , Math.round(location.getAccuracy()));
            headerLocationTv.setText(headerLocationText);

            GeocoderHandler handler = new GeocoderHandler();
            Helper.getAddressFromLocation(mLastLocation, this, handler);
            //updateTimeTv.setText(mLastUpdateTime);

            if(map.getCameraPosition().zoom == 2.0f) // biggest zoom value
                zoom = INITIAL_ZOOM;
            else
                zoom = map.getCameraPosition().zoom;

            //Place current location marker
            LatLng latLng = new LatLng(latitude, longitude);
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("Current Position");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
            mCurrLocationMarker = map.addMarker(markerOptions);

            //move map camera
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(latitude, longitude), zoom), ZOOM_DURATION, null);

        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        map = googleMap;

        //mapCamera(zoom, bearing);

        map.setMyLocationEnabled(true);
        map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                startLocationUpdates();
                return false;
            }
        });

        map.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int reason) {
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    if (mGoogleApiClient != null) {
                        stopLocationUpdates();
                    }
                }
            }
        });

        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(true);
        //map.getUiSettings().setScrollGesturesEnabled(false);

    }

    private class GeocoderHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            Address address;
            switch (message.what) {
                case 1:
                    Bundle bundle = message.getData();
                    address = bundle.getParcelable("address");
                    break;
                default:
                    address = null;
            }
            // replace by what you need to do
            if(address != null) {
                String result = address.getAddressLine(0) + "\n"
                        + address.getLocality() + "\n"
                        + address.getPostalCode() + " "
                        + (address.getSubAdminArea() == null ? "" : address.getSubAdminArea() + "\n")
                        + address.getAdminArea() + "\n"
                        + address.getCountryName();
                addressTv.setText(result);
            } else {
                addressTv.setText("Cannot get address");
            }
        }
    }
}

