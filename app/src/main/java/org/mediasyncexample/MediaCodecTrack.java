package org.mediasyncexample;

import android.media.MediaCodec;

import java.nio.ByteBuffer;
import java.util.LinkedList;

public abstract class MediaCodecTrack {

    protected int trackIndex;
    protected MediaCodec mediaCodec;

    protected ByteBuffer[] codecInputBuffers;
    protected ByteBuffer[] codecOutputBuffers;

    protected LinkedList<Integer> availableInputBufferIndexList = new LinkedList<>();

    public MediaCodecTrack(MediaCodec mediaCodec) {
        this.mediaCodec = mediaCodec;
        codecInputBuffers = mediaCodec.getInputBuffers();
        codecOutputBuffers = mediaCodec.getOutputBuffers();
    }

    public void doDecodeWork(){
        int indexInput = mediaCodec.dequeueInputBuffer(0);

        if (indexInput != MediaCodec.INFO_TRY_AGAIN_LATER){
            availableInputBufferIndexList.add(indexInput);
        }

        while (extractDataToCodec()){
            //do nothing
        }


        while (decodeDataToConsumer()){
            //do nothing
        }
    }

    //从提取器中取出数据，并送给codec 进行解码
    public abstract boolean extractDataToCodec();


    //从解码器中拿出解好的数据，并给消费者（Surface或AudioTrack）
    public abstract boolean decodeDataToConsumer();
}
