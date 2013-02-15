package it.sister.statportal.odata.proxy;

import it.sister.statportal.odata.Entities;
import it.sister.statportal.odata.domain.Column;
import it.sister.statportal.odata.domain.Column.ColumnType;
import it.sister.statportal.odata.domain.MdData;
import it.sister.statportal.odata.domain.MdDataDim;
import it.sister.statportal.odata.domain.MdDataFiles;
import it.sister.statportal.odata.domain.MdMeasureFields;
import it.sister.statportal.odata.domain.OdataDomainException;
import it.sister.statportal.odata.domain.Row;
import it.sister.statportal.odata.proxy.rdf.RdfFormatter;
import it.sister.statportal.odata.proxy.rdf.RdfWriter;
import it.sister.statportal.odata.proxy.rdf.XmlRdfFormatter;
import it.sister.statportal.odata.utility.DBUtils;
import it.sister.statportal.odata.wrapper.ColumnWrapper;
import it.sister.statportal.odata.wrapper.DistinctCountRowWrapper;
import it.sister.statportal.odata.wrapper.GenericColumnAggregationRowWrapper;
import it.sister.statportal.odata.wrapper.MdDataDimWrapper;
import it.sister.statportal.odata.wrapper.MdDataWrapper;
import it.sister.statportal.odata.wrapper.MdMeasureFieldsWrapper;
import it.sister.statportal.odata.wrapper.MinMaxEntity;
import it.sister.statportal.odata.wrapper.TotalEntity;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.joda.time.DateTime;
import org.odata4j.core.OComplexObject;
import org.odata4j.core.OComplexObjects;
import org.odata4j.core.OEntities;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OFunctionParameter;
import org.odata4j.core.OLink;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmAssociation;
import org.odata4j.edm.EdmAssociationEnd;
import org.odata4j.edm.EdmAssociationSet;
import org.odata4j.edm.EdmAssociationSetEnd;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntityContainer;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmFunctionParameter;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.edm.EdmEntityType.Builder;
import org.odata4j.edm.EdmFunctionImport;
import org.odata4j.edm.EdmMultiplicity;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.edm.EdmSchema;
import org.odata4j.expression.OrderByExpression;
import org.odata4j.producer.BaseResponse;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.EntityIdResponse;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.ODataProducer;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.Responses;
import org.odata4j.producer.edm.MetadataProducer;
import org.odata4j.producer.exceptions.NotImplementedException;

import com.google.gson.Gson;
import com.google.gson.internal.StringMap;

/**
 * Producer utilizzato dal proxy per mascherare gli id del servizio sottostante
 *
 */
public class StatPortalProxyProducer implements ODataProducer{

	private static String namespace = "http://odata.statportal.sister.it";
	
	private static EdmDataServices edmDataServices = null;

	protected String serviceUrl;

	protected String myName;
	
	/**
	 * Costruisce il producer del proxy
	 * @param myName nome del dominio
	 * @param serviceUrl url del servizio sottostante
	 */
	public StatPortalProxyProducer(String myName, String serviceUrl){
		StatPortalProxyProducer.buildMetadata();
		this.myName = myName;
		this.serviceUrl = serviceUrl;
	}
	
	/**
	 * Metodo statico per la generazione dei metadati (entità, proprietà, associazioni, etc.).
	 * Per ora creiamo i metadati a mano, successivamente implementeremo la lettura dei metadati da un file xml di configurazione
	 */
	private static void buildMetadata() {
		
		List<EdmEntitySet.Builder> entitySets = new ArrayList<EdmEntitySet.Builder>();
		List<EdmEntityType.Builder> entityTypes = new ArrayList<EdmEntityType.Builder>();
		List<EdmComplexType.Builder> complexTypes = new ArrayList<EdmComplexType.Builder>();
		List<EdmAssociation.Builder> associations = new ArrayList<EdmAssociation.Builder>();
		List<EdmAssociationSet.Builder> associationSets = new ArrayList<EdmAssociationSet.Builder>();
	
		//Complex types per l'integrazione del vecchio ODataWS nel proxy
		//FileUrl per l'esportazione in formato CSV e KML di un dato
		final List<EdmProperty.Builder> fileUrlProperties = new ArrayList<EdmProperty.Builder>();
		fileUrlProperties.add(EdmProperty.newBuilder("url").setType(EdmSimpleType.STRING));
		EdmComplexType.Builder fileUrlType = EdmComplexType.newBuilder().setName(namespace).setName("FileUrl").addProperties(fileUrlProperties);
		complexTypes.add(fileUrlType);
		
		final List<EdmProperty.Builder> fileContentProperties = new ArrayList<EdmProperty.Builder>();
		fileContentProperties.add(EdmProperty.newBuilder("content").setType(EdmSimpleType.STRING));
		EdmComplexType.Builder fileContentType = EdmComplexType.newBuilder().setName(namespace).setName("FileContent").addProperties(fileContentProperties);
		complexTypes.add(fileContentType);
	
		//LayerInfo per la creazione della tematizzazione e dello shape file
		EdmComplexType.Builder layerInfoType = LayerInfoWrapper.getTypeDefinition(namespace);
		complexTypes.add(layerInfoType);
		
		EdmComplexType.Builder dataDescriptionType = DataDescriptionWrapper.getTypeDefinition(namespace);
		complexTypes.add(dataDescriptionType);
		
		List<EdmFunctionImport.Builder> functionImports = new ArrayList<EdmFunctionImport.Builder>();
		List<EdmFunctionParameter.Builder> exportCsvParameters = new ArrayList<EdmFunctionParameter.Builder>();
		exportCsvParameters.add(EdmFunctionParameter.newBuilder().setName("uid").setType(EdmSimpleType.STRING));
		exportCsvParameters.add(EdmFunctionParameter.newBuilder().setName("lang").setType(EdmSimpleType.STRING));
		functionImports.add(EdmFunctionImport.newBuilder().setName("ExportCSV").setReturnType(fileUrlType).addParameters(exportCsvParameters));
		List<EdmFunctionParameter.Builder> exportKmlParameters = new ArrayList<EdmFunctionParameter.Builder>();
		exportKmlParameters.add(EdmFunctionParameter.newBuilder().setName("uid").setType(EdmSimpleType.STRING));
		functionImports.add(EdmFunctionImport.newBuilder().setName("ExportKML").setReturnType(fileUrlType).addParameters(exportKmlParameters));
		
		//Esportazione dei dati in formato RDF
		List<EdmFunctionParameter.Builder> exportRdfParameters = new ArrayList<EdmFunctionParameter.Builder>();
		exportRdfParameters.add(EdmFunctionParameter.newBuilder().setName("uid").setType(EdmSimpleType.STRING));
		exportRdfParameters.add(EdmFunctionParameter.newBuilder().setName("offset").setType(EdmSimpleType.INT32));
		exportRdfParameters.add(EdmFunctionParameter.newBuilder().setName("limit").setType(EdmSimpleType.INT32));
		exportRdfParameters.add(EdmFunctionParameter.newBuilder().setName("outformat").setType(EdmSimpleType.STRING));
		functionImports.add(EdmFunctionImport.newBuilder().setName("ExportRDF").setReturnType(fileUrlType).addParameters(exportRdfParameters));		
		
		List<EdmFunctionParameter.Builder> createThematizationLayerParameters = new ArrayList<EdmFunctionParameter.Builder>();
		createThematizationLayerParameters.add(EdmFunctionParameter.newBuilder().setName("dataUid").setType(EdmSimpleType.STRING));
		createThematizationLayerParameters.add(EdmFunctionParameter.newBuilder().setName("territorialDimensionUid").setType(EdmSimpleType.STRING));
		createThematizationLayerParameters.add(EdmFunctionParameter.newBuilder().setName("temporalDimensionUid").setType(EdmSimpleType.STRING));
		createThematizationLayerParameters.add(EdmFunctionParameter.newBuilder().setName("temporalDimensionValue").setType(EdmSimpleType.STRING));
		functionImports.add(EdmFunctionImport.newBuilder().setName("CreateThematizationLayer").setReturnType(layerInfoType).addParameters(createThematizationLayerParameters));
		List<EdmFunctionParameter.Builder> createShapeLayerParameters = new ArrayList<EdmFunctionParameter.Builder>();
		createShapeLayerParameters.add(EdmFunctionParameter.newBuilder().setName("dataUid").setType(EdmSimpleType.STRING));
		functionImports.add(EdmFunctionImport.newBuilder().setName("CreateShapeLayer").setReturnType(layerInfoType).addParameters(createShapeLayerParameters));
		List<EdmFunctionParameter.Builder> downloadContentParameters = new ArrayList<EdmFunctionParameter.Builder>();
		downloadContentParameters.add(EdmFunctionParameter.newBuilder().setName("url").setType(EdmSimpleType.STRING));
		functionImports.add(EdmFunctionImport.newBuilder().setName("DownloadContent").setReturnType(EdmSimpleType.STRING).addParameters(downloadContentParameters));
		List<EdmFunctionParameter.Builder> dataDescriptionParameters = new ArrayList<EdmFunctionParameter.Builder>();
		dataDescriptionParameters.add(EdmFunctionParameter.newBuilder().setName("dataUid").setType(EdmSimpleType.STRING));
		functionImports.add(EdmFunctionImport.newBuilder().setName("DataDescription").setReturnType(dataDescriptionType).addParameters(dataDescriptionParameters));
		//Entity set del servizio OpenData sottostante
		EdmEntityType.Builder rowType = EdmEntityType.newBuilder().setNamespace(namespace).setName("Row");
		EdmEntitySet.Builder rowEntitySet = EdmEntitySet.newBuilder().setName("Rows").setEntityType(rowType);
		
		EdmEntityType.Builder columnType = ColumnWrapper.getEntityModel(namespace);
		entityTypes.add(columnType);
		EdmEntitySet.Builder columnEntitySet = EdmEntitySet.newBuilder().setName("DataColumns").setEntityType(columnType);
		entitySets.add(columnEntitySet);
		
		EdmEntityType.Builder minMaxType = MinMaxEntity.getEntityModel(namespace);
		entityTypes.add(minMaxType);
		EdmEntitySet.Builder minMaxEntitySet = EdmEntitySet.newBuilder().setName("MinMax").setEntityType(minMaxType);
		entitySets.add(minMaxEntitySet);
		
		EdmEntityType.Builder distinctCountRowType = DistinctCountRowWrapper.getEntityModel(namespace);
		entityTypes.add(distinctCountRowType);
		EdmEntitySet.Builder distinctCountRowEntitySet = EdmEntitySet.newBuilder().setName("DistinctCountRows").setEntityType(distinctCountRowType);
		entitySets.add(distinctCountRowEntitySet);
		
		EdmEntityType.Builder genericColumnAggregationRowType = GenericColumnAggregationRowWrapper.getEntityModel(namespace);
		entityTypes.add(genericColumnAggregationRowType);
		EdmEntitySet.Builder genericColumnAggregationRowEntitySet = EdmEntitySet.newBuilder().setName("GenericColumnAggregationRows").setEntityType(genericColumnAggregationRowType);
		entitySets.add(genericColumnAggregationRowEntitySet);
		
		EdmEntityType.Builder mdDataType = MdDataWrapper.getEntityModelForProxy(namespace);
		entityTypes.add(mdDataType);
		EdmEntitySet.Builder mdDataEntitySet = EdmEntitySet.newBuilder().setName("MdData").setEntityType(mdDataType);
		entitySets.add(mdDataEntitySet);
		
		EdmEntityType.Builder mdDataDimType = MdDataDimWrapper.getEntityModelForProxy(namespace);
		entityTypes.add(mdDataDimType);
		EdmEntitySet.Builder mdDataDimEntitySet = EdmEntitySet.newBuilder().setName("MdDataDim").setEntityType(mdDataDimType);	
		entitySets.add(mdDataDimEntitySet);
		
		EdmEntityType.Builder mdMeasureFieldsType = MdMeasureFieldsWrapper.getEntityModelForProxy(namespace);
		entityTypes.add(mdMeasureFieldsType);
		EdmEntitySet.Builder mdMeasureFieldsEntitySet = EdmEntitySet.newBuilder().setName("MdMeasureFields").setEntityType(mdMeasureFieldsType);	
		entitySets.add(mdMeasureFieldsEntitySet);
		
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
		associations.add(mdDataGenericColumnAggregationRow);
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

		EdmEntityContainer.Builder container = EdmEntityContainer.newBuilder().setName(namespace + "/Entities").setIsDefault(true).addEntitySets( entitySets).addFunctionImports(functionImports);
		EdmSchema.Builder modelSchema = EdmSchema.newBuilder().setNamespace(namespace + "/Model").addEntityTypes(entityTypes).addComplexTypes(complexTypes);
		EdmSchema.Builder containerSchema = EdmSchema.newBuilder().setNamespace(namespace + "/Container").addEntityContainers(container);

		edmDataServices = EdmDataServices.newBuilder().addSchemas(containerSchema, modelSchema).build();

	}

