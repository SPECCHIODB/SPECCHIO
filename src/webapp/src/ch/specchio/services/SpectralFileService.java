package ch.specchio.services;


import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.annotation.security.*;

import ch.specchio.constants.UserRoles;
import ch.specchio.factories.SPECCHIOFactoryException;
import ch.specchio.factories.SpectralFileFactory;
import ch.specchio.jaxb.XmlBoolean;
import ch.specchio.jaxb.XmlInteger;
import ch.specchio.jaxb.XmlIntegerAdapter;
import ch.specchio.types.SpecchioMessage;
import ch.specchio.types.SpectralFile;
import ch.specchio.types.SpectralFileInsertResult;
import ch.specchio.types.SpectralFiles;


/**
 * Spectral file services.
 */
@Path("/spectral_file")
@DeclareRoles({UserRoles.ADMIN, UserRoles.USER})
public class SpectralFileService extends SPECCHIOService {
	
	/**
	 * Test whether or not a given spectral file exists in the database.
	 * 
	 * @param spec_file	the spectrum file to test
	 * 
	 * @return true if the spectrum exists, or false if not
	 * 
	 * @throws SPECCHIOFactoryException	could not access the database
	 */
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_XML)
	@Path("exists")
	public XmlBoolean exists(SpectralFile spec_file) throws SPECCHIOFactoryException {
		
		SpectralFileFactory factory = new SpectralFileFactory(
				getClientUsername(),
				getClientPassword(), 
				getSecurityContext().isUserInRole(UserRoles.ADMIN),
				getDataSourceName(),
				spec_file.getCampaignId()				
			);
				
		
		boolean b = factory.spectrumExists(spec_file, spec_file.getHierarchyId());
		factory.dispose();
		
		return  new XmlBoolean(b);
	}	
	
	/**
	 * Test whether or not a list of given spectral files exist in the database.
	 * 
	 * @param spec_files	list of spectral files
	 * 
	 * @return list of existence per file encoded as 0/1
	 * 
	 * @throws SPECCHIOFactoryException	could not access the database
	 */
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_XML)
	@Path("exist")
	public XmlInteger[] exist(SpectralFiles spec_files) throws SPECCHIOFactoryException {
		
		SpectralFileFactory factory = new SpectralFileFactory(
				getClientUsername(),
				getClientPassword(), 
				isAdmin(),
				getDataSourceName(),
				spec_files.getCampaignId()				
			);
		
		ArrayList<Integer> exist_list = new ArrayList<Integer>();
				
		if(spec_files.getSpectral_file_list() != null) // catch calls with empty lists ... (can happen from e.g. Matlab)
		{

			ArrayList<Boolean> exist_list_ = factory.spectraExist(spec_files.getSpectral_file_list(), spec_files.getSpectral_file_list().get(0).getHierarchyId());

			for(int i=0;i<exist_list_.size();i++)
			{
				exist_list.add((exist_list_.get(i) == true) ? 1 : 0);
			}

		}

		factory.dispose();		
 		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		return adapter.marshalArray(exist_list);			

	}		
	
	
	/**
	 * Get the identifier for a given file format.
	 * 
	 * @param file_format_name	the name of the file format
	 * 
	 * @return the identifier of the given file format
	 * 
	 * @throws SPECCHIOFactoryException	could not access the database
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("file_format_id/{file_format_name}")
	public XmlInteger file_format_id(@PathParam("file_format_name") String file_format_name) throws SPECCHIOFactoryException {
		
		SpectralFileFactory factory = new SpectralFileFactory(
				getClientUsername(),
				getClientPassword(), getDataSourceName(),
				this.isAdmin()
			);
		int id = factory.getIdForFileFormat(file_format_name);
		factory.dispose();
		
		return new XmlInteger(id);
		
	}
	
	
	/**
	 * Insert a spectral file into the database.
	 * 
	 * @param spec_file	the file to be inserted
	 * 
	 * @return a list of spectrum identifiers created by the insertion
	 * 
	 * @throws SPECCHIOFactoryException	could not access the database
	 */
	@POST
	@Path("insert")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public SpectralFileInsertResult insert(SpectralFile spec_file)  {

		SpectralFileFactory factory = new SpectralFileFactory(
				getClientUsername(),
				getClientPassword(), 
				getSecurityContext().isUserInRole(UserRoles.ADMIN),
				getDataSourceName(),
				spec_file.getCampaignId()
			);
		
		Instant start = Instant.now();
		SpectralFileInsertResult insert_result = factory.insertSpectralFile(spec_file, spec_file.getHierarchyId());
		Instant end = Instant.now();
		insert_result.addError(new SpecchioMessage("Total insert time on server: " + Duration.between(start, end).getSeconds(), SpecchioMessage.INFO) );
		
		factory.dispose();
		
//		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
//		return adapter.marshalArray(ids);
		return insert_result;
	}

}
