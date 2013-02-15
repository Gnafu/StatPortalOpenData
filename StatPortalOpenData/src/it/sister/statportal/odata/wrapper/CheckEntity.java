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
 * Entità di tipo check
 *
 */
public class CheckEntity{
	
	/**
	 * Restituisce il modello dell'entità
	 * @param namespace
	 * @return
	 */
	public static EdmEntityType.Builder getEntityModel(final String namespace) {
		final List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
		properties.add(EdmProperty.newBuilder("Res").setType(EdmSimpleType.INT32));
		return EdmEntityType.newBuilder().setNamespace(namespace).setName("Check").addKeys("Res").addProperties(properties);
	}
	
	/**
	 * Crea un'entità di tipo Check
	 * @param checkEntitySet l'entity set a cui appartiene l'entità
	 * @param res un intero che rappresenta lo stato
	 * @return un'entità di tipo check
	 */
	public static OEntity createCheckEntity(final EdmEntitySet checkEntitySet, final int res){
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		final List<OLink> links = new ArrayList<OLink>();
		properties.add(OProperties.int32("Res", res));
		return OEntities.create(checkEntitySet, OEntityKey.create(res), properties, links);
	}
}
