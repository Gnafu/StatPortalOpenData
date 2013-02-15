package it.sister.statportal.odata.utility;

public class StringUtils {


    public static String getUniqueIdentifier(String field){
	int hashCode = field.hashCode();
	
	if(hashCode == Integer.MIN_VALUE)
	{
		hashCode = Integer.MAX_VALUE;			
	}
	
	String hashCodeStr = String.valueOf(Math.abs(hashCode));
	String prefix = "C"+ field.replaceAll("[^\\w^0-9]", "_") + "_";
	String candidateName = prefix.substring(0, Math.min(prefix.length(), 60 - hashCodeStr.length())) + hashCodeStr;
	return candidateName;
    }
    
}
