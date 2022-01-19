package ch.specchio.services;

import javax.ws.rs.core.MediaType;

import java.util.ArrayList;

import javax.annotation.security.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import ch.specchio.constants.UserRoles;
import ch.specchio.factories.SPECCHIOFactoryException;
import ch.specchio.factories.UncertaintyFactory;
import ch.specchio.jaxb.XmlInteger;
import ch.specchio.types.AdjacencyMatrix;
import ch.specchio.types.InstrumentNode;
import ch.specchio.types.SpectralSet;
import ch.specchio.types.SpectralSetDescriptor;


/**
 * Uncertainty service.
 */

@Path("/uncertainty")
@DeclareRoles({UserRoles.ADMIN, UserRoles.USER})

public class UncertaintyService extends SPECCHIOService {
	
	/**
	 * Insert an uncertainty node.
	 * 
	 * @param spectral set descriptor
	 * 
	 * @return the id of the new uncertainty node (can be spectrum or instrument)
	 * 
	 */
	
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_XML)
	@Path("insertUncertaintyNode")
	
	public XmlInteger insertUncertaintyNode(SpectralSetDescriptor ssd) throws SPECCHIOFactoryException {
	
		SpectralSet spectral_set = ssd.getSpectralSet();
		
		try
		{
				
			UncertaintyFactory factory = new UncertaintyFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
			factory.insertUncertaintyNode(ssd.getUcSourcePairs(), ssd.getUcSourceIds(), ssd.getUcSpectrumIds(), ssd.getUcSpectrumSubsetIds(), spectral_set);
			factory.dispose();
	
		}
		catch(SPECCHIOFactoryException e)
		{
			System.out.println(e.toString());
			throw(e);
		}
	
		return new XmlInteger(spectral_set.getUncertaintyNodeId()); 
	
	}
	
	/**
	 * Insert a spectrum subset.
	 * 
	 * @param spectral set descriptor
	 * 
	 * @return the id of the new spectrum subset
	 * 
	 */
	
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_XML)
	@Path("insertSpectrumSubset")
	
	public XmlInteger insertSpectrumSubset(SpectralSetDescriptor ssd) throws SPECCHIOFactoryException {
	
		SpectralSet spectral_set = ssd.getSpectralSet();
		
		try
		{
				
			UncertaintyFactory factory = new UncertaintyFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
			factory.insertSpectrumSubset(ssd.getUcSpectrumIds(), spectral_set);
			factory.dispose();
	
		}
		catch(SPECCHIOFactoryException e)
		{
			System.out.println(e.toString());
			throw(e);
		}
	
		return new XmlInteger(spectral_set.getSpectrumSubsetId()); 
	
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
		
		System.out.println("Uncertainty service: Inserting new instrument node");
		
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
		
		return new XmlInteger(instrument_node.getId()); 
		
	}
	
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_XML)
	@Path("insertNewUncertaintySet")
	public XmlInteger insertNewUncertaintySet(SpectralSet spectral_set) throws SPECCHIOFactoryException {
		
		System.out.println("Uncertainty service: Inserting new uncertainty set");
		
		try
		{
		
			UncertaintyFactory factory = new UncertaintyFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.insertNewUncertaintySet(spectral_set);
		factory.dispose();
		
		}
		catch(SPECCHIOFactoryException e)
		{
			System.out.println(e.toString());
			throw(e);
		}
		
		return new XmlInteger(spectral_set.getUncertaintySetId()); 
		
	}

	
	/**
	 * Get the adjacency matrix for an uncertainty set
	 * 
	 * @param uncertainty_set_id
	 * 
	 * @return an array integers of the adjacency matrix
	 * 
	 * @throws SPECCHIOFactoryException	
	 */
	@GET
	@Path("getAdjacencyMatrix/{uncertainty_set_id}")
	@Produces(MediaType.APPLICATION_XML)
	public AdjacencyMatrix getAdjacencyMatrix(@PathParam("uncertainty_set_id") int uncertainty_set_id) throws SPECCHIOFactoryException {
		
		UncertaintyFactory factory = new UncertaintyFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		AdjacencyMatrix cm = factory.getAdjacencyMatrix(uncertainty_set_id);
		factory.dispose();
		
		return cm;
		
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
	
	/**
	 * Retrieve an uncertainty set
	 * 
	 * @param uncertainty_set_id
	 * 
	 * @return SpectralSet 
	 * 
	 * @throws SPECCHIOFactoryException
	 */
	
	@GET
	@Path("getUncertaintySet/{uncertainty_set_id}")
	public SpectralSet getUncertaintySet(
			@PathParam("uncertainty_set_id") int uncertainty_set_id
		) throws SPECCHIOFactoryException {
		
		UncertaintyFactory factory = new UncertaintyFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		SpectralSet selectedUncertaintySet  = factory.getUncertaintySet(uncertainty_set_id);
		factory.dispose();
		return selectedUncertaintySet;
		
	}
	
	
}