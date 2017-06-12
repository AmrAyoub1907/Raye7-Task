package com.amrayoub.Raye7Task;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Build;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMapLongClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback {
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private double lat;
    private double lng;
    private double dest_lat;
    private double dest_lng;
    EditText source, destination;
    LatLng[] markerPoints = new LatLng[2];
    private int mFrom_to = -1;
    Marker[] markers = new Marker[2];
    TripInfo tripInfo = new TripInfo();
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public static final int REQUEST_LOCATION = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, myToolbar, 0, 0);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }
        markerPoints[0] = null;
        markerPoints[1] = null;
        source = (EditText) findViewById(R.id.source_edittext);
        destination = (EditText) findViewById(R.id.dest_edittext);
        source.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    mFrom_to = 0;
                    address_LatlngURL(source.getText().toString());
                    return true;
                }
                return false;
            }
        });
        destination.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    mFrom_to = 1;
                    address_LatlngURL(destination.getText().toString());
                    return true;
                }
                return false;
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //User has previously accepted this permission
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }
        } else {
            //Not in api-23, no need to prompt
            mMap.setMyLocationEnabled(true);
        }
        // Add a marker in Sydney and move the camera
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMapLongClickListener(this);

    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Check Permissions Now
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        } else {
            // permission has been granted, continue as usual
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        }
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {

                            mFrom_to = 0;
                            lat = location.getLatitude();
                            lng = location.getLongitude();
                            LatLng source = new LatLng(lat, lng);
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(source,10));
                            mMap.animateCamera(CameraUpdateFactory.zoomIn());
                            latlng_adderssURL(source);
                            checkmarkers(source);

                        }
                    }
                });
        return false;
    }
    @Override
    public void onMapLongClick(LatLng latLng) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,10));
        mMap.animateCamera(CameraUpdateFactory.zoomIn());
        mFrom_to = 1;
        latlng_adderssURL(latLng);
        checkmarkers(latLng);

        /*dest_lat = latLng.latitude;
        dest_lng = latLng.longitude;
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Destination")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));*/
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_notification:
                Toast.makeText(this, "Notificafftion selected", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_message:
                Toast.makeText(this, "Messages selected", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
        return true;
    }
    private String getdirectionurl(LatLng latlng_source ,LatLng latlng_dest){
        String main = "https://maps.googleapis.com/maps/api/directions/json?";
        String origin = "origin="+latlng_source.latitude+","+latlng_source.longitude;
        String dest = "destination="+latlng_dest.latitude+","+latlng_dest.longitude;
        String url = main+origin+"&"+dest+"&key=AIzaSyCwWYOlTr3yTmxV9ezdorU66ChxPSXRjKU";
        return url;
    }
    public void Request_a_pickup(View view) {
        Calendar c = Calendar.getInstance();
        int mHour = c.get(Calendar.HOUR_OF_DAY);
        int mMinute = c.get(Calendar.MINUTE);
        // Launch Time Picker Dialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay,
                                          int minute) {

                        Toast.makeText(getBaseContext(),hourOfDay + ":" + minute,Toast.LENGTH_SHORT).show();
                    }
                }, mHour, mMinute, false);
        tripInfo.setTime(mHour,mMinute);
        timePickerDialog.show();

        c = Calendar.getInstance();
        int mYear = c.get(Calendar.YEAR);
        int mMonth = c.get(Calendar.MONTH);
        int mDay = c.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        Toast.makeText(getBaseContext(),
                                dayOfMonth + "-" + (monthOfYear + 1) + "-" + year,
                                Toast.LENGTH_SHORT).show();
                    }
                }, mYear, mMonth, mDay);
        tripInfo.setDate(mDay,mMonth,mYear);
        datePickerDialog.show();
    }
    private class DirectionsJSON extends AsyncTask<String, Void, String> {
        //get JSON-string in background thread
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                data = JSONstrongConnection(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
        }
    }
    private class GeocodingJSONlatlng extends AsyncTask<String, Void, String> {
        //get JSON-string in background thread
        String[] addresses ;
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                data = JSONstrongConnection(url[0]);
                JSONObject jsonObject = new JSONObject(data);
                JSONArray jsonArray = jsonObject.getJSONArray("results");
                addresses = new String[jsonArray.length()];
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject theObject = jsonArray.getJSONObject(i);
                    addresses[i]=(String)theObject.get("formatted_address");
                }
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Select address");
            int selected = -1; // does not select anything
            builder.setSingleChoiceItems(addresses, selected,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(mFrom_to==1)
                                destination.setText(addresses[which]);
                            else
                                source.setText(addresses[which]);
                            dialog.dismiss();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }
    private class GeocodingJSONaddress extends AsyncTask<String, Void, String> {
        //get JSON-string in background thread
        private ProgressDialog progressDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Searching on address, Please wait...");
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }

        String[] addresses ;
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                data = JSONstrongConnection(url[0]);
                JSONObject jsonObject = new JSONObject(data);
                JSONArray jsonArray = jsonObject.getJSONArray("results");
                addresses = new String[jsonArray.length()];
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject theObject = jsonArray.getJSONObject(i);
                    JSONObject temp = theObject.getJSONObject("geometry");
                    JSONObject addressObject = temp.getJSONObject("location");
                    String lat = addressObject.getString("lat");
                    String lng = addressObject.getString("lng");
                    dest_lat = Double.parseDouble(lat);
                    dest_lng = Double.parseDouble(lng);
                }
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            LatLng latLng = new LatLng(dest_lat,dest_lng);
            progressDialog.dismiss();
            checkmarkers(latLng);
        }
    }
    private String JSONstrongConnection(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            br.close();
        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
        private ProgressDialog progressDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Fetching route, Please wait...");
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.BLUE);
                lineOptions.geodesic(true);

            }
            // Drawing polyline in the Google Map for the i-th route
            mMap.addPolyline(lineOptions);
            progressDialog.dismiss();
        }
    }
    private void latlng_adderssURL(LatLng latLng) {
        //https://maps.googleapis.com/maps/api/geocode/json?latlng=40.714224,-73.961452&key=YOUR_API_KEY
        String S_latlng = latLng.latitude + "," + latLng.longitude;
        String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" +
                S_latlng + "&key=AIzaSyBlHocZUFT_b3Uk04TjineQ6cRSFBk8U-w";
        GeocodingJSONlatlng jsondata = new GeocodingJSONlatlng();
        // Start downloading json data from Google Directions API
        jsondata.execute(url);
    }
    private void address_LatlngURL(String address){
        String S_address = address;
        StringBuilder myName = new StringBuilder(S_address);
        for(int i =0;i<S_address.length();i++){
            if(S_address.charAt(i)==' '){
                myName.setCharAt(i, '+');
            }
        }
        S_address=myName.toString();
        String url = "https://maps.googleapis.com/maps/api/geocode/json?address=" +
                S_address + "&key=AIzaSyBlHocZUFT_b3Uk04TjineQ6cRSFBk8U-w";
        GeocodingJSONaddress jsondata = new GeocodingJSONaddress();
        // Start downloading json data from Google Directions API
        jsondata.execute(url);
    }
    private void checkmarkers(LatLng latLng) {
        if(mFrom_to == 0){
            markers[0]=mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Source")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }
        else if(mFrom_to == 1){
                markers[1]=mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title("Destination")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }
        markerPoints[mFrom_to]=latLng;
        if (markerPoints[0]!= null &&markerPoints[1]!=null ) {
            mMap.clear();
            addmarkers();
            LatLng origin = markerPoints[0];
            LatLng dest = markerPoints[1];
            // Getting URL to the Google Directions API
            String url = getdirectionurl(origin, dest);
            DirectionsJSON jsondata = new DirectionsJSON();
            // Start downloading json data from Google Directions API
            jsondata.execute(url);
        }

    }
    private void addmarkers(){
        markers[0]=mMap.addMarker(new MarkerOptions()
                .position(markerPoints[0])
                .title("Source")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        markers[1]=mMap.addMarker(new MarkerOptions()
                .position(markerPoints[1])
                .title("Destination")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        tripInfo.setSource(markerPoints[0]);
        tripInfo.setDestination(markerPoints[1]);


    }
    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                //  TODO: Prompt with explanation!

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                    if (ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mMap.setMyLocationEnabled(true);
                        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

        }
    }

}
