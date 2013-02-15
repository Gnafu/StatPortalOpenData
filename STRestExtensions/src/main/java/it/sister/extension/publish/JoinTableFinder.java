package it.sister.extension.publish;

import it.sister.utils.STUtils;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.rest.AbstractCatalogFinder;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

public class JoinTableFinder extends AbstractCatalogFinder {

    public JoinTableFinder(Catalog catalog) {
        super(catalog);       
    }
    
    @Override
    public Resource findTarget(Request request, Response response) {    	
    	if(response==null){
    		STUtils.writeDebugMessage("null object");
    	}
        return new JoinTableResource( null, request, response, catalog);
    }
}
