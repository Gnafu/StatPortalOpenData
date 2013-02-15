package it.sister.statportal.odata.etl.exchange;

/**
 * Un generico warning generato durante il caricamento di un file
 *
 */
public class Warning {

	/**
	 * Messaggio riportato dal warning
	 */
	protected String message;
	
	/**
	 * Tipo di warning
	 */
	protected WarningType type;
	
	/**
	 * Crea un nuovo warning
	 * @param message il messaggio
	 * @param type il tipo
	 */
	public Warning(String message, WarningType type){
		this.message = message;
		this.type = type;
	}
	
	/**
	 * Espone il messaggio del warning
	 * @return il messaggio del warning
	 */
	public String getMessage(){
		return this.message;
	}
	
	/**
	 * Espone il tipo di warning
	 * @return il tipo di warning
	 */
	public WarningType getType(){
		return this.type;
	}
}
