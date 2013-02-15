package it.sister.statportal.odata.etl.reader;

import it.sister.statportal.odata.domain.IRepository.GeometryType;
import it.sister.statportal.odata.etl.WSEtl;
import it.sister.statportal.odata.etl.WSEtl.VerifyUniquenessStatus;
import it.sister.statportal.odata.etl.exchange.ColumnInfo;
import it.sister.statportal.odata.etl.exchange.DimensionInfo;
import it.sister.statportal.odata.etl.exchange.GenericColumnInfo;
import it.sister.statportal.odata.etl.exchange.IImportColumnInfo;
import it.sister.statportal.odata.etl.exchange.MeasureInfo;
import it.sister.statportal.odata.etl.exchange.TableInfo;
import it.sister.statportal.odata.etl.exchange.Warning;
import it.sister.statportal.odata.etl.exchange.WarningSet;
import it.sister.statportal.odata.etl.exchange.ColumnInfo.ColumnDimension;
import it.sister.statportal.odata.etl.exchange.ColumnInfo.ColumnType;
import it.sister.statportal.odata.utility.DBUtils;
import it.sister.statportal.odata.utility.DBUtils.DBColumnType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.DataType;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Table;

/**
 * Reader per i file mdb di MS Access
 *
 */
public class MdbFileReader extends FileReader {

	public MdbFileReader(File file){
		super(file);			
	}
	
	@Override
	public ColumnInfo[] readColumnInfo(boolean isShape, String extraArguments)
			throws Exception {
		ColumnInfo[] columnInfo = null;
		Table table = null;
		try{
			table = getFirstTable();
			if(table != null){
				List<Column> columns = table.getColumns();
				int recordToRead = Math.min(WSEtl.getMaxVerifyRecords(), count(extraArguments));
				int scanningRecordToRead = Math.min(WSEtl.getMaxScanningRecords(), count(extraArguments));
				List<Map<String, Object>> records = new ArrayList<Map<String, Object>>(recordToRead);
				List<Map<String, Object>> recordsForScanning = new ArrayList<Map<String, Object>>(scanningRecordToRead);
				for(int i = 0; i < recordToRead; i++){
					Map<String, Object> row = table.getNextRow();
					records.add(row);
					recordsForScanning.add(row);
				}
				for(int i = recordToRead; i < scanningRecordToRead; i++){
					recordsForScanning.add(table.getNextRow());
				}
				columnInfo = initializeColumnInfo(columns);
				inferColumnType(columnInfo, columns, records, recordsForScanning);
			}
			return columnInfo; 
		} finally{
			if(table != null){
				table.getDatabase().close();
			}
		}
	}

	@Override
	public VerifyUniquenessStatus verifyUniqueness(String columnName,
			String extraArguments) throws Exception {
		VerifyUniquenessStatus status = VerifyUniquenessStatus.OK;
		Table table = null;
		try{
			table = getFirstTable();
			if(table != null){
				Collection<String> columns = new HashSet<String>();
				columns.add(columnName);
				Iterator<Map<String, Object>> rowIterator = table.iterator(columns);
				Set<String> rowValues = new HashSet<String>();
				//se il file non è vuoto
				while(rowIterator.hasNext()){
					String value = getCellValue(rowIterator.next(), columnName);
					if(rowValues.contains(value)){
						status = VerifyUniquenessStatus.NOT_UNIQUE;
						break;
					}else{
						rowValues.add(value);
					}
				}
			}
		} catch(Exception ex){
			status = VerifyUniquenessStatus.ERROR;
			status.setErrorMessage(ex.getMessage());
		} finally {
			if(table != null){
				table.getDatabase().close();
			}
		}
		return status;
	}

