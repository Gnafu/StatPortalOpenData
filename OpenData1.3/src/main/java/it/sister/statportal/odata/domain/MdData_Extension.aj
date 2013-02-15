package it.sister.statportal.odata.domain;

import org.aspectj.lang.annotation.Before;

public aspect MdData_Extension {
    
    public MdGenericColumn MdData.removeGenericColumn(int id) throws OdataDomainException{
	for(MdGenericColumn genericColumn : this.getMdGenericColumns()){
	    if(genericColumn.getId() == id){
		genericColumn.remove();
		genericColumn.merge();
		return genericColumn;
	    }
	}
	return null;
    }
    
    /**
     * Rimuovere una dimensione dato un id; rimuove esclusivamente la dimensione (non il nodo e la gerarchia)
     * 
     * @param id
     *        id della dimensione da rimuovere
     * @return la dimensione rimossa
     * @throws OdataDomainException 
     */
    public MdDataDim MdData.removeDimension(int id) throws OdataDomainException{
	for (MdDataDim dim : this.getMdDataDims()) {
	    if (dim.getId() == id) {
		dim.remove();
		dim.merge();
		return dim;
	    }
	}
	return null;
    }

    /**
     * Aggiunge una dimensione al dato
     * 
     * @param alias
     *        alias della dimensione
     * @param description
     *        descrizione della dimensione
     * @param dimcodeField
     *        nome fisico della tabella della dimensione
     * @param idHierNode
     *        id del nodo a cui appartiene la dimensione
     * @return l'id della dimensione. -1 in caso di esito negativo
     * @throws OdataDomainException 
     */
    public int MdData.addDimension(String alias, String description,
	    String dimcodeField, int idHierNode) throws OdataDomainException {
	MdHierNode hierNode = MdHierNode.findMdHierNode(idHierNode);
	if (hierNode != null) {
	    MdDataDim newDimension = new MdDataDim();
	    newDimension.setAlias(alias);
	    newDimension.setDescription(description);
	    newDimension.setDimcodeField(dimcodeField);
	    newDimension.setIdData(this);

	    newDimension.setIdHierNode(hierNode);
	    newDimension.persist();
	    return newDimension.getId();
	} else {
	    return -1;
	}
    }

    /**
     * Aggiunge una misura al dato
     * 
     * @param alias
     *        alias della misura
     * @param description
     *        descrizione della misura
     * @param measureField
     *        nome fisico
     * @param measureUnits
     *        unità di misura
     * @param decimalPlaces
     *        numero di decimali
     * @param pos
     *        posizione
     * @return l'id della misura. -1 in caso di esito negativo
     */
    public int MdData.addMeasure(String alias, String description,
	    String measureField, Short decimalPlaces, String measureUnits, int pos) throws OdataDomainException {
	MdMeasureFields newMeasure = new MdMeasureFields();
	newMeasure.setAlias(alias);
	newMeasure.setDescription(description);
	newMeasure.setIdData(this);
	newMeasure.setMeasureField(measureField);
	newMeasure.setMeasureUnits(measureUnits);
	newMeasure.setDecimalPlaces(decimalPlaces);
	newMeasure.setPos(pos);

	newMeasure.persist();
	return newMeasure.getId();
    }
    
    
    public int MdData.addGenericColumn(String columnField, String alias, Integer cardinality, Boolean descriptiveField) throws OdataDomainException {
	MdGenericColumn newGenericColumn = new MdGenericColumn(columnField, alias, cardinality, this, descriptiveField);
	newGenericColumn.persist();
	return newGenericColumn.getId();
    }

    /**
     * Rimuovere una misura al dato
     * 
     * @param uid
     *        id della misura
     * @return la misura rimossa
     */
    public MdMeasureFields MdData.removeMeasure(int uid) throws OdataDomainException{
	for (MdMeasureFields measure : this.getMdMeasureFieldss()) {
	    if (measure.getId() == uid) {
		measure.remove();
		measure.merge();
		return measure;
	    }
	}
	return null;
    }

}
