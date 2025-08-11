package ch.specchio.proc_modules;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.gui.*;
import ch.specchio.types.*;
import net.e175.klaus.solarpositioning.AzimuthZenithAngle;
import net.e175.klaus.solarpositioning.Grena3;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;

/**
 * Worker thread for performing the actual calculations.
 */
public class SunAngleCalcThread extends Thread {

    /** the spectrum identifiers to be processed */
    private Integer spectrumIds[];

    /** client object */
    private SPECCHIOClient specchioClient;

    /** azimuth attribute to be filled in by this dialogue */
    private attribute azimuthAttribute;

    /** zenith attribute to be filled in by this dialogue */
    private attribute zenithAttribute;

    boolean isGUI;

    SunAngleCalcDialog dialog;


    /**
     * Constructor.
     *
     * @param spectrumIdsIn	the list of spectrum identifiers to be processed
     */
    public SunAngleCalcThread(List<Integer> spectrumIdsIn, SPECCHIOClient specchioClient, SunAngleCalcDialog dialog) {

        super();

        // save the input parameters for later
        spectrumIds = spectrumIdsIn.toArray(new Integer[spectrumIdsIn.size()]);
        this.specchioClient = specchioClient;
        this.dialog = dialog;
        if(dialog == null)
            this.isGUI = false;
        else
            this.isGUI = true;
    }


