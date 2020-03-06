package ch.specchio.query_builder;


public class EAVQueryField extends QueryField{
	
	String attribute_name;
	int int_minVal, int_maxVal;
	double double_minVal, double_maxVal;


	public EAVQueryField(String name, String default_storage_field) {
		
		this.attribute_name = name;
		this.fieldname = default_storage_field;
	}

	public EAVQueryField(String attribute_name, int int_minVal, int int_maxVal, double double_minVal, double double_maxVal) {
		this.attribute_name = attribute_name;
		this.int_minVal = int_minVal;
		this.int_maxVal = int_maxVal;
		this.double_minVal = double_minVal;
		this.double_maxVal = double_maxVal;
	}

	public String get_fieldname() {

		return fieldname;
	}

	public int getInt_minVal() {
		return int_minVal;
	}

	public void setInt_minVal(int int_minVal) {
		this.int_minVal = int_minVal;
	}

	public int getInt_maxVal() {
		return int_maxVal;
	}

	public void setInt_maxVal(int int_maxVal) {
		this.int_maxVal = int_maxVal;
	}

	public double getDouble_minVal() {
		return double_minVal;
	}

	public void setDouble_minVal(double double_minVal) {
		this.double_minVal = double_minVal;
	}

	public double getDouble_maxVal() {
		return double_maxVal;
	}

	public void setDouble_maxVal(double double_maxVal) {
		this.double_maxVal = double_maxVal;
	}

	@Override
	public void textReport() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getLabel() {
		return attribute_name;
	}



}
