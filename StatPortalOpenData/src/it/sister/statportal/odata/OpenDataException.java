package it.sister.statportal.odata;

/**
 * Generica eccezione che viene generata dai producer
 *
 */
public class OpenDataException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public OpenDataException(final String message){
		super(message);
	}
	
	public OpenDataException(final Exception exception){
		super(exception);
	}

	public OpenDataException(){
		super();
	}
}
