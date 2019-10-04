package ch.specchio.services;

import java.io.IOException;

import javax.annotation.security.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import ch.specchio.constants.UserRoles;
import ch.specchio.factories.InstrumentationFactory;
import ch.specchio.factories.SPECCHIOFactoryException;
import ch.specchio.factories.SpectralFileFactory;
import ch.specchio.jaxb.XmlBoolean;
import ch.specchio.jaxb.XmlInteger;
import ch.specchio.types.Calibration;
import ch.specchio.types.CalibrationMetadata;
import ch.specchio.types.Instrument;
import ch.specchio.types.InstrumentDescriptor;
import ch.specchio.types.Picture;
import ch.specchio.types.PictureTable;
import ch.specchio.types.Reference;
import ch.specchio.types.ReferenceBrand;
import ch.specchio.types.ReferenceDescriptor;
import ch.specchio.types.Sensor;
import ch.specchio.types.SpectralFile;


/**
 * Instrumentation service.
 */
@Path("/instrumentation")
@DeclareRoles({UserRoles.ADMIN, UserRoles.USER})
public class InstrumentationService extends SPECCHIOService {
	
	
	/**
	 * Delete an instrument from the database.
	 * 
	 * @param instrument_id	the identifier of the instrument to be deleted
	 * 
	 * @return an empty string
	 * 
	 * @throws SPECCHIOFactoryException	the instrument does not exist
	 */
	@GET
	@Path("delete/{instrument_id: [0-9]+}")
	@Produces(MediaType.APPLICATION_XML)
	public String delete(@PathParam("instrument_id") int instrument_id) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.deleteInstrument(instrument_id);
		factory.dispose();
		
