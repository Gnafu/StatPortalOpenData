package it.sister.statportal.odata.domain;


import it.sister.statportal.odata.domain.MdData.FIELD_TO_SORT;


import it.sister.statportal.odata.domain.MdData.SORTING_DIRECTION;
import it.sister.statportal.odata.utility.DBUtils.DBColumnType;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.management.openmbean.OpenDataException;

import org.aspectj.lang.annotation.Pointcut;

/**
 * 
 * Interfaccia per il Repository. Il Repository viene iniettato dinamicamente
 * attraverso Dependency Injection. Il file di configurazione è beans.xml
 * 
 */
public interface IRepository {

    /**
     * Restituisce l'insieme delle colonne di una tabella
     * 
     * @param tableName
     *        nome della tabella
     * @return la lista delle colonne
     */
    public List<Column> getTableColumns(String tableName);

    /**
     * Restituisce una lista di righe di una particolare tabella
     * 
     * @param tableName
     *        nome della tabella
     * @return l'insieme delle righe
     */
    public List<Row> getTableRows(String tableName, List<Column> columns)
	    throws OdataDomainException;

    /**
     * Restituisce parte delle righe della tabella
     * 
     * @param tableName
     *        nome della tabella
     * @param startIndex
     *        indice di partenza (0 indica il primo record)
     * @param count
     *        numero di risultati
     * @return l'insieme delle righe
     */
    public List<Row> getTableRows(String tableName, List<Column> columns,
	    int startIndex, int count) throws OdataDomainException;

    /**
     * Restituisce parte delle righe della tabella
     * 
     * @param tableName
     *        nome della tabella
     * @param startIndex
     *        indice di partenza (0 indica il primo record)
     * @param count
     *        numero di risultati
     * @param filter
     *        filtri
     * @return l'insieme delle righe
     */
    public List<Row> getTableRows(String tableName, List<Column> columns,
	    int startIndex, int count, String filter)
	    throws OdataDomainException;

    /**
     * Restituisce i risultati di una query sulla tabella
     * 
     * @param tableName
     *        nome della tabella
     * @param mdDataDims
     *        insieme delle dimensioni
     * @param measureAggregation
     *        insieme delle misure (ed eventuali funzioni di aggregazione)
     * @param startIndex
     *        indice di partenza (0 indica il primo record)
     * @param count
     *        numero di risultati
     * @param groupByEnabled
     *        indica se si vuole effettuare un raggruppamento. Nel caso in cui
     *        sia true e non siano definite delle funzioni di aggregazione per
     *        le misure sarà applicata la SUM di default
     * @param filterExpression
     *        espressione testuale che rappresenta un filtro sui dati
     * @param orderBy
     *        espressione testuale che rappresenta l'ordinamento. Esempi:
     *        "NOME_FISICO_COLONNA DESC"
     *        "NOME_FISICO_COLONNA1 DESC, NOME_FISICO_COLONNA2 ASC"
     * @param locale
     * 	      localizzazione
     * @return l'insieme delle righe
     */
    public List<Row> getTableRows(String tableName, List<Column> columns,
	    List<MdDataDim> mdDataDims,
	    List<MeasureAggregation> measureAggregation, int startIndex,
	    int count, boolean groupByEnabled, String filterExpression,
	    String orderBy, List<MdDataDim> joinedDimensions, Locale locale) throws OdataDomainException;

    public void createView(String tableName, String viewName,
	    List<MdDataDim> mdDataDims,
	    List<MeasureAggregation> measureAggregation, boolean groupByEnabled, String filterExpression);

    /**
     * Restituisce la count
     * 
     * @param tableName
     *        nome della tabella
     * @param mdDataDims
     *        insieme delle dimensioni
     * @param measureAggregation
     *        insieme delle misure (ed eventuali funzioni di aggregazione)
     * @param startIndex
     *        indice di partenza (0 indica il primo record)
     * @param count
     *        numero di risultati
     * @param groupByEnabled
     *        indica se si vuole effettuare un raggruppamento. Nel caso in cui
     *        sia true e non siano definite delle funzioni di aggregazione per
     *        le misure sarà applicata la SUM di default
     * @param filterExpression
     *        espressione testuale che rappresenta un filtro sui dati
     * @param orderBy
     *        espressione testuale che rappresenta l'ordinamento. Esempi:
     *        "NOME_FISICO_COLONNA DESC"
     *        "NOME_FISICO_COLONNA1 DESC, NOME_FISICO_COLONNA2 ASC"
     * @return il numero di righe della collezione
     */
    public int getCountRows(String tableName, List<MdDataDim> mdDataDims,
	    List<MeasureAggregation> measureAggregation, int startIndex,
	    int count, boolean groupByEnabled, String filterExpression,
	    String orderBy, List<MdDataDim> joinedDimensions, boolean excludeNullByCount);
    
