package it.sister.statportal.odata;

import it.sister.statportal.odata.domain.DbTable;

import it.sister.statportal.odata.domain.MdData;
import it.sister.statportal.odata.domain.MdDataDim;
import it.sister.statportal.odata.domain.MdHierNode;
import it.sister.statportal.odata.domain.MdHierarchy;
import it.sister.statportal.odata.domain.MdLuHierType;
import it.sister.statportal.odata.domain.MdMeasureFields;
import it.sister.statportal.odata.domain.OdataDomainException;
import it.sister.statportal.odata.translator.Translator;
import it.sister.statportal.odata.utility.DBUtils;
import it.sister.statportal.odata.wrapper.CheckEntity;
import it.sister.statportal.odata.wrapper.ColumnWrapper;
import it.sister.statportal.odata.wrapper.DbTableWrapper;
import it.sister.statportal.odata.wrapper.DistinctCountRowWrapper;
import it.sister.statportal.odata.wrapper.GenericColumnAggregationRowWrapper;
import it.sister.statportal.odata.wrapper.MdDataDimWrapper;
import it.sister.statportal.odata.wrapper.MdDataWrapper;
import it.sister.statportal.odata.wrapper.MdHierNodeWrapper;
import it.sister.statportal.odata.wrapper.MdHierarchyWrapper;
import it.sister.statportal.odata.wrapper.MdLuHierTypeWrapper;
import it.sister.statportal.odata.wrapper.MdMeasureFieldsWrapper;
import it.sister.statportal.odata.wrapper.TotalEntity;
//import it.sister.statportal.odata.wrapper.MdRelHierNodeWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.odata4j.core.NamedValue;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OFunctionParameter;
import org.odata4j.core.ORelatedEntityLink;
import org.odata4j.edm.EdmAssociation;
import org.odata4j.edm.EdmAssociationEnd;
import org.odata4j.edm.EdmAssociationSet;
import org.odata4j.edm.EdmAssociationSetEnd;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntityContainer;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmFunctionImport;
import org.odata4j.edm.EdmMultiplicity;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.edm.EdmSchema;
import org.odata4j.producer.BaseResponse;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.EntityIdResponse;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.ODataProducer;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.Responses;
import org.odata4j.producer.edm.MetadataProducer;
import org.odata4j.producer.exceptions.NotImplementedException;

/**
 * Implementazione del producer odata per l'esposizione dei dati di StatPortal.
 * Questo producer si basa su un producer sottostante a cui gira le richieste che arrivano dai consumer una volta tradotte.
 * Crea le risposte da girare ai consumer leggendo la risposta arrivata dal producer sottostante e mappandola nel suo dominio.
 * Per ora il livello sottostante può essere un JPAProducer.
 */
public class StatPortalProducer implements ODataProducer{

	/**
	 * producer del livello sottostante inglobato nel nostro producer
	 * generiamo le risposte da passare all'utente basandoci su quelle che arrivano dal livello sotto
	 */
	ODataProducer baseProducer;
	
	/**
	 * producer per la generazione al volo degli entitySet relativi alle tabelle dei fatti/nodi/grezze
	 */
	JITProducer jitProducer;
	
	/**	
	 * metadati
	 */
	static EdmDataServices edmDataServices = null;
	
	/**
	 * Costruisce la nostra implementazione del producer odata.
	 * @param baseProducer il producer su cui basiamo le risposte (per ora un JPAProducer ma possiamo cambiarlo in modo trasparente)
	 */
	public StatPortalProducer(ODataProducer baseProducer){
		this.baseProducer = baseProducer;
		this.jitProducer = new JITProducer();
		StatPortalProducer.buildMetadata();
	}
	
