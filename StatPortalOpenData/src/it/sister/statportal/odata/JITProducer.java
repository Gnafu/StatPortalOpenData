package it.sister.statportal.odata;

import it.sister.statportal.odata.domain.Column;

import it.sister.statportal.odata.domain.DbTable;
import it.sister.statportal.odata.domain.IData;
import it.sister.statportal.odata.domain.IRepository.MinMax;
import it.sister.statportal.odata.domain.MdData;
import it.sister.statportal.odata.domain.MdData.FIELD_TO_SORT;
import it.sister.statportal.odata.domain.MdData.SORTING_DIRECTION;
import it.sister.statportal.odata.domain.MdDataDim;
import it.sister.statportal.odata.domain.MdGenericColumn;
import it.sister.statportal.odata.domain.MdHierNode;
import it.sister.statportal.odata.domain.MdMeasureFields;
import it.sister.statportal.odata.domain.MeasureAggregation;
import it.sister.statportal.odata.domain.MeasureAggregation.AggregateFunctions;
import it.sister.statportal.odata.domain.OdataDomainException;
import it.sister.statportal.odata.domain.Row;
import it.sister.statportal.odata.wrapper.ColumnWrapper;
import it.sister.statportal.odata.wrapper.DistinctCountRowWrapper;
import it.sister.statportal.odata.wrapper.GenericColumnAggregationRowWrapper;
import it.sister.statportal.odata.wrapper.MinMaxEntity;
import it.sister.statportal.odata.wrapper.TotalEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.odata4j.core.OEntities;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OFunctionParameter;
import org.odata4j.core.OLink;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntityContainer;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.edm.EdmFunctionImport;
import org.odata4j.edm.EdmSchema;
import org.odata4j.expression.EntitySimpleProperty;
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

/**
 * Producer pensato per generare al volo dei tipi in base alla tabella dei fatti/nodi/grezza richiesta.
 *
 */
public class JITProducer implements ODataProducer{

	private static final String ID_LITERAL = "id";
	
	private static EdmDataServices edmDataServices = null;
	
	private static String namespace = "http://jit.odata.statportal.sister.it";
	
	private static List<EdmEntitySet.Builder> entitySets = new ArrayList<EdmEntitySet.Builder>();
	
	private static List<EdmEntityType.Builder> entityTypes = new ArrayList<EdmEntityType.Builder>();
	
	private static HashMap<String, HashMap<String, String>[]> nameMap = new HashMap<String, HashMap<String,String>[]>();
	
	public JITProducer(){
		JITProducer.buildMetadata();
	}
	
	public EdmEntitySet getTotalEntitySet(){
		return edmDataServices.getEdmEntitySet("Totals");
	}
	
	public EdmEntitySet getCheckEntitySet(){
		return edmDataServices.getEdmEntitySet("Check");
	}	
	
	/**
	 * Metodo statico per la generazione dei metadati (entità, proprietà, associazioni, etc.).
	 * Per ora creiamo i metadati a mano, successivamente implementeremo la lettura dei metadati da un file xml di configurazione
	 */
	private static void buildMetadata() {
		JITProducer.initializeMetadata();		
	}

	/**
	 * Inizializza l'insieme di entità del JitProducer. 
	 * Per il momento crea un'unica entità che ci serve per simulare la count fino a che non verrà supportata da odata4j.
	 */
	private static void initializeMetadata(){

		final EdmEntityType.Builder totalType = TotalEntity.getEntityModel(namespace);
		entityTypes.add(totalType);
		final EdmEntitySet.Builder totalEntitySet = EdmEntitySet.newBuilder().setName("Totals").setEntityType(totalType);
		entitySets.add(totalEntitySet);
		
		final EdmEntityType.Builder minMaxType = MinMaxEntity.getEntityModel(namespace);
		entityTypes.add(minMaxType);
		final EdmEntitySet.Builder minMaxEntitySet = EdmEntitySet.newBuilder().setName("MinMax").setEntityType(minMaxType);
		entitySets.add(minMaxEntitySet);
		
		final EdmEntityType.Builder checkType = TotalEntity.getEntityModel(namespace);
		entityTypes.add(checkType);
		final EdmEntitySet.Builder checkEntitySet = EdmEntitySet.newBuilder().setName("Check").setEntityType(checkType);
		entitySets.add(checkEntitySet);
		
		EdmEntityType.Builder columnType = ColumnWrapper.getEntityModel(namespace);
		entityTypes.add(columnType);
		EdmEntitySet.Builder columnEntitySet = EdmEntitySet.newBuilder().setName("DataColumns").setEntityType(columnType);
		entitySets.add(columnEntitySet);
		
		EdmEntityType.Builder distinctCountRowsType = DistinctCountRowWrapper.getEntityModel(namespace);
		entityTypes.add(distinctCountRowsType);
		EdmEntitySet.Builder distinctCountRowsEntitySet = EdmEntitySet.newBuilder().setName("DistinctCountRows").setEntityType(distinctCountRowsType);
		entitySets.add(distinctCountRowsEntitySet);
		
		EdmEntityType.Builder genericColumnAggregationRowsType = GenericColumnAggregationRowWrapper.getEntityModel(namespace);
		entityTypes.add(genericColumnAggregationRowsType);
		EdmEntitySet.Builder genericColumnAggregationRowsEntitySet = EdmEntitySet.newBuilder().setName("GenericColumnAggregationRows").setEntityType(genericColumnAggregationRowsType);
		entitySets.add(genericColumnAggregationRowsEntitySet);
		
		final EdmEntityContainer.Builder container = EdmEntityContainer.newBuilder().setName(namespace + "/Entities").setIsDefault(true).addEntitySets( entitySets);
		final EdmSchema.Builder modelSchema = EdmSchema.newBuilder().setNamespace(namespace + "/Model").addEntityTypes(entityTypes);
		final EdmSchema.Builder containerSchema = EdmSchema.newBuilder().setNamespace(namespace + "/Container").addEntityContainers(container);

		edmDataServices = EdmDataServices.newBuilder().addSchemas(containerSchema, modelSchema).build();
	}
	
