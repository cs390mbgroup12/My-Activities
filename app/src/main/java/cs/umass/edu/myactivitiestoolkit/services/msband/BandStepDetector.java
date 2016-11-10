package cs.umass.edu.myactivitiestoolkit.services.msband;

import android.util.Log;

import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;

import java.util.ArrayList;
import java.util.List;

import cs.umass.edu.myactivitiestoolkit.steps.OnStepListener;

public class BandStepDetector implements BandAccelerometerEventListener {
    /** Used for debugging purposes. */
    @SuppressWarnings("unused")
    private static final String TAG = BandStepDetector.class.getName();

    /** Maintains the set of listeners registered to handle step events. **/
    private ArrayList<OnStepListener> mStepListeners;

    /**
     * The number of steps taken.
     */
    private int stepCount;

    public BandStepDetector(){
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
    private List<Point> xBuffer = new ArrayList<Point>(500);
    private List<Point> yBuffer = new ArrayList<Point>(500);
    private List<Point> zBuffer = new ArrayList<Point>(500);
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

    public void onBandAccelerometerChanged(BandAccelerometerEvent event) {
            if(xBuffer.isEmpty()){
                windowStartTime = System.currentTimeMillis();
            }

            long currentTime = System.currentTimeMillis();

            xBuffer.add(new Point(event.getAccelerationX(), currentTime));
            yBuffer.add(new Point(event.getAccelerationY(), currentTime));
            zBuffer.add(new Point(event.getAccelerationZ(), currentTime));

            if(currentTime - windowStartTime > windowLength){
                //TODO: analyze the data within buffer
//                Log.d(TAG, "in loop to process steps");
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
                boolean isHigher = (xBuffer.get(0).getPoint() > upperBound);
                long lastTimestamp = xBuffer.get(0).getTime();
//                int remainingThresholds = 3;
//                if (isHigher)
//                    remainingThresholds = 2;
                for (int i = 1; i < xBuffer.size(); i++) {
                    if (isHigher) {
                        if (xBuffer.get(i).getPoint() <= lowerBound && Math.abs(lastTimestamp - xBuffer.get(i).getTime()) > 0.3) {
                            if ((max - min) > .2) {
//                                Log.d(TAG, "found a step");
                                isHigher = false;
                                float[] points = new float[3];
                                points[0] = xBuffer.get(i).getPoint();
                                points[1] = yBuffer.get(i).getPoint();
                                points[2] = zBuffer.get(i).getPoint();
                                Log.d(TAG, "Found step at time: " + buffer.get(i).getTime() + "   Current time: " + currentTime);
                                onStepDetected(xBuffer.get(i).getTime(), points);
                                lastTimestamp =xBuffer.get(i).getTime();
                            }
                        }
                    }
                    else {
                        if (xBuffer.get(i).getPoint() > upperBound)
                            isHigher = true;
                    }
                }

                //Empty buffers for next time interval
                xBuffer = new ArrayList<Point>(500);
                yBuffer = new ArrayList<Point>(500);
                zBuffer = new ArrayList<Point>(500);

//                if(findAverage(buffer) <3 && findAverage(buffer)>1){
//                    stepCount++;
//                    onStepDetected(windowLength,);//??????????);
//                }




            }
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