	/**
	 * Metodo statico per la generazione dei metadati (entità, proprietà, associazioni, etc.).
	 * Per ora creiamo i metadati a mano, successivamente implementeremo la lettura dei metadati da un file xml di configurazione
	 */
	private static void buildMetadata() {
		String namespace = "http://odata.statportal.sister.it";
		
		List<EdmEntitySet.Builder> entitySets = new ArrayList<EdmEntitySet.Builder>();
		List<EdmEntityType.Builder> entityTypes = new ArrayList<EdmEntityType.Builder>();
		List<EdmAssociation.Builder> associations = new ArrayList<EdmAssociation.Builder>();
		List<EdmAssociationSet.Builder> associationSets = new ArrayList<EdmAssociationSet.Builder>();
	
		EdmEntityType.Builder rowType = EdmEntityType.newBuilder().setNamespace(namespace).setName("Row");
		EdmEntitySet.Builder rowEntitySet = EdmEntitySet.newBuilder().setName("Rows").setEntityType(rowType);
		
		EdmEntityType.Builder columnType = ColumnWrapper.getEntityModel(namespace);
		EdmEntitySet.Builder columnEntitySet = EdmEntitySet.newBuilder().setName("DataColumns").setEntityType(columnType);
		
		EdmEntityType.Builder distinctCountRowType = DistinctCountRowWrapper.getEntityModel(namespace);
		EdmEntitySet.Builder distinctCountRowEntitySet = EdmEntitySet.newBuilder().setName("DistinctCountRows").setEntityType(distinctCountRowType);
		
		EdmEntityType.Builder genericColumnAggregationRowType = GenericColumnAggregationRowWrapper.getEntityModel(namespace);
		EdmEntitySet.Builder genericColumnAggregationRowEntitySet = EdmEntitySet.newBuilder().setName("GenericColumnAggregationRows").setEntityType(genericColumnAggregationRowType);
		
		EdmEntityType.Builder dbTableType = DbTableWrapper.getEntityModel(namespace);
		entityTypes.add(dbTableType);
		EdmEntitySet.Builder dbTableEntitySet = EdmEntitySet.newBuilder().setName("DbTable").setEntityType(dbTableType);
		entitySets.add(dbTableEntitySet);
		
		EdmEntityType.Builder mdDataType = MdDataWrapper.getEntityModel(namespace);
		entityTypes.add(mdDataType);
		EdmEntitySet.Builder mdDataEntitySet = EdmEntitySet.newBuilder().setName("MdData").setEntityType(mdDataType);
		entitySets.add(mdDataEntitySet);
		
		EdmEntityType.Builder mdDataDimType = MdDataDimWrapper.getEntityModel(namespace);
		entityTypes.add(mdDataDimType);
		EdmEntitySet.Builder mdDataDimEntitySet = EdmEntitySet.newBuilder().setName("MdDataDim").setEntityType(mdDataDimType);	
		entitySets.add(mdDataDimEntitySet);
		
		EdmEntityType.Builder mdHierarchyType = MdHierarchyWrapper.getEntityModel(namespace);
		entityTypes.add(mdHierarchyType);
		EdmEntitySet.Builder mdHierarchyEntitySet = EdmEntitySet.newBuilder().setName("MdHierarchy").setEntityType(mdHierarchyType);	
		entitySets.add(mdHierarchyEntitySet);

		EdmEntityType.Builder mdHierNodeType = MdHierNodeWrapper.getEntityModel(namespace);
		entityTypes.add(mdHierNodeType);
		EdmEntitySet.Builder mdHierNodeEntitySet = EdmEntitySet.newBuilder().setName("MdHierNode").setEntityType(mdHierNodeType);	
		entitySets.add(mdHierNodeEntitySet);
		
		EdmEntityType.Builder mdLuHierTypeType = MdLuHierTypeWrapper.getEntityModel(namespace);
		entityTypes.add(mdLuHierTypeType);
		EdmEntitySet.Builder mdLuHierTypeEntitySet = EdmEntitySet.newBuilder().setName("MdLuHierType").setEntityType(mdLuHierTypeType);	
		entitySets.add(mdLuHierTypeEntitySet);
		
		EdmEntityType.Builder mdMeasureFieldsType = MdMeasureFieldsWrapper.getEntityModel(namespace);
		entityTypes.add(mdMeasureFieldsType);
		EdmEntitySet.Builder mdMeasureFieldsEntitySet = EdmEntitySet.newBuilder().setName("MdMeasureFields").setEntityType(mdMeasureFieldsType);	
		entitySets.add(mdMeasureFieldsEntitySet);
		
//		EdmEntityType.Builder mdRelHierNodeType = MdRelHierNodeWrapper.getEntityModel(namespace);
//		entityTypes.add(mdRelHierNodeType);
//		EdmEntitySet.Builder mdRelHierNodeEntitySet = EdmEntitySet.newBuilder().setName("MdRelHierNode").setEntityType(mdRelHierNodeType);	
//		entitySets.add(mdRelHierNodeEntitySet);		
		EdmAssociationEnd.Builder dbTableRowEnd = EdmAssociationEnd.newBuilder().setRole("DbTable").setType(dbTableType).setMultiplicity(EdmMultiplicity.ONE);
		EdmAssociationEnd.Builder rowDbTableEnd = EdmAssociationEnd.newBuilder().setRole("Rows").setType(rowType).setMultiplicity(EdmMultiplicity.MANY);
		EdmAssociation.Builder dbTableRow = EdmAssociation.newBuilder().setName("MdData-Rows").setEnds(dbTableRowEnd, rowDbTableEnd);
		associations.add(dbTableRow);
		EdmAssociationSet.Builder tableRows = EdmAssociationSet.newBuilder().setName("tableRows").setAssociation(dbTableRow).setEnds(
		EdmAssociationSetEnd.newBuilder().setRole(dbTableRowEnd).setEntitySet(dbTableEntitySet),
		EdmAssociationSetEnd.newBuilder().setRole(rowDbTableEnd).setEntitySet(rowEntitySet));
		associationSets.add(tableRows);

		
		EdmAssociationEnd.Builder mdDataRowEnd = EdmAssociationEnd.newBuilder().setRole("MdData").setType(mdDataType).setMultiplicity(EdmMultiplicity.ONE);
		EdmAssociationEnd.Builder rowMdDataEnd = EdmAssociationEnd.newBuilder().setRole("Rows").setType(rowType).setMultiplicity(EdmMultiplicity.MANY);
		EdmAssociationEnd.Builder mdDataColumnEnd = EdmAssociationEnd.newBuilder().setRole("MdData").setType(mdDataType).setMultiplicity(EdmMultiplicity.ONE);
		EdmAssociationEnd.Builder columnMdDataEnd = EdmAssociationEnd.newBuilder().setRole("Columns").setType(columnType).setMultiplicity(EdmMultiplicity.MANY);
		EdmAssociationEnd.Builder mdDataDistinctCountRowEnd = EdmAssociationEnd.newBuilder().setRole("MdData").setType(mdDataType).setMultiplicity(EdmMultiplicity.ONE);
		EdmAssociationEnd.Builder distinctCountRowMdDataEnd = EdmAssociationEnd.newBuilder().setRole("DistinctCountRows").setType(distinctCountRowType).setMultiplicity(EdmMultiplicity.MANY);
		EdmAssociationEnd.Builder mdDataGenericColumnAggregationRowEnd = EdmAssociationEnd.newBuilder().setRole("MdData").setType(mdDataType).setMultiplicity(EdmMultiplicity.ONE);
		EdmAssociationEnd.Builder genericColumnAggregationRowMdDataEnd = EdmAssociationEnd.newBuilder().setRole("GenericColumnAggregationRows").setType(genericColumnAggregationRowType).setMultiplicity(EdmMultiplicity.MANY);
		EdmAssociationEnd.Builder mdDataMdDataDimEnd = EdmAssociationEnd.newBuilder().setRole("MdData").setType(mdDataType).setMultiplicity(EdmMultiplicity.ONE);
		EdmAssociationEnd.Builder mdDataDimMdDataEnd = EdmAssociationEnd.newBuilder().setRole("MdDataDim").setType(mdDataDimType).setMultiplicity(EdmMultiplicity.MANY);
		EdmAssociation.Builder mdDataRow = EdmAssociation.newBuilder().setName("MdData-Rows").setEnds(mdDataRowEnd, rowMdDataEnd);
		EdmAssociation.Builder mdDataColumn = EdmAssociation.newBuilder().setName("MdData-Columns").setEnds(mdDataColumnEnd, columnMdDataEnd);
		EdmAssociation.Builder mdDataDistinctCountRow = EdmAssociation.newBuilder().setName("MdData-DistinctCountRows").setEnds(mdDataDistinctCountRowEnd, distinctCountRowMdDataEnd);
		EdmAssociation.Builder mdDataGenericColumnAggregationRow = EdmAssociation.newBuilder().setName("MdData-GenericColumnAggregationRows").setEnds(mdDataGenericColumnAggregationRowEnd, genericColumnAggregationRowMdDataEnd);
		EdmAssociation.Builder mdDataMdDataDim =EdmAssociation.newBuilder().setName("MdData-MdDataDim").setEnds(mdDataMdDataDimEnd, mdDataDimMdDataEnd);
		EdmAssociation.Builder mdDataDimMdData =EdmAssociation.newBuilder().setName("MdDataDim-MdData").setEnds(mdDataDimMdDataEnd, mdDataMdDataDimEnd);
		associations.add(mdDataMdDataDim);	 
		associations.add(mdDataDimMdData);
		associations.add(mdDataRow);
		associations.add(mdDataColumn);
		associations.add(mdDataDistinctCountRow);
		EdmAssociationSet.Builder dataDimensions = EdmAssociationSet.newBuilder().setName("dataDimensions").setAssociation(mdDataMdDataDim).setEnds(
		EdmAssociationSetEnd.newBuilder().setRole(mdDataMdDataDimEnd).setEntitySet(mdDataEntitySet),
		EdmAssociationSetEnd.newBuilder().setRole(mdDataDimMdDataEnd).setEntitySet(mdDataDimEntitySet));
		associationSets.add(dataDimensions);
		EdmAssociationSet.Builder dimensionData = EdmAssociationSet.newBuilder().setName("dimensionData").setAssociation(mdDataDimMdData).setEnds(
		EdmAssociationSetEnd.newBuilder().setRole(mdDataDimMdDataEnd).setEntitySet(mdDataDimEntitySet),
		EdmAssociationSetEnd.newBuilder().setRole(mdDataMdDataDimEnd).setEntitySet(mdDataEntitySet));
		associationSets.add(dimensionData);
		EdmAssociationSet.Builder dataRows = EdmAssociationSet.newBuilder().setName("dataRows").setAssociation(mdDataRow).setEnds(
		EdmAssociationSetEnd.newBuilder().setRole(mdDataRowEnd).setEntitySet(mdDataEntitySet),
		EdmAssociationSetEnd.newBuilder().setRole(rowMdDataEnd).setEntitySet(rowEntitySet));
		associationSets.add(dataRows);
		EdmAssociationSet.Builder dataColumns = EdmAssociationSet.newBuilder().setName("dataColumns").setAssociation(mdDataColumn).setEnds(
		EdmAssociationSetEnd.newBuilder().setRole(mdDataColumnEnd).setEntitySet(mdDataEntitySet),
		EdmAssociationSetEnd.newBuilder().setRole(columnMdDataEnd).setEntitySet(columnEntitySet));
		associationSets.add(dataColumns);
		EdmAssociationSet.Builder dataDistinctCountRows = EdmAssociationSet.newBuilder().setName("dataDistinctCountRows").setAssociation(mdDataDistinctCountRow).setEnds(
		EdmAssociationSetEnd.newBuilder().setRole(mdDataDistinctCountRowEnd).setEntitySet(mdDataEntitySet),
		EdmAssociationSetEnd.newBuilder().setRole(distinctCountRowMdDataEnd).setEntitySet(distinctCountRowEntitySet));
		associationSets.add(dataDistinctCountRows);
		EdmAssociationSet.Builder dataGenericColumnAggregationRows = EdmAssociationSet.newBuilder().setName("dataGenericColumnAggregationRows").setAssociation(mdDataGenericColumnAggregationRow).setEnds(
		EdmAssociationSetEnd.newBuilder().setRole(mdDataGenericColumnAggregationRowEnd).setEntitySet(mdDataEntitySet),
		EdmAssociationSetEnd.newBuilder().setRole(genericColumnAggregationRowMdDataEnd).setEntitySet(genericColumnAggregationRowEntitySet));
		associationSets.add(dataGenericColumnAggregationRows);
		
		
		EdmAssociationEnd.Builder mdDataMdMeasureFieldsEnd = EdmAssociationEnd.newBuilder().setRole("MdData").setType(mdDataType).setMultiplicity(EdmMultiplicity.ONE);
		EdmAssociationEnd.Builder mdMeasureFieldsMdDataEnd = EdmAssociationEnd.newBuilder().setRole("MdMeasureFields").setType(mdMeasureFieldsType).setMultiplicity(EdmMultiplicity.MANY);
		EdmAssociation.Builder mdDataMdMeasureFields =EdmAssociation.newBuilder().setName("MdData-MdMeasureFields").setEnds(mdDataMdMeasureFieldsEnd, mdMeasureFieldsMdDataEnd);
		EdmAssociation.Builder mdMeasureFieldsMdData =EdmAssociation.newBuilder().setName("MdMeasureFields-MdData").setEnds(mdMeasureFieldsMdDataEnd, mdDataMdMeasureFieldsEnd);
		associations.add(mdDataMdMeasureFields);	
		associations.add(mdMeasureFieldsMdData);
		EdmAssociationSet.Builder dataMeasures = EdmAssociationSet.newBuilder().setName("dataMeasures").setAssociation(mdDataMdMeasureFields).setEnds(
		EdmAssociationSetEnd.newBuilder().setRole(mdDataMdMeasureFieldsEnd).setEntitySet(mdDataEntitySet),
		EdmAssociationSetEnd.newBuilder().setRole(mdMeasureFieldsMdDataEnd).setEntitySet(mdMeasureFieldsEntitySet));
		associationSets.add(dataMeasures);
		EdmAssociationSet.Builder measureData = EdmAssociationSet.newBuilder().setName("measureData").setAssociation(mdMeasureFieldsMdData).setEnds(
		EdmAssociationSetEnd.newBuilder().setRole(mdMeasureFieldsMdDataEnd).setEntitySet(mdMeasureFieldsEntitySet),
		EdmAssociationSetEnd.newBuilder().setRole(mdDataMdMeasureFieldsEnd).setEntitySet(mdDataEntitySet));
		associationSets.add(measureData);
		
		EdmAssociationEnd.Builder mdHierNodeRowEnd = EdmAssociationEnd.newBuilder().setRole("MdHierNode").setType(mdHierNodeType).setMultiplicity(EdmMultiplicity.ONE);
		EdmAssociationEnd.Builder rowMdHierNodeEnd = EdmAssociationEnd.newBuilder().setRole("Rows").setType(rowType).setMultiplicity(EdmMultiplicity.MANY);
		EdmAssociation.Builder mdHierNodeRow = EdmAssociation.newBuilder().setName("MdHierNode-Rows").setEnds(mdHierNodeRowEnd, rowMdHierNodeEnd);
		EdmAssociationEnd.Builder mdHierNodeMdDataDimEnd = EdmAssociationEnd.newBuilder().setRole("MdHierNode").setType(mdHierNodeType).setMultiplicity(EdmMultiplicity.ONE);
		EdmAssociationEnd.Builder mdDataDimMdHierNodeEnd = EdmAssociationEnd.newBuilder().setRole("MdDataDim").setType(mdDataDimType).setMultiplicity(EdmMultiplicity.MANY);
		EdmAssociation.Builder mdHierNodeMdDataDim =EdmAssociation.newBuilder().setName("MdHierNode-MdDataDim").setEnds(mdHierNodeMdDataDimEnd, mdDataDimMdHierNodeEnd);
		EdmAssociation.Builder mdDataDimMdHierNode =EdmAssociation.newBuilder().setName("MdDataDim-MdHierNode").setEnds(mdDataDimMdHierNodeEnd, mdHierNodeMdDataDimEnd);
		associations.add(mdHierNodeMdDataDim);	 
		associations.add(mdDataDimMdHierNode);
		associations.add(mdHierNodeRow);
		EdmAssociationSet.Builder nodeData = EdmAssociationSet.newBuilder().setName("nodeData").setAssociation(mdHierNodeMdDataDim).setEnds(
		EdmAssociationSetEnd.newBuilder().setRole(mdHierNodeMdDataDimEnd).setEntitySet(mdHierNodeEntitySet),
		EdmAssociationSetEnd.newBuilder().setRole(mdDataDimMdHierNodeEnd).setEntitySet(mdDataDimEntitySet));
		associationSets.add(nodeData);
		EdmAssociationSet.Builder dataDimension = EdmAssociationSet.newBuilder().setName("dataNode").setAssociation(mdDataDimMdHierNode).setEnds(
		EdmAssociationSetEnd.newBuilder().setRole(mdDataDimMdHierNodeEnd).setEntitySet(mdDataDimEntitySet),
		EdmAssociationSetEnd.newBuilder().setRole(mdHierNodeMdDataDimEnd).setEntitySet(mdHierNodeEntitySet));
		associationSets.add(dataDimension);
		EdmAssociationSet.Builder nodeRows = EdmAssociationSet.newBuilder().setName("nodeRows").setAssociation(mdHierNodeRow).setEnds(
		EdmAssociationSetEnd.newBuilder().setRole(mdHierNodeRowEnd).setEntitySet(mdHierNodeEntitySet),
		EdmAssociationSetEnd.newBuilder().setRole(rowMdHierNodeEnd).setEntitySet(rowEntitySet));
		associationSets.add(nodeRows);
		
		EdmAssociationEnd.Builder mdLuHierTypeMdHierarchyEnd = EdmAssociationEnd.newBuilder().setRole("MdLuHierType").setType(mdLuHierTypeType).setMultiplicity(EdmMultiplicity.ONE);
		EdmAssociationEnd.Builder mdHierarchyMdLuHierTypeEnd = EdmAssociationEnd.newBuilder().setRole("MdHierarchy").setType(mdHierarchyType).setMultiplicity(EdmMultiplicity.MANY);
		EdmAssociation.Builder mdLuHierTypeMdHierarchy =EdmAssociation.newBuilder().setName("MdLuHierType-MdHierarchy").setEnds(mdLuHierTypeMdHierarchyEnd, mdHierarchyMdLuHierTypeEnd);
		EdmAssociation.Builder mdHierarchyMdLuHierType =EdmAssociation.newBuilder().setName("MdHierarchy-MdLuHierType").setEnds(mdHierarchyMdLuHierTypeEnd, mdLuHierTypeMdHierarchyEnd);
		associations.add(mdLuHierTypeMdHierarchy);	   
		associations.add(mdHierarchyMdLuHierType);
		EdmAssociationSet.Builder hierarchiesByType = EdmAssociationSet.newBuilder().setName("hierarchiesByType").setAssociation(mdLuHierTypeMdHierarchy).setEnds(
		EdmAssociationSetEnd.newBuilder().setRole(mdLuHierTypeMdHierarchyEnd).setEntitySet(mdLuHierTypeEntitySet),
		EdmAssociationSetEnd.newBuilder().setRole(mdHierarchyMdLuHierTypeEnd).setEntitySet(mdHierarchyEntitySet));
		associationSets.add(hierarchiesByType);
		EdmAssociationSet.Builder hierarchyType = EdmAssociationSet.newBuilder().setName("hierarchyType").setAssociation(mdHierarchyMdLuHierType).setEnds(
		EdmAssociationSetEnd.newBuilder().setRole(mdHierarchyMdLuHierTypeEnd).setEntitySet(mdHierarchyEntitySet),
		EdmAssociationSetEnd.newBuilder().setRole(mdLuHierTypeMdHierarchyEnd).setEntitySet(mdLuHierTypeEntitySet));
		associationSets.add(hierarchyType);
		
		EdmAssociationEnd.Builder mdHierarchyMdHierNodeEnd = EdmAssociationEnd.newBuilder().setRole("MdHierarchy").setType(mdHierarchyType).setMultiplicity(EdmMultiplicity.ONE);
		EdmAssociationEnd.Builder mdHierNodeMdHierarchyEnd = EdmAssociationEnd.newBuilder().setRole("MdHierNode").setType(mdHierNodeType).setMultiplicity(EdmMultiplicity.MANY);
		EdmAssociation.Builder mdHierarchyMdHierNode =EdmAssociation.newBuilder().setName("MdHierarchy-MdHierNode").setEnds(mdHierarchyMdHierNodeEnd, mdHierNodeMdHierarchyEnd);
		EdmAssociation.Builder mdHierNodeMdHierarchy =EdmAssociation.newBuilder().setName("MdHierNode-MdHierarchy").setEnds(mdHierNodeMdHierarchyEnd, mdHierarchyMdHierNodeEnd);
		associations.add(mdHierarchyMdHierNode);
		associations.add(mdHierNodeMdHierarchy);
		EdmAssociationSet.Builder nodesByHierarchy = EdmAssociationSet.newBuilder().setName("nodesByHierarchy").setAssociation(mdHierarchyMdHierNode).setEnds(
		EdmAssociationSetEnd.newBuilder().setRole(mdHierarchyMdHierNodeEnd).setEntitySet(mdHierarchyEntitySet),
		EdmAssociationSetEnd.newBuilder().setRole(mdHierNodeMdHierarchyEnd).setEntitySet(mdHierNodeEntitySet));
		associationSets.add(nodesByHierarchy);
		EdmAssociationSet.Builder nodeHierarchy = EdmAssociationSet.newBuilder().setName("nodeHierarchy").setAssociation(mdHierNodeMdHierarchy).setEnds(
		EdmAssociationSetEnd.newBuilder().setRole(mdHierNodeMdHierarchyEnd).setEntitySet(mdHierNodeEntitySet),
		EdmAssociationSetEnd.newBuilder().setRole(mdHierarchyMdHierNodeEnd).setEntitySet(mdHierarchyEntitySet));
		associationSets.add(nodeHierarchy);
		
//		EdmAssociationEnd.Builder mdRelHierNodeMdHierNodeEnd = EdmAssociationEnd.newBuilder().setRole("MdRelHierNode").setType(mdRelHierNodeType).setMultiplicity(EdmMultiplicity.MANY);
//		EdmAssociationEnd.Builder mdHierNodeMdRelHierNodeEnd = EdmAssociationEnd.newBuilder().setRole("MdHierNode").setType(mdHierNodeType).setMultiplicity(EdmMultiplicity.ONE);
//		EdmAssociation.Builder mdRelHierNodeMdHierNode =EdmAssociation.newBuilder().setName("MdRelHierNode-MdHierNode").setEnds(mdRelHierNodeMdHierNodeEnd, mdHierNodeMdRelHierNodeEnd);
//		EdmAssociation.Builder mdHierNodeMdRelHierNode =EdmAssociation.newBuilder().setName("MdHierNode-MdRelHierNode").setEnds(mdHierNodeMdRelHierNodeEnd, mdRelHierNodeMdHierNodeEnd);
//		associations.add(mdRelHierNodeMdHierNode);
//		associations.add(mdHierNodeMdRelHierNode);
//		EdmAssociationSet.Builder parentNode = EdmAssociationSet.newBuilder().setName("parentNode").setAssociation(mdRelHierNodeMdHierNode).setEnds(
//		EdmAssociationSetEnd.newBuilder().setRole(mdRelHierNodeMdHierNodeEnd).setEntitySet(mdRelHierNodeEntitySet),
//		EdmAssociationSetEnd.newBuilder().setRole(mdHierNodeMdRelHierNodeEnd).setEntitySet(mdHierNodeEntitySet));
//		associationSets.add(parentNode);
//		EdmAssociationSet.Builder childNode = EdmAssociationSet.newBuilder().setName("childNode").setAssociation(mdRelHierNodeMdHierNode).setEnds(
//		EdmAssociationSetEnd.newBuilder().setRole(mdRelHierNodeMdHierNodeEnd).setEntitySet(mdRelHierNodeEntitySet),
//		EdmAssociationSetEnd.newBuilder().setRole(mdHierNodeMdRelHierNodeEnd).setEntitySet(mdHierNodeEntitySet));
//		associationSets.add(childNode);
//		EdmAssociationSet.Builder relationsAsParent = EdmAssociationSet.newBuilder().setName("relationsAsParent").setAssociation(mdHierNodeMdRelHierNode).setEnds(
//		EdmAssociationSetEnd.newBuilder().setRole(mdHierNodeMdRelHierNodeEnd).setEntitySet(mdHierNodeEntitySet),
//		EdmAssociationSetEnd.newBuilder().setRole(mdRelHierNodeMdHierNodeEnd).setEntitySet(mdRelHierNodeEntitySet));
//		associationSets.add(relationsAsParent);
//		EdmAssociationSet.Builder relationsAsChild = EdmAssociationSet.newBuilder().setName("relationsAsChild").setAssociation(mdHierNodeMdRelHierNode).setEnds(
//		EdmAssociationSetEnd.newBuilder().setRole(mdHierNodeMdRelHierNodeEnd).setEntitySet(mdHierNodeEntitySet),
//		EdmAssociationSetEnd.newBuilder().setRole(mdRelHierNodeMdHierNodeEnd).setEntitySet(mdRelHierNodeEntitySet));
//		associationSets.add(relationsAsChild);
				
		EdmNavigationProperty.Builder tableRowsNavigationProperty = EdmNavigationProperty.newBuilder("TableRows").setRelationship(dbTableRow).setFromTo(dbTableRow.getEnd1(), dbTableRow.getEnd2());
		dbTableType.addNavigationProperties(tableRowsNavigationProperty);
		EdmNavigationProperty.Builder dataRowsNavigationProperty = EdmNavigationProperty.newBuilder("DataRows").setRelationship(mdDataRow).setFromTo(mdDataRow.getEnd1(), mdDataRow.getEnd2());
		mdDataType.addNavigationProperties(dataRowsNavigationProperty);
		EdmNavigationProperty.Builder dataColumnsNavigationProperty = EdmNavigationProperty.newBuilder("DataColumns").setRelationship(mdDataRow).setFromTo(mdDataColumn.getEnd1(), mdDataColumn.getEnd2());
		mdDataType.addNavigationProperties(dataColumnsNavigationProperty);
		EdmNavigationProperty.Builder dataDistinctCountRowsNavigationProperty = EdmNavigationProperty.newBuilder("DistinctCountRows").setRelationship(mdDataDistinctCountRow).setFromTo(mdDataDistinctCountRow.getEnd1(), mdDataDistinctCountRow.getEnd2());
		mdDataType.addNavigationProperties(dataDistinctCountRowsNavigationProperty);
		EdmNavigationProperty.Builder dataGenericColumnAggregationRowsNavigationProperty = EdmNavigationProperty.newBuilder("GenericColumnAggregationRows").setRelationship(mdDataGenericColumnAggregationRow).setFromTo(mdDataGenericColumnAggregationRow.getEnd1(), mdDataGenericColumnAggregationRow.getEnd2());
		mdDataType.addNavigationProperties(dataGenericColumnAggregationRowsNavigationProperty);
		EdmNavigationProperty.Builder rawDataRowsNavigationProperty = EdmNavigationProperty.newBuilder("RawDataRows").setRelationship(mdDataRow).setFromTo(mdDataRow.getEnd1(), mdDataRow.getEnd2());
		mdDataType.addNavigationProperties(rawDataRowsNavigationProperty);
		EdmNavigationProperty.Builder dataDimensionsNavigationProperty = EdmNavigationProperty.newBuilder("DataDimensions").setRelationship(mdDataMdDataDim).setFromTo(mdDataMdDataDim.getEnd1(), mdDataMdDataDim.getEnd2());
		mdDataType.addNavigationProperties(dataDimensionsNavigationProperty);
		EdmNavigationProperty.Builder dimensionDataNavigationProperty = EdmNavigationProperty.newBuilder("IdData").setRelationship(mdDataDimMdData).setFromTo(mdDataDimMdData.getEnd1(), mdDataDimMdData.getEnd2());
		mdDataDimType.addNavigationProperties(dimensionDataNavigationProperty);  
		EdmNavigationProperty.Builder dataMeasuresNavigationProperty = EdmNavigationProperty.newBuilder("DataMeasures").setRelationship(mdDataMdMeasureFields).setFromTo(mdDataMdMeasureFields.getEnd1(), mdDataMdMeasureFields.getEnd2());
		mdDataType.addNavigationProperties(dataMeasuresNavigationProperty);  
		EdmNavigationProperty.Builder measureDataNavigationProperty = EdmNavigationProperty.newBuilder("IdData").setRelationship(mdMeasureFieldsMdData).setFromTo(mdMeasureFieldsMdData.getEnd1(), mdMeasureFieldsMdData.getEnd2());
		mdMeasureFieldsType.addNavigationProperties(measureDataNavigationProperty);  
		EdmNavigationProperty.Builder nodeRowsNavigationProperty = EdmNavigationProperty.newBuilder("NodeRows").setRelationship(mdHierNodeRow).setFromTo(mdHierNodeRow.getEnd1(), mdHierNodeRow.getEnd2());
		mdHierNodeType.addNavigationProperties(nodeRowsNavigationProperty);
		EdmNavigationProperty.Builder nodeDataNavigationProperty = EdmNavigationProperty.newBuilder("NodeData").setRelationship(mdHierNodeMdDataDim).setFromTo(mdHierNodeMdDataDim.getEnd1(), mdHierNodeMdDataDim.getEnd2());
		mdHierNodeType.addNavigationProperties(nodeDataNavigationProperty);  
		EdmNavigationProperty.Builder dataDimensionNavigationProperty = EdmNavigationProperty.newBuilder("IdHierNode").setRelationship(mdDataDimMdHierNode).setFromTo(mdDataDimMdHierNode.getEnd1(), mdDataDimMdHierNode.getEnd2());
		mdDataDimType.addNavigationProperties(dataDimensionNavigationProperty);  
		EdmNavigationProperty.Builder hierarchiesByTypeNavigationProperty = EdmNavigationProperty.newBuilder("HierarchiesByType").setRelationship(mdLuHierTypeMdHierarchy).setFromTo(mdLuHierTypeMdHierarchy.getEnd1(), mdLuHierTypeMdHierarchy.getEnd2());
		mdLuHierTypeType.addNavigationProperties(hierarchiesByTypeNavigationProperty);  
		EdmNavigationProperty.Builder hierarchyTypeNavigationProperty = EdmNavigationProperty.newBuilder("IdLuHierType").setRelationship(mdHierarchyMdLuHierType).setFromTo(mdHierarchyMdLuHierType.getEnd1(), mdHierarchyMdLuHierType.getEnd2());
		mdHierarchyType.addNavigationProperties(hierarchyTypeNavigationProperty);  
		EdmNavigationProperty.Builder nodesByHierarchyNavigationProperty = EdmNavigationProperty.newBuilder("NodesByHierarchy").setRelationship(mdHierarchyMdHierNode).setFromTo(mdHierarchyMdHierNode.getEnd1(), mdHierarchyMdHierNode.getEnd2());
		mdHierarchyType.addNavigationProperties(nodesByHierarchyNavigationProperty);  
		EdmNavigationProperty.Builder nodeHierarchyNavigationProperty = EdmNavigationProperty.newBuilder("IdHierarchy").setRelationship(mdHierNodeMdHierarchy).setFromTo(mdHierNodeMdHierarchy.getEnd1(), mdHierNodeMdHierarchy.getEnd2());
		mdHierNodeType.addNavigationProperties(nodeHierarchyNavigationProperty);  
//		EdmNavigationProperty.Builder parentNodeNavigationProperty = EdmNavigationProperty.newBuilder("IdParent").setRelationship(mdRelHierNodeMdHierNode).setFromTo(mdRelHierNodeMdHierNode.getEnd1(), mdRelHierNodeMdHierNode.getEnd2());
//		mdRelHierNodeType.addNavigationProperties(parentNodeNavigationProperty); 
//		EdmNavigationProperty.Builder childNodeNavigationProperty = EdmNavigationProperty.newBuilder("IdChild").setRelationship(mdRelHierNodeMdHierNode).setFromTo(mdRelHierNodeMdHierNode.getEnd1(), mdRelHierNodeMdHierNode.getEnd2());
//		mdRelHierNodeType.addNavigationProperties(childNodeNavigationProperty); 
//		EdmNavigationProperty.Builder relationsAsParentNavigationProperty = EdmNavigationProperty.newBuilder("RelationsAsParent").setRelationship(mdHierNodeMdRelHierNode).setFromTo(mdHierNodeMdRelHierNode.getEnd1(), mdHierNodeMdRelHierNode.getEnd2());
//		mdHierNodeType.addNavigationProperties(relationsAsParentNavigationProperty); 
//		EdmNavigationProperty.Builder relationsAsChildNavigationProperty = EdmNavigationProperty.newBuilder("RelationsAsChild").setRelationship(mdHierNodeMdRelHierNode).setFromTo(mdHierNodeMdRelHierNode.getEnd1(), mdHierNodeMdRelHierNode.getEnd2());
//		mdHierNodeType.addNavigationProperties(relationsAsChildNavigationProperty); 
		EdmEntityContainer.Builder container = EdmEntityContainer.newBuilder().setName(namespace + "/Entities").setIsDefault(true).addEntitySets( entitySets);
		EdmSchema.Builder modelSchema = EdmSchema.newBuilder().setNamespace(namespace + "/Model").addEntityTypes(entityTypes);
		EdmSchema.Builder containerSchema = EdmSchema.newBuilder().setNamespace(namespace + "/Container").addEntityContainers(container);

		edmDataServices = EdmDataServices.newBuilder().addSchemas(containerSchema, modelSchema).build();
	}

