package it.sister.statportal.odata.domain;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * 
 * Factory dedicata al Repository
 * 
 */
public class RepositoryFactory {

    
    public static IRepository rep = null;
    /**
     * Restituisce il Repository corretto
     * 
     * @return il Repository
     */
    public static IRepository getRepository() {
	if(rep == null){
	    	// legge dal file di configurazione qual'è il repository da utilizzare
    		BeanFactory ctx = new XmlBeanFactory(new ClassPathResource("beans.xml"));
    		rep = (IRepository) ctx.getBean("repository");
	}
	return rep;
    }

}
