package it.sister.statportal.odata.domain;

import it.sister.statportal.odata.utility.DBUtils;

import org.springframework.roo.addon.dbre.RooDbManaged;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

/**
 * 
 * Classe che definisce una dimensione associata ad un dato
 * 
 */
@RooJavaBean
@RooToString
@RooEntity(versionField = "", table = "MD_DATA_DIM", schema = "public")
@RooDbManaged(automaticallyDelete = true)
public class MdDataDim {

    /**
     * Costruttore
     */
    public MdDataDim() {

    }

    /**
     * Costruttore completo (null per lasciare i campi incompleti)
     * 
     * @param alias
     *        alias della dimensione
     * @param description
     *        descrizione della dimensione
     * @param idData
     *        dato di riferimento
     * @param dimCodeField
     *        nome della tabella relativa alla dimensione
     *        
     */
    public MdDataDim(String alias, String description, MdData idData,
	    String dimCodeField, MdHierNode idHierNode) {
	setAlias(alias);
	setDescription(description);
	setIdData(idData);
	setDimcodeField(dimCodeField);
	setIdHierNode(idHierNode);
    }

    private MdLayer getMdLayer() {
	MdLayer layer = null;
	MdRelLayerNode rel = getMdRelLayerNode();
	if (rel != null) {
	    layer = rel.getIdLayer();
	}

	return layer;
    }

    private MdRelLayerNode getMdRelLayerNode() {
	return MdRelLayerNode.findMdRelLayerNode(this.getIdHierNode().getId());
    }

    /***
     * Restituisce il nome del layer associato (se presente) in caso di
     * dimensione territoriale
     * 
     * @return	il nome del layer associato (se presente) in caso di
     * dimensione territoriale
     */
    public String getMapLayerName() {
	MdLayer layer = this.getMdLayer();
	if (layer != null) {
	    return layer.getName();
	}
	return null;
    }

    /**
     * Restituisce il path del layer associato (se presente) in caso di
     * dimensione territoriale
     * 
     * @return	il path del layer associato (se presente) in caso di
     * dimensione territoriale
     */
    public String getMapLayerPath() {
	MdLayer layer = this.getMdLayer();
	if (layer != null) {
	    return layer.getPath();
	}
	return null;
    }

    /**
     * Restituisce il nome del campo di aggancio del layer
     * @return	il nome del campo di aggancio del layer
     */
    public String getMapLayerField() {
	MdRelLayerNode rel = getMdRelLayerNode();
	if (rel != null) {
	    return rel.getLayerField();
	}
	return null;
    }

    /**
     * Espone il campo in relazione con il layer  
     * @return il campo, null altrimenti
     */
    public String getMapNodeField() {
	MdRelLayerNode rel = getMdRelLayerNode();
	if (rel != null) {
	    return rel.getNodeField();
	}
	return null;
    }
    
    /**
     * Espone il tipo della dimensione
     * @return il tipo della dimensione
     */
    public String getDimType(){
	int dimTypeInt = this.getIdHierNode().getIdHierarchy().getIdLuHierType().intValue();
	MdLuHierType dimType = MdLuHierType.findMdLuHierType(dimTypeInt);
	return dimType.getName();
    }
    
    /**
     * Espone la cardinalità della dimensione (quanti elementi diversi)
     * @return la cardinalità 
     */
    public int getCardinality(){
	return RepositoryFactory.getRepository().getCardinality(this.getIdData().getTableName(), this.getDimcodeField());
    }
    
    /**
     * Valida l'alias della dimensione
     * @throws OdataDomainException
     */
    public void validate() throws OdataDomainException{
	if(this.getAlias().length() > 100){
	    throw new OdataDomainException("");
	}
    }
    
    /**
     * Dato un uid restituisce il relativo id
     * @param uid
     * @return	l'id associato alla dimensione
     */
    public static int getIdFromUid(String uid){
	return DBUtils.getIntValueFromUniqueStringField("md_data_dim", "uid", uid, "id");
    }
    
    

}
