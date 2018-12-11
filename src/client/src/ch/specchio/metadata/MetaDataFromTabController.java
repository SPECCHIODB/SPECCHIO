package ch.specchio.metadata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

import org.joda.time.DateTime;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.gui.MetaDataFromTabView;
import ch.specchio.gui.SPECCHIOApplication;
import ch.specchio.gui.combo_table_data;
import ch.specchio.types.Category;
import ch.specchio.types.MatlabAdaptedArrayList;
import ch.specchio.types.MetaDate;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.MetaParameterFormatException;
import ch.specchio.types.MetaSpatialPoint;
import ch.specchio.types.Point2D;
import ch.specchio.types.attribute;

import jxl.Workbook;
import jxl.read.biff.BiffException;

public class MetaDataFromTabController  {
	
	MetaDataFromTabView view;
	MetaDataFromTabModel model = new MetaDataFromTabModel();
	SPECCHIOClient specchio_client;
	
	 
	class MetaDataFromTabWorker extends SwingWorker<Integer, Void> {
		
		private ProgressMonitor progressMonitor;
		
		public MetaDataFromTabWorker(ProgressMonitor progressMonitorIn) {
			
			progressMonitor = progressMonitorIn;
			
		}

		@Override
		public Integer doInBackground() throws SPECCHIOClientException {

			int progress = 0;

			setProgress(progress);


			// insert data
			// insert each meta element separately
			//
			ArrayList<Integer> assigned_cols = model.getColumnNumbersOfAssignedColumns();

			progressMonitor.setMaximum( assigned_cols.size());
			progressMonitor.setMillisToDecideToPopup(0);
			progressMonitor.setMillisToPopup(0);

			Hashtable<Integer, Integer> LUT = model.getMatchingLUT();				

			int col_index = 0;
			int delta_progress = 1;
			int param_count = 0;
			boolean spatial_attribute_present = false;
			int lat_col = -1;
			int lon_col = -1;
			ArrayList<Point2D> spatial_positions = new ArrayList<Point2D>();
			
			// pre-scan for spatial attribute handling
			while (col_index < assigned_cols.size())
			{	
				Integer assigned_col = assigned_cols.get(col_index);
				attribute attr = model.getAttributeOfColumn(assigned_cols.get(col_index));
				
				String col_name = model.getItem_name_of_column(assigned_col);
				
				if(model.getItem_name_of_column(assigned_col).equals("Spatial Position (Lon)"))
				{
					lon_col = assigned_col;
				}
				
				if(model.getItem_name_of_column(assigned_col).equals("Spatial Position (Lat)"))
				{
					lat_col = assigned_col;
				}
				
				col_index++;
			}

			if(lat_col >=0 && lon_col >= 0)
			{
				spatial_attribute_present = true;
				
				ArrayList<Object> lon_values = model.getValuesOfTableColumn(lon_col);
				ArrayList<Object> lat_values = model.getValuesOfTableColumn(lat_col);
				
				// compile spatial position records
				Enumeration<Integer> li = LUT.keys();

				// loop over all spectra in matching LUT
				while(li.hasMoreElements())
				{
					Integer spectrum_index = li.nextElement();

					Integer row = LUT.get(spectrum_index);
					
					if(lat_values.get(row) instanceof Number && lon_values.get(row) instanceof Number)
					{
						
						double lat = 0; double lon = 0;
						
						if(lat_values.get(row) instanceof Integer)
							lat = ((Integer) lat_values.get(row)).doubleValue();
						else
							lat = (Double) lat_values.get(row);

						if(lon_values.get(row) instanceof Integer)
							lon = ((Integer) lon_values.get(row)).doubleValue();
						else
							lon = (Double) lon_values.get(row);						
						
						
						Point2D p = new Point2D(lat,lon);
						
						spatial_positions.add(p);
					}
					else
					{
						spatial_positions.add(null);
					}

				}
				
				// remove longitude from assigned columns (only one column needed to control insert of point)
				assigned_cols.remove(new Integer(lon_col));

				
			}

			col_index = 0;

			while (col_index < assigned_cols.size()  && !isCancelled()) 
			{

				Integer assigned_col = assigned_cols.get(col_index);
				attribute attr = model.getAttributeOfColumn(assigned_col);
				ArrayList<Object> table_values = model.getValuesOfTableColumn(assigned_col);

				progressMonitor.setNote("Inserting " + attr.getName());

				int decision = MetaDataFromTabView.INSERT;

				// check that the type of the table column is compatible with the type of the attribute
				Object sample_table_value = null;
				Iterator<Object> iter = table_values.iterator();
				while (sample_table_value == null && iter.hasNext()) {
					sample_table_value = iter.next();
				}
				if (sample_table_value != null) {
					if (attribute.INT_VAL.equals(attr.default_storage_field) && !(sample_table_value instanceof Number)) {
						decision = view.get_user_decision_on_mismatched_type(attr, sample_table_value);
					} else if (attribute.DOUBLE_VAL.equals(attr.default_storage_field) && !(sample_table_value instanceof Number)) {
						decision = view.get_user_decision_on_mismatched_type(attr, sample_table_value);
					} else if (attribute.DATETIME_VAL.equals(attr.default_storage_field) && !(sample_table_value instanceof Date)) {
						decision = view.get_user_decision_on_mismatched_type(attr, sample_table_value);
					}
				}

				if (decision == MetaDataFromTabView.INSERT) {
					// check for attribute existance and warn user if exists for some spectra
					ArrayList<Integer> spectrum_ids = new  ArrayList<Integer>();				
					for(int index : LUT.keySet())
					{
						spectrum_ids.add(model.getSpectrum_ids().get(index));			
					}
					int existing_count = check_attribute_existance(attr, spectrum_ids);
					if(existing_count > 0)
					{
						decision = view.get_user_decision_on_existing_fields(attr);
						if(decision == MetaDataFromTabView.DELETE_EXISTING_AND_INSERT_NEW)
						{						
							specchio_client.removeEavMetadata(attr, spectrum_ids, MetaParameter.SPECTRUM_LEVEL);
							decision = MetaDataFromTabView.INSERT;					
						}
					}
				}

				if(decision == MetaDataFromTabView.INSERT)
				{
					Enumeration<Integer> li = LUT.keys();
					int spat_ind = 0;

					// loop over all spectra in matching LUT
					while(li.hasMoreElements())
					{
						Integer spectrum_index = li.nextElement();

						Integer spectrum_id = model.getSpectrum_ids().get(spectrum_index);

						Integer row = LUT.get(spectrum_index);
						Object table_value;
						
						if(attr.getName().equals("Spatial Position"))
							table_value = spatial_positions.get(spat_ind++);
						else
							table_value = table_values.get(row);


						// create new metaparameter for attribute
						if(table_value != null && !table_value.equals(""))
						{
							try {
								MetaParameter mp = MetaParameter.newInstance(attr);
								
								if(mp instanceof MetaSpatialPoint)
									((MetaSpatialPoint) mp).setValue((Point2D) table_value);
								else
									mp.setValue(table_value);

								ArrayList<Integer> ids = new ArrayList<Integer>();
								ids.add(spectrum_id);

								specchio_client.updateEavMetadata(mp, ids);
								param_count++;
							}
							catch (MetaParameterFormatException ex) {
								// could not convert the table value to a meta-parameter of appropriate type
								String message = "Skipped column " + (assigned_col+1) + ", row " + (row+1) + " : " + ex.getMessage();
								progressMonitor.setNote(message);
								System.err.println(message);
							}
						}
						else
						{
							// empty value
							// System.out.println("empty value at row " + row);
						}

					}

				}


				progress += delta_progress;
				setProgress(progress);
				col_index++;
			}
			return param_count;

		}

