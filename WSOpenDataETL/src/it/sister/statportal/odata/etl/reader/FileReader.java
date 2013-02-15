package it.sister.statportal.odata.etl.reader;

import it.sister.statportal.odata.domain.MdHierNode;
import it.sister.statportal.odata.etl.WSEtl;
import it.sister.statportal.odata.etl.WSEtl.ImportDataStatus;
import it.sister.statportal.odata.etl.WSEtl.VerifyUniquenessStatus;
import it.sister.statportal.odata.etl.exchange.ColumnInfo;
import it.sister.statportal.odata.etl.exchange.DimensionInfo;
import it.sister.statportal.odata.etl.exchange.GenericColumnInfo;
import it.sister.statportal.odata.etl.exchange.IImportColumnInfo;
import it.sister.statportal.odata.etl.exchange.MeasureInfo;
import it.sister.statportal.odata.etl.exchange.Pair;
import it.sister.statportal.odata.etl.exchange.TableInfo;
import it.sister.statportal.odata.etl.exchange.Warning;
import it.sister.statportal.odata.etl.exchange.WarningSet;
import it.sister.statportal.odata.etl.exchange.WarningType;
import it.sister.statportal.odata.utility.DBUtils;
import it.sister.statportal.odata.utility.DBUtils.DBColumnType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.mozilla.universalchardet.UniversalDetector;

public abstract class FileReader implements IFileReader {

	protected File file;
	
	protected String escapeSequence;
	
	protected String geometryQuery;
	
	//dimensione temporale
	protected DimensionInfo year;
	
	//comuni
	protected DimensionInfo city;
	
	//province
	protected DimensionInfo province;
	
	//regioni
	protected DimensionInfo region;
	
	@Override
	public abstract ColumnInfo[] readColumnInfo(boolean isShape, String extraArguments) throws Exception;

	@Override
	public abstract VerifyUniquenessStatus verifyUniqueness(String columnName, String extraArguments) throws Exception;

	@Override
	public abstract TableInfo writeTable(List<IImportColumnInfo> columnList, String extraArguments) throws Exception;
	
	@Override
	public abstract int getCardinality(String columnName, String extraArguments) throws Exception;
	
	@Override
	public abstract int count(String extraArguments) throws Exception;
	
	@Override
	public List<String> getOriginalNames(List<IImportColumnInfo> columnList) throws Exception {
		List<String> originalNames = new ArrayList<String>();
		for(int i = 0; i < columnList.size(); i++){
			originalNames.add(columnList.get(i).getColumnName());
		}
		return originalNames;
	}
	
	/**
	 * Costruisce un file reader
	 * @param file il file su cui il reader deve lavorare
	 */
	public FileReader(File file){
		this.escapeSequence = DBUtils.getEscapeSequence();
		this.geometryQuery = DBUtils.getPointStringFromCoordinates("longitude", "latitude");
		this.file = file;
		year = initializeDimensionInfo("yearId");
		city = initializeDimensionInfo("cityId");
		province = initializeDimensionInfo("provinceId");
		region = initializeDimensionInfo("regionId");
	}

	/**
	 * Fornisce la query per le coordinate
	 * @param latitude la latitudine
	 * @param longitude la longitudine
	 * @return la stringa delle coordinate
	 */
	protected String getPointStringFromCoordinates(String latitude, String longitude){
		return this.geometryQuery.replaceFirst("longitude", longitude).replaceFirst("latitude", latitude);
	}
	
	/**
	 * Inizializza le informazioni su una dimensione basandosi sull'id letto nel file di configurazione
	 * @param key il nome della chiave nel file di configurazione
	 * @return le informazioni su una dimensione
	 */
	private DimensionInfo initializeDimensionInfo(String key) {
		String id = WSEtl.readConfig(key);
		DimensionInfo dimensionInfo = null;
		if(id != null){
			try{
				int parsedId = Integer.parseInt(id);
				dimensionInfo = new DimensionInfo(parsedId, "", "", "");
			}catch(Exception ex){
			
			}
		}
		return dimensionInfo;
	}
	