	@Override
	public EdmDataServices getMetadata() {
		/**
		 * Restituisco direttamente l'oggetto che identifica i metadati.
		 * L'oggetto è stato costruito nel costruttore e non sarà mai nullo.
		 */
		return edmDataServices;
	}

	@Override
	public MetadataProducer getMetadataProducer() {
		throw new NotImplementedException();
	}

	@Override
	public EntitiesResponse getEntities(String entitySetName,
			QueryInfo queryInfo) {	
		EdmEntitySet ees = edmDataServices.getEdmEntitySet(entitySetName);			
		if(ees == null){
			StatPortalOData.logger.error("getEntities: IllegalArgumentException");
			throw new IllegalArgumentException();
		}
		Entities requestedEntities = Entities.parse(ees.getName());
		if(requestedEntities == Entities.UNKNOWN){
			StatPortalOData.logger.error("getEntities: IllegalArgumentException");
			throw new IllegalArgumentException();
		}
		EntitiesResponse response = null;
		//controlliamo se dobbiamo simulare la count nell'attesa di odata4j 0.6
		if(queryInfo.customOptions.containsKey("count")){
			long total;
			switch(requestedEntities){
				case MD_DATA:
					total = MdData.countMdDatas();
					break;
				case MD_DATA_DIM:
					total = MdDataDim.countMdDataDims();
					break;
				case MD_MEASURE_FIELDS:
					total = MdMeasureFields.countMdMeasureFieldses();
					break;
				case MD_HIER_NODE:
					total = MdHierNode.countMdHierNodes();
					break;
				case MD_HIERARCHY:
					total = MdHierarchy.countMdHierarchys();
					break;
				case MD_LU_HIER_TYPE:
					total = MdLuHierType.countMdLuHierTypes();
					break;
				case DB_TABLE:
					total = MdData.countMdDatas();
					total += MdHierNode.countMdHierNodes();
					total += DbTable.countDbTables();
					break;
				default:
					total = 0;
			}
			List<OEntity> totalList = new ArrayList<OEntity>();
			EdmEntitySet totalEntitySet = jitProducer.getTotalEntitySet();
			totalList.add(TotalEntity.createTotalEntity(totalEntitySet,(int)total));
			return Responses.entities(totalList, totalEntitySet, null, null);
		}else if (queryInfo.customOptions.containsKey("checkConfiguration")){
			String dbName = queryInfo.customOptions.get("checkConfiguration");
			List<OEntity> checkList = new ArrayList<OEntity>();
			EdmEntitySet checkEntitySet = jitProducer.getCheckEntitySet();
			if(DBUtils.getDatabaseName().equals(dbName)){
				checkList.add(CheckEntity.createCheckEntity(checkEntitySet,1));
			}else{
				checkList.add(CheckEntity.createCheckEntity(checkEntitySet,-1));
			}
			return Responses.entities(checkList, checkEntitySet, null, null);
		}
		else{
			if(requestedEntities == Entities.DB_TABLE){
				EntitiesResponse dbTableResponse = baseProducer.getEntities(entitySetName, queryInfo);
				EntitiesResponse mdDataResponse = baseProducer.getEntities("MdData", queryInfo);
				EntitiesResponse mdHierNodeResponse = baseProducer.getEntities("MdHierNode", queryInfo);
				List<OEntity> dbTableList = dbTableResponse.getEntities();
				List<OEntity> mdDataList = mdDataResponse.getEntities();
				List<OEntity> mdHierNodeList = mdHierNodeResponse.getEntities();
				dbTableList.addAll(mdDataList);
				dbTableList.addAll(mdHierNodeList);
				response = Responses.entities(dbTableList, ees, 0, null);
			}
			else{
				response = baseProducer.getEntities(entitySetName, queryInfo);
			}
			List<OEntity> entities = createEntities(response, ees);
			return Responses.entities(entities, ees, null, null);
		}
	}

