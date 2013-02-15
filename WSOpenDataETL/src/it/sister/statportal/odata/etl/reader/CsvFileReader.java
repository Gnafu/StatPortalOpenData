package it.sister.statportal.odata.etl.reader;

import it.sister.statportal.odata.domain.IRepository.GeometryType;
import it.sister.statportal.odata.etl.WSEtl;
import it.sister.statportal.odata.etl.WSEtl.VerifyUniquenessStatus;
import it.sister.statportal.odata.etl.exchange.ColumnInfo;
import it.sister.statportal.odata.etl.exchange.DimensionInfo;
import it.sister.statportal.odata.etl.exchange.GenericColumnInfo;
import it.sister.statportal.odata.etl.exchange.IImportColumnInfo;
import it.sister.statportal.odata.etl.exchange.MeasureInfo;
import it.sister.statportal.odata.etl.exchange.Warning;
import it.sister.statportal.odata.etl.exchange.ColumnInfo.ColumnDimension;
import it.sister.statportal.odata.etl.exchange.ColumnInfo.ColumnType;
import it.sister.statportal.odata.etl.exchange.TableInfo;
import it.sister.statportal.odata.etl.exchange.WarningSet;
import it.sister.statportal.odata.utility.DBUtils;
import it.sister.statportal.odata.utility.DBUtils.DBColumnType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Lettore di un file CSV
 * Utilizza la libreria OpenCSV
 *
 */
public class CsvFileReader extends FileReader {

	/**
	 * Crea il reader sul file
	 * @param file il file da leggere
	 */
	public CsvFileReader (File file){
		super(file);
	}
	
	@Override
	public WarningSet getWarnings(DimensionInfo[] dimensions, MeasureInfo[] measures, GenericColumnInfo[] genericColumns, String extraArguments) throws Exception {
		CSVReader reader = null;
		try{
			reader = getCSVReader(extraArguments);
			String [] firstLine = reader.readNext();
			if(firstLine == null){
				throw new Exception("Il file è vuoto");
			}
			String[] nextLine = null;
			int index = 1;
			List<Warning> warnings = new ArrayList<Warning>();
			//se il file non è vuoto
			int rowsWithWarning = 0;
			while((nextLine = reader.readNext()) != null){
				boolean rowWarning = false;
				//verifico le dimensioni
				rowWarning |= checkDimensionWarnings(index+1, nextLine, firstLine, dimensions, warnings);	
				//verifico le misure
				rowWarning |= checkMeasureWarnings(index+1, nextLine, firstLine, measures, warnings);
				//verifico le colonne generiche
				rowWarning |= checkGenericColumnWarnings(index+1, nextLine, firstLine, genericColumns, warnings);
				index++;
				if(rowWarning){
					rowsWithWarning++;
				}
			}
			return new WarningSet(rowsWithWarning < count(extraArguments), warnings.toArray(new Warning[warnings.size()]));
		}finally{
			if(reader != null){
				reader.close();
			}
		}
	}	
	
	/**
	 * Verifica i warning per le colonne generiche
	 * @param i indice di riga
	 * @param nextLine riga
	 * @param firstLine riga d'intestazione
	 * @param genericColumns insieme di colonne generiche
	 * @param warnings elenco di warning
	 * @return true se sono stati trovati dei warning, false altrimenti
	 * @throws Exception
	 */
	private boolean checkGenericColumnWarnings(int i, String[] nextLine, String[] firstLine, GenericColumnInfo[] genericColumns, List<Warning> warnings) throws Exception {
		boolean warningFound = false;
		for (GenericColumnInfo genericColumn : genericColumns) {
			if (genericColumn.getColumnName() == null) {
				throw new Exception("Nome di colonna non valido per la colonna generica:" + genericColumn.getAlias());
			}
			int index = getColumnIndex(genericColumn.getColumnName(), firstLine);
			warningFound |= checkWarning(i, nextLine[index], genericColumn, warnings);
		}
		return warningFound;
	}
	
