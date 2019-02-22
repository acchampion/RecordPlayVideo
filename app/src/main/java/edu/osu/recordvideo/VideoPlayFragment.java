package edu.osu.recordvideo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;

import static android.app.Activity.RESULT_OK;

/*
 * Fragment for playing video. I consulted
 *
 * https://www.dev2qa.com/android-play-video-file-from-local-sd-card-web-example/
 *
 * to help make this class.
 */
public class VideoPlayFragment extends Fragment implements View.OnClickListener {
    private ViewGroup mContainer;
    private VideoView mVideoView;
    private Button mOpenButton, mPlayButton, mStopButton;
    private Uri mVideoFileUri;

    private final String TAG = getClass().getSimpleName();
    private final int PERMISSION_STORAGE_REQUEST = 1, REQUEST_VIDEO_FILE = 2;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = getViewBasedOnRotation(inflater, container);
        mContainer = container;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (lacksStoragePermissions()) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_STORAGE_REQUEST);
            } else {
                setUpVideoPlayer(v);
            }
        } else {
            setUpVideoPlayer(v);
        }

        setRetainInstance(true);

        return v;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.openButton:
                Log.d(TAG, "Open video");
                Intent selectVideoIntent = new Intent(Intent.ACTION_GET_CONTENT);
                selectVideoIntent.setType("video/*");
                startActivityForResult(selectVideoIntent, REQUEST_VIDEO_FILE);
                break;
            case R.id.playButton:
                mVideoView.setVideoURI(mVideoFileUri);
                mVideoView.start();
                break;
            case R.id.stopButton:
                mVideoView.stopPlayback();
                break;
        }
    }

    private void setUpVideoPlayer(View v) {
        mVideoView = v.findViewById(R.id.videoView);

        mOpenButton = v.findViewById(R.id.openButton);
        mOpenButton.setOnClickListener(this);
        mPlayButton = v.findViewById(R.id.playButton);
        mPlayButton.setOnClickListener(this);
        mStopButton = v.findViewById(R.id.stopButton);
        mStopButton.setOnClickListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean lacksStoragePermissions() {
        boolean lacksPermissions = false;
        Activity activity = getActivity();

        if (activity != null) {
            lacksPermissions = activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
        }
        return lacksPermissions;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Identify activity by request code.
        if (requestCode == REQUEST_VIDEO_FILE) {
            // If the request is success.
            if (resultCode == RESULT_OK) {
                mVideoFileUri = data.getData();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Activity activity = getActivity();

        if (activity != null) {
            if (requestCode == PERMISSION_STORAGE_REQUEST) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (mContainer != null) {
                        setUpVideoPlayer(getViewBasedOnRotation(LayoutInflater.from(activity), mContainer));
                    }
                } else {
                    Log.e(TAG, "Error: Permission denied to read storage");
                    Toast.makeText(activity, "User denied storage permission", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private View getViewBasedOnRotation(LayoutInflater inflater, ViewGroup container) {
        Activity activity = getActivity();
        View v;

        if (activity != null) {
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

            if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
                v = inflater.inflate(R.layout.fragment_play_video_land, container, false);
            } else {
                v = inflater.inflate(R.layout.fragment_play_video, container, false);
            }
        } else {
            v = inflater.inflate(R.layout.fragment_play_video, container, false);
        }

        return v;
    }
}
