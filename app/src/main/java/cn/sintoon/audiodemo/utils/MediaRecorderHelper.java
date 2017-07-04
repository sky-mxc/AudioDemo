package cn.sintoon.audiodemo.utils;

import android.media.MediaRecorder;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by mxc on 2017/7/4.
 * description:
 */

public class MediaRecorderHelper {

    public static final int STATE_RECORDING = 0;
    public static final int STATE_STOP = 1;
    public static final int STATE_PAUSE = 2;


    private MediaRecorder mRecorder;
    private File file;  //录音的文件
    private boolean isRecorder;
    private List<File> tempFiles;
    private File mkdir;
    private int state = STATE_STOP;
    private RecorderListener recorderListener;

    private static MediaRecorderHelper instance;

    public static MediaRecorderHelper getInstance() {
        if (null == instance) {
            instance = new MediaRecorderHelper();
        }
        return instance;
    }

    private MediaRecorderHelper() {
    }

    public void startRecorder(String path) {
        try {
            if (TextUtils.isEmpty(path)) {
                error("没有保存路径，无法进行录音");
                return;
            }
            file = new File(path);
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();

            mkdir = file.getParentFile();
            tempFiles = new ArrayList<>();
            createRecorder();
            mRecorder.start();
            isRecorder = true;
            state = STATE_RECORDING;
        } catch (IOException e) {
            e.printStackTrace();
            error("录音文件出现错误");
        } catch (Exception e) {
            e.printStackTrace();
            error("录音出现异常");
        }
    }

    public void pauseRecorder() {
        try {
            if (isRecorder) {
                closeRecorder();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            error("出现异常");
        }
        state = STATE_PAUSE;
    }

    public void resumeRecorder() {
        try {
            if (isRecorder) {
                createRecorder();
                mRecorder.start();
            }
            state = STATE_RECORDING;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            error("出现异常");
        }
    }


    public boolean isRecorder() {
        return isRecorder;
    }

    public void stopRecorder() {
        state = STATE_STOP;
        isRecorder =false;
        closeRecorder();
        new Thread(mergeRun).start();
    }


    /**
     * 参考资料 http://www.cnblogs.com/lqminn/archive/2012/11/12/2766563.html
     */
    private Runnable mergeRun = new Runnable() {
        @Override
        public void run() {
            FileOutputStream fos = null;
            if (null != recorderListener) {
                recorderListener.starSave();
            }
            try {
                int size = tempFiles.size();
                fos = new FileOutputStream(file);
                for (int i = 0; i < size; i++) {
                    File temp = tempFiles.get(i);
                    Log.e("merge", "temp->" + temp.getPath());
                    FileInputStream fis = new FileInputStream(temp);
                    if (i ==0){
                        writeFile(fos,fis,0);
                    }else {
                        writeFile(fos, fis, 6);
                    }
                    fis.close();
                    temp.delete();
                }
                fos.flush();
                if (null != recorderListener) {
                    recorderListener.saveSucc();
                }
            } catch (IOException e) {
                e.printStackTrace();
                error("保存录音文件失败");

            } finally {
                try {
                    if (null != fos) {
                        fos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                tempFiles.clear();

            }

        }
    };

    private void writeFile(FileOutputStream fos, FileInputStream fis, int offset) throws IOException {
        byte[] buffer = new byte[2048];
        int len = -1;
        int first = 0;

        while ((len = fis.read(buffer)) != -1) {
            if (first == 0) {
                fos.write(buffer, offset, len - offset);
            } else {
                fos.write(buffer, 0, len);
            }
            first++;
        }

    }

    private File getTempFile() {
        Calendar calendar = Calendar.getInstance();
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int milli = calendar.get(Calendar.MILLISECOND);
        try {
            File file = new File(mkdir, "media_" + minute + "_" + second + "_" + milli + ".amr");
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            tempFiles.add(file);
            Log.e("getTempFile", file.getPath());
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            error("录音文件异常");
        }
        return file;
    }

    private void createRecorder() {
        if (null == file) return;
        if (null == mRecorder) {
            mRecorder = new MediaRecorder();
        }
        try {
            initRecorder();
            mRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            error("录音初始化失败");
        }
    }

    private void initRecorder() {
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
        mRecorder.setOutputFile(getTempFile().getPath());
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    }

    private void closeRecorder() {
        if (null != mRecorder) {
            if (state == STATE_RECORDING) {
                mRecorder.stop();
            }
            mRecorder.release();
            mRecorder = null;
        }
    }

    private void error(String msg) {
        Log.e("error", msg);
        if (null != recorderListener) {
            recorderListener.onError(msg);
        }
    }

    public int getState() {
        return state;
    }

    public void setRecorderListener(RecorderListener recorderListener) {
        this.recorderListener = recorderListener;
    }

    public interface RecorderListener {
        void onError(String message);

        void starSave();

        void saveSucc();

    }

}
