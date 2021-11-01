package org.mediasyncexample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class PlayerActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    //private KkMediaPlayer mediaPlayer = new KkMediaPlayer();
    private MediaCodePlayer mediaPlayer = new MediaCodePlayer();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        SurfaceView msvPlay = (SurfaceView) findViewById(R.id.svPlay);
        SurfaceHolder sh = msvPlay.getHolder();
        sh.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mediaPlayer.setSurface(surfaceHolder.getSurface());
        mediaPlayer.setDataSource("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4");
        mediaPlayer.prepare();
        mediaPlayer.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }
}