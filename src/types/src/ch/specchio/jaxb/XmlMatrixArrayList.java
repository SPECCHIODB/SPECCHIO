package ch.specchio.jaxb;

import org.ujmp.core.Matrix;

import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;

public class XmlMatrixArrayList {

    private ArrayList<Matrix> obj;

    public XmlMatrixArrayList(ArrayList<Matrix> obj) { this.obj = obj; }

    @XmlElement(name="contents")
    public ArrayList<Matrix> getMatrices() { return this.obj; }
    public void setMatrices(ArrayList<Matrix> obj) { this.obj = obj; }


}
