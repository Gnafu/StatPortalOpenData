package it.sister.statportal.odata.repository;

import it.sister.statportal.odata.domain.Column;




import it.sister.statportal.odata.domain.Column.ColumnType;
import it.sister.statportal.odata.domain.IData;
import it.sister.statportal.odata.domain.IRepository;
import it.sister.statportal.odata.domain.MdData;
import it.sister.statportal.odata.domain.MdData.FIELD_TO_SORT;
import it.sister.statportal.odata.domain.MdData.SORTING_DIRECTION;
import it.sister.statportal.odata.domain.MdDataDim;
import it.sister.statportal.odata.domain.MdGenericColumn;
import it.sister.statportal.odata.domain.MdHierNode;
import it.sister.statportal.odata.domain.MdMeasureFields;
import it.sister.statportal.odata.domain.MeasureAggregation;
import it.sister.statportal.odata.domain.OdataDomainException;
import it.sister.statportal.odata.domain.Row;
import it.sister.statportal.odata.utility.DBUtils.DBColumnType;
import it.sister.statportal.odata.utility.StringUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.management.openmbean.OpenDataException;

import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * 
 * Classe che rappresenta l'implementazione del Repository per la base di dati
 * Postegres
 * 
 */
public class PostgreSQLRepository extends JdbcDaoSupport implements IRepository {

    
    private static String dimensionTablePrefix = "a_";
    /**
     * Costruttore
     */
    public PostgreSQLRepository() {

    }

    @Override
    public List<Column> getTableColumns(String tableName) {

	List<Column> columns = new ArrayList<Column>();

	List<Map<String, Object>> rows = getJdbcTemplate().queryForList(
		"SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.Columns where table_name = '"
			+ tableName.toLowerCase() + "' order by ordinal_position");
	
	int idx = 0;
	for (Map<String, Object> row : rows) {
	    columns.add(new Column(((String) row.get("COLUMN_NAME"))
		    .toLowerCase(), null, idx));
	    idx++;
	}

	return columns;
    }

    @Override
    public List<Row> getTableRows(String tableName, List<Column> columns)
	    throws OdataDomainException {

	List<Row> rows = new ArrayList<Row>();
	List<Map<String, Object>> result = getJdbcTemplate().queryForList(
		"SELECT * FROM \"" + tableName.toLowerCase() + "\"");
	populateRowList(result, columns, rows, false /* era true */, null);

	return rows;
    }

    @Override
    public List<Row> getTableRows(String tableName, List<Column> columns,
	    int startIndex, int count) throws OdataDomainException {
	return getTableRows(tableName, columns, startIndex, count, null);
    }

    @Override
    public List<Row> getTableRows(String tableName, List<Column> columns,
	    int startIndex, int count, String filter)
	    throws OdataDomainException {
	if (startIndex == 0) {
	    count = count + 1;
	}
	List<Row> rows = new ArrayList<Row>();
	List<Map<String, Object>> result = getJdbcTemplate().queryForList(
		"SELECT * FROM \""
			+ tableName.toLowerCase()
			+ "\""
			+ ((filter != null && !filter.equals("")) ? " WHERE "
				+ filter + " " : "") + " LIMIT " + count
			+ " OFFSET " + startIndex);
	populateRowList(result, columns, rows, false/* era startIndex == 0 */, null);
	return rows;
    }

    /**
     * Popola la lista di righe passata tra i parametri con il risultato della
     * query
     * 
     * @param result
     *        risultato della query
     * @param columns
     *        lista delle colonne
     * @param rows
     *        lista di righe da popolare
     * @param excludeFirstRow
     *        se true esclude la prima riga (perchè rappresenta il nome della
     *        colonna)
     * @param locale
     * 	      localizzazione
     * @throws OpenDataException
     */
    private void populateRowList(List<Map<String, Object>> result,
	    List<Column> columns, List<Row> rows, boolean excludeFirstRow, Locale locale)
	    throws OdataDomainException {
	Row newRow;
	
	// per tenere conto del numero di decimali per ciascuna misura
	// leggiamo il numero di decimali (se impostati)
	short DEFAULT_DECIMAL_PLACES = 2;
	String zeroDecimalPlacesStr = "." + new String(new char[DEFAULT_DECIMAL_PLACES]).replace('\0', '0');
	
	HashMap<Integer, Short> decimalPlacesMap = new HashMap<Integer, Short>();
	int columnsLen = columns.size();
	for (int x = 0; x < columnsLen; x++) {
	    Column c = columns.get(x);
	    if(c.getType() == ColumnType.MEASURE){
		int columnId = c.getId();
		MdMeasureFields measure = MdMeasureFields.findMdMeasureFields(columnId);
		Short decimalPlaces = measure.getDecimalPlaces();
		decimalPlacesMap.put(columnId, decimalPlaces != null ? decimalPlaces : DEFAULT_DECIMAL_PLACES);
	    }
	}
	
	// nella prima riga ci potrebbe essere il nome della colonna
	for (Map<String, Object> row : result) {
	    if (!excludeFirstRow) {
		newRow = new Row();

		for (int x = 0; x < columnsLen; x++) {
		    Column c = columns.get(x);
		    for (String key : row.keySet()) {
			if (c.getLogicalName().toLowerCase().equals(key.toLowerCase()) || c.getPhysicalName().toLowerCase().equals(key.toLowerCase())) {
			    
			    // se è un numero va tolta la notazione scientifica
			    String value = (row.get(key) != null ? row.get(key).toString() : "");
			    String originalValue = value;
			    if (value != null && c.getType() == ColumnType.MEASURE) {
				try {
				    short decimalPlaces = decimalPlacesMap.containsKey(c.getId()) ? decimalPlacesMap.get(c.getId()) : DEFAULT_DECIMAL_PLACES;
				    
				    if (locale == null) {
					BigDecimal bd = (new BigDecimal(value)).setScale(decimalPlaces,BigDecimal.ROUND_HALF_UP);
					value = bd.toPlainString();
				    } else {
					NumberFormat nf = NumberFormat.getInstance(locale);
					nf.setMaximumFractionDigits(decimalPlaces);
					nf.setMinimumFractionDigits(decimalPlaces);
					double l = Double.parseDouble(value);
					value = nf.format(l);
				    }
				} catch (Exception ex) {
				    // se schianta il cast si lascia il toString
				}
			    }
			    
			    newRow.addValue(c.getPhysicalName().toLowerCase(),value);
			    break;
			}
		    }
		}
		rows.add(newRow);
	    }
	    excludeFirstRow = false;
	}
    }

