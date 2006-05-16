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
  along with Foobar; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
*/

package org.photovault.swingui;

import java.util.Collection;
import javax.media.jai.JAI;
import org.odmg.*;
import javax.swing.JOptionPane;
import org.photovault.common.SchemaUpdateAction;
import org.photovault.common.SchemaUpdateEvent;
import org.photovault.common.SchemaUpdateListener;
import org.photovault.dbhelper.ODMG;
import java.net.URL;
import org.photovault.common.PVDatabase;
import org.photovault.common.PhotovaultSettings;
import org.apache.log4j.PropertyConfigurator;
import org.photovault.swingui.db.DbSettingsDlg;


/**
   Main class for the photovault application
*/


public class Photovault implements SchemaUpdateListener {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( Photovault.class.getName() );

    PhotovaultSettings settings = null;

    public Photovault() {
	settings = PhotovaultSettings.getSettings();
    }

    private boolean login( LoginDlg ld ) {
	boolean success = false;
        String user = ld.getUsername();
	String passwd = ld.getPassword();
	String dbName = ld.getDb();
	log.debug( "Using configuration " + dbName );
	settings.setConfiguration( dbName );
        PVDatabase db = settings.getDatabase( dbName );
        String sqldbName = db.getDbName();
	log.debug( "Mysql DB name: " + sqldbName );
	if ( sqldbName == null ) {
	    JOptionPane.showMessageDialog( ld, "Could not find dbname for configuration " + db, "Configuration error", JOptionPane.ERROR_MESSAGE );
	    return false;
        }
        
        if ( ODMG.initODMG( user, passwd, db ) ) {
            log.debug( "Connection succesful!!!" );
            // Login is succesfull
            // ld.setVisible( false );
            success = true;
            
            int schemaVersion = db.getSchemaVersion();
            if ( schemaVersion < db.CURRENT_SCHEMA_VERSION ) {
                String options[] = {"Proceed", "Exit Photovault"};
                if ( JOptionPane.YES_OPTION == JOptionPane.showOptionDialog( ld,
                        "The database was created with an older version of Photovault\n" +
                        "Photovault will upgrade the database format before starting.",
                        "Upgrade database",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        options,
                        null ) ) {
                    final SchemaUpdateAction updater = new SchemaUpdateAction( db );
                    SchemaUpdateStatusDlg statusDlg = new SchemaUpdateStatusDlg( null, true );
                    updater.addSchemaUpdateListener( statusDlg );
                    Thread upgradeThread = new Thread() {
                        public void run() {
                            updater.upgradeDatabase();
                        }
                    };
                    upgradeThread.start();
                    statusDlg.setVisible( true );
                    success = true;
                }
            }
        }
        return success;
    }
    
    void run() {
        checkSystem();
        PhotovaultSettings settings = PhotovaultSettings.getSettings();
        Collection databases = settings.getDatabases();
        if ( databases.size() == 0 ) {
            DbSettingsDlg dlg = new DbSettingsDlg( null, true );
            if ( dlg.showDialog() != dlg.APPROVE_OPTION ) {
                System.exit( 0 );
            }
        }
     
	LoginDlg login = new LoginDlg( this );
        boolean loginOK = false;
        while ( !loginOK ) {
	int retval = login.showDialog();
            switch( retval ) {
                case LoginDlg.RETURN_REASON_CANCEL:
                    System.exit( 0 );
                    break;
                case LoginDlg.RETURN_REASON_NEWDB:
                    DbSettingsDlg dlg = new DbSettingsDlg( null, true );
                    if ( dlg.showDialog() == dlg.APPROVE_OPTION ) {
                        login = new LoginDlg( this );
                    }
                    break;
                case LoginDlg.RETURN_REASON_APPROVE:
                    if ( login( login ) ) {
                        loginOK = true;
                        BrowserWindow wnd = new BrowserWindow();
                    } else {
                        JOptionPane.showMessageDialog( null, "Error logging into Photovault", 
                                "Login error", JOptionPane.ERROR_MESSAGE );
                    }
                    break;
                default:
                    log.error( "Unknown return code form LoginDlg.showDialog(): " + retval );
                    break;
            }
        }
    }


    protected void checkJAI() throws PhotovaultException {
        try {
            String jaiVersion = JAI.getBuildVersion();
        } catch ( Throwable t ) {
            throw new PhotovaultException( 
                      "Java Advanced Imaging not installed\n"
                    + "properly. This is needed by Photovault,"
                    + "please download and install it from\n"
                    + "http://java.sun.com/products/java-media/jai/");
        }
    }
    
    /** 
     Checks that the system is in OK state.
     */
    protected void checkSystem() {
        try {
            checkJAI();
        } catch ( PhotovaultException e ) {
            JOptionPane.showMessageDialog( null, e.getMessage(),
                    "Photovault error", JOptionPane.ERROR_MESSAGE );
            System.exit( 1 );
        }
    }
   
    class PhotovaultException extends Exception {
        PhotovaultException( String msg ) {
            super( msg );
        }
    }
    
    public static void main( String [] args ) {
        URL log4jPropertyURL = Photovault.class.getClassLoader().getResource( "photovault_log4j.properties");
        PropertyConfigurator.configure( log4jPropertyURL );	
        Photovault app = new Photovault();
	app.run();
    }

    public void schemaUpdateStatusChanged(SchemaUpdateEvent e) {
        System.out.println( "Phase " + e.getPhase()+ ", " + e.getPercentComplete() + "%" );
    }

}


