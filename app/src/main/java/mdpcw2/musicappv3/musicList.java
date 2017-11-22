/*
    This Activity holds the playlist available, after scanning musics (.mp3 files) the SD card
 */

package mdpcw2.musicappv3;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.util.ArrayList;

public class musicList extends AppCompatActivity {

    //Line 45-47
    //Static global ID
    private static final int REQ = 1;       //Permission Request ID
    private static final int FIN = 20;      //Finished ID
    private static final int NOTI_ID = 50;  //Notification ID

    //Line 52-62
    //Global variables to be used
    ArrayList<File> songList;                       //Array to hold File
    ArrayList<String> displayList;                  //Array to hold file description
    private ListView listView;
    private ImageView imgPrev;
    private TextView txtTitlePrev, txtArtistPrev;

    NotificationCompat.Builder notification;        //notification builder (to update with service)
    MediaMetadataRetriever metadataRetriever;       //retrieving meta-data from File
    String loc,dur;                                 //loc = location // dur = duration
    byte[] art;                                     //Byte[] data-type to hold Bitmap byte conversion
    Bitmap bmp;                                     //Bitmap variable to hold Album/Song image

    //This method initialise values and items
    private void init(){
        listView = findViewById(R.id.listView);
        imgPrev = findViewById(R.id.imgPrev);
        txtTitlePrev = findViewById(R.id.txtTitlePrev);
        txtArtistPrev = findViewById(R.id.txtArtistPrev);

        txtTitlePrev.setSelected(true);

        loc = null;

        //Line 77-86
        //All 3 IF statement: getting intent data from MainActivity into global variable
        if (getIntent().getByteArrayExtra("imgPrev") != null){
            byte[] byteArray = getIntent().getByteArrayExtra("imgPrev");
            bmp = BitmapFactory.decodeByteArray(byteArray,0,byteArray.length);
            imgPrev.setImageBitmap(bmp);
        }
        if (getIntent().getStringExtra("artistPrev") != null){
            txtArtistPrev.setText(getIntent().getStringExtra("artistPrev"));}
        if (getIntent().getStringExtra("titlePrev") != null){
            txtTitlePrev.setText(getIntent().getStringExtra("titlePrev"));}
            dur = getIntent().getStringExtra("dur");
    }

