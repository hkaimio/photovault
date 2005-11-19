/*
 * PVDatabase.java
 *
 * Created on 18. marraskuuta 2005, 20:45
 */
 
package photovault.common;

import java.util.ArrayList;
import java.util.List;
import  imginfo.Volume;

/**
 * PVDatabase represents metadata about a single Photrovault database. A database 
 * consists of an SQL database for storing photo metadata and 1 or more volumes 
 * for storing the actual photos
 * @author Harri Kaimio
 */
public class PVDatabase {
    
    /** Creates a new instance of PVDatabase */
    public PVDatabase() {
        volumes = new ArrayList();
    }
    
    private String name;
    private String dbHost = "";
    private String dbName = "";
    private ArrayList volumes;

    public void setName( String name ) {
        this.name = name;
    }
    
    public String getName() {
        return this.name;
    }
    
    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        if ( dbHost != null ) {
            this.dbHost = dbHost;
        } else {
            this.dbHost = "";
        }
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }
    
    public void addVolume( Volume volume ) {
        volumes.add( volume );
    }
    
    public List getVolumes( ) {
        return  volumes;
    }

    public Volume getDefaultVolume() {
        return (Volume) volumes.get(0);
    }
            
}
