package cn.sencs.client;


import android.content.Context;
import android.content.Intent;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;



import static android.content.Context.SENSOR_SERVICE;

/**
 * Created by sensj on 2017/12/12.
 */

public class MySensor implements SensorEventListener{
    private SensorManager msensorManager;
    private Sensor msensor;

    private Context mContext;
    private MainActivity mActivityInstance;
    public MySensor(Context context){
        super();
        mContext=context;
        mActivityInstance=(MainActivity)mContext;
    }

    public void start(){
        msensorManager=(SensorManager)mActivityInstance.getSystemService( SENSOR_SERVICE);
        msensor= msensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        msensorManager.registerListener(this, msensor,SensorManager.SENSOR_DELAY_GAME);//注册监听器

    }

    public void stop(){
        if(msensorManager!=null){
            msensorManager.unregisterListener(this);
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor==null){
            return;
        }
        if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            int x=(int)event.values[0];
            int y=(int)event.values[1];
            int z=(int)event.values[2];
            Intent intent = new Intent();
            if (x>-1 && x<1 &&y>-1 &&y<1 && z>8) {
                //output=("停止");intent.putExtra("result", output);
                MyControler.bluetoothmsg="x" ;
                //SensorActivity.this.setResult(1, intent);SensorActivity.this.finish();
            }else if(x<-3 &&y>-2&& y<2 && z<8) {
                //output=("前进");intent.putExtra("result", output);
                MyControler.bluetoothmsg="w" ;
                //SensorActivity.this.setResult(1, intent);//SensorActivity.this.finish();
            }else if(x>3 &&y>-2&& y<2 && z<8) {
                //output=("后退");intent.putExtra("result", output);
                MyControler.bluetoothmsg="s" ;
                //SensorActivity.this.setResult(1, intent);//SensorActivity.this.finish();
            }else if(y<-4 &&x>-2&& x<2 && z<8){
                //output=("左转");intent.putExtra("result", output);
                MyControler.bluetoothmsg="a" ;
                //SensorActivity.this.setResult(1, intent);//SensorActivity.this.finish();
            }else if(y>4 &&x>-2&& x<2 && z<8){
                //output=("右转");intent.putExtra("result", output);
                MyControler.bluetoothmsg="d" ;
                //SensorActivity.this.setResult(1, intent);//SensorActivity.this.finish();
            }
        }
    }

}
