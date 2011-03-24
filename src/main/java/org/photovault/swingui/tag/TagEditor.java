/*
  Copyright (c) 2011 Harri Kaimio

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

package org.photovault.swingui.tag;

import javax.swing.DefaultComboBoxModel;
import org.photovault.imginfo.Tag;

/**
 * TagEditor is a simple UI conponent for editing a {@link Tag}
 * @author Harri Kaimio
 * @since 0.6.0
 */
public class TagEditor extends javax.swing.JPanel {

    /**
     * Data structure used for handling the categories for combo box
     */
    private static class TagCategoryInfo {
        TagCategoryInfo( String id, String name ) {
            this.id = id;
            this.name = name;
        }
        /**
         * Name of the category (shown to user)
         */
        String name;

        /**
         * ID of the categor (stored in database)
         */
        String id;

        public String toString() {
            return name;
        }
    }

    /** Creates new form TagEditor */
    public TagEditor() {
        initComponents();
        DefaultComboBoxModel categories = new DefaultComboBoxModel();
        categories.addElement( new TagCategoryInfo( Tag.TEXT_TAG, "Tag" ) );
        categories.addElement( new TagCategoryInfo( Tag.PERSON_TAG, "Person" ) );
        categoryField.setModel( categories );
    }

    /**
     * Get the tag after editing
     * @return A tag that matches UI status. Note! A new Tag object is created
     * every time!
     */
    public Tag getTag() {
        Object catValue = categoryField.getSelectedItem();
        TagCategoryInfo category = null;
        if ( catValue instanceof TagCategoryInfo ){
            category = (TagCategoryInfo) catValue;
        } else {
            String catStr = (String) catValue;
            category = new TagCategoryInfo( catStr, catStr );
        }
        String name = tagField.getText();
        return new Tag( category.id, name );
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        tagField = new javax.swing.JTextField();
        categoryField = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();

        jLabel1.setText("Tag");

        tagField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tagFieldActionPerformed(evt);
            }
        });

        categoryField.setEditable(true);
        categoryField.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Tag", "Person" }));

        jLabel2.setText("Category");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(categoryField, 0, 130, Short.MAX_VALUE)
                    .addComponent(tagField, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tagField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(categoryField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void tagFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tagFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tagFieldActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox categoryField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JTextField tagField;
    // End of variables declaration//GEN-END:variables

}
