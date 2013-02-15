package it.sister.statportal.odata.domain;

import java.util.List;

/**
 * 
 * Interfaccia che definisce le operazioni possibili su tabelle (strutturate e non)
 *
 */
public interface IData {

    /**
     * Restituisce informazioni sulle colonne della tabella
     * @return la lista delle righe
     */
    List<Column> getColumns() throws OdataDomainException;
    
    /**
     * Restituisce tutte le righe di una tabella (dato grezzo)
     * @return la lista delle righe
     * @throws OdataDomainException
     */
    List<Row> getRows() throws OdataDomainException;
    
    /**
     * Restituisce la count delle righe di una tabella
     * @return il numero di righe
     * @throws OdataDomainException
     */
    int getCountRows() throws OdataDomainException;
    
    /**
     * 
     * @param filter filtri da applicare alla tabella
     * @return in numero di righe che rispettano la condizione impostata
     * @throws OdataDomainException
     */
    int getCountRows(String filter) throws OdataDomainException;
    
    /**
     * Restituisce le righe della tabella (paginate)
     * @param startIndex indice di partenza (0 indica il primo record)
     * @param count numero di risultati
     * @return la lista delle righe
     * @throws OdataDomainException
     */
    List<Row> getRows(int startIndex, int count, String filter)  throws OdataDomainException;
    
    
    /**
     * Metodo creato appositamente per implementare i filtri sulle dimensioni (in dati strutturati)
     * Nel caso di DbTable e MdHierNode viene restituito il parametro in input
     * 
     * @param physicalName nome fisico
     * @return campo descrittivo
     */
    String getDescriptionField(String physicalName);
    
}