    /**
     * Conta il numero di occorrenze diverse (cardinalità) del campo field nella tabella tableName
     * 
     * @param tableName nome della tabella
     * @param field campo di cui si vuole sapere la cardinalità
     * @return cardinalità del campo passato
     */
    public int getCardinality(String tableName, String field);
    
    
    /***
     * Restituisce le righe contenente i punti formattati in kml
     * IMPORTANTE: nella query la definizione dei punti (tag points) deve comparire per prima, ovvero
     * la query deve essere della forma "select asKml(geomField), * from tableName"
     * @param data dato dal quale ricavare il kml
     * @param geomField campo che definisce la geometria
     * @param pointField campo che definisce l'oggetto punto in xml (contenente le coordinate)
     * @return le righe contenente i punti formattati in kml
     */
    public List<Map<String, Object>> getKmlPoints(MdData data, String geomField, String pointField);

    /**
     * Restituisce un insieme di righe contenenti tutti i valori distinti della colonna
     * passata come parametro e il numero di occorrenze degli stessi
     * @param tableName
     * @param field
     * @param joinTableName
     * @param joinFieldName
     * @param joinKeyName
     * @param top
     * @return un insieme di righe contenenti tutti i valori distinti della colonna
     * @throws OdataDomainException
     */
    public List<Map<String, Object>> getDistinctValueAndCountForField(String tableName, String field, String joinTableName, String joinFieldName, String joinKeyName, int skip, int top, FIELD_TO_SORT fieldToSort, SORTING_DIRECTION sortingDirection, String filter) throws OdataDomainException;
    
    /**
     * Esegue una query
     * @param query
     * @return la lista (il risultato della query)
     */
    public List<Map<String, Object>> executeQueryForList(String query);
    
    /**
     * Restituisce true se esiste già una tabella con lo stesso nome
     * @param tableName
     * @return true se esiste già una tabella con lo stesso nome, false altrimenti
     */
    public boolean tableExists(String tableName);
    
    
    /**
     * Restituisce il nome del db a cui siamo attualmente connessi
     * @return il nome del db a cui siamo attualmente connessi
     */
    public String databaseName();
    
    /**
     * inserisce una nuova tabella (con struttura passata) nel db
     * @param tableName nome della tabella
     * @param columnsName nomi delle colonne
     * @param columnsType tipi delle colonne
     * @param primaryKey
     * @return true se è andato tutto bene, false altrimenti
     * @throws Exception
     */
    public boolean createTable(String tableName, String[] columnsName, DBColumnType[] columnsType, String primaryKey) throws Exception;
    
    /**
     * Inserisce una riga in una tabella
     * @param tableName nome della tabella
     * @param columns colonne della tabella
     * @param values valori da associare alle colonne
     * @return true se è andato tutto bene, false altrimenti
     */
    public boolean insertRow(String tableName, String[] columns, List<String[]> values);
    
    /**
     * Cancella una tabella dal db
     * @param tableName
     * @return true se la cancellazione è andata a buon fine, false altrimenti
     */
    public boolean dropTable(String tableName);
    
    /**
     * Esegue l'escape della stringa passata come parametro
     * @param toEscape	stringa sulla quale eseguire l'escape
     * @return	l'escape della stringa 
     */
    public String escape(String toEscape);
    
    /**
     * Espone il carattere di escape
     * @return il carattere di escape
     */
    public String getEscapeSequence();
    
    /**
     * Indica se esiste già una vista con il nome indicato
     * @param viewName	nome della vista
     * @return	true se esiste già una vista con lo stesso nome, false altrimenti
     */
    public boolean viewExists(String viewName);
    
    /**
     * Generazione di un nuovo UID
     * @return	un UID generato
     * @throws OdataDomainException
     */
    public String generateUid() throws OdataDomainException;
    
    /**
     * A partire da latitudine e longitudine, genera la stringa che restituisce la geometria (nel db geo)
     * @param longitude	latitudine
     * @param latitude	longitudine
     * @return	l'SQL per la generazione della geometria a partire dalle coordinate
     */
    public String getPointStringFromCoordinates(String longitude, String latitude);
    
