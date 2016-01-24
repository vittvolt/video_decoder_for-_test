package com.example.thwu.decodertest;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.media.MediaFormat;
import android.opengl.GLSurfaceView;
import android.os.*;
import android.os.Process;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.media.MediaCodec;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MainActivity extends Activity implements TextureView.SurfaceTextureListener{

    private TextureView mSurfaceView;
    int width = 960;
    int height = 540;
    String videoFormat = "video/avc";
    MediaFormat format = MediaFormat.createVideoFormat(videoFormat, width, height);

    int inIndex;
    int s = -1;
    boolean start = false;
    int c = 0;
    int co = 0;

    private MediaCodec mMediaCodec;
    private byte[] iframe = new byte[781];
    private byte[] stream = new byte[13652382];
    private boolean seq_start = false;
    private final int SHOWDIALOG = 1;
    private final int SHOWTOAST = 2;
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
                Toast.makeText(MainActivity.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurfaceView = (TextureView) findViewById(R.id.surface_view);

        InputStream is = getResources().openRawResource(R.raw.iframe_1280_3s);

        BufferedInputStream buf = new BufferedInputStream(is);
        try {
            buf.read(iframe, 0, iframe.length);
            buf.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }

        is = getResources().openRawResource(R.raw.stream_data_2500_verified);

        buf = new BufferedInputStream(is);
        try {
            buf.read(stream, 0, stream.length);
            buf.close();
            is.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }

        format.setString("KEY_MIME", videoFormat);
        //mSurfaceView.getHolder().addCallback(this);
        mSurfaceView.setSurfaceTextureListener(this);

        new Thread(){
            public void run(){
                while(!seq_start){}

                for (int i=0; i<stream.length-7;i++){
                    if (stream[i]==0x00 && stream[i+1]==0x00 && stream[i+2]==0x00 && stream[i+4]==0x09){
                        if (!start){
                            start = true;
                            s = i;
                            i = i + 5;
                        }
                        else{
                            inIndex = mMediaCodec.dequeueInputBuffer(0);
                            if (inIndex > 0){
                                ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inIndex);
                                inputBuffer.put(Arrays.copyOfRange(stream,s,i), 0, i-s);
                                mMediaCodec.queueInputBuffer(inIndex, 0, i - s, 0, 0);
                                s = i;
                                i = i + 5;
                                c++;
                                //handler.sendMessage(handler.obtainMessage(SHOWTOAST, "Queued frame num: " + c));
                            }
                            else
                                i--;

                            MediaCodec.BufferInfo bufferinfo = new MediaCodec.BufferInfo();
                            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferinfo, 0);

                            if (outputBufferIndex >= 0) {
                                co++;
                                mMediaCodec.releaseOutputBuffer(outputBufferIndex, true);
                                //handler.sendMessage(handler.obtainMessage(SHOWTOAST, "Decoded frame number: " + co + " And queued frame: " + c));
                            }
                        }
                    }
                }
            }
        }.start();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.os.Process.killProcess(Process.myPid());
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        try {
            mMediaCodec = MediaCodec.createDecoderByType(videoFormat);
            mMediaCodec.configure(format, new Surface(surface), null, 0);
            mMediaCodec.start();

            new Thread() {
                public void run() {
                    int ind = mMediaCodec.dequeueInputBuffer(0);
                    while (ind < 0)
                        ind = mMediaCodec.dequeueInputBuffer(0);
                    ByteBuffer b = mMediaCodec.getInputBuffer(ind);
                    b.put(iframe, 0, iframe.length);
                    mMediaCodec.queueInputBuffer(ind, 0, iframe.length, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                    handler.sendMessage(handler.obtainMessage(SHOWTOAST, "I-frame queued!!!"));
                    seq_start = true;
                }
            }.start();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // TODO Auto-generated method stub

    }

    /*public void surfaceChanged(SurfaceHolder holder, int format, int w, int h){

    }

    public void surfaceDestroyed(SurfaceHolder holder){

    }  */
}
