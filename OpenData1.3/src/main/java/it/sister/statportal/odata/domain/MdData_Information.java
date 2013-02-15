package it.sister.statportal.odata.domain;

public class MdData_Information {
    
    public MdData_Information(){
	
    }
    
    public MdData_Information(int fieldCount, int rowsCount, MdData_Information_Column[] columns){
	this.fieldCount = fieldCount;
	this.rowsCount = rowsCount;
	this.columns = columns;
    }

    public int fieldCount;
    public int rowsCount;
    public MdData_Information_Column[] columns;

    
}