package edu.osu.recordvideo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

public class VideoRecordFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    // Instance fields.
    private CameraSurfaceView mCameraSurfaceView;
    private Camera mCamera;
    private MediaRecorder mMediaRecorder;
    private Boolean mIsRecording = false;
    private static File mVideoDir;
    private WifiManager mWifiManager;
    private ConnectivityManager mConnectivityManager;

    private final String TAG = getClass().getSimpleName();
    private final int PERMISSION_CAMCORDER_REQUEST = 1;


    private enum CLIENT_SERVER_STATE {CLIENT, SERVER}

    private CLIENT_SERVER_STATE mClientServerState;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return getViewBasedOnRotation(inflater, container);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (lacksCameraPermissions()) {
                requestPermissions(new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO}, PERMISSION_CAMCORDER_REQUEST);
            } else {
                setUpCamera();
            }
        } else {
            setUpCamera();
        }

        initClientServerState();

        Activity activity = getActivity();
        if (activity != null) {
            mWifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            mConnectivityManager = (ConnectivityManager) activity.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        }
    }

    private void writeStatus(Activity activity) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (mClientServerState.equals(CLIENT_SERVER_STATE.SERVER)) {
            editor.putString(activity.getString(R.string.client_server_key), "server");
        } else {
            editor.putString(activity.getString(R.string.client_server_key), "client");
        }
        editor.apply();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Activity activity = getActivity();

        if (activity != null) {
            // If the button's checked, we selected "server"; otherwise, it's "client".
            if (isChecked) {
                mClientServerState = CLIENT_SERVER_STATE.SERVER;
            } else {
                mClientServerState = CLIENT_SERVER_STATE.CLIENT;
            }
            writeStatus(activity);
            doInitStateAndLayouts(activity);
        }
    }


    @Override
    public void onClick(View v) {
        final Activity activity = getActivity();
        if (v.getId() == R.id.button) {
            if (activity != null) {
                if (mIsRecording) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ImageButton imgButton = (activity.findViewById(R.id.button));
                            imgButton.setImageResource(R.drawable.ic_videocam_grey600_48dp);
                            mVideoDir = new File(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_MOVIES), "RecordVideo");
                            Toast.makeText(activity.getApplicationContext(),
                                    getText(R.string.stop_rec) + mVideoDir.toString(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    // stop recording and release camera
                    mMediaRecorder.stop();  // stop the recording
                    releaseMediaRecorder(); // release the MediaRecorder object
                    mCamera.lock();         // take camera access back from MediaRecorder

                    // inform the user that recording has stopped
                    mIsRecording = false;
                } else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ImageButton imgButton = (activity.findViewById(R.id.button));
                            imgButton.setImageResource(R.drawable.ic_videocam_off_grey600_48dp);
                            Toast.makeText(activity.getApplicationContext(), getText(R.string.start_rec),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    // initialize video camera
                    if (prepareVideoRecorder()) {
                        // Camera is available and unlocked, MediaRecorder is prepared,
                        // now you can start recording
                        mMediaRecorder.start();

                        // inform the user that recording has started
                        mIsRecording = true;
                    } else {
                        // prepare didn't work, release the camera
                        releaseMediaRecorder();
                        // inform user
                    }
                }
            }
        }
    }

    @Override
    public void onStart() {
        Log.d(TAG, "++ onStart() ++");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.i(TAG, "+ onResume() +");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.i(TAG, "- onPause() -");
        super.onPause();
        releaseMediaRecorder();
        releaseCamera();
    }

    @Override
    public void onStop() {
        Log.i(TAG, "-- onStop() --");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "--- onDestroy() ---");
        super.onDestroy();
    }


    /*
     * Helper methods from https://developer.android.com/guide/topics/media/camera.html
     * */

    /**
     * A safe way to get an instance of the Camera object.
     */
    private Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            Log.e(TAG, "Camera not available!");
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }

    private boolean prepareVideoRecorder() {

        if (mCamera == null) {
            mCamera = getCameraInstance();
        }
        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();

        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        // Step 4: Set output file
        File outputMediaFile = getOutputMediaFile();
        if (outputMediaFile != null) {
            mMediaRecorder.setOutputFile(outputMediaFile.toString());
        }

        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mCameraSurfaceView.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        mVideoDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES), "RecordVideo");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mVideoDir.exists()) {
            if (!mVideoDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());

        return new File(mVideoDir.getPath() + File.separator +
                "VID_" + timeStamp + ".mp4");
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean lacksCameraPermissions() {
        boolean lacksPermissions = false;
        Activity activity = getActivity();

        if (activity != null) {
            lacksPermissions = activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED;
        }
        return lacksPermissions;
    }

    private void setUpCamera() {
        // Create an instance of Camera
        mCamera = getCameraInstance();
        Activity activity = getActivity();

        if (activity != null) {
            // Create our Preview view and set it as the content of our activity.
            mCameraSurfaceView = new CameraSurfaceView(activity.getApplicationContext(), mCamera);
            FrameLayout preview = activity.findViewById(R.id.camera_preview);
            preview.addView(mCameraSurfaceView);

            // Get Record Button and set icon.
            ImageButton imageButton = activity.findViewById(R.id.button);
            imageButton.setOnClickListener(this);

            ToggleButton toggleButton = activity.findViewById(R.id.client_server_togglebutton);
            toggleButton.setOnCheckedChangeListener(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Activity activity = getActivity();

        if (activity != null) {
            if (requestCode == PERMISSION_CAMCORDER_REQUEST) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setUpCamera();
                } else {
                    Log.e(TAG, "Error: Camera permission denied");
                    Toast.makeText(activity, "User denied camera permission", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void doInitStateAndLayouts(Activity activity) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        String stateStr = sharedPreferences.getString(activity.getString(R.string.client_server_key), "client");

        LinearLayout clientOnlyLayout = activity.findViewById(R.id.client_only_layout);
        LinearLayout serverOnlyLayout = activity.findViewById(R.id.server_only_layout);

        if (stateStr != null) {
            if (stateStr.equals("client")) {
                mClientServerState = CLIENT_SERVER_STATE.CLIENT;
                serverOnlyLayout.setVisibility(View.GONE);
                clientOnlyLayout.setVisibility(View.VISIBLE);
            } else {
                mClientServerState = CLIENT_SERVER_STATE.SERVER;
                serverOnlyLayout.setVisibility(View.VISIBLE);
                clientOnlyLayout.setVisibility(View.GONE);

                TextView ipAddressText = activity.findViewById(R.id.server_ip_address_text);
                if (mWifiManager != null) {
                    // This endian-ness code is from Stack Overflow.
                    WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                    if (wifiInfo != null) {
                        int ipAddress = wifiInfo.getIpAddress();

                        // Convert little-endian to big-endian if needed
                        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                            ipAddress = Integer.reverseBytes(ipAddress);
                        }

                        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

                        String ipAddressString;
                        try {
                            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
                            ipAddressText.setText(ipAddressString);
                        } catch (UnknownHostException ex) {
                            Log.e("WIFIIP", "Unable to get host address.");
                        }
                    }
                }
            }
        }

    }

    private void initClientServerState() {
        Activity activity = getActivity();

        if (activity != null) {
            doInitStateAndLayouts(activity);
        } else {
            Log.e(TAG, "Activity is null, cannot restore client/server state!");
        }
    }

    private View getViewBasedOnRotation(LayoutInflater inflater, ViewGroup container) {
        Activity activity = getActivity();
        View v;

        if (activity != null) {
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

            if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
                v = inflater.inflate(R.layout.fragment_record_video_land, container, false);
            } else {
                v = inflater.inflate(R.layout.fragment_record_video, container, false);
            }
        } else {
            v = inflater.inflate(R.layout.fragment_record_video, container, false);
        }

        return v;
    }

}