	/**
	 * Verifica i warning per le misure
	 * @param i indice di riga
	 * @param nextLine riga
	 * @param firstLine riga d'intestazione
	 * @param measures insieme di misure
	 * @param warnings elenco di warning
	 * @return true se sono stati trovati dei warning, false altrimenti
	 * @throws Exception
	 */
	private boolean checkMeasureWarnings(int i, String[] nextLine, String[] firstLine,
			MeasureInfo[] measures, List<Warning> warnings) throws Exception {
		boolean warningFound = false;
		for (MeasureInfo measure : measures) {
			if (measure.getColumnName() == null) {
				throw new Exception("Nome di colonna non valido per la misura:" + measure.getAlias());
			}
			int index = getColumnIndex(measure.getColumnName(), firstLine);
			warningFound |= checkWarning(i, nextLine[index], measure, warnings);
		}
		return warningFound;
	}

	/**
	 * Verifica i warning per le dimensioni
	 * @param i indice di riga
	 * @param nextLine riga
	 * @param firstLine riga d'intestazione
	 * @param dimensions insieme di dimensioni
	 * @param warnings elenco di warning
	 * @return true se sono stati trovati dei warning, false altrimenti
	 * @throws Exception
	 */
	private boolean checkDimensionWarnings(int i, String[] nextLine, String[] firstLine, DimensionInfo[] dimensions, List<Warning> warnings) throws Exception {
		boolean warningFound = false;
		for (DimensionInfo dim : dimensions) {
			if (dim.getColumnName() == null) {
				throw new Exception("Nome di colonna non valido per la dimensione:" + dim.getAlias());
			}
			int index = getColumnIndex(dim.getColumnName(), firstLine);
			warningFound |= checkWarning(i, nextLine[index], dim, warnings);
		}
		return warningFound;
	}

	@Override
	public ColumnInfo[] readColumnInfo(boolean isShape, String extraArguments) throws Exception {
		ColumnInfo[] columnInfo = null;
		CSVReader reader = null;
		try {
			reader = getCSVReader(extraArguments);
			String [] nextLine = reader.readNext();
			//se il primo readNext non ha dato risultati, allora siamo in presenza di un file vuoto
			//altrimenti posso iniziare ad estrarre almeno i nomi delle colonne
			if(nextLine == null || nextLine.length == 0){
				columnInfo = new ColumnInfo[0];
			} else {
				int recordToRead = Math.min(WSEtl.getMaxVerifyRecords(), count(extraArguments));
				int scanningRecordToRead = Math.min(WSEtl.getMaxScanningRecords(), count(extraArguments));
				List<String[]> records = new ArrayList<String[]>(recordToRead);
				List<String[]> recordsForScanning = new ArrayList<String[]>(scanningRecordToRead);
				records.add(nextLine);
				for(int i = 1; i < recordToRead; i++){
					String[] line = reader.readNext();
					records.add(line);
					recordsForScanning.add(line);
				}
				for(int i = recordToRead; i < scanningRecordToRead; i++){
					recordsForScanning.add(reader.readNext());
				}
				columnInfo = initializeColumnInfo(nextLine);
				//ora devo cercare di inferire i tipi delle varie colonne
				inferColumnTypes(columnInfo, records, recordsForScanning);
			}						
		} finally {
			if(reader != null){
				reader.close();
			}
		}
		return columnInfo;
	}
	
