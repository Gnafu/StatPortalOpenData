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
 * Wrapper per gli oggetti di tipo LayerInfo
 *
 */
public class LayerInfoWrapper {

	/**
	 * Fornisce la struttura degli oggetti LayerInfo
	 * @param namespace
	 * @return
	 */
	public static EdmComplexType.Builder getTypeDefinition(final String namespace) {
		final List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
		properties.add(EdmProperty.newBuilder("geoServerUrl").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("geoServerRestUrl").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("workspaceName").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("layerName").setType(EdmSimpleType.STRING));
		return EdmComplexType.newBuilder().setNamespace(namespace).setName("LayerInfo").addProperties(properties);
	}
	
	/**
	 * Crea un'istanza di tipo LayerInfo
	 * @param type il tipo dell'oggetto
	 * @param geoServerUrl url del geoServer
	 * @param geoServerRestUrl url del geoserver con interfaccia rest
	 * @param workspaceName nome del workspace nel geoserver
	 * @param layerName nome del layer
	 * @return un'istanza di tipo LayerInfo
	 */
	public static OComplexObject createInstance(EdmComplexType type, String geoServerUrl, String geoServerRestUrl, 
										String workspaceName, String layerName){
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		properties.add(OProperties.string("geoServerUrl", geoServerUrl));
		properties.add(OProperties.string("geoServerRestUrl", geoServerRestUrl));
		properties.add(OProperties.string("workspaceName", workspaceName));
		properties.add(OProperties.string("layerName", layerName));
		return OComplexObjects.create(type, properties);
	}
	
	/**
	 * Crea un'istanza di tipo LayerInfo partendo dalla rappresentazione di una serializzazione json
	 * @param type il tipo dell'oggetto
	 * @param json la rappresentazione di una serializzazione json
	 * @return un'istanza di tipo LayerInfo
	 */
	public static OComplexObject createInstance(EdmComplexType type, StringMap<?> json){
		StringMap<?> d = (StringMap<?>)json.get("d");
		String geoServerUrl = (String) d.get("geoServerUrl");
		String geoServerRestUrl = (String) d.get("geoServerRestUrl");
		String workspaceName = (String) d.get("workspaceName");
		String layerName = (String) d.get("layerName");
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		properties.add(OProperties.string("geoServerUrl", geoServerUrl));
		properties.add(OProperties.string("geoServerRestUrl", geoServerRestUrl));
		properties.add(OProperties.string("workspaceName", workspaceName));
		properties.add(OProperties.string("layerName", layerName));
		return OComplexObjects.create(type, properties);
	}
}
