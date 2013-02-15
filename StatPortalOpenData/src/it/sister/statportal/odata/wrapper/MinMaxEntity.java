package it.sister.statportal.odata.wrapper;

import java.util.ArrayList;
import java.util.List;

import org.odata4j.core.OEntities;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OLink;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;

/**
 * Wrapper per le entità di tipo MinMax
 *
 */
public class MinMaxEntity {
	
	/**
	 * Fornisce il modello per le entità di tipo MinMax
	 * @param namespace
	 * @return
	 */
	public static EdmEntityType.Builder getEntityModel(final String namespace) {
		final List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
		properties.add(EdmProperty.newBuilder("Min").setType(EdmSimpleType.DOUBLE));
		properties.add(EdmProperty.newBuilder("Max").setType(EdmSimpleType.DOUBLE));
		return EdmEntityType.newBuilder().setNamespace(namespace).setName("MinMax").addKeys("Min").addProperties(properties);
	}
	
	/**
	 * Crea un'entità di tipo MinMax
	 * @param minMaxEntitySet l'entity set
	 * @param min il valore del minimo
	 * @param max il valore del massimo
	 * @return un'entità di tipo MinMax
	 */
	public static OEntity createMinMaxEntity(final EdmEntitySet minMaxEntitySet, final double min, final double max){
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		final List<OLink> links = new ArrayList<OLink>();
		properties.add(OProperties.double_("Min", min));
		properties.add(OProperties.double_("Max", max));
		return OEntities.create(minMaxEntitySet, OEntityKey.create(min), properties, links);
	}
}
