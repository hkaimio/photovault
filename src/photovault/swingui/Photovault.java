package photovault.swingui;

import org.odmg.*;
import javax.swing.JOptionPane;
import dbhelper.ODMG;

/**
   Main class for the photovault application
*/


public class Photovault {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( Photovault.class.getName() );


    Photovault() {

    }

    void login( LoginDlg ld ) {
	String user = ld.getUsername();
	String passwd = ld.getPassword();
	String db = ld.getDb();

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
	Photovault app = new Photovault();
	app.run();
    }

}


