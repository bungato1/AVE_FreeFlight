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
            droneControlService = ((DroneControlService.LocalBinder) service).getService();
            try {
                Log.i("AMANDA","I'm trying!!");
                droneControlService.triggerTakeOff();
                Thread.sleep(5000);
                droneControlService.setYaw(0.5f);
                Thread.sleep(2000);
                droneControlService.setYaw(-0.3f);
                Thread.sleep(2000);
                droneControlService.setGaz(0.2f);
                Thread.sleep(2000);
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

      /* try {
           for(int i = 0; i<10;i++){
               if (droneControlService != null){
                   break;
               } else {
                   wait(100);
               }
           }
          /* Log.i("AMANDA","IM TRYING");
           droneControlService.triggerTakeOff();
           droneControlService.setYaw(0.5f);
           wait(1000);
           droneControlService.setYaw(0);
           droneControlService.setGaz(0.5f);
           wait(1000);
           droneControlService.setGaz(0);
           droneControlService.triggerTakeOff();
       } catch(Exception e) {
           e.printStackTrace();
       }*/
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }
}