		@Override
		public void done() {
		}
	}
 	

	public void loadFile(File f) throws BiffException, IOException
	{

		// get a reference to the application's client object
		this.specchio_client = SPECCHIOApplication.getInstance().getClient();
				
	    this.clearModel();
	    
	    model.setWorkbook(Workbook.getWorkbook(f));
		
	}


	public MetaDataFromTabModel getModel() {
		return model;
	}
	
	public void clearModel() {
		model.reset();
	}


	public void setMatchingColumn(int col, int index) {
		
		// actual column depends on the selected index, a NIL (index 0) value is not a valid matching column
		if(index == 0)
		{
			model.matchingColumnIsSet(false);
		}
		else
		{
			model.matchingColumnIsSet(true);
			model.setMatchingColumn(col);
		}
		
	}


	public void setCategory(int col, int index) {
		
		model.setCategoryOfColumn(model.getPossible_categories().get(index), col);
		
	}


	public void setSpectrumIds(ArrayList<Integer> spectrum_ids) {
		// TODO Auto-generated method stub
		model.setSpectrumIds(spectrum_ids);
	}


	public ArrayList<Object> getDbValuesforMatchingCol() throws SPECCHIOClientException {
		
		if(model.matchingColumnIsSet())
		{
			//Category cat = model.getCategoryOfColumn(model.getMatching_col());
			
			String attr_name = model.getAttributeNameOfColumn(model.getMatching_col());
			
			if(!attr_name.equals("NIL"))
			{
				MatlabAdaptedArrayList<Object> values = specchio_client.getMetaparameterValues(model.getSpectrum_ids(), attr_name);			
				model.setMatching_col_db_values(values);		 
				return values;
				
			}
			
		}
		
		return null;
	}