	@Override
	public TableInfo writeTable(List<IImportColumnInfo> columnList, String extraArguments) throws Exception {
		TableInfo tableInfo = null;
		List<String> originalNames = getOriginalNames(columnList);
		String tableName = getTableName(file.getName());
		String dbName = DBUtils.getDatabaseName();
		boolean geom = createTable(tableName, columnList);
		tableInfo = new TableInfo(tableName, dbName, count(""), new ArrayList<it.sister.statportal.odata.domain.Column>(), originalNames, geom);		
		return tableInfo;
	}

	@Override
	public int getCardinality(String columnName, String extraArguments)
			throws Exception {
		Table table = null;
		try{
			table = getFirstTable();
			if(table != null){
				if(table.getColumn(columnName) != null){
					Collection<String> columns = new HashSet<String>();
					columns.add(columnName);
					Iterator<Map<String,Object>> iterator = table.iterator(columns);
					Collection<String> distinctValues = new HashSet<String>();
					while(iterator.hasNext()){
						String value = getCellValue(iterator.next(), columnName);
						if(!distinctValues.contains(value)){
							distinctValues.add(value);
						}
					}
					return distinctValues.size();
				}else{
					return 0;
				}
			}else{
				return 0;
			}
		}catch(Exception e){
			return 0;
		}finally{
			if(table != null){
				table.getDatabase().close();
			}
		}
	}

	@Override
	public int count(String extraArguments) throws Exception {
		//si contano le righe della prima tabella
		Table table = null;
		try{
			table = getFirstTable();
			if(table != null){
				return table.getRowCount();
			}else{
				return 0;
			}
		}finally{
			//chiudo il db
			if(table != null){
				table.getDatabase().close();
			}
		}

	}

	@Override
	public WarningSet getWarnings(DimensionInfo[] dimensions, MeasureInfo[] measures, GenericColumnInfo[] genericColumns, String extraArguments) throws Exception {
		Table table = null;
		try{
			table = getFirstTable();
			if(table == null){
				throw new Exception("Non è presente una tabella");
			}
			Iterator<Map<String, Object>> rowIterator = table.iterator();
			//se il file non è vuoto
			List<Warning> warnings = new ArrayList<Warning>();
			int rowsWithWarning = 0;
			int index = 1;
			while(rowIterator.hasNext()){
				boolean rowWarning = false;
				Map<String, Object> row = rowIterator.next();
				//verifico le dimensioni
				rowWarning |= checkDimensionWarnings(index, row, dimensions, warnings);	
				//verifico le misure
				rowWarning |= checkMeasureWarnings(index, row,  measures, warnings);
				//verifico le colonne generiche
				rowWarning |= checkGenericColumnWarnings(index, row,  genericColumns, warnings);
				if(rowWarning){
					rowsWithWarning++;
				}
				index++;
			}
			return new WarningSet(rowsWithWarning < count(extraArguments), warnings.toArray(new Warning[warnings.size()]));
		}finally{
			if(table != null){
				table.getDatabase().close();
			}
		}
		
	}

	/**
	 * Individua e restituisce la prima tabella del db
	 * @return La prima tabella del db o null se il db è vuoto o in caso di eccezione
	 */
	protected Table getFirstTable(){
		Database db = null;
		try{
			db = Database.open(this.file);
			Set<String> tableNames = db.getTableNames();
			//file senza tabelle
			if(tableNames.size() == 0){
				return null;
			}
			//restituisco la prima tabella
			return db.getTable(tableNames.iterator().next());
		}catch(IOException ioe){
			//chiusura del file
			if(db != null){
				try {
					db.close();
				} catch (IOException e) {
					//in questo caso limite stampo l'errore
					e.printStackTrace();
				}
			}
			return null;
		}
	}
	
