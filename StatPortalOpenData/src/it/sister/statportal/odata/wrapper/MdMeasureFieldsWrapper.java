package it.sister.statportal.odata.wrapper;

import it.sister.statportal.odata.domain.MdMeasureFields;

import java.util.ArrayList;
import java.util.List;

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
 * Wrapper per le entità di tipo MdMeasureFields
 *
 */
public class MdMeasureFieldsWrapper {
	
	private static String typeName = "MdMeasureFields";
	
	private static final String ID_LITERAL = "id";
     
     /**
 	 * Fornisce la definizione del modello delle entità per quanto riguarda il wrapper delle tabelle MD_MEASURE_FIELDS
 	 * @param namespace il namespace da applicare al modello. è uguale per tutte le entità e quindi viene passato come parametro.
 	 * @return L'entity set con la struttura del wrapper MdMeasureFields
 	 */
 	public static EdmEntityType.Builder getEntityModel(final String namespace) {
 		final List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
 		properties.add(EdmProperty.newBuilder(ID_LITERAL).setType(EdmSimpleType.INT32));
 		properties.add(EdmProperty.newBuilder("alias").setType(EdmSimpleType.STRING));
 		properties.add(EdmProperty.newBuilder("description").setType(EdmSimpleType.STRING));
 		properties.add(EdmProperty.newBuilder("measureField").setType(EdmSimpleType.STRING));
 		properties.add(EdmProperty.newBuilder("measureUnits").setType(EdmSimpleType.STRING));
 		properties.add(EdmProperty.newBuilder("decimalPlaces").setType(EdmSimpleType.INT16));
 		properties.add(EdmProperty.newBuilder("pos").setType(EdmSimpleType.INT32));

 		return EdmEntityType.newBuilder().setNamespace(namespace).setName(typeName).addKeys(ID_LITERAL).addProperties(properties);
 	}
 	 
 	/**
 	 * Fornisce la definizione del modello delle entità per quanto riguarda il wrapper delle tabelle MD_MEASURE_FIELDS
 	 * @param namespace il namespace da applicare al modello. è uguale per tutte le entità e quindi viene passato come parametro.
 	 * @return L'entity set con la struttura del wrapper MdMeasureFields
 	 */
 	public static EdmEntityType.Builder getEntityModelForProxy(final String namespace) {
 		final List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
 		properties.add(EdmProperty.newBuilder(ID_LITERAL).setType(EdmSimpleType.STRING));
 		properties.add(EdmProperty.newBuilder("alias").setType(EdmSimpleType.STRING));
 		properties.add(EdmProperty.newBuilder("description").setType(EdmSimpleType.STRING));
 		properties.add(EdmProperty.newBuilder("measureField").setType(EdmSimpleType.STRING));
 		properties.add(EdmProperty.newBuilder("measureUnits").setType(EdmSimpleType.STRING));
 		properties.add(EdmProperty.newBuilder("decimalPlaces").setType(EdmSimpleType.INT16));
 		properties.add(EdmProperty.newBuilder("pos").setType(EdmSimpleType.INT32));

 		return EdmEntityType.newBuilder().setNamespace(namespace).setName(typeName).addKeys(ID_LITERAL).addProperties(properties);
 	}
 	
 	/**
 	 * Crea un'entità di tipo MdMeasureFields
 	 * @param ees l'entity set
 	 * @param mdMeasureFields una misura di un dato
 	 * @param proxyName il nome del proprietario del dato
 	 * @return un'entità di tipo MdMeasureFields
 	 */
 	public static OEntity createInstance(final EdmEntitySet ees, final MdMeasureFields mdMeasureFields, String proxyName){
 		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
 		final List<OLink> links = new ArrayList<OLink>();	
 		String uid =  mdMeasureFields.getUid();
		boolean external = false;
 		if(uid.indexOf("@") == -1){
			uid += ("@"+proxyName);
		}else{
			external = true;
		}
 		final OEntityKey key = OEntityKey.create(uid);
		properties.add(OProperties.string(ID_LITERAL, uid));
		properties.add(OProperties.string("alias", mdMeasureFields.getAlias()));
		properties.add(OProperties.int16("decimalPlaces", mdMeasureFields.getDecimalPlaces()));
		if(!external){
			links.add(OLinks.relatedEntity("measureData", "IdData", "MdData("+mdMeasureFields.getIdData().getUid()+"@"+proxyName+")"));		
		}
		return OEntities.create(ees, key, properties, links);
 	}
 	
 	/**
 	 * Crea un'entità di tipo MdMeasureFields
 	 * @param ees l'entity set
 	 * @param mdMeasureFields una misura di un dato
 	 * @return un'entità di tipo MdMeasureFields
 	 */
 	public static OEntity createInstance(final EdmEntitySet ees, final MdMeasureFields mdMeasureFields){
 		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
 		final List<OLink> links = new ArrayList<OLink>();	
 		final Integer id = mdMeasureFields.getId();
 		final OEntityKey key = OEntityKey.create(id);
		properties.add(OProperties.int32(ID_LITERAL, id));
		properties.add(OProperties.string("alias", mdMeasureFields.getAlias()));
		properties.add(OProperties.string("description", mdMeasureFields.getDescription()));
		properties.add(OProperties.string("measureField", mdMeasureFields.getMeasureField()));
		properties.add(OProperties.string("measureUnits", mdMeasureFields.getMeasureUnits()));
		properties.add(OProperties.int16("decimalPlaces", mdMeasureFields.getDecimalPlaces()));
		properties.add(OProperties.int32("pos", mdMeasureFields.getPos()));
		links.add(OLinks.relatedEntity("measureData", "IdData", "MdData("+mdMeasureFields.getIdData().getId()+")"));		
		return OEntities.create(ees, key, properties, links);
 	}
 	
 	/**
 	 * Crea un'entità di MdMeasureFields
 	 * @param ees l'entity set
 	 * @param entity un'entità da cui estrarre le informazioni
 	 * @param baseProducer il producer che ha creato l'entità passata come parametro
 	 * @return un'entità di tipo MdMeasureFields
 	 */
 	public static OEntity createInstance(final EdmEntitySet ees, final OEntity entity, final ODataProducer baseProducer){
 		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
 		final List<OLink> links = new ArrayList<OLink>();	
 		final Integer id = (Integer)entity.getProperty(ID_LITERAL).getValue();
 		final OEntityKey key = OEntityKey.create(id);
		properties.add(OProperties.int32(ID_LITERAL, id));
		properties.add(OProperties.string("alias", (String)entity.getProperty("alias").getValue()));
		properties.add(OProperties.string("description", (String)entity.getProperty("description").getValue()));
		properties.add(OProperties.string("measureField", (String)entity.getProperty("measureField").getValue()));
		properties.add(OProperties.string("measureUnits", (String)entity.getProperty("measureUnits").getValue()));
		properties.add(OProperties.int16("decimalPlaces", (Short)entity.getProperty("decimalPlaces").getValue()));
		properties.add(OProperties.int32("pos", (Integer)entity.getProperty("pos").getValue()));
		final EntityResponse idDataResponse = ((EntityResponse)baseProducer.getNavProperty(typeName, key, "idData", null));
		links.add(OLinks.relatedEntity("measureData", "IdData", "MdData("+(Integer)idDataResponse.getEntity().getProperty(ID_LITERAL).getValue()+")"));		
		return OEntities.create(ees, key, properties, links);
	}
}
