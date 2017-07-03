package cn.sintoon.audiodemo;

import android.Manifest;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.util.List;

import cn.sintoon.audiodemo.utils.AudioPlayer;
import cn.sintoon.audiodemo.utils.AudioRecordManager;
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

    private String folder = Environment.getExternalStorageDirectory().getPath()+"/"+"audioDemo";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAudioRecord = (Button) findViewById(R.id.audio_record);
        mAudioPlay = (Button) findViewById(R.id.audio_play);
        boolean b = EasyPermissions.hasPermissions(this, permissions);
        File mkdir = new File(folder);
        if (!mkdir.exists()){
            mkdir.mkdir();
        }

        if (!b){
            EasyPermissions.requestPermissions(this,"录音需要部分权限",200,permissions);
        }


    }


    public void onClick(View view){
                String name ="audio.pcm";
                String path = folder+"/"+name;
        switch (view.getId()){
            case R.id.audio_record:
                record = AudioRecordManager.getInstance().getState();
                if (!record) {
                    AudioRecordManager.getInstance().startRecord(path);
                    mAudioRecord.setText("audio停止");
                }else{
                    AudioRecordManager.getInstance().stopRecord();
                    mAudioRecord.setText("audio开始录音");
                }
                break;
            case R.id.audio_play:
                play = AudioPlayer.getInstance().getState();
                if (!play){
                    AudioPlayer.getInstance().start(path);
                    mAudioPlay.setText("audio play stop");
                }else{
                    AudioPlayer.getInstance().stop();
                    mAudioPlay.setText("audio play start");
                }
                break;
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }
}
