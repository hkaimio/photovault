/*
  Copyright (c) 2010 Harri Kaimio

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
  along with Photovault; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
*/

package org.photovault.swingui.conflict;

import java.awt.CardLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JOptionPane;
import org.photovault.command.ApplyChangeCommand;
import org.photovault.command.CommandException;
import org.photovault.command.CommandHandler;
import org.photovault.replication.Change;
import org.photovault.replication.ChangeDTO;
import org.photovault.replication.FieldConflictBase;
import org.photovault.replication.ValueFieldConflict;
import org.photovault.swingui.PhotoViewController;

/**
 * Dialog for resolving conflicts between two changes. Currently this dialog
 * creates a new change based on user selections and applies it via
 * {@link ApplyChangeCommand}. Later, it should be integrated more closely with
 * the {@link PhotoViewController} framework, but this would require that it too
 * used {@link ApplyChangeCommand}.
 * <p>
 * Also, currently this is a modal dialog but it probably should be an UI form
 * that can be used as part of other UI
 * @author Harri Kaimio
 * @since 0.6.0
 */
public class ResolveConflictDlg extends javax.swing.JDialog
        implements ConflictResolutionListener {

    /** Creates new form ResolveConflictDlg 
     * @param parent The parent frame
     * @param cmdHandler Command handler used to apply change to database
     * @param ch1 First of the changes to be merged
     * @param ch2 Second of the changes to be merged
     */
    public ResolveConflictDlg(java.awt.Frame parent, CommandHandler cmdHandler ,
            Change ch1, Change ch2) {
        super(parent, true );
        initComponents();
        this.ch1 = ch1;
        this.ch2 = ch2;
        this.cmdHandler = cmdHandler;
        initModel();
        okBtn.setEnabled( unresolved.isEmpty() );
    }

    Change ch1;
    Change ch2;
    /**
     * The merged change
     */
    Change merged;
    CommandHandler cmdHandler;
    /**
     * Names of the conflicting fields
     */
    List<String> fieldNames = new ArrayList<String>();
    /**
     * All conflicts betheen ch1 and ch2
     */
    List<FieldConflictBase> conflicts = new ArrayList<FieldConflictBase>();
    /**
     * Still unresolved conflicts
     */
    Set<FieldConflictBase> unresolved = new HashSet<FieldConflictBase>();
    /**
     * UI conponents that are used for resolving the conflicts (one per conflict)
     */
    List<DefaultConflictResolverForm> conflictForms = 
            new ArrayList<DefaultConflictResolverForm>();
    CardLayout conflictEditorLayout = new CardLayout();

    private void initModel() {
        conflictPane.setLayout( conflictEditorLayout );
        merged = ch1.merge( ch2 );
        for ( Object o: merged.getFieldConficts() ) {
            FieldConflictBase c = (FieldConflictBase) o;
            conflicts.add( c);
            unresolved.add( c );
            fieldNames.add( c.getFieldName() );
        }
        fieldList.setListData( fieldNames.toArray() );
        for ( FieldConflictBase c: conflicts ) {
            ValueFieldConflict vc = (ValueFieldConflict) c;
            DefaultConflictResolverForm f = new DefaultConflictResolverForm( vc );
            conflictForms.add( f );
            conflictPane.add( f, vc.getFieldName() );
            f.addResolutionListener( this );
        }
        conflictEditorLayout.first( conflictPane );
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        cancelBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        fieldList = new javax.swing.JList();
        conflictPane = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

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

        fieldList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        fieldList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                fieldListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(fieldList);

        conflictPane.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        javax.swing.GroupLayout conflictPaneLayout = new javax.swing.GroupLayout(conflictPane);
        conflictPane.setLayout(conflictPaneLayout);
        conflictPaneLayout.setHorizontalGroup(
            conflictPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 271, Short.MAX_VALUE)
        );
        conflictPaneLayout.setVerticalGroup(
            conflictPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 231, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(conflictPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(okBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(cancelBtn)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(conflictPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 235, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelBtn)
                    .addComponent(okBtn))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void fieldListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_fieldListValueChanged
        int selected = fieldList.getSelectedIndex();
        conflictEditorLayout.show( conflictPane, fieldNames.get( selected ) );
    }//GEN-LAST:event_fieldListValueChanged

    private void cancelBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelBtnActionPerformed
        setVisible( false );
        dispose();
    }//GEN-LAST:event_cancelBtnActionPerformed

    private void okBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okBtnActionPerformed
        ApplyChangeCommand cmd =
                new ApplyChangeCommand( new ChangeDTO( merged ) );
        try {
            cmdHandler.executeCommand( cmd );
        } catch ( CommandException e ) {
            JOptionPane.showMessageDialog( this,
                    "Error applying the change:\n" + e.getMessage() ,
                    "Error resolving conflict", JOptionPane.ERROR_MESSAGE );
        }
        setVisible( false );
        dispose();
    }//GEN-LAST:event_okBtnActionPerformed



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelBtn;
    private javax.swing.JPanel conflictPane;
    private javax.swing.JList fieldList;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton okBtn;
    // End of variables declaration//GEN-END:variables

    /**
     * Called by conflict resolver form to inform tha ta particular conflict is
     * resolved.
     * @param c The resolved conflict
     */
    public void conflictResolved( FieldConflictBase c ) {
        unresolved.remove( c );
        okBtn.setEnabled( unresolved.isEmpty() );
    }

}