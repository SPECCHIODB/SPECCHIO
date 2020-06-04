package ch.specchio.types;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="QueryAttribute")
public class QueryAttribute {

    private String attributeName;
    private int attributeId;
    private String minVal;
    private String maxVal;
    private String defaultStorageField;

    public QueryAttribute() {
    }

    public QueryAttribute(String attributeName, int attributeId, String defaultStorageField, String minVal, String maxVal) {
        this.attributeName = attributeName;
        this.attributeId = attributeId;
        this.defaultStorageField = defaultStorageField;
        this.minVal = minVal;
        this.maxVal = maxVal;
    }

    @XmlElement(name="attributeName")
    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    @XmlElement(name="attributeId")
    public int getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(int attributeId) {
        this.attributeId = attributeId;
    }

    @XmlElement(name="minVal")
    public String getMinVal() {
        return minVal;
    }

    public void setMinVal(String minVal) {
        this.minVal = minVal;
    }

    @XmlElement(name="maxVal")
    public String getMaxVal() {
        return maxVal;
    }

    public void setMaxVal(String maxVal) {
        this.maxVal = maxVal;
    }

    @XmlElement(name="defaultStorageField")
    public String getDefaultStorageField() {
        return defaultStorageField;
    }

    public void setDefaultStorageField(String defaultStorageField) {
        this.defaultStorageField = defaultStorageField;
    }
}
