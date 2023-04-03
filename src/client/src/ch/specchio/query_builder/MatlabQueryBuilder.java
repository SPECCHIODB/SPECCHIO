package ch.specchio.query_builder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.*;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.queries.*;
import ch.specchio.types.attribute;


public class MatlabQueryBuilder {

	private SPECCHIOClient specchio_client;
	
	/**
	 * Default constructor.
	 */
	public MatlabQueryBuilder(SPECCHIOClient specchio_client) {
		this.specchio_client = specchio_client;
	}


	/**
	 * Escape a string.
	 * 
	 * @param s	the string to be escaped
	 * 
	 * @return the input string with percent signs replaced by %% and apostrophes replaced by ''
	 */
	private String escapeString(String s) {
		
		return s.replaceAll("%", "%%").replaceAll("'", "''");
		
	}
	
	
	/**
	 * Format an array literal.
	 * 
	 * @param values	the array
	 * 
	 * @return a string representing the equivalent array in Matlab
	 */
	private String quoteArray(Object values[]) {
		
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("[");
		for (Object value : values) {
			if (sbuf.length() > 1) {
				sbuf.append(",");
			}
			sbuf.append(quoteValue(value));
		}
		sbuf.append("]");
		
		return sbuf.toString();
		
	}
	
	
	/**
	 * Format a date literal.
	 * 
	 * @param date	the date
	 * 
	 * @return a string literal representing the date
	 */
	private String quoteDate(Date date) {
		
		TimeZone tz = TimeZone.getTimeZone("UTC");
		Calendar cal = Calendar.getInstance(tz);
		cal.setTime(date);
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
		formatter.setTimeZone(tz);
		
		return quoteString(formatter.format(cal.getTime()));
		
	}
	
	
	/**
	 * Format an array literal.
	 * 
	 * @param values	the contents of the array
	 * 
	 * @return a string representing the equivalent array in Matlab
	 */
	private String quoteList(List<?> values) {
		
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("[");
		for (Object value : values) {
			if (sbuf.length() > 1) {
				sbuf.append(",");
			}
			sbuf.append(quoteValue(value));
		}
		sbuf.append("]");
		
		return sbuf.toString();
		
	}
	
	
	/**
	 * Format a string literal.
	 * 
	 * @param s	the string
	 * 
	 * @return the input string, escaped and surrounded by quotes
	 */
	private String quoteString(String s) {
		
		return "'" + escapeString(s) + "'";
		
	}
	
	
	/**
	 * Quote a literal.
	 * 
	 * @param value	the object to be quoted
	 *
	 * @return a Matlab literal representing the input object
	 */
	private String quoteValue(Object value) {
		
		if (value == null) {
			return "null";
		} else if (value.getClass().isArray()) {
			return quoteArray((Object[])value);
		} else if (value instanceof List<?>) {
			return quoteList((List<?>)value);
		} else if (value instanceof String) {
			return quoteString((String)value);
		} else if (value instanceof Integer) {
			return value.toString();
		} else if (value instanceof Double || value instanceof Float) {
			return value.toString();
		} else if (value instanceof Date) {
			return quoteDate((Date)value);
		} else {
			// don't know what to do with this
			return quoteString(value.toString());
		}
	}
	
	
	/**
	 * Generate Matlab code that will execute a given query.
	 * 
	 * @param writer	the writer object to which the code will be written
	 * @param query		the query
	 * 
	 * @throws IOException	write error
	 */
	public void writeMatlabCode(Writer writer, Query query) throws IOException {
		
		// create a print writer connected to the output
		PrintWriter out = new PrintWriter(writer);
		
		// output code to create the query object
		out.println("query = Query(" + quoteString(query.getTableName()) + ");");
		out.println("query.setQueryType(Query.SELECT_QUERY);");
		out.println("attr_hash = specchio_client.getAttributesNameHash();");
		out.println();
		
		// output code to add columns to the query
		for (String column : query.getColumns()) {
			out.println("query.addColumn(" + quoteString(column) + ")");
		}
		out.println();
		
		// output code to add conditions to the query
		for (QueryCondition cond : query.getConditionFields()) {
			
			// output code to construct an object of the appropriate type
			if (cond instanceof SpectrumQueryCondition) {
				
				out.println(
					"cond = SpectrumQueryCondition(" +
							quoteString(cond.getTableName()) +
							", " +
							quoteString(cond.getFieldName()) +
					");"
				);
				
			} else if (cond instanceof QueryConditionObject) {
				
				out.println(
					"cond = QueryConditionObject(" +
							quoteString(cond.getTableName()) +
							", " +
							quoteString(cond.getFieldName()) +
					");"
				);
				
			} else if (cond instanceof EAVQueryConditionObject) {

				EAVQueryConditionObject eavCond = (EAVQueryConditionObject)cond;
				if (eavCond.getAttributeName() != null) {
					out.println(
							"cond = EAVQueryConditionObject(attr_hash.get(" +
									quoteString(eavCond.getAttributeName()) +
									"));"
					);

				} else {
					out.println(
						"cond = QueryConditionObject(" +
							quoteString(cond.getTableName()) +
							", " +
							quoteString(cond.getFieldName()) +
						");"
					);
				}
				
			}
			
			// output code to set operator and value
			if (cond instanceof QueryConditionObject)
			{
				// ids need special coding
				out.println("id_array = " + quoteValue(cond.getValue()) + ";");
				out.println("ids_list = java.util.ArrayList();");
				out.println("for i=1:size(id_array,2) ids_list.add(id_array(i)); end;");				
				out.println("cond.setValue(ids_list);");				
			}
			else if(cond instanceof EAVQueryConditionObject && cond.getFieldName().equals("taxonomy_id"))
			{
				out.println("name_id_hash = specchio_client.getTaxonomyHash(attr_hash.get(" +
						quoteString(cond.getAttributeName()) +
						"));"
				);

				Hashtable<String, attribute> attr_hash = specchio_client.getAttributesNameHash();
				Hashtable<Integer, String> id_name_hash = specchio_client.getTaxonomyIdToNameHash(attr_hash.get(cond.getAttributeName()).getId());
				Integer taxonomy_item_id = ((EAVQueryConditionObject) cond).getIntegerValue();
				String taxonomy_item_name = id_name_hash.get(taxonomy_item_id);

						out.println("cond.setValue(num2str(name_id_hash.get(" + quoteValue(taxonomy_item_name) + ")));");

			}
			else
			{
				out.println("cond.setValue(" + quoteValue(cond.getValue()) + ");");
			}
			
			out.println("cond.setOperator(" + quoteString(cond.getOperator()) + ");");
			
			// output code to add the condition to the query
			out.println("query.add_condition(cond);");
			out.println();
			
		}
		
		// output code to get the ids
		out.println("ids = specchio_client.getSpectrumIdsMatchingQuery(query);");
		
	}

}