    @Override
    public List<Row> getTableRows(String tableName, List<Column> columns,
	    List<MdDataDim> mdDataDims,
	    List<MeasureAggregation> measureAggregation, int startIndex,
	    int count, boolean groupByEnabled, String filterExpression,
	    String orderBy, List<MdDataDim> joinedDimensions, Locale locale) throws OdataDomainException {

	String query = composeSQL(tableName, mdDataDims, measureAggregation,
		startIndex, count, groupByEnabled, filterExpression, orderBy,
		false, columns, joinedDimensions, false, false);

	List<Row> rows = new ArrayList<Row>();
	List<Map<String, Object>> result = getJdbcTemplate().queryForList(
		query.toString());
	populateRowList(result, columns, rows, false, locale);

	return rows;
    }
    
    @Override
    public int getCardinality(String tableName, String field){
	String query = "SELECT COUNT(DISTINCT(\"" + tableName.toLowerCase() + "\".\"" + field.toLowerCase() + "\"" + ")) FROM \"" + tableName.toLowerCase() + "\"";
	return getJdbcTemplate().queryForInt(query);
    }

    private String composeSQL(String tableName, List<MdDataDim> mdDataDims,
	    List<MeasureAggregation> measureAggregation, int startIndex,
	    int count, boolean groupByEnabled, String filterExpression,
	    String orderBy, boolean onlyCount, List<Column> columnsForOrder, List<MdDataDim> joinedDimensions, boolean excludeNullByCount, boolean minMax) {

	return composeSQL(tableName, mdDataDims, measureAggregation,
		startIndex, count, groupByEnabled, filterExpression, orderBy,
		onlyCount, false, columnsForOrder, joinedDimensions, excludeNullByCount, minMax);
    }

