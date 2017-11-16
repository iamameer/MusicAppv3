package mdpcw2.musicappv3;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

    private static final int REQ = 1;
    private static final int FIN = 20;
    ArrayList<File> songList;
    ArrayList<String> displayList;
    private ListView listView;
    private ImageView imgPrev;
    private TextView txtTitlePrev, txtArtistPrev;

    MediaMetadataRetriever metadataRetriever;
    String loc;
    byte[] art;
    Bitmap bmp;

    //initialising values and items
    private void init(){
        listView = findViewById(R.id.listView);
        imgPrev = findViewById(R.id.imgPrev);
        txtTitlePrev = findViewById(R.id.txtTitlePrev);
        txtArtistPrev = findViewById(R.id.txtArtistPrev);

        txtTitlePrev.setSelected(true);

        loc = null;

        //getting intent data from MainActivity into global variable
        if (getIntent().getByteArrayExtra("imgPrev") != null){
            byte[] byteArray = getIntent().getByteArrayExtra("imgPrev");
            bmp = BitmapFactory.decodeByteArray(byteArray,0,byteArray.length);
            imgPrev.setImageBitmap(bmp);
        }
        if (getIntent().getStringExtra("artistPrev") != null){
            txtArtistPrev.setText(getIntent().getStringExtra("artistPrev"));}
        if (getIntent().getStringExtra("titlePrev") != null){
            txtTitlePrev.setText(getIntent().getStringExtra("titlePrev"));}
    }

    //sending result once item is selected
    private void  setEvents(){
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent();
                try{
                    String filepath = songList.get(i).getPath();
                    intent.putExtra("filepath",filepath);
                    Toast.makeText(getApplicationContext(),
                            "Playing: "+filepath.substring(filepath.lastIndexOf("/")+1),Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK,intent);
                }catch (Exception e){
                    //case user did not choose any music
                    setResult(Activity.RESULT_CANCELED,intent);
                }
                finish();
            }
        });

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

    //setting up list (from Coursework Sheet Instruction)
    private void setList(){
        File musicDir = new File(Environment.getExternalStorageDirectory().getPath()+"/Download/");
        File list[] = musicDir.listFiles();
        if(list != null && list.length>0){
            songList = new ArrayList<>();
            displayList = new ArrayList<>();
            //filter
            for (File file : list){
                if (file.getName().toLowerCase().endsWith(".mp3")){
                    songList.add(file);
                    getMeta(Uri.parse(file.toURI().toString()));
                }
            }
        }
        listView.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,displayList));
    }

    //acquiring meta-data
    //http://mrbool.com/how-to-extract-meta-data-from-media-file-in-android/28130
    //Sanyam Kalra 2014
    private void getMeta(Uri uri){
        metadataRetriever = new MediaMetadataRetriever();
        try{
            //getting filepath (filedescriptor) from Uri
            //https://stackoverflow.com/questions/3401579/get-filename-and-path-from-uri-from-mediastore
            //Jun 24 '16 at 15:55
            //YYamil
            ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri,"r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            metadataRetriever.setDataSource(fileDescriptor);

            //getting description-meta
            String title, artist;
            title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);

            if (title != null){
                displayList.add(title+"\n"+artist);
            }else{
                displayList.add(getFileName(uri));
            }

            //getting image-meta
            art = metadataRetriever.getEmbeddedPicture();
            Bitmap songImage = BitmapFactory.decodeByteArray(art,0,art.length);
            Drawable d = new BitmapDrawable(getResources(),songImage);
            //trying to assign diff respective background album :c
        }catch (Exception e){
            //do nothing
        }
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

    @Override
    //TODO - scan entire storage
    // TODO 2 - next autoplay
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_list);

        init();
        setEvents();

        //reading storage permission
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,int[] grantResults){
        switch (requestCode){
            case REQ:
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
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

}
