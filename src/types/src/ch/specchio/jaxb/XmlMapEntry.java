package ch.specchio.jaxb;

import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;



/**
 * Class representing a map entry
 */
@XmlRootElement(name="entry")
public class XmlMapEntry<K, V> implements Map.Entry<K, V> {
	
	/** key */
	private K key;
	
	/** value */
	private V value;
	
	/**
	 * Default constructor.
	 */
	public XmlMapEntry() {
		
		key = null;
		value = null;
		
	}
	
	
	/**
	 * Constructor.
	 *
	 * @param keyIn	the key
	 * @param valueIn	the value
	 */
	public XmlMapEntry(K keyIn, V valueIn) {
		
		key = keyIn;
		value = valueIn;
		
	}
	
	
	/**
	 * Copy constructor.
	 * 
	 * @param entryIn	the entry to be copied
	 */
	public XmlMapEntry(Map.Entry<K, V> entryIn) {
		
		this(entryIn.getKey(), entryIn.getValue());
		
	}
	
	
	/**
	 * Get the key of the entry.
	 * 
	 * @return the key
	 */
	@XmlElement(name="key")
	public K getKey() {
		
		return key;
		
	}
	
	
	/**
	 * Set the key of the entry.
	 * 
	 * @param keyIn	the new key
	 * @return 
	 */
	public K setKey(K keyIn) {
		
		key = keyIn;
		
		return key;
		
	}
	
	
	/**
	 * Get the value of the entry.
	 * 
	 * @return the value
	 */
	@XmlElement(name="value")
	public V getValue() {
		
		return value;
		
	}
	
	
	/**
	 * Set the value of the entry.
	 * 
	 * @param valueIn	the new value
	 * @return 
	 */
	public V setValue(V valueIn) {
		
		value = valueIn;
		
		return value;
		
	}
	
}