    private String composeSQL(String tableName, List<MdDataDim> mdDataDims,
	    List<MeasureAggregation> measureAggregation, int startIndex,
	    int count, boolean groupByEnabled, String filterExpression,
	    String orderBy, boolean onlyCount, boolean isForTerritorialView, List<Column> columnsForOrder, List<MdDataDim> joinedDimensions, boolean excludeNullByCount, boolean minMax) {

	tableName = tableName.toLowerCase();
	// componiamo la query
	StringBuilder query = new StringBuilder();
	StringBuilder joins = new StringBuilder();
	StringBuilder groupBy = new StringBuilder();
	
	HashMap<String, String> selection = new HashMap<String, String>();
	
	boolean minMaxConditionsRespected = minMax && measureAggregation.size() == 1; 
	if(minMaxConditionsRespected){
	    String nameFirstMeasure = measureAggregation.get(0).getMeasure().getMeasureField();
	    query.append("SELECT MIN(\"" + nameFirstMeasure + "\") AS \"min\",  MAX(\"" + nameFirstMeasure + "\") AS \"max\" FROM ( ");
	}

	if ((onlyCount && (groupByEnabled || (mdDataDims != null && mdDataDims
		.size() == 0)))) {
	    query.append("SELECT COUNT(*) FROM (");
	}

	query.append("SELECT ");
	if (onlyCount){
	    if(!(groupByEnabled || (mdDataDims != null && mdDataDims.size() == 0))) {
		    query.append("COUNT(*) ");
	    }else{
		if(!groupByEnabled){
		    query.append("* ");
		}
	    }
	}

	if (measureAggregation != null) {
	    for (MeasureAggregation singleMeasureAggregation : measureAggregation) {
		if (!onlyCount
			|| (onlyCount && (groupByEnabled || (mdDataDims != null && mdDataDims
				.size() == 0)))) {
		    
		    String measVal = (getSqlAggregationSelect(
			    tableName,
			    singleMeasureAggregation,
			    groupByEnabled
				    || (groupByEnabled || (mdDataDims != null && mdDataDims
					    .size() == 0)))  + " AS \"" + singleMeasureAggregation.getMeasure().getMeasureField() + "\"" + ",");
		    if(columnsForOrder != null && !groupByEnabled){
			selection.put(singleMeasureAggregation.getMeasure().getMeasureField().toLowerCase(), measVal);
		    }else{
			query.append(measVal);
		    }
		}
	    }
	}

	if (mdDataDims != null) {

	    for (MdDataDim dim : mdDataDims) {
		String dimensionAliasTableName = dimensionTablePrefix + dim.getDimcodeField().toLowerCase();
		if (!onlyCount || (onlyCount && groupByEnabled)) {
		    
		    String dimVal = (dimensionAliasTableName
			    + ".\""
			    + ((!isForTerritorialView) ? dim.getIdHierNode()
				    .getDescField().toLowerCase() : "id")
			    + "\""
			    + " AS \"" + dim.getDimcodeField().toLowerCase() + "\"" + ",");
		    if(columnsForOrder != null && !groupByEnabled){
			selection.put(dim.getDimcodeField().toLowerCase(), dimVal);
		    }else{
			query.append(dimVal);
		    }
		}
		joins.append(" JOIN "
			+ "\"" + dim.getIdHierNode().getTableName().toLowerCase() + "\" " + dimensionAliasTableName 
			+ " ON \"" + tableName.toLowerCase() + "\".\""
			+ dim.getDimcodeField().toLowerCase() + "\" = "
			+ dimensionAliasTableName
			+ ".\"id\" ");

		if (groupByEnabled) {
		    groupBy.append("\"" + dimensionAliasTableName + "\""
			    + ".\""
			    + ((!isForTerritorialView) ? dim.getIdHierNode()
				    .getDescField().toLowerCase() : "id")
			    + "\",");
		}
		
	    }
	}
	
	// se si sono richieste ulteriori join (a causa dei filtri) si aggiungono
	if(joinedDimensions != null && joinedDimensions.size() != 0){
	    // devo aggiungere solo nel caso che non ci sia già
	    for (MdDataDim dimToJoin : joinedDimensions) {
		String dimensionAliasTableName = dimensionTablePrefix + dimToJoin.getDimcodeField().toLowerCase();
		
		boolean appendJoin = true;
		if (mdDataDims != null) {
		    for (MdDataDim dim : mdDataDims) {
			if(dim.getId().intValue() == dimToJoin.getId().intValue()){
			    appendJoin = false;
			    break;
			}
		    }
		}else{
		    appendJoin = true;
		}
		
		if(appendJoin){
        	    joins.append(" JOIN "
        			+ "\"" + dimToJoin.getIdHierNode().getTableName().toLowerCase() + "\" " + dimensionAliasTableName + ""
        			+ " ON \"" + tableName.toLowerCase() + "\".\""
        			+ dimToJoin.getDimcodeField().toLowerCase() + "\" = "
        			+ "\"" + dimensionAliasTableName + "\""
        			+ ".\"id\" ");
		}
	    }
	}
	
	// variabili utilizzate per comporre l'ordinamento che permette di 
	// far funzionare correttamente l'rodinamento
	String moreOrdering = "";
	String betweenVal = ", ";
	    
	
	// se sono state passate le colonne tra i parametri 
	// si aggiungono tutti i select nell'ordine corretto
	// se le colonne che sono state passate sono null
	// significa che i dati sono già composti
	if(columnsForOrder != null && !groupByEnabled){
	    for (Column c : columnsForOrder) {
		String toAppend = "";
		// se si tratta di una dimensione o misura
		if(selection.containsKey(c.getPhysicalName().toLowerCase())){
		    toAppend = selection.get(c.getPhysicalName());
		    moreOrdering += "\"" + tableName + "\".\"" + c.getPhysicalName().trim() + "\"" + betweenVal;
		    query.append(toAppend);
		}else{
		    toAppend = "\"" + tableName + "\".\"" + c.getPhysicalName() + "\" " + " AS \"" + c.getPhysicalName() + "\"" + ",";
		    moreOrdering += "\"" + tableName + "\".\"" + c.getPhysicalName().trim() + "\"" + betweenVal;
		    // se si tratta di un campo descrittivo
		    query.append(toAppend);
		}
	    }
	}

	// si rimuove l'ultima virgola dalla query e si aggiunge il from
	if (query.substring(query.length() - 1).equals(",")) {
	    query.deleteCharAt(query.length() - 1);
	}
	query.append(" FROM \"" + tableName.toLowerCase() + "\" ");

	// si aggiungono le JOIN
	query.append(joins);
	
	// si aggiunge la clausola WHERE
	if (filterExpression != null && !filterExpression.equals("")) {
	    query.append(" WHERE " + filterExpression + " ");
	}

	// si aggiunge la groupby (se richiesta), rimuovendone prima l'ultima
	// virgola. Poi si aggiunge anche l'eventuale HAVING
	if (groupByEnabled) {
	    groupBy.deleteCharAt(groupBy.length() - 1);
	    query.append(" GROUP BY ");
	    query.append(groupBy);
	}

	if (!onlyCount) {
	   
	    // per far si che la paginazione non scavoli bisogna sempre inserire un ordinamento.
	    // Bisogna sempre ordinare per tutti i campi presenti nella select (escluse le aggregazioni
	    // e i count). Ovviamente nel caso non si sia richiesto un count
	    
	    // se si tratta di aggregazione si mettono le dimensioni aggregate, 
	    // altrimenti si mettono tutti campi della select (sono già stati composti in precedenza)
	    if (groupByEnabled) {
		moreOrdering = "";
		for (MdDataDim d : mdDataDims) {
		    MdHierNode h = d.getIdHierNode();
		    moreOrdering += "\""
			    + dimensionTablePrefix + d.getDimcodeField().toLowerCase()
			    + "\".\"" + ((!isForTerritorialView) ? h.getDescField().toLowerCase() : "id")
			    + "\"" + betweenVal;
		}
	    } 
	    
	    int moreOrderingLen = moreOrdering.length();
	    if(moreOrderingLen != 0){
		// si toglie l'ultima virgola
		moreOrdering = moreOrdering.substring(0, moreOrderingLen - betweenVal.length());
	    }
	    
	    if (orderBy != null && !orderBy.equals("")) {
		query.append(" ORDER BY " + orderBy + "  NULLS LAST " + (moreOrdering.isEmpty() ? "" : "," + moreOrdering));
	    }else{
		// Si aggiunge sempre l'ordinamento perchè in caso 
		// di paginato non è garantito un ordinamento dal db
		if (!onlyCount){
		    query.append(" ORDER BY " + moreOrdering);
		}
	    }
	}


	// si aggiunge la paginazione (se richiesta)
	if (!onlyCount) {
	    if (startIndex != -1 && count != -1) {
		query.append(" LIMIT " + count + " OFFSET " + startIndex);
	    }
	}

	if ((onlyCount && (groupByEnabled || (mdDataDims != null && mdDataDims
		.size() == 0)))) {
	    query.append(") q");
	    
		// si compone la clausola WHERE nel caso in cui è stata richiesta una count
		// e si vogliono escludere i valori null
		StringBuilder excludeNullByCountFilter = new StringBuilder();
		if(onlyCount && excludeNullByCount){
		    if (measureAggregation != null && measureAggregation.size() > 0) {
			excludeNullByCountFilter.append(" WHERE ");
			for (MeasureAggregation singleMeasureAggregation : measureAggregation) {
			    excludeNullByCountFilter.append(" q.\"" + singleMeasureAggregation.getMeasure().getMeasureField() + "\" IS NOT NULL AND ");
			}
			// si toglie l'ultimo AND
			int excludeNullByCountFilterLen = excludeNullByCountFilter.length();
			if(excludeNullByCountFilterLen > 4){
			    excludeNullByCountFilter.delete(excludeNullByCountFilterLen -5, excludeNullByCountFilterLen);
			}
			
			query.append(excludeNullByCountFilter.toString());
		    }
		}
	}
	
	if(minMaxConditionsRespected){
	    query.append(" ) q");
	}
	
	return query.toString();
    }
    
    private void printForDebug(String print, String filePath){
	try {
//	    System.out.println();
//	    System.out.println();
//	    System.out.println(print.toString());
//	    System.out.println();
//	    System.out.println();
//	    
	    
	    
	    // Create file
//	    FileWriter fstream = new FileWriter(filePath, true);
//	    BufferedWriter out = new BufferedWriter(fstream);
//	    out.write(print.toString());
//	    out.write("\r\n");
//	    // Close the output stream
//	    out.close();
	} catch (Exception e) {

	}
    }