	@Override
	public BaseResponse callFunction(EdmFunctionImport function,
			Map<String, OFunctionParameter> parameters, QueryInfo queryInfo) {
		String functionName = function.getName();
		if(functionName.equals("ExportCSV")){
			return exportCSV(function, parameters, queryInfo);
		} else if(functionName.equals("ExportKML")){
			return exportKML(function, parameters, queryInfo);
		} else if(functionName.equals("ExportRDF")){
			return exportRDF(function, parameters, queryInfo);
		} else if(functionName.equals("DownloadContent")){
			return downloadContent((String)parameters.get("url").getValue().toString());		
		}else if(functionName.equals("CreateThematizationLayer")){
			return createThematizationLayer(function, parameters, queryInfo);
		}else if(functionName.equals("CreateShapeLayer")){
			return createShapeLayer(function, parameters, queryInfo);
		}else if(functionName.equals("DataDescription")){
			return dataDescription(function, parameters, queryInfo);
		}else{
			throw new NotImplementedException();
		}
	}

	/**
	 * Scarica il contenuto di un url
	 * @param url l'url da contattare
	 * @return una base response creata usando il contenuto dell'url
	 */
	private BaseResponse downloadContent(String url){
		String csvUrl = StatPortalODataProxy.readConfig("csvurl");
		String kmlUrl = StatPortalODataProxy.readConfig("kmlurl");
		if(!url.startsWith(csvUrl) && !url.startsWith(kmlUrl)){
			return null;
		}
		StringBuilder resultBuilder = new StringBuilder();
		URL fileUrl;
		BufferedReader in = null;
		try {
			fileUrl = new URL(url);
			in = new BufferedReader(new InputStreamReader(fileUrl.openStream()));
		    String inputLine;
		    while ((inputLine = in.readLine()) != null){
		    	resultBuilder.append(inputLine);
		    	resultBuilder.append("\r\n");
		    }
		} catch (MalformedURLException e) {
			StatPortalODataProxy.Log(e);
			return null;
		} catch (IOException e) {
			StatPortalODataProxy.Log(e);
			return null;
		} finally{
			if(in != null){
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		properties.add(OProperties.string("content", resultBuilder.toString()));
		return Responses.complexObject(OComplexObjects.create(getEdmComplexType("FileContent"), properties));
	}

	/**
	 * Estrae le informazioni su un dato
	 * @param function la service operation chiamata
	 * @param parameters i parametri della service operation
	 * @param queryInfo eventuali restrizioni
	 */
	private BaseResponse dataDescription(EdmFunctionImport function, Map<String, OFunctionParameter> parameters, QueryInfo queryInfo){
		try {
			String dataUid = (String)parameters.get("dataUid").getValue().toString();
			String[] splittedKey = splitKey(dataUid, false);
			if(splittedKey[1].equals(myName)){
				return dataDescriptionLocal(splittedKey[0]);
			}else{
				return dataDescriptionFederation(splittedKey[1], function, parameters, queryInfo);
			}
		} catch (Throwable t) {
			StatPortalODataProxy.Log(t);
			return null;
		}
	}
	
	/**
	 * Estra le informazioni su un dato che si trova in locale
	 * @param uid uid del dato
	 * @return una risposta odata4j con incapsulata la descrizione del dato
	 */
	private BaseResponse dataDescriptionLocal(String uid){
		Integer mdDataIdInt = MdData.getIdFromUid(uid);
		MdData data = MdData.findMdData(mdDataIdInt.intValue());
		String dataDescription = "";
		try {
			dataDescription = data.getDataDescription(false);
		} catch (OdataDomainException e) {
			StatPortalODataProxy.Log(e);
		}
		return Responses.complexObject(DataDescriptionWrapper.createInstance(getEdmComplexType("DataDescription"), dataDescription));
	}
	
	/**
	 * Estrae le informazioni su un dato che si trova su un'altra istanza federata
	 * @param name nome dell'istanza
	 * @param function service operation da richiamare
	 * @param parameters parametri della service operation
	 * @param queryInfo eventuali restrizioni
	 */
	private BaseResponse dataDescriptionFederation(String name, EdmFunctionImport function,
			Map<String, OFunctionParameter> parameters, QueryInfo queryInfo) {
		String requestUrl = buildServiceOperationUrl(name, function, parameters, queryInfo);
		StringMap<?> deserialized = readJsonFromUrl(requestUrl);
		OComplexObject complexObject = DataDescriptionWrapper.createInstance(getEdmComplexType("DataDescription"), deserialized);
		return Responses.complexObject(complexObject);
	}
	
	/**
	 * Crea un layer di geoserver
	 * @param function la service operation chiamata
	 * @param parameters i parametri della service operation
	 * @param queryInfo eventuali restrizioni
	 */
	private BaseResponse createShapeLayer(EdmFunctionImport function,
			Map<String, OFunctionParameter> parameters, QueryInfo queryInfo) {
		try {
			String dataUid = (String)parameters.get("dataUid").getValue().toString();
			String[] splittedKey = splitKey(dataUid, false);
			if(splittedKey[1].equals(myName)){
				return createShapeLayerLocal(splittedKey[0]);
			}else{
				return createShapeLayerFederation(splittedKey[1], function, parameters, queryInfo);
			}
		} catch (Throwable t) {
			StatPortalODataProxy.Log(t);
			return null;
		}
	}

	/**
	 * Crea un layer di geoserver girando la richiesta ad un'altra istanza federata
	 * @param name nome dell'istanza
	 * @param function la service operation
	 * @param parameters i parametri della service operation
	 * @param queryInfo eventuali restrizioni
	 */
	private BaseResponse createShapeLayerFederation(String name, EdmFunctionImport function,
			Map<String, OFunctionParameter> parameters, QueryInfo queryInfo) {
		String requestUrl = buildServiceOperationUrl(name, function, parameters, queryInfo);
		StringMap<?> deserialized = readJsonFromUrl(requestUrl);
		OComplexObject complexObject = LayerInfoWrapper.createInstance(getEdmComplexType("LayerInfo"), deserialized);
		return Responses.complexObject(complexObject);
	}

	/**
	 * Crea un layer di geoserver basandosi su un dato che si trova nell'istanza locale
	 * @param uid l'uid del dato
	 */
	private BaseResponse createShapeLayerLocal(String uid) {
		Integer mdDataIdInt = MdData.getIdFromUid(uid);
		MdData data = MdData.findMdData(mdDataIdInt.intValue());
		
		String geoServerUrl = StatPortalODataProxy.readConfig("geoServerUrl");
		String geoServerRestUrl = StatPortalODataProxy.readConfig("geoServerRestUrl");
		String user = "restuser";
		String password = "restpwd";

		// PUBLISH
		String workspaceName = StatPortalODataProxy.readConfig("workspaceName");
		String storeName = StatPortalODataProxy.readConfig("storeName");
		
		String method = "publish";
		String parameters = workspaceName + "," + storeName + "," + data.getTableName() + "," + "shape_" + data.getId().toString();
		String ret = getUrl(geoServerRestUrl + method + "?" + parameters, user, password);

		return Responses.complexObject(LayerInfoWrapper.createInstance(getEdmComplexType("LayerInfo"), geoServerUrl, geoServerRestUrl, workspaceName, ret));
	}
	
	/**
	 * Effettua una get dell'url con autenticazione
	 * @param url l'url di cui fare la get
	 * @param user l'utente
	 * @param password la password
	 * @param il risultato della get dell'url
	 */
	private static String getUrl(String url, String user, String password) {
		StatPortalODataProxy.logger.info("Chiamata alla GetUrl");
		String retVal = "";
		URL u;
		HttpURLConnection uc;

		try {
			u = new URL(url);
			try {
				uc = (HttpURLConnection)u.openConnection();
				uc.setRequestMethod("GET");
				uc.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
				uc.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
				uc.setRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
				if (user != null) {
					uc.setRequestProperty("Authorization", "Basic "+ encode(user + ":" + password));
				}
				InputStream content = (InputStream) uc.getInputStream();
				byte[] bytes = new byte[uc.getContentLength()];
				org.apache.axis.utils.IOUtils.readFully(content, bytes);
				ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
				//GZIPInputStream gzis = new GZIPInputStream(bais);
//				InputStreamReader reader = new InputStreamReader(gzis);
				InputStreamReader reader = new InputStreamReader(bais);
				BufferedReader boh = new BufferedReader(reader);
				
				String tmp = "";
				while ((tmp = boh.readLine()) != null){
					retVal+= tmp;
				}
				
			} catch (Exception e) {
				StatPortalODataProxy.Log(e);
				return "";
			}
		} catch (Exception e) {
			StatPortalODataProxy.Log(e);
			return null;
		}
		StatPortalODataProxy.logger.info("Restituisco risultato: "+retVal);
		return retVal;
	}
	
	/**
	 * Codifica il parametro usando un BASE64Encoder
	 * @param source la stringa da codificare
	 * @return il parametro codificato
	 */
	private static String encode(String source) {
		sun.misc.BASE64Encoder enc = new sun.misc.BASE64Encoder();
		return (enc.encode(source.getBytes()));
	}

	/**
	 * Crea un layer tematizzato di geoserver
	 * @param function la service operation chiamata
	 * @param parameters i parametri della service operation
	 * @param queryInfo eventuali restrizioni
	 * @return il nome di un layer geoserver
	 */
	private BaseResponse createThematizationLayer(EdmFunctionImport function,
			Map<String, OFunctionParameter> parameters, QueryInfo queryInfo) {
		try {
			String dataUid = (String)parameters.get("dataUid").getValue().toString();
			String territorialDimensionUid = (String)parameters.get("territorialDimensionUid").getValue().toString();
			String temporalDimensionUid = (String)parameters.get("temporalDimensionUid").getValue().toString();
			String temporalDimensionValue = (String)parameters.get("temporalDimensionValue").getValue().toString();
			String[] splittedDataUid = splitKey(dataUid, false);
			if(splittedDataUid[1].equals(myName)){
				return createThematizationLayerLocal(territorialDimensionUid,
						temporalDimensionUid, temporalDimensionValue,
						splittedDataUid[0]);
			}else{
				return createThematizationLayerFederation(splittedDataUid[1], function, parameters, queryInfo);				
			}
		} catch (Throwable t) {
			StatPortalODataProxy.Log(t);
			return null;
		}
	}

	/**
	 * Crea un layer geoserver girando la richiesta ad un'istanza federata
	 * @param name nome dell'istanza
	 * @param function service operation chiamata
	 * @param parameters parametri della service operation
	 * @param queryInfo eventuali restrizioni
	 * @return un layer geoserver
	 */
	private BaseResponse createThematizationLayerFederation(String name, EdmFunctionImport function,
			Map<String, OFunctionParameter> parameters, QueryInfo queryInfo) {
		String requestUrl = buildServiceOperationUrl(name, function, parameters, queryInfo);
		StringMap<?> deserialized = readJsonFromUrl(requestUrl);
		OComplexObject complexObject = LayerInfoWrapper.createInstance(getEdmComplexType("LayerInfo"), deserialized);
		return Responses.complexObject(complexObject);
	}

	/**
	 * Crea un layer geoserver tematizzato 
	 * @param territorialDimensionUid uid della dimensione territoriale
	 * @param temporalDimensionUid uid della dimensione temporale
	 * @param temporalDimensionValue valore della dimensione temporale
	 * @param dataUid uid del dato
	 * @return un layer geoserver tematizzato
	 * @throws Exception
	 */
	private BaseResponse createThematizationLayerLocal(
			String territorialDimensionUid, String temporalDimensionUid,
			String temporalDimensionValue, String dataUid)
			throws Exception {
		String ret = "";
		MdDataDim temporalDim = null;			
		Integer mdDataIdInt = null;
		Integer mdIdDataDimension = null;
		Integer mdIdTemporalDimension = null;
		mdDataIdInt = MdData.getIdFromUid(dataUid);
		String[] splittedTerritorialDimUid = splitKey(territorialDimensionUid, false);
		mdIdDataDimension = MdDataDim.getIdFromUid(splittedTerritorialDimUid[0]);
		try{
			String[] splittedTemportalDimUid = splitKey(temporalDimensionUid, false);
			mdIdTemporalDimension = MdDataDim.getIdFromUid(splittedTemportalDimUid[0]);
			temporalDim = MdDataDim.findMdDataDim(mdIdTemporalDimension);
			if(temporalDimensionValue == null || temporalDimensionValue.isEmpty()){
				temporalDim = null;
				temporalDimensionValue = null;
			}
		}catch(Exception e){
			temporalDim = null;
		}
		MdData data = MdData.findMdData(mdDataIdInt.intValue());					
		MdDataDim dim = MdDataDim.findMdDataDim(mdIdDataDimension);					
		String viewName = data.createViewForMap(dim, temporalDim, temporalDimensionValue);
		String geoServerUrl = "";
		String geoServerRestUrl = "";
		String workspaceName = "";
		if (viewName != null) {
			// JOIN
			geoServerUrl = StatPortalODataProxy.readConfig("geoServerUrl");
			geoServerRestUrl = StatPortalODataProxy.readConfig("geoServerRestUrl");
			String user = "restuser";
			String password = "restpwd";
			String method = "jointable";
			
			String storeName = StatPortalODataProxy.readConfig("storeName");
			workspaceName = StatPortalODataProxy.readConfig("workspaceName");
			
			String parameters = workspaceName + "," + storeName + "," + dim.getMapLayerName() + "," + viewName
			 + "," + dim.getMapLayerField() + ","
			 + dim.getDimcodeField().toLowerCase();

			String joinResponse = getUrl(geoServerRestUrl + method + "?"
					+ parameters, user, password);
			
			if (joinResponse != null
					&& !joinResponse.equals("")
					&& !joinResponse
							.equals("<JoinTable><ViewName></ViewName></JoinTable>")) {

				// si recupera il nome della vista creata
				String start = "<ViewName>";
				String end = "</ViewName>";
				String joinResponseParsed = joinResponse.substring(
						joinResponse.indexOf(start) + start.length(),
						joinResponse.indexOf(end));

				// PUBLISH
				
				method = "publish";
				parameters = workspaceName + "," + storeName + "," + joinResponseParsed + "," + viewName + "_p";
				ret = getUrl(geoServerRestUrl + method + "?" + parameters, user,
						password);
			}
		}
		return Responses.complexObject(LayerInfoWrapper.createInstance(getEdmComplexType("LayerInfo"), geoServerUrl, geoServerRestUrl, workspaceName, ret));
	}

	/**
	 * Scrive un file
	 * @param content contenuto da scrivere nel file
	 * @param file file da creare
	 * si lascia invariato il file.
	 * @throws IOException
	 */
	private void writeFile(String content, File file) throws IOException{
		BufferedWriter fw = null;
		try {
			fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "ISO-8859-1"));
			file.createNewFile();
			//fw = new FileWriter(file);
			fw.write(content);
		} catch (IOException e) {
			throw e;
		} finally{
			if(fw != null){
				try {
					fw.close();
				} catch (IOException e) {
					StatPortalODataProxy.Log(e);
				}
			}
		}
	}
	
