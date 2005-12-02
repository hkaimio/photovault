/*
 * DbSettingsDlg.java
 *
 * Created on 28. marraskuuta 2005, 21:45
 */

package photovault.swingui.db;

import imginfo.Volume;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import photovault.common.PVDatabase;
import photovault.common.PhotovaultSettings;

/**
 * This dialog is used to enter setup for a new Photovault database and to 
 * create it.
 * @author  Harri Kaimio
 */
public class DbSettingsDlg extends javax.swing.JDialog {
    
    /** Creates new form DbSettingsDlg */
    public DbSettingsDlg(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        dirChooser = new javax.swing.JFileChooser();
        jLabel1 = new javax.swing.JLabel();
        volumeDirFld = new javax.swing.JTextField();
        okBtn = new javax.swing.JButton();
        CancelBtn = new javax.swing.JButton();
        dirSelectBtn = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        nameFld = new javax.swing.JTextField();
        sqlDbPane = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        dbHostFld = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        dbNameFld = new javax.swing.JTextField();
        testDbBtn = new javax.swing.JButton();
        createDbBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Database settings");
        jLabel1.setText("Photo directory");

        okBtn.setText("OK");
        okBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                okBtnMouseClicked(evt);
            }
        });

        CancelBtn.setText("Cancel");
        CancelBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CancelBtnActionPerformed(evt);
            }
        });

        dirSelectBtn.setText("...");
        dirSelectBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dirSelectBtnActionPerformed(evt);
            }
        });

        jLabel4.setText("Database name");

        sqlDbPane.setBorder(javax.swing.BorderFactory.createTitledBorder("SQL database settings"));
        jLabel2.setText("SQL database host");

        jLabel3.setText("SQL database name");

        testDbBtn.setText("Test");
        testDbBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testDbBtnActionPerformed(evt);
            }
        });

        createDbBtn.setText("Create");
        createDbBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createDbBtnActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout sqlDbPaneLayout = new org.jdesktop.layout.GroupLayout(sqlDbPane);
        sqlDbPane.setLayout(sqlDbPaneLayout);
        sqlDbPaneLayout.setHorizontalGroup(
            sqlDbPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, sqlDbPaneLayout.createSequentialGroup()
                .add(sqlDbPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel2)
                    .add(jLabel3))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 16, Short.MAX_VALUE)
                .add(sqlDbPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(dbNameFld)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, dbHostFld, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 238, Short.MAX_VALUE)))
            .add(org.jdesktop.layout.GroupLayout.TRAILING, sqlDbPaneLayout.createSequentialGroup()
                .add(226, 226, 226)
                .add(createDbBtn)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(testDbBtn))
        );
        sqlDbPaneLayout.setVerticalGroup(
            sqlDbPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.LEADING, sqlDbPaneLayout.createSequentialGroup()
                .add(sqlDbPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(dbHostFld, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(sqlDbPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(dbNameFld, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(17, 17, 17)
                .add(sqlDbPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(testDbBtn)
                    .add(createDbBtn))
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap(243, Short.MAX_VALUE)
                .add(okBtn, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 59, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(CancelBtn)
                .add(13, 13, 13))
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .add(sqlDbPane))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(31, 31, 31)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel4)
                            .add(jLabel1))
                        .add(23, 23, 23)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                                .add(volumeDirFld, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 215, Short.MAX_VALUE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(dirSelectBtn, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(nameFld, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(nameFld, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(dirSelectBtn)
                    .add(volumeDirFld, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(sqlDbPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(79, 79, 79)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(okBtn)
                    .add(CancelBtn))
                .addContainerGap())
        );
        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void createDbBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createDbBtnActionPerformed
        JOptionPane.showMessageDialog( this, "Database connection testing\nnot yet implemented", 
                "Create database", JOptionPane.ERROR_MESSAGE );
    }//GEN-LAST:event_createDbBtnActionPerformed

    private void testDbBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testDbBtnActionPerformed
        JOptionPane.showMessageDialog( this, "Database connection testing\nnot yet implemented", 
                "Create database", JOptionPane.ERROR_MESSAGE );
    }//GEN-LAST:event_testDbBtnActionPerformed

    private void CancelBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CancelBtnActionPerformed
        retval = CANCEL_OPTION;
    }//GEN-LAST:event_CancelBtnActionPerformed

    /**
     * Display a file selection dialog for selecting the volume folder
     */
    private void dirSelectBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dirSelectBtnActionPerformed
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode( fc.DIRECTORIES_ONLY );
        
        /* Set the starting directory for file chooser. If a directory name has been
         * entered into the box, use it as an starting point. Otherwise use 
         * user's home directory
         */
        String dirname = volumeDirFld.getText();
        if ( dirname.length() == 0 ) {
            dirname = System.getProperty( "user.home" );
        }
        File startingDir = new File( dirname );
        fc.setCurrentDirectory( startingDir );
        
        if ( fc.showDialog( this, "OK" ) == JFileChooser.APPROVE_OPTION ) {
            File volumeDir = fc.getSelectedFile();
            this.volumeDirFld.setText( volumeDir.getAbsolutePath() );
        }
    }//GEN-LAST:event_dirSelectBtnActionPerformed

    private void okBtnMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_okBtnMouseClicked
        createDatabase();
        retval = APPROVE_OPTION;
        setVisible( false );
    }//GEN-LAST:event_okBtnMouseClicked
    
    /**
     * Error happened during dialog processing
     */
    public static final int ERROR_OPTION = 0;

    /**
     * User approved the dialog
     */
    public static final int APPROVE_OPTION = 1;
    
    /**
     * User canceled the dialog
     */
    public static final int CANCEL_OPTION = 2;
    
    /**
     * Return value for the dialog.
     */
    int retval;
    
    /**
     * Shows the dialog and waiths until user either approves or cancels it
     * @return Reason for dialog dismissal, either
     * <ul>
     * <li> APPROVE_OPTION - the dialog was approved
     * <li> CANCEL_OPTION - user canceled the dialog
     * <li> ERROR_OPTION - SOme kind of error happened during the dialog
     * </ul>
     *
     */
    public int showDialog( ) {
        // Center the dialog on screen
        int w = getSize().width;
        int h = getSize().height;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width-w)/2;
        int y = (screenSize.height-h)/2;
        setLocation( x, y );
        
        retval = ERROR_OPTION;
        setVisible( true );
        return retval;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                DbSettingsDlg dlg = new DbSettingsDlg(new javax.swing.JFrame(), true);
                dlg.showDialog();
            }
        });
    }

    private void createDatabase() {
        // Ask for the admin password
        AdminLoginDlg loginDlg = new AdminLoginDlg( this, true );
        if ( loginDlg.showDialog() == AdminLoginDlg.LOGIN_DLG_OK ) {
            PVDatabase db = new PVDatabase();
            String user = loginDlg.getUsername();
            String passwd = loginDlg.getPasswd();
            db.setDbName( dbNameFld.getText() );
            db.setDbHost( dbHostFld.getText() );
            db.setName( nameFld.getText() );
            Volume vol = new Volume( "defaultVolume", volumeDirFld.getText() );
            db.addVolume( vol );
            db.createDatabase( user, passwd );
            
            PhotovaultSettings settings = PhotovaultSettings.getSettings();
            settings.addDatabase( db );
            settings.saveConfig();
            
            JOptionPane.showMessageDialog( this, "Database " + nameFld.getText() + " created succesfully",
                    "Database created", JOptionPane.INFORMATION_MESSAGE );
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton CancelBtn;
    private javax.swing.JButton createDbBtn;
    private javax.swing.JTextField dbHostFld;
    private javax.swing.JTextField dbNameFld;
    private javax.swing.JFileChooser dirChooser;
    private javax.swing.JButton dirSelectBtn;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JTextField nameFld;
    private javax.swing.JButton okBtn;
    private javax.swing.JPanel sqlDbPane;
    private javax.swing.JButton testDbBtn;
    private javax.swing.JTextField volumeDirFld;
    // End of variables declaration//GEN-END:variables
    
}
