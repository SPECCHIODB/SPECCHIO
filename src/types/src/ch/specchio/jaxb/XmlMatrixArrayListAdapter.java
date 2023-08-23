package ch.specchio.jaxb;

import org.ujmp.core.Matrix;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.ArrayList;

public class XmlMatrixArrayListAdapter  extends XmlAdapter<XmlMatrixArrayList, ArrayList<Matrix>> {

    public XmlMatrixArrayListAdapter() {}

    @Override
    public ArrayList<Matrix> unmarshal(XmlMatrixArrayList v) throws Exception {
        return null;
    }

    @Override
    public XmlMatrixArrayList marshal(ArrayList<Matrix> v) throws Exception {
        return null;
    }

}
