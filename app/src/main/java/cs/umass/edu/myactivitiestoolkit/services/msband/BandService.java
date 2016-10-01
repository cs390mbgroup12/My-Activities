package cs.umass.edu.myactivitiestoolkit.services.msband;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;
import com.microsoft.band.sensors.SampleRate;

import cs.umass.edu.myactivitiestoolkit.R;
import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.services.SensorService;
import cs.umass.edu.myactivitiestoolkit.steps.OnStepListener;
import cs.umass.edu.myactivitiestoolkit.steps.StepDetector;
import edu.umass.cs.MHLClient.sensors.AccelerometerReading;
import edu.umass.cs.MHLClient.sensors.GyroscopeReading;

/**
 * The BandService is responsible for starting and stopping the sensors on the Band and receiving
 * accelerometer and gyroscope data periodically. It is a foreground service, so that the user
 * can close the application on the phone and continue to receive data from the wearable device.
 * Because the {@link BandGyroscopeEvent} also receives accelerometer readings, we only need to
 * register a {@link BandGyroscopeEventListener} and no {@link BandAccelerometerEventListener}.
 * This should be compatible with both the Microsoft Band and Microsoft Band 2.
 *
 * @author Sean Noran
 *
 * @see Service#startForeground(int, Notification)
 * @see BandClient
 * @see BandGyroscopeEventListener
 */
public class BandService extends SensorService implements BandGyroscopeEventListener {

    /** used for debugging purposes */
    private static final String TAG = BandService.class.getName();

    /** The object which receives sensor data from the Microsoft Band */
    private BandClient bandClient = null;

    @Override
    protected void onServiceStarted() {
        broadcastMessage(Constants.MESSAGE.BAND_SERVICE_STARTED);
    }

    @Override
    protected void onServiceStopped() {
        broadcastMessage(Constants.MESSAGE.BAND_SERVICE_STOPPED);
    }

    private int localStepCount = 0;
    public class myStepListener implements OnStepListener{
        public void onStepCountUpdated(int stepCount){
            localStepCount = stepCount ;
            Log.d(TAG, "Testing: localstepcount is " + localStepCount);
            broadcastLocalStepCount(localStepCount);
        }
        public void onStepDetected(long timestamp, float[] values){
            broadcastStepDetected(timestamp, values);
        }
    }

    /**
     * Asynchronous task for connecting to the Microsoft Band accelerometer and gyroscope sensors.
     * Errors may arise if the Band does not support the Band SDK version or the Microsoft Health
     * application is not installed on the mobile device.
     **
     * @see com.microsoft.band.BandErrorType#UNSUPPORTED_SDK_VERSION_ERROR
     * @see com.microsoft.band.BandErrorType#SERVICE_ERROR
     * @see BandClient#getSensorManager()
     * @see com.microsoft.band.sensors.BandSensorManager
     */
    private class SensorSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    broadcastStatus(getString(R.string.status_connected));
                    bandClient.getSensorManager().registerGyroscopeEventListener(BandService.this, SampleRate.MS16);
                    //////////////////////////////////////////////////
                    BandStepDetector mStepDetector = new BandStepDetector();
                    bandClient.getSensorManager().registerAccelerometerEventListener(mStepDetector, SampleRate.MS16);
                    myStepListener mStepListener = new myStepListener();
                    mStepDetector.registerOnStepListener(mStepListener);


//                    mSensorManager.registerListener(mStepDetector, mAccelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
//                    OnStepListener sListen = new myStepListener();
//                    mStepDetector.registerOnStepListener(sListen);
                } else {
                    broadcastStatus(getString(R.string.status_not_connected));
                }
            } catch (BandException e) {
                String exceptionMessage;
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = getString(R.string.err_unsupported_sdk_version);
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = getString(R.string.err_service);
                        break;
                    default:
                        exceptionMessage = getString(R.string.err_default) + e.getMessage();
                        break;
                }
                Log.e(TAG, exceptionMessage);
                broadcastStatus(exceptionMessage);

            } catch (Exception e) {
                broadcastStatus(getString(R.string.err_default) + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Connects the mobile device to the Microsoft Band
     * @return True if successful, False otherwise
     * @throws InterruptedException if the connection is interrupted
     * @throws BandException if the band SDK version is not compatible or the Microsoft Health band is not installed
     */
    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (bandClient == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                broadcastStatus(getString(R.string.status_not_paired));
                return false;
            }
            bandClient = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == bandClient.getConnectionState()) {
            return true;
        }

        broadcastStatus(getString(R.string.status_connecting));
        return ConnectionState.CONNECTED == bandClient.connect().await();
    }

    @Override
    protected void registerSensors() {
        new SensorSubscriptionTask().execute();

    }

    /**
     * unregisters the sensors from the sensor service
     */
    @Override
    public void unregisterSensors() {
        if (bandClient != null) {
            try {
                bandClient.getSensorManager().unregisterAllListeners();
                disconnectBand();
            } catch (BandIOException e) {
                broadcastStatus(getString(R.string.err_default) + e.getMessage());
            }
        }
    }

    @Override
    protected int getNotificationID() {
        return Constants.NOTIFICATION_ID.ACCELEROMETER_SERVICE;
    }

    @Override
    protected String getNotificationContentText() {
        return getString(R.string.activity_service_notification);
    }

    @Override
    protected int getNotificationIconResourceID() {
        return R.drawable.ic_running_white_24dp;
    }

    /**
     * disconnects the sensor service from the Microsoft Band
     */
    public void disconnectBand() {
        if (bandClient != null) {
            try {
                bandClient.disconnect().await();
            } catch (InterruptedException | BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
    }

    @Override
    public void onBandGyroscopeChanged(BandGyroscopeEvent event) {
        Object[] data = new Object[]{event.getTimestamp(),
                event.getAccelerationX(), event.getAccelerationY(), event.getAccelerationZ(),
                event.getAngularVelocityX(), event.getAngularVelocityY(), event.getAngularVelocityZ()};
        mClient.sendSensorReading(new AccelerometerReading(mUserID, "", "", event.getTimestamp(),
                event.getAccelerationX(), event.getAccelerationY(), event.getAccelerationZ()));
        broadcastAccelerometerReading(event.getTimestamp(),
                event.getAccelerationX(), event.getAccelerationY(), event.getAccelerationZ());
        mClient.sendSensorReading(new GyroscopeReading(mUserID, "", "", event.getTimestamp(),
                event.getAngularVelocityX(), event.getAngularVelocityY(), event.getAngularVelocityZ()));


        String sample = TextUtils.join(",", data);
        Log.d(TAG, sample);
    }

    public void broadcastAccelerometerReading(final long timestamp, final float... accelerometerReadings) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.TIMESTAMP, timestamp);
        intent.putExtra(Constants.KEY.ACCELEROMETER_DATA, accelerometerReadings);
        intent.setAction(Constants.ACTION.BROADCAST_ACCELEROMETER_DATA);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }

    /////////////////////////////////////////////////////////////////////////////////////
    /**
     * Broadcasts the step count computed by your step detection algorithm
     * to other application components, e.g. the main UI.
     */
    public void broadcastLocalStepCount(int stepCount) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.STEP_COUNT, stepCount);
        intent.setAction(Constants.ACTION.BROADCAST_LOCAL_STEP_COUNT);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }

    public void broadcastStepDetected(long timestamp, float[] values) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.ACCELEROMETER_PEAK_TIMESTAMP, timestamp);
        intent.putExtra(Constants.KEY.ACCELEROMETER_PEAK_VALUE, values);
        intent.setAction(Constants.ACTION.BROADCAST_ACCELEROMETER_PEAK);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }
}