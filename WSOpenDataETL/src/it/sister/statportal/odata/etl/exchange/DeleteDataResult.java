package it.sister.statportal.odata.etl.exchange;

/**
 * Risultato dell'operazione di cancellazione di un datoi
 *
 */
public class DeleteDataResult {

	protected boolean deleted;
	
	protected String message;
	
	/**
	 * Costruisce l'oggetto
	 * @param deleted flag che indica se il dato è stato correttamente cancellato
	 * @param message eventuali messaggi
	 */
	public DeleteDataResult(boolean deleted, String message){
		this.deleted = deleted;
		this.message = message;
	}
	
	/**
	 * Costruttore vuoto necessario per esporre il tipo come tipo di ritorno di un'operazione del web service.
	 */
	public DeleteDataResult(){}
	
	/**
	 * Imposta il flag 
	 * @param value il valore del flag
	 */
	public void setDeleted(boolean value){
		this.deleted = value;
	}
	
	/**
	 * Imposta il messaggio 
	 * @param value il messaggio
	 */
	public void setMessage(String value){
		this.message = value;
	}
	
	/**
	 * Fornisce il valore del flag
	 * @return il valore del flag
	 */
	public boolean getDeleted(){
		return this.deleted;
	}
	
	/**
	 * Fornisce il valore del messaggio
	 * @return il valore del messaggio
	 */
	public String getMessage(){
		return this.message;
	}
}