	@Override
	public EdmDataServices getMetadata() {
		return edmDataServices;
	}

	@Override
	public MetadataProducer getMetadataProducer() {
		throw new NotImplementedException();
	}

	@Override
	public EntitiesResponse getEntities(final String entitySetName,
			final QueryInfo queryInfo) {
		throw new NotImplementedException();
	}
	
	/**
	 * Restituisce le rige richieste.
	 * Se l'entitySet richiesto non è presente lo crea al volo.
	 * Per supportare la count, in attesa della versione 0.6 di odata4j, 
	 * restituisce un entitySet con un'unica entità se richiesto.
	 * @param entity L'entità di cui si chiedono le righe
	 * @param queryInfo Opzioni della query
	 * @param raw Flag che indica se restituire i dati grezzi o tradotti
	 * @return Un insieme di entità rappresentante le righe di una tabella dei fatti/nodi/grezza
	 * @throws OdataDomainException Nel caso ci sia un problema nel recuperare le righe
	 * @throws OpenDataException Nel caso l'entità non sia un'entità valida.
	 */
	public EntitiesResponse getRows(final OEntity entity, final QueryInfo queryInfo, boolean raw) throws OdataDomainException, OpenDataException{
		//ottengo il tipo dell'entità
		final Entities entityType = Entities.parse(entity.getEntitySetName());
		//costruisco il nome dell'entitySet da cercare
		final StringBuffer nameBuffer = new StringBuffer();
		boolean isRaw = raw;
		if(entityType == Entities.DB_TABLE){
			nameBuffer.append("1-");
			isRaw = true;
		}else if(entityType == Entities.MD_DATA){
			nameBuffer.append("2-");
		} else if(entityType == Entities.MD_HIER_NODE){
			nameBuffer.append("3-");
			isRaw = true;
		}
		nameBuffer.append(entity.getProperty(ID_LITERAL).getValue().toString());
		final String rowEntitySetName = nameBuffer.toString();
		EdmEntitySet edmEntitySet = edmDataServices.findEdmEntitySet(rowEntitySetName);
		//se non trovo l'entitySet lo costruisco al volo
		if(edmEntitySet == null){
			final EdmEntityType.Builder jitType = getEntityModel(rowEntitySetName, entity);
			final EdmEntitySet.Builder jitEntitySet = EdmEntitySet.newBuilder().setName(rowEntitySetName).setEntityType(jitType);	
			edmEntitySet = jitEntitySet.build();
		}
		//creo le entità da restituire
		final List<OEntity> entities = buildEntities(edmEntitySet, entity, queryInfo, isRaw);
		return Responses.entities(entities, edmEntitySet, 0, null);
	}
	
	/**
	 * Fornisce l'insieme di colonne di un dato
	 * @param entity l'entità dato
	 * @param queryInfo eventuali restrizioni
	 * @return un insieme di entità che rappresentano l'insieme di colonne del dato
	 * @throws OpenDataException
	 */
	public EntitiesResponse getColumns(final OEntity entity, final QueryInfo queryInfo) throws OpenDataException{
		final String columnEntitySetName = "DataColumns";
		EdmEntitySet edmEntitySet = edmDataServices.findEdmEntitySet(columnEntitySetName);
		//se non trovo l'entitySet lo costruisco al volo
		if(edmEntitySet == null){
			StatPortalOData.logger.error("edmEntitySet non valido");
			throw new OpenDataException();
		}
		//creo le entità da restituire
		final List<OEntity> entities = buildColumnEntities(edmEntitySet, entity, queryInfo);
		return Responses.entities(entities, edmEntitySet, 0, null);
	}
	
	/**
	 * Crea una risposta basata su un insieme di entità generate dalla proprietà GenericColumnAggregation
	 * @param entity l'entità su cui è chiamata la proprietà
	 * @param queryInfo eventuali parametri
	 * @return una risposta OData
	 * @throws OpenDataException
	 */
	public EntitiesResponse getGenericColumnAggregationRows(final OEntity entity, final QueryInfo queryInfo) throws OpenDataException{
		final String genericColumnAggregationRowEntitySetName = "GenericColumnAggregationRows";
		EdmEntitySet edmEntitySet = edmDataServices.findEdmEntitySet(genericColumnAggregationRowEntitySetName);
		if(edmEntitySet == null){
			StatPortalOData.logger.error("edmEntitySet non valido");
			throw new OpenDataException();
		}
		//creo le entità da restituire
		final List<OEntity> entities = buildGenericColumnAggregationEntity(edmEntitySet, entity, queryInfo);
		return Responses.entities(entities, edmEntitySet, 0, null);
	}
	
	/**
	 * Fornisce un insieme di entità che rappresentano il count dei valori distinti delle righe
	 * @param entity l'entità dato
	 * @param queryInfo eventuali restrizioni
	 * @return un insieme di entità che rappresentano il count dei valori distinti delle righe
	 * @throws OpenDataException
	 */
	public EntitiesResponse getDistinctCountRows(final OEntity entity, final QueryInfo queryInfo) throws OpenDataException{
		final String distinctCountRowEntitySetName = "DistinctCountRows";
		EdmEntitySet edmEntitySet = edmDataServices.findEdmEntitySet(distinctCountRowEntitySetName);
		if(edmEntitySet == null){
			StatPortalOData.logger.error("edmEntitySet non valido");
			throw new OpenDataException();
		}
		//creo le entità da restituire
		final List<OEntity> entities = buildDistinctCountRowsEntity(edmEntitySet, entity, queryInfo);
		return Responses.entities(entities, edmEntitySet, 0, null);
	}

