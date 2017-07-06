package com.howtoandtutorial.translateapp;

import android.graphics.Bitmap;
import android.os.Environment;
import com.googlecode.tesseract.android.TessBaseAPI;
import java.io.File;

/*
 * Created by Dao on 5/22/2017.
 * TessOCR:
 * Image processing to text
 */
public class TessOCR {
    private TessBaseAPI mTess;

    public TessOCR() {
        mTess = new TessBaseAPI();
       // AssetManager assetManager=
        String datapath = Environment.getExternalStorageDirectory() + "/DemoOCR/";
        String language = "eng";
       // AssetManager assetManager = getAssets();
        File dir = new File(datapath + "tessdata/");
        if (!dir.exists())
            dir.mkdirs();
        mTess.init(datapath, language);
    }

    public String getOCRResult(Bitmap bitmap) {

        mTess.setImage(bitmap);
        String result = mTess.getUTF8Text();

        return result;
}

    public void onDestroy() {
        if (mTess != null)
            mTess.end();
    }

}
