package cn.sintoon.audiodemo.utils;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

/**
 * Created by mxc on 2017/7/5.
 * description:
 */

public class MediaPlayerHelper {

    private MediaPlayer mMediaPlayer;
    private String path;
    private boolean isPlay;
    private Thread mThread;
    private MediaPlayListener playListener;

    private static MediaPlayerHelper instance;

    private MediaPlayerHelper() {
    }

    public static MediaPlayerHelper getInstance() {
        if (null == instance) {
            instance = new MediaPlayerHelper();
        }
        return instance;
    }

    public void startPlay(String path) {
        if (TextUtils.isEmpty(path)) {
            error("播放文件出现异常");
            return;
        }
        this.path = path;
        try {
            if (!isPlay) {
                createMediaPlayer();
            }
        } catch (IOException e) {
            e.printStackTrace();
            error("没有找到播放文件");
        }catch (Exception e){
            e.printStackTrace();
            error("播放器异常");
        }
    }

    public void stopPlay() {
        try {
            if (isPlay) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            error("播放器异常");
        } finally {
            mMediaPlayer = null;
            isPlay = false;
        }
    }

    public void pausePlay() {
        try {
            if (isPlay) {
                mMediaPlayer.pause();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            error("播放器出现异常");
        }
    }

    public void resumePlay() {
        try {
            if (isPlay) {
                mMediaPlayer.start();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            error("播放器出现异常");
        }
    }


    private void createMediaPlayer() throws IOException {
        if (null == mMediaPlayer) {
            mMediaPlayer = new MediaPlayer();
        }
        //init
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setDataSource(path);
        mMediaPlayer.setOnErrorListener(onErrorListener);
        mMediaPlayer.setOnPreparedListener(onPreparedListener);
        mMediaPlayer.setOnCompletionListener(onCompletionListener);
        //prepare
        mMediaPlayer.prepareAsync();
        if (null!=playListener){
            playListener.onStartPre();
        }
    }

    private MediaPlayer.OnPreparedListener onPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            if (null!=playListener){
                playListener.onStart();
            }
            //准备好了
            mMediaPlayer.start();
            isPlay = true;
        }
    };

    private MediaPlayer.OnErrorListener onErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            error("what->"+what+";extra->"+extra);
            return true;
        }
    };

    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            Log.e("completion", "播放完毕");
            if (null!=playListener){
                playListener.onCompletion();
            }
            stopPlay();
        }
    };

    private void error(String msg) {
        Log.e("error", msg);
        if (null!=playListener){
            playListener.onError(msg);
        }
    }

    public void setPlayListener(MediaPlayListener playListener) {
        this.playListener = playListener;
    }

    public boolean isPlay(){
        return isPlay;
    }

    public interface MediaPlayListener {
        void onError(String msg);

        void onStartPre();

        void onStart();
        void onCompletion();
    }
}