    /**
     * Restituisce l'SQL per la funzione di aggregazione
     * 
     * @param tableName
     *        nome della tabella
     * @param measureAggregation
     *        oggetto che rappresenta una misura e una funzione di aggregazione
     * @return  l'SQL per la funzione di aggregazione
     */
    private String getSqlAggregationSelect(String tableName,
	    MeasureAggregation measureAggregation, boolean doAggregation) {

	if(measureAggregation.getMeasure().getAlias().equals(MdData.COUNT_STAR_FIELD) && measureAggregation.getMeasure().getMeasureField().equals(MdData.COUNT_STAR_FIELD)){
	    return "COUNT(*)";
	}
	
	tableName = tableName.toLowerCase();
	
	String aggregationSelect = 	"\""
					+ tableName
					+ "\".\""
					+ measureAggregation.getMeasure().getMeasureField().toLowerCase() 
					+ "\"";
	
	if (measureAggregation.getAggregateFunc() != null && doAggregation) {

	    aggregationSelect = measureAggregation.getAggregateFunc()
		    .toString()
		    + "("
		    + aggregationSelect
		    + ")";
	}

	return aggregationSelect;
    }

    @Override
    public int getCountRows(String tableName, List<MdDataDim> mdDataDims,
	    List<MeasureAggregation> measureAggregation, int startIndex,
	    int count, boolean groupByEnabled, String filterExpression,
	    String orderBy,List<MdDataDim> joinedDimensions, boolean excludeNullByCount) {

	String query = composeSQL(tableName, mdDataDims, measureAggregation,
		startIndex, count, groupByEnabled, filterExpression, orderBy,
		true, null, joinedDimensions, excludeNullByCount, false);

	int countRows = getJdbcTemplate().queryForInt(query.toString());

	return countRows;
    }

    @Override
    public void createView(String tableName, String viewName,
	    List<MdDataDim> mdDataDims,
	    List<MeasureAggregation> measureAggregation, boolean groupByEnabled, String filterExpression) {

	String sqlCreationView = "CREATE VIEW "
		+ viewName
		+ " AS "
		+ composeSQL(tableName, mdDataDims, measureAggregation, -1, -1,
			groupByEnabled, filterExpression, null, false, true, null, null, false, false);

	//System.out.println(sqlCreationView);
	getJdbcTemplate().execute(sqlCreationView);
    }

    @Override
    public List<Map<String, Object>> getKmlPoints(MdData data, String geomField, String pointField) {
	
	StringBuilder sbSelect = new StringBuilder();
	StringBuilder sbJoin = new StringBuilder();
	String tableName = data.getTableName().toLowerCase();
	// si creano le JOIN e si popola la select con le dimensioni
	for(MdDataDim dim : data.getMdDataDims()){
	    String dimensionTable = dim.getIdHierNode().getTableName().toLowerCase();
	    String dimensionKey = dim.getIdHierNode().getPkField().toLowerCase();
	    String dimensionField = dim.getDimcodeField().toLowerCase();
	    String dimensionDesc = dim.getIdHierNode().getDescField().toLowerCase();
	    String dimensionAlias = dim.getAlias();
	    sbSelect.append("\"" + dimensionTable + "\".\"" + dimensionDesc + "\" AS \"" + dimensionAlias + "\", ");
	    sbJoin.append("LEFT JOIN \"" + dim.getIdHierNode().getTableName().toLowerCase() + "\" ON \"" + tableName + "\".\"" + dimensionField + "\" = \"" + dimensionTable + "\".\"" + dimensionKey + "\" ");
	}
	
	// si popola la select con le misure
	for(MdMeasureFields measure : data.getMdMeasureFieldss()){
	    String measureField = measure.getMeasureField().toLowerCase();
	    String measureAlias = measure.getAlias();
	    sbSelect.append("\"" + tableName + "\".\"" + measureField + "\" AS \"" + measureAlias + "\", ");
	}
	
	// si popola la select con le colonne generiche
	for(MdGenericColumn column : data.getMdGenericColumns()){
	    String columnField = column.getColumnField();
	    String columnAlias = column.getAlias();
	    if (columnAlias.equalsIgnoreCase("DO_X")) {
		columnAlias = "Longitudine";
	    } else if (columnAlias.equalsIgnoreCase("DO_Y")) {
		columnAlias = "Latitudine";
	    }
	    sbSelect.append("\"" + tableName + "\".\"" + columnField + "\" AS \"" + columnAlias + "\", ");
	}
	
	// si toglie l'ultima virgola dalla select
	if(sbSelect.length() > 0) {
	    sbSelect.deleteCharAt(sbSelect.lastIndexOf(","));
	}
	
	// si compone la query
	String query = 	"SELECT asKml(\"" + geomField + "\") as \"" + pointField + "\"," + sbSelect.toString() + " " + 
			"FROM \"" + tableName + "\" " + 
			sbJoin.toString();
	
	List<Map<String, Object>> set = getJdbcTemplate().queryForList(query);
	return set;
    }

    @Override
    public List<Map<String, Object>> getDistinctValueAndCountForField(String tableName, String field, String joinTableName, String joinFieldName, String joinKeyName, int skip, int top, FIELD_TO_SORT fieldToSort, SORTING_DIRECTION sortingDirection, String filter) throws OdataDomainException {
	
	String query = composeSQLforDistinctValueAndCountField(tableName, field, joinTableName, joinFieldName, joinKeyName, skip, top, fieldToSort, sortingDirection, filter, false);
	
	List<Map<String, Object>> result = getJdbcTemplate().queryForList(
		query.toString());

	return result;
    }
    

    @Override
    public MinMax getMinMaxCountForDistinctValueAndCountForField(String tableName, String field, String joinTableName, String joinFieldName, String joinKeyName, int skip, int top, FIELD_TO_SORT fieldToSort, SORTING_DIRECTION sortingDirection, String filter) throws OdataDomainException {
	
	String query = composeSQLforDistinctValueAndCountField(tableName, field, joinTableName, joinFieldName, joinKeyName, skip, top, fieldToSort, sortingDirection, filter, true);
	
	List<Map<String, Object>> minMax = getJdbcTemplate().queryForList(query);
	
	if(minMax.size() != 1){
	    return null;
	}
	
	MinMax ret = new MinMax(new Double(minMax.get(0).get("min").toString()), new Double(minMax.get(0).get("max").toString()));
	return ret;
    }
    
