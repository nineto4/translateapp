package com.howtoandtutorial.translateapp

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import com.howtoandtutorial.translateapp.Common.Common
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric




/*
 * Created by Dao on 5/27/2017.
 * SplashActivity:
 * The screen slash and checks the permissions of the application
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Fabric.with(this, Crashlytics())
    }

    override fun onStart() {
        super.onStart()
        checkAudioPermissions()
    }

    private fun checkAudioPermissions() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            checkCameraPermissions()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO),
                    Common.REQUEST_RECORD_AUDIO_PERMISSION)
        }
    }

    private fun checkCameraPermissions() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            checkWriteStoragePermissions()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA),
                    Common.REQUEST_CAMERA_PERMISSION)
        }
    }

    private fun checkWriteStoragePermissions() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            checkAlertWindowsPermissions()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    Common.REQUEST_WRITE_PERMISSION)
        }
    }

    private fun checkAlertWindowsPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + packageName))
                startActivityForResult(intent, Common.ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE)
            }else{
                handlerSplash()
            }
        }else{
            handlerSplash()
        }
    }

    //@TargetApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Common.ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    // You don't have permission
                    checkAlertWindowsPermissions()
                } else {
                    handlerSplash()
                }
            }
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == Common.REQUEST_RECORD_AUDIO_PERMISSION) {
            if (permissions.size == 1 && grantResults.size == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkCameraPermissions()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO),
                        Common.REQUEST_RECORD_AUDIO_PERMISSION)
            }
        } else if (requestCode == Common.REQUEST_CAMERA_PERMISSION) {
            if (permissions.size == 1 && grantResults.size == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkWriteStoragePermissions()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA),
                        Common.REQUEST_CAMERA_PERMISSION)
            }
        } else if (requestCode == Common.REQUEST_WRITE_PERMISSION) {
            if (permissions.size == 1 && grantResults.size == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkAlertWindowsPermissions()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        Common.REQUEST_WRITE_PERMISSION)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    //Set timeout to through the MainActivity
    public fun handlerSplash() {
        Handler().postDelayed({
            val loginIntent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(loginIntent)
            finish()
        }, Common.SPLASH_TIME_OUT.toLong())
    }

}