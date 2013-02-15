package it.sister.statportal.odata.wrapper;

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

/**
 * Wrapper delle entità di tipo DbTable
 *
 */
public class DbTableWrapper {

	private static String typeName = "DbTable";
    
	private static final String ID_LITERAL = "id";
	
	private static final String TABLETYPE_LITERAL = "tableType";
	    
	/**
	 * Fornisce il modello dell'entity set dbTable
	 * @param namespace
	 * @return
	 */
    public static EdmEntityType.Builder getEntityModel(final String namespace) {
		final List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
		properties.add(EdmProperty.newBuilder(ID_LITERAL).setType(EdmSimpleType.INT32));
		properties.add(EdmProperty.newBuilder(TABLETYPE_LITERAL).setType(EdmSimpleType.INT32));
		properties.add(EdmProperty.newBuilder("name").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("description").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("numRows").setType(EdmSimpleType.INT32));
		properties.add(EdmProperty.newBuilder("lastUpdate").setType(EdmSimpleType.DATETIME));
				
		return EdmEntityType.newBuilder().setNamespace(namespace).setName(typeName).addKeys(ID_LITERAL, TABLETYPE_LITERAL).addProperties(properties);
	}
    
    /**
     * Crea un'entità a partire da un'altra entità
     * @param ees l'entity set dell'entità da creare
     * @param entity l'entità da cui derivare le proprietà
     * @return un'entità di tipo DbTable
     */
    public static OEntity createInstance(final EdmEntitySet ees, final OEntity entity){
    	final String sourceName = entity.getEntitySetName();
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		final List<OLink> links = new ArrayList<OLink>();							
		Date date = null;
		final LocalDateTime localDateTime = (LocalDateTime) entity.getProperty("lastUpdate").getValue();
		if(localDateTime != null){
			date = localDateTime.toDateTime().toDate();
		}
		final Integer id = (Integer)entity.getProperty(ID_LITERAL).getValue();
		properties.add(OProperties.int32(ID_LITERAL, id));
		Integer tableType = 0;
		if(sourceName.compareTo("DbTable") == 0){
			tableType = 1;
		} else if(sourceName.compareTo("MdData") == 0){
			tableType = 2;
		} else if(sourceName.compareTo("MdHierNode") == 0){
			tableType = 3;
		}
		properties.add(OProperties.int32(TABLETYPE_LITERAL, tableType));
		properties.add(OProperties.string("name", (String)entity.getProperty("name").getValue()));
		properties.add(OProperties.string("description", (String)entity.getProperty("description").getValue()));
		String dbName = "";
		if(sourceName.compareTo("MdHierNode") != 0){
			dbName = (String)entity.getProperty("dbName").getValue();
			properties.add(OProperties.string("dbName", dbName));
		}
		final String tableName = (String)entity.getProperty("tableName").getValue();
		properties.add(OProperties.string("tableName", tableName));
		properties.add(OProperties.int32("numRows", (Integer)entity.getProperty("numRows").getValue()));
		properties.add(OProperties.datetime("lastUpdate", date));
		links.add(OLinks.relatedEntities("tableRows", "TableRows", "TableRows"));
		return OEntities.create(ees, OEntityKey.create(ID_LITERAL, id, TABLETYPE_LITERAL, tableType), properties, links);
	}
}
