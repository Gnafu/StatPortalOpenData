package it.sister.statportal.odata.domain;

import java.util.Date;
import java.util.List;

import javax.persistence.PreRemove;

import org.springframework.roo.addon.dbre.RooDbManaged;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

/**
 * 
 * Classe che descrive un nodo di una gerarchia dimensionale
 *
 */
@RooJavaBean
@RooToString
@RooEntity(versionField = "", table = "MD_HIER_NODE", schema = "public")
@RooDbManaged(automaticallyDelete = true)
public class MdHierNode implements IData {
    
    /**
     * Costruttore
     */
    public MdHierNode(){
	
    }
    
    /**
     * Costruttore completo
     * 
     * @param descField 
     * @param description descrizione del nodo
     * @param genericGrants 
     * @param hierarchy gerarchia alla quale il nodo è collegato
     * @param idUserOwner id del proprietario
     * @param lastUpdate data di ultimo aggiornamento
     * @param name nome
     * @param numRows numero di righe
     * @param pkField 
     * @param rowSize 
     * @param sortField
     * @param tableName nome della tabella
     */
    public MdHierNode(String descField, String description, boolean genericGrants, MdHierarchy hierarchy, Integer idUserOwner, Date lastUpdate, String name, Integer numRows, String pkField, Integer rowSize, String sortField, String tableName){
	setDescField(descField);
	setDescription(description);
	
	setGenericGrants(genericGrants);
	setIdHierarchy(hierarchy);
	setIdUserOwner(idUserOwner);
	setLastUpdate(lastUpdate);
	setName(name);
	setNumRows(numRows);
	setPkField(pkField);
	setRowSize(rowSize);
	setSortField(sortField);
	setTableName(tableName);
    }
    
    @PreRemove
    void onPreRemove() {
	for (MdDataDim dim : getMdDataDims()) {
	    dim.remove();
	    dim.flush();
	}
	
	for (MdRelHierNode rel : getMdRelHierNodes()) {
	    rel.remove();
	    rel.flush();
	}
	
	for (MdRelHierNode rel1 : getMdRelHierNodes1()) {
	    rel1.remove();
	    rel1.flush();
	}
    }

    @Override
    public List<Column> getColumns() {
	return RepositoryFactory.getRepository().getTableColumns(
		this.getTableName());
    }

    @Override
    public List<Row> getRows() throws OdataDomainException {
	CheckTableNameNotNull();
	return RepositoryFactory.getRepository().getTableRows(
		this.getTableName(), getColumns());
    }

    @Override
    public List<Row> getRows(int startIndex, int count, String filter)
	    throws OdataDomainException {
	CheckTableNameNotNull();
	return RepositoryFactory.getRepository().getTableRows(
		this.getTableName(), getColumns(), startIndex, count, filter);
    }
    
    @Override
    public int getCountRows() throws OdataDomainException {
	return getCountRows(null);
    }
    
    /**
     * Funzione di utilità che controlla che il dato abbia impostato il nome
     * della tabella. Se non impostato lancia eccezione
     * 
     * @throws OdataDomainException
     */
    private void CheckTableNameNotNull() throws OdataDomainException {
	if (this.getTableName() == null) {
	    throw new OdataDomainException(
		    "Nome della tabella del dato non definita");
	}
    }

    @Override
    public int getCountRows(String filter) throws OdataDomainException {
	CheckTableNameNotNull();
	return RepositoryFactory.getRepository().getCountRows(this.getTableName(), null, null, -1, -1, false, filter, null, null, false);
    }

    @Override
    public String getDescriptionField(String physicalName) {
	return physicalName;
    }
    
    
}
