package org.mediasyncexample;

import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

public class MediaCodecAudioTrack extends MediaCodecTrack{

    private static final String TAG = "MediaCodecAudioTrack";

    private final MediaExtractor extractor;
    private final AudioTrack audioTrack;

    public MediaCodecAudioTrack(MediaCodec mediaCodec, MediaExtractor extractor, AudioTrack audioTrack) {
        super(mediaCodec);
        this.extractor = extractor;
        this.audioTrack = audioTrack;
    }

    @Override
    public void start() {
        super.start();
        audioTrack.play();
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

    }

    @Override
    public void onOutputBufferAvailable(int outputIndex, MediaCodec.BufferInfo bufferInfo) {
        if (MediaCodec.BUFFER_FLAG_END_OF_STREAM == bufferInfo.flags){
            MLog.d(TAG, "onOutputBufferAvailable BUFFER_FLAG_END_OF_STREAM");
        }

        ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outputIndex);

        if (byteBuffer != null) {
            int written = audioTrack.write(byteBuffer, bufferInfo.size, AudioTrack.WRITE_BLOCKING);
            MLog.d(TAG, "onOutputBufferAvailable written: " + written + " buffer size: " + bufferInfo.size);
        }
        mediaCodec.releaseOutputBuffer(outputIndex, false);
    }
}
