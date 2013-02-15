package it.sister.statportal.odata.proxy.rdf;

import java.util.List;
import java.util.Map;

import it.sister.statportal.odata.domain.Column;
import it.sister.statportal.odata.domain.OdataDomainException;
import it.sister.statportal.odata.domain.Row;

/**
 * Formattatore in formato RDF/XML
 *
 */
public class XmlRdfFormatter extends RdfFormatter {

	@Override
	public String getExtension() {
		return "rdf";
	}

	@Override
	public String getHeader(String name, String tableName, boolean pkExists, List<Column> columns, String d2rqServer) throws OdataDomainException {
		StringBuilder builder = new StringBuilder();
		builder.append("<?xml version=\"1.0\"?>\n");
		builder.append("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "+
								"xmlns:vocab=\""+d2rqServer+"vocab/\" "+
								"xmlns:owl=\"http://www.w3.org/2002/07/owl#\" "+
								"xmlns:db=\""+d2rqServer+"\" "+
								"xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\" "+
								"xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" "+
								"xmlns:map=\""+d2rqServer+"#\" "+
								"xml:base=\""+d2rqServer+"\" >\n");
		builder.append("<rdf:Description rdf:about=\"http://www.w3.org/2000/01/rdf-schema#label\">\n"+
					   	"<rdf:type rdf:resource=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#Property\"/>\n"+
					   "</rdf:Description>\n");
		builder.append("<rdf:Description rdf:about=\"vocab/"+(pkExists ? tableName : "view_"+tableName)+"\">\n"+
						"<rdfs:label>"+xmlEscapeText(name)+"</rdfs:label>\n"+
						"<rdf:type rdf:resource=\"http://www.w3.org/2000/01/rdf-schema#Class\"/>\n"+
					   "</rdf:Description>\n");
		if(!pkExists){
			builder.append("<rdf:Description rdf:about=\"vocab/do_serialuid\">\n"+
					"<rdfs:label>Serial Id</rdfs:label>\n"+
					"<rdf:type rdf:resource=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#Property\"/>\n"+
				   "</rdf:Description>\n");
		}
		for(Column column : columns){
			if(column.getPhysicalName().equalsIgnoreCase("the_geom")){
				continue;
			}
			builder.append("<rdf:Description rdf:about=\"vocab/"+column.getPhysicalName()+"\">\n"+
							"<rdfs:label>"+xmlEscapeText(column.getLogicalName())+"</rdfs:label>\n"+
							"<rdf:type rdf:resource=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#Property\"/>\n"+
						   "</rdf:Description>\n");
		}
		
		return builder.toString();
	}
	
	@Override
	public String getFooter() {
		return "</rdf:RDF>\n";
	}
	
	@Override
	protected String formatRow(String tableName, boolean pkExists, Row row, List<Map<String, Object>> primaryKeys, List<Column> columns) throws OdataDomainException {
		final StringBuilder builder = new StringBuilder();		
		String key = "";
		for(Map<String, Object> primaryKey : primaryKeys){
			key += row.getValue((String)primaryKey.get("column_name")) + "/";
		}
		key = (key.length() > 0) ? key.substring(0, key.length() - 1) : key;
		
		builder.append("<rdf:Description rdf:about=\""+tableName+"/"+key+"\">\n");
		builder.append("<rdfs:label>"+tableName+" #"+key+"</rdfs:label>\n");
		if(!pkExists){
			builder.append("<vocab:do_serialuid rdf:datatype=\"http://www.w3.org/2001/XMLSchema#integer\">"+row.getValue("do_serialuid")+"</vocab:do_serialuid>\n");
		}
		for(Column column : columns){
			String physicalName = column.getPhysicalName();
			if(physicalName.equalsIgnoreCase("the_geom")){
				continue;
			}
			String type = "";
			switch(column.getType()){
				case DIMENSION:
					type = " rdf:datatype=\"http://www.w3.org/2001/XMLSchema#integer\"";
					break;
				case MEASURE:
					type = " rdf:datatype=\"http://www.w3.org/2001/XMLSchema#double\"";
					break;
				case GENERIC_COLUMN:
					type = "";
					break;
			}			
			if(row.getValues().containsKey(physicalName)){
				String value = row.getValue(physicalName);
				if(value == null){
					value = "";
				}
				builder.append("<vocab:"+physicalName+type+">"+xmlEscapeText(row.getValue(physicalName))+"</vocab:"+physicalName+">\n");
			}
		}

	    builder.append("<rdf:type rdf:resource=\"vocab/"+tableName+"\"/>\n");
	    builder.append("</rdf:Description>\n");
	    return builder.toString();
	}
	
	/**
	 * Fa l'escape dei caratteri speciali xml sostituendoli con le relative codifiche
	 * @param text il testo da modificare
	 * @return il testo con i caratteri speciali sostituiti dalle loro codifiche
	 */
	protected static String xmlEscapeText(String text) {
	   StringBuilder sb = new StringBuilder();
	   for(int i = 0; i < text.length(); i++){
	      char c = text.charAt(i);
	      switch(c){
	      case '<': sb.append("&lt;"); break;
	      case '>': sb.append("&gt;"); break;
	      case '\"': sb.append("&quot;"); break;
	      case '&': sb.append("&amp;"); break;
	      case '\'': sb.append("&apos;"); break;
	      default:
	         if(c>0x7e) {
	            sb.append("&#"+((int)c)+";");
	         }else
	            sb.append(c);
	      }
	   }
	   return sb.toString();
	}
	
}