    /**
     * Main thread method.
     */
    public void run() throws SPECCHIOClientException {
        ProgressReportDialog pr = null;
        if(isGUI) {
            pr = new ProgressReportDialog(dialog, "Sun Angle Calculator", false, 20);
            pr.set_operation("Updating illumination angles");
            pr.setVisible(true);
        }

        // get the attribute descriptors to be filled in
        Hashtable<String, attribute> attributes = specchioClient.getAttributesNameHash();
        azimuthAttribute = attributes.get("Illumination Azimuth");
        zenithAttribute = attributes.get("Illumination Zenith");
        if (azimuthAttribute == null || zenithAttribute == null) {
            throw new SPECCHIOClientException("The application server does not support the illumination azimuth and illumination zenith attributes.");
        }



        ArrayList<Integer> spectra = new ArrayList<>();
        for(Integer id : spectrumIds){
            spectra.add(id);
        }

        ArrayList<Integer> updatedIds = new ArrayList<Integer>();

        // calculate angles for each identifier
        int cnt = 0;
        int progress = 0;
        double tot = new Double(spectrumIds.length);
        try {

            boolean spatial_extension = specchioClient.getCapability(Capabilities.SPATIAL_EXTENSION) != null && specchioClient.getCapability(Capabilities.SPATIAL_EXTENSION).equals("true");

            for (Integer id : spectrumIds) {

                // download spectrum metadata from server
                //Spectrum s = specchioClient.getSpectrum(id, true);
                //Metadata md = s.getMetadata();
                //ArrayList<Integer> ids = new ArrayList<Integer>();
                //ids.add(id);

                //System.out.println(id);

                boolean spat_pos_available = false;
                double lat = 0, lon = 0;

                // get latitude and longitude
                if(spatial_extension)
                {
                    //ArrayList<MetaParameter> pos_tmp = specchioClient.getMetaparameters(ids, "Spatial Position");
                    //MetaSpatialPoint pos = (MetaSpatialPoint)md.get_first_entry("Spatial Position");
                    //MetaSpatialPoint pos = (MetaSpatialPoint) pos_tmp.get(0);
//						MetaParameter tmp = specchioClient.getMetaparameter(id, "Spatial Position");
                    MetaSpatialPoint pos = (MetaSpatialPoint)specchioClient.getMetaparameter(id, "Spatial Position");

                    if(pos != null)
                    {
                        lat = pos.getPoint2D().getY();
                        lon = pos.getPoint2D().getX();
                        spat_pos_available = true;
                    }

                    if(!spat_pos_available)
                    {
                        //MetaSpatialPolyline t = (MetaSpatialPolyline)md.get_first_entry("Spatial Transect");
                        //pos_tmp = specchioClient.getMetaparameters(ids, "Spatial Transect");
                        //MetaSpatialPolyline t = (MetaSpatialPolyline) pos_tmp.get(0);
                        MetaSpatialPolyline t = (MetaSpatialPolyline)specchioClient.getMetaparameter(id, "Spatial Transect");

                        if(t != null)
                        {

                            ArrayListWrapper wrapper = (ArrayListWrapper) t.getValue();
                            List coords = wrapper.getList();

                            Point2D coord1 = (Point2D) coords.get(0);
                            Point2D coord_end = (Point2D) coords.get(coords.size()-1);

                            lat = (coord1.getY() + coord_end.getY()) / 2;
                            lon = (coord1.getX() + coord_end.getX()) / 2;
                            spat_pos_available = true;
                        }


                    }


                }
                else
                {
//						ArrayList<MetaParameter> lat_tmp = specchioClient.getMetaparameters(ids, "Latitude");
//						ArrayList<MetaParameter> lon_tmp = specchioClient.getMetaparameters(ids, "Longitude");
////						MetaSimple latitude = (MetaSimple)md.get_first_entry("Latitude");
////						MetaSimple longitude = (MetaSimple)md.get_first_entry("Longitude");
//
//						MetaSimple latitude = (MetaSimple) lat_tmp.get(0);
//						MetaSimple longitude = (MetaSimple) lon_tmp.get(0);

                    MetaSimple latitude = (MetaSimple) specchioClient.getMetaparameter(id, "Latitude");
                    MetaSimple longitude = (MetaSimple) specchioClient.getMetaparameter(id, "Longitude");

                    if (latitude != null && longitude != null)
                    {
                        lat = (Double)latitude.getValue();
                        lon = (Double)longitude.getValue(); // longitude east of Greenwich is positive, west is negative
                        spat_pos_available = true;
                    }
                }

                // get acquisition time
                //MetaDate acquisitionTime = (MetaDate)md.get_first_entry("Acquisition Time (UTC)");
//					ArrayList<MetaParameter> utc_tmp = specchioClient.getMetaparameters(ids, "Acquisition Time (UTC)");
//					MetaDate acquisitionTime = (MetaDate)utc_tmp.get(0);
                MetaDate acquisitionTime = null;
                if(spat_pos_available)
                {
                    acquisitionTime = (MetaDate) specchioClient.getMetaparameter(id, "Acquisition Time (UTC)");
                }

                // calculate angles only if we have a position and acquisition time
                if (spat_pos_available && acquisitionTime != null) {

                    // calculate the angle of the sun at the position and acquisition time
                    CelestialAngle angle = calculateSunAngle(
                            lat,
                            lon,
                            (DateTime)acquisitionTime.getValue()
                    );

                    // round sun angle to 4 digits
                    double azimuth = MetaDataEditorView.round(angle.azimuth, 6);
                    double zenith = MetaDataEditorView.round(angle.zenith, 6);

                    // build the list of identifiers to be updated
                    ArrayList<Integer> updateIds = new ArrayList<Integer>();
                    updateIds.add(id);

                    // update azimuth
                    MetaParameter azimuthParameter = (MetaParameter) specchioClient.getMetaparameter(id, "Illumination Azimuth");  ; // md.get_first_entry(azimuthAttribute.getId());

                    if (azimuthParameter == null) {
                        azimuthParameter = MetaParameter.newInstance(azimuthAttribute);
                    }
                    azimuthParameter.setValue(new Double(azimuth));
                    specchioClient.updateEavMetadata(azimuthParameter, updateIds);

                    // update zenith
                    MetaParameter zenithParameter = (MetaParameter) specchioClient.getMetaparameter(id, "Illumination Zenith");
                    //MetaParameter zenithParameter = md.get_first_entry(zenithAttribute.getId());
                    if (zenithParameter == null) {
                        zenithParameter = MetaParameter.newInstance(zenithAttribute);
                    }
                    zenithParameter.setValue(new Double(zenith));
                    specchioClient.updateEavMetadata(zenithParameter, updateIds);

                    // add the identifier to the list of updated identifiers
                    updatedIds.add(id);

                    // udpate counter
                    cnt++;

                }

                // update progress meter
                if(isGUI) pr.set_progress(++progress * 100.0 / tot);

            }

        }
        catch (SPECCHIOClientException ex) {
            // error contacting server
            if(isGUI) {
                ErrorDialog error = new ErrorDialog((Frame) dialog.getOwner(), "Error", ex.getUserMessage(), ex);
                error.setVisible(true);
            }
            else {
                throw new SPECCHIOClientException("The application server does not support the illumination azimuth and illumination zenith attributes.", ex);
            }
        }
        catch (ClassCastException ex) {
            // invalid data stored in the database
            if (isGUI) {
                ErrorDialog error = new ErrorDialog((Frame) dialog.getOwner(), "Error", "The database contains invalid data for one or more of these spectra.", ex);
                error.setVisible(true);
            } else {
                throw new SPECCHIOClientException("The database contains invalid data for one or more of these spectra.", ex);
            }
        }
        catch (MetaParameterFormatException ex) {
            // the parameter have the wrong type
            if(isGUI) {
                ErrorDialog error = new ErrorDialog((Frame) dialog.getOwner(), "Error", "The illumination attributes have the wrong type. Please contact your system administrator.", ex);
                error.setVisible(true);
            }  else {
            throw new SPECCHIOClientException("The illumination attributes have the wrong type. Please contact your system administrator.", ex);
        }

        }

        if (updatedIds.size() > 0) {

            attribute attr = specchioClient.getAttributesNameHash().get("Solar Angle Computation");

            specchioClient.removeEavMetadata(attr, updatedIds, MetaParameter.SPECTRUM_LEVEL); // remove any existing solar angle computation entries

            // create a metaparameter noting that the time was shifted
            MetaParameter mpCalcInfo;
            DateTime dt = new DateTime(DateTimeZone.UTC);
            try {
                mpCalcInfo = MetaParameter.newInstance(
                        attr,
                        "Solar angles calculated using the SPECCHIO sun angle function (" + dt.toString() + ")"
                );

                // add the metaparameter to the database
                pr.set_operation("Updating database: adding provenance info ...");
                specchioClient.updateEavMetadata(mpCalcInfo, updatedIds);

            } catch (MetaParameterFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }



        }

        // show a completion message
        StringBuffer message = new StringBuffer();
        if (cnt > 0) {
            message.append("Sun angles computed for " + Integer.toString(cnt) + " spectra.");
            if (cnt < progress) {
                message.append("\n" + Integer.toString(progress - cnt) + " of the selected spectra do not have longitude, latitude or acquisition time data.");
                message.append("\nSun angles cannot be computed for these spectra.");
            }
        } else {
            message.append("No longitude, latitude or UTC acquisition time data found. No sun angles could be computed.");
        }
        JOptionPane.showMessageDialog(dialog, message, "Calculation complete", JOptionPane.INFORMATION_MESSAGE, SPECCHIOApplication.specchio_icon);

        pr.setVisible(false);
    }


