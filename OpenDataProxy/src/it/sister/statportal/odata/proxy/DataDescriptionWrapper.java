package it.sister.statportal.odata.proxy;

import java.util.ArrayList;
import java.util.List;

import org.odata4j.core.OComplexObject;
import org.odata4j.core.OComplexObjects;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;

import com.google.gson.internal.StringMap;

/**
 * Wrapper per le entità di tipo DataDescription
 *
 */
public class DataDescriptionWrapper {
	
	/**
	 * Fornisce la definizione della struttura degli oggetti di tipo DataDescription
	 * @param namespace
	 * @return
	 */
	public static EdmComplexType.Builder getTypeDefinition(final String namespace) {
		final List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
		properties.add(EdmProperty.newBuilder("jsonSerialization").setType(EdmSimpleType.STRING));
		return EdmComplexType.newBuilder().setNamespace(namespace).setName("DataDescription").addProperties(properties);
	}
	
	/**
	 * Crea un oggetto di tipo DataDescription 
	 * @param type il tipo dell'oggetto
	 * @param jsonSerialization la serializzazione in formato json da inserire come campo
	 * @return un oggetto di tipo DataDescription 
	 */
	public static OComplexObject createInstance(EdmComplexType type, String jsonSerialization){
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		properties.add(OProperties.string("jsonSerialization", jsonSerialization));
		return OComplexObjects.create(type, properties);
	}
	
	/**
	 * Crea un oggetto di tipo DataDescription deserializzando una stringa json
	 * @param type il tipo dell'oggetto
	 * @param json una struttura che rispecchia una serializzazione json
	 * @return un oggetto di tipo DataDescription 
	 */
	public static OComplexObject createInstance(EdmComplexType type, StringMap<?> json){
		StringMap<?> d = (StringMap<?>)json.get("d");
		String jsonSerialization = (String) d.get("jsonSerialization");
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		properties.add(OProperties.string("jsonSerialization", jsonSerialization));
		return OComplexObjects.create(type, properties);
	}
}
