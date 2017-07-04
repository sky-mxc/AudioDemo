package cn.sintoon.audiodemo.utils;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
    private String tempPath ;
    private FileOutputStream dos;
    private boolean recordIng;
    private Thread mThread;

    private static AudioRecordManager instance;

    //录制参数
    private int default_AudioSource = MediaRecorder.AudioSource.MIC;    //麦克风来源
    private int default_RateInHz = 44100;    //采样率
    private int default_Channel = AudioFormat.CHANNEL_IN_STEREO;   //通道
    private int default_AudioFormart = AudioFormat.ENCODING_PCM_16BIT;  //编码格式

    private AudioRecordManager() {
        createAudioRecorder();
    }

    private void createAudioRecorder() {
        bufferSize = AudioRecord.getMinBufferSize(default_RateInHz, default_Channel, default_AudioFormart);
        mAudioRecord = new AudioRecord(default_AudioSource, default_RateInHz, default_Channel, default_AudioFormart, bufferSize);
    }

    public static AudioRecordManager getInstance() {
        if (null == instance) {
            synchronized (AudioRecordManager.class) {
                if (null == instance) {
                    instance = new AudioRecordManager();
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
            mAudioRecord = null;
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
            if (null == mAudioRecord){
                createAudioRecorder();
            }
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
            while (isStarted) {
                int read = mAudioRecord.read(buffer, 0, bufferSize);
                if (read == AudioRecord.ERROR_INVALID_OPERATION
                        || read == AudioRecord.ERROR_BAD_VALUE
                        || read == AudioRecord.ERROR_DEAD_OBJECT
                        || read == AudioRecord.ERROR) {
                    continue;
                }

                try {
                    if (null == dos) return;
                    //写入文件
                    dos.write(buffer, 0, read);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                if (null != dos) {
                    dos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            //加入头文件，转码
            copyWaveFile(tempPath,path);
        }
    };

    private void setPath(String path) {
        if (TextUtils.isEmpty(path)) {
            return;
        }
        this.path = path;
        try {
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            //临时文件
            String name = "temp.raw";
            File tempFile = new File(file.getParent(),name);
            if (!tempFile.exists())
            {
                tempFile.delete();
            }
            tempFile.createNewFile();
            this.tempPath = tempFile.getPath();
            dos = new FileOutputStream(tempFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean getState() {
        return isStarted;
    }

    // 这里得到可播放的音频文件
    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = default_RateInHz;
        int channels = 2;
        long byteRate = 16 * default_RateInHz * channels / 8;
        byte[] data = new byte[bufferSize];
        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 这里提供一个头信息。插入这些信息就可以得到可以播放的文件。
     * 为啥插入这44个字节，这个还真没深入研究，不过你随便打开一个wav
     * 音频的文件，可以发现前面的头文件可以说基本一样哦。每种格式的文件都有
     * 自己特有的头文件。
     */
    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }
}
