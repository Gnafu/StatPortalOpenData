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
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Lettore di un generico file Excel
 *
 */
public abstract class ExcelFileReader extends FileReader{

	public ExcelFileReader(File file){
		super(file);
	}
	
	/**
	 * Data la riga di intestazione ed il nome di una colonna restituisce l'indice della cella corrispondente.
	 * @param row la riga di intestazione
	 * @param columnName il nome della colonna da trovare
	 * @return l'indice della colonna o -1
	 */
	protected int getColumnIndex(Row row, String columnName){
		//Ci serve perchè nei file Excel non c'è il concetto di colonna
		//Generalmente la prima riga contiene nelle proprie celle dei valori
		//che sono i nomi delle colonne.
		Iterator<Cell> cellIterator = row.cellIterator();
		while(cellIterator.hasNext()){
			Cell cell = cellIterator.next();
			if(getStringValue(cell).equals(columnName)){
				return cell.getColumnIndex();
			}
		}
		return -1;
	}
	
	/**
	 * Ottiene il valore di una cella come stringa
	 * @param cell la cella
	 * @return il valore contenuto nella cella
	 */
	protected String getStringValue(Cell cell){
		if(cell.getCellType() == 0){
			try{
				double d = cell.getNumericCellValue();
				int i = new Double(d).intValue();
				if(i == d){
					return ""+i;
				}else{
					return ""+d;
				}
			}catch(NumberFormatException nfe){
				return cell.toString();
			}
		}else{
			return cell.toString();
		}
	}
	
	/**
	 * Scorre il file e controlla se una colonna contiene dei duplicati
	 * @param iterator L'iteratore sulle righe
	 * @param columnIndex l'indice della colonna
	 * @return true se ha dei duplicati, false altrimenti
	 */
	protected boolean hasDuplicates(Iterator<Row> iterator, int columnIndex){
		HashSet<String> values = new HashSet<String>();
		while(iterator.hasNext()){
			String value = getCellValue(iterator.next(), columnIndex);
			if(values.contains(value)){
				return true;
			}else{
				values.add(value);
			}
		}
		return false;
	}
	
	/**
	 * Data una riga ed un indice di colonna restituisce il valore di quella colonna
	 * @param row la riga
	 * @param columnIndex l'indice di colonna
	 * @return il valore della colonna nella riga
	 */
	protected String getCellValue(Row row, int columnIndex){
		Cell cell = row.getCell(columnIndex);
		if(cell == null){
			return "";
		}else{
			return getStringValue(cell);
		}
	}
	
	/**
	 * Scorre le righe di un workbook e calcola in numero di valori distinti presenti in una colonna
	 * @param workbook
	 * @param columnName
	 * @return
	 */
	protected int calculateCardinality(Workbook workbook, String columnName){
		Sheet sheet = workbook.getSheetAt(0);
		if(sheet != null){
			Iterator<Row> rowIterator = sheet.rowIterator();
			//se il file non è vuoto
			if(rowIterator.hasNext()){
				//estraggo dalla prima riga l'indice di colonna
				Row firstRow = rowIterator.next();
				int columnIndex = getColumnIndex(firstRow, columnName);
				HashSet<String> values = new HashSet<String>();
				while(rowIterator.hasNext()){
					String value = getCellValue(rowIterator.next(), columnIndex);
					if(!values.contains(value)){
						values.add(value);
					}
				}
				return values.size();
			}else{
				return 0;
			}
		}else{
			return 0;
		}
	}
	
	@Override
	public WarningSet getWarnings(DimensionInfo[] dimensions, MeasureInfo[] measures, GenericColumnInfo[] genericColumns, String extraArguments) throws Exception {
		FileInputStream fileInputStream = null;
		try{
			fileInputStream = new FileInputStream(file.getAbsolutePath());
			Workbook workbook = getWorkbook(fileInputStream);
			Sheet sheet = workbook.getSheetAt(0);
			if(sheet == null){
				throw new Exception("Non è presente un foglio di lavoro");
			}
			Iterator<Row> rowIterator = sheet.rowIterator();
			Row firstRow = rowIterator.next();
			//se il file non è vuoto
			List<Warning> warnings = new ArrayList<Warning>();
			int rowsWithWarning = 0;
			while(rowIterator.hasNext()){
				boolean rowWarning = false;
				Row row = rowIterator.next();
				//verifico le dimensioni
				rowWarning |= checkDimensionWarnings(row, firstRow, dimensions, warnings);	
				//verifico le misure
				rowWarning |= checkMeasureWarnings(row, firstRow, measures, warnings);
				//verifico le colonne generiche
				rowWarning |= checkGenericColumnWarnings(row, firstRow, genericColumns, warnings);
				if(rowWarning){
					rowsWithWarning++;
				}
			}
			return new WarningSet(rowsWithWarning < count(extraArguments), warnings.toArray(new Warning[warnings.size()]));
		}finally{
			if(fileInputStream != null){
				fileInputStream.close();
			}
		}
	}
	
