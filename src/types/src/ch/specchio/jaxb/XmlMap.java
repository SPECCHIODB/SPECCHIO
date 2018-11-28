package ch.specchio.jaxb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * JAXB-friendly implementation of a map. Some versions of Jersey are not able
 * to work with hash tables, so we need to provide our own implementation.
 */
@XmlRootElement(name="map")
public class XmlMap<K, V> implements Map<K, V> {
	
	/** entries */
	@XmlElement(name="entries") private XmlMapEntry<K, V> entries[];
	
	
	/**
	 * Default constructor.
	 */
	@SuppressWarnings("unchecked")
	public XmlMap() {
		
		entries = new XmlMapEntry[0];
		
	}
	
	
	/**
	 * Constructor.
	 */
	@SuppressWarnings("unchecked")
	public XmlMap(Map<K, V> mapIn) {
		
		entries = new XmlMapEntry[mapIn.size()];
		int i = 0;
		for (Map.Entry<K, V> entry : mapIn.entrySet()) {
			entries[i] = new XmlMapEntry<K, V>(entry);
			i++;
		}
		
	}
	
	
	/**
	 * Clear the map. Not supported.
	 * 
	 * @throws UnsupportedOperationException	always
	 */
	public void clear() {
		
		throw new UnsupportedOperationException("XmlMap is a read-only map.");
		
	}
	
	
	/**
	 * Check for the existence of a key in the map.
	 * 
	 * @param key
	 * 
	 * @return true if the value is in the map, false otherwise
	 */
	public boolean containsKey(Object key) {
		
		for (XmlMapEntry<K, V> entry : entries) {
			if (entry.getKey() != null && entry.getKey().equals(key)) {
				return true;
			}
		}
		
		return false;
		
	}
	
	
	/**
	 * Check for the existence of a value in the map.
	 * 
	 * @param value	the value
	 * 
	 * @return true if the value is in the map, false otherwise
	 */
	public boolean containsValue(Object value) {
		
		for (XmlMapEntry<K, V> entry : entries) {
			if (entry.getValue() != null && entry.getValue().equals(value)) {
				return true;
			}
		}
		
		return false;
		
	}
	
	
	/**
	 * Get a set containing all of the entries in the map.
	 * 
	 * @return a new set containing all of the entries in this map
	 */
	public Set<Map.Entry<K, V>> entrySet() {
		
		Set<Map.Entry<K, V>> entrySet = new HashSet<Map.Entry<K, V>>();
		for (XmlMapEntry<K, V> entry : entries) {
			entrySet.add(entry);
		}
		
		return entrySet;
		
	}
	
	
	/**
	 * Test whether or not the map is empty.
	 * 
	 * @return true if there are no elements in the map, false otherwise
	 */
	public boolean isEmpty() {
		
		return entries.length == 0;
		
	}
	
	
	/**
	 * Get a value corresponding to a key.
	 * 
	 * @param key	the key
	 * 
	 * @return the value corresponding to the given key, or null if the key does not exist
	 */
	public V get(Object key) {
		
		for (XmlMapEntry<K, V> entry : entries) {
			if (entry.getKey() != null && entry.getKey().equals(key)) {
				return entry.getValue();
			}
		}
		
		return null;
		
	}
	
	
	/**
	 * Get a set containing all of the keys in the map.
	 * 
	 * @return a new set containing all of the keys in the map
	 */
	public Set<K> keySet() {
		
		Set<K> keySet = new HashSet<K>();
		for (XmlMapEntry<K, V> entry : entries ) {
			keySet.add(entry.getKey());
		}
		
		return keySet;
		
	}
	
	
	/**
	 * Add a key-value pair to the map. Not supported.
	 * 
	 * @param key	the key
	 * @param value	the value
	 * 
	 * @return the value
	 * 
	 * @throws UnsupportedOperationException	always
	 */
	public V put(K key, V value) {
		
		throw new UnsupportedOperationException("XmlMap is a read-only map.");
		
	}
	
	
	/**
	 * Copy the elements of another map into this map. Not supported.
	 * 
	 * @param map	the map
	 * 
	 * @throws UnsupportedOperationException	always
	 */
	public void putAll(Map<? extends K, ? extends V> map) {
		
		throw new UnsupportedOperationException("XmlMap is a read-only map.");
		
	}
	
	
	/**
	 * Remove an entry from the map. Not supported.
	 * 
	 * @param key  the key of the entry to be removed
	 * 
	 * @return the value corresponding to the key
	 * 
	 * @throws UnsupportedOperationException	always
	 */
	public V remove(Object key) {
		
		throw new UnsupportedOperationException("XmlMap is a read-only map.");
		
	}
	
	
	/**
	 * Get the number of elements in the map.
	 * 
	 * @return the number of elements in the map.
	 */
	public int size() {
		
		return entries.length;
		
	}
	
	
	/**
	 * Get a collection containing the values in the map.
	 * 
	 * @return a new Collection containing all of the values in the map
	 */
	public Collection<V> values() {
		
		Collection<V> values = new ArrayList<V>(entries.length);
		for (XmlMapEntry<K, V> entry : entries) {
			values.add(entry.getValue());
		}
		
		return values;
		
	}

}
