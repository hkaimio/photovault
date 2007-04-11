/*
  Copyright (c) 2006 Harri Kaimio
  
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
import java.io.File;
import java.io.IOException;
import org.photovault.dbhelper.ODMG;

/**
 * This class handles proper OJB initialization for unti tests. This class is 
 * a singleton and it just sets up the OJB environment according to configuration files
 * so it is enough that the unit tests just get a reference to it.
 *<p>
 * Environment setup is doe like this:
 * <ul>
 * <li> First, property file is looked based on system property photovault.propFname
 * <li> Then the configuration is set to "pv_junit"
 * <li> Last, ODMG is initialized using no username or password
 * </ul>
 * @author harri Kaimio
 */
public class JUnitOJBManager {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( JUnitOJBManager.class.getName() );

    /** Creates a new instance of JUnitOJBManager */
    private JUnitOJBManager() {
        System.setProperty( "photovault.configfile", "conf/junittest_config.xml" );
        log.error( "Initializing OB for JUnit tests" );
        createDatabase();
        PhotovaultSettings settings = PhotovaultSettings.getSettings();
        settings.setConfiguration( "pv_junit" );
	PVDatabase db = settings.getDatabase( "pv_junit" );
        
        if ( db == null ) {
            log.error( "Could not find dbname for configuration " );
            return;
        }
        
        try {
            ODMG.initODMG( "", "", db );
            log.debug( "Connection succesful!!!" );
        } catch (PhotovaultException e ) {
            log.error( "Error logging into Photovault: " + e.getMessage() );
            System.exit( 1 );
        } 
        
        if ( db.getSchemaVersion() < PVDatabase.CURRENT_SCHEMA_VERSION ) {
            SchemaUpdateAction updater = new SchemaUpdateAction( db );
            updater.upgradeDatabase();
        }
    }
    
    /**
     Creates a new Photovault database in temp directory.
     */
    private void createDatabase() {
        File dbDir = null;
        try {
            dbDir = File.createTempFile("pv_junit_derby_instance", "");
            dbDir.delete();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        PVDatabase pvd = new PVDatabase();
        pvd.setInstanceType( PVDatabase.TYPE_EMBEDDED );
        pvd.setEmbeddedDirectory( dbDir );
        pvd.createDatabase( "", "", "junit_seed_data.xml" );
        PhotovaultSettings settings = PhotovaultSettings.getSettings();
        pvd.setName( "pv_junit" );
        try {
            settings.addDatabase( pvd );
        } catch (PhotovaultException ex) {
            ex.printStackTrace();
        }
    }
    
    static JUnitOJBManager mgr = null;
    public static JUnitOJBManager getOJBManager() {
        if ( mgr == null ) {
            mgr = new JUnitOJBManager();
        }
        return mgr;
    }
}
