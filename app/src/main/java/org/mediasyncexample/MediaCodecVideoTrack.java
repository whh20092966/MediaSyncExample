package org.mediasyncexample;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

public class MediaCodecVideoTrack extends MediaCodecTrack{


    public MediaCodecVideoTrack(MediaCodec mediaCodec) {
        super(mediaCodec);
    }

    @Override
    public boolean extractDataToCodec() {

        if (availableInputBufferIndexList.isEmpty()){
            return false;
        }

        int index = availableInputBufferIndexList.peekFirst();
        ByteBuffer byteBuffer = codecInputBuffers[index];




        return false;
    }

    @Override
    public boolean decodeDataToConsumer() {
        return false;
    }
}