	/**
	 * Esporta un dato in formato RDF
	 * @param function la funzione ExportRDF
	 * @param parameters i parametri (uid e outformat)
	 * @param queryInfo informazioni usate per il redirect nel caso di esportazione in un contesto di federazione
	 * @return l'indirizzo di un file che contiene l'esportazione di un dato in formato RDF
	 */
	private BaseResponse exportRDF(EdmFunctionImport function, Map<String, OFunctionParameter> parameters, QueryInfo queryInfo) {
		try {
			String uid = (String)parameters.get("uid").getValue().toString();
			String outformat = (String)parameters.get("outformat").getValue().toString();
			String[] splittedKey = splitKey(uid, false);
			if(splittedKey[1].equals(myName)){
				return exportRDFLocal(splittedKey[0], outformat);
			}else{
				return exportRDFFederation(splittedKey[1], function, parameters, queryInfo);
			}
		}catch(Throwable ex){
			StatPortalODataProxy.Log(ex);
			return null;
		}
	}

	/**
	 * Esporta una parte di un dato in formato RDF 
	 * @param uid l'uid del dato da esportare
	 * @param outformat il formato di RDF di output
	 * @return l'indirizzo di un file contenente l'esportazione di una parte di un dato in formato RDF
	 * @throws Exception 
	 */
	private BaseResponse exportRDFLocal(String uid, String outformat) throws Exception {
		Integer mdDataIdInt = MdData.getIdFromUid(uid);
		MdData data = MdData.findMdData(mdDataIdInt.intValue());
		String tableName = data.getTableName();
		//creo la vista usando nome tabella
		if(mustCreateView(data)){
			data.createViewWithSerialUID("view_"+tableName, false, "do_serialuid");
			tableName = "view_"+tableName;
		}
		//creo il mapping
		String rdfDir = StatPortalODataProxy.readConfig("rdfDir");
		String rdfUrl = StatPortalODataProxy.readConfig("rdfUrl");
		String d2rqServer = StatPortalODataProxy.readConfig("d2rqServer");
		long dirSizeBestBelow = 1000000000;
		String dirSizeBestBelowKey = StatPortalODataProxy.readConfig("dirSizeBestBelow");
		if(dirSizeBestBelowKey != null){
			try{
				dirSizeBestBelow = Long.parseLong(dirSizeBestBelowKey);
			}catch(NumberFormatException nfe){
				dirSizeBestBelow = 1000000000;
			}
		}
		long maxAllowedDirSize = 10000000000L;
		String maxAllowedDirSizeKey = StatPortalODataProxy.readConfig("maxAllowedDirSize");
		if(maxAllowedDirSizeKey != null){
			try{
				maxAllowedDirSize = Long.parseLong(maxAllowedDirSizeKey);
			}catch(NumberFormatException nfe){
				maxAllowedDirSize = 10000000000L;
			}
		}
		String hoursBeforeDeletableKey = StatPortalODataProxy.readConfig("hoursBeforeDeletable");
		int hoursBeforeDeletable = 1;
		if(hoursBeforeDeletableKey != null){
			try{
				hoursBeforeDeletable = Integer.parseInt(hoursBeforeDeletableKey);
			}catch(NumberFormatException nfe){
				hoursBeforeDeletable = 1;
			}
		}
		String zipFilePath = rdfDir+"/"+uid+".zip";
		File zipFile = new File(zipFilePath);
		if(!zipFile.exists() || data.getLastUpdate().getTime() > zipFile.lastModified()){
			RdfFormatter formatter = null;
			if(outformat.equalsIgnoreCase("N3")){
				throw new Exception("Formato N3 non supportato");
			}else if(outformat.equalsIgnoreCase("TURTLE")){
				throw new Exception("Formato Turtle non supportato");
			}else if(outformat.equalsIgnoreCase("RDF/XML") || outformat.equalsIgnoreCase("RDF/XML-ABBREV")){
				formatter = new XmlRdfFormatter();
			}else if(outformat.equalsIgnoreCase("N-TRIPLE")){
				throw new Exception("Formato N-TRIPLE non supportato");
			}
			RdfWriter writer = new RdfWriter(formatter);
			File dumpFile = writer.writeFile(data, new File(rdfDir), d2rqServer);		
			zipFile.delete();
			zipFile(dumpFile, zipFile);
			dumpFile.delete();
			//controllo se devo ripulire la cartella
			File exportDirectory = new File(rdfDir);
			long currentSize = 0;
			for(File file : exportDirectory.listFiles()){
				currentSize += file.length();
			}			
			//se la dimensione corrente è maggiore del limite che abbiamo impostato eliminiamo dei file (se possibile)
			if(currentSize > dirSizeBestBelow){
				resizeDirectory(exportDirectory, currentSize, dirSizeBestBelow, hoursBeforeDeletable, maxAllowedDirSize);
			}
		}
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		properties.add(OProperties.string("url", rdfUrl+"/"+uid+".zip"));
		return Responses.complexObject(OComplexObjects.create(getEdmComplexType("FileUrl"), properties));
	}
	
