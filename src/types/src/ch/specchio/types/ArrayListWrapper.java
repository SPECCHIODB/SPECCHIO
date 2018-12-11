package ch.specchio.types;

import java.util.ArrayList;
import java.util.List;

//import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlSeeAlso;

// see: http://blog.bdoughan.com/2012/11/creating-generic-list-wrapper-in-jaxb.html

@XmlRootElement(name="array_list_wrapper")
//@XmlSeeAlso({Point2D.class, spatial_pos.class})
@XmlSeeAlso({Point2D.class})


public class ArrayListWrapper<T> {
	

	
	private List<T> list;

	public ArrayListWrapper() {
		this(new ArrayList<T>());
	}
	
	public ArrayListWrapper(ArrayList<T> list) {
		this.list = list;
	}	

	@XmlAnyElement(lax=true)
	public List<T> getList() {
		return list;
	}

	public void setList(ArrayList<T> list) {
		this.list = list;
	}

}
