package com.parrot.freeflight.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Binder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.os.Bundle;

import android.location.LocationListener;
import android.location.LocationManager;
import android.location.Location;


public class DroneAutonomyAVE extends Service {
    DroneControlService droneControlService;
    private IBinder myBinder = new MyBinder();

    public class MyBinder extends Binder {
        public DroneAutonomyAVE getService() {
            return DroneAutonomyAVE.this;
        }
    }

    public boolean AVEenabled; //for AVE button enabled status

    public BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();
    private static final String TAG = "CommsActivity";
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    public String turn;
    public String direction;
    public String speed;
    private long stopwatch;
    private int emptyReceiveCount = 0;

    // Variables to send to CCP
    String AVEFlag = "1";
    String LFFlag = "0";
    String vehicleName = "AVE1"; // first drone name
    String systemStatus;


    // AVE Flag (0 = PTP, 1 = AVE)
    // Leader/Follower Flag (0 = follower, 1 = leader)
    // Vehicle name (AVE1)
    // system status (0 = System OK, 1 = Emergency Stop Required)
    // Location: Latitude, Longitude
    // Line of Bearing
    // Velocity (XYZ)
    // Acceleration (XYZ)
    // Extra Info
    // 1,0,AVE1,0,100.0000090,100.0050000,0,20,0,0,0,0.2,0

    //Location Management Variables
    private LocationManager locationManager;
    double longitudeGPS, latitudeGPS, velocityGPS;
    float bearingGPS;