	/**
	 * Crea una lista di entità a partire da una risposta
	 * @param response la risposta del livello inferiore da cui estrarre le entità
	 * @param ees l'entity set richiesto
	 * @return una lista di entità
	 */
	private List<OEntity> createEntities(EntitiesResponse response,
			EdmEntitySet ees) {		
		List<OEntity> baseList = response.getEntities();
		List<OEntity> entities = new ArrayList<OEntity>();
		for(OEntity entity : baseList){
			try{
				entities.add(Translator.translate(ees, entity, baseProducer));
        	}catch(Exception ex){
    			StatPortalOData.logger.error(ex.getMessage(), ex);
        	}
		}
		return entities;
	}

	/**
	 * Crea una singola entità
	 * @param response la risposta del livello inferiore da cui estrarre l'entità
	 * @param ees l'entity set richiesto
	 * @return un'entità
	 */
	private OEntity createEntity(EntityResponse response, EdmEntitySet ees){
		try{
			return Translator.translate(ees, response.getEntity(), baseProducer);
		}
		catch(Exception ex){
			StatPortalOData.logger.error(ex.getMessage(), ex);
			return null;
		}
	}

	@Override
	public EntityResponse getEntity(String entitySetName, OEntityKey entityKey,
			QueryInfo queryInfo) {
		EdmEntitySet ees = edmDataServices.getEdmEntitySet(entitySetName);			
		if(ees == null){
			StatPortalOData.logger.error("getEntity: IllegalArgumentException");
			throw new IllegalArgumentException();
		}
		Entities requestedEntities = Entities.parse(ees.getName());
		if(requestedEntities == Entities.UNKNOWN) {
			StatPortalOData.logger.error("getEntity: IllegalArgumentException");
			throw new IllegalArgumentException();
		}
		EntityResponse response = null;
		if(requestedEntities == Entities.DB_TABLE){
			TreeSet<NamedValue<?>> nameKeys = (TreeSet<NamedValue<?>>) entityKey.asComplexValue();
			NamedValue<?> first = nameKeys.pollFirst();
			NamedValue<?> second = nameKeys.pollFirst();			
			Integer tableType = first.getName().compareTo("tableType") == 0 ? (Integer)first.getValue() : (Integer)second.getValue();
			Integer id = first.getName().compareTo("id") == 0 ? (Integer)first.getValue() : (Integer)second.getValue();
			String baseEntityName;
			switch(tableType){
				case 1:
					baseEntityName = "DbTable";
					break;
				case 2:
					baseEntityName = "MdData";
					break;
				case 3:
					baseEntityName = "MdHierNode";
					break;
				default:
					baseEntityName = "";
			}
			response = baseProducer.getEntity(baseEntityName, OEntityKey.create(id), queryInfo);
		}else{
			response = baseProducer.getEntity(entitySetName, entityKey, queryInfo);
		}
		return Responses.entity(createEntity(response, getMetadata().getEdmEntitySet(entitySetName)));
	}
	