	/**
	 * Inizializza l'insieme di informazioni sulle colonne.
	 * Per iniziare imposta il numero di elementi nella dimensione
	 * e per ogni elemento imposta il nome.
	 * @param columns L'insieme delle colonne
	 * @return Un insieme di informazioni sulle colonne
	 */
	protected ColumnInfo[] initializeColumnInfo(List<Column> columns) throws Exception{
		ColumnInfo[] columnInfo = null;
		if(columns == null || columns.size() <= 0){
			columnInfo = new ColumnInfo[0];
		} else {
			columnInfo = new ColumnInfo[columns.size()];
			int i = 0;
			for(Column column : columns){	
				String name = column.getName();
//				if(name.length() > 63){
//					throw new Exception("Nome di colonna troppo lungo: "+name);
//				}
				columnInfo[i] = new ColumnInfo(name, getType(column.getType()), true);
				byte b = column.getPrecision();
				columnInfo[i].setDecimalPositions(b);
				i++;
			}
		}
		return columnInfo;
	}
	
	/**
	 * Mappa i tipi presenti nel file access nei tipi di colonna dell'etl
	 * @param dataType il tipo del file access
	 * @return il tipo dell'etl
	 */
	protected ColumnType getType(DataType dataType){
		switch(dataType){
			case BOOLEAN:
				return ColumnType.BOOLEAN;
			case DOUBLE:
			case FLOAT:
			case NUMERIC:
			case LONG:
				return ColumnType.FLOAT;
			case INT:
				return ColumnType.NUMBER;
			case TEXT:
			case MEMO:
				return ColumnType.STRING;
			case MONEY:
				return ColumnType.CURRENCY;
			default:
				return ColumnType.UNKNOWN;
		}
	}
	
	/**
	 * Cerca di inferire il tipo delle colonne basandosi sul contenuto del foglio Excel
	 * @param columnInfo Insieme di informazioni sulle colonne
	 * @param columns insieme di colonne
	 * @param records insieme di righe campione
	 * @param recordsForScanning insieme di righe campione da usare nel caso non si individui subito il tipo
	 */
	protected void inferColumnType(ColumnInfo[] columnInfo, List<Column> columns, List<Map<String, Object>> records, List<Map<String, Object>> recordsForScanning) {
		boolean latitudeFound = false;
		boolean longitudeFound = false;
		int latitudeIndex = -1;
		int longitudeIndex = -1;
		for(int i = 0; i < columnInfo.length; i++){
			String fieldName = columnInfo[i].getName();
			if(fieldName.equalsIgnoreCase("anno") || fieldName.equalsIgnoreCase("year")){
				columnInfo[i].setInferedDimension(verifyDimensionByLabel(records, fieldName, ColumnDimension.YEAR) ? ColumnDimension.YEAR : ColumnDimension.UNKNOWN);
			} else if(fieldName.equalsIgnoreCase("cap") || fieldName.equalsIgnoreCase("postalCode")){
				columnInfo[i].setInferedDimension(verifyDimensionByLabel(records, fieldName, ColumnDimension.POSTAL_CODE) ? ColumnDimension.POSTAL_CODE : ColumnDimension.UNKNOWN);
			} else if(fieldName.equalsIgnoreCase("comune") || fieldName.equalsIgnoreCase("city")){
				columnInfo[i].setInferedDimension(verifyDimensionByLabel(records, fieldName, ColumnDimension.CITY) ? ColumnDimension.CITY : ColumnDimension.UNKNOWN);
			} else if(fieldName.equalsIgnoreCase("provincia") || fieldName.equalsIgnoreCase("province")){
				columnInfo[i].setInferedDimension(verifyDimensionByLabel(records, fieldName, ColumnDimension.PROVINCE) ? ColumnDimension.PROVINCE : ColumnDimension.UNKNOWN);
			} else if(fieldName.equalsIgnoreCase("regione") || fieldName.equalsIgnoreCase("region")){
				columnInfo[i].setInferedDimension(verifyDimensionByLabel(records, fieldName, ColumnDimension.REGION) ? ColumnDimension.REGION : ColumnDimension.UNKNOWN);
			} else if(!latitudeFound && (fieldName.toUpperCase().startsWith("LAT") || fieldName.toUpperCase().endsWith("_Y") || fieldName.equalsIgnoreCase("y"))){
				if(verifyDimensionByLabel(records, fieldName, ColumnDimension.LATITUDE)){
					columnInfo[i].setInferedDimension(ColumnDimension.LATITUDE);
					columnInfo[i].setType(ColumnType.FLOAT);
					latitudeIndex = i;
					latitudeFound = true;					
				}
			} else if(!longitudeFound && (fieldName.toUpperCase().startsWith("LON") || fieldName.toUpperCase().endsWith("_X") || fieldName.equalsIgnoreCase("x"))){ 
				if(verifyDimensionByLabel(records, fieldName, ColumnDimension.LONGITUDE)){
					columnInfo[i].setInferedDimension(ColumnDimension.LONGITUDE);
					columnInfo[i].setType(ColumnType.FLOAT);
					longitudeIndex = i;
					longitudeFound = true;
				}
			}else {
				//bisogna leggere un buon numero di record e cercare di capire che dimensione può essere
				inferByScanning(recordsForScanning, fieldName, columnInfo[i]);
			}
		}
		//se è stata trovata la latitudine ma non la longitudine
		if(latitudeFound && !longitudeFound){
			//elimino le informazioni sulla latitudine
			columnInfo[latitudeIndex].setInferedDimension(ColumnDimension.UNKNOWN);
			columnInfo[latitudeIndex].setType(ColumnType.UNKNOWN);
		}
		//se è stata trovata la longitudine ma non la latitudine
		if(longitudeFound && !latitudeFound){
			//elimino le informazioni sulla longitudine
			columnInfo[longitudeIndex].setInferedDimension(ColumnDimension.UNKNOWN);
			columnInfo[longitudeIndex].setType(ColumnType.UNKNOWN);
		}
	}
	