    // Constant for UUID
    static final UUID myUUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
    public final static String EXTRA_ADDRESS = "B8:27:EB:7A:B9:13";

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("AMANDA", "service connected");
            droneControlService = ((DroneControlService.LocalBinder) service).getService(); //represents the drone
            //try and catch statements are going to have if statements inside:
            // if (variable taken from comms = right forward)
            //    then turn that into droneControlService.'command'
            /*try {                        //inside should be the comms variables that kayla parsed
                Log.i("AMANDA","I'm trying!!");
                droneControlService.triggerTakeOff();
                Thread.sleep(5000);
                droneControlService.setYaw(0.5f);
                Thread.sleep(2000);
                droneControlService.setYaw(-0.3f);
                Thread.sleep(2000);
                droneControlService.setGaz(0.3f);
                Thread.sleep(2500);
                droneControlService.triggerTakeOff();
            } catch(Exception e) {
                e.printStackTrace();
            } */
        }

        public void onServiceDisconnected(ComponentName name) {
            droneControlService = null;
            /*try {
                mmSocket.close();
            } catch (IOException closeException) {
            }*/
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void startBluetoothThread() {
        final BluetoothDevice device = BTAdapter.getRemoteDevice(EXTRA_ADDRESS);
        try {
            new ConnectThread(device).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setAVEenabled(boolean b) {
        AVEenabled = b;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //Create the location manager and try to request updates
        bindService(new Intent(this, DroneControlService.class), mConnection, Context.BIND_AUTO_CREATE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(

                    LocationManager.GPS_PROVIDER, 1000, 0.0f, locationListenerGPS);
            Log.i("AMANDA", "Inside location manager");

        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return myBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        try {
            mmSocket.close(); //closes Bluetooth socket
        } catch (IOException closeException) {

        }
        unbindService(mConnection); //unbinds from DroneControlService
        locationManager.removeUpdates(locationListenerGPS); //stops GPS from updating
        return true;
    }

    public class ConnectThread extends Thread {
        private ConnectThread(BluetoothDevice device) throws IOException {
            /*if (mmSocket != null) {
                if(mmSocket.isConnected()) {
                    send();
                }
            }*/
            mmDevice = device;
        }

        public void run() {
            BluetoothSocket tmp = null;
            try {
                Log.i("AMANDA", "Inside ConnectThread try before createRFComm");
                // This is the UUID that is in the Python Code on the RP3
                tmp = mmDevice.createRfcommSocketToServiceRecord(myUUID);

                Log.i("AMANDA", "Inside ConnectThread try after createRFComm");

                Log.i(TAG, "BOND STATE: " + mmDevice.getBondState());
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
            BTAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                Log.v(TAG, "Connection exception!");
                try {
                    mmSocket.close();
                   /* mmSocket = (BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(mmDevice, 1);
                    mmSocket.connect();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                */
                } catch (IOException closeException) {

                }
            }

            receive();
        }

        // InputStream: accepts an input stream of bytes
        // getInputStream(): called in order to retreive InputStream objects, which
        // are automatically connected to the socket
        public void receive() {
            try {
            InputStream mmInputStream = mmSocket.getInputStream();
            OutputStream mmOutputStream = mmSocket.getOutputStream();
            byte[] buffer = new byte[1024];
                emptyReceiveCount = 0;
                while (emptyReceiveCount < 100 && AVEenabled) {
                    // Sample rate = 250 ms = .25 second
                    // SEND DATA
                    String msg = dataToSend();
                    // writes bytes from the specified byte array to this output stream
                    mmOutputStream.write(msg.getBytes());

                    // RECEIVE DATA
                    // Try to receive message
                    if (mmInputStream.available() != 0) {
                        int bytes = mmInputStream.read(buffer);
                        String readMessage = new String(buffer, 0, bytes);
                        Log.d(TAG, "Receive);d from CCP (Unparsed): " + readMessage);
                        parseDriveCommand(readMessage);
                    }

                    // if message wasn't received, increment emptyReceiveCount
                    else {
                        emptyReceiveCount++;
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // NEED TO LAND, WE HAVE NOT GOTTEN MESSAGES IN 1 SEC

                /*String readMessage = "";
                // continue until no byte is available because the stream is at the end of the file
                while (readMessage != "End") {
                    int bytes = mmInputStream.read(buffer);
                    readMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "Receive);d from CCP (Unparsed): " + readMessage);
                    parseDriveCommand(readMessage);
*//*                    String sendMsg = dataToSend();
                    send(sendMsg);*//*
                }
                // Sets TextView object to display the message
//                TextView test1 = (TextView) findViewById(R.id.Voltage);
//                test1.setText("Data from CCP\n" + readMessage);
                //mmSocket.close();*/
            } catch (IOException e) {
                Log.e(TAG, "Problems occurred!");
            }
        }
    }

    public void parseDriveCommand(String message) {
        // turn, direction, speed, stuff for PTP
        String string = "Straight,Forward,27,36,-2";

        String[] values = string.split(",");
        turn = values[0];
        direction = values[1];
        speed = values[2];

        //TODO: Replace the logging with actual drone movement
        Log.d(TAG, "Received turn: " + turn);
        Log.d(TAG, "Received turn: " + direction);
        Log.d(TAG, "Received turn: " + speed);

        //Yaw is right or left
        //Gaz is up or down
        //Pitch is forward or backwards
/*
        if (values[0].equals("Left")) {
            if (values[1].equals("Forward")) {
                //TODO: Go forwards left

            } else if (values[1].equals("Reverse")) {
                //TODO: Go backwards left
            }
        } else if (values[0].equals("Right")) {
            if (values[1].equals("Forward")) {
                //TODO: Go forwards right
            } else if (values[1].equals("Reverse")) {
                //TODO: Go backwards right
            }
        } else if (values[0].equals("Straight")) {
            if (values[1].equals("Forward")) {
                //TODO: Go forwards straight
                droneControlService.setPitch(0.5f);
            } else if (values[1].equals("Reverse")) {
                //TODO: Go backwards straight
                droneControlService.setPitch(-0.5f);
            }
        } else {
            //Do nothing; bad data
        }
*/

    }

    public String dataToSend() {
        // AVE Flag (0 = PTP, 1 = AVE)
        // Leader/Follower Flag (0 = follower, 1 = leader)
        // Vehicle name (AVE1)
        // system status (0 = System OK, 1 = Emergency Stop Required)
        // Location: Latitude, Longitude
        // Line of Bearing
        // Velocity (XYZ)
        // Acceleration (XYZ)
        // Extra Info
        // 1,0,AVE1,0,100.0000090,100.0050000,0,20,0,0,0,0.2,0
        Log.i("AMANDA", "Before getting longitude/other");
        String longitude = Double.toString(longitudeGPS);
        String latitude = Double.toString(latitudeGPS);
        String bearing = Float.toString(bearingGPS);
        String speed = Double.toString(velocityGPS);
        Log.i("AMANDA", "After getting longitude/other");


        //String data = st  + " " + Integer.toString(emptyReceiveCount);
        String data = longitude + "," + latitude + "," + bearing + "," + speed;
        return data;

    }

    private final LocationListener locationListenerGPS = new LocationListener() {
        public void onLocationChanged(Location location) {
            Log.i(TAG, "onLocationChanged: " + location.toString());
            longitudeGPS = location.getLongitude();
            latitudeGPS = location.getLatitude();
            bearingGPS = location.getBearing();
            velocityGPS = location.getSpeed();

        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
            //change AVE status - something went wrong with GPS (maybe)
        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {
            //change AVE status to emergency land - not connection to GPS
        }
    };


}