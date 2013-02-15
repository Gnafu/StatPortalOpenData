package it.sister.statportal.odata.domain;

import javax.persistence.PreRemove;

import org.springframework.roo.addon.dbre.RooDbManaged;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

/**
 * 
 * Classe che descrive una gerarchia
 *
 */
@RooJavaBean
@RooToString
@RooEntity(versionField = "", table = "MD_HIERARCHY", schema = "public")
@RooDbManaged(automaticallyDelete = true)
public class MdHierarchy {
    
    /**
     * Costruttore
     */
    public MdHierarchy(){
	
    }
    
    /**
     * Costruttore completo
     * 
     * @param name nome della gerarchia
     * @param description descrizione della gerarchia
     * @param idLuHierType tipologia della gerarchia
     */
    public MdHierarchy(String name, String description, Integer idLuHierType){
	setName(name);
	setDescription(description);
	setIdLuHierType(idLuHierType);
    }
    
    @PreRemove
    void onPreRemove() {
	for (MdHierNode node : getMdHierNodes()) {
	    node.remove();
	    node.flush();
	}
    }
   
}