	/**
	 * Cerca di individuare l'encoding del file su cui è basato il reader
	 * @return l'encoding su cui è basato il file o UTF-8 se non riesce ad individuarlo
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	protected String detectEncoding() throws FileNotFoundException, IOException {
		byte[] buf = new byte[4096];
		java.io.FileInputStream fis = new java.io.FileInputStream(file.getAbsoluteFile());
		UniversalDetector detector = new UniversalDetector(null);
		int nread;
		while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
		     detector.handleData(buf, 0, nread);
		}
		detector.dataEnd();
		String encoding = detector.getDetectedCharset();
		if(encoding == null){
			encoding = "UTF-8";
		}
		detector.reset();
		return encoding;
	}
	
	/**
	 * Aggiunge un valore alla lista
	 * @param fieldValue il valore da aggiungere
	 * @param valueList la lista
	 * @param importColumnInfo la colonna a cui il valore fa riferimento
	 */
	protected boolean addValue(String fieldValue, ArrayList<String> valueList, IImportColumnInfo importColumnInfo) {
		String value = fieldValue;
		if(value != null){
			value = fieldValue.trim();
		}	
		switch(importColumnInfo.getColumnType()){
			case DIMENSION:	
				if(!addDimensionValue(value, valueList, importColumnInfo)){
					return false;
				}
				break;
			case MEASURE:
				if(!addMeasureValue(value, valueList)){
					return false;
				}
				break;
			case GENERIC:
				if(!addGenericColumnValue(value, valueList, (GenericColumnInfo)importColumnInfo)){
					return false;
				}
				break;
		}
		return true;
	}
	
	/**
	 * Riempie due liste con i nomi e i tipi delle colonne da creare nel db
	 * @param columnList la lista di colonne del file
	 * @param columns i nomi delle colonne da creare
	 * @param types i tipi delle colonne da creare
	 * @return true se sono state aggiunte le colonne della geometria, false altrimenti
	 */
	protected boolean prepareDbColumns(List<IImportColumnInfo> columnList, ArrayList<String> columns, ArrayList<DBColumnType> types) {
		boolean geom = false;
		for(int i = 0; i < columnList.size(); i++){
			IImportColumnInfo column = columnList.get(i);
			columns.add(column.getColumnName());
			switch(column.getColumnType()){
				case DIMENSION:					
					types.add(DBColumnType.INT);
					break;
				case MEASURE:
					types.add(DBColumnType.DOUBLE);
					break;
				case GENERIC:					
					geom |= (((GenericColumnInfo)column).getIsLatitude() || ((GenericColumnInfo)column).getIsLongitude());
					types.add(DBColumnType.STRING);
					break;
			}
		}
		//se ho la geometria
		if(geom){
			columns.add(0, "gid");
			types.add(0, DBColumnType.INT);
		}
		return geom;
	}
	
	/**
	 * Cambia i nomi delle colonne in modo che siano nomi validi
	 * @param columns l'insieme di nomi corrente
	 * @return un insieme di nomi di colonne
	 */
	protected String[] getColumnNames(ArrayList<String> columns) {
		String[] columnsArray = new String[columns.size()];
		columnsArray = columns.toArray(columnsArray);
		for(int i = 0; i < columnsArray.length; i++){
			if(!columnsArray[i].equalsIgnoreCase("gid") && !columnsArray[i].equalsIgnoreCase("the_geom")){
				columnsArray[i] = ("C"+columnsArray[i].replaceAll("[^\\w^0-9]", "_")).toLowerCase();
				columnsArray[i] = columnsArray[i].substring(0, Math.min(50, columnsArray[i].length()));
			}
		}
		return columnsArray;
	}
	