	/**
	 * Verifica i warning relativi alle colonne generiche per una riga del file 
	 * @param row la riga
	 * @param firstRow la riga di intestazione
	 * @param genericColumns l'insieme delle colonne generiche
	 * @param warnings l'elenco di warning
	 * @return true se è stato individuato un warning, false altrimenti
	 * @throws Exception
	 */
	private boolean checkGenericColumnWarnings(Row row, Row firstRow, GenericColumnInfo[] genericColumns, List<Warning> warnings) throws Exception {
		boolean warningFound = false;
		for (GenericColumnInfo genericColumn : genericColumns) {
			if (genericColumn.getColumnName() == null) {
				throw new Exception("Nome di colonna non valido per la colonna generica:" + genericColumn.getAlias());
			}
			int columnIndex = getColumnIndex(firstRow, genericColumn.getColumnName());
			warningFound |= checkWarning(row.getRowNum()+1, getCellValue(row, columnIndex), genericColumn, warnings);
		}
		return warningFound;
	}
	
	/**
	 * Verifica i warning relativi alle misure per una riga del file 
	 * @param row la riga
	 * @param firstRow la riga di intestazione
	 * @param measures l'insieme di misure
	 * @param warnings l'elenco di warning
	 * @return true se è stato individuato un warning, false altrimenti
	 * @throws Exception
	 */
	private boolean checkMeasureWarnings(Row row, Row firstRow, MeasureInfo[] measures, List<Warning> warnings) throws Exception {
		boolean warningFound = false;
		for (MeasureInfo measure : measures) {
			if (measure.getColumnName() == null) {
				throw new Exception("Nome di colonna non valido per la misura:" + measure.getAlias());
			}
			int columnIndex = getColumnIndex(firstRow, measure.getColumnName());
			warningFound |= checkWarning(row.getRowNum()+1, getCellValue(row, columnIndex), measure, warnings);
		}
		return warningFound;
	}

	/**
	 * Verifica i warning relativi alle dimensioni per una riga del file
	 * @param row la riga
	 * @param firstRow la riga d'intestazione
	 * @param dimensions l'insieme delle dimensioni
	 * @param warnings l'elenco di warning
	 * @return true se è stato individuato un warning, false altrimenti
	 * @throws Exception
	 */
	private boolean checkDimensionWarnings(Row row, Row firstRow, DimensionInfo[] dimensions, List<Warning> warnings) throws Exception {
		boolean warningFound = false;
		for (DimensionInfo dim : dimensions) {
			if (dim.getColumnName() == null) {
				throw new Exception("Nome di colonna non valido per la dimensione:" + dim.getAlias());
			}
			int columnIndex = getColumnIndex(firstRow, dim.getColumnName());
			warningFound |= checkWarning(row.getRowNum()+1, getCellValue(row, columnIndex), dim, warnings);
		}
		return warningFound;
	}

	@Override
	public int getCardinality(String columnName, String extraArguments) throws Exception {
		FileInputStream fileInputStream = null;
		try{
			fileInputStream = new FileInputStream(file.getAbsolutePath());
			Workbook workbook = getWorkbook(fileInputStream);
			Sheet sheet = workbook.getSheetAt(0);
			if(sheet != null){
				Iterator<Row> rowIterator = sheet.rowIterator();
				//se il file non è vuoto
				if(rowIterator.hasNext()){
					//estraggo dalla prima riga l'indice di colonna
					Row firstRow = rowIterator.next();
					int columnIndex = getColumnIndex(firstRow, columnName);
					HashSet<String> values = new HashSet<String>();
					while(rowIterator.hasNext()){
						String value = getCellValue(rowIterator.next(), columnIndex);
						if(!values.contains(value)){
							values.add(value);
						}
					}
					return values.size();
				}else{
					return 0;
				}
			}else{
				return 0;
			}
		}catch(Exception ex){
			return 0;
		} finally{
			if(fileInputStream != null){
				fileInputStream.close();
			}
		}
	}
	
