package it.sister.statportal.odata.domain;

/**
 * 
 * Eccezione generica lanciata all'interno del dominio
 *
 */
public class OdataDomainException extends Exception {

    public OdataDomainException(){
	super();
    }
    
    public OdataDomainException(String message){
	super(message);
    }
}
