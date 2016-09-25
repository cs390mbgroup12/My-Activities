package cs.umass.edu.myactivitiestoolkit.steps;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.lang.Math.*;

import cs.umass.edu.myactivitiestoolkit.processing.Filter;

/**
 * This class is responsible for detecting steps from the accelerometer sensor.
 * All {@link OnStepListener step listeners} that have been registered will
 * be notified when a step is detected.
 */
public class StepDetector implements SensorEventListener {
    /** Used for debugging purposes. */
    @SuppressWarnings("unused")
    private static final String TAG = StepDetector.class.getName();

    /** Maintains the set of listeners registered to handle step events. **/
    private ArrayList<OnStepListener> mStepListeners;

    /**
     * The number of steps taken.
     */
    private int stepCount;

    public StepDetector(){
        mStepListeners = new ArrayList<>();
        stepCount = 0;
    }

    /**
     * Registers a step listener for handling step events.
     * @param stepListener defines how step events are handled.
     */
    public void registerOnStepListener(final OnStepListener stepListener){
        mStepListeners.add(stepListener);
    }

    /**
     * Unregisters the specified step listener.
     * @param stepListener the listener to be unregistered. It must already be registered.
     */
    public void unregisterOnStepListener(final OnStepListener stepListener){
        mStepListeners.remove(stepListener);
    }

    /**
     * Unregisters all step listeners.
     */
    public void unregisterOnStepListeners(){
        mStepListeners.clear();
    }

    private class Point{
        private float x;
        private float y;
        private float z;
        public Point(float x, float y, float z){
            this.x = x;
            this.y = y;
            this.z = z;
        }
        public float getX(){
            return this.x;
        }
        public float getY(){
            return this.y;
        }
        public float getZ(){
            return this.z;
        }
    }

    // own defined variables and methods
//    private List<Point> buffer = new ArrayList<Point>();
    private List<Float> xBuffer = new ArrayList<Float>();
    private List<Float> yBuffer = new ArrayList<Float>();
    private List<Float> zBuffer = new ArrayList<Float>();
    long windowStartTime;
    final long windowLength = 2;
    final float marginError = 1;

    private float findMax (List<Float> arr){
        float max = arr.get(0);
        for(int i = 1; i < arr.size(); i++){
            if(arr.get(i) > max){
                max = arr.get(i);
            }
        }
        return max;
    }

    private float findMin (List<Float> arr){
        float min = arr.get(0);
        for(int i = 1; i < arr.size(); i++){
            if(arr.get(i) < min){
                min = arr.get(i);
            }
        }
        return min;
    }

    private int findAverage(List<Float> arr){
        float avg = (findMin(arr)+findMax(arr))/2;
        int avgCount = 0;
        for (int i =1; i< arr.size(); i++){
            if(arr.get(i)+.25 > avg)
                avgCount ++;
            if(arr.get(i)-.25 <avg)
                avgCount ++;
        }
        return avgCount;
    }

    /////////////////////////////////////
    /**
     * Here is where you will receive accelerometer readings, buffer them if necessary
     * and run your step detection algorithm. When a step is detected, call
     * {@link #onStepDetected(long, float[])} to notify all listeners.
     *
     * Recall that human steps tend to take anywhere between 0.5 and 2 seconds.
     *
     * @param event sensor reading
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long timestamp = event.timestamp;

            if(xBuffer.isEmpty()){
                windowStartTime = timestamp;
            }

            xBuffer.add(event.values[0]);
            yBuffer.add(event.values[1]);
            zBuffer.add(event.values[2]);

            if(timestamp - windowStartTime > windowLength){
                //TODO: analyze the data within buffer
                // Find max and min for each buffer and compute their ranges
                float xRange = findMax(xBuffer) - findMin(xBuffer);
                float yRange = findMax(yBuffer) - findMin(yBuffer);
                float zRange = findMax(zBuffer) - findMin(zBuffer);
                float maxRange = Math.max(Math.max(xRange, yRange), zRange);
                List<Float> buffer = new ArrayList<Float>();
                if (maxRange == xRange)
                    buffer = xBuffer;
                else
                    if (maxRange == yRange)
                        buffer = yBuffer;
                    else
                        buffer = zBuffer;

                float max = findMax(buffer);
                float min = findMin(buffer);
                float average = (max + min)/2;

//                if(findAverage(buffer) <3 && findAverage(buffer)>1){
//                    stepCount++;
//                    onStepDetected(windowLength,);//??????????);
//                }




            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // do nothing
    }

    /**
     * This method is called when a step is detected. It updates the current step count,
     * notifies all listeners that a step has occurred and also notifies all listeners
     * of the current step count.
     */
    private void onStepDetected(long timestamp, float[] values){
        stepCount++;
        for (OnStepListener stepListener : mStepListeners){
            stepListener.onStepDetected(timestamp, values);
            stepListener.onStepCountUpdated(stepCount);
        }
    }
}
