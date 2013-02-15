package it.sister.statportal.odata.domain;

import org.springframework.roo.addon.dbre.RooDbManaged;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

/**
 * 
 * Classe che rappresenta la relazione tra i nodi di una gerarchia
 *
 */
@RooJavaBean
@RooToString
@RooEntity(identifierType = MdRelHierNodePK.class, versionField = "", table = "MD_REL_HIER_NODE", schema = "public")
@RooDbManaged(automaticallyDelete = true)
public class MdRelHierNode {
    
    /**
     * Costruttore
     */
    public MdRelHierNode(){
	
    }
    
    /**
     * Costruttore completo
     * @param child nodo figlio
     * @param parent nodo padre
     */
    public MdRelHierNode(MdHierNode child, MdHierNode parent, String fkField){
	setIdChildNode(child);
	setIdParentNode(parent);
	setFkField(fkField);
	setId(new MdRelHierNodePK(parent.getId(), child.getId()));
    }
}