    /**
     * Restituisce un intero a partire dal valore di una colonna di tipo string (unique)
     * @param tableName nome della tabella
     * @param stringField nome del campo stringa
     * @param stringValue valore del campo stringa
     * @param intField nome del campo stringa
     * @return l'intero
     */
    public int getIntValueFromUniqueStringField(String tableName, String stringField, String stringValue, String intField);
    
    /**
     * Espone l'insieme di colonne che formano la chiave primaria di una particolare tabella
     * @param tableName nome della tabella
     * @return l'insieme di colonne che formano la chiave primaria
     */
    public List<Map<String, Object>> getPrimaryKeys(String tableName);
    
    /**
     * Restituisce il minimo e il massimo valore della misura aggregata
     * @param tableName	nome della tabella
     * @param mdDataDims	elenco delle dimensioni sulle quali aggregare
     * @param measureAggregation	misura (con relativa funzione di aggregazione)
     * @param startIndex	indice di partenza (per la paginazione)
     * @param count	numero di risultati (per la paginazione)
     * @param groupByEnabled	abilita raggruppamento
     * @param filterExpression	eventuali filtri
     * @param orderBy	eventuali ordinamenti
     * @param joinedDimensions	eventuali altre dimensioni da mettere in join
     * @param excludeNullByCount	esclusione dei valori null dalla count
     * @return il minimo e il massimo valore della misura aggregata
     */
    public MinMax getMinMaxForAggregatedMeasure(String tableName, List<MdDataDim> mdDataDims, MeasureAggregation measureAggregation, int startIndex,int count, boolean groupByEnabled, String filterExpression, String orderBy, List<MdDataDim> joinedDimensions, boolean excludeNullByCount);

    /**
     * Calcola il minimo e il massimo count dei valori distinti di un particolare campo della tabella 
     * 
     * @param tableName
     * @param field
     * @param joinTableName
     * @param joinFieldName
     * @param joinKeyName
     * @param skip
     * @param top
     * @param fieldToSort
     * @param sortingDirection
     * @param filter
     * @return il minimo e il massimo count dei valori distinti di un particolare campo della tabella
     * @throws OdataDomainException
     */
    public MinMax getMinMaxCountForDistinctValueAndCountForField(String tableName, String field, String joinTableName, String joinFieldName, String joinKeyName, int skip, int top, FIELD_TO_SORT fieldToSort, SORTING_DIRECTION sortingDirection, String filter) throws OdataDomainException;;
    
    /**
     * Espone il numero di righe di una particolare tabella
     * @param tableName	nome della tabella
     * @return	il numero di righe della tabella
     */
    public int getNumRows(String tableName);
    
    /**
     * Restituisce informazioni sulle colonne della tabella
     * @param tableName	nome della tabella
     * @return	le informazioni sui tipi delle colonne della tabella
     */
    public HashMap<String, SimpleColumnInfo> getDbTypeColumnInfo(String tableName);
    
    /**
     * Aggiunge una colonna geometria alla tabella
     * @param tableName	nome della tabella
     * @param geom_type	tipo della geometria
     * @return	true in caso di esito positivo, false altrimenti
     */
    public boolean addGeometryColumn(String tableName, GeometryType geom_type);
    
    /**
     * Calcola il numero minimo e massimo di valori distinti per una particolare colonna
     * @param tableName	nome della tabella
     * @param descriptiveField	nome del campo descrittivo
     * @param dimension	dimensione
     * @return	il minimo e il massimo
     * @throws OdataDomainException
     */
    public MinMaxCount getMinMaxCountForColumn(String tableName, String descriptiveField, MdDataDim dimension) throws OdataDomainException;
    
    /**
     * Recupera il contenuto più ricorrente di una colonna
     * @param tableName	nome della tabella
     * @param descriptiveField	nome del campo descrittivo
     * @param dimension	dimensione
     * @return	una stringa che rappresenta il contenuto più ricorrente
     */
    public String getMoreRecurrentContent(String tableName, String descriptiveField, MdDataDim dimension);
    
    /**
     * Indica se la tabella ha una geometria di tipo puntuale
     * @param tableName	nome della tabella
     * @return	true se la geometria è di tipo puntuale, false altrimenti
     */
    public boolean isPunctualGeometry(String tableName);
    
    /**
     * Aggiunge le due colonne latitudine e longitudine a partire dalla geometria in tabella (precondizione che deve essere verificata
     * prima di invocare il metodo)
     * @param tableName	nome della tabella
     * @param latitudeCol	colonna indicante la latitudine
     * @param longitudeCol	colonna indicante la longitudine
     */
    public void createLatLngColumns(String tableName, String latitudeCol, String longitudeCol);

