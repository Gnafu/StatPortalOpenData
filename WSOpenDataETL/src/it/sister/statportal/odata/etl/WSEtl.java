package it.sister.statportal.odata.etl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import it.sister.statportal.odata.domain.Column;
import it.sister.statportal.odata.domain.Column.ColumnType;
import it.sister.statportal.odata.domain.IRepository.SimpleColumnInfo;
import it.sister.statportal.odata.domain.MdData;
import it.sister.statportal.odata.domain.MdDataDim;
import it.sister.statportal.odata.domain.MdDataFiles;
import it.sister.statportal.odata.domain.MdDataFilesPK;
import it.sister.statportal.odata.domain.MdGenericColumn;
import it.sister.statportal.odata.domain.MdHierNode;
import it.sister.statportal.odata.domain.MdMeasureFields;
import it.sister.statportal.odata.domain.OdataDomainException;
import it.sister.statportal.odata.etl.exchange.Alias;
import it.sister.statportal.odata.etl.exchange.AliasSet;
import it.sister.statportal.odata.etl.exchange.ColumnInfo;
import it.sister.statportal.odata.etl.exchange.DataInfo;
import it.sister.statportal.odata.etl.exchange.DeleteDataResult;
import it.sister.statportal.odata.etl.exchange.DimensionInfo;
import it.sister.statportal.odata.etl.exchange.FileUploadInfo;
import it.sister.statportal.odata.etl.exchange.GenericColumnInfo;
import it.sister.statportal.odata.etl.exchange.IImportColumnInfo;
import it.sister.statportal.odata.etl.exchange.ImportDataInfo;
import it.sister.statportal.odata.etl.exchange.MeasureInfo;
import it.sister.statportal.odata.etl.exchange.Pair;
import it.sister.statportal.odata.etl.exchange.TableInfo;
import it.sister.statportal.odata.etl.exchange.VerifyUniquenessInfo;
import it.sister.statportal.odata.etl.exchange.Warning;
import it.sister.statportal.odata.etl.exchange.WarningSet;
import it.sister.statportal.odata.etl.proxy.WSEtlProxy;
import it.sister.statportal.odata.etl.reader.CsvFileReader;
import it.sister.statportal.odata.etl.reader.DbfFileReader;
import it.sister.statportal.odata.etl.reader.IFileReader;
import it.sister.statportal.odata.etl.reader.MdbFileReader;
import it.sister.statportal.odata.etl.reader.XlsFileReader;
import it.sister.statportal.odata.etl.reader.XlsxFileReader;
import it.sister.statportal.odata.utility.DBUtils;

/**
 * Web Service che si occupa del caricamento dei dati.
 * 
 */
public class WSEtl {

	protected static Logger logger = Logger.getLogger(WSEtl.class);	
	protected static ExtensionFilter shpFilter = new ExtensionFilter("shp");
	protected static ExtensionFilter dbfFilter = new ExtensionFilter("dbf");
	protected static ExtensionFilter xlsxFilter = new ExtensionFilter("xlsx");
	protected static ExtensionFilter xlsFilter = new ExtensionFilter("xls");
	protected static ExtensionFilter csvFilter = new ExtensionFilter("csv");
	protected static ExtensionFilter mdbFilter = new ExtensionFilter("mdb");
	protected static ExtensionFilter txtFilter = new ExtensionFilter("txt");
	protected static ExtensionFilter kmlFilter = new ExtensionFilter("kml");
	protected static ExtensionFilter kmzFilter = new ExtensionFilter("kmz");
	// lista dei formati supportati per il caricamento
	protected static List<String> supportedFileFormats;
	// lista dei formati che possono essere strutturati
	protected static List<String> structuredFileFormats;
	
	/**
	 * Enumerato che contiene i valori che possono venire restituiti in seguito
	 * a un controllo di univocità su una colonna di un file.
	 * 
	 * @author m.scarpellini
	 * 
	 */
	public enum VerifyUniquenessStatus {
		FILE_NOT_EXISTS, FILE_NOT_SUPPORTED, FILE_NOT_STRUCTURED, COLUMN_NOT_EXISTS, ERROR, NOT_UNIQUE, OK;

		protected String errorMessage = "";

		/**
		 * Restituisce una descrizione in linguaggio naturale dello stato.
		 * 
		 * @return La descrizione del suo stato in linguaggio naturale
		 */
		public String getDescription() {
			String description = "";
			switch (this) {
			case FILE_NOT_EXISTS:
				description = "Il file {0} non esiste.";
				break;
			case FILE_NOT_SUPPORTED:
				description = "Il formato del file {0} non è supportato.";
				break;
			case FILE_NOT_STRUCTURED:
				description = "Il file {0} non può essere letto come file strutturato.";
				break;
			case COLUMN_NOT_EXISTS:
				description = "La colonna {1} non esiste.";
				break;
			case NOT_UNIQUE:
				description = "La colonna non contiene valori univoci";
				break;
			case OK:
				description = "La colonna contiene valori univoci";
				break;
			case ERROR:
				description = errorMessage;
				break;
			default:
				break;
			}
			return description;
		}

		/**
		 * Restituisce un codice che identifica lo stato
		 * 
		 * @return Il codice che identifica lo stato
		 */
		public int getCode() {
			int code = 0;
			switch (this) {
			case FILE_NOT_EXISTS:
				code = -1;
				break;
			case FILE_NOT_SUPPORTED:
				code = -2;
				break;
			case FILE_NOT_STRUCTURED:
				code = -3;
				break;
			case COLUMN_NOT_EXISTS:
				code = -4;
				break;
			case NOT_UNIQUE:
				code = 0;
				break;
			case OK:
				code = 1;
				break;
			case ERROR:
				code = -5;
				break;
			default:
				break;
			}
			return code;
		}

		/**
		 * Se esiste restituisce il messaggio di errore altrimenti la stringa
		 * vuota.
		 * 
		 * @return Se esiste restituisce il messaggio di errore altrimenti la
		 *         stringa vuota.
		 */
		public String getErrorMessage() {
			return errorMessage;
		}

		/**
		 * Imposta il messaggio di errore
		 * 
		 * @param message
		 *            il messaggio di errore.
		 */
		public void setErrorMessage(String message) {
			errorMessage = message;
		}
	}

	/**
	 * Enumerato che contiene i valori che possono venire restituiti in seguito
	 * all'analisi di un file caricato sul server.
	 * 
	 * @author m.scarpellini
	 * 
	 */
	public enum FileUploadStatus {
		NOT_EXIST, NOT_SUPPORTED, NOT_STRUCTURED, ERROR, INVALID_STRUCTURE, EMPTY, OK, DUPLICATED_COLUMN_NAMES;

		
		
		protected String errorMessage = "";

		/**
		 * Restituisce una descrizione in linguaggio naturale dello stato.
		 * 
		 * @return La descrizione del suo stato in linguaggio naturale
		 */
		public String getDescription() {
			String description = "";
			switch (this) {
			case NOT_EXIST:
				description = "Il file {0} non esiste.";
				break;
			case NOT_SUPPORTED:
				description = "Il formato del file {0} non è supportato.";
				break;
			case NOT_STRUCTURED:
				description = "Il file {0} non può essere letto come file strutturato.";
				break;
			case INVALID_STRUCTURE:
				description = "Non è possibile estrarre la struttura del file {0}.";
				break;
			case EMPTY:
				description = "Il file {0} è vuoto";
				break;
			case OK:
				description = "Struttura estratta correttamente";
				break;
			case ERROR:
				description = errorMessage;
				break;
			case DUPLICATED_COLUMN_NAMES:
				description = errorMessage;
				break;
			default:
				break;
			}
			return description;
		}

		/**
		 * Restituisce un codice che identifica lo stato
		 * 
		 * @return Il codice che identifica lo stato
		 */
		public int getCode() {
			int code = 0;
			switch (this) {
			case NOT_EXIST:
				code = -1;
				break;
			case NOT_SUPPORTED:
				code = -2;
				break;
			case NOT_STRUCTURED:
				code = 1;
				break;
			case INVALID_STRUCTURE:
				code = 2;
				break;
			case EMPTY:
				code = 3;
				break;
			case OK:
				code = 4;
				break;
			case ERROR:
				code = -3;
				break;
			case DUPLICATED_COLUMN_NAMES:
				code = -5;
				break;
			default:
				break;
			}
			return code;
		}

