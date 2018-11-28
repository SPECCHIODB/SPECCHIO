package ch.specchio.jaxb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Converts XmlInteger objects used by JAXB into normal Integer objects used
 * by everyone else.
 */
public class XmlIntegerAdapter extends XmlAdapter<XmlInteger, Integer> {

	/**
	 * Convert a Integer into an XmlInteger.
	 * 
	 * @param n	the Integer
	 * 
	 * @return a new XmlInteger object containing the input Integer
	 */
	@Override
	public XmlInteger marshal(Integer n)  {
		
		return new XmlInteger(n);
	}
	
	
	/**
	 * Convert an array of Integers into an array of XmlIntegers.
	 * 
	 * @param n	the array
	 * 
	 * @return a new array of XmlInteger objects
	 */
	public XmlInteger[] marshalArray(Integer n[]) {
		
		XmlInteger xml[] = new XmlInteger[n.length];
		for (int i = 0; i < n.length; i++) {
			xml[i] = marshal(n[i]);
		}
		
		return xml;
		
	}
	
	
	/**
	 * Convert a collection of Integers into an array of XmlIntegers.
	 * 
	 * @param c	the collection
	 * 
	 * @return a new array of XmlInteger objects
	 */
	public XmlInteger[] marshalArray(Collection<Integer> c) {
		
		XmlInteger xml[] = new XmlInteger[c.size()];
		int i = 0;
		for (Integer n : c) {
			xml[i] = marshal(n);
			i++;
		}
		
		return xml;
		
	}
	

	/**
	 * Convert an XmlInteger into a Integer.
	 * 
	 * @param xml	the XML Integer
	 * 
	 * @return a reference to the Integer contained in the input object
	 */
	@Override
	public Integer unmarshal(XmlInteger xml)  {
		
		return xml.getInteger();
		
	}
	
	
	/**
	 * Convert an array of XmlInteger objects in an array of Integer objects.
	 * 
	 * @param xml	the XML Integer array
	 * 
	 * @return a new array of Sring objects
	 */
	public Integer[] unmarshalArray(XmlInteger[] xml) {
		
		Integer n[] = new Integer[xml.length];
		for (int i = 0; i < xml.length; i++) {
			n[i] = xml[i].getInteger();
		}
		
		return n;
		
	}
	
	
	/**
	 * Convert a list of XmlInteger objects into a list of Integer objects
	 * 
	 * @param xmlInts	the list
	 * 
	 * @return a list of Integer objects
	 */
	public List<Integer> unmarshalList(List<XmlInteger> xmlInts) {
		
		List<Integer> ints = new ArrayList<Integer>(xmlInts.size());
		for (XmlInteger xmlInt : xmlInts) {
			ints.add(unmarshal(xmlInt));
		}
		
		return ints;
		
	}

}
