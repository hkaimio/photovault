/*
 * IndexerSetupDlg.java
 *
 * Created on 13. maaliskuuta 2006, 14:40
 */

package org.photovault.swingui.indexer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import org.photovault.folder.PhotoFolder;
import org.photovault.swingui.PhotoFolderTree;

/**
 *
 * @author  harri
 */
public class IndexerSetupDlg extends javax.swing.JDialog {
    
    /** Creates new form IndexerSetupDlg */
    public IndexerSetupDlg(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        folderTree = new PhotoFolderTree();
        folderTree.setPreferredSize( folderTreePane.getSize() );
        folderTreePane.setLayout( new BorderLayout() );        
        folderTreePane.add( folderTree, BorderLayout.CENTER );
        PhotoFolder root = PhotoFolder.getRoot();
        folderTree.setSelected( root );        
    }
    
    PhotoFolderTree folderTree = null;
    
    /**
     Get the folder that user has set as parent for the tree that will be
     created for external volue folder hierarchy:
     @return The parent folder of <code>null</code> if user has selected that
     no folder hierarchy should be created.
     */
    public PhotoFolder getExtvolParentFolder() {
        PhotoFolder folder = null;
        if ( this.jCheckBox1.isSelected() ) {
            folder = folderTree.getSelected();
        }
        return folder;
    }
    
    /**
     Status flag indicating whether user has confirmed the setup by pressing 
     OK.
     */ 
    boolean setupConfirmed = false;
    
    /**
     Shows this dialog.
     @return true if user confirmed changes by pressong OK, false if she dismissed
     the dialog in some other way.
     */
    public boolean showDialog() {
        setupConfirmed = false;
        setVisible( true );
        return setupConfirmed;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        jCheckBox1 = new javax.swing.JCheckBox();
        cancelBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();
        folderTreePane = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        jCheckBox1.setSelected(true);
        jCheckBox1.setText("Create folders for indexed images in");
        jCheckBox1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jCheckBox1.setMargin(new java.awt.Insets(0, 0, 0, 0));

        cancelBtn.setText("Cancel");
        cancelBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelBtnActionPerformed(evt);
            }
        });

        okBtn.setText("OK");
        okBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okBtnActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout folderTreePaneLayout = new org.jdesktop.layout.GroupLayout(folderTreePane);
        folderTreePane.setLayout(folderTreePaneLayout);
        folderTreePaneLayout.setHorizontalGroup(
            folderTreePaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 297, Short.MAX_VALUE)
        );
        folderTreePaneLayout.setVerticalGroup(
            folderTreePaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 182, Short.MAX_VALUE)
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(okBtn)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(cancelBtn))
                    .add(jCheckBox1)
                    .add(folderTreePane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jCheckBox1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(folderTreePane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 22, Short.MAX_VALUE)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(cancelBtn)
                    .add(okBtn))
                .addContainerGap())
        );
        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     Called when user presses Cancel.
     @param evt The button press action event.
     */
    private void cancelBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelBtnActionPerformed
        setupConfirmed = false;
        setVisible( false );
    }//GEN-LAST:event_cancelBtnActionPerformed

    /**
     Called when user presses OK.
     @param evt The button press action event.
     */
    private void okBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okBtnActionPerformed
        setupConfirmed = true;
        setVisible( false );
    }//GEN-LAST:event_okBtnActionPerformed
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new IndexerSetupDlg(new javax.swing.JFrame(), true).setVisible(true);
            }
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelBtn;
    private javax.swing.JPanel folderTreePane;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JButton okBtn;
    // End of variables declaration//GEN-END:variables
    
}
