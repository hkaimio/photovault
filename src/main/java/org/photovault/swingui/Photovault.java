package org.photovault.swingui;

import java.util.Collection;
import org.odmg.*;
import javax.swing.JOptionPane;
import org.photovault.dbhelper.ODMG;
import java.net.URL;
import org.photovault.common.PVDatabase;
import org.photovault.common.PhotovaultSettings;
import org.apache.log4j.PropertyConfigurator;
import org.photovault.swingui.db.DbSettingsDlg;


/**
   Main class for the photovault application
*/


public class Photovault {
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
        }
        return success;

    }
    
    void run() {
        PhotovaultSettings settings = PhotovaultSettings.getSettings();
        Collection databases = settings.getDatabases();
        if ( databases.size() == 0 ) {
            // No known database exists, so create new
            Object options[] = { "Create", "Exit" };
            int retval = JOptionPane.showOptionDialog( null, "No known database exist.\nDo you want to create a new one?",
                    "Photovault", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0] );
            if ( retval == JOptionPane.YES_OPTION ) {
                DbSettingsDlg dlg = new DbSettingsDlg( null, true );
                if ( dlg.showDialog() != dlg.APPROVE_OPTION ) {
                    System.exit( 0 );
                }
            } else {
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


    
    public static void main( String [] args ) {
        URL log4jPropertyURL = Photovault.class.getClassLoader().getResource( "photovault_log4j.properties");
        PropertyConfigurator.configure( log4jPropertyURL );
	Photovault app = new Photovault();
	app.run();
    }

}


