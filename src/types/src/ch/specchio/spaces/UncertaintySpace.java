package ch.specchio.spaces;
import javax.xml.bind.annotation.*;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="uncertainty_space")
public class UncertaintySpace extends SensorAndInstrumentSpace {

    public int uncertainty_set_id;

    public UncertaintySpace() {
        super();
        setOrderBy(null);
    }

    public UncertaintySpace(int sensor_id, int instrument_id, int calibration_id, MeasurementUnit measurement_unit, int uncertainty_set_id) {
        super(sensor_id, instrument_id, calibration_id, measurement_unit);
        setOrderBy(null);
        this.uncertainty_set_id   = uncertainty_set_id;
    }

    // returns true if the space definition is identical
    public boolean matches(int instrument_id, int sensor_id, int calibration_id, int measurement_unit_id, int uncertainty_set_id)
    {
        if(this.instrument_id == instrument_id && this.sensor_id == sensor_id && this.calibration_id == calibration_id && this.unit.getUnitId() == measurement_unit_id && this.uncertainty_set_id == uncertainty_set_id)
            return true;
        else
            return false;
    }

}
