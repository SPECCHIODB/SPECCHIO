package ch.specchio.jaxb;

import org.ujmp.core.Matrix;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

//@XmlRootElement(name="Matrix")
@XmlRootElement
public class XmlMatrix {

    private Matrix obj;

    public XmlMatrix() {};
    public XmlMatrix(Matrix obj) { this.obj = obj; }

    //@XmlElement(name="contents")
    @XmlAttribute
    public Matrix getMatrix() { return this.obj; }
    public void setMatrix(Matrix obj) { this.obj = obj; }
}