	/**
	 * Esamina un insieme di righe ed associa alla colonna indicata dall'indice il tipo più probabile
	 * @param records l'insieme di righe campione
	 * @param index indice della colonna
	 * @param columnInfo informazioni attuali sulla colonna
	 */
	protected void inferByScanning(List<Map<String, Object>> records, String columnName, ColumnInfo columnInfo){
		if(records.size() == 0){
			columnInfo.setInferedDimension(ColumnDimension.UNKNOWN);
		}else{
			//10% è il numero massimo di record non verificati per poter suggerire questa dimensione
			int maxInvalidRecords = records.size() / 10;
			//array che conta i record non validi per ogni dimensione considerata
			int[] invalidRecords = new int[4];
			int cityIndex = 0;
			int provinceIndex = 1;
			int regionIndex = 2;
			int yearIndex = 3;
			for(int i = 0; i < records.size(); i++){
				String value = getCellValue(records.get(i), columnName);
				if(!verifyCity(value)){
					invalidRecords[cityIndex]++;
				}
				if(!verifyProvince(value)){
					invalidRecords[provinceIndex]++;
				}
				if(!verifyRegion(value)){
					invalidRecords[regionIndex]++;
				}
				if(!verifyYear(value)){
					invalidRecords[yearIndex]++;
				}	
			}
			if(invalidRecords[cityIndex] <= maxInvalidRecords){
				columnInfo.setInferedDimension(ColumnDimension.CITY);				
			}else if(invalidRecords[provinceIndex] <= maxInvalidRecords){
				columnInfo.setInferedDimension(ColumnDimension.PROVINCE);
			}else if(invalidRecords[regionIndex] <= maxInvalidRecords){
				columnInfo.setInferedDimension(ColumnDimension.REGION);
			}else if(invalidRecords[yearIndex] <= maxInvalidRecords){
				columnInfo.setInferedDimension(ColumnDimension.YEAR);
			}else{
				columnInfo.setInferedDimension(ColumnDimension.UNKNOWN);
			}
		}
	}
	
	
	/**
	 * Verifica che una dimensione candidata sia effettivamente la dimensione associata ad una colonna 
	 * @param records insieme di righe "campione"
	 * @param columnName nome della colonna
	 * @param candidate dimensione candidata
	 * @return false se viene individuato un valore non valido per la dimensione candidata, true altrimenti
	 */
	protected boolean verifyDimensionByLabel(List<Map<String, Object>> records, String columnName, ColumnDimension candidate) {	
		int maxInvalidRecords = records.size() / 10;
		int invalidRecords = 0;
		for(int i = 1; i < records.size(); i++){
			String value = getCellValue(records.get(i), columnName);
			if(candidate == ColumnDimension.YEAR){
				if(!verifyYear(value)){
					invalidRecords++;
				}
			} else if(candidate == ColumnDimension.POSTAL_CODE){
				if(!verifyPostalCode(value)){
					invalidRecords++;
				}
			} else if(candidate == ColumnDimension.CITY){
				if(!verifyCity(value)){
					invalidRecords++;
				}
			} else if(candidate == ColumnDimension.PROVINCE){
				if(!verifyProvince(value)){
					invalidRecords++;
				}				
			} else if(candidate == ColumnDimension.REGION){
				if(!verifyRegion(value)){
					invalidRecords++;
				}
			} else if(candidate == ColumnDimension.LATITUDE){
				if(!verifyLatitude(value)){
					invalidRecords++;
				}
			} else if(candidate == ColumnDimension.LONGITUDE){
				if(!verifyLongitude(value)){
					invalidRecords++;
				}
			}
			if(invalidRecords > maxInvalidRecords){
				return false;
			}
		}
		return true;
	}
	
	
	/**
	 * Restituisce la conversione in stringa del valore contenuto nella colonna indicata
	 * @param row l'insieme di celle
	 * @param columnName il nome della colonna
	 * @return il contenuto della cella
	 */
	protected String getCellValue(Map<String, Object> row, String columnName){
		Object object = row.get(columnName);
		if(object != null){
			return object.toString();
		}else{
			return "";
		}
	}

