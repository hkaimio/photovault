package photovault.swingui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
   ProgressDlg is a simple dialog that displays a progress bar and information message
*/

public class ProgressDlg extends JDialog {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoInfoDlg.class.getName() );

    static final String DIALOG_TITLE = "Progress";

    /**
       Constructor
    */
    ProgressDlg( Frame owner, boolean modal ) {
	super( owner, DIALOG_TITLE, modal );
	createUI();

    }

    protected void createUI() {
	progressBar = new JProgressBar();
	progressBar.setMaximum( 100 );
	progressBar.setValue( 50 ); // for debugging purposes
	getContentPane().add( progressBar, BorderLayout.NORTH );

	pack();
    }

    /**
       Sets the progress bar to specified percentage
       @param percent percentage of completeness (0-100)
    */
    
    void setProgressPercent( int percent ) {
	final int percent2 = percent;
	SwingUtilities.invokeLater( 
				   new Runnable() {
				       public void run() {
					   progressBar.setValue( percent2 );
				       }
				   }
				   );

    }

    /**
       Sets the status string displayed
       @param status The status string
    */
    void setStatus( String status ) {
    }

    /**
       Informs the dialog that the task has been completed. Dialog is dismissed
    */
    void completed() {
	SwingUtilities.invokeLater( 
				   new Runnable() {
				       public void run() {
					   setVisible( false );
				       }
				   }
				   );
    }
	
       
    JProgressBar progressBar;
}