	/**
	 * Costruisce le entità per rispondere ad una richiesta di GenericColumnAggregation
	 * @param ees l'entitySet
	 * @param entity l'entità a cui si riferisce la navigation property
	 * @param queryInfo eventuali parametri
	 * @return una lista di entità
	 * @throws OpenDataException
	 */
	private List<OEntity> buildGenericColumnAggregationEntity(EdmEntitySet ees, OEntity entity, QueryInfo queryInfo) throws OpenDataException{
		final List<OEntity> list = new ArrayList<OEntity>();
		final Entities entityType = Entities.parse(entity.getEntitySetName());
		if(entityType != Entities.MD_DATA){
			StatPortalOData.logger.error("GenericColumnAggregation è supportata solo da MdData");
			throw new OpenDataException("GenericColumnAggregation è supportata solo da MdData");
		}
		try{	
			IData data = getData(entity, entityType);
			String rowEntitySetName = "2-"+entity.getProperty(ID_LITERAL).getValue().toString();
			EdmEntitySet edmEntitySet = edmDataServices.findEdmEntitySet(rowEntitySetName);
			//se non trovo l'entitySet lo costruisco al volo
			if(edmEntitySet == null){
				final EdmEntityType.Builder jitType = getEntityModel(rowEntitySetName, entity);
				final EdmEntitySet.Builder jitEntitySet = EdmEntitySet.newBuilder().setName(rowEntitySetName).setEntityType(jitType);	
				edmEntitySet = jitEntitySet.build();
			}
			List<Row> rows = null;
			//measureAggregation e genericColumn
			if(!queryInfo.customOptions.containsKey("measureList") || !queryInfo.customOptions.containsKey("column")){
				StatPortalOData.logger.error("GenericColumnAggregation richiede i due parametri measureList e column");
				throw new OpenDataException("GenericColumnAggregation richiede i due parametri measureList e column");
			}
			final List<MeasureAggregation> aggregationList = prepareMeasureList(queryInfo);
			MeasureAggregation measureAggregation = aggregationList.get(0);
			final String columnName = queryInfo.customOptions.get("column");				
			String rawName = uniqueNameToRawName(rowEntitySetName, columnName);

			MdGenericColumn genericColumn = null;
			for(MdGenericColumn gc : ((MdData)data).getMdGenericColumns()){
				if(gc.getAlias().equals(rawName)){
					genericColumn = gc;
					break;
				}
			}
			
			if(queryInfo.customOptions.containsKey("minPercentage") && 
					queryInfo.customOptions.containsKey("minResults") &&
					queryInfo.customOptions.containsKey("maxResults")){
				//restituire l'insieme di righe derivanti dall'aggregazione filtrate in base alle opzioni indicate
				double minPercentage = Double.parseDouble(queryInfo.customOptions.get("minPercentage"));
				int minResults = Integer.parseInt(queryInfo.customOptions.get("minResults"));
				int maxResults = Integer.parseInt(queryInfo.customOptions.get("maxResults"));
				boolean othersValue = false;
				if(queryInfo.customOptions.containsKey("othersValue")){
					othersValue = Boolean.parseBoolean(queryInfo.customOptions.get("othersValue"));
				}
				rows = ((MdData)data).getMeasuresAggregatedValuesGbGenericColumnsForPie(measureAggregation, genericColumn, minPercentage, minResults, maxResults, othersValue);
			} else {
				//filterExpression
				String filterExpression = null;
				if(queryInfo.customOptions.containsKey("minMax") && queryInfo.customOptions.get("minMax").equalsIgnoreCase("true")){
					//restituire la coppia minimo e massimo delle righe derivanti dall'aggregazione
					MinMax minMax = ((MdData)data).getMinMaxForMeasuresAggregatedValuesGbGenericColumns(measureAggregation, genericColumn, filterExpression);
					return buildMinMaxEntities(minMax);
				}
				//top
				Integer top = queryInfo.customOptions.containsKey("top") ? Integer.parseInt(queryInfo.customOptions.get("top")) : null;
				//skip
				Integer skip = queryInfo.skip;				
				//orderBy
				final String orderBy = prepareOrderBy(rowEntitySetName, queryInfo, data, data.getColumns(), false);
				if(queryInfo.customOptions.containsKey("count")){
					//restituire un singolo elemento con il count delle righe derivanti dall'aggregazione
					int count = ((MdData)data).getMeasuresAggregatedValuesGbGenericColumnsCount(measureAggregation, genericColumn, skip, top, filterExpression, orderBy);
					return buildCountEntities(count);
				}
				rows = ((MdData)data).getMeasuresAggregatedValuesGbGenericColumns(measureAggregation, genericColumn, skip, top, filterExpression, orderBy);
			}
			//creo le entità
			for(int j = 0; j < rows.size(); j++){
				final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
				final List<OLink> links = new ArrayList<OLink>();					
				Row values = rows.get(j);
				properties.add(OProperties.int32("id", j));
				properties.add(OProperties.string("variable", values.getValue(measureAggregation.getMeasure().getMeasureField())));
				properties.add(OProperties.string("column",  values.getValue(genericColumn.getColumnField())));
				
				final OEntity newEntity = OEntities.create(ees, OEntityKey.create(j), properties, links);
				list.add(newEntity);
			}
		}catch(OdataDomainException odde){
			StatPortalOData.logger.error(odde.getMessage(), odde);
			return list;
		}
		return list;
	}
	
