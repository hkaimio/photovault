/*
  Copyright (c) 2006-2007 Harri Kaimio
  
  This file is part of Photovault.

  Photovault is free software; you can redistribute it and/or modify it
  under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  Photovault is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with Photovault; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
*/

package org.photovault.common;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Collection;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.URL;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.commons.digester.Digester;
import org.photovault.imginfo.ExternalVolume;
import org.photovault.imginfo.Volume;
import org.xml.sax.SAXException;

/**
   PhotovaultSettings provides access to the installation specific settings - most 
 * importantly to available photovault databases. <p>
 *
 * This class is singleton, only 1 instance is allowed.
*/


public class PhotovaultSettings {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotovaultSettings.class.getName() );
    static PhotovaultSettings settings = null;
    
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
        File oldConfigFile = null;
        if ( confFileName != null ) {
            log.debug( "photovault.configfile " + confFileName );
            configFile = new File( confFileName );
            log.debug( configFile );
        } else {
            // If the photovault.configfile property is not set, use file photovault.xml 
            // in directory .photovault in user's home directory
            File homeDir = new File( System.getProperty( "user.home", "" ) );
            File photovaultDir = new File( homeDir, ".photovault" );
            if ( !photovaultDir.exists() ) {
                photovaultDir.mkdir();
            }
            configFile = new File( photovaultDir, "photovault_config.xml" );
            if ( !configFile.exists() ) {
                // check if there is a config file in the old (pre-0.4.0) format 
                // and read it if it exists.
                oldConfigFile = new File( photovaultDir, "photovault.xml" );
            }
        }
        if ( configFile.exists() ) {
            log.debug( "Using config file " + configFile.getAbsolutePath() );
            loadConfig( configFile );
//            databases = PhotovaultDatabases.loadDatabases( configFile );
        } else if ( oldConfigFile.exists() ) {
            loadConfig( oldConfigFile );
            saveConfig();
//            try {
//                configFile.createNewFile();
//            } catch (IOException ex) {
//                ex.printStackTrace();
//            }
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
        URL buildPropertyURL = PhotovaultSettings.class.getClassLoader().getResource( "buildinfo.properties");
        String version = "unknown";
        try {
            InputStream is = buildPropertyURL.openStream();
            Properties prop = new Properties();
            prop.load( is );
            version = prop.getProperty( "build.version", "unknown" );
        } catch (IOException e ) {
        }
        
        try {
            BufferedWriter outputWriter = new BufferedWriter( new FileWriter( configFile ));
            outputWriter.write("<?xml version='1.0' ?>\n");
            outputWriter.write( "<!--\n" +
                    "This is configuration file for Photovault image organizing application\n" +
                    "-->\n");
            outputWriter.write( "<photovault-config version=\"" + version + "\">\n" );
            String indent = "  ";
            outputWriter.write( indent + "<!-- Installation specific properties -->\n" );
            Iterator iter = properties.entrySet().iterator();
            while ( iter.hasNext() ) {
                Map.Entry e = (Entry) iter.next();
                outputWriter.write( indent + "<property name=\"" + e.getKey() +
                        "\" value=\"" + e.getValue() + "\"/>\n" );
            }
            databases.save( outputWriter );
            outputWriter.write( "</photovault-config>\n" );
        outputWriter.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    
    private void loadConfig( File f ) {
        Digester digester = new Digester();
        digester.push(this); // Push controller servlet onto the stack
        digester.setValidating(false);
        
        // Digester rules for parsing the file
        digester.addCallMethod( "photovault-config", "setConfigFileVersion", 1 );
        digester.addCallParam( "photovault-config", 0,"version" );
        digester.addObjectCreate( "databases/databases", PhotovaultDatabases.class );
        digester.addSetNext( "databases/databases", "setDatabases" );
        digester.addObjectCreate( "photovault-config/databases", PhotovaultDatabases.class );
        digester.addSetNext( "photovault-config/databases", "setDatabases" );
        digester.addObjectCreate( "*/databases/database", PVDatabase.class );
        digester.addSetProperties( "*/databases/database" );
        digester.addSetNext( "*/databases/database", "addDatabase" );
        
        // Property setting
        digester.addCallMethod( "*/photovault-config/property", "setProperty", 2 );
        digester.addCallParam( "*/photovault-config/property", 0, "name" );
        digester.addCallParam( "*/photovault-config/property", 1, "value" );
        
        // Volume creation
        digester.addObjectCreate( "*/database/volumes/volume", Volume.class );
        String [] volumeAttrNames = {
            "basedir", "name"
        };
        String [] volumePropNames = {
            "baseDir", "name"
        };
        digester.addSetProperties( "*/database/volumes/volume",
                volumeAttrNames, volumePropNames );
        digester.addSetNext( "*/database/volumes/volume", "addVolume" );
        digester.addObjectCreate( "*/database/volumes/external-volume", ExternalVolume.class );
        String [] extVolAttrNames = {
            "folder", "basedir", "name"
        };
        String [] extVolPropNames = {
            "folderId", "baseDir", "name"
        };
        
        digester.addSetProperties( "*/database/volumes/external-volume", 
                extVolAttrNames, extVolPropNames );
        digester.addSetNext( "*/database/volumes/external-volume", "addVolume" );
        try {
            
            digester.parse( f );
        } catch (SAXException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void setDatabases( PhotovaultDatabases dbs ) {
        this.databases = dbs;
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
    
    HashMap properties = new HashMap();
    
    public void setProperty( String name, String value ) {
        properties.put( name, value );
    }
    
    public String getProperty( String name ) {
        return getProperty( name, null );
    }
    
    public String getProperty( String name, String defaultValue ) {
        String ret = defaultValue;
        if ( properties.containsKey( name ) ) {
            ret = (String) properties.get( name );
        }
        return ret;
    }

    String configFileVersion = "unknown";
    
    public void setConfigFileVersion( String version ) {
        configFileVersion = version;
    }
    /**
     * Add a new database to the configuration.Note that the configuration is not 
     * saved before calling saveConfiguration().
     * @pram db The database that is added to the configuration
     @throws @see PhotovaultException if a database with the same name already exists
     */
    public void addDatabase(PVDatabase db) throws PhotovaultException {
        databases.addDatabase( db );
    }
	
    String confName;
    
}