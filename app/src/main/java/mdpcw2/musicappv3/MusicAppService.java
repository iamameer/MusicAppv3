/*
    This Service generates notification and handle music
 */

package mdpcw2.musicappv3;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class MusicAppService extends IntentService {

    //Global ID
    private static final int NOTI_ID = 100;  //Permission Request ID
    NotificationCompat.Builder notification; //Create a notification object

    String title, artist, duration; //String to hold title, artist and duration
    Bitmap bmp;                     //Bitmap variable to hold Album/Song image

    public MusicAppService() {
        super("Notification Service");
    }

    //This method initialise values and items
    private void init(){
        title = "Unknown title";
        artist = "Unknown artist";
        duration = "0:00";
        bmp = null;
    }

    //This method launch a notification
    public void startNoti(){
        //creating new one, assuming music updated
        notification = new NotificationCompat.Builder(this,"MusicAppv3");
        notification.setAutoCancel(true);

        //setting up notification
        notification.setColor(Color.rgb(40, 94, 18));
        notification.setSmallIcon(R.drawable.music_note);
        notification.setTicker(title); //showing current song
        notification.setContentTitle(title);
        //TODO current/progress in noti :c
        //TODO https://www.youtube.com/watch?v=g9LDWM3a3H8
        notification.setContentText(artist+"\t\t\t\t\t\t\t "+duration);
        notification.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle());
        //set LargeIcon = Album
        if (bmp != null){
            notification.setLargeIcon(bmp);
        }else{
            notification.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.bg_music));
        }

        //return to Main activity
        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setContentIntent(pendingIntent);

        //start notification
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTI_ID,notification.build());
        Log.d("MusicApp","Notification via MusicAppServiceStarted");
    }

    //This method handle incoming intent //onStartCommand passed to here eventually
    @Override
    protected void onHandleIntent(Intent intent){
        if (intent == null){
            Log.d("MusicApp","Null intent from MainActivity");
        }else{
            //getting intent data from MainActivity into global variable
            if (intent.getByteArrayExtra("imgPrev") != null){
                byte[] byteArray = intent.getByteArrayExtra("imgPrev");
                bmp = BitmapFactory.decodeByteArray(byteArray,0,byteArray.length);
            }
            if (intent.getStringExtra("artistPrev") != null){
                artist =intent.getStringExtra("artistPrev");}
            if (intent.getStringExtra("titlePrev") != null){
                title = intent.getStringExtra("titlePrev");}
            duration = intent.getStringExtra("dur");
            startNoti();
        }
        Log.d("MusicApp","MusicAppService onHandleIntent");
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
        return START_STICKY;
    }*/

    //onCreate lifecycle
    @Override
    public void onCreate() {
        super.onCreate();
        init();
        Log.d("MusicApp","MusicAppService created");
    }

    //onDestroy lifecycle
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("MusicApp","MusicAppService destroyed");
    }
}