	/**
	 * Costruisce un insieme di entità che sono usate per rispondere alla query
	 * della navigation property DistinctCountRows
	 * @param ees l'entity set richiesto
	 * @param entity l'entità a cui sono associate le informazioni
	 * @param queryInfo eventuali restrizioni
	 * @return un insieme di entità che sono usate per rispondere alla query
	 * @throws OpenDataException
	 */
	private List<OEntity> buildDistinctCountRowsEntity(
			EdmEntitySet ees, OEntity entity, QueryInfo queryInfo) throws OpenDataException {
		final List<OEntity> list = new ArrayList<OEntity>();
		final Entities entityType = Entities.parse(entity.getEntitySetName());
		if(entityType != Entities.MD_DATA){
			StatPortalOData.logger.error("DistinctCountRows è supportata solo da MD_DATA");
			throw new OpenDataException();
		}
		try{	
			IData data = getData(entity, entityType);
			String rowEntitySetName = "2-"+entity.getProperty(ID_LITERAL).getValue().toString();
			EdmEntitySet edmEntitySet = edmDataServices.findEdmEntitySet(rowEntitySetName);
			//se non trovo l'entitySet lo costruisco al volo
			if(edmEntitySet == null){
				final EdmEntityType.Builder jitType = getEntityModel(rowEntitySetName, entity);
				final EdmEntitySet.Builder jitEntitySet = EdmEntitySet.newBuilder().setName(rowEntitySetName).setEntityType(jitType);	
				edmEntitySet = jitEntitySet.build();
			}
			if(queryInfo.customOptions.containsKey("distinctField")){
				final String distinctField = queryInfo.customOptions.get("distinctField");				
				String rawName = uniqueNameToRawName(rowEntitySetName, distinctField);
				String physicalDistinctField = "";
				for(Column column : ((MdData)data).getColumns()){
					if(column.getLogicalName().equals(rawName)){
						physicalDistinctField = column.getPhysicalName();
						break;
					}
				}
				if(physicalDistinctField.equals("")){
					return list;
				}
				//top, othersValue, minPercentage
				boolean othersValue = false;
				if(queryInfo.customOptions.containsKey("othersValue")){
					othersValue = Boolean.parseBoolean(queryInfo.customOptions.get("othersValue"));
				}
				FIELD_TO_SORT fieldToSort = FIELD_TO_SORT.COUNT;
				if(queryInfo.customOptions.containsKey("fieldToSort")){
					try{
						fieldToSort = Enum.valueOf(FIELD_TO_SORT.class, queryInfo.customOptions.get("fieldToSort"));
					}catch(Exception exception){
						fieldToSort = FIELD_TO_SORT.COUNT;
					}
				}
				SORTING_DIRECTION sortingDirection = SORTING_DIRECTION.DESC;
				if(queryInfo.customOptions.containsKey("sortingDirection")){
					try{
						sortingDirection = Enum.valueOf(SORTING_DIRECTION.class, queryInfo.customOptions.get("sortingDirection"));
					}catch(Exception exception){
						sortingDirection = SORTING_DIRECTION.DESC;
					}
				}
				String filter = "";
				if(queryInfo.filter != null){
					SqlExpressionVisitor visitor = new SqlExpressionVisitor(rowEntitySetName, data, true, true);
					queryInfo.filter.visit(visitor);
					filter = visitor.toString();
					filter = filter.substring(filter.indexOf('\'')+1, filter.lastIndexOf('\''));
				}
				List<Map<String,Object>> rows = null;
				if(queryInfo.customOptions.containsKey("top")){
					try{
						int top = Integer.parseInt(queryInfo.customOptions.get("top"));
						int skip = queryInfo.skip;
											
						rows = ((MdData)data).getDistinctValueAndCountForField(physicalDistinctField, skip, top, othersValue, fieldToSort, sortingDirection, filter);
					}catch(NumberFormatException nfe){
						StatPortalOData.logger.error(nfe.getMessage(), nfe);
						rows = ((MdData)data).getDistinctValueAndCountForField(physicalDistinctField, fieldToSort, sortingDirection);
					}
				} else if(queryInfo.customOptions.containsKey("minPercentage") && 
						queryInfo.customOptions.containsKey("minResults") &&
						queryInfo.customOptions.containsKey("maxResults")){
					try{
						double minPercentage = Double.parseDouble(queryInfo.customOptions.get("minPercentage"));
						int minResults = Integer.parseInt(queryInfo.customOptions.get("minResults"));
						int maxResults = Integer.parseInt(queryInfo.customOptions.get("maxResults"));
						rows = ((MdData)data).getDistinctValueAndCountForField(physicalDistinctField, minPercentage, minResults, maxResults, othersValue, fieldToSort, sortingDirection);
					}catch(NumberFormatException nfe){
						StatPortalOData.logger.error(nfe.getMessage(), nfe);
						rows = ((MdData)data).getDistinctValueAndCountForField(physicalDistinctField, fieldToSort, sortingDirection);
					}
				} else if(queryInfo.customOptions.containsKey("minMax") && queryInfo.customOptions.get("minMax").equalsIgnoreCase("true")){
					MinMax minMax = ((MdData)data).getMinMaxCountForDistinctValueAndCountForField(physicalDistinctField, filter);
					return buildMinMaxEntities(minMax);
				} 
				else{
					rows = ((MdData)data).getDistinctValueAndCountForField(physicalDistinctField, fieldToSort, sortingDirection);
				}
				//creo le entità
				for(int j = 0; j < rows.size(); j++){		
					final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
					final List<OLink> links = new ArrayList<OLink>();					
					Map<String, Object> values = rows.get(j);
					properties.add(OProperties.int32("id", j));
					properties.add(OProperties.string("label", values.get("label").toString()));
					properties.add(OProperties.int32("count",  Integer.parseInt(values.get("occurrences").toString())));
					
					final OEntity newEntity = OEntities.create(ees, OEntityKey.create(j), properties, links);
					list.add(newEntity);
				}
			}
		}catch(OdataDomainException odde){
			StatPortalOData.logger.error(odde.getMessage(), odde);
			return list;
		}
		return list;
	}

