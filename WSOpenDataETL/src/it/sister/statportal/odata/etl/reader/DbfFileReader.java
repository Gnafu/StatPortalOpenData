package it.sister.statportal.odata.etl.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import it.sister.statportal.odata.domain.IRepository.GeometryType;
import it.sister.statportal.odata.etl.StreamCatcher;
import it.sister.statportal.odata.etl.WSEtl;
import it.sister.statportal.odata.etl.WSEtl.VerifyUniquenessStatus;
import it.sister.statportal.odata.etl.exchange.ColumnInfo;
import it.sister.statportal.odata.etl.exchange.DimensionInfo;
import it.sister.statportal.odata.etl.exchange.GenericColumnInfo;
import it.sister.statportal.odata.etl.exchange.IImportColumnInfo;
import it.sister.statportal.odata.etl.exchange.MeasureInfo;
import it.sister.statportal.odata.etl.exchange.Pair;
import it.sister.statportal.odata.etl.exchange.TableInfo;
import it.sister.statportal.odata.etl.exchange.WarningSet;
import it.sister.statportal.odata.etl.exchange.ColumnInfo.ColumnDimension;
import it.sister.statportal.odata.etl.exchange.ColumnInfo.ColumnType;
import it.sister.statportal.odata.etl.exchange.Warning;
import it.sister.statportal.odata.utility.DBUtils;
import it.sister.statportal.odata.utility.DBUtils.DBColumnType;

import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import org.xBaseJ.fields.Field;


/**
 * Lettore di un file Dbf
 *
 */
public class DbfFileReader extends FileReader {
		
	/**
	 * Crea il reader per il file dbf/shp
	 * @param file il file su cui si basa il reader
	 */
	public DbfFileReader(File file){
		super(file);
	}
	
