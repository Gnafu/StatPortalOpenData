package it.sister.statportal.odata;

import java.io.BufferedReader;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.odata4j.producer.jpa.JPAProducer;
import org.odata4j.producer.resources.ODataProducerProvider;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class StatPortalOData {

	public static Logger logger = Logger.getLogger(StatPortalOData.class);
	
	@SuppressWarnings({})
	public static void main(String[] args) {
		try {

			new ClassPathXmlApplicationContext("classpath*:META-INF/applicationContext.xml");			
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
				throw new Exception("Chiave url non valida");
			}
			String persistenceUnitName = readConfig("unitName");
			String namespace = "StatPortal";

			EntityManagerFactory emf = Persistence
					.createEntityManagerFactory(persistenceUnitName);
			final JPAProducer jpaProducer = new JPAProducer(emf, namespace, 50);
			final it.sister.statportal.odata.StatPortalProducer producer = new it.sister.statportal.odata.StatPortalProducer(
					jpaProducer);

			// register the producer as the static instance, then launch the
			// http server
			ODataProducerProvider.setInstance(producer);
			logger.info("Starting service");
			ProducerUtil.hostODataServer(endpointUri);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
	}
	
	/**
	 * Legge una chiave dal file di configurazione
	 * @param key il nome della chiave 
	 * @return il valore della chiave se è presente, null altrimenti
	 */
	private static String readConfig(String key) {
		try{			
			BufferedReader reader = null;
			try{
				reader = new BufferedReader(new java.io.FileReader("META-INF/config.txt"));
			}catch(Exception exception){
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
			logger.warn("Can't read key: "+key);
			return null;
		}
	}
}