		/**
		 * Se esiste restituisce il messaggio di errore altrimenti la stringa
		 * vuota.
		 * 
		 * @return Se esiste restituisce il messaggio di errore altrimenti la
		 *         stringa vuota.
		 */
		public String getErrorMessage() {
			return errorMessage;
		}

		/**
		 * Imposta il messaggio di errore
		 * 
		 * @param message
		 *            il messaggio di errore.
		 */
		public void setErrorMessage(String message) {
			errorMessage = message;
		}
	}

	/**
	 * Enumerato che contiene i valori che possono venire restituiti in seguito
	 * all'importazione di un file
	 * 
	 * @author m.scarpellini
	 * 
	 */
	public enum ImportDataStatus {
		INVALID_INPUT, INVALID_DATA_INFO, INVALID_MEASURE_COLLECTION, INVALID_MEASURE_VALUE, INVALID_DIMENSION_COLLECTION, INVALID_DIMENSION_KEY, INVALID_GENERIC_COLUMN_COLLECTION, FILE_NOT_EXISTS, FILE_NOT_STRUCTURED, FILE_NOT_SUPPORTED, COLUMN_NOT_EXISTS, ERROR, VERIFIED_BY_KEY, VERIFIED_BY_DESC, OK;

		protected String errorMessage = "";

		/**
		 * Restituisce una descrizione in linguaggio naturale dello stato.
		 * 
		 * @return La descrizione del suo stato in linguaggio naturale
		 */
		public String getDescription() {
			String description = "";
			if (errorMessage != null && !errorMessage.equals("")) {
				description = errorMessage + ". ";
			}
			switch (this) {
			case INVALID_INPUT:
				description += "Bisogna definire almeno una misura, dimensione o colonna generica";
				break;
			case INVALID_DATA_INFO:
				description += "Informazioni sul dato non strutturate correttamente";
				break;
			case INVALID_MEASURE_COLLECTION:
				description += "Informazioni sulle misure non strutturate correttamente";
				break;
			case INVALID_MEASURE_VALUE:
				description += "Le misure possono avere solo valori numerici";
				break;
			case INVALID_DIMENSION_COLLECTION:
				description += "Informazioni sulle dimensioni non strutturate correttamente";
				break;
			case INVALID_DIMENSION_KEY:
				description += "I valori delle dimensioni devono essere presenti nel nodo selezionato";
				break;
			case FILE_NOT_EXISTS:
				description += "Il file {0} non esiste.";
				break;
			case FILE_NOT_SUPPORTED:
				description += "Il formato del file {0} non è supportato.";
				break;
			case FILE_NOT_STRUCTURED:
				description += "Il file {0} non può essere letto come file strutturato.";
				break;
			case COLUMN_NOT_EXISTS:
				description += "La colonna {1} non esiste.";
				break;
			case OK:
				description += "Struttura estratta correttamente";
				break;
			case ERROR:
				description = errorMessage;
				break;
			case INVALID_GENERIC_COLUMN_COLLECTION:
				description += "Informazioni sulle colonne generiche non strutturate correttamente";
				break;
			default:
				break;
			}
			return description;
		}

		/**
		 * Restituisce un codice che identifica lo stato
		 * 
		 * @return Il codice che identifica lo stato
		 */
		public int getCode() {
			int code = 0;
			switch (this) {
			case INVALID_INPUT:
				code = -10;
				break;
			case INVALID_DATA_INFO:
				code = -1;
				break;
			case INVALID_MEASURE_COLLECTION:
				code = -2;
				break;
			case INVALID_MEASURE_VALUE:
				code = 1;
				break;
			case INVALID_DIMENSION_COLLECTION:
				code = -3;
				break;
			case INVALID_DIMENSION_KEY:
				code = 2;
				break;
			case FILE_NOT_EXISTS:
				code = -4;
				break;
			case FILE_NOT_SUPPORTED:
				code = -5;
				break;
			case FILE_NOT_STRUCTURED:
				code = -6;
				break;
			case COLUMN_NOT_EXISTS:
				code = -7;
				break;
			case OK:
				code = 3;
				break;
			case ERROR:
				code = -8;
				break;
			case INVALID_GENERIC_COLUMN_COLLECTION:
				code = -9;
				break;
			default:
				break;
			}
			return code;
		}

		/**
		 * Se esiste restituisce il messaggio di errore altrimenti la stringa
		 * vuota.
		 * 
		 * @return Se esiste restituisce il messaggio di errore altrimenti la
		 *         stringa vuota.
		 */
		public String getErrorMessage() {
			return errorMessage;
		}

		/**
		 * Imposta il messaggio di errore
		 * 
		 * @param message
		 *            il messaggio di errore.
		 */
		public void setErrorMessage(String message) {
			errorMessage = message;
		}
	}
	
	static {
		
		List<String> supportedList = new ArrayList<String>();
		supportedList.add("SHP");
		supportedList.add("CSV");
		supportedList.add("XLS");
		supportedList.add("XLSX");
		supportedList.add("DBF");
		supportedList.add("PDF");
		supportedList.add("MDB");
		supportedList.add("TXT");
		supportedList.add("KML");
		supportedList.add("KMZ");
		supportedFileFormats = Collections.unmodifiableList(supportedList);

		List<String> structuredList = new ArrayList<String>();
		structuredList.add("SHP");
		structuredList.add("CSV");
		structuredList.add("XLS");
		structuredList.add("XLSX");
		structuredList.add("DBF");
		structuredList.add("MDB");
		structuredList.add("TXT");
		structuredList.add("KML");
		structuredList.add("KMZ");
		structuredFileFormats = Collections.unmodifiableList(structuredList);
		
		try{
			String logConfigFile = WSEtl.readConfig("logConfigFile");
			DOMConfigurator.configureAndWatch(logConfigFile);
		}catch(Exception ex){
			//si perde il logging ma l'applicazione continua a funzionare
		}
		logger.info("Inizializzazione completata");
		
		try{
			new FileSystemXmlApplicationContext("webapps/WSOpenDataETL/applicationContext.xml");
		}catch(Exception ex){
			new FileSystemXmlApplicationContext("E:\\progetti\\Eclipse\\WSOpenDataETL\\ZZZproperties\\applicationContext.xml");					
		}
	}
	
	/**
	 * Dato l'id di un dataDim e il codice di una dimensione
	 * restituisce la descrizione associata
	 * @param dataDimId id della riga MD_DATA_DIM
	 * @param code codice della dimensione
	 * @return la descrizione della dimensione
	 */
	public String getDimDescription(int dataDimId, int code){
		logger.info("getDimDescription");
		try{
			MdDataDim mdDataDim = MdDataDim.findMdDataDim(dataDimId);
			if(mdDataDim == null){
				return code+"";
			}
			MdHierNode mdHierNode = mdDataDim.getIdHierNode();
			if(mdHierNode == null){
				return code+"";
			}
			List<Map<String, Object>> result = 
					DBUtils.executeQueryForList("SELECT "+mdHierNode.getDescField()+" FROM "+mdHierNode.getTableName()+" WHERE "+mdHierNode.getPkField()+" = "+ code);
			if(result.size() == 0){
				return code+"";
			}
			return result.get(0).get(mdHierNode.getDescField()).toString();
		}catch(Exception ex){
			log(ex);
			return code+"";
		}
	}
	
	/**
	 * Logga un'eccezione
	 * @param ex l'eccezione da loggare
	 */
	protected static void log(Throwable ex){
		//al livello più alto metto solo il messaggio
		logger.warn(ex.getMessage());
		//al livello più basso tutto lo stack trace
		logger.error(ex.getMessage(), ex);
	}
	
