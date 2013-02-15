package it.sister.statportal.odata.wrapper;

import it.sister.statportal.odata.domain.MdData;

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
 * Wrapper della classe MdData che rappresenta i dati del dominio. 
 * 
 */
public class MdDataWrapper {
	
	private static String typeName = "MdData";
	
	private static final String ID_LITERAL = "id";

	/**
	 * Fornisce la definizione del modello delle entità per quanto riguarda il wrapper delle tabelle MD_DATA
	 * @param namespace il namespace da applicare al modello. è uguale per tutte le entità e quindi viene passato come parametro.
	 * @return L'entity set con la struttura del wrapper MdData
	 */
	public static EdmEntityType.Builder getEntityModel(final String namespace) {
		final List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
		properties.add(EdmProperty.newBuilder(ID_LITERAL).setType(EdmSimpleType.INT32));
		properties.add(EdmProperty.newBuilder("name").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("description").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("numRows").setType(EdmSimpleType.INT32));
		properties.add(EdmProperty.newBuilder("idLuDataType").setType(EdmSimpleType.INT32));
		properties.add(EdmProperty.newBuilder("lastUpdate").setType(EdmSimpleType.DATETIME));		
		return EdmEntityType.newBuilder().setNamespace(namespace).setName(typeName).addKeys(ID_LITERAL).addProperties(properties);
	}
	
	/**
	 * Fornisce la definizione del modello delle entità per quanto riguarda il wrapper delle tabelle MD_DATA
	 * @param namespace il namespace da applicare al modello. è uguale per tutte le entità e quindi viene passato come parametro.
	 * @return L'entity set con la struttura del wrapper MdData
	 */
	public static EdmEntityType.Builder getEntityModelForProxy(final String namespace) {
		final List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
		properties.add(EdmProperty.newBuilder(ID_LITERAL).setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("name").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("description").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("numRows").setType(EdmSimpleType.INT32));
		properties.add(EdmProperty.newBuilder("idLuDataType").setType(EdmSimpleType.INT32));
		properties.add(EdmProperty.newBuilder("lastUpdate").setType(EdmSimpleType.DATETIME));		
		return EdmEntityType.newBuilder().setNamespace(namespace).setName(typeName).addKeys(ID_LITERAL).addProperties(properties);
	}
	
	/**
	 * Crea un'entità di tipo MdData
	 * @param ees l'entity set
	 * @param mdData il dato
	 * @param proxyName il nome dell'istanza a cui appartiene il dato
	 * @return un'entità di tipo MdData
	 */
	public static OEntity createInstance(final EdmEntitySet ees, final MdData mdData, final String proxyName){
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		final List<OLink> links = new ArrayList<OLink>();							
		String uid =  mdData.getUid();
		if(uid.indexOf("@") == -1){
			uid += ("@"+proxyName);
		}
		properties.add(OProperties.string(ID_LITERAL, uid));
		properties.add(OProperties.int32("idLuDataType", mdData.getIdLuDataType()));
		links.add(OLinks.relatedEntities("dataRows", "DataRows", "DataRows"));
		links.add(OLinks.relatedEntities("dataColumns", "DataColumns", "DataColumns"));
		links.add(OLinks.relatedEntities("rawDataRows", "RawDataRows", "RawDataRows"));
		links.add(OLinks.relatedEntities("distinctCountRows", "DistinctCountRows", "DistinctCountRows"));
		links.add(OLinks.relatedEntities("dataDimensions", "DataDimensions", "DataDimensions("+uid+"@"+proxyName+")"));
		links.add(OLinks.relatedEntities("dataMeasures", "DataMeasures", "DataMeasures("+uid+"@"+proxyName+")"));
		return OEntities.create(ees, OEntityKey.create(uid), properties, links);
	}
	
	/**
	 * Crea un'entità di tipo MdData
	 * @param ees l'entity set
	 * @param entity l'entità da cui prendere i valori
	 * @return un'entità di tipo MdData
	 */
	public static OEntity createInstance(final EdmEntitySet ees, final OEntity entity){
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		final List<OLink> links = new ArrayList<OLink>();							
		Date date = null;
		final LocalDateTime localDateTime = (LocalDateTime) entity.getProperty("lastUpdate").getValue();
		if(localDateTime != null){
			date = localDateTime.toDateTime().toDate();
		}
		final Integer id = (Integer)entity.getProperty(ID_LITERAL).getValue();
		properties.add(OProperties.int32(ID_LITERAL, id));
		properties.add(OProperties.string("name", (String)entity.getProperty("name").getValue()));
		properties.add(OProperties.string("description", (String)entity.getProperty("description").getValue()));
		properties.add(OProperties.int32("numRows", (Integer)entity.getProperty("numRows").getValue()));
		properties.add(OProperties.int32("idLuDataType", (Integer)entity.getProperty("idLuDataType").getValue()));
		properties.add(OProperties.datetime("lastUpdate", date));
		links.add(OLinks.relatedEntities("dataRows", "DataRows", "DataRows"));
		links.add(OLinks.relatedEntities("dataColumns", "DataColumns", "DataColumns"));
		links.add(OLinks.relatedEntities("rawDataRows", "RawDataRows", "RawDataRows"));
		links.add(OLinks.relatedEntities("distinctCountRows", "DistinctCountRows", "DistinctCountRows"));
		links.add(OLinks.relatedEntities("dataDimensions", "DataDimensions", "DataDimensions("+id+")"));
		links.add(OLinks.relatedEntities("dataMeasures", "DataMeasures", "DataMeasures("+id+")"));
		return OEntities.create(ees, OEntityKey.create(id), properties, links);
	}
}
