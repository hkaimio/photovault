package org.photovault.common;

import java.util.Properties;
import java.util.Collection;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.URL;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
   PhotovaultSettings provides access to the installation specific settings - most 
 * importantly to available photovault databases. <p>
 *
 * This class is singleton, only 1 instance is allowed.
*/


public class PhotovaultSettings {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotovaultSettings.class.getName() );
    protected static PhotovaultSettings settings = null;
    
    /**
     * Get the singleton settings object.
     */
    public static PhotovaultSettings getSettings( ) {
        if ( settings == null ) {
            settings = new PhotovaultSettings();
        }
        return settings;
    }
    
    File configFile;
    PhotovaultDatabases databases;
    
    static final String defaultPropFname = "photovault.properties";
    protected PhotovaultSettings() {
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
            log.debug( "Using config file " + configFile.getAbsolutePath() );
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
    public Collection getDatabases() {
        return databases.getDatabases();
    }
    
    /**
     * Returns the photovault database descriptor for given database
     * @return the PVDatabase object or null if the named database was not found
     */
    public PVDatabase getDatabase( String dbName ) {
        return databases.getDatabase( dbName );
    }
    
    /**
     * Saves the configuration to the current configuration file.
     */
    public void saveConfig() {
        databases.save( configFile );
    }


    /**
       Set the configuration used when determining property values
       @param confName Name of the configuration. This must be defined in
       photovault.configNames property - results are undefined otherwise.
    */
    public void setConfiguration( String confName ) {
	this.confName = confName;
    }


    public PVDatabase getCurrentDatabase() {
        return databases.getDatabase( confName );
    }

    /**
     * Add a new database to the configuration.Note that the configuration is not 
     * saved before calling saveConfiguration().
     * @pram db The database that is added to the configuration
     */
    public void addDatabase(PVDatabase db) {
        databases.addDatabase( db );
    }
	
    String confName;

}