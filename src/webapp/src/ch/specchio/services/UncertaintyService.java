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
import ch.specchio.types.UncertaintyInstrumentNode;
import ch.specchio.types.UncertaintyInstrumentNodeDescriptor;
import ch.specchio.types.UncertaintyNode;
import ch.specchio.types.UncertaintyNodeDescriptor;
import ch.specchio.types.UncertaintySet;
import ch.specchio.types.UncertaintySpectrumNode;
import ch.specchio.types.UncertaintySpectrumNodeDescriptor;


/**
 * Uncertainty service.
 */

@Path("/uncertainty")
@DeclareRoles({UserRoles.ADMIN, UserRoles.USER})

public class UncertaintyService extends SPECCHIOService {
	

	/**
	 * Insert an uncertainty node.
	 * 
	 * @param uncertainty node
	 * 
	 * @return the id of the new uncertainty node (not spectrum/instrument node id)
	 * 
	 */
	
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_XML)
	@Path("insertUncertaintyNodeSpectrum")
	
	public XmlInteger insertUncertaintyNodeSpectrum(UncertaintySpectrumNodeDescriptor usnd) throws SPECCHIOFactoryException {
	
		UncertaintySpectrumNode uc_spectrum_node = usnd.getUcSpectrumNode();
		int uc_set_id = usnd.getUcSetId();
		ArrayList<Integer> uc_spectrum_ids = usnd.getUcSpectrumIds();
		ArrayList<Integer> uc_spectrum_subset_ids = usnd.getUcSpectrumSubsetIds();
		
		try
		{
				
			UncertaintyFactory factory = new UncertaintyFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
			factory.insertUncertaintyNode(uc_spectrum_node, uc_set_id, uc_spectrum_ids, uc_spectrum_subset_ids);
			factory.dispose();
	
		}
		catch(SPECCHIOFactoryException e)
		{
			System.out.println(e.toString());
			throw(e);
		}
	
		return new XmlInteger(uc_spectrum_node.getUncertaintyNodeId()); 
	
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
	@Path("insertUncertaintyNodeInstrument")
	public XmlInteger insertUncertaintyNodeInstrument(UncertaintyInstrumentNodeDescriptor uind) throws SPECCHIOFactoryException {
		
		UncertaintyInstrumentNode uc_instrument_node = uind.getUcInstrumentNode();
		int uc_set_id = uind.getUcSetId();
		
		try
		{
		
			
			UncertaintyFactory factory = new UncertaintyFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.insertUncertaintyNode(uc_instrument_node, uc_set_id);
		factory.dispose();
		
		}
		catch(SPECCHIOFactoryException e)
		{
			System.out.println(e.toString());
			throw(e);
		}
		
		return new XmlInteger(uc_instrument_node.getUncertaintyNodeId()); 
		
	}
	
	
	/**
	 * Insert a spectrum subset.
	 * 
	 * @param uncertainty spectrum node descriptor descriptor
	 * 
	 * @return the id of the new spectrum subset
	 * 
	 */
	
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_XML)
	@Path("insertSpectrumSubset")
	
	public XmlInteger insertSpectrumSubset(UncertaintySpectrumNodeDescriptor usnd) throws SPECCHIOFactoryException {
	
		UncertaintySpectrumNode uc_spectrum_node = usnd.getUcSpectrumNode();
		
		try
		{
				
			UncertaintyFactory factory = new UncertaintyFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
			factory.insertSpectrumSubset(usnd.getUcSpectrumIds(), uc_spectrum_node);
			factory.dispose();
	
		}
		catch(SPECCHIOFactoryException e)
		{
			System.out.println(e.toString());
			throw(e); 
		}
	
		return new XmlInteger(uc_spectrum_node.getSpectrumSubsetId()); 
	
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
	public XmlInteger insertNewUncertaintySet(UncertaintySet uc_set) throws SPECCHIOFactoryException {
		
		try
		{
			
			UncertaintyFactory factory = new UncertaintyFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
			factory.insertNewUncertaintySet(uc_set);
			factory.dispose();
		
		}
		catch(SPECCHIOFactoryException e)
		{
			System.out.println(e.toString());
			throw(e);
		}
		
		return new XmlInteger(uc_set.getUncertaintySetId()); 
		
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
	 * Retrieve an edge value
	 * 
	 * @param an edge id
	 * 
	 * @return the corresponding edge value
	 * 
	 * @throws SPECCHIOFactoryException
	 * 
	 */
	@GET
	@Path("getEdgeValue/{edge_id}")
	@Produces(MediaType.APPLICATION_XML)
	public String getEdgeValue(@PathParam("edge_id") int edge_id) throws SPECCHIOFactoryException {
		
		UncertaintyFactory factory = new UncertaintyFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		String edge_value = factory.getEdgeValue(edge_id);
		factory.dispose();
		
		return edge_value;
		
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
	 * Retrieve a spectrum node.
	 * 
	 * @param an spectrum_node_id
	 * 
	 * @return the corresponding spectrum node
	 * 
	 * @throws SPECCHIOFactoryException
	 * 
	 */
	
	@GET
	@Path("getSpectrumNode/{spectrum_node_id}")
	@Produces(MediaType.APPLICATION_XML)
	public InstrumentNode getSpectrumNode(
			@PathParam("spectrum_node_id") int spectrum_node_id
		) throws SPECCHIOFactoryException {
		
		System.out.println("Fetching spectrum node");
		
		UncertaintyFactory factory = new UncertaintyFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		InstrumentNode selectedInstrumentNode  = factory.getSpectrumNode(spectrum_node_id);
		factory.dispose();
		return selectedInstrumentNode;
		

	}
	
	
	/**
	 * Retrieve an uncertainty set
	 * 
	 * @param uncertainty_set_id
	 * 
	 * @return UncertaintySet 
	 * 
	 * @throws SPECCHIOFactoryException
	 */
	
	@GET
	@Path("getUncertaintySet/{uncertainty_set_id}")
	public UncertaintySet getUncertaintySet(
			@PathParam("uncertainty_set_id") int uncertainty_set_id
		) throws SPECCHIOFactoryException {
		
		
		UncertaintyFactory factory = new UncertaintyFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		UncertaintySet selectedUncertaintySet  = factory.getUncertaintySet(uncertainty_set_id);
		factory.dispose();
		return selectedUncertaintySet;
		
	}
	
	
}