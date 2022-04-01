package ch.specchio.types;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class represents a spectrum uncertainty node
 */

@XmlRootElement(name="uncertainty_spectrum_node")
public class UncertaintySpectrumNode extends UncertaintyNode {

	@XmlElement public Float[][] u_vectors;
	@XmlElement public int spectrum_node_id; 
	
	public UncertaintySpectrumNode() {
		
		node_type = "spectrum";
		
	}
	
	/**
	 * Set the instrument node uncertainty matrix.
	 * @param u_matrix
	 */
	
	public void setUncertaintyMatrix(double[][] u_matrix) {
		
		int columns = u_matrix[0].length;
		int rows = u_matrix.length;	
		
		System.out.println("columns:" + columns);
		System.out.println("rows:" + rows);
		
		u_vectors = new Float[rows][columns];
		
	    // store data
	    for (int r = 0; r < rows;r++)
	    {
		    for (int c = 0; c < columns;c++)
		    {	
		    	float f = (float)(u_matrix[r][c]);
		    	this.u_vectors[r][c] = Float.valueOf(f);		    	
		    }	    	
	    }	
	    
	    System.out.println("Injected into vectors!");
	
	}	
	
	/** 
	 * Get the spectrum node id
	 * @return spectrum_node_id
	 */
	
	@XmlElement(name = "spectrum_node_id")
	public int getSpectrumNodeId() {
		return spectrum_node_id;
	}
	
	/**
	 * Set the spectrum node id
	 * @param spectrum_node_id  
	 */
	
	public void setSpectrumNodeId(int spectrum_node_id) {
		this.spectrum_node_id = spectrum_node_id;
	}
	
	
	
}
