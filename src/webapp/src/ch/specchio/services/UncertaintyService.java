package ch.specchio.services;

import javax.ws.rs.core.MediaType;

import javax.annotation.security.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import ch.specchio.constants.UserRoles;
import ch.specchio.factories.SPECCHIOFactoryException;
import ch.specchio.factories.SpecchioCampaignFactory;
import ch.specchio.factories.UncertaintyFactory;
import ch.specchio.jaxb.XmlInteger;
import ch.specchio.types.Campaign;
import ch.specchio.types.InstrumentNode;
import ch.specchio.types.SpectralSet;


/**
 * Uncertainty service.
 */

@Path("/uncertainty")
@DeclareRoles({UserRoles.ADMIN, UserRoles.USER})

public class UncertaintyService extends SPECCHIOService {
	
	/**
	 * Insert an uncertainty node.
	 * 
	 * @param spectral set id
	 * 
	 * @return the id of the new uncertainty node (can be spectrum or instrument)
	 * 
	 */
	
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_XML)
	@Path("insertUncertaintyNode")
	
	public XmlInteger insertUncertaintyNode(SpectralSet spectral_set) throws SPECCHIOFactoryException {
		
		System.out.println("Inserting new uncertainty node");
	
		try
		{
	
			UncertaintyFactory factory = new UncertaintyFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
			factory.insertUncertaintyNode(spectral_set);
			factory.dispose();
	
		}
		catch(SPECCHIOFactoryException e)
		{
			System.out.println(e.toString());
			throw(e);
		}
	

		return new XmlInteger(spectral_set.instrument_node_id); 
	
		
		
	}
	
	
	/**
	 * Insert instrument node.
	 * 
	 * @param an InstrumentNode object
	 * 
	 * @return the id of the new instrument node
	 * 
	 */
	
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_XML)
	@Path("insertInstrumentNode")
	public XmlInteger insertInstrumentNode(InstrumentNode instrument_node) throws SPECCHIOFactoryException {
		
		System.out.println("Inserting new instrument node");
		
		try
		{
		
			UncertaintyFactory factory = new UncertaintyFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.insertInstrumentNode(instrument_node);
		factory.dispose();
		
		}
		catch(SPECCHIOFactoryException e)
		{
			System.out.println(e.toString());
			throw(e);
		}
		
		// this might need more of a chain than just 'Factory'
		return new XmlInteger(instrument_node.getId()); 
		
	}
	
	/**
	 * Retrieve an instrument node.
	 * 
	 * @param an instrument_node_id
	 * 
	 * @return the corresponding instrument node
	 * 
	 * @throws SPECCHIOFactoryException
	 * 
	 */
	
	@GET
	@Path("getInstrumentNode/{instrument_node_id}")
	@Produces(MediaType.APPLICATION_XML)
	public InstrumentNode getInstrumentNode(
			@PathParam("instrument_node_id") int instrument_node_id
		) throws SPECCHIOFactoryException {
		
		System.out.println("Fetching instrument node");
		
		UncertaintyFactory factory = new UncertaintyFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		InstrumentNode selectedInstrumentNode  = factory.getInstrumentNode(instrument_node_id);
		factory.dispose();
		return selectedInstrumentNode;
		

	}
	
	
}