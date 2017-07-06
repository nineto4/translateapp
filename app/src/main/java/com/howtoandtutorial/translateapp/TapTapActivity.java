package com.howtoandtutorial.translateapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.translate.Translate;
import com.google.api.services.translate.model.TranslationsListResponse;
import com.howtoandtutorial.translateapp.Common.Common;
import com.howtoandtutorial.translateapp.model.Database;
import com.howtoandtutorial.translateapp.recotest.InkView;
import com.howtoandtutorial.translateapp.recotest.RecognizerService;
import com.howtoandtutorial.translateapp.write.WritePadFlagManager;
import com.howtoandtutorial.translateapp.write.WritePadManager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class TapTapActivity extends AppCompatActivity implements View.OnClickListener{

    //Declare for write to text
    private boolean mRecoInit;
    private InkView inkView;
    public RecognizerService mBoundService;
    private ServiceConnection mConnection;

    //Declare for database and translate
    private String textKey, textResult;
    private Database database;
    private boolean viToEn;
    private TextView tvResult, tvLeftLang, tvRightLang;
    private ImageView imgSwapLang;
    private ImageButton imgTranslateTapTap, imgRemoveTapTap;
    private EditText edtInput;

    @Override
    protected void onResume() {
        if (inkView != null) {
            inkView.cleanView(true);
        }
        WritePadFlagManager.initialize(this);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        unbindService(mConnection);
        super.onDestroy();
        if (mRecoInit) {
            WritePadManager.recoFree();
        }
        mRecoInit = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tap_tap);
        addControl();
        initInkView();
        database = new Database(this);
        controlImgSwapLang();

        imgTranslateTapTap.setOnClickListener(this);
        imgRemoveTapTap.setOnClickListener(this);

    }

    private void initInkView(){
        WritePadManager.setLanguage(null, this);

        // initialize ink inkView class
        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        defaultDisplay.getSize(size);
        mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                mBoundService = ((RecognizerService.RecognizerBinder) service).getService();
                mBoundService.mHandler = inkView.getHandler();
            }

            public void onServiceDisconnected(ComponentName className) {
                mBoundService = null;
            }
        };

        bindService(new Intent(TapTapActivity.this,
                RecognizerService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    private void addControl(){
        edtInput = (EditText)findViewById(R.id.edtInput);
        inkView = (InkView) findViewById(R.id.inkView);
        inkView.setEdtInput(edtInput);

        tvResult = (TextView)findViewById(R.id.tvResult);
        tvLeftLang = (TextView)findViewById(R.id.tvLeftLang);
        tvRightLang = (TextView)findViewById(R.id.tvRightLang);
        imgSwapLang = (ImageView)findViewById(R.id.imgSwapLang);
        imgTranslateTapTap = (ImageButton)findViewById(R.id.imgTranslateTapTap);
        imgRemoveTapTap = (ImageButton)findViewById(R.id.imgRemoveTapTap);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imgTranslateTapTap:
                newTranslateText();
                break;
            case R.id.imgRemoveTapTap:
                inkView.cleanView(true);
                break;
        }
    }

    private void controlImgSwapLang(){
        final Animation animation = AnimationUtils.loadAnimation(this, R.anim.anim_swap_lang);
        imgSwapLang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(animation);
                if(viToEn){
                    viToEn = false;
                    tvLeftLang.setText(getString(R.string.english));
                    tvRightLang.setText(getString(R.string.vietnamese));
                }else{
                    viToEn = true;
                    tvLeftLang.setText(getString(R.string.vietnamese));
                    tvRightLang.setText(getString(R.string.english));
                }
            }
        });
    }

    private void newTranslateText(){
        textKey = edtInput.getText().toString().trim();
        if(textKey.equals("")){
            Toast.makeText(this, "Please enter some text!", Toast.LENGTH_SHORT).show();
        }else if(textKey.length() > 199){
            Toast.makeText(this, "You enter a lot of characters!", Toast.LENGTH_SHORT).show();
        }else {
            if(viToEn){
                (new TapTapActivity.TranslateTask()).execute(textKey, Common.LANGUAGE_VIETNAMESE, Common.LANGUAGE_ENGLISH);
            }else{
                (new TapTapActivity.TranslateTask()).execute(textKey, Common.LANGUAGE_ENGLISH, Common.LANGUAGE_VIETNAMESE);
            }
        }
    }

    private void insertTableHistory(String textKey, String textResult, String lang){
        database.insertTableHistory(textKey, textResult, lang);
    }

    private class TranslateTask extends AsyncTask<String, Void, String>
    {
        @Override
        protected String doInBackground(String... params) {
            Log.d(Common.TAG, "Translating...");
            try {
                // Set up the HTTP transport and JSON factory
                HttpTransport httpTransport = new NetHttpTransport();
                //JsonFactory jsonFactory = JsonFactory.getDefaultInstance();
                JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

                Translate.Builder translateBuilder = new Translate.Builder(httpTransport, jsonFactory, null);
                translateBuilder.setApplicationName(getString(R.string.app_name));

                Translate translate = translateBuilder.build();

                List<String> q = new ArrayList<String>();
                q.add(params[0]);
                Log.d(Common.TAG,"params[0]: "+ params[0] +" params[1]: "+params[1]+" params[2]: "+params[2]);
                Translate.Translations.List list = translate.translations().list(q, params[2]);
                list.setKey(Common.KEY);
                list.setSource(params[1]);
                TranslationsListResponse translateResponse = list.execute();
                String response = translateResponse.getTranslations().get(0).getTranslatedText();

                Log.d(Common.TAG, "Result translated: "+response);
                return response;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e){
                e.printStackTrace();
            }
            //No Internet Connection
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if(result == null){
                Toast.makeText(TapTapActivity.this, "No Internet Connection!", Toast.LENGTH_SHORT).show();
                return;
            }
            tvResult.setText(result);
            textResult = result;
            insertTableHistory(textKey, textResult, viToEn?"vi":"en");
        }
    }

    public interface OnInkViewListener {
        void cleanView(boolean emptyAll);
        Handler getHandler();
    }
}
