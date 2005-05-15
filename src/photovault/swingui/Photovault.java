package photovault.swingui;

import org.odmg.*;
import javax.swing.JOptionPane;
import dbhelper.ODMG;
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
	String db = ld.getDb();
	log.debug( "Using configuration " + db );
	PhotovaultSettings.setConfiguration( db );
	String sqldbName = PhotovaultSettings.getConfProperty( "dbname" );
	log.debug( "Mysql DB name: " + sqldbName );
	if ( sqldbName == null ) {
	    JOptionPane.showMessageDialog( ld, "Could not find dbname for configuration " + db, "Configuration error", JOptionPane.ERROR_MESSAGE );
	    return;
	}
	    

	if ( ODMG.initODMG( user, passwd, sqldbName ) ) {
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
        PropertyConfigurator.configure( "conf/log4j.properties" );
	Photovault app = new Photovault();
	app.run();
    }

}