	/**
	 * Restituisce l'astrazione del file DBF
	 * @return l'astrazione del file DBF
	 * @throws Exception 
	 */
	private DBF getDBF() throws Exception{
		String filePath = file.getAbsolutePath();
		if(file.getName().toUpperCase().endsWith(".SHP")){
			filePath = filePath.substring(0, filePath.length() -3) + "dbf";
		}
		DBF dbfFile = null;
		try {
			String otherValidCharacters = WSEtl.readConfig("otherValidCharactersInFieldNames");
			if(otherValidCharacters != null){
				org.xBaseJ.Util.setxBaseJProperty("otherValidCharactersInFieldNames", WSEtl.readConfig("otherValidCharactersInFieldNames"));
			}
			dbfFile = new DBF(filePath, DBF.READ_ONLY, detectEncoding());
		} catch (Exception e) {
			if(dbfFile != null){
				try {
					dbfFile.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
			throw e;
		} 
		return dbfFile;
	}

	@Override
	public WarningSet getWarnings(DimensionInfo[] dimensions,MeasureInfo[] measures, GenericColumnInfo[] genericColumns,  String extraArguments) throws Exception {
		DBF dbfFile = null;
		try{
			List<Warning> warnings = new ArrayList<Warning>();
			//il file su cui lavorare
			dbfFile = getDBF();	
			//scorro le righe
			int rowsWithWarning = 0;
			for(int i = 1; i < dbfFile.getRecordCount(); i++){
				boolean rowWarning = false;
				dbfFile.read();
				//verifico le dimensioni per la riga
				rowWarning |= checkDimensionWarnings(dbfFile, i+1, dimensions, warnings);	
				//verifico le misure per la riga
				rowWarning |= checkMeasureWarnings(dbfFile, i+1, measures, warnings);
				//verifico le colonne generiche per la riga
				rowWarning |= checkGenericColumnWarning(dbfFile, i+1, genericColumns, warnings);
				if(rowWarning){
					rowsWithWarning++;
				}
			}
			return new WarningSet(rowsWithWarning < count(extraArguments), warnings.toArray(new Warning[warnings.size()]));
		}finally{
			try {
				dbfFile.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	/**
	 * Verifica i warning di una riga di un file dbf legati alle colonne generiche
	 * @param dbfFile il file dbf
	 * @param i l'indice di riga
	 * @param genericColumns le colonne generiche
	 * @param warnings l'elenco dei warning correnti a cui aggiungere i nuovi
	 * @return true se sono stati trovati dei warning, false altrimenti
	 * @throws Exception 
	 */
	private boolean checkGenericColumnWarning(DBF dbfFile, int i, GenericColumnInfo[] genericColumns, List<Warning> warnings) throws Exception{
		boolean warningFound = false;
		for(GenericColumnInfo genericColumn : genericColumns){
			if(genericColumn.getColumnName() == null){
				throw new Exception("Nome di colonna non valido per la colonna generica:" + genericColumn.getAlias());
			}
			Field field = dbfFile.getField(genericColumn.getColumnName());
			warningFound |= checkWarning(i, field.get().trim().toUpperCase(), genericColumn, warnings);
		}
		return warningFound;
	}
	
	/**
	 * Verifica i warning di una riga di un file dbf legati alle dimensioni
	 * @param dbfFile il file dbf
	 * @param i l'indice di riga
	 * @param dimensions le dimensioni
	 * @param warnings l'elenco dei warning correnti a cui aggiungere i nuovi
	 * @return true se sono stati trovati dei warning, false altrimenti
	 * @throws Exception
	 * @throws xBaseJException
	 */
	private boolean checkDimensionWarnings(DBF dbfFile, int i, DimensionInfo[] dimensions, List<Warning> warnings) throws Exception, xBaseJException {
		boolean warningFound = false;
		for (DimensionInfo dim : dimensions) {
			if (dim.getColumnName() == null) {
				throw new Exception("Nome di colonna non valido per la dimensione:" + dim.getAlias());
			}
			Field field = dbfFile.getField(dim.getColumnName());
			//chiamo il controllo dei warning per il valore e la dimensione correnti
			warningFound |= checkWarning(i, field.get().trim().toUpperCase(), dim, warnings);
		}
		return warningFound;
	}	
	
	/**
	 * Verifica i warning relative alle misure per una riga del file 
	 * @param dbfFile il file dbf
	 * @param i l'indice di riga
	 * @param measures l'insieme di misure da controllare
	 * @param warnings l'elenco di warning in cui aggiungerne di nuovi
	 * @return true se sono stati trovati dei warning, false altrimenti
	 * @throws Exception
	 * @throws xBaseJException
	 */
	private boolean checkMeasureWarnings(DBF dbfFile, int i, MeasureInfo[] measures, List<Warning> warnings) throws Exception, xBaseJException {
		boolean warningFound = false;
		for (MeasureInfo measure : measures) {
			if (measure.getColumnName() == null) {
				throw new Exception("Nome di colonna non valido per la misura:" + measure.getAlias());
			}
			Field field = dbfFile.getField(measure.getColumnName());
			//chiamo il controllo dei warning per la misura corrente
			warningFound |= checkWarning(i, field.get(), measure, warnings);
		}
		return warningFound;
	}
	
	@Override
	public TableInfo writeTable(List<IImportColumnInfo> columnList, String extraArguments) throws Exception{
		TableInfo tableInfo = null;
		List<String> originalNames = getOriginalNames(columnList);
		String tableName = getTableName(file.getName());
		String dbName = DBUtils.getDatabaseName();
		Pair<String, String> addedGeometryColumns = null;
		boolean geom = false;
		if(file.getName().toUpperCase().endsWith(".SHP")){			
			//a seconda del tipo di architettura sottostante devo usare dei comandi diversi
			//i comandi vengono salvati in un file di configurazione (META-INF/shpimport.txt)
			createTableFromShp(tableName, dbName);
			if(DBUtils.isPunctualGeometry(tableName)){
				addedGeometryColumns = selectValidPairName(originalNames);
				DBUtils.createLatLngColumns(tableName, addedGeometryColumns.getFirst(), addedGeometryColumns.getSecond());
			}
			geom = true;
		} else {
			//è un dbf, devo creare la tabella a mano
			geom = createTableFromDbf(tableName, columnList);
		}
		tableInfo = new TableInfo(tableName, dbName, count(""), null, originalNames, geom);		
		if(addedGeometryColumns != null){
			tableInfo.setAddedGeometryColumns(addedGeometryColumns);
			tableInfo.getOriginalColumnNames().add(addedGeometryColumns.getFirst());
			tableInfo.getOriginalColumnNames().add(addedGeometryColumns.getSecond());
		}
		return tableInfo;
	}

	/**
	 * Crea una coppia di nomi di colonne valida.
	 * Se è possibile la coppia sarà (glatitude, glongitude).
	 * Se uno di questi nomi è già presente nelle colonne gli si concatenerà 
	 * un intero partendo da zero e incrementandolo fino a quando non si troverà un
	 * nome di colonna non esistente.
	 * @param columnList lista dei nomi delle colonne
	 * @return la coppia dei nomi per le colonne latitudine e longitudine
	 */
	private Pair<String, String> selectValidPairName(List<String> columnList) {
		String latitude = "glatitude";
		String longitude = "glongitude";
		int i = 0;
		while(columnList.contains(latitude)){
			latitude = "glatitude_"+i;
			i++;
		}
		i = 0;
		while(columnList.contains(longitude)){
			longitude = "glongitude_"+i;
			i++;
		}
		return new Pair<String, String>(latitude, longitude);
	}

	/**
	 * Crea la tabella a partire da un file shp.
	 * Utilizza degli script che legge da un file di configurazione
	 * @param tableName nome della tabella
	 * @param dbName nome del db
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws Exception
	 */
	private void createTableFromShp(String tableName, String dbName)
			throws FileNotFoundException, IOException, InterruptedException,
			Exception {
		BufferedReader reader = null;
		File temporaryFile = null;
		PrintWriter pw = null;
		try{
			reader = new BufferedReader(new java.io.FileReader("webapps/WSOpenDataETL/META-INF/shpimport.txt"));
			List<String> lines = new ArrayList<String>();
			String fileLine = null;
			while((fileLine = reader.readLine()) != null){
				lines.add(fileLine);
			}
			temporaryFile = new File(WSEtl.readConfig("baseShpImportDir")+tableName+".executable");
			pw = new PrintWriter(temporaryFile);
			Runtime run = Runtime.getRuntime();
			for(String line : lines){
				line = line.replace("{path}", file.getAbsolutePath());
				line = line.replace("{name}", tableName);
				line = line.replace("{db_name}", dbName);
				pw.println(line);
			}
			pw.flush();
			pw.close();
			temporaryFile.setExecutable(true, false);
			Process process = run.exec("./"+tableName+".executable", null, new File(WSEtl.readConfig("baseShpImportDir")));
			StreamCatcher errorGobbler = new StreamCatcher(process.getErrorStream(), "ERROR");
			StreamCatcher outputGobbler = new StreamCatcher(process.getInputStream(), "OUTPUT");
			errorGobbler.start();
		    outputGobbler.start();
			process.waitFor();	
			temporaryFile.delete();
		}finally{
			if(reader != null){
				reader.close();
			}
			if(pw != null){
				pw.close();
			}
			if(temporaryFile != null){
				temporaryFile.delete();
			}
		}
	}

	/**
	 * Crea una tabella a partire da un file dbf
	 * @param tableName nome della tabella
	 * @param dimensionInfo insieme di dimensioni
	 * @param measureInfo insieme di misure
	 * @param genericColumnInfo insieme di colonne generiche
	 * @throws Exception
	 */
	private boolean createTableFromDbf(String tableName, List<IImportColumnInfo> columnList)
			throws Exception {
		ArrayList<String> columns = new ArrayList<String>();
		ArrayList<DBColumnType> types = new ArrayList<DBColumnType>();
		//faccio l'elenco dei tipi delle colonne
		boolean geom = prepareDbColumns(columnList, columns, types);
		DBColumnType[] classArray = new DBColumnType[types.size()];
		classArray = types.toArray(classArray);
		//genero i nomi delle colonne
		String[] columnsArray = getColumnNames(columns);
		//creazione della tabella
		DBF dbfFile = null;
		try{
			if(DBUtils.createTable(tableName, columnsArray, classArray, geom ? "gid" : "")){
				if(geom){
					DBUtils.addGeometryColumn(tableName, GeometryType.POINT);
					columns.add("the_geom");
					columnsArray = getColumnNames(columns);
				}
				dbfFile = getDBF();
				List<String[]> batchList = new ArrayList<String[]>();
				for(int i = 0; i < dbfFile.getRecordCount(); i++){
					dbfFile.read();
					String[] values = getValues(dbfFile, columnList, geom, i);
					if(values != null){
						batchList.add(values);
					}
					if(batchList.size() == 1000 || i == (dbfFile.getRecordCount()-1)){
						//inserisco la riga
						if(!DBUtils.insertRow(tableName, columnsArray, batchList)){
							DBUtils.dropTable(tableName);
						}
						//svuoto la lista
						batchList.clear();
					}
				}
			}else{
				//drop della tabella
				//DBUtils.dropTable(tableName);
			}
			return geom;
		}catch(Throwable ex){
			//drop della tabella
			DBUtils.dropTable(tableName);
			throw new Exception(ex.getMessage());
		}
		finally{
			if(dbfFile != null){
				dbfFile.close();
			}
		}
	}

	/**
	 * Crea un array di stringhe con i valori presenti nelle celle del file
	 * @param dbfFile il file da cui estrarli. Il cursore sul file è già alla riga corretta.
	 * @param columnList la lista delle colonne da importare
	 * @param geom indica se si sta popolando una tabella con la geometria
	 * @return Un array di valori
	 * @throws ArrayIndexOutOfBoundsException
	 * @throws xBaseJException
	 */
	private String[] getValues(DBF dbfFile, List<IImportColumnInfo> columnList, boolean geom, int gid) throws ArrayIndexOutOfBoundsException, xBaseJException {
		ArrayList<String> valueList = new ArrayList<String>();
		if(geom){
			valueList.add(""+gid);
		}
		//scorro le colonne 
		String latitude = "";
		String longitude = "";
		for(int i = 0; i < columnList.size(); i++){
			
			IImportColumnInfo importColumnInfo = columnList.get(i);
			Field field = dbfFile.getField(importColumnInfo.getColumnName());
			String fieldValue = field.get();
			if(importColumnInfo instanceof GenericColumnInfo){
				GenericColumnInfo generic = (GenericColumnInfo)importColumnInfo;
				if(generic.getIsLatitude() || generic.getIsLongitude()){
					try{
						double d = Double.parseDouble(fieldValue);
						fieldValue = ""+d;
					}catch(NumberFormatException nfe){
						
					}
					if(generic.getIsLatitude()){
						latitude = fieldValue;
					}else{
						longitude = fieldValue;
					}
				}
			}
			if(!addValue(fieldValue, valueList, importColumnInfo)){
				return null;
			}			
		}	
		if(geom){
			valueList.add(getPointStringFromCoordinates(latitude, longitude));
		}
		return valueList.toArray(new String[valueList.size()]);
	}

	@Override
	public ColumnInfo[] readColumnInfo(boolean isShape, String extraArguments) throws Exception {
		//Per la lettura delle colonne utilizza la libreria xbasej
		DBF dbfFile = null;
		try{
			dbfFile = getDBF();
			int fieldCount = dbfFile.getFieldCount();			
			ColumnInfo[] fields = new ColumnInfo[fieldCount];
			for(int i = 1; i <= fieldCount; i++){
				Field field =  dbfFile.getField(i);
				String name = field.getName().toUpperCase();
//				if(name.length() > 63){
//					throw new Exception("Nome di colonna troppo lungo: "+name);
//				}
				fields[i -1] = createColumnInfo(field, isShape, name);
			}
			//a questo punto devo cercare di inferire le dimensioni
			inferColumnDimensions(dbfFile, fields);
			return fields;
		}finally{
			if(dbfFile != null){
				dbfFile.close();
			}
		}
	}
	
	@Override
	public VerifyUniquenessStatus verifyUniqueness(String columnName, String extraArguments) throws Exception{
		Field field = null;
		VerifyUniquenessStatus status = VerifyUniquenessStatus.OK;
		DBF dbfFile = null;
		try{
			dbfFile = getDBF();
			field = dbfFile.getField(columnName);
			HashSet<String> values = new HashSet<String>();
			for(int i = 1; i < dbfFile.getRecordCount(); i++){
				dbfFile.read();
				String value = field.get().toString();
				if(values.contains(value)){
					status = VerifyUniquenessStatus.NOT_UNIQUE;
					break;
				}
				values.add(value);
			}
		}catch(xBaseJException xbe){
			status = VerifyUniquenessStatus.COLUMN_NOT_EXISTS;
		}finally{
			if(dbfFile != null){
				dbfFile.close();
			}
		}
		return status;
	}
	
	/**
	 * Cerca di inferire il tipo di dimensione associata ad ogni colonna.
	 * Si basa sia sull'etichetta della colonna che sul contenuto delle prime 
	 * n righe.
	 * @param dbfFile il file da analizzare
	 * @param columnInfo l'array di informazioni sulle colonne da riempire con
	 * il tipo di dimensione che si ritiene associata
	 * @throws xBaseJException 
	 * @throws ArrayIndexOutOfBoundsException 
	 * @throws IOException 
	 */
	private void inferColumnDimensions(DBF dbfFile, ColumnInfo[] columnInfo) throws ArrayIndexOutOfBoundsException, xBaseJException, IOException {
		boolean latitudeFound = false;
		boolean longitudeFound = false;
		int latitudeIndex = -1;
		int longitudeIndex = -1;
		for(int i = 0; i < columnInfo.length; i++){
			Field field = dbfFile.getField(i+1);
			String fieldName = field.getName();
			if(fieldName.equalsIgnoreCase("anno") || fieldName.equalsIgnoreCase("year")){
				columnInfo[i].setInferedDimension(verifyDimensionByLabel(dbfFile, i+1, ColumnDimension.YEAR) ? ColumnDimension.YEAR : ColumnDimension.UNKNOWN);
//			} else if(fieldName.equalsIgnoreCase("cap") || fieldName.equalsIgnoreCase("postalCode")){
//				columnInfo[i].setInferedDimension(verifyDimensionByLabel(dbfFile, i+1, ColumnDimension.POSTAL_CODE) ? ColumnDimension.POSTAL_CODE : ColumnDimension.UNKNOWN);
			} else if(fieldName.equalsIgnoreCase("comune") || fieldName.equalsIgnoreCase("city")){
				columnInfo[i].setInferedDimension(verifyDimensionByLabel(dbfFile, i+1, ColumnDimension.CITY) ? ColumnDimension.CITY : ColumnDimension.UNKNOWN);
			} else if(fieldName.equalsIgnoreCase("provincia") || fieldName.equalsIgnoreCase("province")){
				columnInfo[i].setInferedDimension(verifyDimensionByLabel(dbfFile, i+1, ColumnDimension.PROVINCE) ? ColumnDimension.PROVINCE : ColumnDimension.UNKNOWN);
			} else if(fieldName.equalsIgnoreCase("regione") || fieldName.equalsIgnoreCase("region")){
				columnInfo[i].setInferedDimension(verifyDimensionByLabel(dbfFile, i+1, ColumnDimension.REGION) ? ColumnDimension.REGION : ColumnDimension.UNKNOWN);
			}  else if(!latitudeFound && (fieldName.toUpperCase().startsWith("LAT") || fieldName.toUpperCase().endsWith("_Y") || fieldName.equalsIgnoreCase("y"))){
				if(verifyDimensionByLabel(dbfFile, i+1, ColumnDimension.LATITUDE)){
					columnInfo[i].setInferedDimension(ColumnDimension.LATITUDE);
					columnInfo[i].setType(ColumnType.FLOAT);
					latitudeIndex = i;
					latitudeFound = true;					
				}
			} else if(!longitudeFound && (fieldName.toUpperCase().startsWith("LON") || fieldName.toUpperCase().endsWith("_X") || fieldName.equalsIgnoreCase("x"))){ 
				if(verifyDimensionByLabel(dbfFile, i+1, ColumnDimension.LONGITUDE)){
					columnInfo[i].setInferedDimension(ColumnDimension.LONGITUDE);
					columnInfo[i].setType(ColumnType.FLOAT);
					longitudeIndex = i;
					longitudeFound = true;
				}
			} else {
				//bisogna leggere un buon numero di record e cercare di capire che dimensione può essere
				inferByScanning(dbfFile, i+1, columnInfo[i]);
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
	 * Scorre un numero predefinito di righe del file per cercare di capire che dimensione è 
	 * associata alla i-esima colonna (sempre che una dimensione sia associata)
	 * @param dbfFile il file dbf
	 * @param index l'indice della colonna
	 * @param columnInfo le informazioni correnti su questa colonna
	 * @throws xBaseJException propaga le eccezioni del livello sottostante
	 * @throws IOException propaga le eccezioni del livello sottostante
	 */
	private void inferByScanning(DBF dbfFile, int index, ColumnInfo columnInfo) throws xBaseJException, IOException {
		dbfFile.gotoRecord(1);
		int recordToRead = Math.min(WSEtl.getMaxScanningRecords(), dbfFile.getRecordCount());
		if(recordToRead == 0){
			columnInfo.setInferedDimension(ColumnDimension.UNKNOWN);
		}else{
			//10% è il numero massimo di record non verificati per poter suggerire questa dimensione
			int maxInvalidRecords = recordToRead / 10;
			//array che conta i record non validi per ogni dimensione considerata
			int[] invalidRecords = new int[4];
			int cityIndex = 0;
			int provinceIndex = 1;
			int regionIndex = 2;
			int yearIndex = 3;
			Field field = dbfFile.getField(index);
			for(int i = 0; i < recordToRead; i++){
				String value = field.get().trim();
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
				if(i < (recordToRead - 1)){
					dbfFile.read();
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
	 * @param dbfFile il file dbf
	 * @param index l'indice della colonna a cui la dimensione dovrebbe essere associata
	 * @param candidate La dimensione candidata
	 * @return True se la dimensione è effettivamente associata a quella colonna, false altrimenti
	 * @throws xBaseJException 
	 * @throws ArrayIndexOutOfBoundsException 
	 * @throws IOException 
	 */
	private boolean verifyDimensionByLabel(DBF dbfFile, int index, ColumnDimension candidate) throws ArrayIndexOutOfBoundsException, xBaseJException, IOException {
		dbfFile.gotoRecord(1);
		int recordToRead = Math.min(WSEtl.getMaxVerifyRecords(), dbfFile.getRecordCount());
		Field field = dbfFile.getField(index);
		//10% è il numero massimo di record non verificati per poter suggerire questa dimensione
		int maxInvalidRecords = recordToRead / 10;
		int invalidRecords = 0;
		for(int i = 0; i < recordToRead; i++){
			dbfFile.read();
			String value = field.get().trim();		
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
	 * Crea l'informazione su una colonna a partire da un campo 
	 * di un file dbf.
	 * @param field Il campo del file dbf
	 * @param extraArguments indica se il file è uno shape (prende valore "true")
	 * @param name il nome della colonna
	 * @return L'informazione su una colonna estratta dal campo del file
	 */
	private ColumnInfo createColumnInfo(Field field, boolean isShape, String name) {
		ColumnType type = ColumnType.UNKNOWN;
		if(field.isCharField()){
			type = ColumnType.CHAR;
		} else if(field.isCurrencyFIeld()){
			type = ColumnType.CURRENCY;
		} else if(field.isDateField()){
			type = ColumnType.DATE;
		} else if(field.isFloatField()){
			type = ColumnType.FLOAT;
		} else if(field.isLogicalField()){
			type = ColumnType.BOOLEAN;
		} else if(field.isMemoField()){
			type = ColumnType.STRING;
		} else if(field.isNumField()){
			type = ColumnType.NUMBER;
		} else if(field.isPictureField()){
			type = ColumnType.PICTURE;
		}		
		ColumnInfo info = new ColumnInfo(name, type, !isShape); 
		if(type == ColumnType.NUMBER){
			info.setDecimalPositions(0);
		} else if(type == ColumnType.FLOAT){
			info.setDecimalPositions(field.getDecimalPositionCount());
		} 
		return info;
	}

	@Override
	public int getCardinality(String columnName, String extraArguments) throws Exception {
		String filePath = file.getAbsolutePath();
		if(file.getName().toUpperCase().endsWith(".SHP")){
			filePath = filePath.substring(0, filePath.length() -3) + "dbf";
		}
		DBF dbfFile = null;
		try{
			dbfFile = new DBF(filePath, DBF.READ_ONLY);
			HashSet<String> values = new HashSet<String>();
			Field field = dbfFile.getField(columnName);
			for(int i = 0; i < dbfFile.getRecordCount(); i++){
				String value = field.get();
				if(value != null && !values.contains(value)){
					values.add(value);
				}
			}
			return values.size();
		} finally{
			if(dbfFile != null){
				dbfFile.close();
			}
		}
	}

	@Override
	public int count(String extraArguments) throws Exception {
		DBF dbfFile = null;
		try{
			dbfFile = getDBF();
			return dbfFile.getRecordCount();
		} finally{
			if(dbfFile != null){
				dbfFile.close();
			}
		}
	}

}