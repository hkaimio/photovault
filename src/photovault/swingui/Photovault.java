package photovault.swingui;

import org.odmg.*;
import javax.swing.JOptionPane;
import dbhelper.ODMG;
import java.net.URL;
import photovault.common.PVDatabase;
import photovault.common.PhotovaultSettings;
import org.apache.log4j.PropertyConfigurator;


/**
   Main class for the photovault application
*/


public class Photovault {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( Photovault.class.getName() );


    Photovault() {
	PhotovaultSettings.init();
    }

    void login( LoginDlg ld ) {
	String user = ld.getUsername();
	String passwd = ld.getPassword();
	String dbName = ld.getDb();
	log.debug( "Using configuration " + dbName );
	PhotovaultSettings.setConfiguration( dbName );
        PVDatabase db = PhotovaultSettings.getDatabase( dbName );
        String sqldbName = db.getDbName();
	log.debug( "Mysql DB name: " + sqldbName );
	if ( sqldbName == null ) {
	    JOptionPane.showMessageDialog( ld, "Could not find dbname for configuration " + db, "Configuration error", JOptionPane.ERROR_MESSAGE );
	    return;
	}
	    

	if ( ODMG.initODMG( user, passwd, db ) ) {
	    log.debug( "Connection succesful!!!" );
	    // Login is succesfull
	    ld.setVisible( false );
	    BrowserWindow br = new BrowserWindow();
	} else {
	    JOptionPane.showMessageDialog( ld, "Error logging into Photovault", "Login error", JOptionPane.ERROR_MESSAGE );
	}

    }
    
    void run() {
	LoginDlg login = new LoginDlg( this );
	login.setVisible( true );
    }


    
    public static void main( String [] args ) {
        URL log4jPropertyURL = Photovault.class.getClassLoader().getResource( "log4j.properties");
        PropertyConfigurator.configure( log4jPropertyURL );
	Photovault app = new Photovault();
	app.run();
    }

}