	@Override
	public BaseResponse getNavProperty(String entitySetName,
			OEntityKey entityKey, String navProp, QueryInfo queryInfo) {
		EdmEntitySet ees = edmDataServices.findEdmEntitySet(entitySetName);
		EdmMultiplicity multiplicity = ees.getType().findNavigationProperty(navProp).getToRole().getMultiplicity();
		EntityResponse entityResponse = getEntity(entitySetName, entityKey, null);
		if(multiplicity == EdmMultiplicity.ONE){
			ORelatedEntityLink link = entityResponse.getEntity().getLink(navProp, ORelatedEntityLink.class);
			String href = link.getHref();
			String[] splittedHref = href.split("\\(");
			String id = splittedHref[1].split("\\)")[0];
			return getEntity(splittedHref[0], OEntityKey.create(Integer.valueOf(id)), queryInfo);
		}else if(multiplicity == EdmMultiplicity.MANY){
			if(navProp.compareTo("DataRows") == 0 || navProp.compareTo("NodeRows") == 0 || navProp.compareTo("TableRows") == 0 || navProp.compareTo("RawDataRows") == 0){
				try {
					if(navProp.compareTo("RawDataRows") == 0){
						return jitProducer.getRows(entityResponse.getEntity(), queryInfo, true);
					}else{
						return jitProducer.getRows(entityResponse.getEntity(), queryInfo, false);
					}
				} catch (OdataDomainException e){
					StatPortalOData.logger.error(e.getMessage(), e);
					return Responses.entities(new ArrayList<OEntity>(), ees, 0, null);
				} catch (OpenDataException e) {
					StatPortalOData.logger.error(e.getMessage(), e);
					return Responses.entities(new ArrayList<OEntity>(), ees, 0, null);
				}
			} else if(navProp.compareTo("DataColumns") == 0){
				try{
					return jitProducer.getColumns(entityResponse.getEntity(), queryInfo);					
				}catch(Exception ex){
					StatPortalOData.logger.error(ex.getMessage(), ex);
					return Responses.entities(new ArrayList<OEntity>(), ees, 0, null);
				}
			} else if(navProp.equals("DistinctCountRows")){
				try{
					return jitProducer.getDistinctCountRows(entityResponse.getEntity(), queryInfo);
				} catch (Exception ex){
					StatPortalOData.logger.error(ex.getMessage(), ex);
					return Responses.entities(new ArrayList<OEntity>(), ees, 0, null);
				}
			} else if(navProp.equals("GenericColumnAggregationRows")){
				try{
					return jitProducer.getGenericColumnAggregationRows(entityResponse.getEntity(), queryInfo);
				}catch(Exception ex){
					StatPortalOData.logger.error(ex.getMessage(), ex);
					return Responses.entities(new ArrayList<OEntity>(), ees, 0, null);
				}
			} else if(navProp.equals("DataDimensions")){
				try{
					return getDataDimensions(entityResponse.getEntity());
				}catch(Exception ex){
					StatPortalOData.logger.error(ex.getMessage(), ex);
					return Responses.entities(new ArrayList<OEntity>(), ees, 0, null);
				}				
			} else if(navProp.equals("DataMeasures")){
				try{
					return getDataMeasures(entityResponse.getEntity());
				}catch(Exception ex){
					StatPortalOData.logger.error(ex.getMessage(), ex);
					return Responses.entities(new ArrayList<OEntity>(), ees, 0, null);
				}
			}
			BaseResponse baseResponse = baseProducer.getNavProperty(entitySetName, entityKey, Translator.translateNavProperty(entitySetName, navProp), queryInfo);
			EdmEntitySet targetEntitySet = edmDataServices.findEdmEntitySet(Translator.targetEntitySet(entitySetName, navProp));
			List<OEntity> entities = createEntities((EntitiesResponse)baseResponse, targetEntitySet);
			return Responses.entities(entities, targetEntitySet, 0, null);
		}

		throw new NotImplementedException();
	}

