package it.sister.statportal.odata.etl.exchange;

import it.sister.statportal.odata.domain.Column;

import java.util.List;

/**
 * Classe che descrive una generica tabella creata importando
 * un file in StatPortalOpenData con l'ETL.
 * La descrizione riguarda il nome fisico della tabella e
 * le caratteristiche delle sue colonne.
 *
 */
public class TableInfo {

	//Nome fisico della tabella descritta dall'oggetto
	protected String tableName;
	
	//Nome del database
	protected String dbName;
	
	//numero di righe presenti nella tabella
	protected int numRows;
	
	//Lista di colonne della tabella descritta dall'oggetto
	protected List<Column> columns;
	
	//nomi che avevano le colonne nel file prima di venire importate
	protected List<String> originalColumnNames;
	
	protected boolean isGeometry;
	
	protected Pair<String,String> addedGeometryColumns;
	
	/**
	 * Costruisce la descrizione della tabella.
	 * @param tableName nome fisico
	 * @param dbName nome del db in cui si trova la tabella
	 * @param numRows numero di righe presenti nella tabella
	 * @param columns lista di descrizione delle colonne
	 * @param originalNames lista dei nomi delle colonne nel file da cui deriva la tabella
	 */
	public TableInfo(String tableName, String dbName, int numRows, List<Column> columns, List<String> originalNames, boolean isGeometry){
		this.tableName = tableName;
		this.dbName = dbName;
		this.numRows = numRows;
		this.columns = columns;
		this.originalColumnNames = originalNames;
		this.isGeometry = isGeometry;
	}
	
	/**
	 * Espone il nome fisico della tabella descritta dall'oggetto.
	 * @return il nome fisico della tabella
	 */
	public String getTableName(){
		return this.tableName;
	}
	
	/**
	 * Espone l'insieme delle descrizioni delle colonne della tabella.
	 * @return l'insieme delle descrizioni delle colonne della tabella
	 */
	public List<Column> getColumns(){
		return this.columns;
	}
	
	/**
	 * Imposta l'insieme delle descrizioni delle colonne della tabella.
	 * @param columns l'insieme delle descrizioni delle colonna della tabella.
	 */
	public void setColumns(List<Column> columns){
		for(int i = 0; i <columns.size(); i++){
			if(columns.get(i).getPhysicalName().equals("the_geom") || columns.get(i).getPhysicalName().equals("gid")){
				columns.remove(i);
			}
		}
		this.columns = columns;
	}
	
	/**
	 * Espone il nome del db in cui si trova la tabella
	 * @return il nome del db in cui si trova la tabella
	 */
	public String getDbName(){
		return this.dbName;
	}
	
	/**
	 * Espone il numero di righe presenti nella tabella
	 * @return il numero di righe presenti nella tabella
	 */
	public int getNumRows(){
		return this.numRows;
	}
	
	/**
	 * Espone l'insieme dei nomi che avevano le colonne nel file
	 * da cui sono state importate
	 * @return la lista dei nomi delle colonne nel file da cui sono state importate
	 */
	public List<String> getOriginalColumnNames(){
		return this.originalColumnNames;
	}
	
	/**
	 * Dato il nome che avrebbe dovuto avere una colonna nel file
	 * usato per creare la tabella, individua la colonna della tabella corrispondente
	 * @param originalName Nome della colonna nel file
	 * @return
	 */
	public Column getColumn(String originalName){
		for(int i = 0; i < originalColumnNames.size(); i++){
			if(originalColumnNames.get(i).equals(originalName)){
				return columns.get(i);
			}
		}
		return null;
	}
	
	/**
	 * Restituisce il valore del flag che indica se la tabella contiene una geometria
	 * @return il valore del flag
	 */
	public boolean getIsGeometry(){
		return this.isGeometry;
	}
	
	/**
	 * Setta il valore delle due colonne latitudine e longitudine aggiunte dopo la creazione della tabella.
	 * Questo metodo serve nel caso del caricamento di uno shape quando si estraggono automaticamente dalla geometria 
	 * i valori x e y.
	 * @param newColumns coppia di nomi fisici delle nuove colonne.
	 */
	public void setAddedGeometryColumns(Pair<String, String> newColumns){
		this.addedGeometryColumns = newColumns;
	}
	
	/**
	 * Restituisce l'eventuale coppia dei nomi delle colonne (latitudine e longitudine) aggiunte
	 * automaticamente dopo il caricamento di uno shape puntuale
	 * @return la coppia dei nomi delle colonne latitudine-longitudine. Nel caso non esistano restituisce null.
	 */
	public Pair<String, String> getAddedGeometryColumns(){
		return this.addedGeometryColumns;
	}
}
