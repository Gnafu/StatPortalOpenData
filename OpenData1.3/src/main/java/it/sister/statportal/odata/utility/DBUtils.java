package it.sister.statportal.odata.utility;

import it.sister.statportal.odata.domain.OdataDomainException;
import it.sister.statportal.odata.domain.RepositoryFactory;
import it.sister.statportal.odata.domain.IRepository.GeometryType;
import it.sister.statportal.odata.domain.IRepository.SimpleColumnInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBUtils {
    
    /**
     * Esegue una query che ritorna un insieme di righe come risultato
     * @param query query da eseguire
     * @return l'insieme di righe
     */
    public static List<Map<String, Object>> executeQueryForList(String query){
	return RepositoryFactory.getRepository().executeQueryForList(query);
    }
    
    /**
     * Controlla l'esistenza di una tabella nel db corrente
     * @param tableName nome della tabella
     * @return true se la tabella esiste, false altrimenti
     */
    public static boolean tableExists(String tableName){
	return RepositoryFactory.getRepository().tableExists(tableName);
    }
    
    public static boolean primaryKeyExists(String tableName){
	return RepositoryFactory.getRepository().primaryKeyExists(tableName);
    }
    
    /**
     * Espone l'insieme di colonne che formano la chiave primaria di una particolare tabella
     * @param tableName nome della tabella
     * @return l'insieme di colonne che formano la chiave primaria
     */
    public static List<Map<String, Object>> getPrimaryKeys(String tableName){
	return RepositoryFactory.getRepository().getPrimaryKeys(tableName);
    }
    
    /**
     * Restituisce il nome del database corrente
     * @return il nome del database corrente
     */
    public static String getDatabaseName(){
	return RepositoryFactory.getRepository().databaseName();
    }
    
    /**
     * Crea una tabella nel db
     * @param tableName nome della tabella
     * @param columnsName nomi delle colonne
     * @param columnsType tipi delle colonne
     * @return true se tutto è andato bene, false altrimenti
     * @throws Exception
     */
    public static boolean createTable(String tableName, String[] columnsName, DBColumnType[] columnsType, String primaryKey) throws Exception{
	if(columnsName.length != columnsType.length){
	    throw new Exception("La lunghezza delle colonne deve essere uguale a quella dei tipi");
	}
	return RepositoryFactory.getRepository().createTable(tableName, columnsName, columnsType, primaryKey);
    }
    
    /**
     * Inserisce una nuova riga in una tabella
     * @param tableName nome della tabella
     * @param columns insieme delle colonne
     * @param values insieme dei valori da assegnare alle colonne
     * @return true se l'inserzione è avvenuta correttamente, false altrimenti
     * @throws Exception 
     */
    public static boolean insertRow(String tableName, String[] columns, List<String[]> values) throws Throwable{
	int idx;
	int valLen = values.size();
	int colLen = columns.length;
	
	if(values.size() == 0 || columns.length == 0){
	    return true;
	}
	
	for(idx = 0; idx < valLen; idx++){
	    if(colLen != values.get(idx).length){
		    throw new Exception("La lunghezza delle colonne deve essere uguale a quella dei valori");
	    }
	}
	return RepositoryFactory.getRepository().insertRow(tableName, columns, values);
    }
    
    public static boolean dropTable(String tableName){
	return RepositoryFactory.getRepository().dropTable(tableName);
    }
    
    public static void dropView(String viewName){
	RepositoryFactory.getRepository().dropView(viewName);
    }
    
    public static String escape(String toEscape){
	return RepositoryFactory.getRepository().escape(toEscape);
    }
    
    public static String getEscapeSequence(){
	return RepositoryFactory.getRepository().getEscapeSequence();
    }
    
    public static boolean viewExists(String viewName){
	return RepositoryFactory.getRepository().viewExists(viewName);
    }
    
    /**
     * Genera un uid random
     * 
     * @return l'uid oppure null nel caso si sia verificata qualche eccezione
     */
    public static String generateUid(){
	try {
	    return RepositoryFactory.getRepository().generateUid();
	} catch (OdataDomainException e) {
	    return null;
	}
    }
    
    public static int getIntValueFromUniqueStringField(String tableName, String stringField, String stringValue, String intField){
	return RepositoryFactory.getRepository().getIntValueFromUniqueStringField(tableName, stringField, stringValue, intField);
    }
    
    public static HashMap<String, SimpleColumnInfo> getDbTypeColumnInfo(String tableName){
	return RepositoryFactory.getRepository().getDbTypeColumnInfo(tableName);
    }
    
    public static String getPointStringFromCoordinates(String longitude, String latitude){
	return RepositoryFactory.getRepository().getPointStringFromCoordinates(longitude, latitude);
    }
    
    public static boolean addGeometryColumn(String tableName, GeometryType geom_type){
	return RepositoryFactory.getRepository().addGeometryColumn(tableName, geom_type);
    }
    
    public static boolean isPunctualGeometry(String tableName){
	return RepositoryFactory.getRepository().isPunctualGeometry(tableName);
    }
    
    public static void createLatLngColumns(String tableName, String latitudeCol, String longitudeCol){
	RepositoryFactory.getRepository().createLatLngColumns(tableName, latitudeCol, longitudeCol);
    }
    
    public static void deleteFromGeometry_columns(String tableName){
	RepositoryFactory.getRepository().deleteFromGeometry_columns(tableName);
    }
    
    public enum DBColumnType {
	 STRING, 
	 SHORT, 
	 INT, 
	 DOUBLE, 
	 GEOMETRY;
    }
    

}