	/**
	 * Fornisce una risposta costituita da un insieme di entità che rappresentano le misure di un dato
	 * @param entity l'entità che rappresenta il dato
	 * @return una risposta costituita da un insieme di entità che rappresentano le misure di un dat
	 * @throws OdataDomainException
	 */
	private BaseResponse getDataMeasures(OEntity entity) throws OdataDomainException {
		MdData data = MdData.findMdData((Integer)entity.getEntityKey().asSingleValue());
		List<MdMeasureFields> measureList = data.getSortedMdMeasureFields();
		EdmEntitySet ees = edmDataServices.findEdmEntitySet("MdMeasureFields");
		List<OEntity> entityList = buildMeasureEntities(measureList, ees);
		return Responses.entities(entityList, ees, 0, null);
	}

	/**
	 * Costruisce una lista di entità rappresentanti le misure di un dato
	 * @param measureList l'elenco di misure di un dato
	 * @param targetEntitySet l'entità set richiesto
	 * @return una lista di entità rappresentanti le misure di un dato
	 */
	private List<OEntity> buildMeasureEntities(List<MdMeasureFields> measureList, EdmEntitySet targetEntitySet) {
		List<OEntity> list = new ArrayList<OEntity>();
		for(MdMeasureFields mdMeasureFields : measureList){
			list.add(MdMeasureFieldsWrapper.createInstance(targetEntitySet, mdMeasureFields));
		}
		return list;
	}

