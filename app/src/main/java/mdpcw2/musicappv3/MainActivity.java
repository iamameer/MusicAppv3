package mdpcw2.musicappv3;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;

public class MainActivity extends AppCompatActivity {

    //global declaration
    private static final int FIN = 20;
    private final static int MAX_VOLUME = 100;
    private final static int MIN_VOLUME = 0;
    private static final int CODE_LOAD = 5;
    private static final int SELECT_MUSIC = 10;
    private static final int NOTI_ID = 50;

    private ImageView imgBg,imgLoad,imgPlay,imgPause,imgStop,imgAlbum,imgVolume, imgList;
    private TextView txtTitle,txtArtist,txtVolume, txtDur, txtCur;
    private SeekBar seekBar;
    boolean isMute, isListOpened;
    int curVol;
    int oldY,newY;

    Bitmap imgPrev;
    String titlePrev, artistPrev;

    MP3Player mp3Player = new MP3Player();
    MediaMetadataRetriever metadataRetriever;
    NotificationCompat.Builder notification;

    byte[] art;
    Uri uri;
    Handler handler;
    Runnable runnable;

    //initializing values
    private void init(){
        imgBg = findViewById(R.id.imgBg);
        imgLoad = findViewById(R.id.imgLoad);
        imgPlay = findViewById(R.id.imgPlay);
        imgPause = findViewById(R.id.imgPause);
        imgStop = findViewById(R.id.imgStop);
        imgAlbum = findViewById(R.id.imgAlbum);
        imgVolume = findViewById(R.id.imgVolume);
        imgList = findViewById(R.id.imgList);

        imgBg.setImageAlpha(80);
        imgBg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imgAlbum.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imgBg.setImageDrawable(getDrawable(R.drawable.bg_music));
        imgAlbum.setImageDrawable(getDrawable(R.drawable.bg_music));

        imgPlay.setEnabled(false);
        imgPause.setEnabled(false);
        imgPause.setVisibility(View.INVISIBLE);
        imgStop.setEnabled(false);
        imgVolume.setEnabled(false);

        imgPrev = null;
        titlePrev = "";
        artistPrev = "";

        txtTitle = findViewById(R.id.txtTitle);
        txtArtist = findViewById(R.id.txtArtist);
        txtDur = findViewById(R.id.txtDur);
        txtCur = findViewById(R.id.txtCur);
        txtVolume = findViewById(R.id.txtVolume);

        txtCur.setVisibility(View.INVISIBLE);
        txtDur.setVisibility(View.INVISIBLE);
        txtTitle.setSelected(true);

        seekBar = findViewById(R.id.seekBar);
        seekBar.setEnabled(false);
        seekBar.getProgressDrawable().setColorFilter(
                new PorterDuffColorFilter(Color.rgb(242, 53, 138), PorterDuff.Mode.SRC_IN));

        isListOpened = false;
        isMute = false;
        curVol = 50;
    }

