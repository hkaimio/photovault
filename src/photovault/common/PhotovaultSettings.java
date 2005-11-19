package photovault.common;

import java.util.Properties;
import java.util.Collection;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.URL;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
   This class contains useful UI independent routines
*/


public class PhotovaultSettings {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotovaultSettings.class.getName() );
    static File configFile;
    static PhotovaultDatabases databases;
    
    static final String defaultPropFname = "photovault.properties";
    static public void init() {
        // Load XML configuration file
        String confFileName = System.getProperty( "photovault.configfile" );
        if ( confFileName != null ) {
            System.out.println( "photovault.configfile " + confFileName );
            configFile = new File( confFileName );
            System.out.println( configFile );
        } else {
            // If the photovault.configfile property is not set, use file photovault.xml 
            // in directory .photovault in user's home directory
            File homeDir = new File( System.getProperty( "user.home", "" ) );
            File photovaultDir = new File( homeDir, ".photovault" );
            if ( !photovaultDir.exists() ) {
                photovaultDir.mkdir();
            }
            configFile = new File( photovaultDir, "photovault.xml" );
        }
        if ( configFile.exists() ) {
            databases = PhotovaultDatabases.loadDatabases( configFile );
        } else {
            try {
                configFile.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if ( databases == null ) {
            databases = new PhotovaultDatabases();
        }
            
    }

    /**
     * Return all known databases
     *@return Collection of PVDatabase objects
     */
    public static Collection getDatabases() {
        return databases.getDatabases();
    }
    
    /**
     * Returns the photovault database descriptor for given database
     * @return the PVDatabase object or null if the named database was not found
     */
    public static PVDatabase getDatabase( String dbName ) {
        return databases.getDatabase( dbName );
    }
    
    public void saveConfig() {
        databases.save( configFile );
    }


    /**
       Set the configuration used when determining property values
       @param confName Name of the configuration. This must be defined in
       photovault.configNames property - results are undefined otherwise.
    */
    static public void setConfiguration( String confName ) {
	PhotovaultSettings.confName = confName;
    }


    public static PVDatabase getCurrentDatabase() {
        return databases.getDatabase( confName );
    }
	
    static String confName;

}