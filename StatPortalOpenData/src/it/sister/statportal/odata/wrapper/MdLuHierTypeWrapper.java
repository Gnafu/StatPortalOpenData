package it.sister.statportal.odata.wrapper;

//import it.sister.statportal.odata.domain.MdLuHierType;

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

/**
 * Wrapper per le entità di tipo MdLuHierType
 *
 */
public class MdLuHierTypeWrapper {
	
	private static String typeName = "MdLuHierType";
    
    /**
	 * Fornisce la definizione del modello delle entità per quanto riguarda il wrapper delle tabelle MD_LU_HIER_TYPE
	 * @param namespace il namespace da applicare al modello. è uguale per tutte le entità e quindi viene passato come parametro.
	 * @return L'entity set con la struttura del wrapper MdLuHierType
	 */
	public static EdmEntityType.Builder getEntityModel(final String namespace) {
		final List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
		properties.add(EdmProperty.newBuilder("id").setType(EdmSimpleType.INT32));
		properties.add(EdmProperty.newBuilder("name").setType(EdmSimpleType.STRING));
		

		return EdmEntityType.newBuilder().setNamespace(namespace).setName(typeName).addKeys("id").addProperties(properties);
	}
	
	/**
	 * Crea un'entità a partire da un'altra entità
	 * @param ees l'entity set dell'entità da creare
	 * @param entity l'entità da cui estrarre le informazioni
	 * @return un'entità di tipo MdLuHierType
	 */
	public static OEntity createInstance(final EdmEntitySet ees, final OEntity entity){
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		final List<OLink> links = new ArrayList<OLink>();		
		final Integer identifier = (Integer)entity.getProperty("id").getValue();
		properties.add(OProperties.int32("id", identifier));
		properties.add(OProperties.string("name", (String)entity.getProperty("name").getValue()));
		links.add(OLinks.relatedEntities("hierarchiesByType", "HierarchiesByType", "HierarchiesByType("+identifier+")"));
		return OEntities.create(ees, OEntityKey.create(identifier), properties, links);
	}
}