    /**
     * Compone l'SQL per il calcolo dei valori distinti con il relativo count (se minMax = true ne calcola solo il minimo e il massimo della count)
     * @param tableName	nome della tabella
     * @param field	nome del campo
     * @param joinTableName	tabella da mettere in join
     * @param joinFieldName	campo da mettere in join
     * @param joinKeyName	
     * @param skip	da (paginazione)
     * @param top	numero di risultati
     * @param fieldToSort	campo di ordinamento
     * @param sortingDirection	direzione di ordinamento
     * @param filter	eventuali filtri
     * @param minMax	se true si calcola solo il minimo e il massimo della count
     * @return la stringa SQL 
     * @throws OdataDomainException
     */
    private String composeSQLforDistinctValueAndCountField(String tableName, String field, String joinTableName, String joinFieldName, String joinKeyName, int skip, int top, FIELD_TO_SORT fieldToSort, SORTING_DIRECTION sortingDirection, String filter, boolean minMax) throws OdataDomainException {
	String columnSelector = tableName + ".\"" + field + "\"";
	String join = "";
	if(joinFieldName != null && joinKeyName != null && joinTableName != null){
	    columnSelector = joinTableName + ".\"" + joinFieldName + "\"";
	    join = "JOIN " + joinTableName + " ON " + joinTableName + ".\"" + joinKeyName + "\" = " + tableName + ".\"" + field + "\"";
	}
	
	String countField = "occurrences";
	String labelField = "label";
	
	String sortingField = countField;
	if(fieldToSort == FIELD_TO_SORT.LABEL){
	    sortingField = labelField;
	}else if(fieldToSort == FIELD_TO_SORT.COUNT){
	    sortingField = countField;
	}
	 
	String sortDirection = "DESC";
	if(sortingDirection == SORTING_DIRECTION.ASC){
	    sortDirection = "asc";
	}else if(sortingDirection == SORTING_DIRECTION.DESC){
	    sortDirection = "desc";
	}
	
	String minMaxPrefix = "";
	String minMaxSuffix = "";
	
	if(minMax){
	    minMaxPrefix = "SELECT MIN(\"" + countField + "\") AS \"min\", MAX(\"" + countField + "\") AS \"max\" FROM (";
	    minMaxSuffix = ") q";
	}
	
	String query = 	minMaxPrefix + 
			"SELECT " + columnSelector.toLowerCase() + " as " + labelField + ", count(*) as " + countField + 
			" FROM " + tableName.toLowerCase() + " " +  
			join.toLowerCase() + " " +
			((filter != null && !filter.isEmpty()) ? (" WHERE " + columnSelector.toLowerCase() + " ILIKE '%" + filter.replaceAll("'", "''") + "%' ") : " ") +
			" GROUP BY " + columnSelector.toLowerCase() + " " +  
			" ORDER BY " + sortingField.toLowerCase() + " " + sortDirection + " nulls last " + 
			((top != -1 ) ? " LIMIT " + top : " ") + 
			((skip != 0 ) ? " OFFSET " + skip : "") + 
			minMaxSuffix;
	
	return query;
    }

    @Override
    public List<Map<String, Object>> executeQueryForList(String query) {
	return getJdbcTemplate().queryForList(query);
    }

    @Override
    public boolean tableExists(String tableName) {
	String query = "SELECT count(*) FROM pg_class WHERE relname = '" + tableName + "';";
	return getJdbcTemplate().queryForInt(query) > 0;
    }
    
    @Override
    public String databaseName(){
	String query ="select current_database()";
	return getJdbcTemplate().queryForObject(query, String.class);
    }

    @Override
    public boolean createTable(String tableName, String[] columnsName,
	    DBColumnType[] columnsType, String primaryKey) throws Exception {
		
		// si validano i nomi (della tabella e delle colonne)
		for(String col : columnsName){
		    if(col.length() > 63){
			throw new OdataDomainException("Nome di colonna troppo lungo (" + col + ")");
		    }
		}
	
        	StringBuilder query = new StringBuilder();
        	query.append("CREATE TABLE \"public\".\"" + tableName + "\" (");
        	
        	int idx = 0;
        	int len = columnsName.length;
        	for (idx = 0; idx < len; idx++) {
        	    query.append("\"" + columnsName[idx] + "\" " + getColumnTypeByJavaClass(columnsType[idx]) + " " + ((primaryKey.equalsIgnoreCase(columnsName[idx])) ? " PRIMARY KEY " : ""));
        	    if(idx != (len-1)){
        		query.append(",");
        	    }
        	}
        	query.append(")");
        	query.append(" WITH (OIDS=FALSE)");
        	query.append(";");
        	
        	getJdbcTemplate().execute(query.toString());
        	return true;
    }
    
    /**
     * Gestione del mapping tra un tipo di dato definito (DBColumnType) e quello di POSTGRESQL
     * @param type	tipo 
     * @return	la stringa che rappresenta il tipo (POSTGRESQL) relativo al
     * @throws Exception
     */
    private String getColumnTypeByJavaClass(DBColumnType type) throws Exception{
	
	switch (type) {
		case STRING:
		    return "varchar";
		case DOUBLE: 
		    return "float4";
		case INT:
		    return "int4";
		case SHORT:
		    return "int2";
		case GEOMETRY:
		    return "geometry";
	}
	
	throw new Exception("Tipo di colonna non riconosciuto");
    }

    @Override
    public boolean insertRow(String tableName, String[] columns,
	    List<String[]> values) {
	StringBuilder query = new StringBuilder();
	StringBuilder sbValues = new StringBuilder();

	query.append("INSERT INTO \"public\".\"" + tableName + "\" (");
	int idx = 0;
	int len = columns.length;

	// si compongono le colonne
	for (idx = 0; idx < len; idx++) {
	    query.append("\"" + columns[idx] + "\"");

	    if (idx != (len - 1)) {
		query.append(",");
	    }
	}
	query.append(") ");
	int valuesLen = values.size();
	int idx2 = 0;
	// si compongono i valori
	for (idx2 = 0; idx2 < valuesLen; idx2++) {
	    sbValues.append("(");
	    for (idx = 0; idx < len; idx++) {
		sbValues.append(values.get(idx2)[idx]);
		if (idx != (len - 1)) {
		    sbValues.append(",");
		}
	    }
	    sbValues.append(")");
	    if (idx2 != (valuesLen - 1)) {
		    sbValues.append(",");
	    }
	}

	
	query.append("VALUES " + sbValues.toString());
	getJdbcTemplate().execute(query.toString());
	return true;
    }

