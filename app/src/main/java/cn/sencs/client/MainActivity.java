package cn.sencs.client;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.net.Socket;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;
import java.io.File;

import cn.sencs.nfcsocket.NfcServerSocket;
import cn.sencs.nfcsocket.NfcClientSocket;

import com.gcssloop.widget.RockerView;


public class MainActivity extends AppCompatActivity {
    private static final String SD_PATH = "/sdcard/DCIM/pic/";
    private static final String IN_PATH = "/DCIM/pic/";

    public TextView clientStatus;
    private ImageView mCameraView;
    public static String SERVERIP = "192.168.1.106";
    public static final int SERVERPORT = 9192;
    public MyClientThread mClient;
    public Bitmap mLastFrame;

    private int face_count;
    private final Handler handler = new MyHandler(this);

    private FaceDetector mFaceDetector = new FaceDetector(320, 240, 10);
    private FaceDetector.Face[] faces = new FaceDetector.Face[10];
    private PointF tmp_point = new PointF();
    private Paint tmp_paint = new Paint();
    private Button nfcbtn;

    private MyControler controler;
    private RockerView rocker;

    private Button voiceBtn;
    private Thread cThread;
    private CheckBox cBox;
    private Button takepic;

    private MySensor sensor;

    public TextView textInf;

    private MyVoice voice;
    private TextView nfcinf;


    private Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mLastFrame != null) {

                            Bitmap mutableBitmap = mLastFrame.copy(Bitmap.Config.RGB_565, true);
                            face_count = mFaceDetector.findFaces(mLastFrame, faces);
//                            if(face_count>0){
//                                MyControler.bluetoothmsg="x";
//                            }
                            Log.d("Face_Detection", "Face Count: " + String.valueOf(face_count));
                            Canvas canvas = new Canvas(mutableBitmap);

                            for (int i = 0; i < face_count; i++) {
                                FaceDetector.Face face = faces[i];
                                tmp_paint.setColor(Color.RED);
                                tmp_paint.setAlpha(100);
                                face.getMidPoint(tmp_point);
                                canvas.drawCircle(tmp_point.x, tmp_point.y, face.eyesDistance(),
                                        tmp_paint);
                            }

                            mCameraView.setImageBitmap(mutableBitmap);
                        }

                    }
                }); //this function can change value of mInterval.
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                handler.postDelayed(mStatusChecker, 1000 / 15);
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraView = (ImageView) findViewById(R.id.camera_preview);
        clientStatus = (Button) findViewById(R.id.textView);
        nfcbtn = (Button) findViewById(R.id.nfcButton);
        rocker = (RockerView) findViewById(R.id.rock);
        textInf = (TextView) findViewById(R.id.info);
        voiceBtn = (Button) findViewById(R.id.voiceBtn);
        cBox = (CheckBox) findViewById(R.id.gravity);
        controler = new MyControler();
        takepic = (Button) findViewById(R.id.takepic);
        nfcinf = (TextView) findViewById(R.id.nfcinf);
        final Context context = (Context) this;
        takepic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("takepic", "begine");
                if (mLastFrame != null) {
                    final String s = saveBitmap(context, mLastFrame);
                    Toast toa = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                    toa.show();
                    Log.i("takepic", "end");

                }
            }
        });

        nfcbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NfcClientSocket.getInstance(getApplicationContext())
                        .unregister(clientListener);
                NfcServerSocket.getInstance(getApplicationContext())
                        .setListener(serverListener);
                NfcServerSocket.getInstance(getApplicationContext()).listen();
            }
        });

        rocker.setListener(new RockerView.RockerListener() {
            @Override
            public void callback(int eventType, int currentAngle, float currentDistance) {
                switch (eventType) {
                    case RockerView.EVENT_ACTION:
                        if (currentAngle > 315 || 0 < currentAngle && currentAngle <= 45) {
                            MyControler.bluetoothmsg = "d";
                        } else if (45 < currentAngle && currentAngle < 135) {
                            MyControler.bluetoothmsg = "w";
                        } else if (135 <= currentAngle && currentAngle < 225) {
                            MyControler.bluetoothmsg = "a";
                        } else if (225 <= currentAngle && currentAngle <= 315) {
                            MyControler.bluetoothmsg = "s";
                        } else {
                            MyControler.bluetoothmsg = "x";
                        }
                        break;
                    case RockerView.EVENT_CLOCK:
                        break;
                }
            }
        });

        sensor = new MySensor(this);

        cBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    sensor.start();
                } else {
                    sensor.stop();
                    MyControler.bluetoothmsg = "x";
                }
            }
        });


        cThread = new Thread(controler);
