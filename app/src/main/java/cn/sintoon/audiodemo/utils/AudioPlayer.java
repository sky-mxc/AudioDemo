package cn.sintoon.audiodemo.utils;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Process;
import android.text.TextUtils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by mxc on 2017/7/3.
 * description:
 */

public class AudioPlayer {

    private AudioTrack mAudioTrack;

    private boolean plaing;
    private DataInputStream dis;
    private int bufferSize;
    private Thread mThread;

    private int default_StreamType = AudioManager.STREAM_MUSIC; //音乐类型
    private int default_SampleRateInHz = 44100; //采样率
    private int default_channel = AudioFormat.CHANNEL_IN_STEREO;  //
    private int default_AudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int default_mode  = AudioTrack.MODE_STREAM;

    private static AudioPlayer instance;
    public static AudioPlayer getInstance(){
        if (null == instance){
            synchronized (AudioPlayer.class){
                if (null ==instance){
                    instance = new AudioPlayer();
                }
            }
        }
        return instance;
    }

    public boolean getState(){
        return plaing;
    }
    private AudioPlayer(){
        bufferSize = AudioTrack.getMinBufferSize(default_SampleRateInHz,default_channel,default_AudioFormat);
        mAudioTrack = new AudioTrack(default_StreamType,default_SampleRateInHz,default_channel,default_AudioFormat,bufferSize,default_mode);
    }

    public void start(String path) {
        destroyThread();
            setPath(path);
        plaing = true;
        mThread = new Thread(runnable);
        mThread.start();
    }

    public void stop(){
        destroyThread();
        if (null!=mAudioTrack){
            if (mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED){
                mAudioTrack.stop();
            }
            if (null!= mAudioTrack) {
                mAudioTrack.release();
            }
        }
    }

    private void setPath(String path) {
        if (TextUtils.isEmpty(path)) {
            return;
        }
        try {
            File fiel = new File(path);
            if (!fiel.exists()) {
                return;
            }
            dis = new DataInputStream(new FileInputStream(fiel));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void destroyThread() {
        plaing = false;
        try {
            if (null != mThread && mThread.getState() == Thread.State.RUNNABLE) {
                if (null != mAudioTrack) {
                    Thread.sleep(500);
                    mThread.interrupt();
                }
                mThread = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mThread = null;
        }


    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
            if (null == mAudioTrack) {
                return;
            }
            try {
                int readCount = 0;
                if (null == dis) return;
                byte[] buffer = new byte[bufferSize];



                while (dis.available() > 0) {
                    readCount = dis.read(buffer);
                    if (readCount == AudioTrack.ERROR_INVALID_OPERATION || readCount == AudioTrack.ERROR_BAD_VALUE) {
                        continue;
                    }
                    if (readCount != 0 && readCount != -1) {
                        mAudioTrack.play();
                        mAudioTrack.write(buffer, 0, readCount);
                    }
                }
                stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
}
