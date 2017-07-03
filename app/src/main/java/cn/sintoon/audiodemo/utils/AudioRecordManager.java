package cn.sintoon.audiodemo.utils;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by mxc on 2017/7/3.
 * description:
 */

public class AudioRecordManager {

    private int bufferSize; //缓冲区size
    private AudioRecord mAudioRecord;
    private boolean isStarted;
    private String path;
    private FileOutputStream dos;
    private boolean recordIng;
    private Thread mThread;

    private static AudioRecordManager instance;

    //录制参数
    private int default_AudioSource = MediaRecorder.AudioSource.MIC;    //麦克风来源
    private int default_RateInHz = 44100;    //采样率
    private int default_Channel = AudioFormat.CHANNEL_IN_MONO;   //单通道
    private int default_AudioFormart = AudioFormat.ENCODING_PCM_16BIT;  //编码格式

    private AudioRecordManager() {
        bufferSize = AudioRecord.getMinBufferSize(default_RateInHz, default_Channel, default_AudioFormart);
        mAudioRecord = new AudioRecord(default_AudioSource, default_RateInHz, default_Channel, default_AudioFormart, bufferSize);
    }
    public static AudioRecordManager getInstance(){
        if (null == instance){
            synchronized (AudioRecordManager.class){
                if (null == instance){
                    instance =new AudioRecordManager();
                }
            }
        }
        return instance;
    }





    public void startRecord(String path) {
        destroyThread();
        isStarted = true;
        setPath(path);
        mThread = new Thread(recordRun);
        mThread.start();
    }

    public void stopRecord() {
        try {
            destroyThread();
            if (null != mAudioRecord && mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                if (mAudioRecord.getState() == AudioRecord.RECORDSTATE_RECORDING) {
                    mAudioRecord.stop();
                }
                if (null != mAudioRecord) {
                    mAudioRecord.release();
                }
            }
            if (null != dos) {
                //文件
                dos.flush();
                dos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void destroyThread() {
        isStarted = false;
        recordIng = false;
        try {
            if (null != mThread && mThread.getState() == Thread.State.RUNNABLE) {
                mThread.interrupt();
                Thread.sleep(500);
                mThread = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            mThread = null;
        } finally {
            mThread = null;
        }
    }

    private Runnable recordRun = new Runnable() {
        @Override
        public void run() {
            int state = mAudioRecord.getState();
            if (state != AudioRecord.STATE_INITIALIZED) {
                isStarted = false;
                //没有初始化完成
                return;
            }
            //线程优先级
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

            //缓冲数组
            byte[] buffer = new byte[bufferSize];

            mAudioRecord.startRecording();
            recordIng = true;
            while (recordIng) {
                int read = mAudioRecord.read(buffer, 0, bufferSize);
                if (read == AudioRecord.ERROR_INVALID_OPERATION
                        || read == AudioRecord.ERROR_BAD_VALUE
                        || read == AudioRecord.ERROR_DEAD_OBJECT
                        || read == AudioRecord.ERROR) {
                    continue;
                }

                try {
                    if (null ==dos) return;
                    //写入文件
                    dos.write(buffer, 0, read);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private void setPath(String path){
        if (TextUtils.isEmpty(path)){
            return;
        }
        this.path = path;
        try {
            File file = new File(path);
            if (file.exists()){
                file.delete();
            }
            file.createNewFile();
            dos = new FileOutputStream(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean getState() {
        return isStarted;
    }
}