	/**
	 * Verifica i warning relativi alle dimensioni per una riga del file
	 * @param index indice di riga
	 * @param row la riga
	 * @param dimensions l'insieme delle dimensioni
	 * @param warnings l'elenco di warning
	 * @return true se è stato individuato un warning, false altrimenti
	 * @throws Exception
	 */
	protected boolean checkDimensionWarnings(int index, Map<String, Object> row, DimensionInfo[] dimensions, List<Warning> warnings) throws Exception {
		boolean warningFound = false;
		for (DimensionInfo dim : dimensions) {
			if (dim.getColumnName() == null) {
				throw new Exception("Nome di colonna non valido per la dimensione:" + dim.getAlias());
			}
			warningFound |= checkWarning(index, getCellValue(row, dim.getColumnName()), dim, warnings);
		}
		return warningFound;
	}
	
	/**
	 * Verifica i warning relativi alle colonne generiche per una riga del file 
	 * @param index indice della riga
	 * @param row la riga
	 * @param genericColumns l'insieme delle colonne generiche
	 * @param warnings l'elenco di warning
	 * @return true se è stato individuato un warning, false altrimenti
	 * @throws Exception
	 */
	protected boolean checkGenericColumnWarnings(int index, Map<String, Object> row, GenericColumnInfo[] genericColumns, List<Warning> warnings) throws Exception {
		boolean warningFound = false;
		for (GenericColumnInfo genericColumn : genericColumns) {
			if (genericColumn.getColumnName() == null) {
				throw new Exception("Nome di colonna non valido per la colonna generica:" + genericColumn.getAlias());
			}
			warningFound |= checkWarning(index, getCellValue(row, genericColumn.getColumnName()), genericColumn, warnings);
		}
		return warningFound;
	}
	
