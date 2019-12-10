package ch.specchio.proc_modules;


import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.client.SPECCHIOClientFactory;
import ch.specchio.gui.*;
import ch.specchio.types.*;
import net.e175.klaus.solarpositioning.AzimuthZenithAngle;
import net.e175.klaus.solarpositioning.Grena3;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;


public class SunAngleCalc {
    /** client object */
    private SPECCHIOClient specchioClient;

    /** azimuth attribute to be filled in by this dialogue */
    private attribute azimuthAttribute;

    /** zenith attribute to be filled in by this dialogue */
    private attribute zenithAttribute;

    /** the selected spectrum ids */
    private Integer spectrumIds[];


    public SunAngleCalc() throws SPECCHIOClientException  {
        // get a reference to the application's client object
        specchioClient = SPECCHIOClientFactory.getInstance().getCurrent_client();

        // get the attribute descriptors to be filled in
        Hashtable<String, attribute> attributes = specchioClient.getAttributesNameHash();
        azimuthAttribute = attributes.get("Illumination Azimuth");
        zenithAttribute = attributes.get("Illumination Zenith");
        if (azimuthAttribute == null || zenithAttribute == null) {
            throw new SPECCHIOClientException("The application server does not support the illumination azimuth and illumination zenith attributes.");
        }
    }

    public SunAngleCalc(ArrayList<Integer> spectrumIds) throws SPECCHIOClientException  {
        this.spectrumIds = new Integer[spectrumIds.size()];
        int count = 0;
        for(Integer specId : spectrumIds){
            this.spectrumIds[count] = specId;
            count++;
        }

        // get a reference to the application's client object
        specchioClient = SPECCHIOClientFactory.getInstance().getCurrent_client();


//         get the attribute descriptors to be filled in
        Hashtable<String, attribute> attributes = specchioClient.getAttributesNameHash();
        azimuthAttribute = attributes.get("Illumination Azimuth");
        zenithAttribute = attributes.get("Illumination Zenith");
        if (azimuthAttribute == null || zenithAttribute == null) {
            throw new SPECCHIOClientException("The application server does not support the illumination azimuth and illumination zenith attributes.");
        }

    }


    public void calculateSunAngle() {

        ArrayList<Integer> updatedIds = new ArrayList<Integer>();

        try {

            boolean spatial_extension = specchioClient.getCapability(Capabilities.SPATIAL_EXTENSION) != null && specchioClient.getCapability(Capabilities.SPATIAL_EXTENSION).equals("true");
            int count = 0;
            for (Integer id : spectrumIds) {

                boolean spat_pos_available = false;
                double lat = 0, lon = 0;

                // get latitude and longitude
                if(spatial_extension)
                {
                    MetaSpatialPoint pos = (MetaSpatialPoint)specchioClient.getMetaparameter(id, "Spatial Position");

                    if(pos != null)
                    {
                        lat = pos.getPoint2D().getY();
                        lon = pos.getPoint2D().getX();
                        spat_pos_available = true;
                    }

                    if(!spat_pos_available)
                    {

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
                }
                count++;
                System.out.println("Updated Spectrum " + count + " of " + spectrumIds.length);
            }

        }
        catch (SPECCHIOClientException ex1) {
            // error contacting server
            ex1.printStackTrace();
        }
        catch (ClassCastException ex) {
            // invalid data stored in the database
            ex.printStackTrace();
        }
        catch (MetaParameterFormatException e) {
            e.printStackTrace();
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

                specchioClient.updateEavMetadata(mpCalcInfo, updatedIds);

            } catch (MetaParameterFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

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

        GregorianCalendar time = dateTime.toGregorianCalendar();

        AzimuthZenithAngle result = Grena3.calculateSolarPosition(time,
                latitude, longitude, 65, 1000, 20);

        return new CelestialAngle(result.getAzimuth(),result.getZenithAngle());

    }

}