	@Override
	public int count(String extraArguments) throws Exception {
		FileInputStream fileInputStream = null;
		try{
			fileInputStream = new FileInputStream(file.getAbsolutePath());
			Workbook workbook = getWorkbook(fileInputStream);
			Sheet sheet = workbook.getSheetAt(0);
			if(sheet == null){
				return 0;
			}
			return sheet.getPhysicalNumberOfRows()-1;
		}catch(Exception ex){
			return 0;
		} finally{
			if(fileInputStream != null){
				fileInputStream.close();
			}
		}
	}
	
	@Override
	public TableInfo writeTable(List<IImportColumnInfo> columnList, String extraArguments) throws Exception {
		TableInfo tableInfo = null;
		List<String> originalNames = getOriginalNames(columnList);
		String tableName = getTableName(file.getName());
		String dbName = DBUtils.getDatabaseName();
		boolean geom = createTable(tableName, columnList);
		tableInfo = new TableInfo(tableName, dbName, count(""), null, originalNames, geom);		
		return tableInfo;
	}
	
	/**
	 * Crea una tabella
	 * @param tableName il nome della tabella
	 * @param columnList l'insieme di colonna
	 * @return true se la tabella è stata creata, false altrimenti
	 * @throws Exception
	 */
	protected boolean createTable(String tableName, List<IImportColumnInfo> columnList)
			throws Exception {
		ArrayList<String> columns = new ArrayList<String>();
		ArrayList<DBColumnType> types = new ArrayList<DBColumnType>();
		boolean geom = prepareDbColumns(columnList, columns, types);
		DBColumnType[] classArray = new DBColumnType[types.size()];
		classArray = types.toArray(classArray);
		String[] columnsArray = getColumnNames(columns);
		//creazione della tabella
		FileInputStream fileInputStream = null;
		try{
			if(DBUtils.createTable(tableName, columnsArray, classArray, geom ? "gid" : "")){
				if(geom){
					DBUtils.addGeometryColumn(tableName, GeometryType.POINT);
					columns.add("the_geom");
					columnsArray = getColumnNames(columns);
				}
				fileInputStream = new FileInputStream(file.getAbsolutePath());
				Workbook workbook = getWorkbook(fileInputStream);
				List<String[]> batchList = new ArrayList<String[]>();
				Sheet sheet = workbook.getSheetAt(0);
				if(sheet != null){
					Iterator<Row> rowIterator = sheet.iterator();
					if(!rowIterator.hasNext()){
						throw new Exception("Struttura non valida");
					}
					Row firstRow = rowIterator.next();
					addColumnIndexes(firstRow, columnList);
					int i = 0;					
					while(rowIterator.hasNext()){
						String[] values = getValues(rowIterator, columnList, geom, i);
						if(values != null){
							batchList.add(values);
						}
						if(batchList.size() == 1000 || i == (sheet.getPhysicalNumberOfRows()-2)){
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
			if(fileInputStream != null){
				fileInputStream.close();
			}
		}
	}
	
	/**
	 * Calcola gli indici delle varie colonne basandosi sulla riga di intestazione
	 * @param firstRow riga di instestazione 
	 * @param columnList elenco delle colonne
	 */
	private void addColumnIndexes(Row firstRow, List<IImportColumnInfo> columnList) {
		for(int i = 0; i < columnList.size(); i++){
			columnList.get(i).setColumnIndex(getColumnIndex(firstRow, columnList.get(i).getColumnName()));
		}
	}

	/**
	 * Crea una stringa di con i valori presenti nelle celle del file
	 * @param row la riga da cui estrarli
	 * @param columnList elenco delle colonne
	 * @return Un array di valori
	 * @throws Throwable 
	 */
	private String[] getValues(Iterator<Row> rowIterator, List<IImportColumnInfo> columnList, boolean geom, int gid){
		ArrayList<String> valueList = new ArrayList<String>();
		Row row = rowIterator.next();
		if(geom){
			valueList.add(""+gid);
		}
		String longitude = "";
		String latitude = "";
		for(int i = 0; i < columnList.size(); i++){
			IImportColumnInfo columnInfo = columnList.get(i);
			if(!addValue(getCellValue(row, columnInfo.getColumnIndex()), valueList, columnInfo)){
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
	
	@Override
	public VerifyUniquenessStatus verifyUniqueness(String columnName, String extraArguments)
			throws Exception {
		VerifyUniquenessStatus status = VerifyUniquenessStatus.OK;
		FileInputStream fileInputStream = null;
		try{
			fileInputStream = new FileInputStream(file.getAbsolutePath());
			Workbook workbook = getWorkbook(fileInputStream);
			Sheet sheet = workbook.getSheetAt(0);
			if(sheet != null){
				Iterator<Row> rowIterator = sheet.rowIterator();
				//se il file non è vuoto
				if(rowIterator.hasNext()){
					//estraggo dalla prima riga l'indice di colonna
					Row firstRow = rowIterator.next();
					int columnIndex = getColumnIndex(firstRow, columnName);
					if(hasDuplicates(rowIterator, columnIndex)){
						status = VerifyUniquenessStatus.NOT_UNIQUE;
					}
				}
			}
		} catch(Exception ex){
			status = VerifyUniquenessStatus.ERROR;
			status.setErrorMessage(ex.getMessage());
		} finally {
			if(fileInputStream != null){
				fileInputStream.close();
			}
		}
		return status;
	}
	
	@Override
	public ColumnInfo[] readColumnInfo(boolean isShape, String extraArguments) throws Exception {
		ColumnInfo[] columnInfo = null;
		FileInputStream fileInputStream = null;
		try{
			fileInputStream = new FileInputStream(file.getAbsolutePath());
			Workbook workbook = getWorkbook(fileInputStream);
			Sheet sheet = workbook.getSheetAt(0);
			if(sheet != null){
				Row row = sheet.getRow(0);
				int recordToRead = Math.min(WSEtl.getMaxVerifyRecords(), count(extraArguments));
				int scanningRecordToRead = Math.min(WSEtl.getMaxScanningRecords(), count(extraArguments));
				List<Row> records = new ArrayList<Row>(recordToRead);
				List<Row> recordsForScanning = new ArrayList<Row>(scanningRecordToRead);
				records.add(row);
				for(int i = 0; i < recordToRead; i++){
					Row r = sheet.getRow(i+1);
					records.add(r);
					recordsForScanning.add(r);
				}
				for(int i = recordToRead; i < scanningRecordToRead; i++){
					recordsForScanning.add(sheet.getRow(i+1));
				}
				columnInfo = initializeColumnInfo(row);
				inferColumnType(columnInfo, records, recordsForScanning);
			}
			return columnInfo; 
		} finally{
			if(fileInputStream != null){
				fileInputStream.close();
			}
		}
	}
	
	/**
	 * Cerca di inferire il tipo delle colonne basandosi sul contenuto del foglio Excel
	 * @param columnInfo Insieme di informazioni sulle colonne
	 * @param records insieme di righe campione
	 */
	private void inferColumnType(ColumnInfo[] columnInfo, List<Row> records, List<Row> recordsForScanning) {
		boolean latitudeFound = false;
		boolean longitudeFound = false;
		int latitudeIndex = -1;
		int longitudeIndex = -1;
		for(int i = 0; i < columnInfo.length; i++){
			String fieldName = columnInfo[i].getName();
			int index = getColumnIndex(records.get(0), fieldName);
			if(fieldName.equalsIgnoreCase("anno") || fieldName.equalsIgnoreCase("year")){
				columnInfo[i].setInferedDimension(verifyDimensionByLabel(records, index, ColumnDimension.YEAR) ? ColumnDimension.YEAR : ColumnDimension.UNKNOWN);
			} else if(fieldName.equalsIgnoreCase("cap") || fieldName.equalsIgnoreCase("postalCode")){
				columnInfo[i].setInferedDimension(verifyDimensionByLabel(records, index, ColumnDimension.POSTAL_CODE) ? ColumnDimension.POSTAL_CODE : ColumnDimension.UNKNOWN);
			} else if(fieldName.equalsIgnoreCase("comune") || fieldName.equalsIgnoreCase("city")){
				columnInfo[i].setInferedDimension(verifyDimensionByLabel(records, index, ColumnDimension.CITY) ? ColumnDimension.CITY : ColumnDimension.UNKNOWN);
			} else if(fieldName.equalsIgnoreCase("provincia") || fieldName.equalsIgnoreCase("province")){
				columnInfo[i].setInferedDimension(verifyDimensionByLabel(records, index, ColumnDimension.PROVINCE) ? ColumnDimension.PROVINCE : ColumnDimension.UNKNOWN);
			} else if(fieldName.equalsIgnoreCase("regione") || fieldName.equalsIgnoreCase("region")){
				columnInfo[i].setInferedDimension(verifyDimensionByLabel(records, index, ColumnDimension.REGION) ? ColumnDimension.REGION : ColumnDimension.UNKNOWN);
			} else if(!latitudeFound && (fieldName.toUpperCase().startsWith("LAT") || fieldName.toUpperCase().endsWith("_Y") || fieldName.equalsIgnoreCase("y"))){
				if(verifyDimensionByLabel(records, index, ColumnDimension.LATITUDE)){
					columnInfo[i].setInferedDimension(ColumnDimension.LATITUDE);
					columnInfo[i].setType(ColumnType.FLOAT);
					latitudeIndex = i;
					latitudeFound = true;					
				}
			} else if(!longitudeFound && (fieldName.toUpperCase().startsWith("LON") || fieldName.toUpperCase().endsWith("_X") || fieldName.equalsIgnoreCase("x"))){ 
				if(verifyDimensionByLabel(records, index, ColumnDimension.LONGITUDE)){
					columnInfo[i].setInferedDimension(ColumnDimension.LONGITUDE);
					columnInfo[i].setType(ColumnType.FLOAT);
					longitudeIndex = i;
					longitudeFound = true;
				}
			} else {
				//bisogna leggere un buon numero di record e cercare di capire che dimensione può essere
				inferByScanning(recordsForScanning, index, columnInfo[i]);
			} 
			//a questo punto se non ho associato una dimensione e se leggo solo numeri 
			//cambio il tipo della colonna in numerico in modo che venga vista come una misura
			if(columnInfo[i].getInferedDimension() == ColumnDimension.UNKNOWN){
				checkIfNumeric(recordsForScanning, index, columnInfo[i]);
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
	 * Controlla se una colonna può essere definita numerica
	 * @param records insieme di record da controllare
	 * @param index indice della colonna
	 * @param columnInfo informazioni attuali sulla colonna
	 */
	private void checkIfNumeric(List<Row> records, int index, ColumnInfo columnInfo) {
		if(records.size() <= 0){
			return;
		}
		int decimalPlaces = 0;
		boolean isInteger = true;
		for(int i = 0; i < records.size(); i++){
			String value = getCellValue(records.get(i), index).replace(",", ".");
			if(isInteger){
				try{
					Integer.parseInt(value);
				}catch(NumberFormatException nfe){
					//se non può essere un intero potrebbe essere un double
					isInteger = false;
				}
			}
			try{
				Double.parseDouble(value);
				BigDecimal bd = new BigDecimal(value);
				decimalPlaces = Math.max(decimalPlaces, bd.scale());
			}catch(NumberFormatException nfe){
				//se non può essere neanche un double allora non è numerico
				return;
			}
		}
		//se arrivo a questo punto o è un integer o un double
		if(isInteger){
			columnInfo.setType(ColumnType.NUMBER);
		}else{
			columnInfo.setType(ColumnType.FLOAT);
			columnInfo.setDecimalPositions(decimalPlaces);
		}
	}

	/**
	 * Esamina un insieme di righe ed associa alla colonna indicata dall'indice il tipo più probabile
	 * @param records l'insieme di righe campione
	 * @param index indice della colonna
	 * @param columnInfo informazioni attuali sulla colonna
	 */
	private void inferByScanning(List<Row> records, int index, ColumnInfo columnInfo){
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
				String value = getCellValue(records.get(i), index);
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
	 * @param index indice di "colonna"
	 * @param candidate dimensione candidata
	 * @return false se viene individuato un valore non valido per la dimensione candidata, true altrimenti
	 */
	private boolean verifyDimensionByLabel(List<Row> records, int index, ColumnDimension candidate) {
		int maxInvalidRecords = records.size() / 10;
		int invalidRecords = 0;
		for(int i = 1; i < records.size(); i++){
			Row record = records.get(i);
			String value = getCellValue(record, index);
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
	 * Inizializza l'insieme di informazioni sulle colonne.
	 * Per iniziare imposta il numero di elementi nella dimensione
	 * e per ogni elemento imposta il nome.
	 * @param row La prima riga del file
	 * @return Un insieme di informazioni sulle colonne
	 */
	private ColumnInfo[] initializeColumnInfo(Row row) throws Exception{
		ColumnInfo[] columnInfo = null;
		if(row == null){
			columnInfo = new ColumnInfo[0];
		} else {
			columnInfo = new ColumnInfo[row.getPhysicalNumberOfCells()];
			Iterator<Cell> cellIterator = row.cellIterator();
			int i = 0;
			while(cellIterator.hasNext()){
				Cell cell = cellIterator.next();
				String name = getStringValue(cell);
//				if(name.length() > 63){
//					throw new Exception("Nome di colonna troppo lungo: "+name);
//				}
				columnInfo[i] = new ColumnInfo(name, ColumnType.UNKNOWN, true);
				i++;
			}
		}
		return columnInfo;
	}
	
	/**
	 * Ottiene il workbook relativo al file
	 * @param fileInputStream lo stream sul file
	 * @return un workbook xls o xlsx
	 * @throws IOException
	 */
	protected abstract Workbook getWorkbook(FileInputStream fileInputStream) throws IOException;
}
