package it.sister.statportal.odata.translator;

import it.sister.statportal.odata.Entities;
import it.sister.statportal.odata.wrapper.DbTableWrapper;
import it.sister.statportal.odata.wrapper.MdDataDimWrapper;
import it.sister.statportal.odata.wrapper.MdDataWrapper;
import it.sister.statportal.odata.wrapper.MdHierNodeWrapper;
import it.sister.statportal.odata.wrapper.MdHierarchyWrapper;
import it.sister.statportal.odata.wrapper.MdLuHierTypeWrapper;
import it.sister.statportal.odata.wrapper.MdMeasureFieldsWrapper;

import org.odata4j.core.OEntity;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.producer.ODataProducer;

public class Translator {

	/**
	 * Mappa un'entità ottenuta dal producer base in un'entità di un altro producer
	 * @param ees l'entity set richiesto
	 * @param entity l'entità ottenuta dal producer base
	 * @param baseProducer il producer sottostante
	 * @return l'entità corrispondente del producer che ha chiamato il metodo
	 */
	public static OEntity translate(final EdmEntitySet ees, final OEntity entity, final ODataProducer baseProducer){
		final Entities entityType = Entities.parse(ees.getName());
		OEntity toReturn;
		switch(entityType){
			case MD_DATA:
				toReturn = MdDataWrapper.createInstance(ees, entity);
				break;
			case MD_DATA_DIM:
				toReturn = MdDataDimWrapper.createInstance(ees, entity, baseProducer);
				break;
			case MD_HIERARCHY:
				toReturn =  MdHierarchyWrapper.createInstance(ees, entity, baseProducer);
				break;
			case MD_MEASURE_FIELDS:
				toReturn = MdMeasureFieldsWrapper.createInstance(ees, entity, baseProducer);
				break;
			case MD_LU_HIER_TYPE:
				toReturn =  MdLuHierTypeWrapper.createInstance(ees, entity);
				break;
			case MD_HIER_NODE:
				toReturn = MdHierNodeWrapper.createInstance(ees, entity, baseProducer);
				break;
			case DB_TABLE:
				toReturn = DbTableWrapper.createInstance(ees, entity);
				break;
			default:
				throw new IllegalArgumentException();		
		}
		return toReturn;		
	} 
	
	/**
	 * Traduce il nome di una navigationProperty nel nome interno
	 * @param entitySetName l'entity set a cui appartiene la navigation property
	 * @param navProperty la navigation property
	 * @return il nome interno per la proprietà
	 */
	public static String translateNavProperty(final String entitySetName, final String navProperty){
		String toReturn;
		if(entitySetName.compareTo("MdData") == 0){
			if(navProperty.compareTo("DataDimensions") == 0){
				toReturn = "mdDataDims";
			} else if(navProperty.compareTo("DataMeasures") == 0){
				toReturn = "mdMeasureFieldss";
			} else{
				throw new IllegalArgumentException();
			}
		} else if(entitySetName.compareTo("MdHierNode")== 0){
			if(navProperty.compareTo("NodeData") == 0){
				toReturn = "mdDataDims";
			} else if(navProperty.compareTo("RelationsAsParent") == 0){
				toReturn = "mdRelHierNodes";
			} else if(navProperty.compareTo("RelationsAsChild") == 0){
				toReturn = "mdRelHierNodes1";
			} else{
				throw new IllegalArgumentException();
			}
		} else if(entitySetName.compareTo("MdHierarchy") == 0 && navProperty.compareTo("NodesByHierarchy") == 0){
			toReturn = "mdHierNodes";
		} else if(entitySetName.compareTo("MdLuHierType") == 0 && navProperty.compareTo("HierarchiesByType") == 0){
			toReturn = "mdHierarchys";
		} else{
			throw new IllegalArgumentException();
		}
		return toReturn;
	}
	
	/**
	 * Fornisce l'entity set corrispondente ad una navigation property di un entity set
	 * @param sourceName il nome dell'entity set sorgente
	 * @param navProperty la navigation property dell'entity set
	 * @return l'entity set destinazione
	 */
	public static String targetEntitySet(final String sourceName, final String navProperty){
		String toReturn;
		if(sourceName.compareTo("MdData") == 0){
			if(navProperty.compareTo("DataDimensions") == 0){
				toReturn = "MdDataDim";
			} else if(navProperty.compareTo("DataMeasures") == 0){
				toReturn = "MdMeasureFields";
			} else {
				throw new IllegalArgumentException();
			}
		} else if(sourceName.compareTo("MdHierNode")== 0){
			if(navProperty.compareTo("NodeData") == 0){
				toReturn = "MdDataDim";
			} else if(navProperty.compareTo("RelationsAsParent") == 0){
				toReturn = "MdRelHierNode";
			} else if(navProperty.compareTo("RelationsAsChild") == 0){
				toReturn = "MdRelHierNode";
			} else{
				throw new IllegalArgumentException();
			}
		} else if(sourceName.compareTo("MdHierarchy") == 0 && navProperty.compareTo("NodesByHierarchy") == 0){
			toReturn = "MdHierNode";
		} else if(sourceName.compareTo("MdLuHierType") == 0 && navProperty.compareTo("HierarchiesByType") == 0){
			toReturn = "MdHierarchy";
		} else {
			throw new IllegalArgumentException();
		}
		return toReturn;
	}
}
