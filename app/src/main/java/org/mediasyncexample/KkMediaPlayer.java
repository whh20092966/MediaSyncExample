package org.mediasyncexample;

import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.MediaSync;
import android.media.PlaybackParams;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class KkMediaPlayer {

    private final static String TAG = "KkMediaPlayer";

    private MediaPlayer.OnPreparedListener preparedListener;
    private MediaExtractor videoExtractor;
    private MediaExtractor audioExtractor;

    private MediaCodec videoDecoder;
    private MediaCodec audioDecoder;

    private MediaSync mediaSync;

    private int videoWidth = 0;
    private int videoHeight = 0;
    private long duration = 0;

    private Surface surface;

    public KkMediaPlayer(){

    }

    public void setSurface(Surface surface){
        mediaSync = new MediaSync();
        mediaSync.setSurface(surface);
        this.surface = surface;
    }

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

    public void setDataSource(AssetFileDescriptor fileDescriptor){
        videoExtractor = new MediaExtractor();
        audioExtractor = new MediaExtractor();

        try {
            videoExtractor.setDataSource(fileDescriptor);
            audioExtractor.setDataSource(fileDescriptor);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void prepare(){
        if (videoExtractor == null || audioExtractor == null || surface == null){
            Log.d(TAG, "setDataSource and setSurface before prepare !");
            return;
        }

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
                videoDecoder.configure(mediaFormat, mediaSync.createInputSurface(), null, 0);
                videoDecoder.setCallback(videoCodecCallback, new Handler());
                break;
            }
        }

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
                int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                audioDecoder.configure(mediaFormat, null, null, 0);
                audioDecoder.setCallback(audioCodecCallback);

                int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
                AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM);

                mediaSync.setAudioTrack(audioTrack);
                mediaSync.setCallback(mediaSyncCallback, new Handler());
                break;
            }
        }
    }

    public void start(){
        mediaSync.setPlaybackParams(new PlaybackParams().setSpeed(1.0f));
        audioDecoder.start();
        videoDecoder.start();
    }

    public void stop(){
        videoDecoder.stop();
        audioDecoder.stop();
    }

    public void pause(){

    }

    public int getVideoWidth(){
        return videoWidth;
    }

    public int getVideoHeight(){
        return videoHeight;
    }

    public void seekTo(long pos){
        videoExtractor.seekTo(pos*1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
    }

    public int getCurrentPosition(){
        return 0;
    }

    public long getDuration(){
        return duration/1000;
    }

    public void release(){
        mediaSync.release();
        videoDecoder.release();
        audioDecoder.release();

        videoExtractor.release();
        audioExtractor.release();
    }

    public void setLooping(boolean looping){

    }

    private final MediaCodec.Callback videoCodecCallback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int i) {
           // Log.d(TAG, "onInputBufferAvailable " + i);
            //解码器有空闲的buffer
            ByteBuffer byteBuffer = videoDecoder.getInputBuffer(i);
            int read = videoExtractor.readSampleData(byteBuffer, 0);
            Log.d(TAG, "onInputBufferAvailable i "+i+" read : " + read);
            if (read < 0){
                videoDecoder.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }else {
                //把提取到的数据，放入队列进行解码
                videoDecoder.queueInputBuffer(i, 0, read, videoExtractor.getSampleTime(), 0);
                videoExtractor.advance();
            }
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int i, @NonNull MediaCodec.BufferInfo bufferInfo) {

            Log.d(TAG, "onOutputBufferAvailable i "+i);

            //已经解码，并给到surface 渲染了的buffer, 要释放，重复复用
            if (0 != (MediaCodec.BUFFER_FLAG_END_OF_STREAM & bufferInfo.flags)){
                Log.d(TAG, "onOutputBufferAvailable BUFFER_FLAG_END_OF_STREAM");
            }
            videoDecoder.releaseOutputBuffer(i, bufferInfo.presentationTimeUs*1000);
        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
            Log.d(TAG, "onError " + e.getMessage());
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {

        }
    };

    private final MediaCodec.Callback audioCodecCallback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int i) {
            ByteBuffer byteBuffer = audioDecoder.getInputBuffer(i);
            int read = audioExtractor.readSampleData(byteBuffer, 0);

            if (read < 0){
                audioDecoder.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }else {
                audioDecoder.queueInputBuffer(i, 0, read, audioExtractor.getSampleTime(), 0);
                audioExtractor.advance();
            }

        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int i, @NonNull MediaCodec.BufferInfo bufferInfo) {
            //音轨的数据并未消费
            ByteBuffer decoderBuffer = audioDecoder.getOutputBuffer(i);
            ByteBuffer copy = ByteBuffer.allocate(decoderBuffer.remaining());
            copy.put(decoderBuffer);
            copy.flip();

            audioDecoder.releaseOutputBuffer(i, false);
            mediaSync.queueAudio(copy, i, bufferInfo.presentationTimeUs);
        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {

        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {

        }
    };

    private final MediaSync.Callback mediaSyncCallback = new MediaSync.Callback(){

        @Override
        public void onAudioBufferConsumed(@NonNull MediaSync mediaSync, @NonNull ByteBuffer byteBuffer, int i) {
            byteBuffer.clear();
            Log.d(TAG, "onAudioBufferConsumed");
        }
    };

    public void setPreparedListener(MediaPlayer.OnPreparedListener preparedListener) {
        this.preparedListener = preparedListener;
    }
}