	/**
	 * Genera al volo un entityType builder basato sulle righe dell'entità passata come parametro.
	 * @param rowEntitySetName il nome da dare all'entityType
	 * @param entity l'entità la cui tabella dei fatti/nodi/grezza fornisce la struttura all'entitySet
	 * @return Un entityType builder basato sulle righe dell'entità parametro
	 * @throws OdataDomainException nel caso ci sia un problema nel recuperare la struttura della tabella collegata all'entità
	 * @throws OpenDataException nel caso l'entità passata non sia valida 
	 */
	private EdmEntityType.Builder getEntityModel(final String rowEntitySetName, final OEntity entity) throws OdataDomainException, OpenDataException{
		//prendo il tipo di entità
		final Entities entityType = Entities.parse(entity.getEntitySetName());
		//entraggo il dato collegato all'entità
		IData data = getData(entity, entityType);
		//costruisco il modello di entità dandogli come proprietà un id e le colonne della tabella collegata all'entità
		final List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
		properties.add(EdmProperty.newBuilder("row_id").setType(EdmSimpleType.INT32));
		final List<Column> columns = data.getColumns();
		fillMap(rowEntitySetName, columns);
		for(Column column : columns){
			properties.add(EdmProperty.newBuilder(rawNameToUniqueName(rowEntitySetName, column.getLogicalName())).setType(EdmSimpleType.STRING));
		}
		return EdmEntityType.newBuilder().setNamespace(namespace).setName(rowEntitySetName).addKeys("row_id").addProperties(properties);
	}
	
	/**
	 * Riempie una HashMap mettendo in corrispondenza il nome di un entity set con 
	 * una tabella contenente i nomi logici delle sue colonne associati ai nomi univoci
	 * generati dal sistema 
	 * @param entitySetName nome dell'entity set
	 * @param columns insieme di colonne di un dato
	 */
	@SuppressWarnings("unchecked")
	private void fillMap(final String entitySetName, final List<Column> columns){
		
		if(nameMap.containsKey(entitySetName)){
			nameMap.remove(entitySetName);
		}
		
		HashMap<String, String>[] nameArray = (HashMap<String, String>[]) new HashMap[2];
		nameArray[0] = new HashMap<String, String>();
		nameArray[1] = new HashMap<String, String>();
		for(Column column : columns){
			String rawName = column.getLogicalName();
			String uniqueName = generateUniqueName(rawName, nameArray[1]);
			nameArray[0].put(rawName, uniqueName);
			nameArray[1].put(uniqueName, rawName);				
		}
		nameMap.put(entitySetName, nameArray);
	}
	
	/**
	 * Regole per la generazione del nome univoco:
	 * i caratteri a-zA-Z0-9 rimangono invariati.
	 * Tutti gli altri vengono sostituiti da _
	 * In più faccio l'hashcode del nome originale e lo concateno 
	 * mettendoci _ davanti
	 * Per evitare nomi inizianti per numero ci prepongo C
	 * @param rawName
	 * @param hashMap
	 * @return
	 */
	private String generateUniqueName(final String rawName, final HashMap<String, String> hashMap) {	
		int hashCode = rawName.hashCode();
		if(hashCode == Integer.MIN_VALUE)
		{
			hashCode = Integer.MAX_VALUE;			
		}
		String hashCodeStr = String.valueOf(Math.abs(hashCode));
		String prefix = "C"+ rawName.replaceAll("[^\\w^0-9]", "_") + "_";
		String candidateName = prefix.substring(0, Math.min(prefix.length(), 60 - hashCodeStr.length())) + hashCodeStr;
		if(hashMap.containsKey(candidateName)){
			throw new IllegalArgumentException();
		}
		return candidateName;
	}
	
	
	/**
	 * Metodo di conversione da un nome di colonna al nome univoco da usare nel sistema
	 * @param entitySetName nome dell'entity set
	 * @param rawName nome di colonna
	 * @return il nome univoco della colonna
	 */
	public static String rawNameToUniqueName(final String entitySetName, final String rawName){
		if(!nameMap.containsKey(entitySetName)){
			//se il nome dell'entitySet non è contenuto nella hashMap c'è un errore
			throw new IllegalArgumentException();
		}
		//prendo l'array, avrà sempre due elementi, il primo raw -> unique, il secondo unique -> raw
		HashMap<String, String>[] names = nameMap.get(entitySetName);
		if(!names[0].containsKey(rawName)){
			StatPortalOData.logger.error(rawName + " non contenuto nell'elenco di nomi");
			throw new IllegalArgumentException();
		}
		return names[0].get(rawName);		
	}
	
