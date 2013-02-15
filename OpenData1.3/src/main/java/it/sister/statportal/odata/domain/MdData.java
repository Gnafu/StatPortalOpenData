package it.sister.statportal.odata.domain;

import it.sister.statportal.odata.domain.Column.ColumnType;

import it.sister.statportal.odata.domain.IRepository.MinMax;
import it.sister.statportal.odata.domain.IRepository.MinMaxCount;


import it.sister.statportal.odata.domain.MeasureAggregation.AggregateFunctions;
import it.sister.statportal.odata.utility.DBUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.management.openmbean.OpenDataException;
import javax.persistence.PreRemove;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.roo.addon.dbre.RooDbManaged;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.google.gson.internal.StringMap;


/**
 * 
 * Classe che rappresenta un dato strutturato
 * 
 */
@RooJavaBean
@RooToString
@RooEntity(versionField = "", table = "MD_DATA", schema = "public")
@RooDbManaged(automaticallyDelete = true)
public class MdData implements IData {

    public static String COUNT_STAR_FIELD = "v_el_count";
    
    /**
     * Costruttore
     */
    public MdData() {

    }

    /**
     * Costruttore completo (null per lasciare i campi incompleti)
     * 
     * @param available
     * @param dbName
     *        nome del database
     * @param description
     *        descrizione del dato
     * @param genericGrants
     * @param idLuDataType
     * @param idMetadata
     *        id del metadato
     * @param idOwnerUser
     *        id del proprietario del dato
     * @param lastUpdate
     *        data di ultimo aggiornamento del dato
     * @param name
     *        nome
     * @param numRows
     *        numero di righe
     * @param tableName
     *        nome della tabella
     */
    public MdData(Boolean available, String dbName, String description,
	    Boolean genericGrants, Integer idLuDataType, Integer idMetadata,
	    Integer idOwnerUser, Date lastUpdate, String name, Integer numRows, String tableName) {
	setAvailable(available);
	setDbName(dbName);
	setDescription(description);
	setGenericGrants(genericGrants);
	setIdLuDataType(idLuDataType);
	setIdMetadata(idMetadata);
	setIdOwnerUser(idOwnerUser);
	setLastUpdate(lastUpdate);
	setName(name);
	setNumRows(numRows);
	setTableName(tableName);
    }

    @Override
    public List<Column> getColumns() throws OdataDomainException {

	CheckTableNameNotNull();
	// la lista delle colonne sarà l'unione di tutte le misure e le
	// dimensioni del dato
	
	Set<MdDataDim> tmpDims = this.getMdDataDims();
	Set<MdMeasureFields> tmpMeas = this.getMdMeasureFieldss();
	Set<MdGenericColumn> tmpGenericCol = this.getMdGenericColumns();

	// si fa la query per vedere tutte le colonne della tabella (fisiche) e
	// si aggiungono quelle colonne che non sono ne misure ne dimensioni
	List<Column> allColumns = RepositoryFactory.getRepository()
		.getTableColumns(this.getTableName());

	int idx = 0;
	// SI SCORRONO LE COLONNE RECUPERATE DAL DB PERCHE' DEV'ESSERE RISPETTATO L'ORDINE
	for (Column c : allColumns){
	    // booleano per indicare se la colonna è stata trovata oppure no
	    boolean found = false;
	    
	    // si cerca tra le dimensioni
	    for (MdDataDim cDim : tmpDims){
		if(c.getPhysicalName().equalsIgnoreCase(cDim.getDimcodeField())){
		    c.setId(cDim.getId());
		    c.setPhysicalName(cDim.getDimcodeField().toLowerCase());
		    c.setLogicalName(cDim.getAlias());
		    c.setType(ColumnType.DIMENSION);
		    c.setDifferentDistinctCount(cDim.getDifferentDistinctCount());
		    found = true;
		    break;
		}
	    }
	    
	    // si cerca tra le misure
	    if(!found){
		 for (MdMeasureFields cMeas : tmpMeas){ 
		     if(c.getPhysicalName().equalsIgnoreCase(cMeas.getMeasureField())){
			 c.setId(cMeas.getId());
			 c.setPhysicalName(cMeas.getMeasureField().toLowerCase());
			 c.setLogicalName(cMeas.getAlias());
			 c.setType(ColumnType.MEASURE);
			 c.setDifferentDistinctCount(null);
			 found = true;
			 break;
		     } 
		 }
	    }
	    
	    // si cerca tra le colonne generiche
	    if(!found){
		 for (MdGenericColumn cGenericCol : tmpGenericCol){ 
		     if(c.getPhysicalName().equalsIgnoreCase(cGenericCol.getColumnField())){
			 c.setId(cGenericCol.getId());
			 c.setPhysicalName(cGenericCol.getColumnField().toLowerCase());
			 c.setLogicalName(cGenericCol.getAlias());
			 c.setType(ColumnType.GENERIC_COLUMN);
			 c.setDifferentDistinctCount((cGenericCol.getDifferentDistinctCount()));
			 found = true;
			 break;
		     } 
		 }
	    }
	    
	    if(!found){
		c.setId(idx);
		c.setType(ColumnType.OTHER);
	    }
	    
	    idx++;
	}
	 
	return allColumns;
    }

    @Override
    public List<Row> getRows() throws OdataDomainException {
	CheckTableNameNotNull();
	return RepositoryFactory.getRepository().getTableRows(
		this.getTableName(), getColumns());
    }

    @Override
    public List<Row> getRows(int startIndex, int count, String filter)
	    throws OdataDomainException {
	CheckTableNameNotNull();
	return RepositoryFactory.getRepository().getTableRows(
		this.getTableName(), getColumns(), startIndex, count, filter);
    }

    @Override
    public int getCountRows() throws OdataDomainException {
	return getCountRows(null);
    }

    @Override
    public int getCountRows(String filter) throws OdataDomainException {
	CheckTableNameNotNull();
	
	return RepositoryFactory.getRepository().getCountRows(
		this.getTableName(), new ArrayList<MdDataDim>(this.getMdDataDims()), null, -1, -1, false, filter, null, null, false);
    }

    /**
     * Restituisce tutte le righe di una tabella strutturata (con le
     * informazioni leggibili dal viewer; nomi fisici sostituiti dai logici)
     * 
     * @param locale localizzazione
     * @return l'insieme delle righe
     * @throws OdataDomainException
     */
    public List<Row> getRowsForViewer(Locale locale) throws OdataDomainException {
	return getRowsForViewer(-1, -1, null, null, locale);
    }