    //setting up events
    private void setEvents(){

        //loading music from gallery
        imgLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopNoti();
                pickMusic();}
        });

        //play button
        imgPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //check if file selected (after stop = released)
                if (mp3Player == null){
                    Toast.makeText(getApplicationContext(),"No file selected",Toast.LENGTH_SHORT).show();
                }else{playPause();}
            }
        });

        //pause button
        imgPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //check if file selected (after stop = released)
                if (mp3Player == null){
                    Toast.makeText(getApplicationContext(),"No file selected",Toast.LENGTH_SHORT).show();
                }else{playPause();}
            }
        });

        //stop button
        imgStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //check if file selected
                if (mp3Player == null){
                    Toast.makeText(getApplicationContext(),"No music playing",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getApplicationContext(),"Music Stopped",Toast.LENGTH_SHORT).show();
                    stop();
                }
            }
        });

        //mute button
        imgVolume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (mp3Player != null){
                        if (isMute){    //if currently muted, unmute it
                            setVol(curVol);
                            txtVolume.setText(String.valueOf(curVol));
                            isMute = false;
                            imgVolume.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
                            Toast.makeText(getApplicationContext(),"Unmuted",Toast.LENGTH_SHORT).show();
                        }else {          //otherwise
                            mp3Player.mediaPlayer.setVolume(0, 0);
                            txtVolume.setText(String.valueOf(0));
                            isMute = true;
                            imgVolume.setColorFilter(new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_IN));
                            Toast.makeText(getApplicationContext(), "Muted", Toast.LENGTH_SHORT).show();
                        }
                    }
                }catch (Exception e){
                    Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        //seekBar listener
        //seekBar in media player event reference:
        //https://www.youtube.com/watch?v=HB3DoZh1QWU
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress;
            boolean input;

            @Override
            //capture progress and input during onProgressChange
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                progress = i;
                input = b;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            //once user stopped tracking, seekTo() the music player
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (input){
                    mp3Player.mediaPlayer.seekTo(progress);
                }
            }
        });

        //List button
        imgList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //stop a notif if exist
                stopNoti();
                isListOpened = true;
                //passing the meta-data via intent
                Intent intent = new Intent(MainActivity.this,musicList.class);
                //sending bitmap via uri
                //http://www.jayrambhia.com/blog/pass-activity-bitmap
                //Jay Rambhia
                if (imgPrev != null){
                    ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                    imgPrev.compress(Bitmap.CompressFormat.PNG,100,bStream);
                    byte[] byteArray = bStream.toByteArray();
                    intent.putExtra("imgPrev", byteArray);
                }
                //sending song title and song artist (String)
                if (titlePrev != null){intent.putExtra("titlePrev",(String)titlePrev);}
                if (artistPrev != null){intent.putExtra("artistPrev",(String)artistPrev);}
                String dur = txtDur.getText().toString();
                intent.putExtra("dur",dur);
                startActivityForResult(intent, SELECT_MUSIC);
            }
        });

    }

    //creating a thread for the music player
    private void playCycle(){
        seekBar.setProgress(mp3Player.getProgress());
        txtCur.setText(getTime(mp3Player.getProgress()));
        if (!mp3Player.mediaPlayer.isPlaying()){
            stop();
        }

        //if the music player is playing/paused, run thread
        if ((mp3Player.getState() == MP3Player.MP3PlayerState.PLAYING) || (mp3Player.getState() == MP3Player.MP3PlayerState.PAUSED)){
            runnable = new Runnable() {
                @Override
                public void run() {
                    playCycle();
                }
            };
            handler.postDelayed(runnable,1000); //delay call itself once every second
        }else { // stop condition
            if(uri != null){
                Toast.makeText(getApplicationContext(),"Finished playing: "+getFileName(uri),Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(getApplicationContext(),"Finished song",Toast.LENGTH_SHORT).show();
            }
            stop(); //resetting the UI once song ended
        }

    }

    //selecting music directly
    private void pickMusic(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, MediaStore.Audio.Media.INTERNAL_CONTENT_URI);
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String path = dir.getPath();
        Uri data = Uri.parse(path);

        intent.setDataAndType(data,"audio/*");
        startActivityForResult(intent,CODE_LOAD);

        Toast.makeText(getApplicationContext(),"Select a music",Toast.LENGTH_SHORT).show();
    }

    //Getting file name from Uri
    //https://stackoverflow.com/questions/5568874/how-to-extract-the-file-name-from-uri-returned-from-intent-action-get-content
    public String getFileName(Uri uri) {
        String result = null;
        //query the result in virtual table, retrieve via getContentResolver()
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        //cutting out the extension of the file
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    //change Pause/Play respectively
    private void playPause(){
        //if its playing, pause it
        if (mp3Player.getState() ==  MP3Player.MP3PlayerState.PLAYING) {
            imgPlay.setVisibility(View.VISIBLE);
            imgPlay.setEnabled(true);
            imgPause.setVisibility(View.INVISIBLE);
            imgPause.setEnabled(false);
            Toast.makeText(getApplicationContext(),"Music Paused",Toast.LENGTH_SHORT).show();
            mp3Player.pause();
            //if its paused, play it
        }else if (mp3Player.getState() == MP3Player.MP3PlayerState.PAUSED){
            imgPlay.setVisibility(View.INVISIBLE);
            imgPlay.setEnabled(false);
            imgPause.setVisibility(View.VISIBLE);
            imgPause.setEnabled(true);
            Toast.makeText(getApplicationContext(),"Music Resumed",Toast.LENGTH_SHORT).show();
            mp3Player.play();
            //if its stopped, load new
        }else if(mp3Player.getState() == MP3Player.MP3PlayerState.STOPPED){
            imgLoad.performClick();
            //if error, display toast
        }else if (mp3Player.getState() == MP3Player.MP3PlayerState.ERROR) {
            Toast.makeText(getApplicationContext(),"Error - Restart application",Toast.LENGTH_SHORT).show();
        }
    }

    //stopping music and resetting play/pause button
    private void stop(){
        mp3Player.stop();
        imgStop.setEnabled(false);
        imgPlay.setEnabled(true);
        imgPlay.setVisibility(View.VISIBLE);
        imgPause.setEnabled(false);
        imgPause.setVisibility(View.INVISIBLE);
        imgVolume.setEnabled(false);
        reset();
    }

    //reset UI
    private void reset(){
        //reset images
        imgBg.setImageDrawable(getDrawable(R.drawable.bg_music));
        imgAlbum.setImageDrawable(getDrawable(R.drawable.bg_music));
        //reset text
        txtArtist.setText(R.string.artist);
        txtTitle.setText(R.string.title);
        txtCur.setText(R.string.dur);
        txtDur.setText(R.string.dur);
        txtDur.setVisibility(View.INVISIBLE);
        txtCur.setVisibility(View.INVISIBLE);
        //reset seekBar
        seekBar.setEnabled(false);
        seekBar.setProgress(0);
        //clear resource
        if (handler != null){
            handler.removeCallbacks(runnable);
        }
        //clear Prev
        imgPrev = null;
        titlePrev = null;
        artistPrev = null;
    }

    //acquiring meta-data
    //http://mrbool.com/how-to-extract-meta-data-from-media-file-in-android/28130
    //Sanyam Kalra 2014
    private void getMeta(Uri uri){
        metadataRetriever = new MediaMetadataRetriever();
        try{
            //getting filepath (fileDescriptor) from Uri
            //https://stackoverflow.com/questions/3401579/get-filename-and-path-from-uri-from-mediastore
            //Jun 24 '16 at 15:55
            //YYamil
            ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri,"r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            metadataRetriever.setDataSource(fileDescriptor);

            //getting album picture
            art = metadataRetriever.getEmbeddedPicture();
            Bitmap songImage = BitmapFactory.decodeByteArray(art,0,art.length);
            imgAlbum.setImageBitmap(songImage);
            imgBg.setImageBitmap(songImage);

            //getting other data
            txtArtist.setText(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            txtTitle.setText(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));

            //setting prev data for intent passing
            if (songImage != null){imgPrev = songImage;}
            artistPrev = txtArtist.getText().toString();
            titlePrev = txtTitle.getText().toString();
        }catch (Exception e){
            //if no meta-data found, set the Filename to be the title instead
            Toast.makeText(getApplicationContext(),"Error retrieving data",Toast.LENGTH_SHORT).show();
            imgPrev = null;
            artistPrev = null;
            titlePrev = getFileName(uri);
            txtTitle.setText(getFileName(uri));
        }

    }

    //convert milliseconds to minute and seconds
    private String getTime(Integer time){
        int min,sec;
        String minsec;

        if (time <60000){   //under 1 minute
            min = 0;
            sec = time / 1000;
        }else{              //more than 1 minute
            min = (time / 1000) / 60;
            sec = (time / 1000) % 60;
        }                   //not going to do the Hour hand, its unlikely to happen

        if (sec <10){
            minsec = min +":0"+sec; //adding 0 at the beginning to please the eyes
        }else {
            minsec = min +":"+sec;
        }

        return minsec;
    }

    //setting up volume
    private void setVol(Integer vol){
        float newVolume = (float) (1-(Math.log(MAX_VOLUME - vol)/Math.log(MAX_VOLUME)));
        mp3Player.mediaPlayer.setVolume(newVolume,newVolume);
    }

    //changing volume based on coordinate change
    //yes, i created the swipe up/down (based on coordinate algorithm) myself
    private void changeVolume(Integer oldY, Integer newY){

        if (newY < oldY) {      //swipe up =volume increase
            curVol = curVol + 10;
            if (curVol >= MAX_VOLUME){
                curVol = MAX_VOLUME;
            }
        }else if (newY > oldY){ //swipe down = volume decrease
            curVol = curVol - 10;
            if (curVol <= MIN_VOLUME){
                curVol = MIN_VOLUME;
            }
        }

        //set the volume only if song is playing or paused
        if ((mp3Player.getState() == MP3Player.MP3PlayerState.PLAYING) || (mp3Player.getState() == MP3Player.MP3PlayerState.PAUSED)){
            setVol(curVol);
        }

        //UI settings:
        if (curVol == 0){
            imgVolume.setImageResource(R.drawable.ic_vol0);
            imgVolume.setColorFilter(new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_IN));
        }else if(curVol>0 && curVol<=40){
            imgVolume.setImageResource(R.drawable.ic_vol1);
            imgVolume.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        }else if(curVol>40 && curVol <=99){
            imgVolume.setImageResource(R.drawable.ic_vol2);
            imgVolume.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        }else {
            imgVolume.setImageResource(R.drawable.ic_vol3);
            imgVolume.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        }
        Toast.makeText(getApplicationContext(),"Volume changed",Toast.LENGTH_SHORT).show();
        txtVolume.setText(String.valueOf(curVol));
    }

    //method to start noti
    private void startNoti(){
        //creating new one, assuming music updated
        notification = new NotificationCompat.Builder(this,"MusicAppv3");
        notification.setAutoCancel(true);

        //setting up noti
        notification.setColor(Color.rgb(40, 94, 18));
        notification.setSmallIcon(R.drawable.music_note);
        notification.setTicker(txtTitle.getText()); //showing current song
        notification.setContentTitle(txtTitle.getText());  //TODO current/progress in noti :c
        notification.setContentText(txtArtist.getText()+"\t\t "+txtCur.getText()+" / "+txtDur.getText());
        notification.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle());
        if (imgPrev != null){
            notification.setLargeIcon(imgPrev);
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
    }

    //method to cancel a noti
    private void stopNoti(){
        //cancel noti
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(NOTI_ID);
    }

    @Override
    //TODO noti - bind service?
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        setEvents();
    }

    //gesture detect
    //https://guides.codepath.com/android/gestures-and-touch-events
    @Override
    public boolean onTouchEvent(MotionEvent event){
        int action = event.getAction();
        switch (action){
            case MotionEvent.ACTION_DOWN:   //capture current xy coordinates
                oldY = (int)event.getY();
                return true;
            case MotionEvent.ACTION_MOVE:   //capture new xy coordinates
                newY = (int)event.getY();
                return true;
            case MotionEvent.ACTION_UP:     //volume up or down based on coordinate change
                Integer distance;           //setting up distance moved
                if ((newY-oldY) <0){
                    distance = (newY-oldY)*-1;
                }else{
                    distance = (newY-oldY);}
                if (distance > 350){changeVolume(oldY,newY);} //changeVolume only if moved > 350dp
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode, Intent data){
        super.onActivityResult(resultCode,resultCode,data);
        isListOpened = false;
        switch (requestCode){
            //case loading a new song from storage
            case CODE_LOAD:
                if ((resultCode == RESULT_OK) && (data != null)){
                    try {
                        //retrieving data from uri (intent)
                        uri = data.getData();
                        stop();
                        mp3Player.loadUri(data.getData(),this);
                        getMeta(data.getData());
                        setVol(curVol);

                        //setting up the UI
                        txtDur.setVisibility(View.VISIBLE);
                        txtCur.setVisibility(View.VISIBLE);
                        imgPlay.setEnabled(false);
                        imgPlay.setVisibility(View.INVISIBLE);
                        imgPause.setEnabled(true);
                        imgPause.setVisibility(View.VISIBLE);
                        imgStop.setEnabled(true);
                        imgVolume.setEnabled(true);
                        seekBar.setMax(mp3Player.getDuration());
                        seekBar.setEnabled(true);
                        txtDur.setText(getTime(mp3Player.getDuration()));

                        //clear previous resource
                        handler = new Handler();
                        playCycle();

                        Toast.makeText(getApplicationContext(),"Playing: "+getFileName(data.getData()),Toast.LENGTH_SHORT).show();
                    }catch (Exception e){
                        Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_SHORT).show();
                        Log.d("Error",e.toString());
                    }
                }else{
                    Toast.makeText(getApplicationContext(),"No music chosen",Toast.LENGTH_SHORT).show();
                }
                break;
            case SELECT_MUSIC:
                //case getting a song from listView in musicList_activity
                if ((resultCode == RESULT_OK) && (data != null)){
                    try {
                        //retrieving the data
                        String filepath = data.getStringExtra("filepath");
                        stop();
                        if (filepath != null){mp3Player.load(filepath);}
                        uri = Uri.fromFile(new File(filepath));
                        getMeta(uri);
                        setVol(curVol);

                        //setting up the UI
                        txtDur.setVisibility(View.VISIBLE);
                        txtCur.setVisibility(View.VISIBLE);
                        imgPlay.setEnabled(false);
                        imgPlay.setVisibility(View.INVISIBLE);
                        imgPause.setEnabled(true);
                        imgPause.setVisibility(View.VISIBLE);
                        imgStop.setEnabled(true);
                        imgVolume.setEnabled(true);
                        seekBar.setMax(mp3Player.getDuration());
                        seekBar.setEnabled(true);
                        txtDur.setText(getTime(mp3Player.getDuration()));

                        //clear previous resource
                        handler = new Handler();
                        playCycle();

                    }catch (Exception e){
                        Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_SHORT).show();
                        Log.d("Error",e.toString());
                    }
                }else if(resultCode == RESULT_CANCELED){
                    Toast.makeText(getApplicationContext(),"No music chosen",Toast.LENGTH_SHORT).show();
                }else if(resultCode == FIN){
                    // do nothing
                }
                break;
            default:
                Toast.makeText(getApplicationContext(),"Error loading music",Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopNoti();
        finish();
    }

    //starts noti when a user pressed Home key
    //also this method handle onBackPressed()
    @Override
    public void onUserLeaveHint(){
        //only starts noti if its playing, if paused: exit means exit
        if (mp3Player.getState() == MP3Player.MP3PlayerState.PLAYING){
            try{
                startNoti();
            }catch (Exception e){
                Log.e("NOTI ERROR",e.toString());
            }
        }
        //dont run noti if MusicList is opened
        if (isListOpened){stopNoti();}
    }

}
