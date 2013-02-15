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
 * Wrapper delle entità di tipo DistinctCountRows
 *
 */
public class DistinctCountRowWrapper {
	private static String typeName = "DistinctCountRows";
	
	private static final String ID_LITERAL = "id";

	/**
	 * Fornisce il modello dell'entity set
	 * @param namespace
	 * @return
	 */
	public static EdmEntityType.Builder getEntityModel(final String namespace) {
		final List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
		properties.add(EdmProperty.newBuilder(ID_LITERAL).setType(EdmSimpleType.INT32));
		properties.add(EdmProperty.newBuilder("label").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("count").setType(EdmSimpleType.INT32));
		return EdmEntityType.newBuilder().setNamespace(namespace).setName(typeName).addKeys(ID_LITERAL).addProperties(properties);
	}
	
	/**
	 * Crea un'entità
	 * @param ees l'entity set dell'entità
	 * @param id l'id
	 * @param count il valore del campo count
	 * @param label l'etichetta associata al contatore
	 * @return un'entità di tipo DistinctCountRows
	 */
	public static OEntity createInstance(final EdmEntitySet ees, final int id, final int count, final String label){
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		final List<OLink> links = new ArrayList<OLink>();									
		properties.add(OProperties.int32(ID_LITERAL, id));
		properties.add(OProperties.string("label", label));
		properties.add(OProperties.int32("count", count));
		return OEntities.create(ees, OEntityKey.create(id), properties, links);
	}
	
	/**
	 * Crea un'entità a partire da un'altra entità
	 * @param ees l'entity set dell'entità da creare
	 * @param entity l'entità da cui estrarre i valori dei campi
	 * @return un'entità di tipo DistinctCountRows
	 */
	public static OEntity createInstance(final EdmEntitySet ees, final OEntity entity){
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		final List<OLink> links = new ArrayList<OLink>();									
		final Integer id = (Integer)entity.getProperty(ID_LITERAL).getValue();
		properties.add(OProperties.int32(ID_LITERAL, id));
		properties.add(OProperties.string("label", (String)entity.getProperty("label").getValue()));
		properties.add(OProperties.int32("count", (Integer)entity.getProperty("count").getValue()));
		return OEntities.create(ees, OEntityKey.create(id), properties, links);
	}
}