    /**
     * Restituisce un insieme di righe della tabella filtrate e ordinate
     * 
     * @param startIndex
     *        indice di partenza (0 indica il primo record)
     * @param count
     *        numero di risultati
     * @param filterExpression
     *        espressione testuale che rappresenta il filtro sui dati
     * @param orderBy
     *        espressione testuale che rappresenta l'ordinamento sui dati
     * @param locale	localizzazione
     * @return l'insieme di righe
     * @throws OdataDomainException
     */
    public List<Row> getRowsForViewer(int startIndex, int count,
	    String filterExpression, String orderBy, Locale locale)
	    throws OdataDomainException {

	CheckTableNameNotNull();

	// si scorrono tutte le dimensioni per controllare se mancano le
	// informazioni relative alle tabelle
	for (MdDataDim dim : this.getMdDataDims()) {
	    if (dim.getIdHierNode() == null
		    || dim.getIdHierNode().getTableName() == null) {
		throw new OdataDomainException(
			"Nome della tabella relativa alla dimensione "
				+ dim.getAlias() + " non presente.");
	    }
	}

	// si crea una lista di MeasureAggregation (in questo caso non avremmo
	// le funzioni di aggregazioni in quanto non si tratta di
	// raggruppamento)
	List<MeasureAggregation> measureAggregation = new ArrayList<MeasureAggregation>();
	for (MdMeasureFields measure : this.getMdMeasureFieldss()) {
	    measureAggregation.add(new MeasureAggregation(measure));
	}

	// si fa la query
	return RepositoryFactory.getRepository().getTableRows(
		this.getTableName(), getColumns(),
		new ArrayList<MdDataDim>(this.getMdDataDims()),
		measureAggregation, startIndex, count, false, filterExpression,
		orderBy, null, locale);
    }

    /**
     * Permette di fare un raggruppamento dei dati
     * 
     * @param measureAggregation
     *        lista di misure e relative funzioni di aggregazione
     * @param dimensions
     *        lista delle dimensioni sulle quale si vuole raggruppare
     * @return l'insieme dei risultati del raggruppamento
     * @throws OdataDomainException
     */
    public List<Row> getRowsAggregated(List<MdDataDim> dimensions,
	    List<MeasureAggregation> measureAggregation)
	    throws OdataDomainException {
	return getRowsAggregated(dimensions, measureAggregation, -1, -1, null,
		null, null);
    }

    private Tuple<List<MdDataDim>, List<MeasureAggregation>, Boolean> cleanDimensionAndAggregation(
	    List<MdDataDim> dimensions,
	    List<MeasureAggregation> measureAggregation, boolean groupByEnabled) {


	// Se non viene passata alcuna dimensione non si fa il 
	// raggruppamento
	if (dimensions.size() == 0) {
	    groupByEnabled = false;
	}

	// visto che si raggruppa bisogna verificare che tutte le misure che
	// vengono passate abbiano la funzione di aggregazione. Altrimenti
	// mettiamo la SUM di default.
	for (MeasureAggregation singleMeasureAggregation : measureAggregation) {
	    if (singleMeasureAggregation.getAggregateFunc() == null) {
		singleMeasureAggregation
			.setAggregateFunc(AggregateFunctions.SUM);
	    }
	}

	return new Tuple<List<MdDataDim>, List<MeasureAggregation>, Boolean>(
		dimensions, measureAggregation, groupByEnabled);

    }

    /**
     * Permette di fare un raggruppamento dei dati (con paginazione, filtri e
     * ordinamento). Se dimensioni e misure non sono presenti (entrambe null o
     * lista vuota) vengono visualizzate tutte (quindi è come chiamare la
     * getRowsForViewer). Se qualche misura non ha la relativa funzione di
     * aggregazione viene aggiunta la SUM di default
     * 
     * @param measureAggregation
     *        insieme di misure e relative funzioni di aggregazione
     * @param dimensions
     *        lista delle dimensioni sulle quale si vuole raggruppare
     * @param startIndex
     *        indice di partenza (0 per indicare il primo record)
     * @param count
     *        (numero di risultati)
     * @param filterExpression
     *        espressione testuale che rappresenta un filtro
     * @param orderby
     *        espressione testuale che rappresenta l'ordinamento dei dati
     * @return l'insieme delle righe
     * @throws OdataDomainException
     */
    public List<Row> getRowsAggregated(List<MdDataDim> dimensions,
	    List<MeasureAggregation> measureAggregation, int startIndex,
	    int count, String filterExpression, String orderby, List<MdDataDim> joinedDimensions)
	    throws OdataDomainException {

	CheckTableNameNotNull();
	boolean groupByEnabled = true;

	// se non ci sono dimensioni ne misure si chiama la getRowsForViewers
	if ((dimensions == null || dimensions.size() == 0)
		&& (measureAggregation == null || measureAggregation.size() == 0)) {
	    return getRowsForViewer(startIndex, count, filterExpression,
		    orderby, null);
	}

	// si fa la clean (che implementa la logica descritta nella
	// documentazione del metodo
	Tuple<List<MdDataDim>, List<MeasureAggregation>, Boolean> cleaned = cleanDimensionAndAggregation(
		dimensions, measureAggregation, groupByEnabled);

	// si fa la query
	return RepositoryFactory.getRepository().getTableRows(
		this.getTableName(), getColumns(), cleaned.getX(),
		cleaned.getY(), startIndex, count, cleaned.getZ(),
		filterExpression, orderby, joinedDimensions, null);
    }