	public void setAttributeAtColumn(int col, combo_table_data itemAt) {
			
		model.setAttributeOfColumn(col, itemAt.id, itemAt.toString());
		
	}


	public void match() {
		
		// carry out matching for currently selected matching column
		if(model.matchingColumnIsSet())
		{
			Hashtable<Integer, Integer> spectrum_to_table_val_matches = new Hashtable<Integer, Integer>();
			
			ArrayList<Object> table_values = model.getValuesOfTableColumn(model.getMatching_col());
			
			ArrayList<Object> matched_table_values = new ArrayList<Object>();
			
			model.setMatching_problems("");
			int i =0;
			for(Object db_val : model.getMatching_col_db_values())
			{
				int first_table_pos = -1;
				int last_table_pos = -1;
				
				ArrayList<Integer> indices;
				if(model.regexIsSet())
				{
					indices = getRegexMatchingIndices(table_values, db_val, model.getRegex_start(), model.getRegex_end());
				}
				else
				{
					indices = getPlainMatchingIndices(table_values, db_val);
				}
				
				if(indices.size() > 0)
				{
					first_table_pos = indices.get(0);
					last_table_pos = indices.get(indices.size()-1);						
				}

				
				if(first_table_pos >= 0 && first_table_pos == last_table_pos)
				{
					spectrum_to_table_val_matches.put(i, first_table_pos);
					
					matched_table_values.add(table_values.get(first_table_pos));
				}
				else if(first_table_pos >= 0 && first_table_pos != last_table_pos)
				{
					//System.out.println("Warning: Ambiguous values!");
					model.setMatching_problems("Ambiguous values in matching column!");
					matched_table_values.add(null);
				}
				else
				{
					// no match
					matched_table_values.add(null);
				}
				
				i++;
			}
			
			model.setMatchingLUT(spectrum_to_table_val_matches);
			model.setMatchedTableValues(matched_table_values);
			model.setNumber_of_matches(spectrum_to_table_val_matches.size());
			model.setNumber_of_missing_matches((table_values.size() - spectrum_to_table_val_matches.size()));
			
//			System.out.println("Number of matches: " + spectrum_to_table_val_matches.size());
//			System.out.println("Missing matches: " + (table_values.size() - spectrum_to_table_val_matches.size()));
			
		}
		
	}
	
	
	private ArrayList<Integer> getPlainMatchingIndices(ArrayList<Object> table_values, Object db_val)
	{
		ArrayList<Integer> indices = new ArrayList<Integer>();
		int i = 0;
		for (Object table_value : table_values)
		{
			boolean match = false;
			
			if (table_value != null && db_val != null) {
				
				if (db_val instanceof Integer) {
					
					if (table_value instanceof Number) {
						match = ((Number)table_value).intValue() == ((Integer)db_val).intValue();
					} else if (table_value instanceof String) {
						match = table_value.equals(db_val.toString());
					}
					
				} else if (db_val instanceof Double) {
					
					if (table_value instanceof Number) {
						match = ((Number)table_value).doubleValue() == ((Double)db_val).doubleValue();
					} else if (table_value instanceof String) {
						match = table_value.equals(db_val.toString());
					}
					
				} else if (db_val instanceof Date) {
					
					if (table_value instanceof Date) {
						match = table_value.equals(db_val);
					} else if (table_value instanceof String) {
						// we could try to parse the date, but we have no idea what format to expect
						match = false;
					}
					
				} else if (db_val instanceof String) {
					
					if (table_value instanceof Number) {
						match = table_value.toString().equals(db_val);
					} else if (table_value instanceof String) {
						match = table_value.equals(db_val);
					}
				}
				
			}
			
			if (match) {
				indices.add(i);
			}
			i++;
		}
		
		return indices;
		
	}
	
	
	private ArrayList<Integer> getRegexMatchingIndices(ArrayList<Object> table_values, Object db_val, String regex_start, String regex_end)
	{
		ArrayList<Integer> indices = new ArrayList<Integer>(); 
		int i = 0;
		for(Object value : table_values)
		{
			if (value != null) {
				
				// build regular expression according the type of the database value
				String regex = model.getRegexForClass(db_val.getClass(), value);
				
				// check for a match
				boolean match;
				if (value instanceof DateTime) {
					// use internal date format for comparison
					match = MetaDate.formatDate((DateTime)value).matches(regex);
				} else if (value instanceof Date) {
					throw new SPECCHIOClientException("getRegexMatchingIndices: Trying to use a Date object, but should be DateTime!");
				} else {
					match = db_val.toString().matches(regex);
				}
				if(match)
					indices.add(i);
			}
			
			i++;
		}

		return indices;
	}
	
	
	
