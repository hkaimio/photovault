/*
 * SetPhotoOrderAction.java
 *
 * Created on 29. tammikuuta 2006, 10:15
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.photovault.swingui;

import java.awt.event.ActionEvent;
import java.util.Comparator;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

/**
 *
 * @author harri
 */
public class SetPhotoOrderAction extends AbstractAction  {

    /**
       Constructor.
       @param view The view this action object is associated with. The action gets
       the selection to export from this view.
     * @param c Comparator object that is used to sort the photos
     * @param texr Test to display in menus
     * @param icon Icon that is displayed in menus etc.
    */
    public SetPhotoOrderAction( PhotoCollectionThumbView view, Comparator c, String text, ImageIcon icon,
                      String desc, Integer mnemonic) {
	super( text, icon );
	this.view = view;
        this.c = c;
	putValue(SHORT_DESCRIPTION, desc);
        putValue(MNEMONIC_KEY, mnemonic);
    }

    Comparator c;
    
    public void actionPerformed( ActionEvent ev ) {
	// Show the file chooser dialog
        view.setPhotoOrderComparator( c );
    }

    PhotoCollectionThumbView view;
}