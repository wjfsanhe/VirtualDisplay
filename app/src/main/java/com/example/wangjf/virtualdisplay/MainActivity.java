package com.example.wangjf.virtualdisplay;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements DisplayManager.DisplayListener {
    private static final int SCREEN_CAPTURE_PERMISSION_CODE = 1;
    private final String TAG = "WJF_VIRTUAL_DISPLAY";
    private TextView mTextMessage;
    private SurfaceView mSurfaceView;
    private DisplayManager mDisplayManager;
    private DisplayMetrics mDisplayMetrics = new DisplayMetrics();
    private WindowManager mWindowManager;
    private int mVirtualDisplayID;
    private VirtualDisplay mVirtualDisplay;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard);
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }
    };
    private MediaProjectionManager mProjectionManager;
    private int mResultCode;
    private Intent mResultData;
    private MediaProjection.Callback mProjectionCallback;
    private MediaProjection mProjection;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScreenCapture();
        Log.d(TAG, "onDestroy");
        mDisplayManager.unregisterDisplayListener(this);
        if (mProjection != null) {
            Log.i(TAG, "Stop media projection");
            mProjection.unregisterCallback(mProjectionCallback);
            mProjection.stop();
            mProjection = null;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initService();
        mTextMessage = (TextView) findViewById(R.id.message);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        startScreenCapture();

    }
    private void createVirtualDisplay() {
        if (mProjection != null && mVirtualDisplay == null) {
            Log.d(TAG, "createVirtualDisplay WxH (px): " + mDisplayMetrics.widthPixels + "x" + mDisplayMetrics.heightPixels +
                    ", dpi: " + mDisplayMetrics.densityDpi);
            int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;
            //int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY;
            //mVirtualDisplay = mProjection.createVirtualDisplay("MyVirtualDisplay",
            //         mWidth, mHeight, mMetrics.densityDpi, flags, mMediaRecorder.getSurface(),
            //         null /*Callbacks*/, null /*Handler*/);
            mVirtualDisplay = mProjection.createVirtualDisplay("iqiyidisplay",
                    mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels, mDisplayMetrics.densityDpi, flags, mSurfaceView.getHolder().getSurface(),
                    null /*Callbacks*/, null /*Handler*/);
        }
    }
    private void startScreenCapture() {
        if (mProjection != null) {
            // start virtual display
            Log.i(TAG, "The media projection is already gotten");
            createVirtualDisplay();
        } else if (mResultCode != 0 && mResultData != null) {
            // get media projection
            Log.i(TAG, "Get media projection with the existing permission");
            mProjection = getProjection();
            createVirtualDisplay();
        } else {
            Log.i(TAG, "Request the permission for media projection");
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), SCREEN_CAPTURE_PERMISSION_CODE);
        }
    }
    private void stopScreenCapture() {
        destroyVirtualDisplay();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mResultCode = resultCode;
        mResultData = data;
        if (requestCode != SCREEN_CAPTURE_PERMISSION_CODE) {
            Toast.makeText(this, "Unknown request code: " + requestCode, Toast.LENGTH_SHORT).show();
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i(TAG, "Get media projection with the new permission");
        mProjection = getProjection();
        createVirtualDisplay();
        dumpCurrentDisplays();
    }
    private void initService() {
        mDisplayManager = (DisplayManager) getSystemService("display");
        mWindowManager = (WindowManager) getSystemService("window");
        mWindowManager.getDefaultDisplay().getMetrics(mDisplayMetrics);
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mDisplayManager.registerDisplayListener(this, null);
    }
    private MediaProjection getProjection() {
        MediaProjection projection = mProjectionManager.getMediaProjection(mResultCode, mResultData);
        // Add a callback to be informed if the projection
        // will be stopped from the status bar.
        mProjectionCallback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                Log.d(TAG, "MediaProjection.Callback onStop obj:" + toString());
                destroyVirtualDisplay();
                mProjection = null;
            }
        };
        projection.registerCallback(mProjectionCallback, null);
        return projection;
    }
    private void dumpCurrentDisplays() {
        for(Display display : mDisplayManager.getDisplays()) {
            Log.d(TAG,"display[" + display.getDisplayId() + "] = " + display.getName());
        }
    }

    private void destroyVirtualDisplay() {
        Log.d(TAG, "destroyVirtualDisplay");
        if (mVirtualDisplay != null) {
            Log.d(TAG, "destroyVirtualDisplay release");
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
    }
    /**
     * Called whenever a logical display has been added to the system.
     * Use {@link DisplayManager#getDisplay} to get more information about
     * the display.
     *
     * @param displayId The id of the logical display that was added.
     */
    public void onDisplayAdded(int displayId) {
        Log.d(TAG, "onDisplayAdded " + displayId + ", display name " + mDisplayManager.getDisplay(displayId).getName());
    }

    /**
     * Called whenever a logical display has been removed from the system.
     *
     * @param displayId The id of the logical display that was removed.
     */
    public void onDisplayRemoved(int displayId) {
        Log.d(TAG, "onDisplayRemoved " + displayId + ", display name " + mDisplayManager.getDisplay(displayId).getName());
    }

    /**
     * Called whenever the properties of a logical display have changed.
     *
     * @param displayId The id of the logical display that changed.
     */
    public void onDisplayChanged(int displayId) {
        Log.d(TAG, "onDisplayChanged " + displayId + ", display name " + mDisplayManager.getDisplay(displayId).getName());
    }

}
