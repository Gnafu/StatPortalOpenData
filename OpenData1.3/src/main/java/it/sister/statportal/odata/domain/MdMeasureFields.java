package it.sister.statportal.odata.domain;

import it.sister.statportal.odata.utility.DBUtils;

import org.springframework.roo.addon.dbre.RooDbManaged;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

/**
 * Classe che definisce una misura associata ad un dato
 *
 */
@RooJavaBean
@RooToString
@RooEntity(versionField = "", table = "MD_MEASURE_FIELDS", schema = "public")
@RooDbManaged(automaticallyDelete = true)
public class MdMeasureFields {
    
    /**
     * Costruttore
     */
    public MdMeasureFields(){
	
    }
    
    /**
     * Costruttore completo 
     * 
     * @param alias alias 
     * @param description descrizione
     * @param data dato a cui la misura fa riferimento
     * @param measureField nome della colonna che rappresenta la misura
     * @param measureUnits unità di misura della variabile
     * @param decimalPlaces numero di decimali
     * @param pos posizione
     */
    public MdMeasureFields(String alias, String description, MdData data, String measureField, String measureUnits, Short decimalPlaces, Integer pos){
	setAlias(alias);
	setDescription(description);
	setIdData(data);
	setMeasureField(measureField);
	setMeasureUnits(measureUnits);
	setDecimalPlaces(decimalPlaces);
	setPos(pos);
    }
    
    public static int getIdFromUid(String uid){
	return DBUtils.getIntValueFromUniqueStringField("md_measure_fields", "uid", uid, "id");
    }
    
    public void validate() throws OdataDomainException {
	if (this.getAlias().length() > 100) {
	    throw new OdataDomainException("Alias troppo lungo ["
		    + this.getAlias() + "]");
	}
    }
}
