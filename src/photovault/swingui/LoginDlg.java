package photovault.swingui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import imginfo.*;
import java.io.*;
import photovault.common.PhotovaultSettings;


public class LoginDlg extends JFrame {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoInfoDlg.class.getName() );

    static final String DIALOG_TITLE = "Login";
    Photovault app;
    /**
       Constructor. 
       @param owner Owner of the dialog
    */
    public LoginDlg( Photovault app ) {
	super( "Login to Photovault");
	this.app = app;
	SwingUtilities.invokeLater(new java.lang.Runnable() {
		public void run() {
		    createUI();
		}
	    });
    }

    public String getUsername() {
	return idField.getText();
    }

    public String getPassword() {
	char[] pass = passField.getPassword();
	return new String( pass );
    }

    public String getDb() {
	return dbField.getSelectedItem().toString();
    }
    
    boolean okPressed = false;
    
    /**
       Creates the UI components needed for this dialog.
    */
    protected void createUI() {

        GridBagConstraints labelConstraints = new GridBagConstraints();
	labelConstraints.insets = new Insets( 2, 20, 2, 2 );
	labelConstraints.anchor = GridBagConstraints.EAST;
	labelConstraints.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
	labelConstraints.fill = GridBagConstraints.NONE;      //reset to default
	labelConstraints.weightx = 0.0;                       //reset to default
	
        GridBagConstraints fieldConstraints = new GridBagConstraints();
	fieldConstraints.insets = new Insets( 2, 2, 2, 20 );
	fieldConstraints.anchor = GridBagConstraints.WEST;
	fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;     //end row
	fieldConstraints.weightx = 1.0;
	
	JPanel loginPane = new JPanel();
	GridBagLayout gb = new GridBagLayout();
	loginPane.setLayout( gb );

	JLabel idLabel = new JLabel( "Username" );
	labelConstraints.insets = new Insets( 20, 20, 4, 4 );
	gb.setConstraints( idLabel, labelConstraints );
	loginPane.add( idLabel );
	idField = new JTextField( 15 );
	fieldConstraints.insets = new Insets( 20, 4, 4, 20 );
	gb.setConstraints( idField, fieldConstraints );
	loginPane.add( idField );

	JLabel passLabel = new JLabel( "Password" );
	labelConstraints.insets = new Insets( 4, 20, 4, 4 );
	gb.setConstraints( passLabel, labelConstraints );
	loginPane.add( passLabel );
	passField = new JPasswordField( 15 );
	fieldConstraints.insets = new Insets( 4, 4, 4, 20 );
	gb.setConstraints( passField, fieldConstraints );
	loginPane.add( passField );

	String [] dbs = PhotovaultSettings.getConfigurationNames();
	JLabel dbLabel = new JLabel( "Database" );
	gb.setConstraints( dbLabel, labelConstraints );
	loginPane.add( dbLabel );
	dbField = new JComboBox( dbs );
	gb.setConstraints( dbField, fieldConstraints );
	loginPane.add( dbField );

 	getContentPane().add( loginPane, BorderLayout.NORTH );

	// Create a pane for the buttols
	JButton okBtn = new JButton( "OK" );
	final LoginDlg dlg = this; 
	okBtn.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
// 		    setVisible( false );
		    app.login( dlg );
		}
	    } );
		    
	JButton cancelBtn = new JButton( "Cancel" );
	cancelBtn.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    System.exit( 0 );
		}
	    } );
	    
	JPanel buttonPane = new JPanel();
	buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
	buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	buttonPane.add(Box.createHorizontalGlue());
	buttonPane.add(okBtn);
	buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
	buttonPane.add(cancelBtn);
	getContentPane().add( buttonPane, BorderLayout.SOUTH );

	getRootPane().setDefaultButton( okBtn );

	setResizable( false );
	pack();
    }

    

    JTextField idField;
    JPasswordField passField;
    JComboBox dbField;
}