	/**
	 * Cerca di inferire il tipo di una colonna leggendo un campione
	 * delle righe del file.
	 * Nel caso di colonne numeriche cerca anche di capire quale sia 
	 * il numero di cifre decimali utilizzate 
	 * @param columnInfo l'array di informazioni sulle colonne da aggiornare
	 * @param records l'insieme di righe campione
	 * @param recordsForScanning l'insieme di righe (generalmente un insieme maggiore di records) da usare
	 * per inferire il tipo delle colonne leggendone i valori
	 */
	private void inferColumnTypes(ColumnInfo[] columnInfo, List<String[]> records, List<String[]> recordsForScanning) {
		boolean latitudeFound = false;
		boolean longitudeFound = false;
		int latitudeIndex = -1;
		int longitudeIndex = -1;
		for(int i = 0; i < columnInfo.length; i++){
			String fieldName = columnInfo[i].getName();
			int index = getColumnIndex(fieldName, records.get(0));
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
			}else {
				//bisogna leggere un buon numero di record e cercare di capire che dimensione può essere
				inferByScanning(recordsForScanning, index, columnInfo[i]);
			}
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
	private void checkIfNumeric(List<String[]> records, int index, ColumnInfo columnInfo) {
		if(records.size() <= 0){
			return;
		}
		boolean isInteger = true;
		for(int i = 0; i < records.size(); i++){
			String value =  records.get(i)[index].replace(",", ".");
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
		}
	}

	/**
	 * Esamina un insieme di righe ed associa alla colonna indicata dall'indice il tipo più probabile
	 * @param records l'insieme di righe campione
	 * @param index indice della colonna
	 * @param columnInfo informazioni attuali sulla colonna
	 */
	private void inferByScanning(List<String[]> records, int index, ColumnInfo columnInfo){
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
				String value = records.get(i)[index];
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
	 * @param i indice di "colonna"
	 * @param candidate dimensione candidata
	 * @return false se viene individuato un valore non valido per la dimensione candidata, true altrimenti
	 */
	private boolean verifyDimensionByLabel(List<String[]> records, int index, ColumnDimension candidate) {
		int maxInvalidRecords = records.size() / 10;
		int invalidRecords = 0;
		for(int i = 1; i < records.size(); i++){
			String[] record = records.get(i);
			String value = record[index];		
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
	 * Inizializza un array di columnInfo.
	 * In particolare imposta la dimensione dell'array a quella del parametro.
	 * Inizializza ogni elemento dell'array impostandone il nome al valore 
	 * dell'i-esima posizione del parametro
	 * @param firstLine La prima riga letta in un file csv
	 * @return Un array di informazioni sulle colonne con i nomi già impostati
	 * @throws Exception 
	 */
	private ColumnInfo[] initializeColumnInfo(String[] firstLine) throws Exception{
		//HashSet<String> columnNames = new HashSet<String>();
		ColumnInfo[] columnInfo = new ColumnInfo[firstLine.length];
		for(int i = 0; i < columnInfo.length; i++){
			String name = firstLine[i];
//			if(name.length() > 63){
//				throw new Exception("Nome di colonna troppo lungo: " + name);
//			}
			columnInfo[i] = new ColumnInfo(name, ColumnType.UNKNOWN, true);
		}
		return columnInfo;
	}

	@Override
	public VerifyUniquenessStatus verifyUniqueness(String columnName, String extraArguments){
		VerifyUniquenessStatus status = VerifyUniquenessStatus.OK;
		CSVReader reader = null;
		try{
			reader = getCSVReader(extraArguments);
			String [] nextLine = reader.readNext();
			//se il primo readNext non ha dato risultati, allora siamo in presenza di un file vuoto
			//altrimenti posso scorrere il file e verificare l'univocità dei valori della colonna indicata
			if(nextLine == null || nextLine.length == 0){
				status = VerifyUniquenessStatus.FILE_NOT_STRUCTURED;
			} else {
				int index = getColumnIndex(columnName, nextLine);
				if(index == -1){
					status = VerifyUniquenessStatus.COLUMN_NOT_EXISTS;
				}else{
					HashSet<String> values = new HashSet<String>();
					while((nextLine = reader.readNext()) != null){
						if(values.contains(nextLine[index])){
							status = VerifyUniquenessStatus.NOT_UNIQUE;
							break;
						}
						values.add(nextLine[index]);
					}
				}
			}
		}catch(FileNotFoundException fnfe){
			status = VerifyUniquenessStatus.FILE_NOT_EXISTS;
		}catch(IOException ioe){
			status = VerifyUniquenessStatus.ERROR;
			status.setErrorMessage(ioe.getMessage());
		}finally{
			if(reader != null){
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return status;
	}

	/**
	 * Costruisce il reader per il file csv
	 * @param extraArguments un eventuale insieme di parametri extra
	 * @return un reader csv
	 * @throws IOException 
	 */
	private CSVReader getCSVReader(String extraArguments) throws IOException {
		
		String encoding = detectEncoding();

		CSVReader reader;
		
		if(extraArguments != null && extraArguments.length() > 0){
			if(extraArguments.charAt(0) == 't'){
				reader = new CSVReader(new InputStreamReader(new FileInputStream(file.getAbsoluteFile()), encoding) ,'\t');
			}else{
				reader = new CSVReader(new InputStreamReader(new FileInputStream(file.getAbsoluteFile()), encoding) ,extraArguments.charAt(0));
			}
		}else{
			reader = new CSVReader(new InputStreamReader(new FileInputStream(file.getAbsoluteFile()), encoding));	
		}
		return reader;
	}


	
	/**
	 * Ottiene l'indice di colonna per una determinata colonna del file
	 * @param columnName il nome della colonna
	 * @param line la riga d'intestazione
	 * @return l'indice di colonna per una determinata colonna del file
	 */
	private int getColumnIndex(String columnName, String[] line) {
		int index = -1;
		for(int i = 0; i < line.length; i++){
			if(line[i].equals(columnName)){
				index = i;
				break;
			}
		}
		return index;
	}

	@Override
	public TableInfo writeTable(List<IImportColumnInfo> columnList, String extraArguments) throws Exception {
		TableInfo tableInfo = null;
		List<String> originalNames = getOriginalNames(columnList);
		String tableName = getTableName(file.getName());
		String dbName = DBUtils.getDatabaseName();
		ArrayList<String> columns = new ArrayList<String>();
		ArrayList<DBColumnType> types = new ArrayList<DBColumnType>();
		boolean geom = prepareDbColumns(columnList, columns, types);
		String[] columnsArray = getColumnNames(columns);
		DBColumnType[] classArray = new DBColumnType[types.size()];
		classArray = types.toArray(classArray);
		//creazione della tabella
		CSVReader reader = null;
		try{
			reader = getCSVReader(extraArguments);
			if(DBUtils.createTable(tableName, columnsArray, classArray, geom ? "gid" : "")){
				if(geom){
					DBUtils.addGeometryColumn(tableName, GeometryType.POINT);
					columns.add("the_geom");
					columnsArray = getColumnNames(columns);
				}
				List<String[]> batchList = new ArrayList<String[]>();
				List<String[]> fileContent = reader.readAll();
				//Deve avere almeno due righe altrimenti c'è solo l'intestazione ma non il contenuto
				if(fileContent != null && fileContent.size() > 1){
					//la prima riga ha l'intestazione, la uso per ottenere l'insieme degli indici delle colonne da importare
					int[] indexes = getIndexes(fileContent.get(0), columnList);
					for(int i = 1; i < fileContent.size(); i++){
						String[] values = getValues(fileContent.get(i), indexes, columnList, geom, i);
						if(values != null){
							batchList.add(values);
						}
						if(batchList.size() == 1000 || i == (fileContent.size()-1)){
							//inserisco la riga							
							if(!DBUtils.insertRow(tableName, columnsArray, batchList)){
								DBUtils.dropTable(tableName);
							}
							//svuoto la lista
							batchList.clear();
						}
					}
				}
			}else{
				//drop della tabella
				DBUtils.dropTable(tableName);
			}
		}catch(Throwable ex){
			//drop della tabella
			DBUtils.dropTable(tableName);
			throw new Exception(ex.getMessage());
		}
		finally{
			if(reader != null){
				reader.close();
			}
		}
		tableInfo = new TableInfo(tableName, dbName, count(extraArguments), null, originalNames, geom);		
		return tableInfo;
	}

	/**
	 * Data una riga e un insieme di indici fornisce l'elenco dei valori della riga
	 * @param line la riga
	 * @param indexes gli indici a cui si è interessati
	 * @param columnList l'insieme delle colonne
	 * @param geom flag che indica se aggiungere la geometria
	 * @param gid gid da inserire in caso geom sia true
	 * @return  l'elenco dei valori della riga
	 */
	private String[] getValues(String[] line, int[] indexes, List<IImportColumnInfo> columnList, boolean geom, int gid){
		ArrayList<String> valueList = new ArrayList<String>();
		if(geom){
			valueList.add(""+gid);
		}
		String longitude = "";
		String latitude = "";
		for(int i = 0; i < columnList.size(); i++){
			if(!addValue(line[indexes[i]], valueList, columnList.get(i))){
				return null;
			}
			if(columnList.get(i) instanceof GenericColumnInfo){
				GenericColumnInfo genericColumn = (GenericColumnInfo)columnList.get(i);
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

	
	/**
	 * Crea un array che contiene gli indici delle colonne da importare.
	 * @param strings L'insieme dei valori presenti nella riga di instazione (i nomi delle colonne)
	 * @param columnList la lista di colonne da importare
	 * @return Un array che contiene gli indici delle colonne da importare.
	 * @throws Exception 
	 */
	private int[] getIndexes(String[] strings, List<IImportColumnInfo> columnList) throws Exception {
		int[] indexes = new int[columnList.size()];
		for(int i = 0; i < columnList.size(); i++){
			indexes[i] = findColumn(strings, columnList.get(i).getColumnName());
		}
		return indexes;
	}

	/**
	 * Cerca tra le instestazioni di un file csv quella corrispondente al nome di colonna indicato
	 * e ne restituisce l'indice.
	 * @param strings le intestazioni di un file csv
	 * @param columnName il nome della colonna
	 * @return l'indice del nome delle colonna nell'array delle intestazioni
	 * @throws Exception 
	 */
	private int findColumn(String[] strings, String columnName) throws Exception {
		for(int i = 0; i < strings.length; i++){
			if(strings[i].equals(columnName)){
				return i;
			}
		}
		throw new Exception("Colonna non trovata: "+columnName);
	}

	@Override
	public int getCardinality(String columnName, String extraArguments) throws Exception {
		CSVReader reader = null;
		try{
			reader = getCSVReader(extraArguments);
			String [] nextLine = reader.readNext();
			//se il primo readNext non ha dato risultati, allora siamo in presenza di un file vuoto
			//altrimenti posso scorrere il file e contare il numero di valori distinti
			if(nextLine == null || nextLine.length == 0){
				return 0;
			} else {
				int index = getColumnIndex(columnName, nextLine);
				if(index == -1){
					return 0;
				}else{
					HashSet<String> values = new HashSet<String>();
					while((nextLine = reader.readNext()) != null){
						String value = nextLine[index];
						if(!values.contains(value)){
							values.add(value);
						}
					}
					return values.size();
				}
			}
		} finally{
			if(reader != null){
				reader.close();
			}
		}
	}
	
	@Override
	public int count(String extraArguments) throws Exception {
		CSVReader reader = null;
		try{
			reader = getCSVReader(extraArguments);
			List<String[]> lines = reader.readAll();
			if(lines != null){
				//la prima è l'intestazione
				return lines.size()-1;
			}else{
				return 0;
			}
		}finally{
			if(reader != null){
				reader.close();
			}
		}
	}
}
