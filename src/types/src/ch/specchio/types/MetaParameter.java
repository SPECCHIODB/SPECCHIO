package ch.specchio.types;

import java.util.ArrayList;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ch.specchio.jaxb.XmlMetaParameterValueAdapter;


/**
 * This class represents a metadata parameter.
 */
@XmlRootElement(name="meta_parameter")
@XmlSeeAlso({MetaDate.class,MetaDocument.class,MetaFile.class,MetaImage.class,MetaMatrix.class,MetaSimple.class,MetaTaxonomy.class,MetaLink.class,MetaBoolean.class,MetaSpatialGeometry.class,MetaSpatialPoint.class,MetaSpatialPolygon.class,MetaSpatialPolyline.class, SerialisableBufferedImage.class})
public abstract class MetaParameter {
	
	public static final int SPECTRUM_LEVEL = 0;
	public static final int HIERARCHY_LEVEL = 1;
	public static final int CALIBRATION_LEVEL = 2;
	//public static final int HIERARCHY_INHERITANCE_LEVEL = 2;

	private int level = SPECTRUM_LEVEL;

	private String default_storage_field = null;
	private Integer attribute_id = null;
	private String attribute_name;
	private String category_name;
	private String category_value;
	private Object value;
	private Integer unit_id = 0;
	private String unit_name;
	private Integer default_unit_id = 0;
	private Integer eav_id = 0;
	private String description; // help string
	private String annotation;
	private Boolean is_boolean_value = false; // setting for integer values acting as booleans
	private Boolean blob_lazy_loading = false;
	private Integer blob_size = 0; // size in bytes
	
	protected MetaParameter() {
		
	}
	
	
	protected MetaParameter(int eav_id) {
		
		this.eav_id = eav_id;
		
	}
	
	
	protected MetaParameter(String category_name, String category_value, Object meta_value) throws MetaParameterFormatException
	{
		this.category_name = category_name;
		this.category_value = category_value;
		setValue(meta_value);
	}
	
	protected MetaParameter(attribute attr, Object meta_value) throws MetaParameterFormatException
	{
		this(attr);
		setValue(meta_value);
	}
	
	protected MetaParameter(attribute attr)
	{
		this.category_name = attr.cat_name;
		this.attribute_id = attr.getId();
		this.attribute_name = attr.getName();
		this.unit_id = attr.default_unit_id;
		this.default_storage_field = attr.getDefaultStorageField();
		this.description = attr.description;
		this.is_boolean_value = attr.is_boolean_value;
	}
	
	
	@XmlElement(name="attribute_id")
	public Integer getAttributeId() { return this.attribute_id; }
	public void setAttributeId(Integer attribute_id) {	this.attribute_id = attribute_id; }
	
	@XmlElement(name="attribute_name")
	public String getAttributeName() { return this.attribute_name; }
	public void setAttributeName(String attribute_name) { this.attribute_name = attribute_name; }
	
	@XmlElement(name="category_name")
	public String getCategoryName() { return this.category_name; }
	public void setCategoryName(String category_name) { this.category_name = category_name; }
	
	@XmlElement(name="category_value")
	public String getCategoryValue() { return this.category_value; }
	public void setCategoryValue(String category_value) { this.category_value = category_value; }
	
	@XmlElement(name="default_unit_id")
	public Integer getDefaultUnitId() { return this.default_unit_id; }
	public void setDefaultUnitId(int default_unit_id) { this.default_unit_id = default_unit_id; }
	
	@XmlElement(name="default_storage_field")
	public String getDefaultStorageField() { return this.default_storage_field; }
	public void setDefaultStorageField(String default_storage_field) { this.default_storage_field = default_storage_field; }
	
	
	@XmlElement(name="is_boolean_value")
	public Boolean getIsBooleanValue() { return this.is_boolean_value; }
	public void setIsBooleanValue(Boolean is_boolean_value) { this.is_boolean_value = is_boolean_value; }
	
	
	
	@XmlElement(name="eav_id")
	public Integer getEavId() { return this.eav_id; }
	public void setEavId(Integer eav_id) { this.eav_id = eav_id; }
	
	
	public int getFieldPos() {

		if(this.default_storage_field == null)
		{
			return 0;
		}
		
		if (this.default_storage_field.equals("int_val")) return 1;
		else if (default_storage_field.equals("double_val")) return 2;
		else if (default_storage_field.equals("string_val")) return 3;
		else if (default_storage_field.equals("binary_val")) return 4;
		else if (default_storage_field.equals("datetime_val")) return 5;
		else if (default_storage_field.equals("taxonomy_id")) return 6;
		
		return 0;
	}	
	
	
	@XmlElement(name="unit_id")
	public Integer getUnitId()
	{
		return this.unit_id;
	}
	
	
	public void setUnitId(Integer unit_id) {
		
		this.unit_id = unit_id;
		
	}
	
	
	@XmlElement(name="unit_name")
	public String getUnitName() {
		
		return this.unit_name;
		
	}
	
	
	public void setUnitName(String unit_name) {
		
		this.unit_name = unit_name;
		
	}
	
	
	public void setUnits(Units u) {
		
		setUnitId(u.id);
		setUnitName(u.short_name);
		
	}
	
	
	@XmlElement(name="value")
	@XmlJavaTypeAdapter(XmlMetaParameterValueAdapter.class)
	public Object getValue() {
		
		return this.value;
		
	}
	
