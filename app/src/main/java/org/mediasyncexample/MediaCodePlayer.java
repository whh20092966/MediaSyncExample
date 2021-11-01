package org.mediasyncexample;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
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
    private MediaCodecVideoTrack mediaCodecVideoTrack;

    private MediaCodec audioDecoder;
    private MediaExtractor audioExtractor;
    private MediaCodecAudioTrack mediaCodecAudioTrack;



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

        prepareVideo();

        //prepareAudio();
    }

    private void prepareAudio() {
        int trackCount = audioExtractor.getTrackCount();
        for (int track=0; track<trackCount; ++track){
            MediaFormat mediaFormat = audioExtractor.getTrackFormat(track);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")){

                int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                int channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

                try {
                    audioDecoder = MediaCodec.createDecoderByType(mime);
                    MLog.d(TAG, "audioDecoder name: " + audioDecoder.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                audioExtractor.selectTrack(track);
                audioDecoder.configure(mediaFormat, null, null, 0);
                Log.d(TAG, "duration: " + duration + " videoWidth: " + videoWidth + " videoHeight: " + videoHeight);

                int channelConfig;
                switch (channelCount) {
                    case 1:
                        channelConfig = AudioFormat.CHANNEL_OUT_MONO;
                        break;
                    case 2:
                        channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
                        break;
                    case 6:
                        channelConfig = AudioFormat.CHANNEL_OUT_5POINT1;
                        break;
                    default:
                        throw new IllegalArgumentException();
                }

                int minBufferSize =
                        AudioTrack.getMinBufferSize(
                                sampleRate,
                                channelCount,
                                AudioFormat.ENCODING_PCM_16BIT);

                AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        channelConfig,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBufferSize,
                        AudioTrack.MODE_STREAM);

                mediaCodecAudioTrack = new MediaCodecAudioTrack(audioDecoder, audioExtractor, audioTrack);
                break;
            }
        }
    }

    private void prepareVideo() {
        int trackCount = videoExtractor.getTrackCount();
        for (int track=0; track<trackCount; ++track){
            MediaFormat mediaFormat = videoExtractor.getTrackFormat(track);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")){
                videoWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                videoHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                duration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                try {
                    videoDecoder = MediaCodec.createDecoderByType(mime);
                    MLog.d(TAG, "videoDecoder name: " + videoDecoder.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                videoExtractor.selectTrack(track);
                videoDecoder.configure(mediaFormat, surface, null, 0);

                Log.d(TAG, "duration: " + duration + " videoWidth: " + videoWidth + " videoHeight: " + videoHeight);
                break;
            }
        }

        mediaCodecVideoTrack = new MediaCodecVideoTrack(videoDecoder,  videoExtractor, surface);
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

        /*new Thread(){
            @Override
            public void run() {
                super.run();
                mediaCodecAudioTrack.start();
                for (;;){
                    mediaCodecAudioTrack.doDecodeWork();
                }
            }
        }.start();*/
    }
}
