package com.hajima.vip7009pro.doctiengviet;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import com.hajima.vip7009pro.doctiengviet.R;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private EditText   mEditText;
    private TextToSpeech mTTS;
    private SeekBar mSeekBarPitch;
    private SeekBar mSeekbarSpeed;
    public MediaPlayer mediaPlayer;
    public TextView tvlength;
    public int show =0;

    String speakTextTxt;


    public static boolean hasPermission(Context context, String... permissions )
    {
        if(context != null && permissions != null)
        {
            for(String permission: permissions)
            {
                if(ActivityCompat.checkSelfPermission(context,permission)!= PackageManager.PERMISSION_GRANTED)
                {
                    return false;
                }
            }
        }
        return true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_main);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        mEditText = (EditText) findViewById(R.id.editTextTextPersonName);
        mSeekBarPitch = findViewById(R.id. seekBar);
        mSeekbarSpeed = findViewById(R.id.seekBar2);
        tvlength = findViewById(R.id.textlength);
        ImageButton imageButtonSpeak = findViewById(R.id.ImButtonSpeak);
        ImageButton imageButtonSave = findViewById(R.id.ImButtonSave);
        AdView adView = findViewById(R.id.adView);
        Button mShowMenuBt = findViewById(R.id.btnShowMenu);


        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        mSeekBarPitch.setProgress(50);
        mSeekbarSpeed.setProgress(50);
        int Permission_All =1;
        String[] Permissions  = {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};
        if(!hasPermission(this,Permissions))
        {
            ActivityCompat.requestPermissions(this,Permissions,Permission_All);
        }

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvlength.setText("Độ Dài: "+s.length() +  "/4000");
                if(s.length()>=2)
                {

                }
                else
                {

                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS)
                {
                    int result = mTTS.setLanguage(Locale.getDefault());
                    if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
                    {
                        Log.e("TTS", "Ngôn ngữ không được hõ trợ !");
                    }
                }
                else
                {
                    Log.e("TTS", "Khởi tạo giọng nói thất bại !");
                }
            }
        });





        mShowMenuBt.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(MainActivity.this, Menu2.class);
                MainActivity.this.startActivity(intent);
            }
        });


        imageButtonSpeak.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                docvanban();
            }
        });



        imageButtonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                saveVoice(mEditText.getText().toString());
                show =1;
            }
        });


    }


    private void playVoice(String filepath)
    {

            mediaPlayer = new MediaPlayer();
            try {
                //Toast.makeText(DownloadActivity.this,filepath, Toast.LENGTH_SHORT).show();
                mediaPlayer.setDataSource(filepath);
                mediaPlayer.prepare();
                mediaPlayer.start();

            } catch (IOException e) {
                e.printStackTrace();
            }


    }

    private void saveVoice(String text) {
        String state = Environment.getExternalStorageState();
        boolean mExternalStorageWriteable = false;
        boolean mExternalStorageAvailable = false;
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;

        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Can't read or write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
        File root = Environment.getExternalStorageDirectory();
        File dir = new File(root.getAbsolutePath() + "/Download");
        dir.mkdirs();
        int textlength = text.length();
        String filename ="";
        if(textlength<20)
        {
            filename ="techtospeech";
        }
        else
        {
           filename = text.substring(0,10);
        }
        File file = new File(dir, filename + ".mp3");
        int test = mTTS.synthesizeToFile((CharSequence) text, null, file,
                "tts");
        if(test ==0)
        {
            Toast.makeText(MainActivity.this,"File được lưu vào " + file.getAbsolutePath().toString() , Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(MainActivity.this,"Lưu file thất bại", Toast.LENGTH_SHORT).show();
        }
    }


    private void docvanban(){
        String text = mEditText.getText().toString();
        float pitch = (float) mSeekBarPitch.getProgress()/50;
        if(pitch<0.1) pitch = 0.1f;
        float speed = (float) mSeekbarSpeed.getProgress()/50;
        if(pitch<0.1) speed = 0.1f;
        mTTS.setPitch(pitch);
        mTTS.setSpeechRate(speed);
        mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);

    }

    @Override
    protected void onDestroy() {
        if(mTTS != null)
        {
            mTTS.stop();
            mTTS.shutdown();
        }
        super.onDestroy();

    }
}