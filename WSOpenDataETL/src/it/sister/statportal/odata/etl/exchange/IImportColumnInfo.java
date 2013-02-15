package it.sister.statportal.odata.etl.exchange;


public interface IImportColumnInfo {

	public enum ColumnType{
		DIMENSION,
		MEASURE,
		GENERIC
	}
	
	/**
	 * Espone l'indice della colonna.
	 * Da usare nel caso di file Excel che non forniscono l'accesso alle 
	 * colonne attraverso il nome
	 * @return l'indice della colonna
	 */
	public int getColumnIndex();
	
	/**
	 * Impota l'indice della colonna
	 * @param value l'indice della colonna
	 */
	public void setColumnIndex(int value);
	
	/**
	 * Espone il nome della colonna del file associata alla dimensione
	 * @return il nome delle colonna del file associata alla dimensione
	 */
	public String getColumnName();
	
	/**
	 * Imposta il valore del nome della colonna del file associata alla dimensione
	 * @param value il nome della colonna del file associata alla dimensione
	 */
	public void setColumnName(String value);
	
	
	/**
	 * Restituisce il tipo della colonna
	 * @return
	 */
	public ColumnType getColumnType();
	
	/**
	 * Espone la posizione della colonna
	 * @return la posizione della colonna
	 */
	public int getPos();
	
	/**
	 * Imposta la posizione della colonna
	 * @param value la posizione della colonna
	 */
	public void setPos(int value);
}
