/*
  Copyright (c) 2006-2007 Harri Kaimio
  
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

package org.photovault.swingui.color;

import com.sun.jdori.common.query.tree.ThisExpr;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.photovault.common.PhotovaultException;
import org.photovault.dcraw.ColorProfileDesc;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.dcraw.RawImage;
import org.photovault.dcraw.RawImageChangeEvent;
import org.photovault.dcraw.RawImageChangeListener;
import org.photovault.dcraw.RawSettingsFactory;
import org.photovault.imginfo.FuzzyDate;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoNotFoundException;
import org.photovault.swingui.JAIPhotoViewer;
import org.photovault.swingui.PhotoInfoController;
import org.photovault.swingui.PhotoInfoView;
import org.photovault.swingui.PhotoViewChangeEvent;
import org.photovault.swingui.PhotoViewChangeListener;
import org.photovault.swingui.RawPhotoView;

/**
 * Dialog box for altering color settings of a photo. Currently only
 * raw images are supporter but in future support for normal image files
 * should be added.
 * @author Harri Kaimio
 */
public class ColorSettingsDlg extends javax.swing.JDialog 
        implements RawImageChangeListener, RawPhotoView, PhotoViewChangeListener {

    static org.apache.log4j.Logger log = 
            org.apache.log4j.Logger.getLogger( ColorSettingsDlg.class.getName() );
    
    /**
     * Creates new form ColorSettingsDlg
     * @param parent Parent frame of this dialog
     * @param modal Whether the dialog is displayed as modal
     * @param photos Array of the photos that will be edited
     */
    public ColorSettingsDlg(java.awt.Frame parent, boolean modal, PhotoInfo[] photos ) {
        super(parent, modal);
        initComponents();
        ctrl = new PhotoInfoController();
        ctrl.setPhotos( photos );
        ctrl.setView( this );
    }
    
    /**
     * Creates new form ColorSettingsDlg
     * @param parent Parent frame of this dialog
     * @param modal Whether the dialog is displayed as modal
     * @param photo The photo that will be edited
     */
    public ColorSettingsDlg(java.awt.Frame parent, boolean modal, PhotoInfo photo ) {
        super(parent, modal);
        initComponents();
        ctrl = new PhotoInfoController();
        ctrl.setPhoto( photo );
        ctrl.setView( this );
    }
    
    /**
     Controller for the photos that will be edited
     */
    PhotoInfoController ctrl = null;
    RawConversionSettings rawSettings = null;
    
    /**
     Saves all changes made in the dialog to model.
     */
    protected void applyChanges() {
        try {
            ctrl.save();
            photoChanged = true;
        } catch (PhotovaultException ex) {
            JOptionPane.showMessageDialog( this, 
                    "Error while applying changes:\n" + ex.getMessage(),
                    "Error saving changes", JOptionPane.ERROR_MESSAGE );
            log.error( "Error while applying changes: " + ex.getMessage() );
        }
    }
    
    /**
     Discard changes made in the dialog & reload model values
     */
    protected void discardChanges() {
        ctrl.discard();
    }
    
    static class ModelValueAnnotation extends JPanel {
        public ModelValueAnnotation( Color color ) {
            super();
            this.color = color;
            Polygon p = new Polygon();
            p.addPoint( 5, 0 );
            p.addPoint( 9, 5 );
            p.addPoint( 5, 10 );
            p.addPoint( 1, 5 );
            shape = p;
        }
        
        Color color;
        Polygon shape;
        
        public Dimension getPreferredSize() {
            return new Dimension ( 10, 10 );
        }
        
        public void paint( Graphics g ) {
            Graphics g2 = g.create();
            g2.setColor( color );
            g2.fillPolygon( shape );
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        fieldSliderCombo1 = new org.photovault.swingui.color.FieldSliderCombo();
        closeBtn = new javax.swing.JButton();
        discardBtn = new javax.swing.JButton();
        applyBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();
        rawSettingsPane = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        colorProfileCombo = new javax.swing.JComboBox();
        newProfileBtn = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        hlightCompSlider = new org.photovault.swingui.color.FieldSliderCombo();
        evCorrSlider = new org.photovault.swingui.color.FieldSliderCombo();
        Hashtable sliderLabels = new Hashtable();
        sliderLabels.put( new Double( -2.0 ), new JLabel("-2") );
        sliderLabels.put( new Double( -1.0 ), new JLabel("-1") );
        sliderLabels.put( new Double(  0 ), new JLabel("0") );
        sliderLabels.put( new Double( 1.0 ), new JLabel("1") );
        sliderLabels.put( new Double( 2.0 ), new JLabel("2") );
        evCorrSlider.setLabelTable( sliderLabels );
        blackLevelSlider = new org.photovault.swingui.color.FieldSliderCombo();
        ctempSlider = new org.photovault.swingui.color.FieldSliderCombo();
        greenGainSlider = new org.photovault.swingui.color.FieldSliderCombo();
        Hashtable greenGainLabels = new Hashtable();
        greenGainLabels.put( new Double( -1.0 ), new JLabel("-1") );
        greenGainLabels.put( new Double( 0.0 ), new JLabel("0") );
        greenGainLabels.put( new Double( 1.0 ), new JLabel("+1") );
        greenGainSlider.setLabelTable( greenGainLabels );
        rawHistogramPane = rawHistogramPane = new HistogramPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Colors");
        closeBtn.setText("Close");
        closeBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeBtnActionPerformed(evt);
            }
        });

        discardBtn.setText("Discard");
        discardBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                discardBtnActionPerformed(evt);
            }
        });

        applyBtn.setText("Apply");
        applyBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                applyBtnActionPerformed(evt);
            }
        });

        okBtn.setText("OK");
        okBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okBtnActionPerformed(evt);
            }
        });

        jLabel1.setText("EV correction");

        jLabel2.setText("Color Temperature");

        jLabel3.setText("Compress highlights");

        jLabel4.setText("Green");

        jLabel5.setText("Black level");

        colorProfileCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "None" }));
        colorProfileCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colorProfileComboActionPerformed(evt);
            }
        });

        newProfileBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/folder_expanded_icon.png")));
        newProfileBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newProfileBtnActionPerformed(evt);
            }
        });

        jLabel6.setText("Color profile");

        hlightCompSlider.setMajorTickSpacing(1.0);
        hlightCompSlider.setMaximum(2.0);
        hlightCompSlider.setMinimum(-1.0);
        hlightCompSlider.setValue(0.0);
        hlightCompSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                hlightCompSliderStateChanged(evt);
            }
        });

        evCorrSlider.setFractionDigits(2);
        evCorrSlider.setMajorTickSpacing(1.0);
        evCorrSlider.setMaximum(2.0);
        evCorrSlider.setMinimum(-2.0);
        evCorrSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                evCorrSliderStateChanged(evt);
            }
        });

        blackLevelSlider.setFractionDigits(0);
        blackLevelSlider.setMajorTickSpacing(500.0);
        blackLevelSlider.setMaximum(1000.0);
        blackLevelSlider.setMinimum(-500.0);
        blackLevelSlider.setValue(0.0);
        blackLevelSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                blackLevelSliderStateChanged(evt);
            }
        });

        ctempSlider.setFractionDigits(0);
        ctempSlider.setMajorTickSpacing(2000.0);
        ctempSlider.setMaximum(12000.0);
        ctempSlider.setMinimum(2000.0);
        ctempSlider.setValue(5500.0);
        ctempSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                ctempSliderStateChanged(evt);
            }
        });

        greenGainSlider.setFractionDigits(2);
        greenGainSlider.setMajorTickSpacing(1.0);
        greenGainSlider.setMinimum(-1.0);
        greenGainSlider.setValue(0.0);
        greenGainSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                greenGainSliderStateChanged(evt);
            }
        });

        org.jdesktop.layout.GroupLayout rawSettingsPaneLayout = new org.jdesktop.layout.GroupLayout(rawSettingsPane);
        rawSettingsPane.setLayout(rawSettingsPaneLayout);
        rawSettingsPaneLayout.setHorizontalGroup(
            rawSettingsPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(rawSettingsPaneLayout.createSequentialGroup()
                .add(rawSettingsPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(rawSettingsPaneLayout.createSequentialGroup()
                        .add(62, 62, 62)
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(evCorrSlider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 374, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(rawSettingsPaneLayout.createSequentialGroup()
                        .addContainerGap()
                        .add(rawSettingsPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(jLabel3)
                            .add(jLabel5)
                            .add(jLabel2)
                            .add(jLabel4)
                            .add(jLabel6))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(rawSettingsPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, greenGainSlider, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, blackLevelSlider, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
                            .add(hlightCompSlider, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, ctempSlider, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
                            .add(rawSettingsPaneLayout.createSequentialGroup()
                                .add(colorProfileCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 317, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 23, Short.MAX_VALUE)
                                .add(newProfileBtn, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 34, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap())
        );
        rawSettingsPaneLayout.setVerticalGroup(
            rawSettingsPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(rawSettingsPaneLayout.createSequentialGroup()
                .addContainerGap()
                .add(rawSettingsPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(evCorrSlider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(rawSettingsPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(hlightCompSlider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel3))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(rawSettingsPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(blackLevelSlider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel5))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(rawSettingsPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel2)
                    .add(ctempSlider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(rawSettingsPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(greenGainSlider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel4))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(rawSettingsPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(colorProfileCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(newProfileBtn, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 24, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel6))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        rawHistogramPane.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        org.jdesktop.layout.GroupLayout rawHistogramPaneLayout = new org.jdesktop.layout.GroupLayout(rawHistogramPane);
        rawHistogramPane.setLayout(rawHistogramPaneLayout);
        rawHistogramPaneLayout.setHorizontalGroup(
            rawHistogramPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 537, Short.MAX_VALUE)
        );
        rawHistogramPaneLayout.setVerticalGroup(
            rawHistogramPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 139, Short.MAX_VALUE)
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(rawSettingsPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                            .add(okBtn)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            .add(applyBtn)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            .add(discardBtn)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            .add(closeBtn)))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, rawHistogramPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(rawSettingsPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(rawHistogramPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 7, Short.MAX_VALUE)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(closeBtn)
                    .add(discardBtn)
                    .add(applyBtn)
                    .add(okBtn))
                .addContainerGap())
        );
        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void greenGainSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_greenGainSliderStateChanged
        double greenEv = greenGainSlider.getValue();
        double newGain = Math.pow( 2, greenEv );
        if ( Math.abs( newGain - this.greenGain ) > 0.005 ) {
            greenGain= newGain;
            ctrl.viewChanged( this, PhotoInfoController.RAW_GREEN );
            if ( rawSettings != null ) {
                RawSettingsFactory f = new RawSettingsFactory( rawSettings );
                f.setGreenGain( newGain );
                try {
                    rawSettings = f.create();
                } catch (PhotovaultException ex) {
                    log.error( "Error setting color temp: " + ex.getMessage() );
                }
                firePreviewChangeEvent( new RawSettingsPreviewEvent(
                        this, ctrl.getPhotos(), rawSettings ) );
                reloadHistogram();
            }
        }
        
    }//GEN-LAST:event_greenGainSliderStateChanged
    
    /**
     * Color temperature slider value was changed
     * @param evt Event describing the change
     */
    private void ctempSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_ctempSliderStateChanged
        double newCTemp = ctempSlider.getValue();
        if ( Math.abs( newCTemp - this.colorTemp ) > 10 ) {
            colorTemp= newCTemp;
            ctrl.viewChanged( this, PhotoInfoController.RAW_CTEMP );
            if ( rawSettings != null ) {
                RawSettingsFactory f = new RawSettingsFactory( rawSettings );
                f.setColorTemp( newCTemp );
                try {
                    rawSettings = f.create();
                } catch (PhotovaultException ex) {
                    log.error( "Error setting color temp: " + ex.getMessage() );
                }
                firePreviewChangeEvent( new RawSettingsPreviewEvent(
                        this, ctrl.getPhotos(), rawSettings ) );
                reloadHistogram();
            }
        }
    }//GEN-LAST:event_ctempSliderStateChanged

    private void blackLevelSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_blackLevelSliderStateChanged
        int newBlack = (int) blackLevelSlider.getValue();
        if ( Math.abs( newBlack - black ) > 0.05 ) {
            black = newBlack;
            ctrl.viewChanged( this, PhotoInfoController.RAW_BLACK_LEVEL );
            if ( rawSettings != null ) {
                RawSettingsFactory f = new RawSettingsFactory( rawSettings );
                f.setBlack( newBlack );
                try {
                    rawSettings = f.create();
                } catch (PhotovaultException ex) {
                    log.error( "Error setting black: " + ex.getMessage() );
                }
                firePreviewChangeEvent( new RawSettingsPreviewEvent(
                        this, ctrl.getPhotos(), rawSettings ) );
                reloadHistogram();
            }
        }

    }//GEN-LAST:event_blackLevelSliderStateChanged

    private void evCorrSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_evCorrSliderStateChanged
        double newEv = evCorrSlider.getValue();
        if ( Math.abs( newEv - evCorr ) > 0.05 ) {
            evCorr = newEv;
            ctrl.viewChanged( this, PhotoInfoController.RAW_EV_CORR );
            if ( rawSettings != null ) {
                RawSettingsFactory f = new RawSettingsFactory( rawSettings );
                f.setEvCorr( newEv );
                try {
                    rawSettings = f.create();
                } catch (PhotovaultException ex) {
                    log.error( "Error setting EV correction: " + ex.getMessage() );
                }
                firePreviewChangeEvent( new RawSettingsPreviewEvent(
                        this, ctrl.getPhotos(), rawSettings ) );
                reloadHistogram();
            }
        }
    }//GEN-LAST:event_evCorrSliderStateChanged

    private void hlightCompSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_hlightCompSliderStateChanged
        double newHlightComp = hlightCompSlider.getValue();
        if ( (Math.abs( newHlightComp - this.hlightComp ) > 0.001 ) ) {
            this.hlightComp = newHlightComp;
            ctrl.viewChanged( this, PhotoInfoController.RAW_HLIGHT_COMP );
            if ( rawSettings != null ) {
                RawSettingsFactory f = new RawSettingsFactory( rawSettings );
                f.setHlightComp( newHlightComp );
                try {
                    rawSettings = f.create();
                } catch (PhotovaultException ex) {
                    log.error( "Error setting green gain: " + ex.getMessage() );
                }
                firePreviewChangeEvent( new RawSettingsPreviewEvent(
                        this, ctrl.getPhotos(), rawSettings ) );
                reloadHistogram();
            }
        }
        
    }//GEN-LAST:event_hlightCompSliderStateChanged
    
    private void colorProfileComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_colorProfileComboActionPerformed
        log.debug( "enter colorProfileComboActionPerformed" );
        int selectedId = colorProfileCombo.getSelectedIndex();
        if ( profiles == null || selectedId < 0 || selectedId >= profiles.length ) {
            return;
        }
        ColorProfileDesc p = profiles[selectedId];
        if ( p != profile ) {
            profile = p;
            ctrl.viewChanged( this, PhotoInfoController.RAW_COLOR_PROFILE );
            if ( rawSettings != null ) {
                RawSettingsFactory f = new RawSettingsFactory( rawSettings );
                f.setColorProfile( p );
                try {
                    rawSettings = f.create();
                } catch (PhotovaultException ex) {
                /*
                 This should never occur in this case (it it thrown only if
                 color balance settings are incomplete, and the seed object could
                 not exist at all if that was the case)
                 */
                    log.error( "Unexpected error while setting color profile: " +
                            ex.getMessage() );
                }
                firePreviewChangeEvent( new RawSettingsPreviewEvent(
                        this, ctrl.getPhotos(), rawSettings ) );
            }
        }
        log.debug( "exit colorProfileComboActionPerformed" );        
    }//GEN-LAST:event_colorProfileComboActionPerformed

    private void newProfileBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newProfileBtnActionPerformed
        JFileChooser fc = new JFileChooser();
        int retval = fc.showDialog( this, "OK" );
        if ( retval == JFileChooser.APPROVE_OPTION ) {
            File profileFile = fc.getSelectedFile();
            ColorProfileDesc.CreateProfile createProfAction = 
                    new ColorProfileDesc.CreateProfile( profileFile, profileFile.getName(), "" );
            ColorProfileDesc p = createProfAction.execute();
            updateColorProfiles();
        }
    }//GEN-LAST:event_newProfileBtnActionPerformed


    /**
     * Close button was pressed. Close the window
     * @param evt The ButtonEvent
     */
    private void closeBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeBtnActionPerformed
        discardChanges();
        setVisible( false );
    }//GEN-LAST:event_closeBtnActionPerformed

    /**
     * Discard button was pressed. Instruct controller to discard changes made
     * @param evt The ButtonEvent
     */
    private void discardBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_discardBtnActionPerformed
        discardChanges();
    }//GEN-LAST:event_discardBtnActionPerformed

    /**
     * Apply button was pressed. Save all changes made.
     * @param evt The ButtonEvent
     */
    private void applyBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_applyBtnActionPerformed
        applyChanges();
    }//GEN-LAST:event_applyBtnActionPerformed

    /**
     * OK button was pressed. Save changes & close window
     * @param evt Event
     */
    private void okBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okBtnActionPerformed
        applyChanges();
        setVisible( false );
    }//GEN-LAST:event_okBtnActionPerformed

    /**
     * Exposure slider value was changed
     * @param evt The event
     */

    RawImage rawPreviewImage = null;
    RawImage previewCtrlImage = null;
    PhotoInfo previewCtrlPhoto = null;
    
    /**
     * Set the preview image into which the changes done in dialog are
     * immediately applied.
     * @param ri Test image
     */
    public void setPreview( RawImage ri ) {
        rawPreviewImage = ri;
        if ( ri != null ) {
            ((HistogramPane) rawHistogramPane).setHistogram( ri.getHistogramBins() );
            ((HistogramPane) rawHistogramPane).setTransferGraph( ri.getGammaLut() );
        }
    }
    
    /**
     Send event to all preview windows that the raw settings have been changed.
     @param e Event that describes the change
     */
    void firePreviewChangeEvent( RawSettingsPreviewEvent e ) {
        if ( previewCtrl != null ) {
            previewCtrl.previewRawSettingsChanged( e );
        }
        
    }
    
    /**
     Reload the histogram data from RawImage displayed in preview control if
     the image matches to current model. If this is not the case, disable 
     histogram.
     */
    void reloadHistogram() {
        if ( previewCtrl == null ) {
            return;
        }
        PhotoInfo[] model = ctrl.getPhotos();
        PhotoInfo photo = previewCtrl.getPhoto();
        if ( photo != null &&
                model != null &&
                model.length == 1 &&
                model[0] == photo) {
            byte[] lut = previewCtrl.getRawConversionLut();
            ((HistogramPane)rawHistogramPane).setTransferGraph( lut );
            ((HistogramPane)rawHistogramPane).setHistogram(
                    previewCtrl.getRawImageHistogram() );
        } else {
            /* Preview control is not displaying image that matches to the current
               model, so we do not have histogram data available.
             */
            ((HistogramPane)rawHistogramPane).setTransferGraph( null );
            ((HistogramPane)rawHistogramPane).setHistogram( null );
            
        }
    }
    
    JAIPhotoViewer previewCtrl = null;
    
    public void setPreviewControl( JAIPhotoViewer viewer ) {
        previewCtrl = viewer;
        previewCtrl.addViewChangeListener( this );
        reloadHistogram();
    }

    /**
     Set the photo whose settings will be changed
     */
    public void setPhoto( PhotoInfo p ) {
        ctrl.setPhoto( p );        
        rawPreviewImage = null;
        checkIsRawPhoto();
    }
    
    /**
     Set the photos whose settings will be changed
     */
    public void setPhotos( PhotoInfo[] p ) {
        ctrl.setPhotos( p );        
        rawPreviewImage = null;
        checkIsRawPhoto();
        reloadHistogram();
        
    }
    
    
    /**
     Checks if the model supports raw conversion settings and disables
     or enables controls based on this. Raw settings are only allowed if all 
     photos controlled bu ctrl are raw photos.
     */
    void checkIsRawPhoto() {
        PhotoInfo[] photos = ctrl.getPhotos();
        boolean isRaw = true;
        if ( photos != null ) {
            /*
             If any of the photos is not a raw image we cannot use raw controls
             */
            for ( int n = 0; n < photos.length; n++ ) {
                if ( photos[n].getRawSettings() == null ) {
                    isRaw = false;
                    break;
                }                
            }
        } else {
            isRaw = false;
        }
        setRawControlsEnabled( isRaw );
    }

    /**
     Enable or disble all controls that affect raw conversion settings
     @param enable <code>true</code> if the controls should be enabled
     */
    void setRawControlsEnabled( boolean enable ) {
        evCorrSlider.setEnabled( enable );
        hlightCompSlider.setEnabled( enable );
        ctempSlider.setEnabled( enable );
        greenGainSlider.setEnabled( enable );
        blackLevelSlider.setEnabled( enable );
        colorProfileCombo.setEnabled( enable );
        newProfileBtn.setEnabled( enable );
    }
    
    /**
     Array of color profiles that match to the items in colorProfileCombo
     combo box.
     */
    ColorProfileDesc profiles[] = null;
    
    /**
     * Update colorProfileCombo list to include all known color profiles. Also the
     * profiles array is recreated.
     */
    void updateColorProfiles() {
        Collection p = ColorProfileDesc.getAllProfiles();
        ColorProfileDesc[] profilesUnsorted = new ColorProfileDesc[0];
        if ( p != null ) {
            profilesUnsorted = (ColorProfileDesc[]) p.toArray( profilesUnsorted );
        }
        /*
         HACK:
         Removing all items & adding None causes an action event from the combo 
         box. By setting profiles to null berfore that we ensure that the event 
         can not be interpreted so that use has selected "None" color profile.
         */
        profiles = null;
        colorProfileCombo.removeAllItems();
        colorProfileCombo.addItem( "None" );
        profiles = new ColorProfileDesc[ profilesUnsorted.length + 1 ];
        profiles[0] = null;
        for ( int n = 0; n < profilesUnsorted.length; n++ ) {
            profiles[n+1] = profilesUnsorted[n];
            colorProfileCombo.addItem( profilesUnsorted[n].getName() );
        }
    }
    
    boolean photoChanged = false;
    
    /**
       Shows the dialog.
       @return True if the dialog modified the photo data, false otherwise.
    */
	
    public boolean showDialog() {
	photoChanged = false;
	setVisible( true );
	return photoChanged;
    }
    
    /**
     * callback that is called is the preview image changes.
     * @param ev The change event
     */
    public void rawImageSettingsChanged(RawImageChangeEvent ev) {
        
    }
    
    /*
     PhotoInfoView implementation
     */

    public void setPhotographer(String newValue) {
    }

    public String getPhotographer() {
        return null;
    }

    public void setPhotographerMultivalued(boolean mv) {
    }

    public void setFuzzyDate(FuzzyDate newValue) {
    }

    public FuzzyDate getFuzzyDate() {
        return null;
    }

    public void setFuzzyDateMultivalued(boolean mv) {
    }

    public void setQuality(Number quality) {
    }

    public Number getQuality() {
        return null;
    }

    public void setQualityMultivalued(boolean mv) {
    }

    public void setShootPlace(String newValue) {
    }

    public String getShootPlace() {
        return null;
    }

    public void setShootPlaceMultivalued(boolean mv) {
    }

    public void setFocalLength(Number newValue) {
    }

    public Number getFocalLength() {
        return null;
    }

    public void setFocalLengthMultivalued(boolean mv) {
    }

    public void setFStop(Number newValue) {
    }

    public Number getFStop() {
        return null;
    }

    public void setFStopMultivalued(boolean mv) {
    }

    public void setCamera(String newValue) {
    }

    public String getCamera() {
        return null;
    }

    public void setCameraMultivalued(boolean mv) {
    }

    public void setFilm(String newValue) {
    }

    public String getFilm() {
        return null;
    }

    public void setFilmMultivalued(boolean mv) {
    }

    public void setLens(String newValue) {
    }

    public String getLens() {
        return null;
    }

    public void setLensMultivalued(boolean mv) {
    }

    public void setDescription(String newValue) {
    }

    public String getDescription() {
        return null;
    }

    public void setDescriptionMultivalued(boolean mv) {
    }

    public void setTechNote(String newValue) {
    }

    public String getTechNote() {
        return null;
    }

    public void setTechNoteMultivalued(boolean mv) {
    }

    public void setShutterSpeed(Number newValue) {
    }

    public Number getShutterSpeed() {
        return null;
    }

    public void setShutterSpeedMultivalued(boolean mv) {
    }

    public void setFilmSpeed(Number newValue) {
    }

    public Number getFilmSpeed() {
        return null;
    }

    public void setFilmSpeedMultivalued(boolean mv) {
    }

    public void setFolderTreeModel(TreeModel model) {
    }
 
    public void setRawSettings(RawConversionSettings rawSettings) {
        this.rawSettings = rawSettings;
        if ( rawSettings != null ) {
            double evCorr = rawSettings.getEvCorr();
            evCorrSlider.setValue( evCorr );
            double comp = rawSettings.getHighlightCompression();
            hlightCompSlider.setValue( comp );
            blackLevelSlider.setValue( rawSettings.getBlack() );
            double colorTemp = rawSettings.getColorTemp();
            ctempSlider.setValue( (int) colorTemp );
            double g = rawSettings.getGreenGain();
            double logGreen = Math.log( g ) / Math.log(2);
            greenGainSlider.setValue( logGreen );
            setupColorProfile();
            firePreviewChangeEvent( new RawSettingsPreviewEvent(
                    this, ctrl.getPhotos(), rawSettings ) );
        } 
    }

    /**
     Set the colorProfileCombo to display color profile defined in current raw 
     settings or "None" if raw settings are <code>null</code>.
     */
    private void setupColorProfile() {
        if ( profiles == null ) {
            updateColorProfiles();
        }
        if ( profile != null ) {
            int settingsIdx = -1;
            for ( int n = 0; n < profiles.length ; n++ ) {
                if ( profiles[n] == profile ) {
                    settingsIdx = n;
                    break;
                }
            }
            if ( settingsIdx >= 0 ) {
                colorProfileCombo.setSelectedIndex( settingsIdx );
            } else {
                colorProfileCombo.setSelectedIndex( 0 );                
            }            
        } else {
            colorProfileCombo.setSelectedIndex( 0 );            
        }                    
    }
    
    public void setRawSettingsMultivalued(boolean mv) {
    }

    public RawConversionSettings getRawSettings() {
        return rawSettings;
    }

    public void expandFolderTreePath(TreePath path) {
    }

    int black = 0;
    
    public void setRawBlack(int black) {
        this.black = black;
        blackLevelSlider.setValue( (double) black );
    }

    public void setRawBlackMultivalued(boolean multivalued, Object[] values ) {
        if ( values != null && values.length > 1  ) {
            double[] annotations = new double[values.length];
            for ( int n = 0; n < values.length ; n++ ) {
                annotations[n] = ((Number)values[n]).doubleValue();
            }
            blackLevelSlider.setAnnotations( annotations );
        } else {
            // restore the normal label table without any extra annotations
            blackLevelSlider.setAnnotations( null );
        }
        blackLevelSlider.setMultivalued( multivalued );
    }

    public int getRawBlack() {
        return (int) blackLevelSlider.getValue();
    }

    double evCorr = 0.0;
    public void setRawEvCorr(double evCorr) {
        this.evCorr = evCorr;
        evCorrSlider.setValue( evCorr );        
    }

    public void setRawEvCorrMultivalued(boolean multivalued, Object[] values ) {
        if ( values != null && values.length > 1  ) {
            annotateSlider( evCorrSlider, values );
        } else {
            // restore the normal label table without any extra annotations
            evCorrSlider.setAnnotations( null );
        }
        evCorrSlider.setMultivalued( multivalued );
    }
    
    public double getRawEvCorr() {
        return evCorrSlider.getValue();
    }

    double hlightComp = 0.0;
    Hashtable hlightSliderLabels = null;
    
    public void setRawHlightComp(double comp) {
        this.hlightComp = comp;
        hlightCompSlider.setValue( comp );
    }

    public void setRawHlightCompMultivalued(boolean multivalued, Object[] values ) {
        if ( values != null && values.length > 1  ) {
            annotateSlider( hlightCompSlider, values );
        } else {
            // restore the normal label table without any extra annotations
            hlightCompSlider.setAnnotations( null );
        }
        hlightCompSlider.setMultivalued( multivalued );
    }

    public double getRawHlightComp() {
        double comp = hlightCompSlider.getValue();
        return comp;
    }

    double colorTemp = 0.0;
    
    public void setRawColorTemp(double ct) {
        colorTemp = ct;
        ctempSlider.setValue( ct );
    }

    public void setRawColorTempMultivalued(boolean multivalued, Object[] values ) {
        if ( values != null && values.length > 1  ) {
            annotateSlider( ctempSlider, values );
        } else {
            // restore the normal label table without any extra annotations
            ctempSlider.setAnnotations( null );
        }
        ctempSlider.setMultivalued( multivalued );
    }

    public double getRawColorTemp() {
        return (double) ctempSlider.getValue();
    }

    double greenGain = 1.0;
    
    public void setRawGreenGain(double g) {
        greenGain = g;
        double logGreen = Math.log( g ) / Math.log(2);
        greenGainSlider.setValue( logGreen );
    }

    public void setRawGreenGainMultivalued(boolean multivalued, Object[] values ) {
        if ( values != null && values.length > 1 ) {
            double[] annotations = new double[values.length];
            for ( int n = 0; n < values.length ; n++ ) {
                annotations[n] = Math.log( ((Number)values[n]).doubleValue() ) / Math.log(2); 
            }
            greenGainSlider.setAnnotations( annotations );
        } else {
            // restore the normal label table without any extra annotations
            greenGainSlider.setAnnotations( null );
        }
        greenGainSlider.setMultivalued( multivalued );
    }
    
    public double getRawGreenGain() {
        double greenEv = greenGainSlider.getValue();
        double green = Math.pow( 2, greenEv );
        return green;
    }

    ColorProfileDesc profile = null;
    public void setRawProfile(ColorProfileDesc p) {
        profile = p;
        setupColorProfile();
    }

    public void setRawProfileMultivalued(boolean multivalued, Object[] values ) {
    }

    public ColorProfileDesc getRawProfile() {
        return profile;
    }

    private void annotateSlider( FieldSliderCombo slider, Object [] values ) {
        double[] annotations = new double[values.length];
        for ( int n = 0; n < values.length ; n++ ) {
            annotations[n] = ((Number)values[n]).doubleValue();
        }
        slider.setAnnotations( annotations );
    }

    /**
     This callback is called by JAIPhotoViewer when the image displayed in the 
     control is changed.
     */
    public void photoViewChanged(PhotoViewChangeEvent e) {
        reloadHistogram();
    }

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton applyBtn;
    private org.photovault.swingui.color.FieldSliderCombo blackLevelSlider;
    private javax.swing.JButton closeBtn;
    private javax.swing.JComboBox colorProfileCombo;
    private org.photovault.swingui.color.FieldSliderCombo ctempSlider;
    private javax.swing.JButton discardBtn;
    private org.photovault.swingui.color.FieldSliderCombo evCorrSlider;
    private org.photovault.swingui.color.FieldSliderCombo fieldSliderCombo1;
    private org.photovault.swingui.color.FieldSliderCombo greenGainSlider;
    private org.photovault.swingui.color.FieldSliderCombo hlightCompSlider;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JButton newProfileBtn;
    private javax.swing.JButton okBtn;
    private javax.swing.JPanel rawHistogramPane;
    private javax.swing.JPanel rawSettingsPane;
    // End of variables declaration//GEN-END:variables
    
}
