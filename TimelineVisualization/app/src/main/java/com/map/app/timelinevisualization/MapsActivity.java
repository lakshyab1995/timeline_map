package com.map.app.timelinevisualization;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ImageButton playBtn;

    //list for storing latitude & longitude
    List<LatLng> list = null;

    //list for storing time data
    List<String> timeList;
    SeekBar seekBar;

    //starting index of time in list
    int timesIndex=0;

    //ending index of time in list
    int timeeindex=0;
    boolean fUser=false;
    int seekBarPos;
    HeatmapTileProvider mProvider;
    TileOverlayOptions mOptions;
    boolean flag;

    // Create the gradient.
    int[] colors = {
            Color.rgb(0, 0, 255), // blue
            Color.rgb(255, 0, 0)    // red
    };

    // Create the intensity start points
    float[] startPoints = {
            0.1f, 1f
    };

    Gradient gradient = new Gradient(colors, startPoints);
    Handler myHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ConnectivityManager conMgr = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);

        // Checking if user is connected to mobile data or wifi for access to internet
        if ( conMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED
                || conMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED ) {

            setContentView(R.layout.activity_maps);
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
            timeList=new ArrayList<>();
            playBtn= (ImageButton) findViewById(R.id.play);
            seekBar= (SeekBar) findViewById(R.id.seekBar);
            seekBar.setProgress(0);
            seekBar.setMax(60);

            //listener for seek bar
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                    fUser = fromUser;
                    seekBarPos = progress;

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {


                    if(seekBarPos==0)
                        mMap.clear();//clearing the map

                    if(seekBarPos>0 && seekBarPos<=60) {

                        if (fUser) {

                            if(seekBarPos==1)
                                Toast.makeText(MapsActivity.this,"Starting Visualization",Toast.LENGTH_SHORT).show();
                            readTimeList();

                            //Creating a sublist from corresponding indexes received from readTimeList()
                            List<LatLng> newList = new ArrayList<>(list.subList(timesIndex, timeeindex));
                            mProvider = new HeatmapTileProvider.Builder()
                                    .gradient(gradient)
                                    .radius(20)
                                    .data(newList)
                                    .build();
                            mOptions = new TileOverlayOptions().tileProvider(mProvider);

                            //if heat map is already visible, clear the map
                            if (mOptions.isVisible()) {
                                mMap.clear();
                            }
                            //adding heatmap on google map
                            mMap.addTileOverlay(mOptions);

                            if(seekBarPos==60)
                                Toast.makeText(MapsActivity.this,"Visualizaion is finished",Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

            seekBar.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {

                    return flag;
                }
            });

            playBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //Handler for calling run method after every 1 second delay
                    myHandler.postDelayed(mMyRunnable, 1000);

                }
            });

        }
        else if ( conMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.DISCONNECTED
                || conMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.DISCONNECTED) {

            Toast.makeText(this,"Make sure you are connected to network",Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

    }

    //reading the time list and return the corresponding start and end index
    private void readTimeList()
    {

            if(seekBarPos>0 && seekBarPos<=10) {
                timesIndex = timeList.indexOf("0" + (seekBarPos-1) + ":00");
                timeeindex = timeList.indexOf("0" + (seekBarPos-1) + ":59");
            }
            else
            {
                timesIndex = timeList.indexOf((seekBarPos-1) + ":00");
                timeeindex = timeList.indexOf((seekBarPos-1) + ":59");
            }

    }


    //Reading CSV file and Adding time into timeList and lat. and long. in list
    private ArrayList<LatLng> readItems() {
        ArrayList<LatLng> list = new ArrayList<>();

        //Reading Data from CSV file
        InputStream inputStream = getResources().openRawResource(R.raw.test_data);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String csvLine;
            while ((csvLine = reader.readLine()) != null) {
                String[] row = csvLine.split(",");

                //Reading time
                String dateTime=row[1];
                String[] dtArr=dateTime.split(" ");
                String otime=dtArr[1];
                int index=otime.indexOf(".");
                String time=otime.substring(3,index);

                //Adding time in timeList
                timeList.add(time);

                //Reading lat and long
                double lat = Double.parseDouble(row[3]);
                double lng = Double.parseDouble(row[4]);

                //Adding lat. & long. in list
                list.add(new LatLng(lat, lng));
            }
        }
        catch (IOException ex) {
            throw new RuntimeException("Error in reading CSV file: "+ex);
        }
        finally {
            try {
                inputStream.close();
            }
            catch (IOException e) {
                throw new RuntimeException("Error while closing input stream: "+e);
            }
        }
        return list;
    }

    //This method is called when map is loaded successfully
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Calling an Async Task
        new AsyncCaller().execute();

    }

    private Runnable mMyRunnable = new Runnable()
    {
        @Override
        public void run()
        {

            seekBarPos = seekBar.getProgress();

            if(seekBarPos==0)
                seekBarPos++;

            if (seekBarPos > 0 && seekBarPos <= 60) {

                if (seekBarPos <= seekBar.getMax()) {


                    if(seekBarPos==1)
                    {
                        Toast.makeText(MapsActivity.this, "Starting Visualization", Toast.LENGTH_SHORT).show();
                    }

                    readTimeList();

                    //Creating a sublist from corresponding indexes received from readTimeList()
                    List<LatLng> newList = new ArrayList<>(list.subList(timesIndex, timeeindex));
                    mProvider = new HeatmapTileProvider.Builder()
                            .gradient(gradient)
                            .radius(20)
                            .data(newList)
                            .build();
                    mOptions = new TileOverlayOptions().tileProvider(mProvider);

                    //if heat map is already visible, clear the map
                    if (mOptions.isVisible()) {
                        mMap.clear();
                    }

                    //adding heatmap on google map
                    mMap.addTileOverlay(mOptions);

                    if(seekBarPos!=60)
                    seekBar.setProgress(seekBarPos + 1);

                    myHandler.postDelayed(mMyRunnable, 1000);
                    flag=true;
                    playBtn.setEnabled(false);
                    if(seekBarPos==seekBar.getMax()){

                        //removing any callbacks left in Handler
                        myHandler.removeCallbacks(mMyRunnable);
                        Toast.makeText(MapsActivity.this, "Visualization is finished", Toast.LENGTH_SHORT).show();
                        playBtn.setEnabled(true);
                        flag=false;
                }

            }
            }
        }
    };

    private class AsyncCaller extends AsyncTask<Void, Void, Void>
    {
        ProgressDialog pdLoading = new ProgressDialog(MapsActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //this method will be running on UI thread
            pdLoading.setMessage("\tLoading...");
            pdLoading.show();
        }
        @Override
        protected Void doInBackground(Void... params) {

            //this method will be running on background thread

            // Get the data: latitude/longitude positions
            list = readItems();

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            //this method will be running on UI thread

            pdLoading.dismiss();
        }

    }
}