    /**
     * Calculate the angle of the sun for a given longitude, latitude and time.
     *
     * @param latitude	the latitude
     * @param longitude	the longitude
     * @param dateTime		the date and time
     *
     * @return a new CelestialAngle object representing the position of the sun
     */
    private CelestialAngle calculateSunAngle(double latitude, double longitude, DateTime dateTime) {



        // get the time of year
////			TimeZone tz = TimeZone.getDefault();
////			Calendar cal = Calendar.getInstance(tz);
////			cal.setTime(dateTime);
////			int dy = cal.get(Calendar.DAY_OF_YEAR);
//			int dy = dateTime.getDayOfYear();
//			dy--; // Java starts the count at 1 (first day of year)
//				// but this routine expects the first day as 0 (zero)
////			int hh = cal.get(Calendar.HOUR_OF_DAY);
////			int mm = cal.get(Calendar.MINUTE);
////			int ss = cal.get(Calendar.SECOND);
//
//			int hh = dateTime.getHourOfDay();
//			int mm = dateTime.getMinuteOfHour();
//			int ss = dateTime.getSecondOfMinute();
//
//
//			double hours = hh + mm/60.0 + ss/3600.0;
//
//
//			int timezone = 0; // time zone: we expect the capture time to be in GMT
//
//			double wdy = 2*Math.PI*dy/365.0;
//
//			// sun declination [rad]
//			double delta = 0.006918-0.399912*Math.cos(wdy)+0.070257*Math.sin(wdy)-0.006758*Math.cos(2*wdy)+0.000908*Math.sin(2*wdy);
//
//			// longitude correction
//			double lc = -longitude / 15;
//
//			// time equation
//			double et = 0.0172+0.4281*Math.cos(wdy)-7.3515*Math.sin(wdy)- 3.3495*Math.cos(2*wdy)-9.3619*Math.sin(2*wdy);
//
//			// true solar time
//			double tst = hours-timezone+lc+et/60;
//			double wtst = Math.PI*(tst-12)/12;
//
//			if(wtst > Math.PI)
//				wtst=wtst-2*Math.PI;
//
//			if(wtst < -Math.PI)
//				wtst=wtst+2*Math.PI;
//
//			// sun height, sun zenith angle
//			double lat_r = Math.toRadians(latitude);
//
//
//			double h = Math.asin(Math.cos(lat_r)*Math.cos(delta)*Math.cos(wtst)+Math.sin(lat_r)*Math.sin(delta));
//
//			double thz = Math.PI/2 - h;
//			double nen = Math.sin(lat_r)*Math.cos(delta)*Math.cos(wtst)-Math.cos(lat_r)*Math.sin(delta);
//			double phi = Math.acos(nen/Math.cos(h));
//
//			if( wtst < 0)
//				phi=-phi;
//
//			double azimuth = phi + Math.PI;
//			if(azimuth > 2*Math.PI)
//				azimuth = azimuth - 2*Math.PI;
//
//			return new CelestialAngle(Math.toDegrees(azimuth), Math.toDegrees(thz));

        Instant instant = Instant.ofEpochMilli(dateTime.getMillis());
        ZoneId zoneId = ZoneId.of(dateTime.getZone().getID(), ZoneId.SHORT_IDS);
        ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, zoneId);

        AzimuthZenithAngle result = Grena3.calculateSolarPosition(zdt,
                latitude, longitude, 65, 1000, 20);



        return new CelestialAngle(result.getAzimuth(),result.getZenithAngle());




    }

}
