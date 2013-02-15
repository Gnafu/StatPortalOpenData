package it.sister.statportal.odata.domain;

import org.springframework.roo.addon.dbre.RooDbManaged;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

@RooJavaBean
@RooToString
@RooEntity(versionField = "", table = "md_generic_column", schema = "public")
@RooDbManaged(automaticallyDelete = true)
public class MdGenericColumn {
    
    /**
     * Costruttore
     */
    public MdGenericColumn(){
	
    }
    
    /**
     * Costruttore 
     * 
     * @param columnField nome fisico della colonna
     * @param alias nome logico della colonna
     * @param cardinality cardinalità del campo all'interno della tabella
     * @param idData dato 
     */
    public MdGenericColumn(String columnField, String alias, Integer cardinality, MdData idData, Boolean descriptiveField) {
	setAlias(alias);
	setCardinality(cardinality);
	setColumnField(columnField);
	setDescriptiveField(descriptiveField);
	setIdData(idData);
    }
    
    public void validate() throws OdataDomainException {
	if (this.getAlias().length() > 100) {
	    throw new OdataDomainException("Alias troppo lungo ["
		    + this.getAlias() + "]");
	}
    }
    
}
