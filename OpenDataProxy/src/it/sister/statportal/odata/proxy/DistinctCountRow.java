package it.sister.statportal.odata.proxy;

/**
 * Generico oggetto DistinctCountRow che
 * rappresenta una riga della navigation property DistinctCountRow
 *
 */
public class DistinctCountRow {
	
	protected int id;
	
	protected int count;
	
	protected String label;
	
	/**
	 * Costruisce l'astrazione della riga
	 * @param id id 
	 * @param count contatore
	 * @param label etichetta
	 */
	public DistinctCountRow(int id, int count, String label){
		this.id = id;
		this.count = count;
		this.label = label;
	}
	
	/**
	 * Fornisce l'id
	 * @return l'id
	 */
	public int getId(){
		return this.id;		
	}
	
	/**
	 * Fornisce il valore del contatore
	 * @return il valore del contatore
	 */
	public int getCount(){
		return this.count;
	}
	
	/**
	 * Fornisce il valore dell'etichetta
	 * @return il valore dell'etichetta
	 */
	public String getLabel(){
		return this.label;
	}
	
}
