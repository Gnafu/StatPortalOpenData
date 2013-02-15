package it.sister.statportal.odata.etl.reader;

import java.util.List;

import it.sister.statportal.odata.etl.WSEtl.VerifyUniquenessStatus;
import it.sister.statportal.odata.etl.exchange.ColumnInfo;
import it.sister.statportal.odata.etl.exchange.DimensionInfo;
import it.sister.statportal.odata.etl.exchange.GenericColumnInfo;
import it.sister.statportal.odata.etl.exchange.IImportColumnInfo;
import it.sister.statportal.odata.etl.exchange.MeasureInfo;
import it.sister.statportal.odata.etl.exchange.TableInfo;
import it.sister.statportal.odata.etl.exchange.WarningSet;

/**
 * Generico reader su un file.
 * Questa interfaccia viene implementata
 * da i vari reader per i file CSV, DBF, XLS, etc.
 *
 */
public interface IFileReader {

	/**
	 * Estrae dal file le informazioni sulle sue colonne
	 * @return Un array di informazioni sulle colonne del file
	 * @throws Exception Propaga le eccezioni del livello sottostante
	 */
	ColumnInfo[] readColumnInfo(boolean isShape, String extraArguments) throws Exception;
	
	/**
	 * Analizza il file e verifica che la colonna indicata contenga valori univoci.
	 * @param columnName Il nome della colonna da leggere
	 * @return Un valore di un enumerato che indica se la colonna contiene valori univoci
	 * @throws Exception Propaga le eccezioni del livello sottostante
	 */
	VerifyUniquenessStatus verifyUniqueness(String columnName, String extraArguments) throws Exception;
	
	/**
	 * Fornisce l'insieme di warning legati al caricamento del file del reader
	 * @param dimensions l'insieme delle dimensioni
	 * @param measures l'insieme delle misure
	 * @param genericColumns l'insieme delle colonne generiche
	 * @param extraArguments eventuali parametri aggiuntivi
	 * @return Un insieme di warning
	 * @throws Exception 
	 */
	WarningSet getWarnings(DimensionInfo[] dimensions, MeasureInfo[] measures, GenericColumnInfo[] genericColumns, String extraArguments) throws Exception;
	
	/**
	 * Crea sul db una tabella e la riempie con 
	 * il contenuto di un file.
	 * @param columnList la lista delle colonne
	 * @return La descrizione della tabella creata.
	 * @throws Exception 
	 */
	TableInfo writeTable(List<IImportColumnInfo> columnList, String extraArguments) throws Exception;
	
	/**
	 * Calcola il numero di valori distinti presenti in una colonna
	 * @param columnName la colonna
	 * @return il numero di valori distinti presenti nella colonna
	 * @throws Exception
	 */
	int getCardinality(String columnName, String extraArguments) throws Exception;
	
	/**
	 * Restituisce il numero di righe del file
	 * @return il numero di righe del file
	 * @throws Exception
	 */
	int count(String extraArguments) throws Exception;
	
	
	/**
	 * Estrae dal file le informazioni sui nomi delle colonne
	 * @return Un array di nomi di colonne
	 */
	public List<String> getOriginalNames(List<IImportColumnInfo> columnList) throws Exception;
}