	/**
	 * Crea un file zip comprimendo un file.
	 * @param toZip il file da comprimere
	 * @param targetFile il file risultante
	 * @throws IOException 
	 */
	private void zipFile(File toZip, File targetFile) throws IOException{
		if(!targetFile.exists()){
			targetFile.createNewFile();
		}
		BufferedInputStream bis = null;
		ZipOutputStream zos = null;
		try{
			zos = new ZipOutputStream(new FileOutputStream(targetFile));
			zos.putNextEntry(new ZipEntry(toZip.getName()));		 
		    bis = new BufferedInputStream(new FileInputStream(toZip));	 
		    byte[] bytesIn = new byte[4096];
		    int read = 0;	 
		    while ((read = bis.read(bytesIn)) != -1) {
		        zos.write(bytesIn, 0, read);
		    }	 
		    zos.closeEntry();
	    }catch(IOException ioe){
	    	StatPortalODataProxy.Log(ioe);	
	    	throw ioe;
	    }finally{
	    	if(bis != null){
	    		try {
					bis.close();
				} catch (IOException e) {
					StatPortalODataProxy.Log(e);
				}
	    	}
	    	if(zos != null){
	    		try {
					zos.close();
				} catch (IOException e) {
					StatPortalODataProxy.Log(e);
				}
	    	}
	    }
	}
	
	/**
	 * Elimina da una directory i file la cui data di modifica è più vecchia dell'orario corrente meno un certo numero di ore.
	 * Li elimina a partire dal più vecchio e continua fino a quando la dimensione della cartella non 
	 * scende al di sotto della soglia specificata
	 * @param directory la directory da cui eliminare i file
	 * @param currentSize la dimensione corrente della cartella
	 * @param dirSizeBestBelow la dimensione massima desiderata
	 * @param hoursBeforeDeletable numero di ore dall'ultima modifica prima che un file sia cancellabile
	 * @param maxAllowedDirSize la dimensione massima consentita. Superata questa soglia si invalida il numero di ore dall'ultima modifica 
	 * per rendere cancellabile un file.
	 */
	private void resizeDirectory(File directory, long currentSize, long dirSizeBestBelow, int hoursBeforeDeletable, long maxAllowedDirSize){
		DateTime currentTime = new DateTime();
		//se la dimensione della cartella è andata oltre il massimo consentito allora si azzera il numero di ore prima di rendere
		//cancellabile un dato
		if(currentSize > maxAllowedDirSize){
			hoursBeforeDeletable = 0;
		}
		//non cancellare file con data di modifica successiva a questa data
		DateTime notAfter = currentTime.minusHours(hoursBeforeDeletable);
		//creo l'elenco dei file più vecchi di quella data e lo ordino dal più vecchio al più recente
		File[] deletableFiles = directory.listFiles(new DateTimeFileFilter(notAfter));
		Arrays.sort(deletableFiles, new Comparator<File>() {
		    public int compare(File first, File second) {
		    	if(first.lastModified() <= second.lastModified()){
		    		return -1;
		    	}else{
		    		return 1;
		    	}
		    }
		} );
		//scorro i file finchè non finisco l'array o la dimensione della cartella non scende sotto la soglia
		int index = 0;
		long sizeToReach = dirSizeBestBelow;
		if(currentSize > maxAllowedDirSize){
			//in questo caso elimino solo i file per tamponare l'emergenza, non cerco di tornare subito sotto la soglia desiderata
			sizeToReach = maxAllowedDirSize;
		}
		while(index < deletableFiles.length && currentSize > sizeToReach){
			//elimino il file
			currentSize -= deletableFiles[index].length();
			deletableFiles[index].delete();
			index++;
		}
	}
	
	/**
	 * Controlla se è necessario creare una vista per poter fare l'export rdf
	 * è necessario creare la vista se:
	 * 1) non esiste una chiave primaria
	 * 2) tra le colonne è presente la colonna the_geom
	 * @param data il dato
	 * @return true se bisogna creare la vista, false altrimenti
	 * @throws OdataDomainException
	 */
	private boolean mustCreateView(MdData data) throws OdataDomainException{
		if(!data.pkEsists()){
			return true;
		}
		List<Column> columns = data.getColumns();
		for(Column column : columns){
			if(column.getPhysicalName().equalsIgnoreCase("the_geom")){
				return true;
			}
		}
		return false;
	}

	/**
	 * Redirige la chiamata di esportazione in formato RDF al proxy dell'entità proprietaria del dato
	 * @param name nome dell'entità proprietaria del dato
	 * @param function funzione da chiamare nel proxy
	 * @param parameters parametri della funzione
	 * @param queryInfo parametri della query
	 * @return l'url di un file contenente l'export RDF di una parte di un dato
	 */
	private BaseResponse exportRDFFederation(String name, EdmFunctionImport function, Map<String, OFunctionParameter> parameters, QueryInfo queryInfo) {
		String requestUrl = buildServiceOperationUrl(name, function, parameters, queryInfo);
		StringMap<?> deserialized = readJsonFromUrl(requestUrl);
		StringMap<?> d = (StringMap<?>)deserialized.get("d");
		String fileUrl = (String) d.get("url");
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		properties.add(OProperties.string("url", fileUrl));
		OComplexObject complexObject = OComplexObjects.create(getEdmComplexType("FileUrl"), properties);
		return Responses.complexObject(complexObject);
	}
	
	/**
	 * Esporta un dato in formato kml
	 * @param function la service operation chiamata
	 * @param parameters i parametri della service operation
	 * @param queryInfo eventuali restrizioni
	 * @return l'esportazione di un dato in formato kml
	 */
	private BaseResponse exportKML(EdmFunctionImport function,
			Map<String, OFunctionParameter> parameters, QueryInfo queryInfo) {
		
		try {
			String uid = (String)parameters.get("uid").getValue().toString();
			String[] splittedKey = splitKey(uid, false);
			if(splittedKey[1].equals(myName)){
				return exportKMLLocal(splittedKey[0]);
			}else{
				return exportKMLFederation(splittedKey[1], function, parameters, queryInfo);
			}
		}catch(Throwable ex){
			StatPortalODataProxy.Log(ex);
			return null;
		}
	}

	/**
	 * Esporta un dato in formato kml girando la richiesta ad un'entità federata
	 * @param name il nome dell'entità
	 * @param function la service operation da chiamare
	 * @param parameters i parametri della service operation
	 * @param queryInfo eventuali restrizioni
	 * @return l'esportazione kml di un dato
	 */
	private BaseResponse exportKMLFederation(String name, EdmFunctionImport function,
			Map<String, OFunctionParameter> parameters, QueryInfo queryInfo) {
		String requestUrl = buildServiceOperationUrl(name, function, parameters, queryInfo);
		StringMap<?> deserialized = readJsonFromUrl(requestUrl);
		StringMap<?> d = (StringMap<?>)deserialized.get("d");
		String fileUrl = (String) d.get("url");
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		properties.add(OProperties.string("url", fileUrl));
		OComplexObject complexObject = OComplexObjects.create(getEdmComplexType("FileUrl"), properties);
		return Responses.complexObject(complexObject);
	}

