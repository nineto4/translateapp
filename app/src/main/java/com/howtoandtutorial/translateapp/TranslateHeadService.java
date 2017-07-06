package com.howtoandtutorial.translateapp;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/*
 * Created by Dao on 5/31/2017.
 * TranslateHeadService:
 * Create TranslateHead service run foreground to translate text
 */

public class TranslateHeadService extends Service {

    private EditText edtInputDialog;
    private Button btnTranslateDialog, btnClearTextService;
    private RelativeLayout input_relativeLayout_service, inputServiceView;
    private Switch switchLanguage;
    private TextView tvResultDialog;
    private String textKey;
    private Database database;

    private WindowManager windowManager;
    private RelativeLayout translateHeadView, removeView;
    private ImageView translateHeadImg, imgRemoveTranslateHead;
    private int x_init_cord, y_init_cord, x_init_margin, y_init_margin, x_cord_remove, y_cord_remove;
    private Point szWindow = new Point();
    private boolean isLeft = true, isClickTranslateHead, isInputServiceView, viToEn;
    private double value;

    @SuppressWarnings("deprecation")

    @Override
    public void onCreate() {
        super.onCreate();
        database = new Database(getApplicationContext());
        Log.d(Common.TAG, "TranslateHeadService.onCreate()");
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void handleStart(){
        //Set for service running separately with activity
        startTranslateInForeground();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        removeView = (RelativeLayout)inflater.inflate(R.layout.layout_remove, null);
        WindowManager.LayoutParams paramRemove = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        paramRemove.gravity = Gravity.TOP | Gravity.LEFT;
        imgRemoveTranslateHead = (ImageView)removeView.findViewById(R.id.imgRemoveTranslateHead);
        windowManager.addView(removeView, paramRemove);
        removeView.setVisibility(View.GONE);

        translateHeadView = (RelativeLayout) inflater.inflate(R.layout.translate_head, null);
        translateHeadImg = (ImageView)translateHeadView.findViewById(R.id.imgTranslateHead);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            windowManager.getDefaultDisplay().getSize(szWindow);
        } else {
            int w = windowManager.getDefaultDisplay().getWidth();
            int h = windowManager.getDefaultDisplay().getHeight();
            szWindow.set(w, h);
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;//vi tri ban dau
        params.y = 10;
        windowManager.addView(translateHeadView, params);

        translateHeadView.setOnTouchListener(new View.OnTouchListener() {
            long time_start = 0, time_end = 0;
            boolean isLongclick = false, inBounded = false;
            int remove_img_width = 0, remove_img_height = 0;

            Handler handler_longClick = new Handler();
            Runnable runnable_longClick = new Runnable() {
                @Override
                public void run() {
                    Log.d(Common.TAG, "Into runnable_longClick");
                    isLongclick = true;
                    removeView.setVisibility(View.VISIBLE);
                    translateHeadLongClick();
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) translateHeadView.getLayoutParams();

                int x_cord = (int) event.getRawX();
                int y_cord = (int) event.getRawY();
                int x_cord_Destination, y_cord_Destination;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN://nhan vao TranslateHead 0.2s
                        time_start = System.currentTimeMillis();
                        handler_longClick.postDelayed(runnable_longClick, 300);//goi Runable de xuat hien Remove Icon

                        remove_img_width = imgRemoveTranslateHead.getLayoutParams().width;
                        remove_img_height = imgRemoveTranslateHead.getLayoutParams().height;

                        x_init_cord = x_cord;
                        y_init_cord = y_cord;

                        x_init_margin = layoutParams.x;
                        y_init_margin = layoutParams.y;

                        if(inputServiceView != null){//An tin nhan
                            inputServiceView.setVisibility(View.GONE);
                        }
                        break;
                    case MotionEvent.ACTION_MOVE://di chuyen TranslateHead
                        int x_diff_move = x_cord - x_init_cord;
                        int y_diff_move = y_cord - y_init_cord;

                        x_cord_Destination = x_init_margin + x_diff_move;
                        y_cord_Destination = y_init_margin + y_diff_move;

                        if(isLongclick){//dang nhan vao Translate
                            int x_bound_left = szWindow.x / 2 - (int)(remove_img_width * 1.5);//Vung anh huong khi TranslateHead di vao
                            int x_bound_right = szWindow.x / 2 +  (int)(remove_img_width * 1.5);
                            int y_bound_top = szWindow.y - (int)(remove_img_height * 1.9);

                            if((x_cord >= x_bound_left && x_cord <= x_bound_right) && y_cord >= y_bound_top){//neu TranslateHead di vao vung anh huong cua Remove Icon
                                inBounded = true;
                                x_cord_remove = (int) ((szWindow.x - (remove_img_height * 1.2)) / 2);
                                y_cord_remove = (int) (szWindow.y - ((remove_img_width * 1.7) + getStatusBarHeight() ));

                                if(imgRemoveTranslateHead.getLayoutParams().height == remove_img_height){
                                    imgRemoveTranslateHead.getLayoutParams().height = (int) (remove_img_height * 1.2);//Phong to remove icon khi TranslateHead di vao
                                    imgRemoveTranslateHead.getLayoutParams().width = (int) (remove_img_width * 1.2);

                                    WindowManager.LayoutParams param_remove = (WindowManager.LayoutParams) removeView.getLayoutParams();
                                    param_remove.x = x_cord_remove;//vi tri cua Remove Icon khi TranslateHead di vao
                                    param_remove.y = y_cord_remove;
                                    windowManager.updateViewLayout(removeView, param_remove);
                                }
                                layoutParams.x = x_cord_remove + (Math.abs(removeView.getWidth() - translateHeadView.getWidth())) / 2;
                                layoutParams.y = y_cord_remove + (Math.abs(removeView.getHeight() - translateHeadView.getHeight())) / 2 ;

                                windowManager.updateViewLayout(translateHeadView, layoutParams);
                                break;
                            }else{//Neu TranslateHead o ngoai vung anh huong cua Remove Icon
                                inBounded = false;
                                imgRemoveTranslateHead.getLayoutParams().height = remove_img_height;
                                imgRemoveTranslateHead.getLayoutParams().width = remove_img_width;

                                WindowManager.LayoutParams param_remove = (WindowManager.LayoutParams) removeView.getLayoutParams();
                                x_cord_remove = (szWindow.x - removeView.getWidth()) / 2;
                                //int y_cord_remove = szWindow.y - (removeView.getHeight() + getStatusBarHeight() );
                                y_cord_remove = (int) (szWindow.y - ((remove_img_width * 1.3) + getStatusBarHeight() ));

                                param_remove.x = x_cord_remove;
                                param_remove.y = y_cord_remove;

                                windowManager.updateViewLayout(removeView, param_remove);
                            }
                        }

                        layoutParams.x = x_cord_Destination;
                        layoutParams.y = y_cord_Destination;

                        windowManager.updateViewLayout(translateHeadView, layoutParams);
                        break;
                    case MotionEvent.ACTION_UP://bo cham TranslateHead
                        isLongclick = false;
                        removeView.setVisibility(View.GONE);
                        imgRemoveTranslateHead.getLayoutParams().height = remove_img_height;
                        imgRemoveTranslateHead.getLayoutParams().width = remove_img_width;
                        handler_longClick.removeCallbacks(runnable_longClick);

                        if(inBounded){//Huy service khi translatehead cham vao remove icon
                            stopService(new Intent(TranslateHeadService.this, TranslateHeadService.class));
                            inBounded = false;
                            break;
                        }

                        int x_diff = x_cord - x_init_cord;
                        int y_diff = y_cord - y_init_cord;
                        if(Math.abs(x_diff) < 30 && Math.abs(y_diff) < 30){//khi click vao TranslateHead ma no k di chuyen qua 30 don vi
                            time_end = System.currentTimeMillis();
                            if((time_end - time_start) < 150){//Thoi gian press TranslateHead (ke tu luc ACTION_DOWN den hien tai
                                translateHeadClick();
                                isClickTranslateHead = true;
                                Log.d(Common.TAG, "translateHeadClick()");
                            }
                        }

                        y_cord_Destination = y_init_margin + y_diff;

                        int BarHeight =  getStatusBarHeight();
                        if (y_cord_Destination < 0) {
                            y_cord_Destination = 0;
                        } else if (y_cord_Destination + (translateHeadView.getHeight() + BarHeight) > szWindow.y) {
                            y_cord_Destination = szWindow.y - (translateHeadView.getHeight() + BarHeight );
                        }
                        layoutParams.y = y_cord_Destination;

                        inBounded = false;
                        if(!isClickTranslateHead){
                            resetPosition(x_cord);
                        }else{
                            isClickTranslateHead = false;
                        }

                        Log.d(Common.TAG, "TranslateHeadView.setOnTouchListener  -> event.getAction() : ACTION_UP");
                        break;
                    default:
                        //Touch outside of TranslateHead
                        Log.d(Common.TAG, "TranslateHeadView.setOnTouchListener  -> event.getAction() : default");
                        break;
                }
                return true;
            }
        });


        //Configured for Input Layout
        inputServiceView = (RelativeLayout)inflater.inflate(R.layout.layout_input_on_service, null);
        edtInputDialog = (EditText) inputServiceView.findViewById(R.id.edtInputDialog);
        btnTranslateDialog = (Button) inputServiceView.findViewById(R.id.btnTranslateDialog);
        switchLanguage = (Switch) inputServiceView.findViewById(R.id.switchLanguage);
        tvResultDialog = (TextView) inputServiceView.findViewById(R.id.tvResultDialog);
        btnClearTextService = (Button) inputServiceView.findViewById(R.id.btnClearTextService);
        input_relativeLayout_service = (RelativeLayout)inputServiceView.findViewById(R.id.input_relativeLayout_service);

        WindowManager.LayoutParams paramsTxt = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        paramsTxt.gravity = Gravity.TOP | Gravity.LEFT;

        inputServiceView.setVisibility(View.GONE);
        windowManager.addView(inputServiceView, paramsTxt);
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if(windowManager == null)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            windowManager.getDefaultDisplay().getSize(szWindow);
        } else {
            int w = windowManager.getDefaultDisplay().getWidth();
            int h = windowManager.getDefaultDisplay().getHeight();
            szWindow.set(w, h);
        }

        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) translateHeadView.getLayoutParams();

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(Common.TAG, "TranslateHeadService.onConfigurationChanged -> Xoay ngang");

            if(inputServiceView != null){
                inputServiceView.setVisibility(View.GONE);
            }

            if(layoutParams.y + (translateHeadView.getHeight() + getStatusBarHeight()) > szWindow.y){
                layoutParams.y = szWindow.y- (translateHeadView.getHeight() + getStatusBarHeight());
                windowManager.updateViewLayout(translateHeadView, layoutParams);
            }

            if(layoutParams.x != 0 && layoutParams.x < szWindow.x){
                resetPosition(szWindow.x);
            }
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Log.d(Common.TAG, "TranslateHeadService.onConfigurationChanged -> Xoay doc");

            if(inputServiceView != null){
                inputServiceView.setVisibility(View.GONE);
            }

            if(layoutParams.x > szWindow.x){
                resetPosition(szWindow.x);
            }
        }
    }

    //tra vi tri TranslateHead sang ben trai hoac phai
    private void resetPosition(int x_cord_now) {
        if(x_cord_now <= szWindow.x / 2){
            isLeft = true;
            moveToLeft(x_cord_now);
        } else {
            isLeft = false;
            moveToRight(x_cord_now);
        }
    }

    //Dua TranslateHead sang sat ben Trai man hinh
    private void moveToLeft(final int x_cord_now){
        final int x = szWindow.x - x_cord_now;

        new CountDownTimer(500, 5) {
            WindowManager.LayoutParams mParams = (WindowManager.LayoutParams) translateHeadView.getLayoutParams();
            long step;
            public void onTick(long t) {
                step = (500 - t)/5;
                mParams.x = 0 - (int)(double)bounceValue(step, x );
                windowManager.updateViewLayout(translateHeadView, mParams);
            }
            public void onFinish() {
                mParams.x = 0;
                windowManager.updateViewLayout(translateHeadView, mParams);
            }
        }.start();
    }

    //Dua TranslateHead sang sat ben Phai man hinh
    private  void moveToRight(final int x_cord_now){
        new CountDownTimer(500, 5) {
            WindowManager.LayoutParams mParams = (WindowManager.LayoutParams) translateHeadView.getLayoutParams();
            long step;
            public void onTick(long t) {
                step = (500 - t)/5;
                mParams.x = szWindow.x + (int)(double)bounceValue(step, x_cord_now) - translateHeadView.getWidth();
                windowManager.updateViewLayout(translateHeadView, mParams);
            }
            public void onFinish() {
                mParams.x = szWindow.x - translateHeadView.getWidth();
                windowManager.updateViewLayout(translateHeadView, mParams);
            }
        }.start();
    }

    //Thuat toan Bounce (google de biet them)
    private double bounceValue(long step, long scale){
        value = scale * java.lang.Math.exp(-0.055 * step) * java.lang.Math.cos(0.08 * step);
        return value;
    }

    private int getStatusBarHeight() {
        return (int) Math.ceil(25 * getApplicationContext().getResources().getDisplayMetrics().density);
    }

    private void translateHeadLongClick(){
        Log.d(Common.TAG, "Into TranslateHeadService translateHeadLongClick() ");

        WindowManager.LayoutParams param_remove = (WindowManager.LayoutParams) removeView.getLayoutParams();
        x_cord_remove = (szWindow.x - removeView.getWidth()) / 2;
        y_cord_remove = szWindow.y - (removeView.getHeight() + getStatusBarHeight() );

        //set animation for remove icon
        param_remove.windowAnimations = android.R.style.Animation_Translucent;
        final Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_in);
        animation.setDuration(400);
        removeView.startAnimation(animation);

        param_remove.x = x_cord_remove;
        param_remove.y = y_cord_remove;

        windowManager.updateViewLayout(removeView, param_remove);
    }

    private void translateHeadClick(){
        if(isInputServiceView){//An tin nhan
            inputServiceView.setVisibility(View.GONE);
            tvResultDialog.setVisibility(View.GONE);
            edtInputDialog.setText(null);
            isInputServiceView = false;
        }else{
            showEditText();
            isInputServiceView = true;
        }
    }

    private void showEditText(){
        if(inputServiceView != null && translateHeadView != null ){
            Log.d(Common.TAG, "showEditText ");

            WindowManager.LayoutParams param_translateHead = (WindowManager.LayoutParams) translateHeadView.getLayoutParams();
            WindowManager.LayoutParams param_input = (WindowManager.LayoutParams) inputServiceView.getLayoutParams();

            input_relativeLayout_service.getLayoutParams().height = szWindow.y/2 - 90;
            input_relativeLayout_service.getLayoutParams().width = szWindow.x/2 + 30;
            param_input.x = 68;//set lai vi tri EditText
            param_input.y = 8;
            input_relativeLayout_service.setGravity(Gravity.LEFT | Gravity.TOP);

            param_translateHead.gravity = Gravity.TOP | Gravity.LEFT;
            param_translateHead.x = 0;//set lai vi tri TranslateHead khi click
            param_translateHead.y = 10;

            inputServiceView.setVisibility(View.VISIBLE);
            windowManager.updateViewLayout(inputServiceView, param_input);
            windowManager.updateViewLayout(translateHeadView, param_translateHead);

            btnTranslateDialog.getBackground().setAlpha(130);
            btnTranslateDialog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    newTranslateText();
                }
            });
            switchLanguage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(switchLanguage.isChecked()){
                        switchLanguage.setText(getString(R.string.vie));
                        edtInputDialog.setHint(getString(R.string.some_text_vi));
                    }
                    else{
                        switchLanguage.setText(getString(R.string.eng));
                        edtInputDialog.setHint(getString(R.string.some_text));
                    }
                }
            });

            edtInputDialog.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if("".equals(edtInputDialog.getText().toString())){
                        btnClearTextService.setVisibility(View.GONE);
                    }else{
                        btnClearTextService.setVisibility(View.VISIBLE);
                    }
                }
            });
            btnClearTextService.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    edtInputDialog.setText(null);
                }
            });
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Common.TAG, "TranslateHeadService.onStartCommand() -> startId=" + startId);
        if(startId == Service.START_STICKY) {
            handleStart();
            return super.onStartCommand(intent, flags, startId);
        }else{
            return  Service.START_NOT_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Common.TAG, "onDestroy()");
        if(translateHeadView != null){
            windowManager.removeView(translateHeadView);
        }

        if(inputServiceView != null){
            windowManager.removeView(inputServiceView);
        }

        if(removeView != null){
            windowManager.removeView(removeView);
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(Common.TAG, "TranslateHeadService.onBind()");
        return null;
    }

    private void startTranslateInForeground(){
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Translate App")
                .setContentText("Do you want to translate something..?")
                .setContentIntent(pendingIntent).build();
        startForeground(1337, notification);
    }

    private void newTranslateText(){
        textKey = edtInputDialog.getText().toString().trim();
        viToEn = switchLanguage.isChecked();
        if(textKey.equals("")){
            Toast.makeText(this, "Please enter some text!", Toast.LENGTH_SHORT).show();
        }else if(textKey.length() > 199){
            Toast.makeText(this, "You enter a lot of characters!", Toast.LENGTH_SHORT).show();
        }else {
            if(viToEn){
                (new TranslateHeadService.TranslateTask()).execute(textKey, Common.LANGUAGE_VIETNAMESE, Common.LANGUAGE_ENGLISH);
            }else{
                (new TranslateHeadService.TranslateTask()).execute(textKey, Common.LANGUAGE_ENGLISH, Common.LANGUAGE_VIETNAMESE);
            }
        }
    }

    private class TranslateTask extends AsyncTask<String, Void, String>
    {
        @Override
        protected String doInBackground(String... params) {
            Log.d(Common.TAG, "Service Translating...");
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

                Log.d(Common.TAG, response);
                String result = response;
                return result;
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
                Toast.makeText(getApplicationContext(), "No Internet Connection!", Toast.LENGTH_SHORT).show();
                return;
            }
            tvResultDialog.setVisibility(View.VISIBLE);
            tvResultDialog.setText(result);
            database.insertTableHistory(textKey, result, viToEn?"vi":"en");
        }
    }
}
