package ch.specchio.metadata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.ListIterator;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.gui.SPECCHIOApplication;
import ch.specchio.types.Category;
import ch.specchio.types.MatlabAdaptedArrayList;
import ch.specchio.types.MetaDate;
import ch.specchio.types.attribute;
import jxl.BooleanCell;
import jxl.Cell;
import jxl.CellType;
import jxl.DateCell;
import jxl.NumberCell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

public class MetaDataFromTabModel {
	
	Workbook workbook;
	Sheet sheet;
	int matching_col = -1; // -1 = no matching col selected
	
	MatlabAdaptedArrayList<Object> matching_col_db_values;
	
	ArrayList<Category> possible_categories;
	
	ArrayList<Category> categories_of_columns;
	
	private Hashtable<String, attribute[]> attributes_per_category = new Hashtable<String, attribute[]>();
	private Hashtable<Integer, attribute> attributes_hash = new Hashtable<Integer, attribute>();
	private Hashtable<String, String> column_element_matching_LUT = new Hashtable<String, String>();
	private Hashtable<String, Category> categories_hash = new Hashtable<String, Category>();
	
	int number_of_matches;
	int number_of_missing_matches;
	
	String regex_start = "";
	String regex_end = "";
	String matching_problems = "";
	
	


	public MetaDataFromTabModel()
	{
		try {
			SPECCHIOClient specchio_client = SPECCHIOApplication.getInstance().getClient();
			ArrayList<Category> categories =  specchio_client.getCategoriesInfo();
			setPossible_categories(categories);
			
			ListIterator<Category> li = categories.listIterator();
			
			
			while(li.hasNext())
			{
				Category c = li.next();		
				
				categories_hash.put(c.name, c);

				attribute[] attr_array = specchio_client.getAttributesForCategory(c.name);
				
				attributes_per_category.put(c.name, attr_array);
			}
			
			attributes_hash = specchio_client.getAttributesIdHash();
			
			// empty attribute
			attribute NIL_attribute = new attribute();
			NIL_attribute.name = "NIL";
			NIL_attribute.id = 0;
			
			attributes_hash.put(0, NIL_attribute);
			
			
		} catch (SPECCHIOClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	public java.util.ArrayList<Integer> getSpectrum_ids() {
		return spectrum_ids;
	}

	ArrayList<attribute> attribute_of_columns;
	ArrayList<String> item_name_of_columns = new ArrayList<String>(); // the name shown for the attribute in the GUI, used to deal with special cases like spatial positions that are split into Lon and Lat
	private java.util.ArrayList<Integer> spectrum_ids;
	private boolean matching_is_set = false;
	private Hashtable<Integer, Integer> spectrum_to_table_val_matches;
	private ArrayList<Object> matched_table_values;
	private String column_element_matching_file;
	
	public ArrayList<Category> getCategories_of_columns() {
		return categories_of_columns;
	}

	public void setCategories_of_columns(ArrayList<Category> categories_of_columns) {
		this.categories_of_columns = categories_of_columns;
	}	
	public ArrayList<Category> getPossible_categories() {
		return possible_categories;
	}

	public void setPossible_categories(ArrayList<Category> possible_categories) {
		this.possible_categories = possible_categories;
	}

	public void setCategoryOfColumn(Category cat, int col){
		
		categories_of_columns.set(col, cat);
	}
	
	
	public Category getCategoryOfColumn(int col){
		
		return categories_of_columns.get(col);
	}
	
	public int getMatching_col() {
		return matching_col;
	}

	public void setWorkbook(Workbook w)
	{
		this.workbook = w;
		sheet = workbook.getSheet(0); // first sheet by default
	}
	
	public Workbook getWorkbook()
	{
		return workbook;
	}
	
	public Sheet getCurrentSheet()
	{
		return sheet;
	}

	public void setMatchingColumn(int col) {
		matching_col = col;		
	}

	public void setSpectrumIds(ArrayList<Integer> spectrum_ids) {
		// TODO Auto-generated method stub
		this.spectrum_ids = spectrum_ids;
	}

	public void matchingColumnIsSet(boolean b) {
		// TODO Auto-generated method stub
		matching_is_set = b;
	}
	
	public boolean matchingColumnIsSet() {
		// TODO Auto-generated method stub
		return matching_is_set;
	}

	public String getAttributeNameOfColumn(int col) {
		return attribute_of_columns.get(col).getName();
				
	}
	
	public attribute getAttributeOfColumn(int col) {
		return attribute_of_columns.get(col);
				
	}	
	
	public String getItem_name_of_column(int col) {
		return item_name_of_columns.get(col);
	}


	public void setAttributeOfColumn(int col, Integer id, String item_name)
	{		
		// TODO edit here ...
		attribute atr = this.attributes_hash.get(id);
				
		this.attribute_of_columns.set(col, atr);
		
		item_name_of_columns.set(col, item_name);
	}

	public ArrayList<String> getItem_name_of_columns() {
		return item_name_of_columns;
	}

	public void setItem_name_of_columns(ArrayList<String> item_name_of_columns) {
		this.item_name_of_columns = item_name_of_columns;
	}

	public ArrayList<attribute> getAttribute_of_columns() {
		return attribute_of_columns;
	}

	public void setAttribute_of_columns(ArrayList<attribute> attribute_of_columns) {
		this.attribute_of_columns = attribute_of_columns;
	}

	public MatlabAdaptedArrayList<Object> getMatching_col_db_values() {
		return matching_col_db_values;
	}

	public void setMatching_col_db_values(
			MatlabAdaptedArrayList<Object> matching_col_db_values) {
		this.matching_col_db_values = matching_col_db_values;
	}

	public ArrayList<Object> getValuesOfTableColumn(int col) {
		
		Cell[] column = this.getCurrentSheet().getColumn(col);
		
		ArrayList<Object> values = new ArrayList<Object>();
		
		for(int i = 1; i < column.length; i++)
		{
			Object value;
			if (column[i] instanceof NumberCell)
			{
				String tmp = (column[i]).getContents();
				
				if(tmp.contains("."))
				{
					// assume it's a double for now
					value = new Double(((NumberCell)column[i]).getValue());					
				}
				else
				{
					Double d = ((NumberCell)column[i]).getValue();
					value = d.intValue();	
				}
				

			}
			else if (column[i] instanceof DateCell)
			{
				value = ((DateCell)column[i]).getDate();
				DateTime dateTime = new DateTime(value);
				DateTime d = dateTime.withZone(DateTimeZone.UTC); // enforce UTC, fingers crossed!
				value = d;
			}
			else if (column[i] instanceof BooleanCell)
			{
				value = ((BooleanCell)column[i]).getValue();
			}
			else if (column[i].getType() == CellType.EMPTY)
			{
				value = null;
			}
			else
			{
				// fall back to string value
				value = column[i].getContents();
			}
			
			values.add(value);
		}
		
		return values;
	}

	public void setMatchingLUT(
			Hashtable<Integer, Integer> spectrum_to_table_val_matches) {
		this.spectrum_to_table_val_matches = spectrum_to_table_val_matches;
		
	}

	public Hashtable<Integer, Integer> getMatchingLUT() {
		return spectrum_to_table_val_matches;
	}

	public void setMatchedTableValues(ArrayList<Object> matched_table_values) {
		this.matched_table_values = matched_table_values;
		
	}

	public ArrayList<Object> getMatchedTableValues() {
		return matched_table_values;
	}

	public boolean validAttribute(int col) {
		
		String attr_name = this.getAttributeNameOfColumn(col);
		
		if(attr_name.equals("NIL")) return false;
		else return true;
	}

	public void reset() {
		this.matchingColumnIsSet(false);
		
	}
	
	public int getNumber_of_matches() {
		return number_of_matches;
	}

	public void setNumber_of_matches(int number_of_matches) {
		this.number_of_matches = number_of_matches;
	}

	public int getNumber_of_missing_matches() {
		return number_of_missing_matches;
	}

	public void setNumber_of_missing_matches(int number_of_missing_matches) {
		this.number_of_missing_matches = number_of_missing_matches;
	}	
	
	public int getNumberOfSpectra()
	{
		if(spectrum_ids == null)
			return 0;
		else
			return this.spectrum_ids.size();
	}
	
	public int getNumberOfAssignedColumns()
	{
		int n = 0;
		for(attribute attr_name : attribute_of_columns)
		{
			if(!attr_name.getName().equals("NIL")) n++;			
		}
		
		if(this.matching_is_set && !getAttributeNameOfColumn(this.getMatching_col()).equals("NIL")) n = n - 1; // correct for matching column
		
		return n;
		
	}

	public int getNumberOfAssignableColumns() {
		
		int n = this.getCurrentSheet().getColumns();
		
		n = n - getNumberOfAssignedColumns();
		
		if(this.matching_is_set && !getAttributeNameOfColumn(this.getMatching_col()).equals("NIL")) n = n - 1; // correct for matching column
		
		return n;
	}
	
	
	public ArrayList<Integer> getColumnNumbersOfAssignedColumns()
	{
		ArrayList<Integer> assigned_col_indices = new ArrayList<Integer>();
		
		int n = 0;
		for(attribute attr_name : attribute_of_columns)
		{
			if(!attr_name.getName().equals("NIL") && n != getMatching_col()) assigned_col_indices.add(n);	
			
			 n++;	
		}
		
		return assigned_col_indices;
	}

	public boolean regexIsSet() {
		
		return regex_start.length()>0 || regex_end.length()>0;
	}

	public String getRegex_start() {
		return regex_start;
	}

	public void setRegex_start(String regex_start) {
		this.regex_start = regex_start;
	}

	public String getRegex_end() {
		return regex_end;
	}

	public void setRegex_end(String regex_end) {
		this.regex_end = regex_end;
	}
	
	
	public String getRegexExample() {
		
		String regex = "";
		
		if(matching_is_set)
		{
			
			 ArrayList<Object> matching_values = getValuesOfTableColumn(getMatching_col());
			 ArrayList<Object> db_values = getMatching_col_db_values();
			
			if(matching_values.size() > 0)
			{
				Object val = matching_values.get(0);
				if (db_values.size() > 0) {
					regex = getRegexForClass(db_values.get(0).getClass(), val);
				} else {
					regex = getRegexForClass(String.class, val);
				}
				
			}		
		}

		return regex;
	}
	
	
	public String getRegexForClass(Class<?> cl, Object value) {
		
		String regex;
		
		if (cl.isAssignableFrom(Integer.class)) {
			if (value instanceof Number) {
				regex = regex_start + Integer.toString(((Number)value).intValue()) + regex_end;
			} else {
				regex = regex_start + value.toString() + regex_end;
			}
		} else if (cl.isAssignableFrom(Double.class)) {
			if (value instanceof Number) {
				regex = regex_start + Double.toString(((Number)value).doubleValue()) + regex_end;
			} else {
				regex = regex_start + value.toString() + regex_end;
			}
		} else if (cl.isAssignableFrom(Date.class)) {
			if (value instanceof DateTime) {
				regex = regex_start + MetaDate.formatDate((DateTime)value) + regex_end;
			} else if (value instanceof Date) {
				throw new SPECCHIOClientException("getRegexForClass: Trying to use a Date object, but should be DateTime!");
			} else {
				regex = regex_start + value.toString() + regex_end;
			}
		} else {
			regex = regex_start + value.toString() + regex_end;
		}
		
		return regex;
		
	}

	public void setMatchingTable(File file) {
		try {
			Workbook w = Workbook.getWorkbook(file);
			
			setElement_column_matching_file(file.getName());
			
			// get 1st sheet
			Sheet s = w.getSheet(0);
			
			// create hashtable
			this.column_element_matching_LUT.clear();
			
			for(int r = 1;r < s.getRows(); r++)
			{
				String column_name = s.getCell(0, r).getContents();
				String attribute_name = s.getCell(1, r).getContents();
				column_element_matching_LUT.put(column_name, attribute_name);
			}
			
			
		} catch (BiffException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public String getElement_column_matching_file() {
		return column_element_matching_file;
	}

	public void setElement_column_matching_file(String element_column_matching_file) {
		this.column_element_matching_file = element_column_matching_file;
	}
		
	
	public ArrayList<String> getListOfTableColumnNames()
	{
		ArrayList<String> col_names = new ArrayList<String>();
		
		for(int c = 0; c < sheet.getColumns(); c++)
		{
			col_names.add(sheet.getCell(c, 0).getContents());
		}
		return col_names;
	}

	public Hashtable<String, String> getColumn_element_matching_LUT() {
		return column_element_matching_LUT;
	}

	public void setColumn_element_matching_LUT(
			Hashtable<String, String> column_element_matching_LUT) {
		this.column_element_matching_LUT = column_element_matching_LUT;
	}

	public Hashtable<String, Category> getCategories_hash() {
		return categories_hash;
	}

	public void setCategories_hash(Hashtable<String, Category> categories_hash) {
		this.categories_hash = categories_hash;
	}

	public String getMatching_problems() {
		return matching_problems;
	}

	public void setMatching_problems(String matching_problems) {
		this.matching_problems = matching_problems;
	}	
	
}