	/**
	 * Estrae le informazioni su un file che è stato caricato sul server. In
	 * particolare: - controlla se il file appartiene all'insieme dei tipi di
	 * file supportati; - controlla se il file è strutturato - nel caso lo sia
	 * estrae i nomi delle colonne
	 * 
	 * @param filePath
	 *            Il path del file da analizzare
	 * @return Un insieme di informazioni sul file (FileUploadInfo) serializzate
	 *         in JSON.
	 */
	public String extractInfo(String filePath, String extraArguments) {
		logger.info("extractInfo");
		FileUploadStatus status;		
		ColumnInfo[] columnNames = null;
		String fileType = "Unknown";
		try {
			File file = new File(filePath);
			// Per prima cosa controllo se il file esiste o meno
			if (file.exists()) {
				if(isZipFile(file)){
					file = extractFile(file);
				}
				fileType = getFileSuffix(file);
				// Poi controllo se il file è supportato
				if (isSupported(file)) {
					// Quindi controllo se il file può essere un file
					// strutturato
					if (canBeStructured(file)) {
						// esiste, è supportato e può essere strutturato.
						// devo provare a leggerne le colonne
						if(isKmzFile(file)){
							file = kmz2kml(file);
						}
						if(isKmlFile(file)){
							file = kml2shp(file);
						}						
						IFileReader reader = getReader(file);
						if (reader != null) {
							columnNames = reader
									.readColumnInfo(Boolean.valueOf(checkExtension(file, "shp")), extraArguments);
						}
						// tre possibilità:
						// non ho la struttura -> non siamo in grado di capirne
						// la struttura
						// ho la struttura ma non ci sono nomi -> ci è stato
						// passato un file vuoto
						// ho la struttura e i nomi -> posso restituire i nomi
						if (columnNames == null) {
							status = FileUploadStatus.INVALID_STRUCTURE;
						} else if (columnNames.length == 0) {
							status = FileUploadStatus.EMPTY;
						} else {
							HashSet<String> columnNameSet = new HashSet<String>();
							boolean duplicated = false;
							String duplicatedColumns = "";
							for(ColumnInfo columnInfo : columnNames){
								String columnName = columnInfo.getName().toUpperCase();
								if(columnNameSet.contains(columnName)){
									duplicated = true;
									duplicatedColumns += columnName+", ";
								}else{
									columnNameSet.add(columnName);
								}
							}
							if(duplicated){
								status = FileUploadStatus.DUPLICATED_COLUMN_NAMES;
								status.setErrorMessage("Colonne duplicate: "+duplicatedColumns.substring(0, duplicatedColumns.length()-2));
							}else{
								status = FileUploadStatus.OK;
							}
						}
					} else {
						status = FileUploadStatus.NOT_STRUCTURED;
					}
				} else {
					status = FileUploadStatus.NOT_SUPPORTED;
				}
			} else {
				status = FileUploadStatus.NOT_EXIST;
			}
		} catch (Throwable exception) {
			log(exception);
			status = FileUploadStatus.ERROR;
			status.setErrorMessage(exception.getMessage());
		}
		FileUploadInfo fileUploadInfo = null;
		if (columnNames == null) {
			fileUploadInfo = new FileUploadInfo(status.getCode(), status
					.getDescription().replace("{0}", filePath),fileType);
		} else {
			fileUploadInfo = new FileUploadInfo(status.getCode(), status
					.getDescription().replace("{0}", filePath), columnNames, fileType);
		}
		Gson gson = new Gson();
		return gson.toJson(fileUploadInfo);
	}

	/**
	 * Converte un file kmz in un file kml decomprimendo l'archivio
	 * @param kmzFile il file kmz da decomprimere
	 * @return un riferimento al file kml risultante
	 * @throws Exception 
	 */
	private File kmz2kml(File kmzFile) throws Exception {
		//directory in cui si trova il file kmz
		String kmzFileDirectory = kmzFile.getParent();
		//nome del file kmz al netto dell'estensione.
		String fileName = getFileName(kmzFile);
		//scompatto il kmz in {directory del kmz}/{nome del kmz}
		String unzipDirectoryName = kmzFileDirectory+File.separator+fileName;
		File unzipDirectory = new File(unzipDirectoryName);
		if(unzipDirectory.exists()){
			deleteFolder(unzipDirectory);
		}else{
			unzipDirectory.mkdir();
		}
		//scompatto il file
		unzip(kmzFile, unzipDirectory);
		String[] kmlFiles = unzipDirectory.list(kmlFilter);
		if(kmlFiles.length > 0){
			return new File(unzipDirectory+File.separator+unzipDirectory.list(kmlFilter)[0]);
		}else{
			throw new Exception("No kml files in kmz");
		}
	}

	/**
	 * Metodo ricorsivo per la cancellazione di una cartella
	 * @param folder la cartella da cancellare
	 */
	private void deleteFolder(File folder){
		for(File file : folder.listFiles()){
			if(file.isDirectory()){
				deleteFolder(file);
			}else{
				file.delete();
			}
		}
		folder.delete();
	}
	
	/**
	 * Converte un file kml in un file shp
	 * @param file il file kml da convertire
	 * @return un file shp
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	private File kml2shp(File kmlFile) throws IOException, InterruptedException {
		//il file viene convertito nella stessa directory in cui si trova
		String fileDirectory = kmlFile.getParent();
		//nome del file kml al netto dell'estensione.
		String fileName = getFileName(kmlFile);
		String convertDirectoryName = fileDirectory+File.separator+fileName;
		File convertDirectory = new File(convertDirectoryName);
		//se la directory esiste la ripulisco
		if(convertDirectory.exists()){
			deleteFolder(convertDirectory);
		}
		convertDirectory.mkdir();		
		String shapeFileName = convertDirectory+File.separator+fileName + ".shp";
		File temporaryFile = null;
		PrintWriter pw = null;
		try{
			//ogr2ogr -f 'ESRI Shapefile' directory/output.shp input.kml
			temporaryFile = new File(convertDirectory+File.separator+"converter.executable");
			pw = new PrintWriter(temporaryFile);			
			pw.println("ogr2ogr -f 'ESRI Shapefile' "+shapeFileName+" "+kmlFile.getAbsolutePath());
			pw.flush();
			pw.close();
			temporaryFile.setExecutable(true, false);
			Runtime run = Runtime.getRuntime();
			Process process = run.exec("./converter.executable", null, convertDirectory);
			StreamCatcher errorGobbler = new StreamCatcher(process.getErrorStream(), "KML_ERROR");
			StreamCatcher outputGobbler = new StreamCatcher(process.getInputStream(), "KML_OUTPUT");
			errorGobbler.start();
		    outputGobbler.start();
			process.waitFor();	
		}finally{
			if(pw != null){
				pw.close();
			}
			if(temporaryFile != null){
				temporaryFile.delete();
			}
		}
		return new File(shapeFileName);
	}

	/**
	 * Scompatta il file
	 * @param file il file da scompattare
	 * @return Il riferimento ad un file presente nell'archivio
	 */
	private File extractFile(File file) throws Exception{
		//controllo se è indicata una directory in cui scompattare
		String fileDirectory = WSEtl.readConfig("unzipDirectory");
		if(fileDirectory == null){
			//non c'è una directory in cui scompattare, uso quella in cui si trova il file
			fileDirectory = file.getParent();
		}
		//se non esiste già la cartella con il nome del file
		String fileName = getFileName(file);
		File directory = new File(fileDirectory+File.separator+fileName);
		//se la directory esiste la ripulisco
		if(directory.exists()){
			deleteFolder(directory);
		}
		//scompatto il file
		unzip(file, directory);
		//se tra i file scompattati c'è un shp
		String[] shpFiles = directory.list(shpFilter);
		if(shpFiles.length > 0){
			//controllo che ci siano gli altri file necessari e in quel caso uso l'shp
			String shpFileName = getFileName(shpFiles[0]);
			if(directory.list(new ExactFilter(shpFileName+".dbf")).length == 1 && 
			   directory.list(new ExactFilter(shpFileName+".shx")).length == 1 &&
			   directory.list(new ExactFilter(shpFileName+".prj")).length == 1){
				return new File(directory+File.separator+shpFiles[0]);
			}
		}
		if(directory.list(dbfFilter).length > 0){
			//altrimenti se c'è un dbf uso il dbf
			return new File(directory+File.separator+directory.list(dbfFilter)[0]);
		}
		if(directory.list(xlsxFilter).length > 0){
			//altrimenti se c'è un xlsx lo uso
			return new File(directory+File.separator+directory.list(xlsxFilter)[0]);
		}
		if(directory.list(xlsFilter).length > 0){
			//altrimenti se c'è un xlsx lo uso
			return new File(directory+File.separator+directory.list(xlsFilter)[0]);
		}
		if(directory.list(csvFilter).length > 0){
			//altrimenti se c'è un csv uso il csv
			return new File(directory+File.separator+directory.list(csvFilter)[0]);
		}
		if(directory.list(txtFilter).length > 0){
			return new File(directory+File.separator+directory.list(txtFilter)[0]);
		}
		if(directory.list(mdbFilter).length > 0){
			//altrimenti se c'è uso l'mdb
			return new File(directory+File.separator+directory.list(mdbFilter)[0]);
		}
		if(directory.list(kmlFilter).length > 0){
			//se c'è uso il kml
			return new File(directory+File.separator+directory.list(kmlFilter)[0]);
		}
		if(directory.list(kmzFilter).length > 0){
			//se c'è uso il kml
			return new File(directory+File.separator+directory.list(kmzFilter)[0]);
		}
		//altrimenti sollevo un'eccezione
		throw new Exception("Il pacchetto zip non contiene nessun file supportato");
	}