	/**
	 * Esporta un dato in formato kml
	 * @param uid l'uid del dato da esportare
	 * @return l'esportazione di un dato in formato kml
	 * @throws IOException
	 */
	private BaseResponse exportKMLLocal(String uid)
			throws IOException {
		Integer mdDataIdInt = MdData.getIdFromUid(uid);
		MdData data = MdData.findMdData(mdDataIdInt.intValue());
		boolean isKmz = false;
		if(data.getIdLuDataType() == 4){
			String filePath = "";
			for(MdDataFiles dataFile : data.getMdDataFileses()){
				filePath = dataFile.getId().getFileUrl();
				break;
			}
			File kmlFile = new File(filePath);
			isKmz = filePath.toUpperCase().endsWith("KMZ");
			String path = StatPortalODataProxy.readConfig("kmldir")+"/"+uid+ (isKmz ? ".kmz":".kml");
			copyFile(kmlFile, path);
		}else{
			File file = new File(StatPortalODataProxy.readConfig("kmldir")+"/"+uid+".kml");
			if(!file.exists() || file.lastModified() <= data.getLastUpdate().getTime()){
				//se il file esiste già ed è stato creato dopo l'ultima modifica del dato allora non lo rigenero altrimenti cancello quello esistente
				if(file.exists()){
					file.delete();
				}
				String kml = data.getKml(); 
				writeFile(kml, file);
			}			
		}
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		properties.add(OProperties.string("url", StatPortalODataProxy.readConfig("kmlurl")+"/"+uid+(isKmz? ".kmz" : ".kml")));
		return Responses.complexObject(OComplexObjects.create(getEdmComplexType("FileUrl"), properties));
	}

	
	/**
	 * Copia il primo file in un nuovo file
	 * @param copyingFile il file da copiare
	 * @param string indirizzo in cui creare il nuovo file
	 * @throws IOException 
	 */
	private void copyFile(File copyingFile, String path) throws IOException {
		File file = new File(path);
		FileReader fr = null;
		BufferedReader reader = null;
		PrintWriter writer = null;
		if(file.exists()){
			file.delete();
		}
		try {
			file.createNewFile();
			fr = new FileReader(copyingFile);
			reader = new BufferedReader(fr);
			writer = new PrintWriter(file);
			String line = null;
			while((line = reader.readLine()) != null){
				writer.println(line);
			}
		} catch (IOException e) {
			throw e;
		} finally{
			if(writer != null){
				writer.close();
			}
			if(reader != null){
				try{
					reader.close();
				}catch(IOException ioe){
					StatPortalODataProxy.Log(ioe);
				}
			}
		}
	}

	/**
	 * Restituisce la tabella di corrispondenza tra nomi fisici e nomi logici delle colonne
	 * @param columns l'elenco di colonne
	 * @return la tabella di corrispondenza tra nomi fisici e nomi logici delle colonne
	 */
	private HashMap<String, String> getColumnMap(List<Column> columns){
		HashMap<String, String> map = new HashMap<String, String>();
		for(Column column : columns){
			if(!column.getPhysicalName().equals("the_geom")){
				map.put(column.getPhysicalName(), column.getLogicalName());
			}
		}
		return map;
	}
	
	/**
	 * Esporta un dato in formato csv
	 * @param function la service operation chiamata
	 * @param parameters i parametri della service operation
	 * @param queryInfo eventuali restrizioni
	 * @return l'esportazione di un dato in formato csv
	 */
	private BaseResponse exportCSV(EdmFunctionImport function,
			Map<String, OFunctionParameter> parameters, QueryInfo queryInfo) {		
		try{
			String uid = (String)parameters.get("uid").getValue().toString();
			String[] splittedKey = splitKey(uid, false);
			String language = null;
			if(parameters.containsKey("lang")){
				language = parameters.get("lang").getValue().toString();
			}
			if(splittedKey[1].equals(myName)){
				return exportCSVLocal(splittedKey[0], language);
			}else{
				return exportCSVFederation(splittedKey[1], function, parameters, queryInfo);
			}
		}catch (Throwable t) {
			StatPortalODataProxy.Log(t);
			return null;
		}
	}

	/**
	 * Esporta un dato in formato csv girando la richiesta ad un'entità federata
	 * @param name nome dell'entità
	 * @param function service operation da chiamare
	 * @param parameters parametri della service operation
	 * @param queryInfo eventuali restrizioni
	 * @return l'esportazione di un dato in formato csv
	 */
	private BaseResponse exportCSVFederation(String name, EdmFunctionImport function,
			Map<String, OFunctionParameter> parameters, QueryInfo queryInfo) {
		String requestUrl = buildServiceOperationUrl(name, function, parameters, queryInfo);
		StringMap<?> deserialized = readJsonFromUrl(requestUrl);
		StringMap<?> d = (StringMap<?>)deserialized.get("d");
		String fileUrl = (String) d.get("url");
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		properties.add(OProperties.string("url", fileUrl));
		OComplexObject complexObject = OComplexObjects.create(getEdmComplexType("FileUrl"), properties);
		return Responses.complexObject(complexObject);
	}

	/**
	 * Crea l'url da chiamare per girare una service operation ad un'altra entità federata
	 * @param name nome dell'entità federata
	 * @param function service operation da chiamare
	 * @param parameters parametri della service operation
	 * @param queryInfo eventuali restrizioni
	 * @return l'url da chiamare per girare una service operation ad un'altra entità federata
	 */
	private String buildServiceOperationUrl(String name,
			EdmFunctionImport function,
			Map<String, OFunctionParameter> parameters, QueryInfo queryInfo) {
		String address = getProxyAddress(name) + function.getName() +"?";
		for(String key : parameters.keySet()){
			address += key + "='"+parameters.get(key).getValue().toString()+"'&";
		}
		return address.substring(0, address.length()-1)+"&$format=json";
	}

	/**
	 * Esporta un dato in formato csv 
	 * @param uid uid del dato
	 * @param language la lingua del browser
	 * @return l'esportazione di un dato in formato csv
	 * @throws OdataDomainException
	 * @throws Exception
	 * @throws IOException
	 */
	private BaseResponse exportCSVLocal(String uid, String language) throws OdataDomainException, Exception, IOException {
		Locale locale = null;
		if(language == null){
			locale = Locale.getDefault();
		} else {
			String[] splitted = language.split("-");
			if(splitted.length < 1 || splitted.length > 2){
				locale = Locale.getDefault();
			}else if(splitted.length == 1){
				locale = new Locale(splitted[0]);
			}else if(splitted.length == 2){
				locale = new Locale(splitted[0], splitted[1]);
			}
		}
		Integer mdDataIdInt = MdData.getIdFromUid(uid);
		MdData data = MdData.findMdData(mdDataIdInt.intValue());
		File file = new File(StatPortalODataProxy.readConfig("csvdir")+"/"+uid+".csv");
		if(!file.exists() || file.lastModified() <= data.getLastUpdate().getTime()){
			//se il file esiste già ed è stato creato dopo l'ultima modifica del dato allora non lo rigenero altrimenti cancello quello esistente
			if(file.exists()){
				file.delete();
			}
			StringBuilder resultBuilder = new StringBuilder();		
			HashMap<String, String> columnMap = getColumnMap(data.getColumns());
			List<Row> rows = data.getRowsForViewer(locale);
			boolean first = true;
			for(Row row : rows){				
				HashMap<String,String> map = row.getValues();
				if(first){
					for(String key : map.keySet()){
						if(!key.equals("the_geom")){
							if(columnMap.containsKey(key)){
								resultBuilder.append("\""+ columnMap.get(key) +"\";");	
							}else{
								throw new Exception("Chiave non presente nella lista di colonne");
							}
						}
					}
					resultBuilder.append("\r\n");
					first = false;
				}
				for(String key : map.keySet()){
					if(!key.equals("the_geom")){
						String value = map.get(key);
						resultBuilder.append("\""+ ((value != null) ? value.replace("\"","\\\"") : "") +"\";");
					}
				}
				resultBuilder.append("\r\n");
			}
			writeFile(resultBuilder.toString(), file);
		}		
		
		final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		properties.add(OProperties.string("url", StatPortalODataProxy.readConfig("csvurl")+"/"+uid+".csv"));
		return Responses.complexObject(OComplexObjects.create(getEdmComplexType("FileUrl"), properties));
	}

	/**
	 * Fornisce il tipo complesso associato al nome
	 * @param name nome del tipo complesso
	 * @return il tipo complesso associato al nome
	 */
	private EdmComplexType getEdmComplexType(String name){
		for(EdmComplexType type : edmDataServices.getComplexTypes()){
			if(type.getName().equals(name)){
				return type;
			}
		}
		return null;
	}
	
	@Override
	public void close() {
		throw new NotImplementedException();
	}

	@Override
	public EntityResponse createEntity(String arg0, OEntity arg1) {
		throw new NotImplementedException();
	}

	@Override
	public EntityResponse createEntity(String arg0, OEntityKey arg1,
			String arg2, OEntity arg3) {
		throw new NotImplementedException();
	}

	@Override
	public void createLink(OEntityId arg0, String arg1, OEntityId arg2) {
		throw new NotImplementedException();
	}

	@Override
	public void deleteEntity(String arg0, OEntityKey arg1) {
		throw new NotImplementedException();
	}

	@Override
	public void deleteLink(OEntityId arg0, String arg1, OEntityKey arg2) {
		throw new NotImplementedException();
	}

	@Override
	public EntitiesResponse getEntities(String arg0, QueryInfo arg1) {
		throw new NotImplementedException();
	}

	/**
	 * Suddivide la chiave nelle sue due parti: guid e nome del servizio a cui fa riferimento
	 * @param key la chiave da suddividere
	 * @return un array di due elementi: il guid e il nome del servizio
	 * @throws Exception Nel caso la chiave splittata non sia di due elementi
	 */
	private String[] splitKey(OEntityKey key) throws Exception{
		return splitKey(key.toKeyStringWithoutParentheses(), true);
	}
	
	/**
	 * Splitta la stringa in due parti basandosi sul separatore "@"
	 * @param key la stringa da separare
	 * @param apex un flag che indica se devo scartare il primo e l'ultimo carattere
	 * @return la stringa originale divisa in due parti
	 * @throws Exception
	 */
	private String[] splitKey(String key, boolean apex) throws Exception{
		String[] splittedKey = key.split("@");
		if(splittedKey.length != 2){
			throw new Exception("Chiave non valida");
		}
		if(apex){
			splittedKey[0] = splittedKey[0].substring(1);
			splittedKey[1] = splittedKey[1].substring(0,splittedKey[1].length()-1);
		}
		return splittedKey;
	}
	
	/**
	 * Deserializza il risultato di una get in un oggetto che rappresenta una serializzazione json
	 * @param requestUrl l'url da cui fare la get
	 * @return un oggetto che rappresenta una serializzazione json
	 */
	private StringMap<?> readJsonFromUrl(String requestUrl){
		BufferedReader in = null;
		try {
            URL url = new URL(requestUrl);
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine,result="";
            while ((inputLine = in.readLine()) != null) {
                result=result.concat(inputLine);
            }
            Gson gson = new Gson();
            return gson.fromJson(result, StringMap.class);
        } catch (IOException e) {
        	return null;
        } finally{
        	if(in != null){
        		try{
        			in.close();
        		}catch(IOException e){
        			e.printStackTrace();
        		}
        	}
        }
	}
	
