package com.example.thwu.decodertest;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.MediaFormat;
import android.opengl.GLSurfaceView;
import android.os.*;
import android.os.Process;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.media.MediaCodec;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View.OnTouchListener;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity implements OnTouchListener, TextureView.SurfaceTextureListener{

    //Color blob detection variables
    private boolean              mIsColorSelected = false;
    private Mat                  mRgba;
    private Scalar mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private Mat                  mSpectrum;
    private Size SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;

    private static final String  TAG              = "OCVSample::Activity";
    /*static{
        System.loadLibrary("opencv_java3");
    } */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    private Handler handlerTimer = new Handler();

    private TextureView mSurfaceView;
    private TextureView mSurfaceView_Display;
    private TextView mTextView, mTextView02;
    int width = 960;
    int height = 540;
    String videoFormat = "video/avc";
    MediaFormat format = MediaFormat.createVideoFormat(videoFormat, width, height);

    int inIndex;
    int s = -1;
    boolean start = false;
    int c = 0;
    int co = 0;
    long presen_time = 0;
    int i = 0;
    int step_count = 0;

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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mSurfaceView = (TextureView) findViewById(R.id.surface_view);
        mSurfaceView_Display = (TextureView) findViewById(R.id.view02);
        //mSurfaceView = new TextureView(this);
        mTextView = (TextView) findViewById(R.id.textView);
        mTextView02 = (TextView) findViewById(R.id.textView02);

        mSurfaceView_Display.setOnTouchListener(this);

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

                for (int i=0; i<stream.length-7;i++) {
                    if (stream[i] == 0x00 && stream[i + 1] == 0x00 && stream[i + 2] == 0x00 && stream[i + 4] == 0x09) {
                        if (!start) {
                            start = true;
                            s = i;
                            i = i + 5;
                        } else {
                            inIndex = mMediaCodec.dequeueInputBuffer(0);
                            if (inIndex > 0) {
                                ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inIndex);
                                inputBuffer.put(Arrays.copyOfRange(stream, s, i), 0, i - s);
                                mMediaCodec.queueInputBuffer(inIndex, 0, i - s, 0, 0);
                                s = i;
                                i = i + 5;
                                c++;
                                //handler.sendMessage(handler.obtainMessage(SHOWTOAST, "Queued frame num: " + c));
                            } else
                                i--;

                            MediaCodec.BufferInfo bufferinfo = new MediaCodec.BufferInfo();
                            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferinfo, 0);

                            if (outputBufferIndex >= 0) {
                                co++;
                                mMediaCodec.releaseOutputBuffer(outputBufferIndex, true);

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // This code will always run on the UI thread, therefore is safe to modify UI elements.
                                        mTextView.setText(String.valueOf(co));
                                    }
                                });
                                try {
                                    Thread.sleep(45);
                                }
                                catch (Exception e){
                                    e.printStackTrace();
                                }
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
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
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
        //Detector Init
        mRgba = new Mat();
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);

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
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (step_count != 5){
                    step_count++;
                    return;
                }
                else{
                    step_count = 0;
                }
                Bitmap frame_bmp = mSurfaceView.getBitmap();
                //Mat frame_mat = new Mat();
                Utils.bitmapToMat(frame_bmp, mRgba);  // frame_bmp is in ARGB format, mRgba is in RBGA format

                //Todo: Do image processing stuff here
                mRgba.convertTo(mRgba,-1,2,0);  // Increase intensity by 2

                if (mIsColorSelected) {
                    //Show the error-corrected color
                    mBlobColorHsv = mDetector.get_new_hsvColor();
                    mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

                    //Debug
                    Log.i(TAG, "mDetector rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                            ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");
                    Log.i(TAG, "mDetector hsv color: (" + mBlobColorHsv.val[0] + ", " + mBlobColorHsv.val[1] +
                            ", " + mBlobColorHsv.val[2] + ", " + mBlobColorHsv.val[3] + ")");

                    mDetector.process(mRgba);
                    List<MatOfPoint> contours = mDetector.getContours();
                    Log.e(TAG, "Contours count: " + contours.size());
                    Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR,2);

                    Mat colorLabel = mRgba.submat(4, 68, 4, 68);
                    colorLabel.setTo(mBlobColorRgba);

                    Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
                    mSpectrum.copyTo(spectrumLabel);
                }


                Utils.matToBitmap(mRgba, frame_bmp);
                Canvas canvas = mSurfaceView_Display.lockCanvas();
                canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
                canvas.drawBitmap(frame_bmp, new Rect(0, 0, frame_bmp.getWidth(), frame_bmp.getHeight()),
                        new Rect((canvas.getWidth() - frame_bmp.getWidth()) / 2,
                                (canvas.getHeight() - frame_bmp.getHeight()) / 2,
                                (canvas.getWidth() - frame_bmp.getWidth()) / 2 + frame_bmp.getWidth(),
                                (canvas.getHeight() - frame_bmp.getHeight()) / 2 + frame_bmp.getHeight()), null);
                mSurfaceView_Display.unlockCanvasAndPost(canvas);
            }
        }).start();
    }

    public boolean onTouch(View v, MotionEvent event) {
        //Todo: to give a notification for debugging
        mTextView02.setText("Touched!!!");

        //Very important! So that the onFrame function won't change the parameters before this section finishes
        mIsColorSelected = false;

        int rows = mRgba.rows();        int cols = mRgba.cols();


        int xOffset = (mSurfaceView_Display.getWidth() - cols) / 2;
        int yOffset = (mSurfaceView_Display.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        org.opencv.core.Rect touchedRect = new org.opencv.core.Rect();

        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;

        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        mDetector.resetStart();
        mDetector.setHsvColor(mBlobColorHsv);

        Log.i(TAG, "mDetector hsv color set: (" + mBlobColorHsv.val[0] + ", " + mBlobColorHsv.val[1] +
                ", " + mBlobColorHsv.val[2] + ", " + mBlobColorHsv.val[3] + ")");

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false;
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
}
