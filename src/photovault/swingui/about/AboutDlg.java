/*
 * AboutDlg.java
 *
 * Created on 12. joulukuuta 2005, 21:55
 */

package photovault.swingui.about;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 *
 * @author  harri
 */
public class AboutDlg extends javax.swing.JDialog {
    
    /** Creates new form AboutDlg */
    public AboutDlg(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setupInfo();
    }
    
    void setupInfo() {
        URL buildPropertyURL = AboutDlg.class.getClassLoader().getResource( "buildinfo.properties");
        Properties prop = new Properties();
        try {
            InputStream is = buildPropertyURL.openStream();
            prop.load( is );
        } catch (IOException e ) {
            // Cannot read the properties for some reason.
            // Do nothing, use the default values instead.
        }
        
        svnrevLabel.setText( prop.getProperty( "svn.revision", "unknown" ) );   
        svnbranchLabel.setText( prop.getProperty( "svn.info.url", "unknown" ) );
        buildTimeLabel.setText( prop.getProperty( "build.time", "unknown" ) );
        builderLabel.setText( prop.getProperty( "build.user", "unknown" ) );
        
        // Set up the splash screen image
        URL splashImageURL = AboutDlg.class.getClassLoader().getResource( "splash.jpg" );
        Icon splash = new ImageIcon( splashImageURL );
        splashLabel.setIcon( splash );
        splashLabel.setText( null );
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        tabPane = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        splashLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        versionLabel = new javax.swing.JLabel();
        svnrevLabel = new javax.swing.JLabel();
        svnbranchLabel = new javax.swing.JLabel();
        buildTimeLabel = new javax.swing.JLabel();
        builderLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        setUndecorated(true);
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                formMouseClicked(evt);
            }
        });

        tabPane.setOpaque(true);
        tabPane.addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
                tabPaneAncestorAdded(evt);
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
            }
        });

        splashLabel.setText("jLabel1");
        splashLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                formMouseClicked(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(splashLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 396, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(splashLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 256, Short.MAX_VALUE)
        );
        tabPane.addTab("Photovault", jPanel1);

        jLabel1.setText("Version:");

        jLabel2.setText("SVN revision:");

        jLabel3.setText("SVN branch:");

        jLabel4.setText("Build time:");

        jLabel5.setText("Built by:");

        versionLabel.setText("jLabel6");

        svnrevLabel.setText("jLabel6");

        svnbranchLabel.setText("jLabel6");

        buildTimeLabel.setText("jLabel6");

        builderLabel.setText("jLabel6");

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel2Layout.createSequentialGroup()
                .add(37, 37, 37)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jLabel5)
                    .add(jLabel4)
                    .add(jLabel3)
                    .add(jLabel2)
                    .add(jLabel1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(versionLabel)
                    .add(svnrevLabel)
                    .add(svnbranchLabel)
                    .add(buildTimeLabel)
                    .add(builderLabel))
                .addContainerGap(256, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(versionLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(svnrevLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(svnbranchLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(buildTimeLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel5)
                    .add(builderLabel))
                .addContainerGap(151, Short.MAX_VALUE))
        );
        tabPane.addTab("Version info", jPanel2);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(tabPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 401, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(tabPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 284, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
        );
        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseClicked
        setVisible( false );
    }//GEN-LAST:event_formMouseClicked

    private void tabPaneAncestorAdded(javax.swing.event.AncestorEvent evt) {//GEN-FIRST:event_tabPaneAncestorAdded
// TODO add your handling code here:
    }//GEN-LAST:event_tabPaneAncestorAdded
    
    public void showDialog() {
        // Center the ialog on screen
        int w = getSize().width;
        int h = getSize().height;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width-w)/2;
        int y = (screenSize.height-h)/2;
        setLocation( x, y );
 
        setVisible( true );
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new AboutDlg(new javax.swing.JFrame(), true).setVisible(true);
            }
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel buildTimeLabel;
    private javax.swing.JLabel builderLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JLabel splashLabel;
    private javax.swing.JLabel svnbranchLabel;
    private javax.swing.JLabel svnrevLabel;
    private javax.swing.JTabbedPane tabPane;
    private javax.swing.JLabel versionLabel;
    // End of variables declaration//GEN-END:variables
    
}