	/**
	 * Metodo di conversione da un nome univoco al nome di colonna
	 * @param entitySetName nome dell'entity set
	 * @param uniqueName nome univoco
	 * @return il nome della colonna
	 */
	public static String uniqueNameToRawName(final String entitySetName, final String uniqueName){
		if(!nameMap.containsKey(entitySetName)){
			//se il nome dell'entitySet non è contenuto nella hashMap c'è un errore
			StatPortalOData.logger.error(entitySetName + " non contenuto nel dizionario");
			throw new IllegalArgumentException();
		}
		//prendo l'array, avrà sempre due elementi, il primo raw -> unique, il secondo unique -> raw
		HashMap<String, String>[] names = nameMap.get(entitySetName);
		String toReturn = "";
		if(names[1].containsKey(uniqueName)){
			toReturn = names[1].get(uniqueName);
		}
		return toReturn;
	}
	
	/**
	 * Costruisce delle entità che rispecchiano la tabella dei fatti/nodi/grezza dell'entità passata come parametro
	 * @param ees l'entitySet a cui appartengono le entità da creare
	 * @param entity l'entità di base da cui crearle
	 * @param queryInfo Parametri di selezione/ordinamento/etc.
	 * @param raw Flag che indica se bisogna creare le entità basandosi sulla tabella tradotta o no.
	 * @return e entità che rispecchiano la tabella dei fatti/nodi/grezza dell'entità passata come parametro
	 * @throws OpenDataException se l'entità passata non è valida
	 */
	private List<OEntity> buildEntities(final EdmEntitySet ees, final OEntity entity, final QueryInfo queryInfo, final boolean raw) throws OpenDataException {
		final List<OEntity> list = new ArrayList<OEntity>();
		final Entities entityType = Entities.parse(entity.getEntitySetName());
		try{
			List<Column> columns = null;
			List<Row> rows = null;				
			IData data = getData(entity, entityType);
			//ottengo la struttura della tabella
			columns = data.getColumns();
			final List<MdDataDim> dimensionList = prepareDimensionList(queryInfo);					
			final List<MeasureAggregation> aggregationList = prepareMeasureList(queryInfo);
			final String orderBy = prepareOrderBy(ees.getName(), queryInfo, data, columns, dimensionList.size() == 0 && aggregationList.size() == 0);					
			final FilterParameters filterParameters = prepareFilter(ees.getName(), queryInfo, data);					
			String filter = filterParameters.getFilter();
			//controllo se stiamo simulando la count
			if(queryInfo.customOptions.containsKey("count")){
				int total;
				if(raw){
					if(filter.compareTo("") == 0){
						total = data.getCountRows();
					}else{
						total = data.getCountRows(filter);
					}
				}else{					
					if(queryInfo.customOptions.containsKey("dimensionList") || queryInfo.customOptions.containsKey("measureList")){
						boolean excludeNulls = queryInfo.customOptions.containsKey("excludeNulls") && queryInfo.customOptions.get("excludeNulls").equalsIgnoreCase("true");
						total = ((MdData)data).getCountRowsAggregated(dimensionList, aggregationList, filter, orderBy, filterParameters.getRelatedDims(), excludeNulls);
					}else{
						total = ((MdData)data).getCountRows(filter);
					}
				}			
				return buildCountEntities(total);
			}else if(queryInfo.customOptions.containsKey("cardinalityForField")){
				String field = queryInfo.customOptions.get("cardinalityForField");
				String logicalName = JITProducer.uniqueNameToRawName(ees.getName(), field);
				String physicalName = "";
				for(Column column : columns){
					if(column.getLogicalName().compareTo(logicalName) == 0){
						physicalName = column.getPhysicalName();
						break;
					}
				}
				int total = ((MdData)data).getCardinalityForField(physicalName);
				return buildCountEntities(total);
			}else if(queryInfo.customOptions.containsKey("minMax") && queryInfo.customOptions.get("minMax").equalsIgnoreCase("true")){
				if(aggregationList.size() != 1){
					return null;
				}
				MinMax minMax = ((MdData)data).getMinMaxForAggregatedMeasure(dimensionList, aggregationList.get(0), filter, filterParameters.relatedDims);
				return buildMinMaxEntities(minMax);
			}else{			
				final Integer skip = queryInfo.skip == null ? 0 : queryInfo.skip;
				final Integer top = queryInfo.top == null ? 50 : queryInfo.top;
								
				//ottengo l'insieme di righe (dovrò generare tante entità quante sono le righe)
				if(raw){
					rows = data.getRows(skip, top, filter);
				}else{
					if(queryInfo.customOptions.containsKey("minPercentage") && 
							queryInfo.customOptions.containsKey("minResults") &&
							queryInfo.customOptions.containsKey("maxResults") &&
							queryInfo.customOptions.containsKey("othersValue")){
						try{
							double minPercentage = Double.parseDouble(queryInfo.customOptions.get("minPercentage"));
							int minResults = Integer.parseInt(queryInfo.customOptions.get("minResults"));
							int maxResults = Integer.parseInt(queryInfo.customOptions.get("maxResults"));
							boolean othersValue = Boolean.parseBoolean(queryInfo.customOptions.get("othersValue"));
							rows = ((MdData)data).getRowsAggregated(dimensionList, aggregationList, skip, top, filter, orderBy, filterParameters.getRelatedDims(), minPercentage, minResults, maxResults, othersValue);
						}catch(NumberFormatException nfe){
							StatPortalOData.logger.error(nfe.getMessage(), nfe);
							rows = ((MdData)data).getRowsAggregated(dimensionList, aggregationList, skip, top, filter, orderBy, filterParameters.getRelatedDims());
						}
					}else{
						rows = ((MdData)data).getRowsAggregated(dimensionList, aggregationList, skip, top, filter, orderBy, filterParameters.getRelatedDims());						
					}
				}
				
				//creo le entità
				for(int j = 0; j< rows.size(); j++){		
					final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
					final List<OLink> links = new ArrayList<OLink>();
					properties.add(OProperties.int32("row_id", j));
					final HashMap<String, String> values = rows.get(j).getValues();
					
					if(queryInfo.select == null || queryInfo.select.size() == 0){
						//se non è specificata la select l'ordine delle colonne viene dato dalla getColumns
						for(int i = 0; i< columns.size(); i++){
							if(values.containsKey(columns.get(i).getPhysicalName())){
								properties.add(OProperties.string(rawNameToUniqueName(ees.getName(), columns.get(i).getLogicalName()), values.get(columns.get(i).getPhysicalName())));							
							}
						}
					}else{
						//altrimenti l'ordine è quello della select
						for(EntitySimpleProperty property : queryInfo.select){
							Column column = findColumn(columns, uniqueNameToRawName(ees.getName(), property.getPropertyName()));
							if(column != null && values.containsKey(column.getPhysicalName())){
								properties.add(OProperties.string(rawNameToUniqueName(ees.getName(), column.getLogicalName()), values.get(column.getPhysicalName())));	
							}
						}
					}
				
					final OEntity newEntity = OEntities.create(ees, OEntityKey.create(j), properties, links);
					list.add(newEntity);
				}
			}
		}catch(OdataDomainException odde){
			StatPortalOData.logger.error(odde.getMessage(), odde);
			return list;
		}
		return list;
	}
	