	@Override
	public EntityResponse getEntity(String entityName, OEntityKey key, QueryInfo queryInfo) {
		String requestUrl = buildRequestUrl(entityName, key, queryInfo, "");
		StringMap<?> deserialized = readJsonFromUrl(requestUrl);
		OEntity entity = buildEntity(entityName, deserialized);
		return Responses.entity(entity);	      
    }

	/**
	 * Crea un'entità data una deserializzazione JSON in una StringMap
	 * @param entityName Nome dell'entitySet
	 * @param deserialized deserializzazione JSON
	 * @return Un'entità OData
	 */
	private OEntity buildEntity(String entityName, StringMap<?> deserialized) {		
		Entities entityType = Entities.parse(entityName);
		EdmEntitySet ees = edmDataServices.getEdmEntitySet(entityName);
		if(entityType == Entities.UNKNOWN){
			return null;
		}
		OEntity entity = null;
		switch(entityType){
			case MD_DATA:
				MdData mdData = buildMdData(deserialized);
				entity = MdDataWrapper.createInstance(ees, mdData, myName);
				break;
//			case MD_DATA_DIM:
//				MdDataDim mdDataDim = buildMdDataDim(deserialized, false);
//				entity = MdDataDimWrapper.createInstance(ees, mdDataDim);
//				break;
//			case MD_MEASURE_FIELDS:
//				MdMeasureFields mdMeasureFields = buildMdMeasureFields(deserialized, false);
//				entity = MdMeasureFieldsWrapper.createInstance(ees, mdMeasureFields);
//				break;
//			case MD_LU_HIER_TYPE:
//				MdLuHierType mdLuHierType = buildMdLuHierType(deserialized);
//				entity = MdLuHierTypeWrapper.createInstance(ees, mdLuHierType);
//				break;
//			case MD_HIER_NODE:
//				MdHierNode mdHierNode = buildMdHierNode(deserialized);
//				entity = MdHierNodeWrapper.createInstance(ees, mdHierNode);
//				break;
//			case MD_HIERARCHY:
//				MdHierarchy mdHierarchy = buildMdHierarchy(deserialized);
//				entity = MdHierarchyWrapper.createInstance(ees, mdHierarchy);
//				break;
			default:
				entity = OEntities.create(ees, OEntityKey.create(0) , new ArrayList<OProperty<?>>(), new ArrayList<OLink>());
				break;
		}
		return entity;
	}

	/**
	 * Costruisce un insieme di entità a partire da una navigationProperty, una deserializzazione JSOn
	 * e eventualmente un entitySet
	 * @param navigationProperty
	 * @param deserialized deserializzazione JSON
	 * @param dataRowsEes entitySet
	 * @return un insieme di entità
	 */
	private List<OEntity> buildEntities(String navigationProperty, StringMap<?> deserialized, EdmEntitySet dataRowsEes){
		List<OEntity> entities = new ArrayList<OEntity>();
		EdmEntitySet ees = null;
		if(navigationProperty.equals("DataDimensions")){
			ees = edmDataServices.findEdmEntitySet("MdDataDim");
			HashMap<String, String[]> dataDimsProperties = new HashMap<String, String[]>();
			MdDataDim[] dataDims = buildMdDataDims(deserialized, dataDimsProperties);
			for(int i = 0; i < dataDims.length; i++){
				if(dataDimsProperties.containsKey(dataDims[i].getUid())){
					String[] dataDimProperties = dataDimsProperties.get(dataDims[i].getUid());
					entities.add(MdDataDimWrapper.createInstance(ees, dataDims[i], Double.valueOf(dataDimProperties[0]).intValue(), dataDimProperties[1], myName));
				}else{
					entities.add(MdDataDimWrapper.createInstance(ees, dataDims[i], dataDims[i].getCardinality(), dataDims[i].getDimType(), myName));
				}
			}
		} else if (navigationProperty.equals("DataMeasures")){
			ees = edmDataServices.findEdmEntitySet("MdMeasureFields");
			MdMeasureFields[] measureFields = buildMdMeasureFieldses(deserialized);
			for(int i = 0; i < measureFields.length; i++){
				entities.add(MdMeasureFieldsWrapper.createInstance(ees, measureFields[i], myName));
			}
		} else if(navigationProperty.equals("DataColumns")){
			ees = edmDataServices.findEdmEntitySet("DataColumns");
			Column[] dataColumns = buildDataColumns(deserialized);
			for(int i = 0; i < dataColumns.length; i++){
				entities.add(ColumnWrapper.createInstance(ees, dataColumns[i], i));
			}			
		} else if(navigationProperty.equals("DistinctCountRows")){
			//se l'utente ha specificato minMax=true non viene restituita un'istanza di DistinctCountRow ma di MinMax
			//controllo se il risultato contiene min e max
			if(isMinMax(deserialized)){
				ees = edmDataServices.findEdmEntitySet("MinMax");
				StringMap<?> d = (StringMap<?>)deserialized.get("d");
				ArrayList<?> results = (ArrayList<?>)d.get("results");
				double min = new Double((String)((StringMap<?>)results.get(0)).get("Min")).doubleValue();
				double max = new Double((String)((StringMap<?>)results.get(0)).get("Max")).doubleValue();
				entities.add(MinMaxEntity.createMinMaxEntity(ees, min, max));
			}else{
				ees = edmDataServices.findEdmEntitySet("DistinctCountRows");
				DistinctCountRow[] distinctCountRows = buildDistinctCountRows(deserialized);
				for(int i = 0; i < distinctCountRows.length; i++){
					entities.add(DistinctCountRowWrapper.createInstance(ees, distinctCountRows[i].getId(),distinctCountRows[i].getCount(), distinctCountRows[i].getLabel()));
				}
			}
		} else if(navigationProperty.equals("GenericColumnAggregationRows")){
			if(isMinMax(deserialized)){
				ees = edmDataServices.findEdmEntitySet("MinMax");
				StringMap<?> d = (StringMap<?>)deserialized.get("d");
				ArrayList<?> results = (ArrayList<?>)d.get("results");
				double min = new Double((String)((StringMap<?>)results.get(0)).get("Min")).doubleValue();
				double max = new Double((String)((StringMap<?>)results.get(0)).get("Max")).doubleValue();
				entities.add(MinMaxEntity.createMinMaxEntity(ees, min, max));
			} else if (isCount(deserialized)){
				StringMap<?> d = (StringMap<?>)deserialized.get("d");
				ArrayList<?> results = (ArrayList<?>)d.get("results");
				Integer total = Integer.valueOf(((Double)((StringMap<?>)results.get(0)).get("Tot")).intValue());
				entities.add(TotalEntity.createTotalEntity(dataRowsEes, total));
			} else{
				ees = edmDataServices.findEdmEntitySet("GenericColumnAggregationRows");
				GenericColumnAggregationRow[] genericColumnAggregationRows = buildGenericColumnAggregationRows(deserialized);
				for(int i = 0; i < genericColumnAggregationRows.length; i++){
					entities.add(GenericColumnAggregationRowWrapper.createInstance(ees, genericColumnAggregationRows[i].getId(), genericColumnAggregationRows[i].getVariable(), genericColumnAggregationRows[i].getColumn()));
				}
			}
			
			
		} else if(navigationProperty.equals("DataRows")){
			ees = dataRowsEes;
			StringMap<?> d = (StringMap<?>)deserialized.get("d");
			ArrayList<?> results = (ArrayList<?>)d.get("results");
			for(int i = 0; i < results.size(); i++){
				StringMap<?> result = (StringMap<?>)results.get(i);
				final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
				final List<OLink> links = new ArrayList<OLink>();	
				for(String key : result.keySet()){
					if(!key.equals("__metadata")){
						if(key.equals("row_id") || key.equals("Tot")){
							properties.add(OProperties.int32(key, Integer.valueOf(((Double)result.get(key)).intValue())));
						}else{
							properties.add(OProperties.string(key, result.get(key) != null ? result.get(key).toString() : null));
						}
					}
				}
				OEntity entity = OEntities.create(ees, OEntityKey.create("row_id"), properties, links);
				entities.add(entity);
			}			
		}
		return entities;
	}
	
