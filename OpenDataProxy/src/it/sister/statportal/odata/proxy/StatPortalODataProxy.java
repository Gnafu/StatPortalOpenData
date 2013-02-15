package it.sister.statportal.odata.proxy;

import java.io.BufferedReader;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.odata4j.producer.resources.ODataProducerProvider;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Proxy che nasconde gli id dei dati
 *
 */
public class StatPortalODataProxy {
	public static Logger logger = Logger.getLogger(StatPortalODataProxy.class);
	
	@SuppressWarnings({ })
	public static void main(String[] args) {
		try{
			new ClassPathXmlApplicationContext(
					"classpath*:META-INF/applicationContext.xml");
			try{
				String logConfigFile = readConfig("logConfigFile");
				DOMConfigurator.configureAndWatch(logConfigFile);
			}catch(Exception ex){
				//si perde il logging ma l'applicazione continua a funzionare
			}
			String urlRegex = "(ftp|http|https):\\/\\/(\\w+:{0,1}\\w*@)?(\\S+)(:[0-9]+)?(\\/|\\/([\\w#!:.?+=&%@!\\-\\/]))";
			String endpointUri = readConfig("url");
			if(endpointUri == null){
				throw new Exception("Manca il parametro url");
			}
			if(!endpointUri.matches(urlRegex)){
				throw new Exception("Parametro url non valido");
			}
			String serviceUrl = readConfig("serviceUrl");
			if(serviceUrl == null){
				throw new Exception("Manca il parametro serviceUrl");
			}
			if(!serviceUrl.matches(urlRegex)){
				throw new Exception("Parametro serviceUrl non valido");
			}
			String myName = readConfig("myName");
			if(myName == null){
				throw new Exception("Manca il parametro myName");
			}
		 	final StatPortalProxyProducer producer = new StatPortalProxyProducer(myName, serviceUrl);
		    ODataProducerProvider.setInstance(producer);
		    ProducerUtil.hostODataServer(endpointUri);
		}catch(Exception ex){
			Log(ex);
		}  
	} 
	
	/**
	 * Logga le eccezioni
	 * @param t un generico throwable
	 */
	public static void Log(Throwable t){
		logger.error(t.getMessage(), t);
	}
	
	/**
	 * Legge una chiave dal file di configurazione
	 * @param key il nome della chiave 
	 * @return il valore della chiave se è presente, null altrimenti
	 */
	protected static String readConfig(String key) {
		try{
			BufferedReader reader = null;
			try{
				reader = new BufferedReader(new java.io.FileReader("META-INF/config.txt"));
			}catch(Exception e){
				reader = new BufferedReader(new java.io.FileReader("src/META-INF/config.txt"));
			}
			String line = null;
			while((line = reader.readLine()) != null){
				String[] splittedLine = line.split("\t");
				if(splittedLine.length == 2){
					if(splittedLine[0].equals(key)){
						return splittedLine[1];
					}
				}
			}
			return null;
		}catch(Exception ex){
			return null;
		}
	}
}
