package cs.umass.edu.myactivitiestoolkit.services.msband;

import android.util.Log;

import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kawo123 on 10/22/2016.
 */
public class BandHeartRateDetector implements BandHeartRateEventListener{

    private static final String TAG = BandStepDetector.class.getName();

    @Override
    public void onBandHeartRateChanged(BandHeartRateEvent bandHeartRateEvent) {

    }
}
