package ch.specchio.types;

import javax.xml.bind.annotation.*;

/**
 * 
 * Adjacency matrix class with matrix values stored as integer array;
 *
 */

@XmlRootElement(name = "adjacency_matrix")
public class AdjacencyMatrix {

	public int uncertainty_set_id; 
	public Integer[] adjacency_matrix_as_int_array;
	public int matrix_dimensions; //dimensions in the plural because adjacency matrix is always square
	
	public AdjacencyMatrix() {};
	public AdjacencyMatrix(int uncertainty_set_id, Integer[] adjacency_matrix_as_int_array)
	{
		this.uncertainty_set_id = uncertainty_set_id;
		this.adjacency_matrix_as_int_array = adjacency_matrix_as_int_array;
	};
	
	public AdjacencyMatrix(int uncertainty_set_id, Integer[] adjacency_matrix_as_int_array, int matrix_dimensions)
	{
		this.uncertainty_set_id = uncertainty_set_id;
		this.adjacency_matrix_as_int_array = adjacency_matrix_as_int_array;
		this.matrix_dimensions = matrix_dimensions;
	};
	
	
	
}
