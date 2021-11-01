package org.mediasyncexample;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaCodecVideoTrack extends MediaCodecTrack{
    private static final String TAG = "MediaCodecVideoTrack";

    private MediaExtractor extractor;
    private final Surface surface;
    //private final MediaFormat mainMediaFormat;
    private MediaExtractor mainExtractor;

    public MediaCodecVideoTrack(MediaCodec mediaCodec, MediaExtractor extractor, Surface surface) {
        super(mediaCodec);
        this.extractor = extractor;
        this.surface = surface;
        this.mainExtractor = extractor;
    }

    public void setAdMediaExtractor(MediaExtractor extractor){
        this.extractor = extractor;
        lastPresentationTimeUs = 0;
    }

    @Override
    public void onInputBufferAvailable(int inputIndex) {
        ByteBuffer byteBuffer = mediaCodec.getInputBuffer(inputIndex);

        if (byteBuffer == null){
            MLog.d(TAG, "getInputBuffer error !");
            return;
        }

        int read = extractor.readSampleData(byteBuffer, 0);
        if (read < 0){
            //END
            mediaCodec.queueInputBuffer(inputIndex, 0, 0, 0,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        }else{
            mediaCodec.queueInputBuffer(inputIndex, 0, read, extractor.getSampleTime(), 0);
            extractor.advance();
        }
    }

    @Override
    public void onFormatChanged(MediaFormat newMediaFormat) {
        //TODO
    }

    private long lastPresentationTimeUs = 0;
    private boolean hasPlayerAd = false;

    private boolean resume = false;
    @Override
    public void onOutputBufferAvailable(int outputIndex, MediaCodec.BufferInfo bufferInfo) {
        //已经解码，并给到surface 渲染了的buffer, 要释放，重复复用
        if (MediaCodec.BUFFER_FLAG_END_OF_STREAM == bufferInfo.flags){
            MLog.d(TAG, "onOutputBufferAvailable BUFFER_FLAG_END_OF_STREAM");
        }

        if (lastPresentationTimeUs > 0){
            long passTimeNs = System.nanoTime() - startTimeNs;

            //MLog.d(TAG, "passTimeNs: " + passTimeNs + " presentationTimeNs " + (lastPresentationTimeUs * 1000));
            long sleepTimeNs = lastPresentationTimeUs * 1000 - passTimeNs;
            if (sleepTimeNs > 0){
                //sleep
                MLog.d(TAG, "sleepTimeNs: " + sleepTimeNs);
                try {
                    Thread.sleep(sleepTimeNs/1000000, (int)(sleepTimeNs%1000000));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (lastPresentationTimeUs > 1000000 * 10 && !hasPlayerAd){
                //TODO 更换提取器
                MediaExtractor adExtractor = new MediaExtractor();
                try {
                    adExtractor.setDataSource("http://vfx.mtime.cn/Video/2019/02/04/mp4/190204084208765161.mp4");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                for (int i=0; i<adExtractor.getTrackCount(); ++i){
                    MediaFormat mediaFormat = adExtractor.getTrackFormat(i);
                    String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                    if (mime.startsWith("video/")){
                        int videoWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                        int videoHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                        long duration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                        /*try {
                            videoDecoder = MediaCodec.createDecoderByType(mime);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/
                        adExtractor.selectTrack(i);
                        mediaCodec.stop();
                        //mediaCodec.flush();
                        mediaCodec.configure(mediaFormat, surface, null, 0);

                        Log.d(TAG, "duration: " + duration + " videoWidth: " + videoWidth + " videoHeight: " + videoHeight);
                        break;
                    }
                }

                //mediaCodec.flush();

                setAdMediaExtractor(adExtractor);
                mediaCodec.start();
                hasPlayerAd = true;
            }

            /*if (hasPlayerAd && !resume){
                mediaCodec.stop();
                mediaCodec.configure(mainMediaFormat, surface, null, 0);
                this.extractor = mainExtractor;
                mediaCodec.start();
                resume = true;
            }*/
        }else {
            MLog.d(TAG, "Rendering first frame");
            //第一帧立刻渲染
            startTimeNs = System.nanoTime();
        }
        try {
            mediaCodec.releaseOutputBuffer(outputIndex, true);
            lastPresentationTimeUs = bufferInfo.presentationTimeUs;
        }catch (Exception e){
            MLog.d(TAG, "error");
        }
    }
}
