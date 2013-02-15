package it.sister.statportal.odata.proxy;

/**
 * Generica eccezione che viene generata dai producer
 *
 */
public class OpenDataException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public OpenDataException(final Exception exception){
		super(exception);
	}

	public OpenDataException(){
		super();
	}
}