//        cThread.start();

        clientStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (controler.isConnected()) {
                    clientStatus.setText("Bluetooth Connected");
                    if (!cThread.isAlive())
                        cThread.start();
                } else {
                    controler.Connect();
                    clientStatus.setText("Bluetooth Unconnected");
//                    cThread.start();
                }
            }
        });

        voice = new MyVoice(this);


        voiceBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    voice.start();
//                    textInf.setText("D");
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    voice.stop();
//                    textInf.setText("U");
                }
                return false;
            }
        });


    }


    private NfcClientSocket.NfcClientSocketListener clientListener
            = new NfcClientSocket.NfcClientSocketListener() {

        @Override
        public void onDiscoveryTag() {
            Log.d("BTR", "tag!");
        }

        @Override
        public Activity getCurrentActivity() {
            return MainActivity.this;
        }
    };

    private NfcServerSocket.NfcServerSocketListener serverListener
            = new NfcServerSocket.NfcServerSocketListener() {

        @Override
        public byte[] onSelectMessage(byte[] message) {
            return "welcome".getBytes();
        }

        @SuppressLint("StaticFieldLeak")
        @Override
        public byte[] onMessage(byte[] message) {
            SERVERIP = new String(message);
            if (message != null)
                nfcinf.setText(SERVERIP);
            Log.i("NFC", "On message");
//            clientStatus.setText("Receiving from IP: " + SERVERIP);
            if (isIP(SERVERIP)) {
                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... unused) {
                        // Background Code
                        Socket s;

                        try {
                            s = new Socket(SERVERIP, SERVERPORT);
                            mClient = new MyClientThread(s, handler);
                            new Thread(mClient).start();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        return null;
                    }

                }.execute();
                mStatusChecker.run();
            }
            return "I know".getBytes();
        }


    };

    public boolean isIP(String addr) {
        if (addr.length() < 7 || addr.length() > 15 || "".equals(addr)) {
            return false;
        }
        /**
         * 判断IP格式和范围
         */
        String rexp = getString(R.string.ipformat);
        Pattern pat = Pattern.compile(rexp);
        Matcher mat = pat.matcher(addr);
        boolean ipAddress = mat.find();
        //============对之前的ip判断的bug在进行判断
        if (ipAddress) {
            String ips[] = addr.split("\\.");
            if (ips.length == 4) {
                try {
                    for (String ip : ips) {
                        if (Integer.parseInt(ip) < 0 || Integer.parseInt(ip) > 255) {
                            return false;
                        }
                    }
                } catch (Exception e) {
                    return false;
                }
                return true;
            } else {
                return false;
            }
        }
        return false;
    }


    public static Bitmap rotateImage(Bitmap source, float angle) {
        if (source != null) {
            Bitmap retVal;

            Matrix matrix = new Matrix();
            matrix.postRotate(angle);
            retVal = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
            source.recycle();
            return retVal;
        }
        return null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        NfcClientSocket.getInstance(getApplicationContext()).unregister(
                clientListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        NfcClientSocket.getInstance(getApplicationContext()).register(
                clientListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        voice.close();
    }

    private static String generateFileName() {
        return UUID.randomUUID().toString();
    }

    public static String saveBitmap(Context context, Bitmap mBitmap) {
        String savePath;
        File filePic;
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            savePath = SD_PATH;
        } else {
            savePath = context.getApplicationContext().getFilesDir()
                    .getAbsolutePath()
                    + IN_PATH;
        }
        try {
            filePic = new File(savePath + generateFileName() + ".jpg");
            if (!filePic.exists()) {
                filePic.getParentFile().mkdirs();
                filePic.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(filePic);
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return filePic.getAbsolutePath();
    }
}
