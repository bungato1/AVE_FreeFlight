package com.parrot.freeflight.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class DroneAutonomyAVE extends Service {
    DroneControlService droneControlService;


    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder service)
        {
            Log.i("AMANDA","service connected");
            droneControlService = ((DroneControlService.LocalBinder) service).getService(); //represents the drone
            //TODO: add Jess' service stuff in here
            //i.e. communicationsService = ((BTService.myBinder) service).getService();     //represents comms

            //try and catch statements are going to have if statements inside:
            // if (variable taken from comms = right forward)
            //    then turn that into droneControlService.'command'
            try {                                           //inside should be the comms variables that kayla parsed
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
            }


        }
        public void onServiceDisconnected(ComponentName name)
        {
            droneControlService = null;
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        bindService(new Intent(this, DroneControlService.class), mConnection, Context.BIND_AUTO_CREATE);

        //TODO: get Jess' service in here and make sure we can connect to raspberry pi still
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }
}
