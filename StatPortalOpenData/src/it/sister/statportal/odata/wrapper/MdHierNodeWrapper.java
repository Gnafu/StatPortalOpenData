package it.sister.statportal.odata.wrapper;

//import it.sister.statportal.odata.domain.MdHierNode;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.joda.time.LocalDateTime;
import org.odata4j.core.OEntities;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OLink;
import org.odata4j.core.OLinks;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.ODataProducer;

/**
 * Wrapper per le entità di tipo MdHierNode
 *
 */
public class MdHierNodeWrapper {
    
	private static String typeName = "MdHierNode";
	
	private static final String ID_LITERAL = "id";
    
	/**
	 * Fornisce la definizione del modello delle entità per quanto riguarda il wrapper delle tabelle MD_HIER_NODE
	 * @param namespace il namespace da applicare al modello. è uguale per tutte le entità e quindi viene passato come parametro.
	 * @return L'entity set con la struttura del wrapper MdHierNode
	 */
	public static EdmEntityType.Builder getEntityModel(final String namespace) {
		final List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
		properties.add(EdmProperty.newBuilder(ID_LITERAL).setType(EdmSimpleType.INT32));
		properties.add(EdmProperty.newBuilder("name").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("description").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("pkField").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("descField").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("sortField").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("numRows").setType(EdmSimpleType.INT32));
		properties.add(EdmProperty.newBuilder("lastUpdate").setType(EdmSimpleType.DATETIME));
		
		return EdmEntityType.newBuilder().setNamespace(namespace).setName(typeName).addKeys(ID_LITERAL).addProperties(properties);
	}
	
	/**
	 * Crea un'entità di tipo MdHierNode
	 * @param ees l'entity set
	 * @param entity un'entità da cui estrarre le informazioni
	 * @param baseProducer il producer che ha creato l'entità
	 * @return un'entità di tipo MdHierNode
	 */
	public static OEntity createInstance(final EdmEntitySet ees, final OEntity entity, final ODataProducer baseProducer) {
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
        final List<OLink> links = new ArrayList<OLink>();			
        Date date = null;
		final LocalDateTime localDateTime = (LocalDateTime) entity.getProperty("lastUpdate").getValue();
		if(localDateTime != null){
			date = localDateTime.toDateTime().toDate();
		}
		final Integer id = (Integer)entity.getProperty(ID_LITERAL).getValue();
		final OEntityKey key = OEntityKey.create(id);
        properties.add(OProperties.int32(ID_LITERAL, id));
		properties.add(OProperties.string("name", (String)entity.getProperty("name").getValue()));
		properties.add(OProperties.string("description", (String)entity.getProperty("description").getValue()));
		properties.add(OProperties.string("pkField", (String)entity.getProperty("pkField").getValue()));
		properties.add(OProperties.string("descField", (String)entity.getProperty("descField").getValue()));
		properties.add(OProperties.string("sortField", (String)entity.getProperty("sortField").getValue()));
		properties.add(OProperties.int32("numRows", (Integer)entity.getProperty("numRows").getValue()));
		properties.add(OProperties.datetime("lastUpdate", date));
		links.add(OLinks.relatedEntities("nodeRows", "NodeRows", "NodeRows"));
		links.add(OLinks.relatedEntities("nodeData", "NodeData", "NodeData("+id+")"));
		final EntityResponse idHierResponse = ((EntityResponse)baseProducer.getNavProperty(typeName, key, "idHierarchy", null));
		links.add(OLinks.relatedEntity("nodeHierarchy", "IdHierarchy", "MdHierarchy("+(Integer)idHierResponse.getEntity().getProperty(ID_LITERAL).getValue()+")"));	
		return OEntities.create(ees, key, properties, links);
	}
}