	/**
	 * Aggiunge un valore alla lista
	 * @param fieldValue il valore che è stato letto dal file
	 * @param valueList la lista a cui aggiungere il valore
	 * @param importColumnInfo informazioni sulla colonna
	 */
	protected boolean addDimensionValue(String fieldValue, ArrayList<String> valueList, IImportColumnInfo importColumnInfo) {
		if(!verifyDimensionValue(fieldValue, (DimensionInfo)importColumnInfo)){
			return false;
		}
		if(((DimensionInfo)importColumnInfo).getIsByKey()){
			String castedFieldValue = toIntegerString(fieldValue);
			valueList.add( Integer.valueOf(castedFieldValue).toString().replace(',', '.'));
		}else{
			valueList.add( Integer.valueOf(((DimensionInfo)importColumnInfo).getTable().get(fieldValue.toUpperCase())).toString().replace(',', '.'));
		}
		return true;
	}
	
	/**
	 * Verifica che un valore sia una misura valida
	 * @param value il valore
	 * @return true se è valido, false altrimenti
	 */
	private boolean verifyMeasureValue(String value){
		if(value != null && !value.equals("")){
			try{
				Double.parseDouble(value.replace(',', '.'));
			}catch(NumberFormatException nfe){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Aggiunge un valore alla lista
	 * @param fieldValue il valore che è stato letto dal file
	 * @param valueList la lista a cui aggiungere il valore
	 */
	protected boolean addMeasureValue(String fieldValue, ArrayList<String> valueList) {
		if(!verifyMeasureValue(fieldValue)){
			return false;
		}
		if(fieldValue.equals("")){
			valueList.add("null");
		} else{
			valueList.add(Double.valueOf(fieldValue.replace(',', '.')).toString().replace(',', '.'));
		}
		return true;
	}
	
	/**
	 * Verifica che un valore sia valido per una colonna generica.
	 * Il controllo viene fatto solo se la colonna è indicata come contenente coordinate.
	 * @param value il valore
	 * @param genericColumn la colonna generica
	 * @return true se è valido, false altrimenti
	 */
	private boolean verifyGenericColumnValue(String value, GenericColumnInfo genericColumn){
		if(genericColumn.getIsLatitude()){
			return verifyLatitude(value);
		}else if(genericColumn.getIsLongitude()){
			return verifyLongitude(value);
		}
		return true;
	}
	
	/**
	 * Aggiunge un valore alla lista
	 * @param fieldValue il valore che è stato letto dal file
	 * @param valueList la lista a cui aggiungere il valore
	 */
	protected boolean addGenericColumnValue(String fieldValue, ArrayList<String> valueList, GenericColumnInfo genericColumn) {
		if(!verifyGenericColumnValue(fieldValue, genericColumn)){
			return false;
		}
		if(genericColumn.getIsLatitude() || genericColumn.getIsLongitude()){
			valueList.add("'"+Double.valueOf(fieldValue.replace(',', '.')).toString().replace(',', '.')+"'");
		}else{
			StringBuilder sb = new StringBuilder(fieldValue.length() * 2);
			int toEscapeLen = fieldValue.length();
			for(int j = 0; j < toEscapeLen; j++){
				char currentChar = fieldValue.charAt(j); 
				if(currentChar == '\'' || currentChar == '\\'){
					sb.append(this.escapeSequence);
				}	
				sb.append(currentChar);						
			}
			valueList.add("'"+sb.toString()+"'");
		}
		return true;
	}
	
	/**
	 * Fornisce l'insieme dei warning relativi ad un file.
	 * Ogni reader implementerà questo metodo a seconda delle proprie esigenze.
	 * @param dimensions l'insieme di dimensioni
	 * @param measures l'insieme di misure
	 * @param genericColumns l'insieme delle colonne generiche
	 * @param warnings lista di misure da riempire
	 * @param extraArguments eventuali argomenti extra (utilizzati nei csv)
	 * @throws Exception
	 */
	public abstract WarningSet getWarnings(DimensionInfo[] dimensions, MeasureInfo[] measures, GenericColumnInfo[] genericColumns, String extraArguments) throws Exception;
	
//	@Override
//	public WarningSet getWarnings(DimensionInfo[] dimensions, MeasureInfo[] measures, GenericColumnInfo[] genericColumns, String extraArguments) throws Exception {
//		List<Warning> warnings = new ArrayList<Warning>();
//		//chiamo il metodo astratto che ogni reader implementa a seconda delle proprie caratteristiche
//		getWarnings(dimensions, measures, genericColumns, warnings, extraArguments);
//		return warnings.toArray(new Warning[warnings.size()]);
//	}

	/**
	 * Verifica che un determinato valore sia valido come misura
	 * @param i indice di riga
	 * @param value valore da verificare
	 * @param measure misura
	 * @param warnings lista di warning a cui eventualmente aggiungere un elemento
	 * @return true se è stato individuato un warning, false altrimenti
	 */
	protected boolean checkWarning(int i, String value, MeasureInfo measure, List<Warning> warnings) {
		if(!verifyMeasureValue(value)){
			warnings.add(new Warning("Riga: "+i+". Colonna: "+measure.getAlias()+". Valore non valido: "+value, WarningType.MEASURE));
			return true;
		}
		return false;
	}
	
	/**
	 * verifica che un determinato valore sia valido per la colonna generica indicata.
	 * Il controllo viene effettuato solo se la colonna generica è indicata come una colonna di coordinate.
	 * In questo caso il contenuto della colonna deve essere un valore valido per le coordinate
	 * @param i indice di riga
	 * @param value valore da verificare
	 * @param genericColumn colonna generica
	 * @param warnings lista di warning a cui eventualmente aggiungere un elemento
	 * @return true se è stato individuato un warning, false altrimenti
	 */
	protected boolean checkWarning(int i, String value, GenericColumnInfo genericColumn, List<Warning> warnings) {
		if(!verifyGenericColumnValue(value, genericColumn)){
			String columnName = "";
			if(genericColumn.getIsLatitude()){
				columnName = "Latitudine";
			}else if(genericColumn.getIsLongitude()){
				columnName = "Longitudine";
			}else {
				columnName = genericColumn.getAlias();
			}
			warnings.add(new Warning("Riga: "+i+". Colonna: "+columnName+". Valore non valido: "+value, WarningType.GENERIC_COLUMN));
			return true;
		}
		return false;
	}
	
	/**
	 * verifica che un determinato valore sia valido per la dimensione indicata.
	 * Il valore può essere corrispondente alle chiavi o alle descrizioni della dimensione
	 * @param i indice di riga
	 * @param value valore da verificare
	 * @param dim dimensione
	 * @param warnings lista di warning a cui eventualmente aggiungere un elemento
	 * @return true se è stato individuato un warning, false altrimenti
	 */
	protected boolean checkWarning(int i, String value, DimensionInfo dim, List<Warning> warnings) {		
		if(!verifyDimensionValue(value, dim)){
			warnings.add(new Warning("Riga: "+i+". Colonna: "+dim.getAlias()+". Valore non valido: "+value, WarningType.DIMENSION));
			return true;
		}
		return false;
	}

	/**
	 * Converte la stringa passata come parametro in una stringa rappresentante un intero
	 * @param value la stringa da convertire
	 * @return una stringa rappresentante un intero
	 */
	private String toIntegerString(String value){
		String castedValue = "";
		try{
			Double d = Double.parseDouble(value);
			castedValue = ""+d.intValue();
		}catch(NumberFormatException nfe){
			castedValue = "";
		}
		return castedValue;
	}
	
	/**
	 * Verifica che un determinato valore sia valido per una dimensione
	 * @param value il valore
	 * @param dim la dimensione
	 * @return true se è valido, false altrimenti
	 */
	private boolean verifyDimensionValue(String value, DimensionInfo dim) {
		Pair<HashSet<String>,HashSet<String>> nodeContent = dim.getKeysAndDescriptions();
		//se la mappa dimensione-tipo di verifica non contiene la entry
		if(dim.getIsByKey() == null){
			String castedValue = toIntegerString(value);
			//devo cercare di verificare sia per chiave che per valore
			if(nodeContent.getFirst().contains(castedValue)){
				dim.setIsByKey(true);
			}else if(value != null && nodeContent.getSecond().contains(value.toUpperCase())){
				dim.setIsByKey(false);
			} else{		
				return false;
			}
		}else{
			//prendo la entry e vedo se verificare per chiave o per valore
			if(dim.getIsByKey()){
				String castedValue = toIntegerString(value);
				if(!nodeContent.getFirst().contains(castedValue)){
					return false;
				}
			}else{
				if(value == null || !nodeContent.getSecond().contains(value.toUpperCase())){
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Calcola un nome valido per una tabella basandosi sul dominio.
	 * @param name Il nome del file
	 * @return un nome valido per la tabella
	 */
	protected String getTableName(String name) {
		String tableName = name;
		tableName = ("T"+tableName.replaceAll("[^\\w^0-9]", "_")).toLowerCase();
		if(tableName.length() > 20){
			tableName = tableName.substring(0,20);
		}
		int i = 0;
		String candidateName = tableName;
		while(DBUtils.tableExists(candidateName)){
			i++;
			candidateName = tableName + "_" + i;
		}
		return candidateName;
	}
	
	/**
	 * Dato un nodo estrae dal db l'elenco delle possibili chiavi e delle descrizioni 
	 * presenti nella tabella associata al nodo
	 * @param node il nodo
	 * @return una coppia di insiemi contenenti le chiavi e le descrizioni del nodo
	 */
	protected Pair<HashSet<String>,HashSet<String>> getNodeKeysAndValues(MdHierNode node){
		List<Map<String, Object>> result = DBUtils.executeQueryForList("SELECT " + node.getPkField()+ " AS NODE_ID, " + node.getDescField()+ " AS NODE_DESCR FROM " + node.getTableName());
		HashSet<String> nodeIdSet = new HashSet<String>();
		HashSet<String> nodeDescrSet = new HashSet<String>();
		for(Map<String, Object> entry : result){
			Double key = Double.parseDouble(entry.get("NODE_ID").toString());				
			String nodeId =  Integer.valueOf(key.intValue()).toString();
			if(!nodeIdSet.contains(nodeId)){
				nodeIdSet.add(nodeId);
			}
			String nodeDescr = entry.get("NODE_DESCR").toString();
			if(!nodeDescrSet.contains(nodeDescr)){
				nodeDescrSet.add(nodeDescr.toUpperCase());
			}
		}
		return new Pair<HashSet<String>,HashSet<String>>(nodeIdSet, nodeDescrSet);
	}
	
	/**
	 * Crea uno stato di errore impostando il messaggio di chiave mancante
	 * @param value
	 * @return
	 */
	protected ImportDataStatus missingKeyStatus(String value){
		ImportDataStatus errorStatus = ImportDataStatus.INVALID_DIMENSION_KEY;
		errorStatus.setErrorMessage("Valore non presente:"+value);
		return errorStatus;
	}
	
	/**
	 * Verifica che il valore passato come parametro possa essere il codice istat di una regione
	 * @param value il valore di una cella
	 * @return true se il valore è un codice istat valido, false altrimenti
	 */
	protected boolean verifyRegion(String value) {
		boolean verified = false;
		try{
			int parsedValue = Integer.parseInt(value);
			if(region != null){
				Pair<HashSet<String>, HashSet<String>> keysAndDescriptions = region.getKeysAndDescriptions();
				return keysAndDescriptions.getFirst().contains(toIntegerString(value));
			}else{
				//se non è stata configurata la chiave continuo a lavorare con il range
				//il codice istat di una regione è un numero di due cifre minore di 21
				if(parsedValue > 0 && parsedValue < 21){
					verified = true;
				} else{
					verified = false;
				}
			}
		}catch(NumberFormatException nfe){
			verified = false;
		}
		return verified;
	}

	/**
	 * Verifica che il valore passato come parametro possa essere il codice istat di una provincia
	 * @param value il valore di una cella
	 * @return true se il valore è un codice istat valido, false altrimenti
	 */
	protected boolean verifyProvince(String value) {
		boolean verified = false;
		try{
			int parsedValue = Integer.parseInt(value);
			if(province != null){
				Pair<HashSet<String>, HashSet<String>> keysAndDescriptions = province.getKeysAndDescriptions();
				return keysAndDescriptions.getFirst().contains(toIntegerString(value));
			}else{
				//il codice istat di una provincia è un numero di tre cifre minore di 111
				if(parsedValue > 0 && parsedValue < 111){
					verified = true;
				} else{
					verified = false;
				}
			}
		}catch(NumberFormatException nfe){
			verified = false;
		}
		return verified;
	}

	/**
	 * Verifica che il valore passato come parametro possa essere il codice istat di una città
	 * @param value il valore di una cella
	 * @return true se il valore è un codice istat valido, false altrimenti
	 */
	protected boolean verifyCity(String value) {
		boolean verified = false;
		if(city != null){
			Pair<HashSet<String>, HashSet<String>> keysAndDescriptions = city.getKeysAndDescriptions();
			return keysAndDescriptions.getFirst().contains(toIntegerString(value));
		}else{
			//per semplificare diciamo che il codice istat deve essere un numero di 4, 5 o 6 cifre
			try{
				Integer.parseInt(value);
				verified = true;
			}catch(NumberFormatException nfe){
				verified = false;
			}
		}
		return verified;
	}

	/**
	 * Verifica che il valore passato come parametro possa essere visto come un cap
	 * @param value il valore di una cella
	 * @return true se il valore è un cap valido, false altrimenti
	 */
	protected boolean verifyPostalCode(String value) {
		boolean verified = false;
		//il cap deve essere lungo 5 e devo poterlo leggere come numero
		if(value.length() == 5){
			try{
				Integer.parseInt(value);
				verified = true;
			}catch(NumberFormatException nfe){
				verified = false;
			}
		} else{
			verified = false;
		}
		return verified;
	}

	/**
	 * Verifica che il valore passato come parametro possa essere visto come un anno
	 * @param value il valore di una cella
	 * @return true se il valore è un anno, false altrimenti
	 */
	protected boolean verifyYear(String value){		
		boolean verified = false;
		try{
			int parsedValue = Integer.parseInt(value);
			if(year != null){
				Pair<HashSet<String>, HashSet<String>> keysAndDescriptions = year.getKeysAndDescriptions();
				return keysAndDescriptions.getFirst().contains(toIntegerString(value));
			}else{
				//consideriamo validi gli anni tra il 1900 e il 2050
				if(parsedValue >= 1800 && parsedValue <= 2050){
					verified = true;
				} else{
					verified = false;
				}
			}
		}catch(NumberFormatException nfe){
			verified = false;
		}
		return verified;
	}
	
	/**
	 * Verifica che il valore passato come parametro possa essere visto come un valore di longitudine
	 * @param value il valore letto
	 * @return true se è nell'intervallo -180, +180, false altrimenti
	 */
	protected boolean verifyLongitude(String value){
		if(value != null && !value.equals("")){
			try{
				double d = Double.parseDouble(value.replace(',', '.'));
				if(d < -180 || d > 180){
					return false;
				}else{
					return true;
				}
			}catch(NumberFormatException nfe){
				return false;
			}
		}
		return false;
	}
	
	/**
	 * Verifica che il valore passato come parametro possa essere visto come un valore di latitudine
	 * @param value il valore letto
	 * @return true se è nell'intervallo -90, +90, false altrimenti
	 */	
	protected boolean verifyLatitude(String value){
		if(value != null && !value.equals("")){
			try{
				double d = Double.parseDouble(value.replace(',', '.'));
				if(d < -90 || d > 90){
					return false;
				}else{
					return true;
				}
			}catch(NumberFormatException nfe){
				return false;
			}
		}
		return false;
	}
}
