package cn.sintoon.audiodemo;

import android.Manifest;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import cn.sintoon.audiodemo.utils.AudioPlayer;
import cn.sintoon.audiodemo.utils.AudioRecordManager;
import cn.sintoon.audiodemo.utils.MediaPlayerHelper;
import cn.sintoon.audiodemo.utils.MediaRecorderHelper;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
            Manifest.permission.RECORD_AUDIO};

    private boolean record;
    private boolean play;
    Button mAudioRecord;
    Button mAudioPlay;
    Button mMediaRecorder;
    Button mMediaRecorderStop;
    Button mMediaPlay;
    private String mediaPath;

    private String folder = Environment.getExternalStorageDirectory().getPath() + "/" + "audioDemo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAudioRecord = (Button) findViewById(R.id.audio_record);
        mAudioPlay = (Button) findViewById(R.id.audio_play);
        mMediaRecorder = (Button) findViewById(R.id.media_recorder);
        mMediaRecorderStop = (Button) findViewById(R.id.media_recorder_stop);
        mMediaPlay = (Button) findViewById(R.id.media_play);

        MediaRecorderHelper.getInstance().setRecorderListener(recorderListener);
        MediaPlayerHelper.getInstance().setPlayListener(playListener);
        boolean b = EasyPermissions.hasPermissions(this, permissions);
        File mkdir = new File(folder);
        if (!mkdir.exists()) {
            mkdir.mkdir();
        }

        if (!b) {
            EasyPermissions.requestPermissions(this, "录音需要部分权限", 200, permissions);
        }


    }


    public void onClick(View view) {
        String name = "audio.wav";
        String path = folder + "/" + name;
        String mediaName = "media.amr";
        String meidaPath = folder + "/" + mediaName;
        this.mediaPath = meidaPath;
        int state = MediaRecorderHelper.getInstance().getState();
        switch (view.getId()) {
            case R.id.audio_record:
                record = AudioRecordManager.getInstance().getState();
                if (!record) {
                    AudioRecordManager.getInstance().startRecord(path);
                    mAudioRecord.setText("audio停止");
                } else {
                    AudioRecordManager.getInstance().stopRecord();
                    mAudioRecord.setText("audio开始录音");
                }
                break;
            case R.id.audio_play:
                play = AudioPlayer.getInstance().getState();
                if (!play) {
                    AudioPlayer.getInstance().start(path);
                    mAudioPlay.setText("audio play stop");
                } else {
                    AudioPlayer.getInstance().stop();
                    mAudioPlay.setText("audio play start");
                }
                break;
            case R.id.media_recorder:
                switch (state) {
                    case MediaRecorderHelper.STATE_STOP:
                        MediaRecorderHelper.getInstance().startRecorder(meidaPath);
                        mMediaRecorder.setText("media recorder pause");
                        mMediaRecorderStop.setVisibility(View.VISIBLE);
                        break;
                    case MediaRecorderHelper.STATE_PAUSE:
                        MediaRecorderHelper.getInstance().resumeRecorder();
                        mMediaRecorder.setText("media recorder pause");
                        break;
                    case MediaRecorderHelper.STATE_RECORDING:
                        MediaRecorderHelper.getInstance().pauseRecorder();
                        mMediaRecorder.setText("media recorder resume");
                        break;
                }
                break;
            case R.id.media_recorder_stop:
                view.setVisibility(View.GONE);
                mMediaRecorder.setText("media recorder start");
                MediaRecorderHelper.getInstance().stopRecorder();
                break;
            case R.id.media_play:
                play = MediaPlayerHelper.getInstance().isPlay();
                if (play){
                    MediaPlayerHelper.getInstance().stopPlay();
                    mMediaPlay.setText("media start");
                }else{
                   AudioManager am= (AudioManager) getSystemService(AUDIO_SERVICE);
                    int audioFocus = am.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                    if (audioFocus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
                        MediaPlayerHelper.getInstance().startPlay(mediaPath);
                        mMediaPlay.setText("media stop");
                    }else{
                        Log.e("startPlay","没有请求到音频焦点");
                        Toast.makeText(this,"没有请求到音频焦点",Toast.LENGTH_LONG);
                    }
                }
                break;
        }
    }

    private AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            // TODO: 2017/7/5 失去音频焦点后的的处理
            switch (focusChange){
                case AudioManager.AUDIOFOCUS_LOSS:
                    MediaPlayerHelper.getInstance().stopPlay();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    MediaPlayerHelper.getInstance().pausePlay();
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    MediaPlayerHelper.getInstance().resumePlay();
                    break;
            }

        }
    };

    private MediaPlayerHelper.MediaPlayListener playListener = new MediaPlayerHelper.MediaPlayListener() {
        @Override
        public void onError(String msg) {
            Toast.makeText(MainActivity.this,msg,Toast.LENGTH_LONG).show();
        }

        @Override
        public void onStartPre() {
            Log.e("开始准备","");
            Toast.makeText(MainActivity.this,"开始准备",Toast.LENGTH_LONG).show();
        }

        @Override
        public void onStart() {
            Log.e("开始播放","");
            Toast.makeText(MainActivity.this,"开始播放",Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCompletion() {
            Log.e("播放完毕","");
            mMediaPlay.setText("media start");
            Toast.makeText(MainActivity.this,"播放完毕",Toast.LENGTH_LONG).show();
            AudioManager am= (AudioManager) getSystemService(AUDIO_SERVICE);
            am.abandonAudioFocus(onAudioFocusChangeListener);
        }
    };

    private MediaRecorderHelper.RecorderListener recorderListener = new MediaRecorderHelper.RecorderListener() {
        @Override
        public void onError(String message) {
            Log.e("onError", message);
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
        }

        @Override
        public void starSave() {
            Log.e("startSave", "");
        }

        @Override
        public void saveSucc() {
            Toast.makeText(MainActivity.this, "save success", Toast.LENGTH_LONG).show();
        }
    };

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }
}