    /**
     * Crea la vista dei dati aggregati per una particolare dimensione territoriale (con eventuali filtri temporali)
     * Alle misure del dato viene sempre aggiunta la count(*) in modo da avere la statistica sul numero di elementi
     * @param territorialDimension	dimensione territoriale
     * @param temporalDimension 	dimensione temporale
     * @param temporalDimensionValue	valore della dimensione temporale
     * @return il nome della vista creata
     */
    public String createViewForMap(MdDataDim territorialDimension, MdDataDim temporalDimension, String temporalDimensionValue) {
	try {
	    if (territorialDimension == null) {
    		return null;
	    }
	    
	    boolean existFilter = temporalDimension != null && temporalDimensionValue != null && !temporalDimensionValue.isEmpty();
	    String filterExpression = "";
	    // se è stato passato un filtro si compone l'espressione
	    if(existFilter){
		filterExpression = temporalDimension.getDimcodeField().toLowerCase() + "=" + temporalDimensionValue;
	    }
	    
    	    String viewName = "vm_" + this.getId().toString() + "_" + territorialDimension.getId().toString() + ((existFilter) ? ("_" + temporalDimension.getId().toString() + "_" + temporalDimensionValue.trim()) : "") ;
    	
    	    // se esiste già una vista per questo se ne restituisce direttamente il nome, altrimenti si crea
    	    if(DBUtils.viewExists(viewName)){
    		return viewName;
    	    }
    	    
    	    
    	    List<MeasureAggregation> measureList = new ArrayList<MeasureAggregation>();

	    // si aggiunge una misura 'particolare', che sarebbe la count(*)
	    MdMeasureFields countStarMeasure = new MdMeasureFields(COUNT_STAR_FIELD, "", null, COUNT_STAR_FIELD, null, null, null);
	    MeasureAggregation countStar = new MeasureAggregation(countStarMeasure);
	    measureList.add(countStar);
    	    
	    // come funzione di aggregazione mettiamo la SUM di default
	    for (MdMeasureFields measure : this.getMdMeasureFieldss()) {
		measureList.add(new MeasureAggregation(measure,
			AggregateFunctions.SUM));
	    }

	    ArrayList<MdDataDim> mdDataDim = new ArrayList<MdDataDim>();
	    mdDataDim.add(territorialDimension);

	    // si crea la view
	    RepositoryFactory.getRepository().createView(this.getTableName(),
		    viewName, mdDataDim, measureList,
		    true, filterExpression);

	    return viewName;

	} catch (Exception e) {
	    return null;
	}
    }
    
    /***
     * Crea una vista con una colonna 'seriale'
     * @param viewName	nome della vista da creare
     * @param forceCreation	se true forza la creazione della vista anche se già esistente (serve nel caso in cui il dato sia stato aggiornato)
     * @param uidName	nome fisico della colonna 'seriale'
     * @throws OdataDomainException
     */
    public void createViewWithSerialUID(String viewName, boolean forceCreation, String uidName) throws OdataDomainException{
	boolean viewExists = DBUtils.viewExists(viewName);
	if(forceCreation || !viewExists){
	    if(viewExists){
		DBUtils.dropView(viewName);
	    }
	    List<String> columns = new ArrayList<String>();
	    String physicalName;
	    for(Column c : this.getColumns()){
		physicalName = c.getPhysicalName(); 
		if(!physicalName.equalsIgnoreCase("the_geom")){
		    columns.add(c.getPhysicalName());
		}
	    }
	    RepositoryFactory.getRepository().createView(viewName, this.getTableName().toLowerCase(), columns, uidName);
	}
    }
    
    /**
     * 
     * @return true se esiste una chiave primaria sulla tabella associata
     */
    public boolean pkEsists(){
	return DBUtils.primaryKeyExists(this.getTableName().toLowerCase());
    }

    /**
     * Espone il count del risultato dell'aggregazione
     * @param mdDataDims		lista delle dimensioni sulle quali raggruppare
     * @param measureAggregation	misure aggregate
     * @param filterExpression		filtri
     * @param orderBy			ordinamenti
     * @param joinedDimensions		dimensioni in join
     * @param excludeNullByCount	esclude i valori null
     * @return il numero di righe del risultato
     * @throws OdataDomainException
     */
    public int getCountRowsAggregated(List<MdDataDim> mdDataDims,
	    List<MeasureAggregation> measureAggregation,
	    String filterExpression, String orderBy, List<MdDataDim> joinedDimensions, boolean excludeNullByCount)
	    throws OdataDomainException {

	CheckTableNameNotNull();

	Tuple<List<MdDataDim>, List<MeasureAggregation>, Boolean> cleaned = cleanDimensionAndAggregation(
		mdDataDims, measureAggregation, true);

	return RepositoryFactory.getRepository().getCountRows(
		this.getTableName(), cleaned.getX(), cleaned.getY(), -1, -1,
		cleaned.getZ(), filterExpression, orderBy, joinedDimensions, excludeNullByCount);
    }
    
    /**
     * Espone il minimo e il massimo delle misure passate tra i parametri raggruppate sulle dimensioni indicate (con eventuali filtri)
     * @param mdDataDims		dimensioni sulle quali raggruppare
     * @param measureAggregation	misure aggregate
     * @param filterExpression		eventuali filtri
     * @param joinedDimensions		dimensioni in join
     * @return	il minimo e il massimo 
     */
    public MinMax getMinMaxForAggregatedMeasure(List<MdDataDim> mdDataDims, MeasureAggregation measureAggregation, String filterExpression, List<MdDataDim> joinedDimensions){
	
	return RepositoryFactory.getRepository().getMinMaxForAggregatedMeasure(this.getTableName(), mdDataDims, measureAggregation, -1, -1, true, filterExpression, null, joinedDimensions, true);
    }

    /**
     * Restituisce la dimensione associata al dato con l'id passato come
     * parametro
     * 
     * @param id
     *        id della dimensione richiesta
     * @return la dimensione richiesta
     */
    public MdDataDim getMdDataDim(Integer id) {
	MdDataDim dimension = null;
	for (MdDataDim dim : this.getMdDataDims()) {
	    if (dim.getId().intValue() == id) {
		return dim;
	    }
	}
	return dimension;
    }

    /**
     * Restituisce la misura associata al dato con l'id passato come parametro
     * 
     * @param id
     *        id della misura richiesta
     * @return la misura richiesta
     */
    public MdMeasureFields getMdMeasureField(Integer id) {
	MdMeasureFields measure = null;
	for (MdMeasureFields meas : this.getMdMeasureFieldss()) {
	    if (meas.getId().intValue() == id) {
		return meas;
	    }
	}
	return measure;
    }
    
    /**
     * Espone la colonna generica con l'id indicato
     * @param id
     * @return la colonna generica
     */
    public MdGenericColumn getMdGenericColumn(Integer id) {
	for (MdGenericColumn col : this.getMdGenericColumns()) {
	    if (col.getId().intValue() == id) {
		return col;
	    }
	}
	return null;
    }

