package it.sister.extension.publish;

import it.sister.utils.STUtils;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.rest.AbstractCatalogFinder;
import org.geoserver.rest.RestletException;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class PublishFinder extends AbstractCatalogFinder {

    public PublishFinder(Catalog catalog) {
        super(catalog);       
    }
    
    @Override
    public Resource findTarget(Request request, Response response) {
        //Get request parameters    	
        String wsName = getAttribute(request, "workspace");
        String dsName = getAttribute(request, "dataStore");       
        try{
        	//check workspace
	        if(!STUtils.workspaceExists(catalog, wsName)){
	            throw new RestletException( "No such workspace: " + wsName, Status.CLIENT_ERROR_NOT_FOUND );
	        }
	        //check datastore
	        if(!STUtils.dataStoreExists(catalog, wsName, dsName)){
	        	throw new RestletException( "No such DataStore: " + dsName, Status.CLIENT_ERROR_NOT_FOUND );
	        }	        
        }
        catch(RestletException re){
        	String msg="Error verifying workspace '" + wsName + "' or datastore '" + dsName + "'";
        	STUtils.writeErrorMessage(msg + " " + re.getMessage());
        	throw re;
        }
        catch(Exception e){
        	String msg="Error verifying workspace '" + wsName + "' or datastore '" + dsName + "'";
        	STUtils.writeErrorMessage(msg + " " + e.getMessage());
        	throw new RestletException(msg, Status.CLIENT_ERROR_NOT_FOUND );        	
        }
        
        return new PublishResource( null, request, response, catalog);
    }
}
