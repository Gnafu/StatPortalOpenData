// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.
// You may push code into the target .java compilation unit if you wish to edit any member(s).

package it.sister.statportal.odata.domain;

import java.lang.String;

privileged aspect MdDataFiles_Roo_ToString {
    
    public String MdDataFiles.toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("IdMetadata: ").append(getIdMetadata());
        return sb.toString();
    }
    
}