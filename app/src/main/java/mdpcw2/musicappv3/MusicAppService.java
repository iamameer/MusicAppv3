package mdpcw2.musicappv3;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class MusicAppService extends IntentService {

    private static final int NOTI_ID = 100;
    NotificationCompat.Builder notification;

    String title, artist, duration;
    Bitmap bmp;
    boolean isNotiRunning;


    public MusicAppService() {
        super("Notification Service");
    }

    //initialising variables
    private void init(){
        title = "Unknown title";
        artist = "Unknown artist";
        duration = "0:00";
        bmp = null;
    }

    //method to launch a notification
    public void startNoti(){
        //creating new one, assuming music updated
        notification = new NotificationCompat.Builder(this,"MusicAppv3");
        notification.setAutoCancel(true);

        //setting up noti
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

        //start noti
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTI_ID,notification.build());
        isNotiRunning = true;
    }

    //method to stop a running notification
    public void stopNoti(){
        if (isNotiRunning){
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.cancelAll();
        }
    }


    @Override
    protected void onHandleIntent(Intent intent){
        if (intent == null){
            //alternate code
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
            //plays music here
        }
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

        init();
        Log.d("MusicApp","MusicAppService created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //stopNoti();
        Log.d("MusicApp","MusicAppService destroyed");
        //stop here
    }
}
