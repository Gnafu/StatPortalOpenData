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
 * Wrapper per le entità di tipo Total
 *
 */
public class TotalEntity {
	
	/**
	 * Fornisce il modello per le entità di tipo Total
	 * @param namespace
	 * @return
	 */
	public static EdmEntityType.Builder getEntityModel(final String namespace) {
		final List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
		properties.add(EdmProperty.newBuilder("Tot").setType(EdmSimpleType.INT32));
		return EdmEntityType.newBuilder().setNamespace(namespace).setName("Total").addKeys("Tot").addProperties(properties);
	}
	
	/**
	 * Crea un'entità di tipo Total
	 * @param totalEntitySet l'entity set
	 * @param total il valore del totale
	 * @return un'entità di tipo Total
	 */
	public static OEntity createTotalEntity(final EdmEntitySet totalEntitySet, final int total){
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		final List<OLink> links = new ArrayList<OLink>();
		properties.add(OProperties.int32("Tot", total));
		return OEntities.create(totalEntitySet, OEntityKey.create(total), properties, links);
	}
}
