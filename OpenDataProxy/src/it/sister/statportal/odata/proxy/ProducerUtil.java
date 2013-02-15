package it.sister.statportal.odata.proxy;

import org.odata4j.producer.resources.CrossDomainResourceConfig;
import org.odata4j.producer.resources.ODataResourceConfig;
import org.odata4j.producer.server.JerseyServer;

import com.sun.jersey.api.container.filter.LoggingFilter;

/**
 * Contenitore di funzioni di utilità generale, principalmente usate per far partire il server jersey
 *
 */
public class ProducerUtil {

  public static void hostODataServer(final String baseUri) {
    startODataServer(baseUri);
  }

  public static JerseyServer startODataServer(final String baseUri) {
	    return createODataServer(baseUri).start();
  }
  
  public static JerseyServer createODataServer(final String baseUri) {
    return new JerseyServer(baseUri)
        .addAppResourceClasses(new ODataResourceConfig().getClasses())
        .addRootResourceClasses(new CrossDomainResourceConfig().getClasses())
        .addJerseyRequestFilter(LoggingFilter.class);
  }
}