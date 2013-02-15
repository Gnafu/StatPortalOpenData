package it.sister.statportal.odata.domain;

public class MdData_Information_Column {
	
	public String fieldName;
	public String fieldType;
	public String differentValuesCount;
	public String differentValuesMin;
	public String differentValuesMax;
	public String firstTenContents;
	
	public MdData_Information_Column(){
	    
	}
	
	public MdData_Information_Column(String fieldName, String fieldType, String differentValuesCount, String differentValuesMin, String differentValuesMax, String firstTenContents){
	    this.fieldName = fieldName;
	    this.fieldType = fieldType;
	    this.differentValuesCount = differentValuesCount;
	    this.differentValuesMin = differentValuesMin;
	    this.differentValuesMax = differentValuesMax;
	    this.firstTenContents = firstTenContents;
	}
	
}