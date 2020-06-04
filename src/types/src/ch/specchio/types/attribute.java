package ch.specchio.types;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.attribute.SimpleNullableAttribute;
import com.googlecode.cqengine.query.option.QueryOptions;
import org.joda.time.DateTime;

import javax.xml.bind.annotation.*;

@XmlRootElement(name="attribute")
public class attribute {
	
	public static final String INT_VAL = "int_val";
	public static final String DOUBLE_VAL = "double_val";
	public static final String STRING_VAL = "string_val";
	public static final String BINARY_VAL = "binary_val";
	public static final String DATETIME_VAL = "datetime_val";
	public static final String TAXONOMY_VAL = "taxonomy_id";
	public static final String SPECTRUM_VAL = "spectrum_id";
	public static final String SPATIAL_VAL = "spatial_val";
	private String MIN_INT_VAL;
	private String MAX_INT_VAL;
	private String MIN_DOUBLE_VAL;
	private String MAX_DOUBLE_VAL;
	private String MIN_DATETIME_VAL;
	private String MAX_DATETIME_VAL;

	@XmlElement public int id;
	@XmlElement public String name;
	@XmlElement public int category_id;
	@XmlElement public String cat_name;
	@XmlElement public String cat_string_val;
	@XmlElement public int default_unit_id;
	@XmlElement public String default_storage_field;
	@XmlElement public String description;
	@XmlElement public int cardinality;
	@XmlElement public boolean is_boolean_value = false;
	@XmlElement public String blob_data_type = "";

	// FIELDS FOR THE COLLECTION CQ ENGINE
	public int spectrumId;
	public int int_val;
	public double double_val;
	public String string_val;
	public String binary_val;
	public DateTime dt_val;

	public String getName()
	{
		return name;
	}
	
	public int getId()
	{
		return id;
	}
	
	public String getDefaultStorageField() {
		
		return default_storage_field;
		
	}

	public String getMIN_INT_VAL() {
		return MIN_INT_VAL;
	}

	public void setMIN_INT_VAL(String MIN_INT_VAL) {
		this.MIN_INT_VAL = MIN_INT_VAL;
	}

	public String getMAX_INT_VAL() {
		return MAX_INT_VAL;
	}

	public void setMAX_INT_VAL(String MAX_INT_VAL) {
		this.MAX_INT_VAL = MAX_INT_VAL;
	}

	public String getMIN_DOUBLE_VAL() {
		return MIN_DOUBLE_VAL;
	}

	public void setMIN_DOUBLE_VAL(String MIN_DOUBLE_VAL) {
		this.MIN_DOUBLE_VAL = MIN_DOUBLE_VAL;
	}

	public String getMAX_DOUBLE_VAL() {
		return MAX_DOUBLE_VAL;
	}

	public void setMAX_DOUBLE_VAL(String MAX_DOUBLE_VAL) {
		this.MAX_DOUBLE_VAL = MAX_DOUBLE_VAL;
	}

	public String getMIN_DATETIME_VAL() {
		return MIN_DATETIME_VAL;
	}

	public void setMIN_DATETIME_VAL(String MIN_DATETIME_VAL) {
		this.MIN_DATETIME_VAL = MIN_DATETIME_VAL;
	}

	public String getMAX_DATETIME_VAL() {
		return MAX_DATETIME_VAL;
	}

	public void setMAX_DATETIME_VAL(String MAX_DATETIME_VAL) {
		this.MAX_DATETIME_VAL = MAX_DATETIME_VAL;
	}

	public static final Attribute<attribute, Integer> SPECTRUM_ID = new SimpleNullableAttribute<attribute, Integer>("spectrumId") {
		public Integer getValue(attribute attr, QueryOptions queryOptions) { return attr.spectrumId; }
	};

	public static final Attribute<attribute, Integer> INT_VALUE = new SimpleNullableAttribute<attribute, Integer>("integerMin") {
		public Integer getValue(attribute attr, QueryOptions queryOptions) { return attr.int_val; }
	};


	public static final Attribute<attribute, Double> DOUBLE_VALUE = new SimpleNullableAttribute<attribute, Double>("doubleMin") {
		public Double getValue(attribute attr, QueryOptions queryOptions) { return attr.double_val; }
	};


	public static final Attribute<attribute, String> NAME = new SimpleNullableAttribute<attribute, String>("attributeName") {
		public String getValue(attribute attr, QueryOptions queryOptions) { return attr.name; }
	};

	public static final Attribute<attribute, String> BINARY_VALUE = new SimpleNullableAttribute<attribute, String>("doubleMin") {
		public String getValue(attribute attr, QueryOptions queryOptions) { return attr.string_val; }
	};

	public static final Attribute<attribute, DateTime> DT_VALUE = new SimpleNullableAttribute<attribute, DateTime>("dtMin") {
		public DateTime getValue(attribute attr, QueryOptions queryOptions) { return attr.dt_val; }
	};

	public int getSpectrumId() {
		return spectrumId;
	}
}
