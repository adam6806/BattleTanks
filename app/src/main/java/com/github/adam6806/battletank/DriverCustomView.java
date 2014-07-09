package com.github.adam6806.battletank;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class DriverCustomView extends View implements SensorEventListener {
    ArrayList<DriverListener> listeners = new ArrayList<DriverListener>();

    private static final int SIZE = 150;
    public float[] gravSens;

    private SparseArray<PointF> mActivePointers;
    private Paint mPaint;
    private Paint backgroundPaint;
    private int[] colors =
            {Color.RED, Color.GREEN};

    public int red, green, blue;

    private Paint textPaint;

    public DriverCustomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView() {
        mActivePointers = new SparseArray<PointF>();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // set painter color to a color you like
        mPaint.setColor(Color.BLUE);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(30);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(25);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(100);
        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            Sensor gravSensor = event.sensor;
            gravSens = event.values;
            Log.d("Sensor","Changing, X:" + gravSens[0] + " Y:" + gravSens[1] + " Z:" + gravSens[2]);
            for (DriverListener listener : listeners)
            {
                listener.setText(gravSens[0], gravSens[1], gravSens[2]);
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void setListener(DriverListener listener)
    {
        listeners.add(listener);
    }
}