package it.sister.statportal.odata.domain;

import org.springframework.roo.addon.dbre.RooDbManaged;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

/**
 * Classe che definisce il tipo della gerarchia
 *
 */
@RooJavaBean
@RooToString
@RooEntity(versionField = "", table = "MD_LU_HIER_TYPE", schema = "public")
@RooDbManaged(automaticallyDelete = true)
public class MdLuHierType {
    
}
