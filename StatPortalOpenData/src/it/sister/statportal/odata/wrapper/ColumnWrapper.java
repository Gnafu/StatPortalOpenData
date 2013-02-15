package it.sister.statportal.odata.wrapper;

import it.sister.statportal.odata.domain.Column;

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
 * Entità che modella le colonne di un dato
 *
 */
public class ColumnWrapper {
private static String typeName = "DataColumns";
	
	private static final String ID_LITERAL = "id";

	/**
	 * Fornisce il modello dell'entità
	 * @param namespace
	 * @return
	 */
	public static EdmEntityType.Builder getEntityModel(final String namespace) {
		final List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
		properties.add(EdmProperty.newBuilder(ID_LITERAL).setType(EdmSimpleType.INT32));
		properties.add(EdmProperty.newBuilder("physicalName").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("logicalName").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("columnType").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("differentDistinctCount").setType(EdmSimpleType.INT32));
		return EdmEntityType.newBuilder().setNamespace(namespace).setName(typeName).addKeys(ID_LITERAL).addProperties(properties);
	}
	
	/**
	 * Crea un'entità a partire da una colonna e il suo id
	 * @param ees l'entity set a cui appartiene l'entità
	 * @param column la colonna
	 * @param id l'id della colonna
	 * @return un'entità di tipo DataColumn
	 */
	public static OEntity createInstance(final EdmEntitySet ees, final Column column, int id){
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		final List<OLink> links = new ArrayList<OLink>();									
		properties.add(OProperties.int32(ID_LITERAL, id));
		properties.add(OProperties.string("physicalName", column.getPhysicalName()));
		properties.add(OProperties.string("logicalName", column.getLogicalName()));
		properties.add(OProperties.string("columnType", column.getType().toString()));
		properties.add(OProperties.int32("differentDistinctCount", column.getDifferentDistinctCount()));
		return OEntities.create(ees, OEntityKey.create(id), properties, links);
	}
	
	/**
	 * Crea un'entità a partire da un'altra entità con la stessa struttura
	 * @param ees l'entity set dell'unità da creare
	 * @param entity l'entità da cui estrarre le caratteristiche
	 * @return un'entità di tipo dataColumn
	 */
	public static OEntity createInstance(final EdmEntitySet ees, final OEntity entity){
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		final List<OLink> links = new ArrayList<OLink>();									
		final Integer id = (Integer)entity.getProperty(ID_LITERAL).getValue();
		properties.add(OProperties.int32(ID_LITERAL, id));
		properties.add(OProperties.string("physicalName", (String)entity.getProperty("physicalName").getValue()));
		properties.add(OProperties.string("logicalName", (String)entity.getProperty("logicalName").getValue()));
		properties.add(OProperties.string("columnType", (String)entity.getProperty("columnType").getValue()));
		properties.add(OProperties.int32("differentDistinctCount", (Integer)entity.getProperty("differentDistinctCount").getValue()));
		return OEntities.create(ees, OEntityKey.create(id), properties, links);
	}
}