	/**
	 * Costruisce un'entità di tipo MinMax
	 * @param minMax l'oggetto MinMax da trasformare in entità
	 * @return un'entità MinMax
	 */
	private List<OEntity> buildMinMaxEntities(MinMax minMax) {
		final List<OEntity> list = new ArrayList<OEntity>();
		list.add(MinMaxEntity.createMinMaxEntity(edmDataServices.findEdmEntitySet("MinMax"), minMax.min.doubleValue(), minMax.max.doubleValue()));
		return list;
	}

	/**
	 * Costruisce delle entità basate sulle informazioni delle colonne
	 * @param ees l'entity set richiesto
	 * @param entity l'entità da cui estrarre i dati
	 * @param queryInfo eventuali restrizioni
	 * @return entità basate sulle informazioni delle colonne
	 * @throws OpenDataException
	 */
	private List<OEntity> buildColumnEntities(final EdmEntitySet ees, final OEntity entity, final QueryInfo queryInfo) throws OpenDataException {
		final List<OEntity> list = new ArrayList<OEntity>();
		final Entities entityType = Entities.parse(entity.getEntitySetName());
		if(entityType != Entities.MD_DATA){
			throw new OpenDataException();
		}
		try{
			List<Column> columns = null;		
			IData data = getData(entity, entityType);
			//ottengo la struttura della tabella
			columns = data.getColumns();				
			//controllo se stiamo simulando la count
			if(queryInfo.customOptions.containsKey("count")){
				int total = columns.size();
				return buildCountEntities(total);
			}else{													
				//creo le entità
				for(int j = 0; j< columns.size(); j++){		
					final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
					final List<OLink> links = new ArrayList<OLink>();
					Column currentColumn = columns.get(j);
					properties.add(OProperties.int32("id", currentColumn.getId()));
					properties.add(OProperties.string("physicalName", currentColumn.getPhysicalName()));
					properties.add(OProperties.string("logicalName", currentColumn.getLogicalName()));
					properties.add(OProperties.string("columnType",currentColumn.getType().name()));
					properties.add(OProperties.int32("differentDistinctCount", currentColumn.getDifferentDistinctCount()));
					final OEntity newEntity = OEntities.create(ees, OEntityKey.create(j), properties, links);
					list.add(newEntity);
				}
			}
		}catch(OdataDomainException odde){
			StatPortalOData.logger.error(odde.getMessage(), odde);
			return list;
		}
		return list;
	}
	
	/**
	 * Metodo di appoggio per trovare una colonna dato il suo nome logico
	 * @param columns l'insieme di colonne su cui cercare
	 * @param propertyName il nome che si cerca 
	 * @return la colonna con il relativo nome logico oppure null
	 */
	private Column findColumn(final List<Column> columns, final String propertyName){
		Column result = null;
		for(Column column : columns){
			if(column.getLogicalName().compareTo(propertyName) == 0){
				result = column;
				break;
			}
		}
		return result;
	}

	/**
	 * Fornisce il dato associato ad una entità
	 * @param entity l'entità
	 * @param entityType il tipo dell'entità
	 * @return il dato associato ad una entità
	 * @throws OpenDataException
	 */
	private IData getData(final OEntity entity, final Entities entityType)
			throws OpenDataException {
		IData data = null;
		if(entityType == Entities.DB_TABLE){
			data = DbTable.findDbTable((Integer)entity.getProperty(ID_LITERAL).getValue());
		}else if(entityType == Entities.MD_DATA){
//			if(entity.getEntityKey().asSingleValue() instanceof String){
//				String key = (String) entity.getEntityKey().asSingleValue();
//				data = MdData.findMdData(MdData.getIdFromUid(key.substring(0, key.indexOf('@'))));
//			}else{
				data = MdData.findMdData((Integer)entity.getEntityKey().asSingleValue());
//			}
		}else if(entityType == Entities.MD_HIER_NODE){
			data = MdHierNode.findMdHierNode((Integer)entity.getEntityKey().asSingleValue());
		}else{
			throw new OpenDataException();
		}
		return data;
	}

