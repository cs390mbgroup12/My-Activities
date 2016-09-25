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
        private float point;
        private long timestamp;

        public Point(float point, long timestamp){
            this.point = point;
            this.timestamp = timestamp;
        }
        public float getPoint() { return this.point; }
        public long getTime() { return this.timestamp; }
    }

    // own defined variables and methods
//    private List<Point> buffer = new ArrayList<Point>();
    private List<Point> xBuffer = new ArrayList<Point>();
    private List<Point> yBuffer = new ArrayList<Point>();
    private List<Point> zBuffer = new ArrayList<Point>();
    long windowStartTime;
    final long windowLength = 2;
    float marginError = 0;

    private float findMax (List<Point> arr){
        float max = arr.get(0).getPoint();
        for(int i = 1; i < arr.size(); i++){
            if(arr.get(i).getPoint() > max){
                max = arr.get(i).getPoint();
            }
        }
        return max;
    }

    private float findMin (List<Point> arr){
        float min = arr.get(0).getPoint();
        for(int i = 1; i < arr.size(); i++){
            if(arr.get(i).getPoint() < min){
                min = arr.get(i).getPoint();
            }
        }
        return min;
    }

    private int findAverage(List<Point> arr){
        float avg = (findMin(arr)+findMax(arr))/2;
        int avgCount = 0;
        for (int i =1; i< arr.size(); i++){
            if(arr.get(i).getPoint()+.25 > avg)
                avgCount ++;
            if(arr.get(i).getPoint()-.25 <avg)
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

            xBuffer.add(new Point(event.values[0], timestamp));
            yBuffer.add(new Point(event.values[1], timestamp));
            zBuffer.add(new Point(event.values[2], timestamp));

            if(timestamp - windowStartTime > windowLength){
                //TODO: analyze the data within buffer
                // Find max and min for each buffer and compute their ranges
                float xRange = findMax(xBuffer) - findMin(xBuffer);
                float yRange = findMax(yBuffer) - findMin(yBuffer);
                float zRange = findMax(zBuffer) - findMin(zBuffer);
                float maxRange = Math.max(Math.max(xRange, yRange), zRange);
                List<Point> buffer = new ArrayList<Point>();
                if (maxRange == xRange)
                    buffer = xBuffer;
                else
                    if (maxRange == yRange)
                        buffer = yBuffer;
                    else
                        buffer = zBuffer;

                float max = findMax(buffer);
                float min = findMin(buffer);

                marginError = (0.0500f)*(max - min);
                float average = (max + min)/2;
                float upperBound = average + marginError;
                float lowerBound = average - marginError;

                //Buffer is in order of timestamps
                boolean isHigher = (buffer.get(0).getPoint() > upperBound);
                long lastTimestamp = buffer.get(0).getTime();
//                int remainingThresholds = 3;
//                if (isHigher)
//                    remainingThresholds = 2;

                for (int i = 1; i < buffer.size(); i++) {
                    if (isHigher) {
                        if (buffer.get(i).getPoint() <= lowerBound && Math.abs(lastTimestamp - buffer.get(i).getTime()) > 0.3) {
                            isHigher = false;
                            float[] points = new float[3];
                            points[0] = xBuffer.get(i).getPoint();
                            points[1] = yBuffer.get(i).getPoint();
                            points[2] = zBuffer.get(i).getPoint();
                            onStepDetected(buffer.get(i).getTime(), points);
                            lastTimestamp = buffer.get(i).getTime();
                        }
                    }
                    else {
                        if (buffer.get(i).getPoint() > upperBound)
                            isHigher = true;
                    }
                }

                //Empty buffers for next time interval
                xBuffer = new ArrayList<Point>();
                yBuffer = new ArrayList<Point>();
                zBuffer = new ArrayList<Point>();

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