    @Override
    public boolean dropTable(String tableName) {
	String query = "DROP TABLE \"public\".\"" + tableName + "\"";
	getJdbcTemplate().execute(query);
	return true;
    }

    @Override
    public String escape(String toEscape) {
	
	StringBuilder sb = new StringBuilder(toEscape.length() * 2);
	int toEscapeLen = toEscape.length();
	for(int j = 0; j < toEscapeLen; j++){
		char currentChar = toEscape.charAt(j);
		if(currentChar == '\'' || currentChar == '\\'){
			sb.append('\\');
		}				
		sb.append(currentChar);
	}
	return sb.toString();
    }

    @Override
    public String getEscapeSequence() {
	return "\\";
    }

    @Override
    public boolean viewExists(String viewName) {
	String query = "SELECT COUNT(*) FROM pg_views WHERE viewname = '" + viewName + "';";
	return (getJdbcTemplate().queryForInt(query) > 0);
    }

    @Override
    public String generateUid() throws OdataDomainException {
	String UID_GENERATED = "uid_generated";
	String query = "SELECT uuid_generate_v4() AS " + UID_GENERATED + ";";
	List<Map<String, Object>> uid = getJdbcTemplate().queryForList(query);
	
	if(uid.size() != 1){
	    throw new OdataDomainException();
	}
	
	return uid.get(0).get(UID_GENERATED).toString();
    }

    @Override
    public int getIntValueFromUniqueStringField(String tableName, String stringField, String stringValue,
	    String intField) {
	String query = "SELECT \"" + tableName + "\".\"" + intField + "\" FROM \"" + tableName + "\" WHERE \"" + tableName + "\".\"" + stringField + "\" like '" + stringValue + "'";
	
	return getJdbcTemplate().queryForInt(query);
    }

    @Override
    public MinMax getMinMaxForAggregatedMeasure(String tableName, List<MdDataDim> mdDataDims, MeasureAggregation measureAggregation, int startIndex,int count, boolean groupByEnabled, String filterExpression, String orderBy, List<MdDataDim> joinedDimensions, boolean excludeNullByCount) {
	
	List<MeasureAggregation> singleMeasureAggregationInList = new ArrayList<MeasureAggregation>();
	singleMeasureAggregationInList.add(measureAggregation);
	String query = composeSQL(tableName, mdDataDims, singleMeasureAggregationInList, startIndex, count, groupByEnabled, filterExpression, orderBy, false, null, joinedDimensions, excludeNullByCount, true);
	
	List<Map<String, Object>> minMax = getJdbcTemplate().queryForList(query);
	
	if(minMax.size() != 1){
	    return null;
	}
	
	MinMax ret = new MinMax(new Double(minMax.get(0).get("min").toString()), new Double(minMax.get(0).get("max").toString()));
	return ret;
    }

    @Override
    public int getNumRows(String tableName) {
	return getJdbcTemplate().queryForInt("SELECT COUNT(*) FROM \"" + tableName.toLowerCase().trim() + "\"");
    }
    
    @Override
    public HashMap<String, SimpleColumnInfo> getDbTypeColumnInfo(String tableName){
	
	String a = "col_name";
	String b = "db_type";
	String c = "decimals";
	HashMap<String,SimpleColumnInfo> map = new HashMap<String,SimpleColumnInfo>();
	List<Map<String, Object>> queryRes = getJdbcTemplate().queryForList("select column_name as " + a + ", data_type as " + b + ", numeric_scale as " + c + " from information_schema.columns where table_name like '" + tableName.toLowerCase() + "' and table_schema = 'public'");
	
	for (Map<String, Object> row : queryRes) {
	    map.put(((String) row.get(a)).toLowerCase(), new SimpleColumnInfo(((String) row.get(b)).toLowerCase(), ((Integer) row.get(c))));
	}
	
	return map;
    }

    @Override
    public String getPointStringFromCoordinates(String longitude,
	    String latitude) {
	return "ST_GeomFromText('POINT(" + longitude + " " + latitude + ")', 4326)";
    }
    
    @Override
    public boolean addGeometryColumn(String tableName, GeometryType geom_type){
	try{
	    String geomType = "";
	    if(geom_type == GeometryType.POINT){
		geomType = "POINT";
	    }else if(geom_type == GeometryType.MULTIPOLYGON){
		geomType = "MULTIPOLYGON";
	    }else{
		throw new OdataDomainException("GeometryType non gestito");
	    }
	    getJdbcTemplate().execute("SELECT AddGeometryColumn('','" + tableName + "','the_geom','4326','" + geomType + "',2);");
	    return true;
	}catch(Exception e){
	    return false;
	}
    }
    
    @Override
    public String getMoreRecurrentContent(String tableName, String descriptiveField, MdDataDim dimension){
	
	// inizializzazione da mettere in una funzione
	tableName = tableName.toLowerCase().trim();
	descriptiveField = descriptiveField.toLowerCase().trim();
	
	String tableField; 
	String pkField = null;
	String joinTable = null;
	String tableNameInJoin;
	
	boolean joinNecessary = false;
	if(dimension != null){
	    joinNecessary = true;
	    
	    MdHierNode hierNode = dimension.getIdHierNode();
	    tableField =  dimension.getDimcodeField().toLowerCase().trim(); 
	    pkField = hierNode.getPkField().toLowerCase().trim();
	    joinTable = hierNode.getTableName().toLowerCase().trim();
	    descriptiveField = hierNode.getDescField().toLowerCase().trim();
	    
	    tableNameInJoin = "t2";
	}else{
	    tableField = descriptiveField;
	    tableNameInJoin = "t1";
	}
	
	// fine inizializzazione da mettere in una funzione 
	
	String query = "";
	if(joinNecessary){
	    query = 	"select valori from  " + 
		    	"( SELECT " + tableNameInJoin + ".\"" + descriptiveField + "\"  valori, count(t.\"" + tableField + "\" ) CNT FROM " + 
		    	"\"" + tableName + "\" t join \"" + joinTable + "\" t2 on t.\"" + tableField + "\" = t2.\"" + pkField + "\" " + 
		    	"group by t2.\"" + descriptiveField + "\" ) q order by q.CNT desc LIMIT 10";
	}else{
	    query = "select valori from  ( SELECT t.\"" + descriptiveField + "\"  valori, count(t.\"" + descriptiveField + "\" ) CNT " + 
		    "FROM \"" + tableName + "\" t group by t.\"" + descriptiveField + "\"  ) q " + 
		    "order by q.CNT desc LIMIT 10";
	}
	
	List<Map<String, Object>> queryRes = getJdbcTemplate().queryForList(query);
	
	StringBuilder sb = new StringBuilder();
	for (Map<String, Object> row : queryRes) {
	    Object str = row.get("valori");
	    sb.append(str != null ? str.toString() + ", " : "");
	}
	
	if(sb.length() > 2){
	    sb.deleteCharAt(sb.length() - 2);
	}
	
	String content = sb.toString(); 
	
	return (content.length() > 240 ? content.substring(0, 240) + "..." : content);
    }

