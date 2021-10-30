package org.mediasyncexample;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import java.io.IOException;

public class MediaCodePlayer {
    private static final String TAG = "MediaCodePlayer";

    private MediaCodec videoDecoder;
    private MediaExtractor videoExtractor;

    private MediaCodec audioDecoder;
    private MediaExtractor audioExtractor;

    private MediaCodecVideoTrack mediaCodecVideoTrack;

    private int videoWidth = 0;
    private int videoHeight = 0;
    private long duration = 0;

    private Surface surface;

    public void setDataSource(String path){
        videoExtractor = new MediaExtractor();
        audioExtractor = new MediaExtractor();

        try {
            videoExtractor.setDataSource(path);
            audioExtractor.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setSurface(Surface surface){
        this.surface = surface;
    }

    public void prepare(){
        if (videoExtractor == null || audioExtractor == null || surface == null){
            Log.d(TAG, "setDataSource and setSurface before prepare !");
            return;
        }

        videoTrack();

        /*int trackCount = audioExtractor.getTrackCount();
        for (int track=0; track<trackCount; ++track){
            MediaFormat mediaFormat = audioExtractor.getTrackFormat(track);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")){

                try {
                    audioDecoder = MediaCodec.createDecoderByType(mime);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                audioExtractor.selectTrack(track);

                videoDecoder.configure(mediaFormat, surface, null, 0);

                Log.d(TAG, "duration: " + duration + " videoWidth: " + videoWidth + " videoHeight: " + videoHeight);
                break;
            }
        }*/
    }

    private void videoTrack() {
        int trackCount = videoExtractor.getTrackCount();
        for (int track=0; track<trackCount; ++track){
            MediaFormat mediaFormat = videoExtractor.getTrackFormat(track);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")){
                videoWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                videoHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                //TODO 回调视频宽高
                duration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                try {
                    videoDecoder = MediaCodec.createDecoderByType(mime);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                videoExtractor.selectTrack(track);
                videoDecoder.configure(mediaFormat, surface, null, 0);

                Log.d(TAG, "duration: " + duration + " videoWidth: " + videoWidth + " videoHeight: " + videoHeight);
                break;
            }
        }

        mediaCodecVideoTrack = new MediaCodecVideoTrack(videoDecoder, videoExtractor);
    }

    public void start(){
        new Thread(){
            @Override
            public void run() {
                super.run();
                mediaCodecVideoTrack.start();
                for (;;) {
                    mediaCodecVideoTrack.doDecodeWork();
                }
            }
        }.start();
    }
}
