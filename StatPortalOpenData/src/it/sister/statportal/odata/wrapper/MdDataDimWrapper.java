package it.sister.statportal.odata.wrapper;

import it.sister.statportal.odata.domain.MdDataDim;

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
 * Wrapper per le entità di tipo MdDataDim
 *
 */
public class MdDataDimWrapper{

	private static String typeName = "MdDataDim";
	
	private static final String ID_LITERAL = "id";
	
	/**
	 * Fornisce la definizione del modello delle entità per quanto riguarda il wrapper delle tabelle MD_DATA_DIM
	 * @param namespace il namespace da applicare al modello. è uguale per tutte le entità e quindi viene passato come parametro.
	 * @return L'entity set con la struttura del wrapper MdDataDim
	 */
	public static EdmEntityType.Builder getEntityModel(final String namespace) {
		final List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
		properties.add(EdmProperty.newBuilder(ID_LITERAL).setType(EdmSimpleType.INT32));
		properties.add(EdmProperty.newBuilder("alias").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("description").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("dimcodeField").setType(EdmSimpleType.STRING));		
		properties.add(EdmProperty.newBuilder("cardinality").setType(EdmSimpleType.INT32));
		properties.add(EdmProperty.newBuilder("dimType").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("differentDistinctCount").setType(EdmSimpleType.INT32));
		return EdmEntityType.newBuilder().setNamespace(namespace).setName(typeName).addKeys(ID_LITERAL).addProperties(properties);
	}
	
	/**
	 * Fornisce la definizione del modello delle entità per quanto riguarda il wrapper delle tabelle MD_DATA_DIM
	 * @param namespace il namespace da applicare al modello. è uguale per tutte le entità e quindi viene passato come parametro.
	 * @return L'entity set con la struttura del wrapper MdDataDim
	 */
	public static EdmEntityType.Builder getEntityModelForProxy(final String namespace) {
		final List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
		properties.add(EdmProperty.newBuilder(ID_LITERAL).setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("alias").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("description").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("dimcodeField").setType(EdmSimpleType.STRING));		
		properties.add(EdmProperty.newBuilder("cardinality").setType(EdmSimpleType.INT32));
		properties.add(EdmProperty.newBuilder("dimType").setType(EdmSimpleType.STRING));
		properties.add(EdmProperty.newBuilder("differentDistinctCount").setType(EdmSimpleType.INT32));
		return EdmEntityType.newBuilder().setNamespace(namespace).setName(typeName).addKeys(ID_LITERAL).addProperties(properties);
	}
	
	/**
	 * Crea un'entità a partire da una dimensione di un dato
	 * @param ees l'entity set dell'entità da creare
	 * @param mdDataDim una dimensione di un dato
	 * @return un'entità di tipo MdDataDim
	 */
	public static OEntity createInstance(final EdmEntitySet ees, final MdDataDim mdDataDim){
		int cardinality = 0;
		String type = "unknown";
		if(mdDataDim != null){
			cardinality = mdDataDim.getCardinality();
			type = mdDataDim.getDimType();
		}
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
        final List<OLink> links = new ArrayList<OLink>();		
        final Integer id = mdDataDim.getId();
        properties.add(OProperties.int32(ID_LITERAL, id));
        properties.add(OProperties.string("alias", mdDataDim.getAlias()));
        properties.add(OProperties.string("description", mdDataDim.getDescription()));
        properties.add(OProperties.string("dimcodeField", mdDataDim.getDimcodeField()));
		properties.add(OProperties.int32("cardinality", cardinality));
		properties.add(OProperties.string("dimType", type));
		properties.add(OProperties.int32("differentDistinctCount", mdDataDim.getDifferentDistinctCount()));
		links.add(OLinks.relatedEntity("dimensionData", "IdData", "MdData("+mdDataDim.getIdData().getId()+")"));
		links.add(OLinks.relatedEntity("dataNode", "IdHierNode", "MdHierNode("+mdDataDim.getIdHierNode().getId()+")"));
		return OEntities.create(ees, OEntityKey.create(id), properties, links);
	}
	
	/**
	 * Crea un'entità di tipo MdDataDim
	 * @param ees l'entity set 
	 * @param mdDataDim una dimensione di un dato
	 * @param cardinality la cardinalità 
	 * @param type il tipo della dimensione
	 * @param proxyName il nome del servizio a cui appartiene
	 * @return
	 */
	public static OEntity createInstance(final EdmEntitySet ees, final MdDataDim mdDataDim, int cardinality, String type, String proxyName){
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
        final List<OLink> links = new ArrayList<OLink>();		
        String uid =  mdDataDim.getUid();
		boolean external = false;
        if(uid.indexOf("@") == -1){
			uid += ("@"+proxyName);			
		}else{
			external = true;
		}
        properties.add(OProperties.string(ID_LITERAL, uid));
        properties.add(OProperties.string("alias", mdDataDim.getAlias()));
		properties.add(OProperties.int32("cardinality", cardinality));
		properties.add(OProperties.string("dimType", type));
		properties.add(OProperties.int32("differentDistinctCount", mdDataDim.getDifferentDistinctCount()));
		if(!external){
			links.add(OLinks.relatedEntity("dimensionData", "IdData", "MdData("+mdDataDim.getIdData().getUid()+"@"+proxyName+")"));
		}
		return OEntities.create(ees, OEntityKey.create(uid), properties, links);
	}
	
	/**
	 * Crea un'entità di tipo MdData
	 * @param ees l'entity set dell'entità da creare
	 * @param entity l'entità da cui estrarre i campi
	 * @param baseProducer il producer che ha generato l'entità passata come parametro
	 * @return un'entità di tipo MdData
	 */
	public static OEntity createInstance(final EdmEntitySet ees, final OEntity entity, final ODataProducer baseProducer) {
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
        final List<OLink> links = new ArrayList<OLink>();		
        final Integer id = (Integer)entity.getProperty(ID_LITERAL).getValue();
        final OEntityKey key = OEntityKey.create(id);
        properties.add(OProperties.int32(ID_LITERAL, (Integer)entity.getProperty(ID_LITERAL).getValue()));
        properties.add(OProperties.string("alias", (String)entity.getProperty("alias").getValue()));
		properties.add(OProperties.string("description", (String)entity.getProperty("description").getValue()));
        properties.add(OProperties.string("dimcodeField", (String)entity.getProperty("dimcodeField").getValue()));
		MdDataDim mdDataDim = MdDataDim.findMdDataDim(id);
		int cardinality = 0;
		String type = "unknown";
		if(mdDataDim != null){
			cardinality = mdDataDim.getCardinality();
			type = mdDataDim.getDimType();
		}
		properties.add(OProperties.int32("cardinality", cardinality));
		properties.add(OProperties.string("dimType", type));
		properties.add(OProperties.int32("differentDistinctCount", (Integer)entity.getProperty("differentDistinctCount").getValue()));
		final EntityResponse idDataResponse = ((EntityResponse)baseProducer.getNavProperty(typeName, key, "idData", null));
		links.add(OLinks.relatedEntity("dimensionData", "IdData", "MdData("+(Integer)idDataResponse.getEntity().getEntityKey().asSingleValue()+")"));
		final EntityResponse idHierResponse = ((EntityResponse)baseProducer.getNavProperty(typeName, key, "idHierNode", null));
		links.add(OLinks.relatedEntity("dataNode", "IdHierNode", "MdHierNode("+(Integer)idHierResponse.getEntity().getProperty(ID_LITERAL).getValue()+")"));
		return OEntities.create(ees, OEntityKey.create(id), properties, links);
	}
}