    //This method set-up the Events Listener for each items created
    private void  setEvents(){
        //Line 93-109
        //Listener if item on ListView is clicked
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d("MusicApp","Item on listView clicked");
                Intent intent = new Intent();
                try{
                    String filepath = songList.get(i).getPath();
                    intent.putExtra("filepath",filepath);
                    Toast.makeText(getApplicationContext(),
                            "Playing: "+filepath.substring(filepath.lastIndexOf("/")+1),Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK,intent);
                    Log.d("MusicApp",filepath.substring(filepath.lastIndexOf("/")+1));
                }catch (Exception e){
                    //case user did not choose any music
                    setResult(Activity.RESULT_CANCELED,intent);
                    Log.d("MusicApp","No music chosen");
                }
                //finishing the current Activity, hence returning to MainActivity
                finish();
            }
        });

        //Line 117-130
        //return to main activity without passing anything if "currently playing" preview clicked
        imgPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {setResult(FIN);finish();
            }
        });
        txtArtistPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {setResult(FIN);finish();}
        });
        txtTitlePrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {setResult(FIN);finish();}
        });
    }

    //This method set up list in the ListView (from Coursework Sheet Instruction)
    private void setList(){
        //Line 135: restricting the scan folder to "Download"
        File musicDir = new File(Environment.getExternalStorageDirectory().getPath()+"/Download/");
        File list[] = musicDir.listFiles();
        if(list != null && list.length>0){
            songList = new ArrayList<>();
            displayList = new ArrayList<>();
            //filter only for .mp3 files
            for (File file : list){
                if (file.getName().toLowerCase().endsWith(".mp3")){
                    //Line 141,142: matching case- add file, at get meta-data
                    songList.add(file);
                    Log.d("MusicApp","Added: "+file.getName());
                    getMeta(Uri.parse(file.toURI().toString()));
                }
            }
        }
        //Displaying music list (displayList) in the ListView
        listView.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,displayList));
    }

    //This method acquire meta-data from the file
    //http://mrbool.com/how-to-extract-meta-data-from-media-file-in-android/28130
    //Sanyam Kalra 2014
    private void getMeta(Uri uri){
        metadataRetriever = new MediaMetadataRetriever();
        try{
            //Line 165-167
            //Getting filepath (filedescriptor) from Uri
            //https://stackoverflow.com/questions/3401579/get-filename-and-path-from-uri-from-mediastore
            //Jun 24 '16 at 15:55
            //YYamil
            ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri,"r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            metadataRetriever.setDataSource(fileDescriptor);

            //Line 171-180
            //Getting description-meta
            String title, artist;
            title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);

            //Condition: meta-data not available, use FileName instead
            if (title != null){
                displayList.add(title+"\n"+artist);
            }else{
                displayList.add(getFileName(uri));
            }

            //Getting image-meta
            art = metadataRetriever.getEmbeddedPicture();
            Bitmap songImage = BitmapFactory.decodeByteArray(art,0,art.length);
            Drawable d = new BitmapDrawable(getResources(),songImage);
            //trying to assign diff respective background album :c
        }catch (Exception e){
            Log.e("MusicApp","No meta \n"+e.toString());
        }
    }

    //Thi method get file name from Uri
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
        //cutting out the file extension
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    //This method updates the notification
    private void startNoti(){
        //creating new one, assuming music updated
        notification = new NotificationCompat.Builder(this,"MusicAppv3");
        notification.setAutoCancel(true);

        //Line 226-237
        //setting up notification
        notification.setColor(Color.rgb(40, 94, 18));
        notification.setSmallIcon(R.drawable.music_note);
        notification.setTicker(txtTitlePrev.getText()); //showing current song
        notification.setContentTitle(txtTitlePrev.getText());
        notification.setContentText(txtArtistPrev.getText()+"\t\t\t\t\t\t\t "+dur);
        notification.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle());
        //set LargeIcon = Album image
        if (imgPrev != null){
            notification.setLargeIcon(bmp);
        }else{
            notification.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.bg_music));
        }

        //Return to Main activity if clicked
        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setContentIntent(pendingIntent);

        //Build
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTI_ID,notification.build());
        Log.d("MusicApp","Notification from MusicList started");
    }

    //This method cancel a notification
    private void stopNoti(){
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancelAll();
        Log.d("MusicApp","Notification from MusicList stopped");
    }

    //onCreate lifecyle
    @Override
    //TODO - scan entire storage
    //TODO 2 - next autoplay
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_list);

        //Line 267-268: Initialising and setting up events for items and variables
        init();
        setEvents();

        //stop a notification if exists
        stopNoti();

        //Line 274-286: Reading storage permission
        if(ContextCompat.checkSelfPermission(musicList.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(musicList.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)){
                ActivityCompat.requestPermissions(musicList.this,
                        new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},REQ);
            }else{
                ActivityCompat.requestPermissions(musicList.this,
                        new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},REQ);
            }
        }else{
            setList();
        }
        Log.d("MusicApp","MusicList onCreate()");
    }

    //onStart lifeCycle
    @Override
    public void onStart(){
        super.onStart();
        Log.d("MusicApp","MusicList onStart()");
    }

    //onResume lifecycle
    @Override
    public void onResume(){
        super.onResume();

        //re-initialized values (update)
        init();
        //stop a notification if exists
        stopNoti();

        Log.d("MusicApp","MusicList onResume()");
    }

    //onPause lifeCycle
    @Override
    public void onPause(){
        super.onPause();
        Log.d("MusicApp","MusicList onPause()");
    }

    //onStop lifeCycle
    @Override
    public void onStop(){
        super.onStop();
        Log.d("MusicApp","MusicList onStop()");
    }

    //onDestroy lifeCycle
    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d("MusicApp","MusicList onDestroy()");
    }

    //Method overriding - during listing and permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,int[] grantResults){
        switch (requestCode){
            case REQ:
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //reading permission from manifest
                    if(ContextCompat.checkSelfPermission(musicList.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(getApplicationContext(),"Permission Granted",Toast.LENGTH_SHORT).show();
                        setList();
                    }
                }else{
                    Toast.makeText(getApplicationContext(),"Not permitted",Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
        }
    }

    //when a user pressed Home key
    //also this method handle onBackPressed()
    @Override
    public void onUserLeaveHint(){
        //Result_cancel: hence no music chosen
        Intent intent = new Intent();
        setResult(Activity.RESULT_CANCELED,intent);
        try{
            stopNoti();
            startNoti();
        }catch (Exception e){
            Log.e("MusicApp",e.toString());
        }
        //finishing current activity and back to MainActivity
        finish();
        Log.d("MusicApp","MusicList onUserLeaveHint");
    }
}