	/**
	 * Controlla se la stringa Json deserializzata in una StringMap è il risultato
	 * di un'invocazione di DistinctCountRows con minMax = true
	 * @param deserialized
	 * @return
	 */
	private boolean isMinMax(StringMap<?> deserialized) {
		StringMap<?> d = (StringMap<?>)deserialized.get("d");
		ArrayList<?> results = (ArrayList<?>)d.get("results");
		if(results.size() > 0){
			StringMap<?> result = (StringMap<?>)results.get(0);
			if(result.containsKey("Min") && result.containsKey("Max")){
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}

	/**
	 * Controlla se la stringa Json deserializzata in una StringMap è il risultato
	 * di un'invocazione di GenericColumnAggregationRows con count = true
	 * @param deserialized
	 * @return
	 */
	private boolean isCount(StringMap<?> deserialized) {
		StringMap<?> d = (StringMap<?>)deserialized.get("d");
		ArrayList<?> results = (ArrayList<?>)d.get("results");
		if(results.size() > 0){
			StringMap<?> result = (StringMap<?>)results.get(0);
			if(result.containsKey("Tot")){
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}
	
	/**
	 * Costruisce un insieme di entità data una navigation property e una deserializzazione JSON
	 * @param navigationProperty
	 * @param deserialized deserializzazione JSON
	 * @return un insieme di entità
	 */
	private List<OEntity> buildEntities(String navigationProperty, StringMap<?> deserialized){
		return buildEntities(navigationProperty, deserialized, null);
	}
	
	/**
	 * Costruisce un array di oggetti che rappresentano il risultato
	 * della navigation property DistinctCountRows
	 * @param deserialized deserializzazione JSON
	 * @return un array di oggetti che rappresentano il risultato
	 * della navigation property DistinctCountRows
	 */
	private DistinctCountRow[] buildDistinctCountRows(StringMap<?> deserialized) {
		StringMap<?> d = (StringMap<?>)deserialized.get("d");
		ArrayList<?> results = (ArrayList<?>)d.get("results");
		DistinctCountRow[] distinctCountRows = new DistinctCountRow[results.size()];
		for(int i = 0; i < results.size(); i++){
			distinctCountRows[i] = buildDistinctCountRow((StringMap<?>)results.get(i), true);
		}
		return distinctCountRows;
	}
	
	/**
	 * Costruisce un array di oggetti che rappresentano il risultato
	 * della navigation property GenericColumnAggregationRows
	 * @param deserialized deserializzazione JSON
	 * @return un array di oggetti che rappresentano il risultato
	 * della navigation property GenericColumnAggregationRows
	 */
	private GenericColumnAggregationRow[] buildGenericColumnAggregationRows(StringMap<?> deserialized) {
		StringMap<?> d = (StringMap<?>)deserialized.get("d");
		ArrayList<?> results = (ArrayList<?>)d.get("results");
		GenericColumnAggregationRow[] genericColumnAggregationRows = new GenericColumnAggregationRow[results.size()];
		for(int i = 0; i < results.size(); i++){
			genericColumnAggregationRows[i] = buildGenericColumnAggregationRow((StringMap<?>)results.get(i), true);
		}
		return genericColumnAggregationRows;
	}

	/**
	 * Costruisce un array con le informazioni sulle colonne di un dato
	 * @param deserialized deserializzazoine JSON
	 * @return array con le informazioni sulle colonne di un dato
	 */
	private Column[] buildDataColumns(StringMap<?> deserialized) {
		StringMap<?> d = (StringMap<?>)deserialized.get("d");
		ArrayList<?> results = (ArrayList<?>)d.get("results");
		Column[] columns = new Column[results.size()];
		for(int i = 0; i < results.size(); i++){
			columns[i] = buildDataColumn((StringMap<?>)results.get(i), true);
		}
		return columns;
	}	

	/**
	 * Costruisce un array con le informazioni sulle misure di un dato
	 * @param deserialized deserializzazoine JSON
	 * @return array con le informazioni sulle misure di un dato
	 */
	private MdMeasureFields[] buildMdMeasureFieldses(StringMap<?> deserialized) {
		StringMap<?> d = (StringMap<?>)deserialized.get("d");
		ArrayList<?> results = (ArrayList<?>) d.get("results");
		MdMeasureFields[] measures = new MdMeasureFields[results.size()];
		for(int i = 0; i < results.size(); i++){
			measures[i] = buildMdMeasureFields((StringMap<?>)results.get(i), true);
		}
		return measures;
	}

	/**
	 * Costruisce un array con le informazioni sulle dimensioni di un dato
	 * @param deserialized deserializzazione JSON
	 * @param dataDimsProperties tabella che mette in relazione l'uid di un MdDataDim con la sua cardinalità e il suo tipo
	 * @return array con le informazioni sulle dimensioni di un dato
	 */
	private MdDataDim[] buildMdDataDims(StringMap<?> deserialized, HashMap<String, String[]> dataDimsProperties) {
		StringMap<?> d = (StringMap<?>)deserialized.get("d");
		ArrayList<?> results = (ArrayList<?>) d.get("results");
		MdDataDim[] dimensions = new MdDataDim[results.size()];
		for(int i = 0; i < results.size(); i++){
			dimensions[i] = buildMdDataDim((StringMap<?>)results.get(i), true, dataDimsProperties);
		}
		return dimensions;
	}

//	private MdHierarchy buildMdHierarchy(StringMap<?> deserialized) {
//		StringMap<?> d = (StringMap<?>) deserialized.get("d");
//		Object id = d.get("id");
//		MdHierarchy hierarchy = MdHierarchy.findMdHierarchy(Integer.valueOf(((Double)id).intValue()));
//		return hierarchy;
//	}
//
//	private MdHierNode buildMdHierNode(StringMap<?> deserialized) {
//		StringMap<?> d = (StringMap<?>) deserialized.get("d");
//		Object id = d.get("id");
//		MdHierNode hierNode = MdHierNode.findMdHierNode(Integer.valueOf(((Double)id).intValue()));
//		return hierNode;
//	}
//
//	private MdLuHierType buildMdLuHierType(StringMap<?> deserialized) {
//		StringMap<?> d = (StringMap<?>) deserialized.get("d");
//		Object id = d.get("id");
//		MdLuHierType luHierNode = MdLuHierType.findMdLuHierType(Integer.valueOf(((Double)id).intValue()));
//		return luHierNode;
//	}

	/**
	 * Costruisce una misura
	 * @param deserialized deserializzazione JSON
	 * @param entityList flag che indica se la deserializzazione riguarda una lista o un singolo oggetto
	 * @return Una misura
	 */
	private MdMeasureFields buildMdMeasureFields(StringMap<?> deserialized, boolean entityList) {
		StringMap<?> d = deserialized;
		if(!entityList){
			d = (StringMap<?>) deserialized.get("d");		
		}
		Object id = d.get("id");
		Object alias = d.get("alias");
		Object decimalPlaces = d.get("decimalPlaces");
		MdMeasureFields measureFields = null;
		if(id instanceof String){
			measureFields = new MdMeasureFields();
			measureFields.setUid((String)id);
			measureFields.setAlias((String)alias);
			measureFields.setDecimalPlaces(Short.valueOf(((Double)decimalPlaces).shortValue()));
		}else{
			measureFields = MdMeasureFields.findMdMeasureFields(Integer.valueOf(((Double)id).intValue()));
		}
		return measureFields;
	}

	/**
	 * Costruisce una colonna
	 * @param deserialized deserializzazione JSON
	 * @param entityList flag che indica se la deserializzazione riguarda una lista o un singolo oggetto
	 * @return Una colonna
	 */
	private Column buildDataColumn(StringMap<?> deserialized, boolean entityList) {
		StringMap<?> d = deserialized;
		if(!entityList){
			d = (StringMap<?>)deserialized.get("d");
		}
		Object id = d.get("id");
		Object physicalName = d.get("physicalName");
		Object logicalName = d.get("logicalName");
		Object columnType = d.get("columnType");
		Object differentDistinctCount = d.get("differentDistinctCount");
		Column column = new Column(
				physicalName != null ? physicalName.toString() : null, 
				logicalName != null ? logicalName.toString() : null, 
				Integer.valueOf(((Double)id).intValue()));
		if(differentDistinctCount != null){
			column.setDifferentDistinctCount(Integer.valueOf(((Double)differentDistinctCount).intValue()));
		}
		if(columnType != null){
			column.setType(ColumnType.valueOf(columnType.toString()));
		}
		return column;
	}
	
	/**
	 * Costruisce un'astrazione del risultato della DistinctCountRows
	 * @param deserialized deserializzazione JSON
	 * @param entityList flag che indica se la deserializzazione riguarda una lista o un singolo oggetto
	 * @return Un'astrazione del risultato della DistinctCountRows
	 */
	private DistinctCountRow buildDistinctCountRow(StringMap<?> deserialized,
			boolean entityList) {
		StringMap<?> d = deserialized;
		if(!entityList){
			d = (StringMap<?>)deserialized.get("d");
		}
		Object id = d.get("id");
		Object label = d.get("label");
		Object count = d.get("count");
		DistinctCountRow row = new DistinctCountRow(
				Integer.valueOf(((Double)id).intValue()),
				count != null ? Integer.valueOf(((Double)count).intValue()) : 0, 
				label != null ? label.toString() : null);
		return row;
	}
	
	/**
	 * Costruisce un'astrazione del risultato della GenericColumnAggregationRows
	 * @param deserialized deserializzazione JSON
	 * @param entityList flag che indica se la deserializzazione riguarda una lista o un singolo oggetto
	 * @return Un'astrazione del risultato della GenericColumnAggregationRows
	 */
	private GenericColumnAggregationRow buildGenericColumnAggregationRow(StringMap<?> deserialized,
			boolean entityList) {
		StringMap<?> d = deserialized;
		if(!entityList){
			d = (StringMap<?>)deserialized.get("d");
		}
		Object id = d.get("id");
		Object variable = d.get("variable");
		Object column = d.get("column");
		GenericColumnAggregationRow row = new GenericColumnAggregationRow(
				Integer.valueOf(((Double)id).intValue()),
				variable != null ? variable.toString() : null, 
				column != null ? column.toString() : null);
		return row;
	}
	
	
	/**
	 * Costruisce una dimensione
	 * @param deserialized deserializzazione JSON
	 * @param entityList flag che indica se la deserializzazione riguarda una lista o un singolo oggetto
	 * @param dataDimsProperties tabella che mette in relazione l'uid di un MdDataDim con la sua cardinalità e il suo tipo
	 * @return Una dimensione
	 */
	private MdDataDim buildMdDataDim(StringMap<?> deserialized, boolean entityList, HashMap<String, String[]> dataDimsProperties) {
		StringMap<?> d = deserialized;
		if(!entityList){
			d = (StringMap<?>) deserialized.get("d");		
		}
		Object id = d.get("id");
		Object alias = d.get("alias");
		MdDataDim dataDim = null;
		if(id instanceof String){
			dataDim = new MdDataDim();
			dataDim.setUid((String)id);
			dataDim.setAlias((String)alias);
			//in questo caso devo impostare cardinalità e tipo
			Object cardinality = d.get("cardinality").toString();
			Object dimType = d.get("dimType");
			dataDimsProperties.put((String)id, new String[]{(String)cardinality, (String)dimType});
		}else{
			dataDim = MdDataDim.findMdDataDim(Integer.valueOf(((Double)id).intValue()));
		}
		return dataDim;
	}
	
	/**
	 * Costruisce un dato
	 * @param deserialized deserializzazione JSON
	 * @return Un dato
	 */
	private MdData buildMdData(StringMap<?> deserialized) {
		StringMap<?> d = (StringMap<?>) deserialized.get("d");
		Object id = d.get("id");
		MdData data = null;
		if(id instanceof String){
			data = new MdData();
			data.setUid((String)id);
			Object idLuDataType = d.get("idLuDataType");
			data.setIdLuDataType(Integer.valueOf(((Double)idLuDataType).intValue()));
		}else{
			//se lo creiamo direttamente dalla deserializzazione velocizziamo!
			data = MdData.findMdData(Integer.valueOf(((Double)id).intValue()));
		}
		return data;
	}

	@Override
	public EntityIdResponse getLinks(OEntityId arg0, String arg1) {
		throw new NotImplementedException();
	}

	@Override
	public EdmDataServices getMetadata() {
		return edmDataServices;
//		throw new NotImplementedException();
	}

	@Override
	public MetadataProducer getMetadataProducer() {
		throw new NotImplementedException();
	}
	
	/**
	 * Costruisce l'url a cui girare la richiesta
	 * @param entitySetName nome dell'entityset
	 * @param entityKey chiave dell'entità
	 * @param queryInfo informazioni aggiuntive
	 * @param navProp eventuale proprietà
	 * @return l'url a cui girare la richiesta
	 */
	private String buildRequestUrl(String entitySetName, OEntityKey entityKey, QueryInfo queryInfo, String navProp){
		String[] splittedKey = null;
		try {
			splittedKey = splitKey(entityKey);
		} catch (Exception e1) {
			return null;
		}
		String requestUrl = null;
		if(myName.equals(splittedKey[1])){
			int id = -1;
			if(entitySetName.equals("MdData")){
				id = MdData.getIdFromUid(splittedKey[0]);
			} else if(entitySetName.equals("MdDataDim")){
				id = MdDataDim.getIdFromUid(splittedKey[0]);				
			} else if(entitySetName.equals("MdMeasureFields")){
				id = MdMeasureFields.getIdFromUid(splittedKey[0]);
			}
			requestUrl = serviceUrl + entitySetName + "("+ id +")"+ (!navProp.equals("") ? "/"+navProp : "") +"?$format=json"+formatQueryInfo(queryInfo, true);			
		}else{
			requestUrl = getProxyAddress(splittedKey[1]) + entitySetName + entityKey.toKeyString() + (!navProp.equals("") ? "/"+navProp : "") +"?$format=json"+formatQueryInfo(queryInfo, false);
		}
		return requestUrl.replace(" ", "%20");
	}

	/**
	 * Dato il nome di un'entità federata fornisce l'indirizzo del suo proxy
	 * @param name il nome di un'entità federata
	 * @return l'indirizzo del proxy dell'entità
	 */
	private String getProxyAddress(String name){
		List<Map<String,Object>> result = DBUtils.executeQueryForList("select address from proxy_map where name like '"+name+"'");
		if(result.size() < 1){
			return "";
		}else{
			return (String) result.get(0).get("address");
		}
	}
	
	@Override
	public BaseResponse getNavProperty(String entitySetName, OEntityKey entityKey,
			String navProp, QueryInfo queryInfo) {
		String requestUrl = buildRequestUrl(entitySetName, entityKey, queryInfo, navProp);	
        StringMap<?> deserialized = readJsonFromUrl(requestUrl);
        EdmEntitySet ees = edmDataServices.findEdmEntitySet(entitySetName);
		EdmMultiplicity multiplicity = ees.getType().findNavigationProperty(navProp).getToRole().getMultiplicity();
		if(multiplicity == EdmMultiplicity.ONE){
			return null;
		}else if(multiplicity == EdmMultiplicity.MANY){				
			if(navProp.equals("DataRows") || navProp.equals("GenericColumnAggregationRows")){
				String rowEntityName = "2-"+entityKey.toKeyStringWithoutParentheses();
				ees = edmDataServices.findEdmEntitySet(rowEntityName);
				if(ees == null){
					final EdmEntityType.Builder jitType = getEntityModel(entitySetName, entityKey, rowEntityName, deserialized);
					final EdmEntitySet.Builder jitEntitySet = EdmEntitySet.newBuilder().setName(rowEntityName).setEntityType(jitType);	
					ees = jitEntitySet.build();
				}				
				return Responses.entities(buildEntities(navProp, deserialized, ees), ees, 0, null);
			}else{
				return Responses.entities(buildEntities(navProp, deserialized), ees, 0, null);
			}
		}
		return null;	
	}

	/**
	 * Costruisce l'entitySet per le richieste di DataRows
	 * @param entitySetName nome dell'entitySet originale (es. MdData)
	 * @param entityKey chiave dell'entità
	 * @param rowEntityName nome dell'entitySet da creare
	 * @param deserialized deserializzazione JSON della chiamata a DataRows del servizio sottostante
	 * @return  l'entitySet per le richieste di DataRows
	 */
	private Builder getEntityModel(String entitySetName, OEntityKey entityKey,
			String rowEntityName, StringMap<?> deserialized) {
		final List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
		properties.add(EdmProperty.newBuilder("row_id").setType(EdmSimpleType.INT32));
		EntitiesResponse dataColumnsResponse = (EntitiesResponse)getNavProperty(entitySetName, entityKey, "DataColumns", null);
		for(OEntity column : dataColumnsResponse.getEntities()){
			properties.add(EdmProperty.newBuilder(rawNameToUniqueName((String)column.getProperty("logicalName").getValue())).setType(EdmSimpleType.STRING));
		}
		return EdmEntityType.newBuilder().setNamespace(namespace).setName(rowEntityName).addKeys("row_id").addProperties(properties);	
	}

	/**
	 * Mappa un nome logico in un nome univoco (come avviene nel servizio OData principale)
	 * @param rawName nome da trasformare
	 * @return un nome univoco
	 */
	private String rawNameToUniqueName(String rawName) {
		int hashCode = rawName.hashCode();
		if(hashCode == Integer.MIN_VALUE)
		{
			hashCode = Integer.MAX_VALUE;			
		}
		String hashCodeStr = String.valueOf(Math.abs(hashCode));
		String prefix = "C"+ rawName.replaceAll("[^\\w^0-9]", "_") + "_";
		return prefix.substring(0, Math.min(prefix.length(), 60 - hashCodeStr.length())) + hashCodeStr;		
	}

	/**
	 * Traduce il contenuto dell'oggetto queryInfo in una stringa da inserire nell'indirizzo
	 * @param queryInfo opzioni
	 * @param translateUid flag che indica se trasformare gli uid in id
	 * @return una stringa da inserire nell'indirizzo
	 */
	private String formatQueryInfo(QueryInfo queryInfo, boolean translateUid) {
		StringBuilder builder = new StringBuilder();
		if(queryInfo != null){
			if(queryInfo.top != null){
				builder.append("&$top="+queryInfo.top);
			}
			if(queryInfo.skip != null){
				builder.append("&$skip="+queryInfo.skip);
			}
			if(queryInfo.skipToken != null && !queryInfo.skipToken.equals("")){
				builder.append("&$skiptoken="+queryInfo.skipToken);
			}
			if(queryInfo.filter != null){
				ReverseExpressionVisitor visitor = new ReverseExpressionVisitor();
				queryInfo.filter.visit(visitor);
				String filter;
				try {
					filter = URLEncoder.encode(visitor.toString(),"UTF-8");
				} catch (UnsupportedEncodingException e) {
					filter = visitor.toString();
				}
				builder.append("&$filter="+ filter);
			}
			if(queryInfo.orderBy != null && queryInfo.orderBy.size() > 0){
				builder.append("&$orderby=");
				for(int i = 0; i < queryInfo.orderBy.size()-1; i++){
					OrderByExpression expression = queryInfo.orderBy.get(i);
					ReverseExpressionVisitor visitor = new ReverseExpressionVisitor();
					expression.visit(visitor);
					builder.append(visitor.toString()+",");
				}
				OrderByExpression expression = queryInfo.orderBy.get(queryInfo.orderBy.size()-1);
				ReverseExpressionVisitor visitor = new ReverseExpressionVisitor();
				expression.visit(visitor);
				builder.append(visitor.toString());
			}
			if(queryInfo.select != null && queryInfo.select.size() > 0){
				builder.append("&$select=");
				for(int i = 0; i < queryInfo.select.size()-1; i++){
					builder.append(queryInfo.select.get(i).getPropertyName());
					builder.append(",");
				}
				builder.append(queryInfo.select.get(queryInfo.select.size()-1).getPropertyName());
			}
			if(queryInfo.customOptions != null){
				for(String key : queryInfo.customOptions.keySet()){
					if(key.equals("dimensionList") && translateUid){
						builder.append("&dimensionList=");
						builder.append(replaceDimensionUid(queryInfo.customOptions.get(key)));						
					}else if(key.equals("measureList") && translateUid){
						builder.append("&measureList=");						
						builder.append(replaceMeasureUid(queryInfo.customOptions.get(key)));						
					}else{
						builder.append("&"+key+"="+queryInfo.customOptions.get(key));
					}
				}
			}
		}
		return builder.toString();
	}	
	
	/**
	 * Sostituisce gli uid con gli id delle misure
	 * @param measures l'elenco di misure separate da ;
	 * @return Lo stesso elenco con i rispettivi id
	 */
	private String replaceMeasureUid(String measures) {
		StringBuilder builder = new StringBuilder();
		for(String selectedMeasure : measures.split(";")){
			final String[] pair = selectedMeasure.split(":");							
			if(pair.length == 1 || pair.length == 2){
				String[] splittedMeasure = pair[0].split("@");
				if(splittedMeasure.length != 2){
					builder.append("-1");
				} else{
					builder.append(MdMeasureFields.getIdFromUid(splittedMeasure[0]));
				}
				if(pair.length == 2){
					builder.append(":"+pair[1]);
				}
				builder.append(";");
			}else{
				builder.append("-1;");
			}
		}
		return builder.toString().substring(0, builder.length()-1);		
	}

	/**
	 * Sostituisce gli uid delle dimensioni con i rispettivi id
	 * @param dimensions l'elenco di dimensioni separate da ;
	 * @return Lo stesso elenco con i rispettivi id
	 */
	private String replaceDimensionUid(String dimensions) {
		StringBuilder builder = new StringBuilder();
		for(String selectedDimension : dimensions.split(";")){			
			String[] splittedDimension = selectedDimension.split("@");
			if(splittedDimension.length != 2){
				builder.append("-1");
			} else{
				builder.append(MdDataDim.getIdFromUid(splittedDimension[0]));
			}
			builder.append(";");
		}
		return builder.toString().substring(0, builder.length()-1);		
	}

	@Override
	public void mergeEntity(String arg0, OEntity arg1) {
		throw new NotImplementedException();
	}

	@Override
	public void updateEntity(String arg0, OEntity arg1) {
		throw new NotImplementedException();
	}

	@Override
	public void updateLink(OEntityId arg0, String arg1, OEntityKey arg2,
			OEntityId arg3) {
		throw new NotImplementedException();
	}

}