    /**
     * Restituisce le righe di una tabella formata da una misura aggregata per una colonna generica
     * @param tableName	nome della tabella
     * @param measureAggregation	misura con relativa funzione di aggregazione
     * @param genericColumn	colonna generica sulla quale raggruppare
     * @param skip	da (paginazione)
     * @param top	numero di risultati
     * @param filterExpression	eventuali filtri
     * @param orderBy	eventuale ordinamento
     * @return	le righe di una tabella formata da una misura aggregata per una colonna generica
     * @throws OdataDomainException
     */
    public List<Row> getTableRowsMeasureAggregatedForGenericColumn(String tableName, MeasureAggregation measureAggregation, MdGenericColumn genericColumn, Integer skip, Integer top, String filterExpression, String orderBy) throws OdataDomainException;
    
    /**
     * Restituisce il numero di risultati per l'aggregazione di una misura per una colonna generica
     * @param tableName	 nome della tabella
     * @param measureAggregation	misura con relativa funzione di aggregazione
     * @param genericColumn	colonna generica sulla quale raggruppare
     * @param skip	da (paginazione)
     * @param top	numero di risultati
     * @param filterExpression	eventuali filtri
     * @param orderBy	eventuali ordinamenti
     * @return	il numerio di risultati per l'aggregazione richiesta
     * @throws OdataDomainException
     */
    public int getTableRowsMeasureAggregatedForGenericColumnCount(String tableName, MeasureAggregation measureAggregation, MdGenericColumn genericColumn, Integer skip, Integer top, String filterExpression, String orderBy) throws OdataDomainException;

    /**
     * 
     * Calcola il minimo e il massimo della count di una misura aggregata per una colonna generica 
     * @param tableName
     * @param measureAggregation
     * @param genericColumn
     * @param filterExpression
     * @return il minimo e il massimo della count di una misura aggregata per una colonna generica
     * @throws OdataDomainException
     */
    public MinMaxCount getMinMaxCountForMeasureAggregatedForGenericColumn(String tableName, MeasureAggregation measureAggregation, MdGenericColumn genericColumn, String filterExpression) throws OdataDomainException;
    
    /**
     * Calcola il numero di valori distinti della count di un particolare campo [select count(*) from (select distinct(count(FIELD_NAME)) from TABLE_NAME group by FIELD_NAME)]
     * @param tableName
     * @param fieldName
     * @return il numero di valori distinti 
     */
    public Integer getDifferentDistictCount(String tableName, String fieldName);
    
    /**
     * Crea una vista a partire dalle colonne passate di una tabella ed aggiungendone una 'serial unique'
     * @param viewName nome della vista da creare
     * @param tableName tabella da cui partire
     * @param columns	colonne della tabella da includere
     * @param uidName	nome della colonna 'serial unique' da aggiungere
     */
    public void createView(String viewName, String tableName, List<String> columns, String uidName);
    
    /**
     * Controlla se esiste una chiave primaria sulla tabella
     * @param tableName tabella da controllare
     * @return true se esiste una chiave primaria
     */
    public boolean primaryKeyExists(String tableName);
    
    /**
     * Cancella una vista dal db
     * @param viewName	vista da cancellare
     */
    public void dropView(String viewName);
    
    /**
     * Cancella la riga di geometry_columns contenente le informazioni relative alla geometria
     * della tabella passata come parametro
     * @param tableName	nome della tabella
     */
    public void deleteFromGeometry_columns(String tableName);
    
    /**
     * 
     * 
     *
     */
    public class MinMax { 
	  public final Double min; 
	  public final Double max; 
	  public MinMax(Double min, Double max) { 
	    this.min = min; 
	    this.max = max; 
	  } 
    }
    
    /**
     * 
     *
     */
    public class SimpleColumnInfo{
	public final String columnType;
	public final Integer decimals;
	public SimpleColumnInfo(String columnType, Integer decimals) { 
	    this.columnType = columnType; 
	    this.decimals = decimals; 
	} 
    }
    
    /**
     * 
     *
     */
    public class MinMaxCount{
	public String min;
	public String max;
	public String count;
	
	public MinMaxCount(String min, String max, String count){
	    this.min = min;
	    this.max = max;
	    this.count = count;
	}
    }
    
    /**
     * 
     *
     */
    public enum GeometryType {
	POINT, MULTIPOLYGON;
    }

}  