    /**
     * Funzione di utilità che controlla che il dato abbia impostato il nome
     * della tabella. Se non impostato lancia eccezione
     * 
     * @throws OdataDomainException
     */
    private void CheckTableNameNotNull() throws OdataDomainException {
	if (this.getTableName() == null) {
	    throw new OdataDomainException(
		    "Nome della tabella del dato non definita");
	}
    }

    @PreRemove
    void onPreRemove() {
	// si cancellano tutte le dimensioni associate al dato
	for (MdDataDim dim : getMdDataDims()) {
	    dim.remove();
	    dim.flush();
	}

	// si cancellano tutte le misure associate al dato
	for (MdMeasureFields measure : getMdMeasureFieldss()) {
	    measure.remove();
	    measure.flush();
	}
	
	// si cancellano tutte le colonne generiche associate al dato
	for (MdGenericColumn genericColumn : getMdGenericColumns()){
	    genericColumn.remove();
	    genericColumn.flush();
	}
	
	try {
	    // si cancella la riga nella tabella geometry_columns (se presente)
	    Integer idLuDataType = this.getIdLuDataType();
	    if (idLuDataType != null
		    && (idLuDataType.intValue() == 1 || idLuDataType.intValue() == 4)) {
		DBUtils.deleteFromGeometry_columns(this.getTableName());
	    }
	} catch (Exception e) {
	    // non importa
	}
    }

    /**
     * Restituisce la prima dimensione territoriale del dato, se presente. null
     * altrimenti
     * 
     * @return	la prima dimensione territoriale del dato, se presente. NULL altrimenti
     */
    private MdDataDim getFirstTerritorialDimension() {
	MdDataDim territorialDimension = null;
	for (MdDataDim dim : this.getMdDataDims()) {
	    if (dim.getIdHierNode().getIdHierarchy().getIdLuHierType() == 1) {
		territorialDimension = dim;
		break;
	    }
	}
	return territorialDimension;
    }

    /**
     * Restituisce il nome del layer associato alla prima dimensione
     * territoriale, se presente. null altrimenti
     * 
     * @return	il nome del layer associato alla prima dimensione
     * territoriale, se presente. null altrimenti
     */
    public String getMapLayerName() {
	String mapLayerName = null;
	MdDataDim territorialDimension = getFirstTerritorialDimension();
	if (territorialDimension != null) {
	    mapLayerName = territorialDimension.getMapLayerName();
	}

	return mapLayerName;
    }

    /**
     * Restituisce il path del layer associato alla prima dimensione
     * territoriale, se presente. Null altrimenti
     * 
     * @return	il path del layer associato alla prima dimensione
     * territoriale, se presente. Null altrimenti
     */
    public String getMapLayerPath() {
	String mapLayerPath = null;
	MdDataDim territorialDimension = getFirstTerritorialDimension();
	if (territorialDimension != null) {
	    mapLayerPath = territorialDimension.getMapLayerPath();
	}

	return mapLayerPath;
    }

    /**
     * Restituisce il campo di aggancio al nodo associato alla prima dimensione
     * territoriale, se presente. Null altrimenti
     * 
     * @return	il campo di aggancio al nodo associato alla prima dimensione
     * territoriale, se presente. Null altrimenti
     */
    public String getMapLayerField() {
	String mapLayerField = null;
	MdDataDim territorialDimension = getFirstTerritorialDimension();
	if (territorialDimension != null) {
	    mapLayerField = territorialDimension.getMapLayerField();
	}

	return mapLayerField;
    }

    /**
     * Restituisce il campo di aggancio al layer associato alla prima dimensione
     * territoriale, se presente. Null altrimenti
     * 
     * @return	il campo di aggancio al layer associato alla prima dimensione
     * territoriale, se presente. Null altrimenti
     */
    public String getMapNodeField() {
	String mapNodeField = null;
	MdDataDim territorialDimension = getFirstTerritorialDimension();
	if (territorialDimension != null) {
	    mapNodeField = territorialDimension.getMapNodeField();
	}

	return mapNodeField;
    }

    @Override
    public String getDescriptionField(String physicalName) {
	String descriptionField = null;
	boolean foundInDimensions = false;
	for (MdDataDim dim : this.getMdDataDims()) {
	    // solo se si tratta di una dimensione si cambia
	    if (dim.getDimcodeField().toLowerCase().equals(physicalName)) {
		/*
		descriptionField = dim.getIdHierNode().getTableName()
			.toLowerCase()
			+ ".\""
			+ dim.getIdHierNode().getDescField().toLowerCase()
			+ "\"";
		 */
		descriptionField = "\"a_" + dim.getDimcodeField().toLowerCase()
			+ "\".\""
			+ dim.getIdHierNode().getDescField().toLowerCase()
			+ "\"";
		foundInDimensions = true;
		break;
	    }
	}

	if(foundInDimensions){
	    return descriptionField;
	}else{
	    return "\"" + getTableName() + "\".\"" + physicalName + "\"";
	}
	
    }
    