	/**
	 * Fornisce una risposta costituita da un insieme di entità che rappresentano le dimensioni di un dato
	 * @param entity l'entità che rappresenta il dato
	 * @return una risposta costituita da un insieme di entità che rappresentano le dimensioni di un dato
	 * @throws OdataDomainException
	 */
	private BaseResponse getDataDimensions(OEntity entity) throws OdataDomainException {
		MdData data = MdData.findMdData((Integer)entity.getEntityKey().asSingleValue());
		List<MdDataDim> dimensionList = data.getSortedMdDataDims();
		EdmEntitySet ees = edmDataServices.findEdmEntitySet("MdDataDim");
		List<OEntity> entityList = buildDimensionEntities(dimensionList, ees);
		return Responses.entities(entityList, ees, 0, null);
	}

	/**
	 * Costruisce una lista di entità rappresentanti le dimensioni di un dato
	 * @param dimensionList l'elenco di dimensioni di un dato
	 * @param targetEntitySet l'entità set richiesto
	 * @return una lista di entità rappresentanti le dimensioni di un dato
	 */
	private List<OEntity> buildDimensionEntities(List<MdDataDim> dimensionList, EdmEntitySet targetEntitySet) {
		List<OEntity> list = new ArrayList<OEntity>();
		for(MdDataDim mdDataDim : dimensionList){
			list.add(MdDataDimWrapper.createInstance(targetEntitySet, mdDataDim));
		}
		return list;
	}

