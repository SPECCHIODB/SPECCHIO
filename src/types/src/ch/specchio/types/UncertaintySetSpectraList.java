package ch.specchio.types;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

@XmlRootElement(name="UncertaintySetSpectraList")
public class UncertaintySetSpectraList {


    @XmlElement
    public int uncertainty_set_id;

    @XmlElement public ArrayList<Integer> spectrum_ids = new ArrayList<Integer>();

}