    /**
     * Genera il kml di un dato di tipo shapeFile
     * @return la stringa che rappresenta il kml
     */
    public String getKml(){
	
	try{
	    if(getIdLuDataType() == 1){
    	    	String geomField = "the_geom";
    	    	String pointField = geomField + "_point";
            	List<Map<String, Object>> result = RepositoryFactory.getRepository().getKmlPoints(this, geomField, pointField);
            	
            	// si legge lo stile da file di configurazione
            	BeanFactory ctx = new XmlBeanFactory(new ClassPathResource("beans.xml"));
    		String hrefIcon = (String) ctx.getBean("hrefIcon");
            	String colorLineStyle = (String) ctx.getBean("colorLineStyle");
            	String widthLineStyle = (String) ctx.getBean("widthLineStyle");
            	String colorPolyStyle = (String) ctx.getBean("colorPolyStyle");
            	// massimo numero di risultati per cui sarà presente anche la descrizione
            	int maxNumElementsForKmlDescription = ((Integer) ctx.getBean("maxNumElementsForKmlDescription")).intValue();
            	// numero massimo di elementi per il kml
            	int maxNumeElementsForKml = ((Integer) ctx.getBean("maxNumeElementsForKml")).intValue();
            	
            	StringBuilder sb = new StringBuilder();
            	sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            	sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">");
            	sb.append("<Document>");
            	sb.append("<Style id=\"customStyle\">");
            	sb.append("<IconStyle><Icon><href>" + hrefIcon + "</href></Icon></IconStyle>");
            	sb.append("<LineStyle><color>" + colorLineStyle + "</color><width>" + widthLineStyle + "</width></LineStyle>");
            	sb.append("<PolyStyle><color>" + colorPolyStyle + "</color></PolyStyle>");
            	sb.append("</Style>"); 
            	
            	// formattazione numeri
            	DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance();
        	
            	boolean MAX_DESCRIPTION_EXCEDEED = result.size() > maxNumElementsForKmlDescription;
            	int index = 0;
            	for (Map<String, Object> row : result) {
            	    if(index > maxNumeElementsForKml){
            		break;
            	    }
            	    sb.append("<Placemark>");
            	    boolean almostOnePropertiesSet = false;
            	    
            	    for (String key : row.keySet()) {
            		String value = (row.get(key) != null ? row.get(key).toString() : "");
            		if(!value.equals("") && !key.equals(geomField)){
                    		if(key.equals(pointField)){
                        	            sb.append(value);
                    		}else{
                    		    if(!MAX_DESCRIPTION_EXCEDEED){
                            		    // se è la prima proprietà aggiungiamo il tag description, altrimenti aggiungiamo semplicemente la proprietà
                            		    if(!almostOnePropertiesSet){
                            			sb.append("<description>");
                            		    }
                            		    // inserire la proprietà
                            		    String propertiesKey = key;
                            		    
                            		    // formattazione in caso di numero 
                            		    try{
                            			value = formatter.format(Double.parseDouble(value));
                            		    }catch(Exception e){
                            			// non era un numero e non si formatta
                            		    }
                            		    sb.append(StringEscapeUtils.escapeXml("<b>" + propertiesKey + ": </b>" + value + "<br />"));
                            		    almostOnePropertiesSet = true;
                    		    }
                    		}
            		}
            	    }
            	    if(almostOnePropertiesSet){
    		        sb.append("</description>");
    		     }
        		    sb.append("<styleUrl>#customStyle</styleUrl>");
            	    sb.append("</Placemark>");
            	    index++;
            	}
            	
            	sb.append("</Document>");
            	sb.append("</kml>");
            	return sb.toString();
    	   }else{
    	       return "L'MDData non è di tipo shape file";
    	   }
	}catch(Exception ex){
	    return ex.getMessage();
	}
    }
    
    /**
     * Espone tutti i valori distinti (con i relativi count) per un singolo campo
     * @param field		campo sul quale fare la distinct
     * @param skip		record dal quale partire
     * @param top		quanti record restituire
     * @param fieldToSort	ordinamento (per count o etichetta)
     * @param sortingDirection	direzione dell'ordinamento
     * @param filter		eventuali filtri
     * @return	tutti i valori distinti (con i relativi count) per un singolo campo
     * @throws OdataDomainException
     */
    private List<Map<String, Object>> getDistinctValueAndCountForField(String field, int skip, int top, FIELD_TO_SORT fieldToSort, SORTING_DIRECTION sortingDirection, String filter) throws OdataDomainException{
	
	String joinTableName = null; 
	String joinFieldName = null;
	String joinKeyName = null;
	// si deve capire se il field corrisponde ad una dimensione si restituisce l'alias
	for (MdDataDim dim : this.getMdDataDims()){
	    if(dim.getDimcodeField().equalsIgnoreCase(field)){
		joinTableName = dim.getIdHierNode().getTableName();
		joinKeyName = dim.getIdHierNode().getPkField();
		joinFieldName = dim.getIdHierNode().getDescField();
		break;
	    }
	}
	
	return RepositoryFactory.getRepository().getDistinctValueAndCountForField(this.getTableName(), field, joinTableName, joinFieldName, joinKeyName, skip, top, fieldToSort, sortingDirection, filter);
    }
    
    /**
     * Espone il minimo e il massimo del count per i valori distinti di un particolare campo 
     * @param field	nome del campo
     * @param filter	eventuali filtri
     * @return	il minimo e massimo count dei valori distinti
     * @throws OdataDomainException
     */
    public MinMax getMinMaxCountForDistinctValueAndCountForField(String field, String filter) throws OdataDomainException{
	String joinTableName = null; 
	String joinFieldName = null;
	String joinKeyName = null;
	// si deve capire se il field corrisponde ad una dimensione si restituisce l'alias
	for (MdDataDim dim : this.getMdDataDims()){
	    if(dim.getDimcodeField().equalsIgnoreCase(field)){
		joinTableName = dim.getIdHierNode().getTableName();
		joinKeyName = dim.getIdHierNode().getPkField();
		joinFieldName = dim.getIdHierNode().getDescField();
		break;
	    }
	}
	return RepositoryFactory.getRepository().getMinMaxCountForDistinctValueAndCountForField(this.getTableName(), field, joinTableName, joinFieldName, joinKeyName, 0, -1, FIELD_TO_SORT.COUNT, SORTING_DIRECTION.ASC, filter);
    }
    
    /**
     * Restituisce i valori distinti (con relativa count) del dato raggruppato sulla colonna indicata
     * @param field	colonna sul quale fare il raggruppamento
     * @param fieldToSort	ordinamento (per campo o count)
     * @param sortingDirection	direzione dell'ordinamento (ASC o DESC)
     * @return	l'insieme dei valori distinti
     * @throws OdataDomainException
     */
    public List<Map<String, Object>> getDistinctValueAndCountForField(String field, FIELD_TO_SORT fieldToSort, SORTING_DIRECTION sortingDirection) throws OdataDomainException{
	return getDistinctValueAndCountForField(field, 0, -1, fieldToSort, sortingDirection, null);	
    }
    
    /**
     * Restituisce i valori distinti (con relativa count) del dato raggruppato sulla colonna indicata, eventualmente paginati
     * @param field	colonna sul quale fare il raggruppamento
     * @param skip	risultati da saltare (per la paginazione)
     * @param top	numero di risultati
     * @param othersValue	raggruppamento di valori in 'altro'
     * @param fieldToSort	definizione dell'ordinamento (label o count?)
     * @param sortingDirection	direzione dell'ordinamento
     * @param filter eventuali filtri 
     * @return i valori distinti (con relativa count) del dato raggruppato sulla colonna indicata, eventualmente paginati
     * @throws OdataDomainException
     */
    public List<Map<String, Object>> getDistinctValueAndCountForField(String field, int skip, int top, boolean othersValue, FIELD_TO_SORT fieldToSort, SORTING_DIRECTION sortingDirection, String filter) throws OdataDomainException{
	
	if(top < 0){
	    throw new OdataDomainException("top deve essere un intero positivo");
	}
	
	if(othersValue){
	    // vanno richiesti tutti e poi va calcolato il valore di 'altri'
	    List<Map<String, Object>> tmpResult = getDistinctValueAndCountForField(field, skip, top, fieldToSort, sortingDirection, filter);
	    List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
	    int count = 0;
	    int others = 0;
	    // c'è almeno un risultato che è andato a formare la riga 'altri'
	    boolean othersNeeded = false;
	    
	    for (Map<String, Object> row : tmpResult) {
		if(count < top){
		    // si aggiunge ai risultati
		    result.add(row);
		}else{
		    // si aggiunge al risultato altri
		    othersNeeded = true;
		    others += Integer.parseInt(row.get("occurrences").toString());
		}
		
		count++;
	    }
	    
	    if(othersNeeded){
		Map<String, Object> othersRow = new HashMap<String, Object>();
		othersRow.put("label", "Altri");
		othersRow.put("occurrences", others);
		result.add(othersRow);
	    }
	    
	    return result;
	}else{
	    // si richiedono solo i top
	    return getDistinctValueAndCountForField(field, skip, top, fieldToSort, sortingDirection, filter);
	}
    }
    
    
    /**
     * Restituisce i valori distinti (con relativa count) del dato raggruppato sulla colonna indicata. 
     * Alcuni parametri servono per gestire la politica di raggruppamento degli elementi (es. definire un numero minimo e massimo
     * di risultati, definire una percentuale minima affinchè l'elemento sia presente tra i risultati, ecc..)
     * 
     * @param field	colonna sul quale fare il raggruppamento
     * @param minPercentage	percentuale minima affinchè l'elemento sia presente tra i risultati
     * @param minResults	minimo numero di risultati
     * @param maxResults	massimo numero di risultati
     * @param othersValue	presenza dell'elemento 'Altro', contenente il raggruppamento dei risultati esclusi 
     * @param fieldToSort	ordinamento
     * @param sortingDirection	direzione dell'ordinamento
     * @return	la lista di elementi distinti con relativa count
     * @throws OdataDomainException
     */
    public List<Map<String, Object>> getDistinctValueAndCountForField(String field, double minPercentage, int minResults, int maxResults, boolean othersValue, FIELD_TO_SORT fieldToSort, SORTING_DIRECTION sortingDirection) throws OdataDomainException{
	
	if(minPercentage < 0){
	    throw new OdataDomainException("minPercentage deve essere un intero positivo");
	}
	
	// si richiedono tutti
	List<Map<String, Object>> queryResults = getDistinctValueAndCountForField(field, 0, -1, fieldToSort, sortingDirection, null);
	
	return composeResultsForPie1(queryResults, minPercentage, minResults, maxResults, othersValue, "occurrences", "label");
    }
    
    /**
     * Lista di righe del dato aggregate (ed eventualmente filtrate e/o ordinate)
     * 
     * @param dimensions	dimensioni sulle quali raggruppare
     * @param measureAggregation	lista di misure e relative funzioni di aggregazioni da applicare
     * @param startIndex	indice di partenza (per paginazione)
     * @param count	numero di risultati (per paginazione)
     * @param filterExpression	filtri
     * @param orderby	ordinamenti
     * @param joinedDimensions	dimensioni da aggiungere in join per risolverne i nomi
     * @param minPercentage	minima percentuale sotto la quale vengono esclusi i risultati
     * @param minResults	minimo numero di risultati
     * @param maxResults	massimo numero di risultati
     * @param othersValue	presenza dell'elemento 'Altro'
     * @return	la lista di righe che rispettano le condizioni specificate
     * @throws OdataDomainException
     */
    public List<Row> getRowsAggregated(List<MdDataDim> dimensions,
	    List<MeasureAggregation> measureAggregation, int startIndex,
	    int count, String filterExpression, String orderby, List<MdDataDim> joinedDimensions, 
	    double minPercentage, int minResults, int maxResults, boolean othersValue)
	    throws OdataDomainException {
	
	List<Row> rows = getRowsAggregated(dimensions, measureAggregation, startIndex, 
		count, filterExpression, orderby, joinedDimensions);
	
	if(measureAggregation.size() != 1 && dimensions.size() != 1){
	    throw new OdataDomainException("Per questa funzionalità deve essere impostata una dimensione ed una misura");
	}
	
	return composeResultsForPie2(rows, minPercentage, minResults, maxResults, othersValue, measureAggregation.get(0).getMeasure().getMeasureField().toLowerCase(), dimensions.get(0).getDimcodeField().toLowerCase());
    }
    
    private List<Row> composeResultsForPie2(List<Row> queryResults, double minPercentage, int minResults, int maxResults, boolean othersValue, String fieldName, String labelName) throws OdataDomainException{
	
	List<Row> result = new ArrayList<Row>();
	
	// si calcola il totale
	double total = 0;
	for (Row row : queryResults) {
	    if(!row.getValue(fieldName).toString().equalsIgnoreCase("")){
		double parsed = Double.parseDouble(row.getValue(fieldName).toString());
		if(parsed < 0){
			throw new OdataDomainException("Valori negativi per il calcolo del totale sulla torta");
		}
		total += parsed;
	    }
	}
	
	double occurrences = 0;
	double percentage = 0;
	double partial = 0;
	boolean othersNeeded = false;
	int count = 0;
	
	for (Row row : queryResults) {
	    
	    if(!row.getValue(fieldName).toString().equalsIgnoreCase("")){
        	    occurrences = Double.parseDouble(row.getValue(fieldName).toString());
                    
        	    percentage = ((occurrences*100)/total);
        	    if(percentage > minPercentage || count < minResults){
        		if(count>=maxResults){
        		    othersNeeded = true;
        		    break;
        		}else{
        		    result.add(row);
        		    partial += occurrences;
            		    count++;
        		}
        	    }else{
        		othersNeeded = true;
        		break;
        	    }
	    }
	}
	
	if(othersValue && othersNeeded){
	    Row othersRow = new Row();
	    othersRow.addValue(labelName, "Altro");
	    othersRow.addValue(fieldName, Double.toString(total - partial));
	    
	    result.add(othersRow);
	}
	return result;
    }
    
    private List<Map<String, Object>> composeResultsForPie1(List<Map<String, Object>> queryResults, double minPercentage, int minResults, int maxResults, boolean othersValue, String fieldName, String labelName) throws OdataDomainException{
	
	List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
	
	// si calcola il totale
	int total = 0;
	for (Map<String, Object> row : queryResults) {
	    int parsed = Integer.parseInt(row.get(fieldName).toString());
	    if(parsed < 0){
		throw new OdataDomainException("Valori negativi per il calcolo del totale sulla torta");
	    }
	    total += parsed;
	}
	
	int occurrences = 0;
	double percentage = 0;
	int partial = 0;
	boolean othersNeeded = false;
	int count = 0;
	
	for (Map<String, Object> row : queryResults) {
	    occurrences = Integer.parseInt(row.get(fieldName).toString());
            
	    percentage = ((occurrences*100)/total);
	    if(percentage > minPercentage || count < minResults){
		if(count>=maxResults){
		    othersNeeded = true;
		    break;
		}else{
		    result.add(row);
		    partial += occurrences;
    		    count++;
		}
	    }else{
		othersNeeded = true;
		break;
	    }
	}
	
	if(othersValue && othersNeeded){
	    Map<String, Object> othersRow = new HashMap<String, Object>();
	    othersRow.put(labelName, "Altro");
	    othersRow.put(fieldName, total - partial);
	    result.add(othersRow);
	}
	return result;
    }
    
    /**
     * Dimensioni del dato ordinate così come sono in tabella fisica
     * @return la lista di dimensioni
     * @throws OdataDomainException
     */
    public List<MdDataDim> getSortedMdDataDims() throws OdataDomainException{
	List<MdDataDim> orderedDims = new ArrayList<MdDataDim>();
	
	for(Column col : getColumns()){
	    if(col.getType() == ColumnType.DIMENSION){
		for(MdDataDim dim : this.getMdDataDims()){
		    if(col.getPhysicalName().equalsIgnoreCase(dim.getDimcodeField())){
			orderedDims.add(dim);
		    }
		}
	    }
	}
	
	return orderedDims;
    }
    
    /**
     * Misure del dato ordinate così come sono in tabella fisica
     * @return la lista delle misure
     * @throws OdataDomainException
     */
    public List<MdMeasureFields> getSortedMdMeasureFields() throws OdataDomainException{
	List<MdMeasureFields> orderedMeas = new ArrayList<MdMeasureFields>();
	
	for(Column col : getColumns()){
	    if(col.getType() == ColumnType.MEASURE){
		for(MdMeasureFields meas : this.getMdMeasureFieldss()){
		    if(col.getPhysicalName().equalsIgnoreCase(meas.getMeasureField())){
			orderedMeas.add(meas);
		    }
		}
	    }
	}
	
	return orderedMeas;
    }
    
    /**
     * Numero di elementi diversi per una particolare colonna del dato
     * @param field	colonna sulla quale fare la distinct
     * @return	la cardinalità
     */
    public int getCardinalityForField(String field){
	return RepositoryFactory.getRepository().getCardinality(this.getTableName(), field);
    }
    
    /**
     * generazione e salvataggio su db (se non presente) della metainformazione relativa al dato (in JSON)
     * @param forceGeneration forza la rigenerazione della metainformazione
     * @return	la metainformazione sul dato
     * @throws OdataDomainException
     */
    public String getDataDescription(boolean forceGeneration) throws OdataDomainException{
	
	String dataDescription = (forceGeneration) ? "" : this.getContentDesc();
	// se si ha già l'informazione si restituisce
	if(forceGeneration || (dataDescription == null || dataDescription.equalsIgnoreCase(""))){
	    // si recupera il numero di colonne (Numero di campi)
	    List<Column> columnList = getColumns();
	    int columnListSize = columnList.size();
		
	    // si recupera il numero di righe (Numero di righe)
	    int countRows = RepositoryFactory.getRepository().getNumRows(getTableName());
		
	    MdData_Information_Column[] columns = new MdData_Information_Column[columnListSize];
		
	    int index = 0;
	    // si recuperano informazioni sul contenuto (Campo,Tipo,Valori Diversi,Contenuti (primi 10))
	    for(Column col : columnList){
		
		String physicalName = col.getPhysicalName();
		// si esclude la colonna della geometria
		if(!physicalName.equalsIgnoreCase("the_geom") && !physicalName.equalsIgnoreCase("gid")){
        		String columnType = "";
        	    
        		MdDataDim dimension = null;
        		if(col.getType() == ColumnType.DIMENSION){
        		    dimension = MdDataDim.findMdDataDim(col.getId());
        		    columnType = dimension.getIdHierNode().getName();
        		}else if(col.getType() == ColumnType.GENERIC_COLUMN){
        		    columnType = "Generica";
        		}else if(col.getType() == ColumnType.MEASURE){
        		    columnType = "Statistica";
        		}else if(col.getType() == ColumnType.OTHER){
        		    columnType = "Altro";
        		}
        		    
        		MinMaxCount mmc = RepositoryFactory.getRepository().getMinMaxCountForColumn(getTableName(), physicalName, dimension);
        		    
        		String moreRecurrentContent = RepositoryFactory.getRepository().getMoreRecurrentContent(getTableName(), physicalName, dimension);
        		    
        		String name = ((col.getLogicalName() != null && !col.getLogicalName().equalsIgnoreCase("")) ? col.getLogicalName() : physicalName);
        		MdData_Information_Column column = new MdData_Information_Column(name, columnType, mmc.count, mmc.min, mmc.max, moreRecurrentContent);
        		columns[index] = column;
		}else{
		    // si decrementano le colonne escluse
		    columnListSize--;
		}
		index++;
	    }
		
	    MdData_Information info = new MdData_Information(columnListSize, countRows, columns);
	    // serializzazione
	    Gson gson = new Gson();
	    dataDescription = gson.toJson(info);
	    
	    // si salva l'informazione sul database
	    this.setContentDesc(dataDescription);
	    this.merge();
	}
	
	return dataDescription;
    }
    
    /**
     * Espone l'id del dato a partire dall'uid (uid senza l'informazione sul portale)
     * @param uid	uid del dato
     * @return	l'id del dato
     */
    public static int getIdFromUid(String uid){
	return DBUtils.getIntValueFromUniqueStringField("md_data", "uid", uid, "id");
    }
    
    /**
     * Espone il numero di risultati dell'aggregazione di una misura per una particolare colonna (eventualmente filtrata, paginata e ordinata)
     * @param measureAggregation	misura sulla quale fare l'aggregazione
     * @param genericColumn		colonna generica per il raggruppamento
     * @param skip			record da saltare (per paginazione)
     * @param top			numero di record del risultato (sempre per la paginazione)
     * @param filterExpression		eventuali filtri
     * @param orderBy			eventuali ordinamenti
     * @return	il numero di righe risultanti
     * @throws OdataDomainException
     */
    public int getMeasuresAggregatedValuesGbGenericColumnsCount(MeasureAggregation measureAggregation, MdGenericColumn genericColumn, Integer skip, Integer top, String filterExpression, String orderBy) throws OdataDomainException{
	return RepositoryFactory.getRepository().getTableRowsMeasureAggregatedForGenericColumnCount(this.getTableName(), measureAggregation, genericColumn, skip, top, filterExpression, orderBy);
    }
    
    /**
     * Espone i risultati dell'aggregazione di una misura per una particolare colonna (eventualmente filtrata, paginata e ordinata)
     * @param measureAggregation	misura sulla quale fare l'aggregazione
     * @param genericColumn	colonna generica per il raggruppamento
     * @param skip	record da saltare (per paginazione)
     * @param top	numero di record del risultato (sempre per la paginazione)
     * @param filterExpression	eventuali filtri
     * @param orderBy	eventuali ordinamenti
     * @return	le righe risultanti
     * @throws OdataDomainException
     */
    public List<Row> getMeasuresAggregatedValuesGbGenericColumns(MeasureAggregation measureAggregation, MdGenericColumn genericColumn, Integer skip, Integer top, String filterExpression, String orderBy) throws OdataDomainException{
	return RepositoryFactory.getRepository().getTableRowsMeasureAggregatedForGenericColumn(this.getTableName(), measureAggregation, genericColumn, skip, top, filterExpression, orderBy);
    }
    
    /**
     * Espone il minimo e il massimo dei risultati dell'aggregazione di una misura per una particolare colonna (eventualmente filtrata)
     * 
     * @param measureAggregation	misura sulla quale fare l'aggregazione
     * @param genericColumn	colonna generica per il raggruppamento
     * @param filterExpression	eventuali filtri
     * @return il minimo e il massimo valore
     * @throws OdataDomainException
     */
    public MinMax getMinMaxForMeasuresAggregatedValuesGbGenericColumns(MeasureAggregation measureAggregation, MdGenericColumn genericColumn, String filterExpression) throws OdataDomainException{
	MinMaxCount mmc = RepositoryFactory.getRepository().getMinMaxCountForMeasureAggregatedForGenericColumn(this.getTableName(), measureAggregation, genericColumn, filterExpression);
	MinMax minMax = new MinMax(Double.parseDouble(mmc.min), Double.parseDouble(mmc.max));
	return minMax;
    }

    /**
     * Espone i risultati dell'aggregazione di una misura per una particolare colonna (eventualmente filtrata, paginata e ordinata), con particolari logiche di raggruppamento dei risultati
     * @param measureAggregation	misura sulla quale fare l'aggregazione
     * @param genericColumn	colonna generica per il raggruppamento
     * @param minPercentage	minima percentuale sotto la quale vengono esclusi i risultati
     * @param minResults	minimo numero di risultati
     * @param maxResults	massimo numero di risultati	
     * @param othersValue	presenza dell'elemento 'Altro'
     * @return	i risultati dell'aggregazione
     * @throws OdataDomainException
     */
    public List<Row> getMeasuresAggregatedValuesGbGenericColumnsForPie(MeasureAggregation measureAggregation, MdGenericColumn genericColumn, double minPercentage, int minResults, int maxResults, boolean othersValue) throws OdataDomainException{
	// si richiedono i dati raggruppati
	String orderBy = "\"" + measureAggregation.getMeasure().getMeasureField() + "\"" + " DESC";
	List<Row> groupedData = getMeasuresAggregatedValuesGbGenericColumns(measureAggregation, genericColumn, null, null, null, orderBy);

	// si compongono per la visualizzazione a torta
	return composeResultsForPie2(groupedData, minPercentage, minResults, maxResults, othersValue, measureAggregation.getMeasure().getMeasureField(), genericColumn.getColumnField());
    }
    

    /**
     * Numero di righe del dato
     * @return il numero di righe
     */
    public int getNumTableRowsFromQuery(){
	return   RepositoryFactory.getRepository().getNumRows(getTableName().toLowerCase());
    }
    
    /**
     * Aggiorna il numero di elementi distinti per le colonne generiche e le dimensioni (campo differentDistinctCount sul db)
     */
    public void updateDifferentDistinctCount(){
	// si scorrono tutte le dimensioni
	for(MdDataDim d : this.getMdDataDims()){
	    Integer differentDistinctCount = RepositoryFactory.getRepository().getDifferentDistictCount(this.getTableName(), d.getDimcodeField());
	    d.setDifferentDistinctCount(differentDistinctCount);
	    try {
		d.merge();
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}

	// si scorrono tutte le colonne generiche
	for(MdGenericColumn c : this.getMdGenericColumns()){
	    Integer differentDistinctCount = RepositoryFactory.getRepository().getDifferentDistictCount(this.getTableName(), c.getColumnField());
	    c.setDifferentDistinctCount(differentDistinctCount);
	    try {
		c.merge();
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
    }
    
    /**
     * Direzione di ordinamento (ascendente o discendente)
     *
     */
    public enum  SORTING_DIRECTION {
	ASC, DESC;  
    }
    
    /**
     * Campo sul quale ordinare (label o count)
     *
     */
    public enum FIELD_TO_SORT {
	LABEL, COUNT;
    }
    
    /**
     *	Classe Tripla di utilità generica
     * @param <X>
     * @param <Y>
     * @param <Z>
     */
    private class Tuple<X, Y, Z> {
	private X x;
	private Y y;
	private Z z;

	public Tuple(X x, Y y, Z z) {
	    this.x = x;
	    this.y = y;
	    this.z = z;
	}

	public X getX() {
	    return this.x;
	}

	public Y getY() {
	    return this.y;
	}

	public Z getZ() {
	    return this.z;
	}
    }
    
}