    @Override
    public MinMaxCount getMinMaxCountForColumn(String tableName, String descriptiveField, MdDataDim dimension) throws OdataDomainException {
	
	/* inizializzazione da mettere in una funzione */
	
	tableName = tableName.toLowerCase().trim();
	descriptiveField = descriptiveField.toLowerCase().trim();
	
	String tableField; 
	String pkField = null;
	String joinTable = null;
	String tableNameInJoin;
	
	boolean joinNecessary = false;
	if(dimension != null){
	    joinNecessary = true;
	    
	    MdHierNode hierNode = dimension.getIdHierNode();
	    tableField =  dimension.getDimcodeField().toLowerCase().trim(); 
	    pkField = hierNode.getPkField().toLowerCase().trim();
	    joinTable = hierNode.getTableName().toLowerCase().trim();
	    descriptiveField = hierNode.getDescField().toLowerCase().trim();
	    
	    tableNameInJoin = "t2";
	}else{
	    tableField = descriptiveField;
	    tableNameInJoin = "t1";
	}
	
	/* fine inizializzazione da mettere in una funzione */
	
	String query = 	"select min(" + tableNameInJoin + ".\"" + descriptiveField + "\") as min, max(" + tableNameInJoin + ".\"" + descriptiveField + "\") as max, count(distinct t1.\"" + tableField + "\") as count_distinct from " + 
			tableName + " t1 ";
		
	if(joinNecessary){
	    query += "JOIN " +	joinTable + " t2 on t1.\"" + tableField + "\" = t2.\"" + pkField + "\"";    
	}
			
	List<Map<String, Object>> queryRes = getJdbcTemplate().queryForList(query);
	
	if(queryRes.size() != 1){
	    throw new OdataDomainException("MinMaxCount: lunghezza errata del risultato della query");
	}
	
	Object minObj = queryRes.get(0).get("min");
	Object maxObj = queryRes.get(0).get("max");
	Object countObj = queryRes.get(0).get("count_distinct");
	
	String min = minObj != null ? minObj.toString() : "";
	String max = maxObj != null ? maxObj.toString() : "";
	String count = countObj != null ? countObj.toString() : "";
	
	MinMaxCount mmc = new MinMaxCount(min, max, count);
	return mmc;
    }
    
    @Override
    public boolean isPunctualGeometry(String tableName){
	String query = 	"SELECT COUNT(*) " + 
			"FROM ( " + 
				"SELECT the_geom as geom " +
				"FROM " + tableName + " " +
			") q " + 
			"WHERE geometrytype(q.geom) LIKE 'POINT'"; 

	int count = getJdbcTemplate().queryForInt(query);
	return count > 0;
    }
    
    @Override
    public void createLatLngColumns(String tableName, String latitudeCol, String longitudeCol) {
    	// prima si aggiungono le colonne
    	String queryAlterTable =	"ALTER TABLE " + tableName + " " + 
    					"ADD COLUMN " + latitudeCol + " numeric, " + 
    					"ADD COLUMN " + longitudeCol + " numeric";
    	
    	getJdbcTemplate().execute(queryAlterTable);
    	
    	// poi si fa l'update
    	String queryUpdateTable = 	"UPDATE " + tableName + " " +
    					"SET " + latitudeCol + " = y(the_geom), " + 
    					longitudeCol + " = x(the_geom) " + 
    					"WHERE geometrytype(the_geom) LIKE 'POINT'";
	
    	getJdbcTemplate().execute(queryUpdateTable);
    	
    	// poi si settano null eventuali valori di coordinate non validi
    	String querySetNullInvalidCoordinates = "UPDATE " + tableName + " " +
						"SET " + latitudeCol + " = null, " + 
						longitudeCol + " = null " + 
						"WHERE geometrytype(the_geom) LIKE 'POINT' AND (" + 
						"(" + latitudeCol + "< -90 OR " + latitudeCol + "> 90) OR" + 
						"(" + longitudeCol + "< -180 OR " + longitudeCol + "> 180))";

    	getJdbcTemplate().execute(querySetNullInvalidCoordinates);
    	
    }

    @Override
    public List<Row> getTableRowsMeasureAggregatedForGenericColumn(String tableName,
	    MeasureAggregation measureAggregation,
	    MdGenericColumn genericColumn, Integer skip, Integer top, String filterExpression, String orderBy) throws OdataDomainException {
	
	// si aggiungono le due colonne
	List<Column> columns = new ArrayList<Column>();
	MdMeasureFields measure = measureAggregation.getMeasure();
	columns.add(new Column(measure.getMeasureField(), measure.getAlias(), measure.getId().intValue(), ColumnType.MEASURE));
	columns.add(new Column(genericColumn.getColumnField(), genericColumn.getAlias(), genericColumn.getId().intValue(), ColumnType.GENERIC_COLUMN));
	
	List<Row> rows = new ArrayList<Row>();
	List<Map<String, Object>> result = getJdbcTemplate().queryForList(getQueryMeasureAggregatedForGenericColumn(tableName, measureAggregation, genericColumn, skip, top, filterExpression, orderBy));
	populateRowList(result, columns, rows, false, null);
	
	return rows;
    }
    
    
    
    public int getTableRowsMeasureAggregatedForGenericColumnCount(
	    String tableName, MeasureAggregation measureAggregation,
	    MdGenericColumn genericColumn, Integer skip, Integer top,
	    String filterExpression, String orderBy)
	    throws OdataDomainException {

	StringBuilder query = new StringBuilder();
	query.append("SELECT COUNT(*) FROM ( ");
	query.append(getQueryMeasureAggregatedForGenericColumn(tableName,
		measureAggregation, genericColumn, skip, top, filterExpression,
		orderBy));
	query.append(" ) Q");

	int res = getJdbcTemplate().queryForInt(query.toString());
	return res;
    }

