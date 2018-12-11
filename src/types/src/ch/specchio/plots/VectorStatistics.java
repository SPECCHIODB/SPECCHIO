package ch.specchio.plots;

import static java.lang.Math.sqrt;

import java.util.ArrayList;

public class VectorStatistics {

	private double mean;
	private double std_dev;
	private double max;
	private double min;
	private double var;
	
	// non-spectral statistics
	public void calc_stats(double[] vector, int start_ind, int end_ind)
	{
		// init min and max with first vector element
		max = vector[0];
		min = vector[0];
		int band;
		double sum = 0;
		int N = end_ind - start_ind + 1; // e.g. 300 to 301 are two bands: (301 - 300) + 1 = 2
		
		// get mean, min and max
		for(band = start_ind; band <= end_ind;band++)
		{
			if(!Double.isNaN(vector[band])) // filter NaNs
			{
				sum += vector[band];
				
				if(max < vector[band])
					max = vector[band];
				
				if(min > vector[band])
					min = vector[band];
			}
							
		}
		
		mean = sum/N;
		
		
		// get standard deviation	
		sum = 0;
		for(band = start_ind; band <= end_ind;band++)
		{
			if(!Double.isNaN(vector[band])) // filter NaNs
				sum += Math.pow(vector[band] - mean, 2);	
			
		}
		
		var = sum/(N-1);
		std_dev = sqrt(var);
		
	}

	// non-spectral statistics
	public void calc_stats(ArrayList<double[]> vectors, int start_ind, int end_ind)
	{
		// init min and max with first vector element
		max = vectors.get(0)[0];
		min = vectors.get(0)[0];
		int band;
		double sum = 0;
		int N = (end_ind - start_ind + 1) * vectors.size(); // e.g. 300 to 301 are two bands: (301 - 300) + 1 = 2
		
		// get mean, min and max
		for(int i = 0; i < vectors.size();i++)
		{
			for(band = start_ind; band <= end_ind;band++)
			{

				if(!Double.isNaN(vectors.get(i)[band]) && !Double.isInfinite(vectors.get(i)[band])) // filter NaNs
				{
				
					
					sum += vectors.get(i)[band];
					
					if(max < vectors.get(i)[band])
						max = vectors.get(i)[band];
					
					if(min > vectors.get(i)[band])
						min = vectors.get(i)[band];
				}
								
			}
		}
		
		mean = sum/N;
		
		
		// get standard deviation	
		sum = 0;
		if (N > 1)
		{
			for(int i = 0; i < vectors.size();i++)

			{		

				for(band = start_ind; band <= end_ind;band++)
				{
					if(!Double.isNaN(vectors.get(i)[band]) && !Double.isInfinite(vectors.get(i)[band])) // filter NaNs
					{
						sum += Math.pow(vectors.get(i)[band] - mean, 2);	
					}
				}
			}
			var = sum/(N);
			std_dev = sqrt(var);
			
			
		}
		else
		{
			var = 0;
			std_dev = 0;
			
		}
		
		
	}
	
	
	public double max()
	{
		return max;
	}
	
	
	public double mean()
	{
		return mean;
	}
	
	
	public double min()
	{
		return min;
	}
	
	
	public double standardDeviation()
	{
		return std_dev;
	}

}