	/**
	 * Scompatta il file in una directory
	 * @param zipFile il file da scompattare
	 * @param folder la directory in cui scompattarlo
	 */
	private void unzip(File zipFile, File folder) throws IOException{
		byte[] buffer = new byte[1024]; 
    	if(!folder.exists()){
    		folder.mkdir();
    	}	
    	ZipInputStream zis = null;
    	try{
	    	zis = new ZipInputStream(new FileInputStream(zipFile));
	    	ZipEntry ze = zis.getNextEntry();	 
	    	while(ze!=null){
	    	   String fileName = ze.getName();
	           File newFile = new File(folder.getAbsoluteFile() + File.separator + fileName);
				if(ze.isDirectory()){
					newFile.mkdirs();
				}else{
					new File(newFile.getParent()).mkdirs();
					FileOutputStream fos = new FileOutputStream(newFile);             
					int len;
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}
					fos.close();   
				}
	           newFile.setReadable(true, false);
	           newFile.setWritable(true, false);
	           newFile.setExecutable(true, false);
	           ze = zis.getNextEntry();
	    	}
    	}finally{
	        if(zis != null){
	    		zis.closeEntry();
		    	zis.close();    
	    	}
    	}
	}

	/**
	 * Restituisce il nome del file senza la sua estensione
	 * @param file il file
	 * @return il nome del file senza la sua estensione
	 */
	private String getFileName(File file){
		String name = file.getName();
		return getFileName(name);

	}
	
	/**
	 * Restituisce il nome del file senza l'estensione
	 * @param name il nome completo del file
	 * @return il nome del file senza l'estensione
	 */
	private String getFileName(String name){
		String[] dotSplittedName = name.split("\\.");
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < dotSplittedName.length-1; i++){
			builder.append(dotSplittedName[i]);
		}
		return builder.toString();
	}

	/**
	 * Verifica che un file abbia una determinata estensione
	 * @param file il file
	 * @param extension l'estensione
	 * @return true se il file ha l'estensione specificata, false altrimenti
	 */
	private boolean checkExtension(File file, String extension){
		return getFileSuffix(file).equalsIgnoreCase(extension);
	}
	
	/**
	 * Verifica se il file passato come parametro è uno zip
	 * @param file il file da controllare
	 * @return true se è uno zip, false altrimenti
	 */
	private boolean isZipFile(File file) {
		return checkExtension(file, "ZIP");
	}

	/**
	 * Verifica se il file passato come parametro è un kml
	 * @param file il file da controllare
	 * @return true se è un kml, false altrimenti
	 */
	private boolean isKmlFile(File file){
		return checkExtension(file, "KML");
	}
	
	/**
	 * Verifica se il file passato come parametro è un kmz
	 * @param file il file da controllare
	 * @return true se è un kml, false altrimenti
	 */
	private boolean isKmzFile(File file){
		return checkExtension(file, "KMZ");
	}
	
	/**
	 * Verifica che una determinata colonna di un determinato file contenga solo
	 * valori univoci
	 * 
	 * @param filePath
	 *            il path del file
	 * @param columnName
	 *            il nome fisico della colonna
	 * @return Un oggetto che indica la riuscita o il fallimento della verifica
	 *         codificato in JSON
	 */
	public String verifyUniqueness(String filePath, String columnName, String extraArguments) {
		logger.info("verifyUniqueness");
		VerifyUniquenessStatus status;
		try {
			File file = new File(filePath);
			// Per prima cosa controllo se il file esiste o meno
			if (file.exists()) {
				if(isZipFile(file)){
					file = extractFile(file);
				}
				// Poi controllo se il file è supportato
				if (isSupported(file)) {
					// Quindi controllo se il file può essere un file
					// strutturato
					if (canBeStructured(file)) {
						if(isKmzFile(file)){
							file = kmz2kml(file);
						}
						if(isKmlFile(file)){
							file = kml2shp(file);
						}	
						IFileReader reader = getReader(file);
						if (reader == null) {
							status = VerifyUniquenessStatus.FILE_NOT_SUPPORTED;
						} else {
							status = reader.verifyUniqueness(columnName, extraArguments);
						}
					} else {
						status = VerifyUniquenessStatus.FILE_NOT_STRUCTURED;
					}
				} else {
					status = VerifyUniquenessStatus.FILE_NOT_SUPPORTED;
				}
			} else {
				status = VerifyUniquenessStatus.FILE_NOT_EXISTS;
			}
		} catch (Exception exception) {
			log(exception);
			status = VerifyUniquenessStatus.ERROR;
			status.setErrorMessage(exception.getMessage());
		}
		VerifyUniquenessInfo verifyUniquenessInfo = new VerifyUniquenessInfo(
				status.getCode(), status.getDescription()
						.replace("{0}", filePath).replace("{1}", columnName));
		Gson gson = new Gson();
		return gson.toJson(verifyUniquenessInfo);
	}

	/**
	 * Verifica che il db utilizzato sia quello passato come parametro.
	 * 
	 * @return 1 se il db utilizzato è quello passato come parametro, -1
	 *         altrimenti.
	 */
	public int checkConfiguration(String name) {
		logger.info("checkConfiguration");
		return (DBUtils.getDatabaseName().equals(name)) ? 1 : -1;
	}

	/**
	 * Scorre le righe del file e verifica che:
	 * 1) le colonne indicate come dimensioni contengano solo valori validi per il nodo a cui fanno riferimento
	 * 2) le colonne indicate come misure contengano solo valori numerici o nulli
	 * @param filePath il path del file
	 * @param dimensionInfoCollection l'insieme delle dimensioni serializzato in json
	 * @param measureInfoCollection l'insieme delle misure serializzato in json
	 * @param genericColumnInfoCollection l'insieme delle colonne generiche serializzato in json
	 * @param extraArguments eventuali parametri aggiunti (es. il separatore per i csv)
	 * @return la serializzazione json del risultato della verifica
	 * @throws Exception 
	 */
	public String getWarnings(String filePath, String dimensionInfoCollection, String measureInfoCollection, String genericColumnInfoCollection, String extraArguments){
		logger.info("getWarnings");
//		try{
//			new FileSystemXmlApplicationContext("webapps/WSOpenDataETL/applicationContext.xml");
//		}catch(Exception ex){
//			new FileSystemXmlApplicationContext("E:\\progetti\\Eclipse\\WSOpenDataETL\\ZZZproperties\\applicationContext.xml");					
//		}
		Gson gson = new Gson();
		try{			
			File file = new File(filePath);
			WarningSet warningSet = null;
			// Per prima cosa controllo se il file esiste o meno
			if (file.exists()) {
				if(isZipFile(file)){
					file = extractFile(file);
				}
				if(isKmzFile(file)){
					file = kmz2kml(file);
				}
				if(isKmlFile(file)){
					file = kml2shp(file);
				}	
				//ottengo il reader per il file
				IFileReader reader = getReader(file);
				//deserializzo le dimensioni
				DimensionInfo[] dimensionInfo = new DimensionInfo[0];
				if (dimensionInfoCollection != null) {
					dimensionInfo = gson.fromJson(dimensionInfoCollection, DimensionInfo[].class);
				}
				//deserializzo le misure
				MeasureInfo[] measureInfo = new MeasureInfo[0];
				if( measureInfoCollection != null){
					measureInfo = gson.fromJson(measureInfoCollection, MeasureInfo[].class);
				}
				//deserializzo le colonne generiche
				GenericColumnInfo[] genericColumnInfo = new GenericColumnInfo[0];
				if(genericColumnInfoCollection != null){
					genericColumnInfo = gson.fromJson(genericColumnInfoCollection, GenericColumnInfo[].class);
				}
				//chiedo al reader di fare un'elenco dei warning
				warningSet = reader.getWarnings(dimensionInfo, measureInfo, genericColumnInfo, extraArguments);
			}else{
				warningSet = new WarningSet(true, new Warning[0]);
			}
			//serializzo e restituisco l'elenco
			if(warningSet.getCanForceImport() == true){
				warningSet.setCanForceImport(!checkExtension(file, "shp"));
			}
			return gson.toJson(warningSet, WarningSet.class);
		}catch(Exception ex){
			logger.error(ex.getMessage(), ex);
			return "";
		}
	}
	
	/**
	 * Importa un dato nel sistema. 
	 * 1) Importa il contenuto del file in una tabella 
	 * 2) Crea il dato 
	 * 3) Eventualmente crea le dimensioni del dato 
	 * 4) Eventualmente crea le misure del dato 
	 * 5) Eventualmente crea le colonne generiche del dato
	 * @param filePath Il path del file da importare
	 * @param dataInfo Le informazioni sul dato codificate in JSON
	 * @param dimensionInfoCollection Un insieme di informazioni sulle dimensioni del dato codificate in JSON
	 * @param measureInfoCollection Un insieme di informazioni sulle misure del dato codificate in JSON
	 * @param genericColumnInfoCollection Un insieme di informazioni sulle colonne generiche codificate in JSON
	 * @return
	 * @throws Exception
	 */
	public String importData(String filePath, String dataInfo, String dimensionInfoCollection, String measureInfoCollection, String genericColumnInfoCollection, String extraArguments) {
		logger.info("importData");
		ImportDataStatus status;
		Gson gson = new Gson();
		try {

//			try{
//				new FileSystemXmlApplicationContext("webapps/WSOpenDataETL/applicationContext.xml");
//			}catch(Exception ex){
//				new FileSystemXmlApplicationContext("E:\\progetti\\Eclipse\\WSOpenDataETL\\ZZZproperties\\applicationContext.xml");					
//			}
			boolean isKmz = false;
			boolean isKml = false;
			String pathToSave = filePath;
			IFileReader reader = null;
			File file = null;
			// controllo che il file indicato esista e possa essere letto
			try {
				file = new File(filePath);
				// Per prima cosa controllo se il file esiste o meno
				if (file.exists()) {
					if(isZipFile(file)){
						file = extractFile(file);
					}
					// Poi controllo se il file è supportato
					if (isSupported(file)) {
						// Quindi controllo se il file può essere un file
						// strutturato
						if (canBeStructured(file)) {
							if(isKmzFile(file)){
								pathToSave = file.getAbsolutePath();
								file = kmz2kml(file);
								isKmz = true;								
							}
							if(isKmlFile(file)){
								if(!isKmz){
									pathToSave = file.getAbsolutePath();
								}
								file = kml2shp(file);
								isKml = !isKmz && true;								
							}	
							reader = getReader(file);
							if (reader == null) {
								status = ImportDataStatus.FILE_NOT_SUPPORTED;
								return gson.toJson(new ImportDataInfo(status.getCode(), status.getDescription()));
							}
						} else {
							status = ImportDataStatus.FILE_NOT_STRUCTURED;
							return gson.toJson(new ImportDataInfo(status.getCode(), status.getDescription()));
						}
					} else {
						status = ImportDataStatus.FILE_NOT_SUPPORTED;
						return gson.toJson(new ImportDataInfo(status.getCode(), status.getDescription()));
					}
				} else {
					status = ImportDataStatus.FILE_NOT_EXISTS;
					return gson.toJson(new ImportDataInfo(status.getCode(), status.getDescription()));
				}
			} catch (Exception exception) {
				log(exception);
				status = ImportDataStatus.ERROR;
				status.setErrorMessage(exception.getMessage());
				return gson.toJson(new ImportDataInfo(status.getCode(), status.getDescription()));
			}

			// deserializzo le informazioni sul dato senza le quali non è
			// possibile creare un dato
			DataInfo dataInformation = deserializeDataInfo(dataInfo);
			if (dataInformation == null) {
				status = ImportDataStatus.INVALID_DATA_INFO;
				return gson.toJson(new ImportDataInfo(status.getCode(), status.getDescription()));
			}

			// se l'utente ha impostato delle dimensioni bisogna verificare che
			// i valori corrispondano a quelli del nodo
			DimensionInfo[] dimensionInfo = new DimensionInfo[0];
			if (dimensionInfoCollection != null) {
				try {
					dimensionInfo = gson.fromJson(dimensionInfoCollection, DimensionInfo[].class);
				} catch (JsonSyntaxException jse) {
					log(jse);
					status = ImportDataStatus.INVALID_DIMENSION_COLLECTION;
					return gson.toJson(new ImportDataInfo(status.getCode(),status.getDescription()));
				}
			}

			// poi se ci sono delle misure bisogna verificare che i valori siano
			// tutti numerici o null
			MeasureInfo[] measureInfo = new MeasureInfo[0];
			if (measureInfoCollection != null) {
				try {
					measureInfo = gson.fromJson(measureInfoCollection, MeasureInfo[].class);
				} catch (JsonSyntaxException jse) {
					log(jse);
					status = ImportDataStatus.INVALID_MEASURE_COLLECTION;
					return gson.toJson(new ImportDataInfo(status.getCode(), status.getDescription()));
				}
			}

			// infine bisogna calcolare la cardinalità delle colonne generiche
			GenericColumnInfo[] genericColumnInfo = new GenericColumnInfo[0];
			if (genericColumnInfoCollection != null) {
				try {
					genericColumnInfo = gson.fromJson(genericColumnInfoCollection, GenericColumnInfo[].class);
				} catch (JsonSyntaxException jse) {
					log(jse);
					status = ImportDataStatus.INVALID_GENERIC_COLUMN_COLLECTION;
					return gson.toJson(new ImportDataInfo(status.getCode(), status.getDescription()));
				}
				if (genericColumnInfo.length > 0) {
					for (int i = 0; i < genericColumnInfo.length; i++) {
						genericColumnInfo[i].setCardinality(reader.getCardinality(genericColumnInfo[i].getColumnName(), ""));
					}
				}
			}

			//controlliamo che l'utente abbia chiesto di creare un dato con almeno una colonna
			if (!isKml && !isKmz &&(dimensionInfo.length + measureInfo.length + genericColumnInfo.length == 0)) {
				status = ImportDataStatus.INVALID_INPUT;
				return gson.toJson(new ImportDataInfo(status.getCode(), status.getDescription()));
			}

			// a questo punto posso importare il contenuto del file in una
			// tabella
			int idLuDataType = 3;
			TableInfo tableInfo = null;
			LinkedList<IImportColumnInfo> columnList = buildColumnList(dimensionInfo, measureInfo, genericColumnInfo);
			try {
				if (dataInformation.getTableName() == null || dataInformation.getTableName().equals("")) {
					tableInfo = reader.writeTable(columnList, extraArguments);
					if(isKml || isKmz){
						idLuDataType = 4;
					} else if(checkExtension(file,"shp") || tableInfo.getIsGeometry()){
						idLuDataType = 1;
						//se era uno shape puntuale ho due nuove colonne da considerare
						Pair<String, String> addedGeometryColumns = tableInfo.getAddedGeometryColumns();
						if(addedGeometryColumns != null){
							String latitude = addedGeometryColumns.getFirst();
							String longitude = addedGeometryColumns.getSecond();
							GenericColumnInfo[] newGenericColumnInfo = new GenericColumnInfo[genericColumnInfo.length+2];
							for(int i = 0; i < genericColumnInfo.length; i++){
								newGenericColumnInfo[i] = genericColumnInfo[i];
							}
							newGenericColumnInfo[newGenericColumnInfo.length-2] = new GenericColumnInfo(latitude, "DO_Y", false, true, false);
							newGenericColumnInfo[newGenericColumnInfo.length-1] = new GenericColumnInfo(longitude, "DO_X", false, false, true);
							genericColumnInfo = newGenericColumnInfo;
						}
					}
				} else {
					tableInfo = new TableInfo(dataInformation.getTableName(), DBUtils.getDatabaseName(), reader.count(""), null, reader.getOriginalNames(columnList), true);
					idLuDataType = 1;
				}
			} catch (Exception e) {
				log(e);
				status = ImportDataStatus.ERROR;
				status.setErrorMessage(e.getMessage());
				return gson.toJson(new ImportDataInfo(status.getCode(), status.getDescription()));
			}

			// poi creo il dato
			if (!isKml && !isKmz && filePath.toUpperCase().endsWith(".SHP")) {
				idLuDataType = 1;
			}
			int dataId = createData(true, tableInfo.getDbName(),
					dataInformation.getDescription(), true, idLuDataType, null,
					null, new Date(), dataInformation.getName(),
					tableInfo.getNumRows(), tableInfo.getTableName());
			MdData data = MdData.findMdData(dataId);
			try {
				tableInfo.setColumns(data.getColumns());
			} catch (OdataDomainException e) {
				log(e);
				data.remove();
				status = ImportDataStatus.ERROR;
				status.setErrorMessage(e.getMessage());
				return gson.toJson(new ImportDataInfo(status.getCode(), status.getDescription()));
			}
			if(isKml || isKmz){
				MdDataFilesPK dataFilePK = new MdDataFilesPK(dataId, pathToSave);
				MdDataFiles dataFile = new MdDataFiles();
				dataFile.setId(dataFilePK);
				dataFile.persist();
			}
			boolean map = (idLuDataType == 1 || idLuDataType == 4);
			// ci aggiungo le eventuali dimensioni
			for (int i = 0; i < dimensionInfo.length; i++) {
				createDimension(dataId, dimensionInfo[i].getAlias(),
						dimensionInfo[i].getDescription(),
						tableInfo.getColumn(dimensionInfo[i].getColumnName()).getPhysicalName(),
						dimensionInfo[i].getIdHierNode());
				MdHierNode node = MdHierNode.findMdHierNode(dimensionInfo[i].getIdHierNode());
				if (node.getIdHierarchy().getIdLuHierType().intValue() == 1) {
					map = true;
				}
			}
			// ci aggiungo le eventuali misure
			for (int i = 0; i < measureInfo.length; i++) {
				createMeasure(dataId, measureInfo[i].getAlias(),
						measureInfo[i].getDescription(),
						tableInfo.getColumn(measureInfo[i].getColumnName()).getPhysicalName(),
						measureInfo[i].getDecimals(),
						measureInfo[i].getMeasureUnit(), i);
			}
			// ci aggiungo le eventuali colonne generiche
			for (int i = 0; i < genericColumnInfo.length; i++) {
				createGenericColumn(dataId, tableInfo.getColumn(genericColumnInfo[i].getColumnName()).getPhysicalName(),
						genericColumnInfo[i].getAlias(),
						genericColumnInfo[i].getCardinality(),
						genericColumnInfo[i].getDescriptiveField());
			}
			data = MdData.findMdData(dataId);
			data.updateDifferentDistinctCount();
			List<String> visualizationFormats = new ArrayList<String>();
			if(!isKml && !isKmz){
				visualizationFormats.add("Table");
			}
			if(!isKml && !isKmz && (measureInfo.length != 0 || !IsDifferentDistinctCountOne(data.getColumns()))){
				visualizationFormats.add("Chart");
			}
			if (map) {
				visualizationFormats.add("Map2D");
			}
			if (isKml || isKmz || (idLuDataType == 1 && reader.count("") < 1000)) {
				visualizationFormats.add("Map3D");
			}
			status = ImportDataStatus.OK;
			String odataLink = readConfig("odataLink");
			String myName = readConfig("myName");
			if (odataLink != null) {
				odataLink = odataLink.replace("{0}", data.getUid() + "@" + myName);
			}
			return gson.toJson(new ImportDataInfo(status.getCode(), status.getDescription(), visualizationFormats.toArray(new String[0]), idLuDataType == 1,
					dimensionInfo.length > 0, odataLink, data.getUid()));
		} catch (Throwable t) {
			log(t);
			status = ImportDataStatus.ERROR;
			status.setErrorMessage(t.getMessage());
			return gson.toJson(new ImportDataInfo(status.getCode(), status.getDescription()));
		}
	}

	/**
	 * Restituisce true se tutte le colonne della lista hanno differentDistinctCount a 1 o null
	 * @param columns un elenco di colonne
	 * @return true se tutte le colonne hanno differentDistinctCount uguale a 1 o a null
	 */
	protected boolean IsDifferentDistinctCountOne(List<Column> columns){
		for(Column column : columns) {
			Integer differentDistinctCount = column.getDifferentDistinctCount();
			if(differentDistinctCount != null && differentDistinctCount != 1){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Cancella un dato con tutte le righe associate 
	 * (dimensioni, misure e colonne generiche)
	 * @param dataId l'id del dato da cancellare
	 */
	public DeleteDataResult deleteData(String dataUid){
		logger.info("deleteData");
		try{
//			try{
//				new FileSystemXmlApplicationContext("webapps/WSOpenDataETL/applicationContext.xml");
//			}catch(Exception ex){
//				new FileSystemXmlApplicationContext("E:\\progetti\\Eclipse\\WSOpenDataETL\\ZZZproperties\\applicationContext.xml");					
//			}
			int dataId = MdData.getIdFromUid(dataUid);
			MdData data = MdData.findMdData(dataId);
			String tableName = data.getTableName();
			if(data != null){
				for(MdDataDim dataDim : data.getMdDataDims()){
					data.removeDimension(dataDim.getId());
				}
				for(MdMeasureFields measureField : data.getMdMeasureFieldss()){
					data.removeMeasure(measureField.getId());
				}
				for(MdGenericColumn genericColumn : data.getMdGenericColumns()){
					data.removeGenericColumn(genericColumn.getId());
				}
				data.remove();
				deleteTable(tableName);
			}
			return new DeleteDataResult(true, "");
		}catch(Exception ex){
			log(ex);
			return new DeleteDataResult(false, ex.getMessage());
		}
	}
	
	/**
	 * Cancella una tabella
	 * @param tableName il nome della tabella da cancellare
	 */
	protected void deleteTable(String tableName){
		DBUtils.dropTable(tableName);
	}
	
	/**
	 * Restituisce l'insieme di alias.
	 * @param dataUid l'uid del dato di cui si vogliono sapere gli alias delle colonne
	 * @return l'insieme degli alias per un dato serializzato in json
	 */
	public String getAliases(String dataUid){
		logger.info("getAliases");


		//per la federazione
		String myName = readConfig("myName");
		if(myName == null){
			logger.warn("Parametro myName non presente nel file di configurazione");
			return "";
		}
		
		String[] splittedUid = dataUid.split("@");
		try{
				
			if(splittedUid.length == 2 && !splittedUid[1].equals(myName)){
				//fare la richiesta al ws esterno
				String wsAddress = getProxyWSAddress(splittedUid[1]);
				if(wsAddress.equals("")){
					logger.warn("Indirizzo del ws da contattare non presente nel file di configurazione");
					return "";
				}
				logger.warn("Chiamata al ws "+wsAddress);
				WSEtlProxy proxy = new WSEtlProxy(wsAddress);
				try {
					String result =proxy.getAliases(splittedUid[0]);
					logger.info(result);
					return result;
				} catch (RemoteException e) {
					logger.error(e.getMessage(), e);
					return "";
				}
			}
		}catch(Throwable e){
			logger.error(e.getMessage(), e);
			return "";
		}
		
		String uid = splittedUid[0];
		int dataId = MdData.getIdFromUid(uid);
		MdData data = MdData.findMdData(dataId);
		AliasSet aliasSet = new AliasSet(new Alias[0]);
		if(data != null){
			HashMap<String,SimpleColumnInfo> dbTypes = DBUtils.getDbTypeColumnInfo(data.getTableName());
			Set<MdDataDim> dataDims = data.getMdDataDims();
			Set<MdMeasureFields> measureFields = data.getMdMeasureFieldss();
			Set<MdGenericColumn> genericColumns = data.getMdGenericColumns();
			Alias[] aliases = new Alias[dataDims.size() + measureFields.size() + genericColumns.size()];
			int index = 0;
			for(MdDataDim dataDim : dataDims){
				aliases[index] = new Alias(dataDim.getId(), ColumnType.DIMENSION, dataDim.getAlias(), dataDim.getDimcodeField(), ColumnInfo.ColumnType.STRING, 0);
				index++;
			}
			for(MdMeasureFields measureField : measureFields){
				String type = dbTypes.get(measureField.getMeasureField()).columnType;
				ColumnInfo.ColumnType columnType = getColumnType(type);
				int	decimals = getDecimals(measureField, dbTypes.get(measureField.getMeasureField()).decimals, columnType);
				aliases[index] = new Alias(measureField.getId(), ColumnType.MEASURE, measureField.getAlias(), measureField.getMeasureField(), columnType, decimals);
				index++;
			}
			for(MdGenericColumn genericColumn : genericColumns){
				String type = dbTypes.get(genericColumn.getColumnField()).columnType;
				ColumnInfo.ColumnType columnType = getColumnType(type);
				aliases[index] = new Alias(genericColumn.getId(), ColumnType.GENERIC_COLUMN, genericColumn.getAlias(), genericColumn.getColumnField(), columnType, 0);
				index++;
			}
			aliasSet = new AliasSet(aliases);
		}
		Gson gson = new Gson();
		return gson.toJson(aliasSet);
	}
	
	/**
	 * Recupera l'indirizzo del web service per l'entità indicata
	 * @param name nome dell'entità federata
	 * @return l'indirizzo del web service per l'entità indicata
	 */
	private String getProxyWSAddress(String name){
		List<Map<String,Object>> result = DBUtils.executeQueryForList("select wsaddress from proxy_map where name like '"+name+"'");
		if(result.size() < 1){
			return "";
		}else{
			return (String) result.get(0).get("wsaddress");
		}
	}
	
	/**
	 * Restituisce il numero di decimali per una misura
	 * @param measureField la misura
	 * @param dbDecimals il numero di decimali impostato nel db
	 * @param columnType il tipo della colonna
	 * @return il numero di decimali per una misura
	 */
	private int getDecimals(MdMeasureFields measureField, Integer dbDecimals, ColumnInfo.ColumnType columnType) {
		Short decimalPlaces = measureField.getDecimalPlaces();
		if(decimalPlaces == null){
			if(columnType == ColumnInfo.ColumnType.FLOAT){
				if(dbDecimals != null){
					return dbDecimals;
				}else{
					return -1;
				}
			}else{
				return -1;
			}
		}else{
			return decimalPlaces;
		}
	}

	/**
	 * Restituisce il tipo di una colonna dato il suo tipo nel db
	 * @param type il tipo della colonna nel db
	 * @return il mapping del tipo in un enumerato 
	 */
	private ColumnInfo.ColumnType getColumnType(String type) {
		if(type.contains("char") || type.equalsIgnoreCase("text")){
			return ColumnInfo.ColumnType.STRING;
		}
		if(type.equalsIgnoreCase("bigint") ||
		   type.equalsIgnoreCase("bigserial") ||
		   type.equalsIgnoreCase("int8") ||
		   type.equalsIgnoreCase("serial8") ||
		   type.equalsIgnoreCase("integer") ||
		   type.equalsIgnoreCase("int") ||
		   type.equalsIgnoreCase("int4") ||
		   type.equalsIgnoreCase("smallint") ||
		   type.equalsIgnoreCase("int2") ||
		   type.equalsIgnoreCase("serial") ||
		   type.equalsIgnoreCase("serial4")
		   ){
			return ColumnInfo.ColumnType.NUMBER;
		}
		if( type.equalsIgnoreCase("float8") ||
		    type.equalsIgnoreCase("double precision") ||
		    type.toUpperCase().startsWith("NUMERIC") ||
		    type.toUpperCase().startsWith("DECIMAL") ||
		    type.equalsIgnoreCase("real") ||
		    type.equalsIgnoreCase("float4")
		    ){
			return ColumnInfo.ColumnType.FLOAT;
		}
		return ColumnInfo.ColumnType.UNKNOWN;
	}

	/**
	 * Imposta l'insieme degli alias per un dato
	 * @param dataUid l'uid del dato
	 * @param aliases il nuovo insieme di alias serializzato in json
	 * @throws OdataDomainException 
	 */
	public boolean setAliases(String dataUid, String aliases){
		logger.info("setAliases");
		//new FileSystemXmlApplicationContext("C:\\Users\\m.tiberio\\st.statportalopen_workspace\\WSOpenDataETL\\ZZZproperties\\applicationContext.xml");
//		try{
//			new FileSystemXmlApplicationContext("webapps/WSOpenDataETL/applicationContext.xml");
//		}catch(Exception ex){
//			new FileSystemXmlApplicationContext("E:\\progetti\\Eclipse\\WSOpenDataETL\\ZZZproperties\\applicationContext.xml");
//		}		
		try{
			Gson gson = new Gson();
			AliasSet aliasSet = gson.fromJson(aliases, AliasSet.class);
			if(aliasSet.hasDuplicatedAliases()){
				return false;
			}
			int dataId = MdData.getIdFromUid(dataUid);
			MdData data = MdData.findMdData(dataId);
			if(data != null){
				for(Alias alias : aliasSet.getAliases()){
					int id = alias.getId();
					String value = alias.getAlias();
					switch(alias.getType()){
						case DIMENSION:
							MdDataDim dataDim = data.getMdDataDim(id);	
							if(dataDim != null && !dataDim.getAlias().equals(value)){
								dataDim.setAlias(value);
								dataDim.merge();	
							}
							break;
						case MEASURE:
							MdMeasureFields measureField = data.getMdMeasureField(id);	
							if(measureField != null && !measureField.getAlias().equals(value)){
								measureField.setAlias(value);
								measureField.merge();
							}
							break;
						case GENERIC_COLUMN:
							MdGenericColumn genericColumn = data.getMdGenericColumn(id);
							if(genericColumn != null && !genericColumn.getAlias().equals(value)){
								genericColumn.setAlias(value);
								genericColumn.merge();
							}
							break;
					}
				}
			}
			return true;
		}catch(Exception ex){
			log(ex);
			return false;
		}
	}
	
	/**
	 * Crea una lista di colonne ordinata in base al campo pos
	 * 
	 * @param dimensionInfo
	 *            le dimensioni
	 * @param measureInfo
	 *            le misure
	 * @param genericColumnInfo
	 *            le colonne generiche
	 * @return
	 * @throws Exception 
	 */
	private LinkedList<IImportColumnInfo> buildColumnList(
			DimensionInfo[] dimensionInfo, MeasureInfo[] measureInfo,
			GenericColumnInfo[] genericColumnInfo) throws Exception {
		LinkedList<IImportColumnInfo> columnList = new LinkedList<IImportColumnInfo>();
		for (int i = 0; i < dimensionInfo.length; i++) {
			add(dimensionInfo[i], columnList);
		}
		for (int i = 0; i < measureInfo.length; i++) {
			add(measureInfo[i], columnList);
		}
		for (int i = 0; i < genericColumnInfo.length; i++) {
			add(genericColumnInfo[i], columnList);
		}
		return columnList;
	}

	/**
	 * Aggiunge una colonna alla lista
	 * 
	 * @param column
	 *            la colonna da aggiungere
	 * @param list
	 *            la lista a cui aggiungerla
	 * @throws Exception 
	 */
	private void add(IImportColumnInfo column,
			LinkedList<IImportColumnInfo> list) throws Exception {
		String columnName = column.getColumnName();
		for(IImportColumnInfo insertedColumn : list){
			if(insertedColumn.getColumnName().equalsIgnoreCase(columnName)){
				throw new Exception("Nome di colonna duplicato: "+columnName);
			}
		}
		int index = binarySearch(list, column.getPos(), 0, list.size());
		list.add(index, column);
	}

	/**
	 * Individua l'indice in cui inserire un elemento in una lista
	 * 
	 * @param list
	 * @param pos
	 * @param initialPosition
	 * @param finalPosition
	 * @return
	 */
	private int binarySearch(LinkedList<IImportColumnInfo> list, int pos,
			int initialPosition, int finalPosition) {
		if (list.size() == 0) {
			return 0;
		} else {
			if (initialPosition == finalPosition) {
				return initialPosition;
			} else {
				int newIndex = (finalPosition + initialPosition) / 2;
				if (list.get(newIndex).getPos() > pos) {
					return binarySearch(list, pos, initialPosition, newIndex);
				} else {
					return binarySearch(list, pos, newIndex + 1, finalPosition);
				}
			}
		}
	}

	/**
	 * Legge una chiave dal file di configurazione
	 * 
	 * @param key
	 *            il nome della chiave
	 * @return il valore della chiave se è presente, null altrimenti
	 */
	public static String readConfig(String key) {
		BufferedReader reader = null;
		try {
			try{
				reader = new BufferedReader(new java.io.FileReader(
					"webapps/WSOpenDataETL/META-INF/config.txt"));
			}catch(Exception ex){
				reader = new BufferedReader(new java.io.FileReader("E:\\progetti\\Eclipse\\WSOpenDataETL\\WebContent\\META-INF\\config.txt"));
			}	
			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] splittedLine = line.split("\t");
				if (splittedLine.length == 2) {
					if (splittedLine[0].equals(key)) {
						return splittedLine[1];
					}
				}
			}
			return null;
		} catch (Exception ex) {
			log(ex);
			return null;
		} finally{
			if(reader != null){
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Restituisce il massimo numero di record da leggere quando si 
	 * scorre un file per inferire il tipo delle colonne.
	 * Il massimo numero di record da leggere può venire specificato nel file
	 * di configurazione con la chiave maxScanningRecords.
	 * Se non viene specificato è 100.
	 * @return il massimo numero di record da leggere
	 */
	public static int getMaxScanningRecords(){
		int maxScanningRecords = 100;
		try{
			maxScanningRecords = Integer.parseInt(readConfig("maxScanningRecords"));
		}catch(Exception ex){
			log(ex);
		}
		return maxScanningRecords;
	}
	
	/**
	 * Restituisce il massimo numero di record da leggere quando si 
	 * scorre un file per verificare la dimensione associata a una colonna.
	 * Il massimo numero di record da leggere può venire specificato nel file
	 * di configurazione con la chiave maxVerifyRecords.
	 * Se non viene specificato è 10.
	 * @return il massimo numero di record da leggere
	 */
	public static int getMaxVerifyRecords(){
		int maxVerifyRecords = 10;
		try{
			maxVerifyRecords = Integer.parseInt(readConfig("maxVerifyRecords"));
		}catch(Exception ex){
			log(ex);
		}
		return maxVerifyRecords;
	}
	
	/**
	 * Deserializza un dataInfo da JSON a oggetto
	 * 
	 * @param dataInfo
	 *            la stringa json da deserializzare
	 * @return l'oggetto dataInfo
	 */
	private DataInfo deserializeDataInfo(String dataInfo) {
		Gson gson = new Gson();
		if (dataInfo == null) {
			return null;
		} else {
			try {
				return gson.fromJson(dataInfo, DataInfo.class);
			} catch (JsonSyntaxException jse) {
				log(jse);
				return null;
			}
		}
	}

	/**
	 * Crea un reader su un file. Il reader viene creato tenendo conto del tipo
	 * di file passato come parametro.
	 * 
	 * @param file
	 *            Un file su cui aprire un reader
	 * @return Un reader per quel tipo di file
	 */
	private IFileReader getReader(File file) {
		String suffix = getFileSuffix(file).toUpperCase();
		IFileReader reader = null;
		if (suffix.equalsIgnoreCase("DBF")) {
			reader = new DbfFileReader(file);
		} else if (suffix.equalsIgnoreCase("SHP")) {
			reader = new DbfFileReader(file);
		} else if (suffix.equalsIgnoreCase("XLS")) {
			reader = new XlsFileReader(file);
		} else if (suffix.equalsIgnoreCase("XLSX")) {
			reader = new XlsxFileReader(file);
		} else if (suffix.equalsIgnoreCase("CSV") || suffix.equalsIgnoreCase("TXT")) {
			reader = new CsvFileReader(file);
		} else if (suffix.equalsIgnoreCase("MDB")) {
			reader = new MdbFileReader(file);
		}
		return reader;
	}

	/**
	 * Calcola e restituisce il suffisso di un file
	 * 
	 * @param file
	 *            Il file di cui calcolare il suffisso
	 * @return Il suffisso del file
	 */
	private String getFileSuffix(File file) {
		String suffix = "";
		String fileName = file.getName();
		int suffixSeparatorIndex = fileName.lastIndexOf('.');
		if (suffixSeparatorIndex > 0) {
			suffix = fileName.substring(suffixSeparatorIndex + 1);
		}
		return suffix;
	}

	/**
	 * Legge il suffisso del file e lo confronta con l'insieme dei formati da
	 * cui si può cercare di estrarre la struttura.
	 * 
	 * @param file
	 *            il file caricato sul server
	 * @return True se il file può essere un file strutturato, false altrimenti
	 */
	private boolean canBeStructured(File file) {
		boolean canBeStructured = false;
		if (structuredFileFormats.contains(getFileSuffix(file).toUpperCase())) {
			canBeStructured = true;
		}
		return canBeStructured;
	}

	/**
	 * Legge il suffisso del file e lo confronta con l'insieme dei formati
	 * supportati
	 * 
	 * @param file
	 *            il file caricato sul server
	 * @return True se il tipo di file è supportato, false altrimenti
	 */
	private boolean isSupported(File file) {
		// non faccio un controllo sul null per filePath perchè questo
		// è un metodo privato e mi accerterò di chiamarlo solo con file validi
		boolean supported = false;
		if (supportedFileFormats.contains(getFileSuffix(file).toUpperCase())) {
			supported = true;
		}
		return supported;
	}

	/**
	 * Crea una nuova riga di MdData
	 * 
	 * @param available
	 *            flag che indica se il dato sarà disponibile
	 * @param dbName
	 *            nome del database
	 * @param description
	 *            descrizione del dato
	 * @param genericGrants
	 *            flag che indica se il dato ha diritti generici
	 * @param idLuDataType
	 *            tipo di dato
	 * @param idMetadata
	 *            id della scheda
	 * @param idOwnerUser
	 *            id dell'utente
	 * @param lastUpdate
	 *            data di ultimo aggiornamento
	 * @param name
	 *            nome del dato
	 * @param numRows
	 *            numero di righe del dato
	 * @param tableName
	 *            nome della tabella del dato
	 * @return l'id del nuovo dato creato
	 */
	private int createData(Boolean available, String dbName,
			String description, Boolean genericGrants, Integer idLuDataType,
			Integer idMetadata, Integer idOwnerUser, Date lastUpdate,
			String name, Integer numRows, String tableName) {
		MdData newData = new MdData(available, dbName, description,
				genericGrants, idLuDataType, idMetadata, idOwnerUser,
				lastUpdate, name, numRows, tableName);
		newData.persist();
		return newData.getId();
	}

	/**
	 * Crea una nuova dimensione di un dato (riga di MdDataDim)
	 * 
	 * @param dataId
	 *            id del dato a cui appartiene la dimensione
	 * @param alias
	 *            alias della dimensione
	 * @param description
	 *            descrizione della dimensione
	 * @param dimcodeField
	 *            nome fisico della colonna della dimensione
	 * @param idHierNode
	 *            id del nodo a cui associare la dimensione
	 * @return Il nuovo id associato alla nuova riga di MdDataDim
	 * @throws OdataDomainException 
	 */
	private int createDimension(int dataId, String alias, String description,
			String dimcodeField, int idHierNode) throws OdataDomainException {
		MdData data = MdData.findMdData(dataId);
		if (data == null) {
			return -1;
		}
		return data.addDimension(alias, description, dimcodeField, idHierNode);
	}

	/**
	 * Crea una nuova misura di un dato (riga di MdMeasureFields)
	 * 
	 * @param dataId
	 *            id del dato a cui appartiene la misura
	 * @param alias
	 *            alias della misura
	 * @param description
	 *            descrizione della misura
	 * @param measureField
	 *            nome fisico della colonna della misura
	 * @param measureUnits
	 *            unità di misura della variabile
	 * @param pos
	 * @return Il nuovo id associato alla riga di MdMeasureFields
	 * @throws OdataDomainException 
	 */
	private int createMeasure(int dataId, String alias, String description,
			String measureField, Short decimals, String measureUnits, int pos) throws OdataDomainException {
		MdData data = MdData.findMdData(dataId);
		if (data == null) {
			return -1;
		}
		return data.addMeasure(alias, description, measureField, decimals,
				measureUnits, pos);
	}

	/**
	 * Crea una nuova colonna generica
	 * 
	 * @param dataId
	 *            ida del dato a cui appartiene la colonna
	 * @param columnField
	 *            nome fisico della colonna
	 * @param alias
	 *            alias della colonna
	 * @param cardinality
	 *            numero di valori univoci nella colonna
	 * @param descriptiveField
	 *            flag che infica se la colonna è un campo descrittivo
	 * @return l'id della colonna
	 * @throws OdataDomainException 
	 */
	private int createGenericColumn(int dataId, String columnField,
			String alias, Integer cardinality, Boolean descriptiveField) throws OdataDomainException {
		MdData data = MdData.findMdData(dataId);
		if (data == null) {
			return -1;
		}
		return data.addGenericColumn(columnField, alias, cardinality,
				descriptiveField);
	}

}