    @Override
    public MinMaxCount getMinMaxCountForMeasureAggregatedForGenericColumn(String tableName,
	    MeasureAggregation measureAggregation, MdGenericColumn genericColumn, String filterExpression) throws OdataDomainException {
	
	String minField = "min";
	String maxField = "max";
	String countField = "count";
	
	MdMeasureFields measure = measureAggregation.getMeasure();
	
	String query = 	"SELECT MIN(\"" + measure.getMeasureField() + "\") AS \"" + minField + "\", " + 
			"MAX(\"" + measure.getMeasureField() + "\") AS \"" + maxField + "\", " + 
			"COUNT(*) AS \"" + countField + "\" " + 
			"FROM (" + 
			getQueryMeasureAggregatedForGenericColumn(tableName, measureAggregation, genericColumn, null, null, null, null) + 
			") Q";
	
	List<Map<String, Object>> queryRes = getJdbcTemplate().queryForList(query);
	
	if(queryRes.size() != 1){
	    throw new OdataDomainException("MinMaxCount: lunghezza errata del risultato della query");
	}
	
	Object minObj = queryRes.get(0).get(minField);
	Object maxObj = queryRes.get(0).get(maxField);
	Object countObj = queryRes.get(0).get(countField);
	
	String min = minObj != null ? minObj.toString() : "";
	String max = maxObj != null ? maxObj.toString() : "";
	String count = countObj != null ? countObj.toString() : "";
	
	MinMaxCount mmc = new MinMaxCount(min, max, count);
	return mmc;
    }
    
    /**
     * Compone l'SQL di un'aggregazione di una misura per una colonna generica
     * @param tableName	nome della tabella
     * @param measureAggregation	misura con relativa funzione di aggregazione
     * @param genericColumn	colonna generica sulla quale raggruppare
     * @param skip	da (paginazione)
     * @param top	numero di risultati
     * @param filterExpression	eventuali filtri
     * @param orderBy	eventuali ordinamenti
     * @return	l'SQL
     */
    private String getQueryMeasureAggregatedForGenericColumn(String tableName,
	    MeasureAggregation measureAggregation,
	    MdGenericColumn genericColumn, Integer skip, Integer top, String filterExpression, String orderBy){
	
	String query = 	"SELECT " + "\"" + tableName + "\".\"" + genericColumn.getColumnField() + "\" AS \"" + genericColumn.getColumnField() + "\" " + ", " + 
			getSqlAggregationSelect(tableName, measureAggregation, true) + " AS \"" + measureAggregation.getMeasure().getMeasureField() + "\" FROM " + tableName + " " + 
			((filterExpression != null && !filterExpression.trim().equals("")) ? "WHERE " + filterExpression + " " : "") +
			"GROUP BY " + "\"" + tableName + "\".\"" + genericColumn.getColumnField() + "\" " + 
			((orderBy != null && !orderBy.trim().equals("")) ? " ORDER BY " + orderBy + " NULLS LAST " : "") +
			((top != null && top.intValue() >= 0) ? " LIMIT " + top.intValue() + " " : "" ) +
			((skip != null && skip.intValue() >= 0) ? "OFFSET " + skip.intValue() + " " : "");
	
	return query;
    }

    @Override
    public Integer getDifferentDistictCount(String tableName, String fieldName) {
	try{
	    	tableName = tableName.toLowerCase();
	    	fieldName = fieldName.toLowerCase();
        	String query =	"select count(*) from ( " + 
        		    	"select distinct(\"count\"(\"" + tableName + "\".\"" + fieldName + "\")) " + 
        		    	"from \"" + tableName + "\"" +
        		    	"group by \"" + tableName + "\".\"" + fieldName + "\" " +
        		    	") q ";
        	
        	int res = getJdbcTemplate().queryForInt(query.toString());
        	return new Integer(res);
	}catch(Exception e){
	    // potrebbe capitare magari che nella metainformazione ci sono delle tabelle
	    // (o colonne) che non corrispondono nel db.
	    return null;
	}
    }

    @Override
    public void createView(String viewName, String tableName,
	    List<String> columns, String uidName) {
	
	StringBuilder columnsStringify = new StringBuilder();
	for(String s : columns){
	    if(columnsStringify.length() > 0){
		columnsStringify.append(",");
	    }
	    
	    columnsStringify.append("\"" + tableName + "\"" + ".\"" + s + "\"");
	}
	
	String sqlCreationView = 	"CREATE VIEW " + viewName + " AS " +
					"SELECT row_number() over (order by " + columnsStringify.toString() + ") AS " + uidName + ", " + columnsStringify.toString() + " FROM " + tableName;
	
	//System.out.println(sqlCreationView);
	getJdbcTemplate().execute(sqlCreationView);
	
    }
    
    @Override
    public boolean primaryKeyExists(String tableName) {
	return getPrimaryKeys(tableName).size() > 0;
    }

    @Override
    public void dropView(String viewName) {
	String query = "DROP VIEW \"public\".\"" + viewName + "\"";
	getJdbcTemplate().execute(query);
    }

    @Override
    public List<Map<String, Object>> getPrimaryKeys(String tableName) {
	
	String tableCatalog = databaseName();
	String tableSchema = "public";
	
	String query = 	"SELECT  kcu.column_name " +
			"FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc " + 
			"LEFT JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu " +  
				"ON kcu.table_catalog = tc.table_catalog " + 
				"AND kcu.table_schema = tc.table_schema " +
				"AND kcu.table_name = tc.table_name " +
				"AND kcu.constraint_name = tc.constraint_name " + 
			"WHERE " +
				"tc.table_catalog = '" + tableCatalog + "' " +
				"AND tc.table_schema = '" + tableSchema + "' " +
				"AND tc.table_name = '" + tableName + "' " + 
				"AND tc.constraint_type = 'PRIMARY KEY' " +
			"ORDER BY kcu.ordinal_position";

	List<Map<String, Object>> keyList = getJdbcTemplate().queryForList(query);
	return keyList;
    }

    @Override
    public void deleteFromGeometry_columns(String tableName) {
	String query = 	"DELETE FROM \"public\".\"geometry_columns\" " + 
			"WHERE f_table_name='" + tableName.toLowerCase() + "'";
	
	getJdbcTemplate().execute(query);
    }
    
}
