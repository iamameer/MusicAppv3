package mdpcw2.musicappv3;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class NotificationGenerator extends IntentService {
    public NotificationGenerator() {
        super("Notification Service");
    }

    @Override
    protected void onHandleIntent(Intent intent){
        /*if (intent == null){
            //alternate code
        }else{
            //int xx = intent.getIntExtra(,);
            //plays music here
        }*/
    }

    //seems like not needed?
   /* @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }*/

    /*@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId); //try comment out? +return
        //plays music here
        return START_STICKY;
    }*/

    @Override
    public void onCreate() {
        super.onCreate();
        //log
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //stop here
    }
}