	// values converted for DB insert: by default just the values as is
	public Object getEAVValue() {
		
		return this.value;
		
	}	
	
	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}	
	
	public void setValue(Object value) throws MetaParameterFormatException {
		
			this.value = value;
		
	}
	
	
	public void setValue(Object value, String unit_string) throws MetaParameterFormatException
	{
		this.setValue(value);
		this.setUnitName(unit_string);
	}
	
	
	
	@XmlElement(name="description")
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}		
	
	
	@XmlElement(name="annotation")
	public String getAnnotation() {
		return annotation;
	}
	
	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}	
	
	
	public static MetaParameter newInstance()
	{
		return new MetaSimple();
	}
	
	
	public static MetaParameter newInstance(attribute attr)
	{
		MetaParameter mp;
		
		// construct a meta-parameter of the type appropriate to the attribute
		if (attr.default_storage_field.equals("datetime_val"))
			mp = new MetaDate(attr);
		else if (attr.blob_data_type.equals("Image") || attr.cat_name.equals("Pictures"))
			mp = new MetaImage(attr);
		else if (attr.blob_data_type.equals("PDF") || attr.cat_name.equals("PDFs"))
			mp = new MetaDocument(attr);
		else if (attr.default_storage_field.equals("taxonomy_id"))
			mp = new MetaTaxonomy(attr);
		else if (attr.is_boolean_value)
			mp = new MetaBoolean(attr);		
		else if (attr.default_storage_field.equals("spatial_val"))
		{
			if(attr.name.equals("Spatial Position"))
				mp = new MetaSpatialPoint(attr);
			else if (attr.name.equals("Spatial Transect"))
				mp = new MetaSpatialPolyline(attr);
			else 
				mp = new MetaSpatialPolygon(attr);
				
		}
		else
			mp = new MetaSimple(attr);
		
		// create an empty value
		mp.setEmptyValue();
		
		return mp;
	}
	
	
	public static MetaParameter newInstance(attribute attr, Object meta_value) throws MetaParameterFormatException
	{
		MetaParameter mp;
		
		if (attr.getDefaultStorageField().equals("datetime_val") && MetaDate.supportsValue(meta_value)) {
			mp = new MetaDate(attr, meta_value);
		} else if (attr.getDefaultStorageField().equals("spectrum_id") && MetaLink.supportsValue(meta_value)) {
			mp = new MetaLink(attr, meta_value);
		} else if (MetaImage.supportsValue(meta_value)) {
			mp = new MetaImage(attr, meta_value);
		} else if (MetaDocument.supportsValue(meta_value)) {
			mp = new MetaDocument(attr, meta_value);
		} else if (MetaMatrix.supportsValue(meta_value)) {
			mp = new MetaMatrix(attr, meta_value);
		} else if (attr.getDefaultStorageField().equals("taxonomy_id") && MetaTaxonomy.supportsValue(meta_value)) {
			mp = new MetaTaxonomy(attr, meta_value);
		} else if (attr.is_boolean_value) {
			mp = new MetaBoolean(attr, meta_value);	
		} else if (attr.getDefaultStorageField().equals("spatial_val")) {
			if(attr.name.equals("Spatial Position"))
				mp = new MetaSpatialPoint(attr, meta_value);
			else if (attr.name.equals("Spatial Transect"))
				mp = new MetaSpatialPolyline(attr, meta_value);
			else 
				mp = new MetaSpatialPolygon(attr, meta_value);						
		} else {
			mp = new MetaSimple(attr, meta_value);
		}
		 
		 return mp;
		 
	}
	
	abstract public boolean hasEqualValue(MetaParameter mp);

	
	
//	public static MetaParameter newInstance(String category_name, String category_value) throws MetaParameterFormatException
//	{	
//		return newInstance(category_name, category_value, null);	
//	}
	
	
	public abstract boolean allows_multi_insert();
	
	public abstract void setEmptyValue();
	
	public abstract String valueAsString();


	public Boolean getBlob_lazy_loading() {
		return blob_lazy_loading;
	}


	public void setBlob_lazy_loading(Boolean blob_lazy_loading) {
		this.blob_lazy_loading = blob_lazy_loading;
	}


	public Integer getBlob_size() {
		return blob_size;
	}
	
	public Integer getBlob_size_in_MB() {
		return blob_size / 1024 / 1024;
	}
	


	public void setBlob_size(Integer blob_size) {
		this.blob_size = blob_size;
	}



}