	public ArrayList<Object> getMatchedTableValues()
	{		
		return model.getMatchedTableValues();
	}
	


	public SwingWorker<Integer, Void> getInsertWorker(ProgressMonitor progressMonitor) throws SPECCHIOClientException {
		
		// ensure that a possible re-insert is really resulting in duplicated rows under all circumstances
		specchio_client.clearMetaparameterRedundancyList();
		
		return new MetaDataFromTabWorker(progressMonitor);

	}


	private int check_attribute_existance(attribute attr, ArrayList<Integer> spectrum_ids) throws SPECCHIOClientException {
				
		int count = specchio_client.getExistingMetaparameterCount(attr.getId(), spectrum_ids);
			
			
//		System.out.println("Number of existing records:" + count);	
			
		return count;
		
	}


	public MetaDataFromTabView getView() {
		return view;
	}


	public void setView(MetaDataFromTabView view) {
		this.view = view;
	}


	public void load_matching_table(File file) {
				
		model.setMatchingTable(file);
			
	}


	public void autoMatching() throws SPECCHIOClientException {
		
		Hashtable<String, String> LUT = model.getColumn_element_matching_LUT();
		Hashtable<String, attribute> attr_hash = specchio_client.getAttributesNameHash();
		
		
		// loop over all table column names and try to find match in LUT
		int col = 0;
		for(String col_name : model.getListOfTableColumnNames())
		{
			String matching_attribute_name = LUT.get(col_name);
			
			System.out.println(col_name + "-> " + matching_attribute_name);
			
			if(matching_attribute_name != null && !matching_attribute_name.equals("NIL"))
			{
				
				String attribute_name = matching_attribute_name;
				
				if(attribute_name.equals("Spatial Position (Lon)") || attribute_name.equals("Spatial Position (Lat)"))
					attribute_name = "Spatial Position";
				
				// try and find attribute in database
				attribute attr = attr_hash.get(attribute_name);
				
				// assign category and element for current column
				if(attr != null)
				{
					Category cat = model.getCategories_hash().get(attr.cat_name);
					
					model.setCategoryOfColumn(cat, col);
					model.setAttributeOfColumn(col, attr.getId(), matching_attribute_name);					
				}
				
				
			}

			col++;
		}
	
		
	}

	

}