	/**
	 * Prepara la lista delle misure che devono essere presenti nel risultato con relative funzioni di aggregazione (se specificate)
	 * @param queryInfo le opzioni richieste dall'utente
	 * @return La lista delle misure che devono essere presenti nel risultato con relative funzioni di aggregazione (se specificate)
	 */
	private List<MeasureAggregation> prepareMeasureList(final QueryInfo queryInfo) {
		final List<MeasureAggregation> aggregationList = new ArrayList<MeasureAggregation>();
		if(queryInfo.customOptions.containsKey("measureList")){
			final String selectedMeasures = queryInfo.customOptions.get("measureList");
			if(selectedMeasures != null){
				for(String selectedMeasure : selectedMeasures.split(";")){
					final String[] pair = selectedMeasure.split(":");							
					if(pair.length == 1 || pair.length == 2){
						int id = -1;
						try{
							id = Integer.valueOf(pair[0]);
						}catch(NumberFormatException nfe){
							continue;							
						}
						final MdMeasureFields measure = MdMeasureFields.findMdMeasureFields(id);
						if(measure != null){
							if(pair.length == 1){
								aggregationList.add(new MeasureAggregation(measure));
							}else{
								try{
									aggregationList.add(new MeasureAggregation(measure, AggregateFunctions.valueOf(pair[1])));
								}catch(IllegalArgumentException iae){
									StatPortalOData.logger.error(iae.getMessage(), iae);
									aggregationList.add(new MeasureAggregation(measure));
								}
							}
						}
					}
				}
			}
		}
		return aggregationList;
	}

	/**
	 * Prepara la lista delle dimensioni che devono essere presenti nel risultato della query
	 * @param queryInfo le opzioni richieste dall'utente
	 * @return la lista delle dimensioni che devono essere presenti nel risultato della query
	 */
	private List<MdDataDim> prepareDimensionList(final QueryInfo queryInfo) {
		final List<MdDataDim> dimensionList = new ArrayList<MdDataDim>();
		if(queryInfo.customOptions.containsKey("dimensionList")){
			final String selectedDims = queryInfo.customOptions.get("dimensionList");
			if(selectedDims != null){
				final String[] selectedDimArray = selectedDims.split(";");
				for(String selectedDimension : selectedDimArray){
					int id = -1;
					try{
						id = Integer.valueOf(selectedDimension);

					}catch(NumberFormatException nfe){
						continue;
					}
					final MdDataDim mdDataDim = MdDataDim.findMdDataDim(id);
					if(mdDataDim != null){
						dimensionList.add(mdDataDim);
					}
				}
			}
		}
		return dimensionList;
	}
	
	/**
	 * Prepara la stringa sql della clausola WHERE
	 * @param queryInfo le opzioni richieste dall'utente
	 * @param data un dato, un nodo o una tabella grezza
	 * @return La string sql della clausola WHERE
	 */
	private FilterParameters prepareFilter(final String entitySetName, final QueryInfo queryInfo, IData data) throws OdataDomainException {
		String filter = "";
		List<MdDataDim> filteredDims = null;
		if(queryInfo.filter != null){
			//faccio creare al SqlExpressionVisitor la stringa
			final SqlExpressionVisitor filterVisitor = new SqlExpressionVisitor(entitySetName, data, true);
			queryInfo.filter.visit(filterVisitor);
			filter = filterVisitor.toString();
			filteredDims = filterVisitor.getFilteredDims();
		}
		return new FilterParameters(filter, filteredDims);
	}

	/**
	 * Prepara la stringa sql della clausola ORDER BY
	 * @param queryInfo le opzioni richieste dall'utente
	 * @param columns l'elenco delle colonne della tabella su cui si esegue la query
	 * @return La stringa sql della clausola ORDER BY
	 */
	private String prepareOrderBy(final String entitySetName, final QueryInfo queryInfo, IData data, final List<Column> columns, final boolean where) {
		final List<OrderByExpression> orderByList = queryInfo.orderBy == null ? new ArrayList<OrderByExpression>() : queryInfo.orderBy;
		final StringBuffer orderByBuffer = new StringBuffer();
		//aggiungo tutte le colonne di ordinamento
		for(OrderByExpression expression : orderByList){
			final SqlExpressionVisitor visitor = new SqlExpressionVisitor(entitySetName, data, columns, where);
			expression.visit(visitor);
			orderByBuffer.append(visitor.toString());
			orderByBuffer.append(',');
		}
		String orderBy = "";
		if(orderByBuffer.length() > 0){
			//elimino la virgola finale
			orderBy = orderByBuffer.substring(0, orderByBuffer.length()-1);
		}
		return orderBy;
	}

	/**
	 * Genera una lista con una entità la cui proprietà indicherà la count delle righe di una tabella
	 * Ci serve per simulare la count in attesa della versione 0.6 di odata4j.
	 * @param total il numero di righe
	 * @return  una lista con una entità la cui proprietà indicherà la count delle righe della tabella
	 */
	private List<OEntity> buildCountEntities(final int total) {
		final List<OEntity> list = new ArrayList<OEntity>();
		list.add(TotalEntity.createTotalEntity(edmDataServices.findEdmEntitySet("Totals"), total));
		return list;
	}

	@Override
	public EntityResponse getEntity(final String entitySetName, final OEntityKey entityKey,
			final QueryInfo queryInfo) {
		throw new NotImplementedException();
	}

	@Override
	public BaseResponse getNavProperty(String entitySetName,
			OEntityKey entityKey, String navProp, QueryInfo queryInfo) {
		throw new NotImplementedException();
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
	
	class FilterParameters{
		protected String filter;
		
		protected List<MdDataDim> relatedDims;
		
		public FilterParameters(String filter, List<MdDataDim> relatedDims){
			this.filter = filter;
			this.relatedDims = relatedDims;
		}
		
		public List<MdDataDim> getRelatedDims(){
			return this.relatedDims;
		}
		
		public String getFilter(){
			return this.filter;
		}
	}
	
}