	@Override
	public void close() {
		throw new NotImplementedException();
	}

	@Override
	public EntityResponse createEntity(String entitySetName, OEntity entity) {
		throw new NotImplementedException();
	}

	@Override
	public EntityResponse createEntity(String entitySetName,
			OEntityKey entityKey, String navProp, OEntity entity) {
		throw new NotImplementedException();
	}

	@Override
	public void deleteEntity(String entitySetName, OEntityKey entityKey) {
		throw new NotImplementedException();
	}

	@Override
	public void mergeEntity(String entitySetName, OEntity entity) {
		throw new NotImplementedException();
	}

	@Override
	public void updateEntity(String entitySetName, OEntity entity) {
		throw new NotImplementedException();
	}

	@Override
	public EntityIdResponse getLinks(OEntityId sourceEntity,
			String targetNavProp) {
		throw new NotImplementedException();
	}

	@Override
	public void createLink(OEntityId sourceEntity, String targetNavProp,
			OEntityId targetEntity) {
		throw new NotImplementedException();
	}

	@Override
	public void updateLink(OEntityId sourceEntity, String targetNavProp,
			OEntityKey oldTargetEntityKey, OEntityId newTargetEntity) {
		throw new NotImplementedException();
	}

	@Override
	public void deleteLink(OEntityId sourceEntity, String targetNavProp,
			OEntityKey targetEntityKey) {
		throw new NotImplementedException();
	}

	@Override
	public BaseResponse callFunction(EdmFunctionImport name,
			Map<String, OFunctionParameter> params, QueryInfo queryInfo) {
		throw new NotImplementedException();
	}
	
}