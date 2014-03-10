package com.aizulab.ooba.compass.compass;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.lang.ref.WeakReference;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceView;
import android.widget.Toast;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;

public class MainActivity extends Activity implements SensorEventListener {
    //方位センサー関係
    private SensorManager manager;
    private boolean isSensorResisted = false;
    private static float orientation;
    private Bitmap needleImg;
    private static boolean isAlive;

    //SurfaceViewへの描画関係
    private SurfaceView surface;

    //unchecked//





    //SurfaceViewを描画するHandler
    private static class MyHandler extends Handler{
        static final int WHAT = 1;

        private SurfaceView surface;
        private Bitmap needleImg;
        private float centerX;
        private float centerY;
        private float drawX;
        private float drawY;
        private boolean isFast = true;

        public void SetMe(WeakReference<MainActivity> me){
            this.surface = me.get().surface;
            this.needleImg = me.get().needleImg;
        }

        @Override
        public void handleMessage(Message msg) {
            removeMessages(WHAT);

            if ( ! isAlive){
                //フラグオフなら終了
                return;
            }

            Canvas canvas = surface.getHolder().lockCanvas();
            if (canvas == null) {
                //SurfaceViewの準備が終わってなければ時間を空けてリトライ
                sendEmptyMessageDelayed(WHAT, 10);
                return;
            }

            //最初の1回だけの処理
            if (isFast) {
                //中心座標を計算
                centerX = surface.getWidth() / 2;
                centerY = surface.getHeight() / 2;
                drawX = centerX - needleImg.getWidth() / 2;
                drawY = centerY - needleImg.getHeight() / 2;
                isFast = false;
            }

            //背景を白く塗りつぶし
            canvas.drawColor(Color.WHITE);

            //矢印を方角に向かって描画
            canvas.save();
            canvas.rotate(-orientation, centerX, centerY);
            canvas.drawBitmap(needleImg, drawX, drawY, null);
            canvas.restore();

            surface.getHolder().unlockCanvasAndPost(canvas);
        }
    };








    private MyHandler myHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //画面の向きを縦に固定
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        surface = new SurfaceView(getApplicationContext());
        setContentView(surface);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //センサーの起動
        if (manager == null) {
            manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        }
        List<Sensor> sensors = manager.getSensorList(Sensor.TYPE_ORIENTATION);
        if (sensors.size() > 0) {
            isSensorResisted = manager.registerListener(this, sensors.get(0), SensorManager.SENSOR_DELAY_FASTEST);

            //針の画像をロード
            needleImg = BitmapFactory.decodeResource(getResources(), R.drawable.needle);

            //ループフラグ・オン
            isAlive = true;

            //Handlerを起動
            myHandler = new MyHandler();
            myHandler.SetMe(new WeakReference<MainActivity>(this));
        } else {
            //方位磁石センサーがなければメッセージを表示
            Toast.makeText(getApplicationContext(), "No Orientaion Sensor!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        //センサーの終了
        if (isSensorResisted) {
            manager.unregisterListener(this);
            isSensorResisted = false;

            //ループフラグ・オフ
            isAlive = false;

            //Handlerにメッセージが残っていれば削除
            myHandler.removeMessages(MyHandler.WHAT);

            //針画像の解放
            needleImg.recycle();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ORIENTATION:
                //北向きをゼロ度とする方位の角度
                orientation = event.values[0];

                //Handlerで針画像の描画
                myHandler.sendEmptyMessage(MyHandler.WHAT);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
