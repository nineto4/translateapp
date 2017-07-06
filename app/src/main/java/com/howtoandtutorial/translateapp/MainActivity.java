package com.howtoandtutorial.translateapp;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.system.ErrnoException;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.translate.Translate;
import com.google.api.services.translate.model.TranslationsListResponse;
import com.howtoandtutorial.translateapp.Common.Common;
import com.howtoandtutorial.translateapp.Common.NetworkUtil;
import com.howtoandtutorial.translateapp.adapter.HistoryAdapter;
import com.howtoandtutorial.translateapp.model.Database;
import com.howtoandtutorial.translateapp.model.History;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.Fabric;

/*
 * Created by Dao on 5/25/2017.
 * MainActivity:
 * - Functions:
 * Translate, speech to text, image to text, camera to text
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener, MessageDialogFragment.Listener{

    //Declare for speech to text
    private SpeechService mSpeechService;
    private VoiceRecorder mVoiceRecorder;
    private int mColorHearing, mColorNotHearing;
    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {
            if (mSpeechService != null) {
                mSpeechService.startRecognizing(mVoiceRecorder.getSampleRate());
            }
        }

        @Override
        public void onVoice(byte[] data, int size) {
            Log.d(Common.TAG,"Hearing..");
            if (mSpeechService != null) {
                mSpeechService.recognize(data, size);
            }
        }

        @Override
        public void onVoiceEnd() {
            if (mSpeechService != null) {
                mSpeechService.finishRecognizing();
            }
        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mSpeechService = SpeechService.from(binder);
            mSpeechService.addListener(mSpeechServiceListener);
            //mStatus.setVisibility(View.VISIBLE);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSpeechService = null;
        }
    };

    //Declare for database and translate
    private String textKey, textResult;
    private Database database;
    private HistoryAdapter historyAdapter;
    private ArrayList<History> mHistory;
    private boolean viToEn, isMicrophoneEnable, isCameraEnable, isFirstClickMicrophone = true,
            isFirstClickCamera = true, isCameraPhotoEnable;
    private TextView tvResult, tvLeftLang, tvRightLang;
    private ImageView imgSwapLang;
    private ImageButton imgCamera, imgTranslate, imgMicrophone, imgCameraPhoto, imgCrop, imgTapTap;
    private Toolbar toolbar;
    private ListView lvHistory;
    private EditText edtInput;
    private SurfaceView surfaceView;
    private CameraSource cameraSource;
    private RelativeLayout.LayoutParams params;
    private LinearLayout lnTop, lnUnderEditText2;

    //Declare for image to text
    private CropImageView mCropImageView;
    private Bitmap cropped;
    private TessOCR mTessOCR;
    private Uri mCropImageUri;
    public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/DemoOCR/";
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Fabric.with(this, new Crashlytics()); //Fabric tracking tool
        addControl();
        checkNetworkConnection();
        database = new Database(this);
        database.createTableHistory();
        toolbar.setTitle(getString(R.string.translate));
        setSupportActionBar(toolbar);
        controlImgSwapLang();
        imageToTextSetup();
        imgTranslate.setOnClickListener(this);
        imgMicrophone.setOnClickListener(this);
        imgCamera.setOnClickListener(this);
        imgCameraPhoto.setOnClickListener(this);
        imgCrop.setOnClickListener(this);
        imgTapTap.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        listHistory();
    }

    @Override
    protected void onStop() {
        // Stop Cloud Speech API
        if(mSpeechService != null){
            // Stop listening to voice
            stopVoiceRecorder();
            mSpeechService.removeListener(mSpeechServiceListener);
            unbindService(mServiceConnection);
            mSpeechService = null;
        }
        startService(new Intent(MainActivity.this, TranslateHeadService.class));
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //stop TranslateHeadService
        stopService(new Intent(MainActivity.this, TranslateHeadService.class));
    }

    private void startVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
        }
        mVoiceRecorder = new VoiceRecorder(mVoiceCallback);
        mVoiceRecorder.start();
    }

    private void stopVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
            mVoiceRecorder = null;
        }
    }

    private final SpeechService.Listener mSpeechServiceListener =
            new SpeechService.Listener() {
                @Override
                public void onSpeechRecognized(final String text, final boolean isFinal) {
                    if (isFinal) {
                        mVoiceRecorder.dismiss();
                    }
                    if (!TextUtils.isEmpty(text)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isFinal) {
                                    edtInput.setText(text);
                                    tvResult.setText(text);
                                    newTranslateText();
                                } else {
                                    edtInput.setText(text);
                                }
                            }
                        });
                    }
                }
            };

    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.imgTranslate:
                newTranslateText();
                break;
            case R.id.imgMicrophone:
                newMicrophone();
                break;
            case R.id.imgCamera:
                //newCamera();
                Toast.makeText(this, "Chức năng đang bảo trì/Maintenance", Toast.LENGTH_LONG).show();
                break;
            case R.id.imgCameraPhoto:
                newCameraPhoto();
                break;
            case R.id.imgCrop:
                if(mCropImageView.getHeight() == 0){
                    Toast.makeText(this, "No image", Toast.LENGTH_LONG).show();
                }else{
                    cropped = mCropImageView.getCroppedImage(500, 500);
                    if (cropped != null)
                        mCropImageView.setImageBitmap(cropped);
                    doOCR(convertColorIntoBlackAndWhiteImage(cropped));
                }

                break;
            case R.id.imgTapTap:
                Intent tapTapActivity = new Intent(MainActivity.this, TapTapActivity.class);
                startActivity(tapTapActivity);
                break;
        }
    }

    //Configure the path
    private void imageToTextSetup(){
        String[] paths = new String[] { DATA_PATH, DATA_PATH + "tessdata/" };
        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.d(Common.TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
                    break;
                } else {
                    Log.d(Common.TAG, "Created directory " + path + " on sdcard");
                }
            }
        }

        if (!(new File(DATA_PATH + "tessdata/" + Common.ENG + ".traineddata")).exists()) {
            try {
                AssetManager assetManager = getAssets();
                InputStream in = assetManager.open(Common.ENG + ".traineddata");
                OutputStream out = new FileOutputStream(DATA_PATH
                        + "tessdata/" + Common.ENG + ".traineddata");
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mTessOCR = new TessOCR();
    }

    //Convert color into black and ưhite image
    private Bitmap convertColorIntoBlackAndWhiteImage(Bitmap orginalBitmap) {
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter colorMatrixFilter = new ColorMatrixColorFilter(
                colorMatrix);
        Bitmap blackAndWhiteBitmap = orginalBitmap.copy(
                Bitmap.Config.ARGB_8888, true);
        Paint paint = new Paint();
        paint.setColorFilter(colorMatrixFilter);
        Canvas canvas = new Canvas(blackAndWhiteBitmap);
        canvas.drawBitmap(blackAndWhiteBitmap, 0, 0, paint);
        return blackAndWhiteBitmap;
    }

    //Image processing converts text then display the result
    public void doOCR(final Bitmap bitmap) {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(this, "Processing",
                    "Please wait...", true);
            // mResult.setVisibility(V.ViewISIBLE);
        }
        else {
            mProgressDialog.show();
        }
        new Thread(new Runnable() {
            public void run() {
                final String result = mTessOCR.getOCRResult(bitmap).toLowerCase();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (result != null && !result.equals("")) {
                            edtInput.setText(result.trim());
                        }
                        mProgressDialog.dismiss();
                    }
                });
            };
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (mCropImageUri != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mCropImageView.setImageUriAsync(mCropImageUri);
        } else {
            Toast.makeText(this, "Required permissions are not granted", Toast.LENGTH_LONG).show();
        }
    }

    //Filtering intent can take image
    public Intent getPickImageChooserIntent() {
        // Determine Uri of camera image to save.
        Uri outputFileUri = getCaptureImageOutputUri();
        List<Intent> allIntents = new ArrayList<>();
        PackageManager packageManager = getPackageManager();
        // collect all camera intents
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            if (outputFileUri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            }
            allIntents.add(intent);
        }
        // collect all gallery intents
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        List<ResolveInfo> listGallery = packageManager.queryIntentActivities(galleryIntent, 0);
        for (ResolveInfo res : listGallery) {
            Intent intent = new Intent(galleryIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            allIntents.add(intent);
        }
        // the main intent is the last in the list
        Intent mainIntent = allIntents.get(allIntents.size() - 1);
        for (Intent intent : allIntents) {
            if (intent.getComponent().getClassName().equals("com.howtoandtutorial.translateapp.ImageToTextActivity")) {
                mainIntent = intent;
                break;
            }
        }
        allIntents.remove(mainIntent);
        // Create a chooser from the main intent
        Intent chooserIntent = Intent.createChooser(mainIntent, "Select image");
        // Add all other intents
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(new Parcelable[allIntents.size()]));
        return chooserIntent;
    }

    private Uri getCaptureImageOutputUri() {
        Uri outputFileUri = null;
        File getImage = getExternalCacheDir();
        if (getImage != null) {
            outputFileUri = Uri.fromFile(new File(getImage.getPath(), "pickImageResult.jpeg"));
        }
        return outputFileUri;
    }

    public Uri getPickImageResultUri(Intent data) {
        boolean isCamera = true;
        if (data != null && data.getData() != null) {
            String action = data.getAction();
            isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
        }
        return isCamera ? getCaptureImageOutputUri() : data.getData();
    }

    public boolean isUriRequiresPermissions(Uri uri) {
        try {
            ContentResolver resolver = getContentResolver();
            InputStream stream = resolver.openInputStream(uri);
            stream.close();
            return false;
        } catch (FileNotFoundException e) {
            if (e.getCause() instanceof ErrnoException) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    //Initialize camera, check permissions, text detection, display results
    private void surfaceView(){
        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!textRecognizer.isOperational()) {
        } else {
            if(isFirstClickCamera){
                cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                        .setFacing(CameraSource.CAMERA_FACING_BACK)//camera sau
                        .setRequestedPreviewSize(1920, 1280)
                        .setRequestedFps(30.0f)
                        .setAutoFocusEnabled(true)
                        .build();
                isFirstClickCamera = false;
            }
            surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {
                    try {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) {
                            cameraSource.start(surfaceView.getHolder());
                        }else{
                            Toast.makeText(getApplicationContext(),"Applications need permission to camera",Toast.LENGTH_LONG).show();
                            return;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                    cameraSource.stop();
                }
            });

            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {
                }

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {

                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if(items.size() != 0){
                        edtInput.post(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder stringBuilder = new StringBuilder();
                                for(int i =0;i<items.size();++i){
                                    TextBlock item = items.valueAt(i);
                                    stringBuilder.append(item.getValue());
                                    stringBuilder.append("\n");
                                }
                                edtInput.setText(stringBuilder.toString());
                            }
                        });
                    }
                }
            });
        }
    }

    private void newTranslateText(){
        textKey = edtInput.getText().toString().trim();
        if(textKey.equals("")){
            Toast.makeText(this, "Please enter some text!", Toast.LENGTH_SHORT).show();
        }else if(textKey.length() > 199){
            Toast.makeText(this, "You enter a lot of characters!", Toast.LENGTH_SHORT).show();
        }else {
            if(viToEn){
                (new TranslateTask()).execute(textKey, Common.LANGUAGE_VIETNAMESE, Common.LANGUAGE_ENGLISH);
            }else{
                (new TranslateTask()).execute(textKey, Common.LANGUAGE_ENGLISH, Common.LANGUAGE_VIETNAMESE);
            }
        }
    }

    private void newMicrophone(){
        if(NetworkUtil.getConnectivityStatusString(this) == NetworkUtil.NETWORK_STATUS_NOT_CONNECTED){
            Toast.makeText(this, "No Internet Connection!", Toast.LENGTH_SHORT).show();
        }else{
            if(isFirstClickMicrophone){
                bindService(new Intent(this, SpeechService.class), mServiceConnection, BIND_AUTO_CREATE);
                isFirstClickMicrophone = false;
            }
            if(isMicrophoneEnable){
                edtInput.setHint(getString(R.string.some_text));
                stopVoiceRecorder();
                edtInput.setHintTextColor(mColorNotHearing);
                imgMicrophone.setImageResource(R.drawable.ic_microphone);
                isMicrophoneEnable = false;
            }else{
                edtInput.setText(null);
                startVoiceRecorder();
                edtInput.setHint(getString(R.string.listening));
                edtInput.setHintTextColor(mColorHearing);
                imgMicrophone.setImageResource(R.drawable.ic_microphone_off);
                isMicrophoneEnable = true;
            }
        }
    }

    private void newCamera(){
        if(isCameraEnable){
            params = (RelativeLayout.LayoutParams) edtInput.getLayoutParams();
            params.height = getResources().getDimensionPixelSize(R.dimen.edt_input_height);
            edtInput.setLayoutParams(params);
            surfaceView.setVisibility(View.GONE);
            tvResult.setVisibility(View.VISIBLE);
            lnTop.setVisibility(View.VISIBLE);
            lnUnderEditText2.setVisibility(View.VISIBLE);
            //cameraSource.stop();
            imgCamera.setImageResource(R.drawable.ic_eye_open);
            isCameraEnable = false;
        }else{
            if(isCameraPhotoEnable){
                Toast.makeText(this, "Disable camera photo first", Toast.LENGTH_SHORT).show();
            }else{
                params = (RelativeLayout.LayoutParams) edtInput.getLayoutParams();
                params.height = getResources().getDimensionPixelSize(R.dimen.edt_input_height_2);
                edtInput.setLayoutParams(params);
                surfaceView.setVisibility(View.VISIBLE);
                tvResult.setVisibility(View.GONE);
                lnTop.setVisibility(View.GONE);
                lnUnderEditText2.setVisibility(View.GONE);
                surfaceView();
                imgCamera.setImageResource(R.drawable.ic_eye_close);
                edtInput.setText(null);
                isCameraEnable = true;
            }
        }
    }

    private void newCameraPhoto(){
        if(isCameraPhotoEnable){
            mCropImageView.setImageResource(android.R.color.transparent);
            params = (RelativeLayout.LayoutParams) edtInput.getLayoutParams();
            params.height = getResources().getDimensionPixelSize(R.dimen.edt_input_height);
            edtInput.setLayoutParams(params);
            mCropImageView.setVisibility(View.GONE);
            imgCrop.setVisibility(View.GONE);
            tvResult.setVisibility(View.VISIBLE);
            lnTop.setVisibility(View.VISIBLE);
            lnUnderEditText2.setVisibility(View.VISIBLE);
            isCameraPhotoEnable = false;
        }else{
            if(isCameraEnable){
                Toast.makeText(this, "Disable camera first", Toast.LENGTH_SHORT).show();
            }else{
                params = (RelativeLayout.LayoutParams) edtInput.getLayoutParams();
                params.height = getResources().getDimensionPixelSize(R.dimen.edt_input_height_2);
                edtInput.setLayoutParams(params);
                mCropImageView.setVisibility(View.VISIBLE);
                imgCrop.setVisibility(View.VISIBLE);
                tvResult.setVisibility(View.GONE);
                lnTop.setVisibility(View.GONE);
                lnUnderEditText2.setVisibility(View.GONE);
                startActivityForResult(getPickImageChooserIntent(), 200);
                edtInput.setText(null);
                isCameraPhotoEnable = true;
            }
        }
    }

    private void checkNetworkConnection(){
        if(NetworkUtil.getConnectivityStatusString(this) == NetworkUtil.NETWORK_STATUS_NOT_CONNECTED){
            showPermissionMessageDialog();
        }
    }

    //Display message dialog
    private void showPermissionMessageDialog() {
        MessageDialogFragment
                .newInstance(getString(R.string.network_connection_request))
                .show(getSupportFragmentManager(), Common.FRAGMENT_MESSAGE_DIALOG);
    }

    @Override
    public void onMessageDialogDismissed() {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifi.setWifiEnabled(true);
    }

    private void addControl(){
        final Resources resources = getResources();
        final Resources.Theme theme = getTheme();
        mColorHearing = ResourcesCompat.getColor(resources, R.color.status_hearing, theme);
        mColorNotHearing = ResourcesCompat.getColor(resources, R.color.status_not_hearing, theme);

        mCropImageView = (CropImageView) findViewById(R.id.cropImageView);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        lnTop = (LinearLayout) findViewById(R.id.lnTop);
        lnUnderEditText2 = (LinearLayout) findViewById(R.id.lnUnderEditText2);
        tvResult = (TextView)findViewById(R.id.tvResult);
        tvLeftLang = (TextView)findViewById(R.id.tvLeftLang);
        tvRightLang = (TextView)findViewById(R.id.tvRightLang);
        imgSwapLang = (ImageView)findViewById(R.id.imgSwapLang);
        imgCamera = (ImageButton) findViewById(R.id.imgCamera);
        imgTranslate = (ImageButton)findViewById(R.id.imgTranslate);
        imgCameraPhoto = (ImageButton)findViewById(R.id.imgCameraPhoto);
        imgMicrophone = (ImageButton)findViewById(R.id.imgMicrophone);
        imgTapTap = (ImageButton)findViewById(R.id.imgTapTap);
        imgCrop = (ImageButton)findViewById(R.id.imgCrop);
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        lvHistory = (ListView)findViewById(R.id.lvHistory);
        edtInput = (EditText)findViewById(R.id.edtInput);
        mHistory = new ArrayList<>();
        historyAdapter = new HistoryAdapter(this, mHistory);
        lvHistory.setAdapter(historyAdapter);
    }

    private void insertTableHistory(String textKey, String textResult, String lang){
        database.insertTableHistory(textKey, textResult, lang);
        listHistory();
    }

    public void deleteTableHistory(int id){
        database.deleteTableHistory(id);
        listHistory();
    }

    //Load history list on listview
    private void listHistory(){
        final String sql = "SELECT * FROM History ORDER BY Id DESC LIMIT 20";
        Cursor cursorRows = database.getData(sql);
        int id;
        String textKey, textResult, lang;
        mHistory.clear();
        while (cursorRows.moveToNext()){
            id = cursorRows.getInt(0);
            textKey = cursorRows.getString(1);
            textResult = cursorRows.getString(2);
            lang = cursorRows.getString(3);
            mHistory.add(new History(id, textKey, textResult, lang));
        }
        historyAdapter.notifyDataSetChanged();
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
                Toast.makeText(MainActivity.this, "No Internet Connection!", Toast.LENGTH_SHORT).show();
                return;
            }
            tvResult.setText(result);
            textResult = result;
            insertTableHistory(textKey, textResult, viToEn?"vi":"en");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(Common.TAG,"1");
        if (resultCode == Activity.RESULT_OK) {
            Log.d(Common.TAG,"2");
            Uri imageUri = getPickImageResultUri(data);
            Log.d(Common.TAG,"imageUri: "+imageUri.toString());
            // For API >= 23 we need to check specifically that we have permissions to read external storage,
            // but we don't know if we need to for the URI so the simplest is to try open the stream and see if we get error.
            boolean requirePermissions = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                    isUriRequiresPermissions(imageUri)) {
                Log.d(Common.TAG,"3");
                // request permissions and handle the result in onRequestPermissionsResult()
                requirePermissions = true;
                mCropImageUri = imageUri;
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }
            if (!requirePermissions) {
                mCropImageView.setImageUriAsync(imageUri);
            }
        }
    }
}