package ch.specchio.jaxb;

import org.ujmp.core.Matrix;
import org.ujmp.core.doublematrix.DenseDoubleMatrix;
import org.ujmp.core.doublematrix.factory.DefaultDenseDoubleMatrixFactory;
import org.ujmp.core.doublematrix.impl.DefaultDenseDoubleMatrixMultiD;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Hashtable;
import java.util.Map;

public class XmlMatrixAdapter extends XmlAdapter<XmlMatrix, DefaultDenseDoubleMatrixMultiD> {

    @Override
    public XmlMatrix marshal(DefaultDenseDoubleMatrixMultiD m)  {

        return new XmlMatrix(m);
    }

    @Override
    public DefaultDenseDoubleMatrixMultiD unmarshal(XmlMatrix xm)  {

        DefaultDenseDoubleMatrixFactory factory = new DefaultDenseDoubleMatrixFactory();
        DefaultDenseDoubleMatrixMultiD m = (DefaultDenseDoubleMatrixMultiD) factory.zeros(1,1,1);

        return m;
    }
}