		return "";
	}
	
	
	/**
	 * Delete an instrument calibration.
	 * 
	 * @param calibration_id	the identifier of the calibration to be deleted
	 * 
	 * @return an empty string
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@GET
	@Path("deleteCalibration/{calibration_id: [0-9]+}")
	@Produces(MediaType.APPLICATION_XML)
	public String deleteCalibration(@PathParam("calibration_id") int calibration_id) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.deleteCalibration(calibration_id);
		factory.dispose();
		
		return "";
	}
	
	
	/**
	 * Delete a picture from the database.
	 * 
	 * param picture_id	the identifier of the picture to be deleted
	 * 
	 * @return an empty string
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@GET
	@Path("deletePicture/{picture_id: [0-9]+}")
	@Produces(MediaType.APPLICATION_XML)
	public String deletePicture(@PathParam("picture_id") int picture_id) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.deletePicture(picture_id);
		factory.dispose();
		
		return "";
	}
	
	
	/**
	 * Delete a reference from the database.
	 * 
	 * @param reference_id	the identifier of the reference to be deleted
	 * 
	 * @return an empty string
	 * 
	 * @throws SPECCHIOFactoryException	the reference does not exist
	 */
	@GET
	@Path("deleteReference/{reference_id: [0-9]+}")
	@Produces(MediaType.APPLICATION_XML)
	public String deleteReference(@PathParam("reference_id") int reference_id) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.deleteReference(reference_id);
		factory.dispose();
		
		return "";
	}
	
	
	/**
	 * Get an instrument object.
	 * 
	 * @param instrument_id	the identifier of the instrument to get
	 * 
	 * @return an Instrument object corresponding to instrument_id
	 * 
	 * @throws SPECCHIOFactoryException	the instrument does not exist
	 */
	@GET
	@Path("get/{instrument_id: [0-9]+}")
	@Produces(MediaType.APPLICATION_XML)
	public Instrument get(@PathParam("instrument_id") int instrument_id) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		Instrument instrument = factory.getInstrument(instrument_id);
		factory.dispose();
		
		return instrument;
	}
	
	
	/**
	 * Get an instrument that matches the passed object or creates a new instrument and returns it
	 * 
	 * @param instrument instance
	 * 
	 * @return an Instrument object matching the passed object, or a new instrument
	 * 
	 * @throws SPECCHIOFactoryException	on error
	 */
	@POST
	@Path("getOrCreateInstrument")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)	
	public Instrument getOrCreateInstrument(Instrument instrument) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		Instrument known_instrument = factory.getOrCreateInstrument(instrument);
		factory.dispose();
		
		return known_instrument;
	}		

	
	/**
	 * Get a calibrated instrument object.
	 * 
	 * @param calibration_id	the calibration identifier
	 * 
	 * @return a new Instrument object, or null if the calibrated instrument does not exist
	 * 
	 * @throws SPECCHIOFactoryException	the instrument does not exist
	 */
	@GET
	@Path("getCalibratedInstrument/{calibration_id: [0-9]+}")
	@Produces(MediaType.APPLICATION_XML)
	public Instrument getCalibratedInstrument(@PathParam("calibration_id") int calibration_id) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		Instrument instrument = factory.getCalibratedInstrument(calibration_id);
		factory.dispose();
		
		return instrument;
	}	
	
	
	/**
	 * Get a instrument object for a given spectral file object.
	 * 
	 * @param spec_file		the spectral file
	 * 
	 * @return a new Instrument object
	 * 
	 * @throws SPECCHIOFactoryException	the instrument does not exist
	 */
	@POST
	@Path("getInstrumentForSpectralFile")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public Instrument getInstrumentForSpectralFile(SpectralFile spec_file) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		Instrument instrument = factory.getInstrumentForSpectralFile(spec_file);
		factory.dispose();
		
		return instrument;
	}		
	
	/**
	 * Get the calibration metadata for an instrument.
	 * 
	 * @param instrument_id	the identifier of the the instrument
	 * 
	 * @return an array of calibration metadata objects associated with the instrument
	 * 
	 * @throws SPECCHIOFactoryException	the instrument does not exist
	 */
	@GET
	@Path("calibrationMetadata/instrument/{instrument_id: [0-9]+}")
	@Produces(MediaType.APPLICATION_XML)
	public CalibrationMetadata[] getInstrumentCalibrationMetadata(@PathParam("instrument_id") int instrument_id) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		CalibrationMetadata[] cm = factory.getInstrumentCalibrationMetadata(instrument_id);
		factory.dispose();
		
		return cm;
		
	}

	/**
	 * Get the pictures associated with an instrument.
	 * 
	 * @param instrument_id	the instrument identifier
	 * 
	 * @return a table of pictures associated with the instrument
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@GET
	@Path("pictures/instrument/{instrument_id: [0-9]+}")
	@Produces(MediaType.APPLICATION_XML)
	public PictureTable getInstrumentPictures(@PathParam("instrument_id") int instrument_id) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		PictureTable pictures = factory.getInstrumentPictures(instrument_id);
		factory.dispose();
		
		return pictures;
	}

	/**
	 * Get a reference object.
	 * 
	 * @param reference_id the identifier of the reference to get
	 * 
	 * @return a Reference object corresponding to reference_id
	 * 
	 * @throws SPECCHIOFactoryException	the reference does not exist
	 */
	@GET
	@Path("getReference/{reference_id: [0-9]+}")
	@Produces(MediaType.APPLICATION_XML)
	public Reference getReference(@PathParam("reference_id") int reference_id) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		Reference reference = factory.getReference(reference_id);
		factory.dispose();
		
		return reference;
	}
	
	
	/**
	 * Get the calibration metadata for an reference.
	 * 
	 * @param reference_id	the identifier of the the reference
	 * 
	 * @return an array of calibration metadata objects associated with the reference
	 * 
	 * @throws SPECCHIOFactoryException	the reference does not exist
	 */
	@GET
	@Path("calibrationMetadata/reference/{reference_id: [0-9]+}")
	@Produces(MediaType.APPLICATION_XML)
	public CalibrationMetadata[] getReferenceCalibrationMetadata(@PathParam("reference_id") int reference_id) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		CalibrationMetadata[] cm = factory.getReferenceCalibrationMetadata(reference_id);
		factory.dispose();
		
		return cm;
		
	}
	
	
	/**
	 * Get the pictures associated with an reference.
	 * 
	 * @param reference_id	the reference identifier
	 * 
	 * @return a table of pictures associated with the reference
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@GET
	@Path("pictures/reference/{reference_id: [0-9]+}")
	@Produces(MediaType.APPLICATION_XML)
	public PictureTable getReferencePictures(@PathParam("reference_id") int reference_id) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		PictureTable pictures = factory.getReferencePictures(reference_id);
		factory.dispose();
		
		return pictures;
	}

	/**
	 * Insert an instrument into the database.
	 * 
	 * @param instrument_name	the name of the new instrument
	 * 
	 * @return an empty string
	 * 
	 * @throws SPECCHIOFactoryException could not insert the new instrument
	 */
	@GET
	@Path("insert/{instrument_name}")
	@Produces(MediaType.APPLICATION_XML)
	public String insert(@PathParam("instrument_name") String instrument_name) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.insertInstrument(instrument_name);
		factory.dispose();
		
		return "";
	}
	
	
	/**
	 * Insert an instrument calibration into the database.
	 * 
	 * @param cal	the calibration object to be inserted
	 * 
	 * @return an empty string
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@POST
	@Path("insertInstrumentCalibration")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public String insertInstrumentCalibration(Calibration cal) throws SPECCHIOFactoryException {
		
		//InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName());
		InstrumentationFactory factory = new InstrumentationFactory(getDataSourceName()); // insert as admin
		factory.insertInstrumentCalibration(cal);
		factory.dispose();
		
		return "";
	}
	
	
	/**
	 * Insert a picture of an instrument into the database.
	 * 
	 * @param picture	the picture
	 * 
	 * @return the identifier of the new picture
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@POST
	@Path("insertInstrumentPicture")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public XmlInteger insertInstrumentPicture(Picture picture) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		int picture_id = factory.insertInstrumentPicture(picture.getObjectId(), picture.getImageData(), picture.getCaption());
		factory.dispose();
		
		return new XmlInteger(picture_id);
		
	}
	
	
	/**
	 * Insert a reference into the database.
	 * 
	 * @param reference_name	the name of the new reference
	 * 
	 * @return an empty string
	 * 
	 * @throws SPECCHIOFactoryException could not insert the new reference
	 */
	@GET
	@Path("insertReference/{reference_name}")
	@Produces(MediaType.APPLICATION_XML)
	public String insertReference(@PathParam("reference_name") String reference_name) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.insertReference(reference_name);
		factory.dispose();
		
		return "";
	}
	
	
	/**
	 * Insert an reference calibration into the database.
	 * 
	 * @param cal	the calibration object to be inserted
	 * 
	 * @return an empty string
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@POST
	@Path("insertReferenceCalibration")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public String insertReferenceCalibration(Calibration cal) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.insertReferenceCalibration(cal);
		factory.dispose();
		
		return "";
	}
	
	
	/**
	 * Insert a picture of an reference into the database.
	 * 
	 * @param picture	the picture
	 * 
	 * @return the identifier of the new picture
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@POST
	@Path("insertReferencePicture")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public XmlInteger insertReferencePicture(Picture picture) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		int picture_id = factory.insertReferencePicture(picture.getObjectId(), picture.getImageData(), picture.getCaption());
		factory.dispose();
		
		return new XmlInteger(picture_id);
		
	}

	/**
	 * Test for the existence of a calibration in the database.
	 * 
	 * @param cal		calibration object to check
	 * 
	 * @return true if the calibration already exists in the database, false otherwise
	 */
	
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_XML)
	@Path("instrument_calibration_exists")
	public XmlBoolean instrumentCalibrationExists(Calibration cal) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		boolean b = factory.instrumentCalibrationExists(cal);
		factory.dispose();		
		
		return  new XmlBoolean(b);
	}		
	
	
	/**
	 * List all of the instruments in the database.
	 * 
	 * @return an array of InstrumentDescriptors identifying every instrument in the database
	 * 
	 * @throws SPECCHIOFactoryException	could not access the database
	 */
	@GET
	@Path("list")
	@Produces(MediaType.APPLICATION_XML)
	public InstrumentDescriptor[] list() throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		InstrumentDescriptor[] instruments = factory.getInstrumentDescriptors();
		factory.dispose();
		
		return instruments;
		
	}
	
	
	/**
	 * List all of the reference brands in the database.
	 * 
	 * @return an array of ReferenceBrands objects describing every brand in the database
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@GET
	@Path("listReferenceBrands")
	@Produces(MediaType.APPLICATION_XML)
	public ReferenceBrand[] listReferenceBrands() throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		ReferenceBrand[] brands = factory.getReferenceBrands();
		factory.dispose();
		
		return brands;
		
	}
		
		
	
	
	/**
	 * List all of the references in the database.
	 * 
	 * @return an array of ReferenceDescriptors identifying every reference in the database
	 * 
	 * @throws SPECCHIOFactoryException	could not access the database
	 */
	@GET
	@Path("listReferences")
	@Produces(MediaType.APPLICATION_XML)
	public ReferenceDescriptor[] listReferences() throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		ReferenceDescriptor[] references = factory.getReferenceDescriptors();
		factory.dispose();
		
		return references;
		
	}
	
	
	/**
	 * Get a list of all of the sensors in the database.
	 * 
	 * @return an aray of Sensor objects
	 * 
	 * @throws SPECCHIOFactoryException	could not connect to the database
	 */
	@GET
	@Path("listSensors")
	@Produces(MediaType.APPLICATION_XML)
	public Sensor[] listSensors() throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		Sensor sensors[] = factory.getSensorDescriptors();
		factory.dispose();
		
		return sensors;
		
	}
	
	
	/**
	 * Load a sensor definition into the database.
	 * 
	 * @return the identifier of the new sensor
	 * 
	 * @throws SPECCHIOFactoryException	the input file was not formatted correctly
	 */
	@POST
	@Path("loadSensor")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_XML)
	@RolesAllowed({UserRoles.ADMIN})
	public XmlInteger loadSensor() throws SPECCHIOFactoryException {
		
		int sensor_id;
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		try {
			sensor_id = factory.loadSensorDefinition(getRequest().getInputStream());
		}
		catch (IOException ex) {
			// not sure what might cause this
			throw new SPECCHIOFactoryException(ex);
		}
		finally {
			factory.dispose();
		}
		
		return new XmlInteger(sensor_id);
		
	}
	
	
	/**
	 * Update an instrument.
	 * 
	 * @param instrument	the instrument
	 * 
	 * @return an empty string
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@POST
	@Path("update")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public String update(Instrument instrument) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.updateInstrument(instrument);
		factory.dispose();
		
		return "";
		
	}
	
	
	/**
	 * Update calibration metadata.
	 * 
	 * @param cm	the new calibration metadata
	 * 
	 * @return an empty string
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@POST
	@Path("updateCalibrationMetadata")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public String updateCalibrationMetadata(CalibrationMetadata cm) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.updateCalibrationMetadata(cm);
		factory.dispose();
		
		return "";
		
	}
	
	
	/**
	 * Update a picture associated with an instrument.
	 * 
	 * @param picture	the new picture
	 * 
	 * @return an emptu string
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@POST
	@Path("updateInstrumentPicture")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public String updateInstrumentPicture(Picture picture) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.updateInstrumentPicture(picture);
		factory.dispose();
		
		return "";
		
	}
	
	
	/**
	 * Update a reference.
	 * 
	 * @param reference	the reference
	 * 
	 * @return an empty string
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@POST
	@Path("updateReference")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public String updateReference(Reference reference) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.updateReference(reference);
		factory.dispose();
		
		return "";
		
	}
	
	
	/**
	 * Update a picture associated with a reference.
	 * 
	 * @param picture	the new picture
	 * 
	 * @return an emptu string
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@POST
	@Path("updateReferencePicture")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public String updateReferencePicture(Picture picture) throws SPECCHIOFactoryException {
		
		InstrumentationFactory factory = new InstrumentationFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.updateReferencePicture(picture);
		factory.dispose();
		
		return "";
		
	}

}