	/**
	 * Verifica i warning relativi alle misure per una riga del file 
	 * @param index indice della riga
     * @param row la riga
	 * @param measures l'insieme di misure
	 * @param warnings l'elenco di warning
	 * @return true se è stato individuato un warning, false altrimenti
	 * @throws Exception
	 */
	protected boolean checkMeasureWarnings(int index, Map<String, Object> row, MeasureInfo[] measures, List<Warning> warnings) throws Exception {
		boolean warningFound = false;
		for (MeasureInfo measure : measures) {
			if (measure.getColumnName() == null) {
				throw new Exception("Nome di colonna non valido per la misura:" + measure.getAlias());
			}
			warningFound |= checkWarning(index, getCellValue(row, measure.getColumnName()), measure, warnings);
		}
		return warningFound;
	}
	
	/**
	 * Crea una tabella nel db
	 * @param tableName il nome della tabella
	 * @param columnList la lista di colonne con cui creare e popolare la tabella
	 * @throws Exception
	 */
	protected boolean createTable(String tableName, List<IImportColumnInfo> columnList) throws Exception {
		ArrayList<String> columns = new ArrayList<String>();
		ArrayList<DBColumnType> types = new ArrayList<DBColumnType>();
		boolean geom = prepareDbColumns(columnList, columns, types);
		DBColumnType[] classArray = new DBColumnType[types.size()];
		classArray = types.toArray(classArray);
		String[] columnsArray = getColumnNames(columns);
		
		Table table = null;
		try{
			if(DBUtils.createTable(tableName, columnsArray, classArray, geom ? "gid" : "")){
				if(geom){
					DBUtils.addGeometryColumn(tableName, GeometryType.POINT);
					columns.add("the_geom");
					columnsArray = getColumnNames(columns);
				}
				List<String[]> batchList = new ArrayList<String[]>();
				table = getFirstTable();
				if(table != null){
					Iterator<Map<String, Object>> rowIterator = table.iterator();
					int i = 0;					
					while(rowIterator.hasNext()){
						Map<String, Object> row = rowIterator.next();
						String[] values = getValues(row, columnList, geom, i);
						if(values != null){
							batchList.add(values);
						}
						if(batchList.size() == 1000 || i == (table.getRowCount()-1)){
							//inserisco la riga
							if(!DBUtils.insertRow(tableName, columnsArray, batchList)){
								DBUtils.dropTable(tableName);
							}
							//svuoto la lista
							batchList.clear();
						}
						i++;
					}
				}else{
					//drop della tabella
					DBUtils.dropTable(tableName);
				}
				return geom;
			}else{
				//drop della tabella
				DBUtils.dropTable(tableName);
				return false;
			}
		}catch(Throwable ex){
			//drop della tabella
			DBUtils.dropTable(tableName);
			throw new Exception(ex.getMessage());
		}
		finally{
			if(table != null){
				table.getDatabase().close();
			}
		}
	}
	
	/**
	 * Crea un array di stringhe con i valori presenti nelle celle della riga
	 * @param row la riga da cui estrarli
	 * @param columnList elenco delle colonne
	 * @param geom indica se devo calcolare i valori aggiuntivi per la geometria
	 * @return Un array di valori
	 */
	protected String[] getValues(Map<String, Object> row, List<IImportColumnInfo> columnList, boolean geom, int gid){
		ArrayList<String> valueList = new ArrayList<String>();
		if(geom){
			valueList.add(""+gid);
		}
		String longitude = "";
		String latitude = "";
		for(int i = 0; i < columnList.size(); i++){
			IImportColumnInfo columnInfo = columnList.get(i);
			if(!addValue(getCellValue(row, columnInfo.getColumnName()), valueList, columnInfo)){
				return null;
			}
			if(columnInfo instanceof GenericColumnInfo){
				GenericColumnInfo genericColumn = (GenericColumnInfo)columnInfo;
				if(genericColumn.getIsLatitude()){
					latitude = valueList.get(valueList.size()-1).replace("'", "");
				}else if(genericColumn.getIsLongitude()){
					longitude = valueList.get(valueList.size()-1).replace("'", "");
				}
			}
		}		
		if(geom){
			valueList.add(getPointStringFromCoordinates(latitude, longitude));
		}
		return valueList.toArray(new String[valueList.size()]);
	}
	
}
