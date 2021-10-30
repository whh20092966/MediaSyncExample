package org.mediasyncexample;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;

public class MediaCodecVideoTrack extends MediaCodecTrack{
    private static final String TAG = "MediaCodecVideoTrack";

    private final MediaExtractor extractor;

    public MediaCodecVideoTrack(MediaCodec mediaCodec, MediaExtractor extractor) {
        super(mediaCodec);
        this.extractor = extractor;
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
        }else {
            MLog.d(TAG, "Rendering first frame");
            //第一帧立刻渲染
            startTimeNs = System.nanoTime();
        }
        mediaCodec.releaseOutputBuffer(outputIndex, true);
        lastPresentationTimeUs = bufferInfo.presentationTimeUs;
    }
}
