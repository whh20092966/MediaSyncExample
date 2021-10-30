package org.mediasyncexample;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;


public abstract class MediaCodecTrack {
    private static final String TAG = "MediaCodePlayer";
    protected int trackIndex;
    protected MediaCodec mediaCodec;
    protected long startTimeNs = 0;

    public MediaCodecTrack(MediaCodec mediaCodec) {
        this.mediaCodec = mediaCodec;
    }

    public void start(){
        mediaCodec.start();
    }

    public void doDecodeWork(){
        int indexInput = mediaCodec.dequeueInputBuffer(10000);
        Log.d(TAG, "doDecodeWork indexInput " + indexInput);
        if (indexInput == MediaCodec.INFO_TRY_AGAIN_LATER){
            return;
        }

        onInputBufferAvailable(indexInput);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int indexOutput = mediaCodec.dequeueOutputBuffer(info, 0 /* timeoutUs */);

        Log.d(TAG, "doDecodeWork indexOutput " + indexOutput);

        if (indexOutput == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
            Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED ");
        }else if (indexOutput == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
            Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED ");
            MediaFormat mediaFormat = mediaCodec.getOutputFormat();
            onFormatChanged(mediaFormat);
        } else if (indexOutput != MediaCodec.INFO_TRY_AGAIN_LATER) {
            onOutputBufferAvailable(indexOutput, info);
        }else {
            Log.d(TAG, "unknown indexOutput " + indexInput);
        }
    }


    public abstract void onInputBufferAvailable(int inputIndex);

    public abstract void onFormatChanged(MediaFormat newMediaFormat);

    public abstract void onOutputBufferAvailable(int outputIndex, MediaCodec.BufferInfo bufferInfo);

}
