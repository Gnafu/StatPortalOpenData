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
 * Wrapper per le entità di tipo GenericColumnAggregationRows
 *
 */
public class GenericColumnAggregationRowWrapper {
	private static String typeName = "GenericColumnAggregationRows";
	
	private static final String ID_LITERAL = "id";

	/**
	 * Fornisce il modello dell'entity set
	 * @param namespace
	 * @return
	 */
	public static EdmEntityType.Builder getEntityModel(final String namespace) {
		final List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
		properties.add(EdmProperty.newBuilder(ID_LITERAL).setType(EdmSimpleType.INT32));
		properties.add(EdmProperty.newBuilder("variable").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("column").setType(EdmSimpleType.STRING));
		return EdmEntityType.newBuilder().setNamespace(namespace).setName(typeName).addKeys(ID_LITERAL).addProperties(properties);
	}
	
	/**
	 * Crea un'istanza dell'entità
	 * @param ees l'entity set dell'entità da creare
	 * @param id l'id
	 * @param variable la variabile associata ad una colonna della riga
	 * @param column la colonna
	 * @return un'entità di tipo GenericColumnAggregationRows
	 */
	public static OEntity createInstance(final EdmEntitySet ees, final int id, final String variable, final String column){
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		final List<OLink> links = new ArrayList<OLink>();									
		properties.add(OProperties.int32(ID_LITERAL, id));
		properties.add(OProperties.string("variable", variable));
		properties.add(OProperties.string("column", column));
		return OEntities.create(ees, OEntityKey.create(id), properties, links);
	}
	
	/**
	 * Crea un'istanza dell'entità a partire da un'altra entità
	 * @param ees l'entity set dell'entità da creare
	 * @param entity l'entità da cui estrarre le informazioni
	 * @return un'entitù di tipo GenericColumnAggregationRows
	 */
	public static OEntity createInstance(final EdmEntitySet ees, final OEntity entity){
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		final List<OLink> links = new ArrayList<OLink>();									
		final Integer id = (Integer)entity.getProperty(ID_LITERAL).getValue();
		properties.add(OProperties.int32(ID_LITERAL, id));
		properties.add(OProperties.string("variable", (String)entity.getProperty("variable").getValue()));
		properties.add(OProperties.string("column", (String)entity.getProperty("column").getValue()));
		return OEntities.create(ees, OEntityKey.create(id), properties, links);
	}
}
