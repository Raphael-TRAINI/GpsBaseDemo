package fr.innodev.trd.gpsbasedemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Location myLocation;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private Log log;
    private Marker piedMarker;
    private Marker last1Marker;
    private Marker lastMarker;
    private Marker oldMarker;
    private boolean launch;
    private boolean firtsMarker;
    public double angle;
    public double distFromLastPoint;

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private float lastNumberStep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.launch = true;
        this.firtsMarker = true;

        while (!permissionGranted()) ;

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            log.v("INFO", "Location Result" + location.toString());
                            updateMapDisplay(location);
                        }
                    }
                });

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    log.v("INFO", "Location Callback" + location.toString());
                    updateMapDisplay(location);
                }
            }
        };

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(30000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            sensorManager.registerListener(mSensorEventListener, stepCounterSensor, 1000000);
        }
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
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        piedMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(10, 10))
                .title("Calcul au pied")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
    }

    private void updateMapDisplay(Location myLocation) {
        LatLng curPos = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
        if (lastMarker != null) {
            last1Marker = lastMarker;
        }
        // On efface les vieux marqueur
        if (oldMarker != null) {
            if (!firtsMarker) {
                oldMarker.setVisible(false);
            } else {
                oldMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                this.firtsMarker = false;
            }
        }

        if (lastMarker != null) {
            // On change la couleur et le tet du marqueur précédent
            lastMarker.setTitle("Old Potition");
            lastMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            mMap.addPolyline(new PolylineOptions()
                    .add(lastMarker.getPosition(), curPos)
                    .width(5)
                    .color(Color.RED));

            // Calcule de la distance entre les deux dernier point
            float[] distance1 = new float[1];
            Location.distanceBetween(curPos.latitude,
                    curPos.longitude,
                    lastMarker.getPosition().latitude,
                    lastMarker.getPosition().longitude,
                    distance1);

            log.d("INFO", "distance : " + distance1[0]);
            ((TextView) findViewById(R.id.textView1)).setText("Distance du gps : " + distance1[0] + " m");

            // On enregistre pour suprimer le dernier marqueu
            oldMarker = lastMarker;
            distFromLastPoint = 0;
        }

        lastMarker = mMap.addMarker(new MarkerOptions().position(curPos).title("Position courante"));

        float zoom;
        if (this.launch) {
            zoom = mMap.getMaxZoomLevel() - 20.0f;
            this.launch = false;
        } else {
            zoom = mMap.getCameraPosition().zoom;
        }
        log.d("INFO", "Zoom = " + zoom);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(curPos, zoom));

        // Mise a jours de l'angle
        if (last1Marker != null && lastMarker != null) {
            angle = GetAngleOfLineBetweenTwoPoints(lastMarker, last1Marker);
            DisplayDirection(angle);
        }
    }


    final SensorEventListener mSensorEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                float nbStep = sensorEvent.values[0];
                if (lastNumberStep == 0.0f) {
                    lastNumberStep = nbStep;
                }
                float nbNewStep = nbStep - lastNumberStep;
                Log.d("INFO", "Step " + nbStep);
                Log.d("INFO", "New Step " + nbNewStep);
                Log.d("INFO", "Distance a pied : " + nbNewStep * 0.70);
                distFromLastPoint += nbNewStep * 0.70;
                ((TextView) findViewById(R.id.textView2)).setText("Distance a pied : " + distFromLastPoint + " m");
                lastNumberStep = nbStep;
                // distance metre to degre
                double distDegr = (distFromLastPoint * 0.00001) / 1.1132;

                if (lastMarker != null && angle != 0) {
                    double longitude = lastMarker.getPosition().longitude + (distDegr * (Math.cos(angle)));
                    double latitude = lastMarker.getPosition().latitude + (distDegr * (Math.sin(angle)));
                    piedMarker.setPosition(new LatLng(latitude, longitude));
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    private boolean permissionGranted() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {//Can add more as per requirement

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    123);
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Determines the angle of a straight line drawn between point one and two. The number returned, which is a double in degrees, tells us how much we have to rotate a horizontal line clockwise for it to match the line between the two points.
     * If you prefer to deal with angles using radians instead of degrees, just change the last line to: "return Math.atan2(yDiff, xDiff);"
     */
    public static double GetAngleOfLineBetweenTwoPoints(Marker p1, Marker p2) {
        double xDiff = p2.getPosition().latitude - p1.getPosition().latitude;
        double yDiff = p2.getPosition().longitude - p1.getPosition().longitude;
        return Math.atan2(yDiff, xDiff);
    }

    public static boolean isBetween(double x, double lower, double upper) {
        return lower <= x && x <= upper;
    }

    public void DisplayDirection(Double angle){
        ((TextView) findViewById(R.id.textView3)).setText("Angle clac : " + angle + " rad");
        if (isBetween(angle, 1, 5)) {

        }
        ((TextView) findViewById(R.id.textView4)).setText("Angle clac : " + angle + " rad");
    }
}
