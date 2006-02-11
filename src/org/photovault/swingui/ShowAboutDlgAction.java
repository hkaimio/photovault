/*
 * ShowAboutDlgAction.java
 *
 * Created on 12. joulukuuta 2005, 22:10
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.photovault.swingui;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import org.photovault.swingui.about.AboutDlg;

/**
 *
 * @author harri
 */
public class ShowAboutDlgAction extends AbstractAction {
    
    /** Creates a new instance of ShowAboutDlgAction */
    public ShowAboutDlgAction( String text, ImageIcon icon,
            String desc, Integer mnemonic) {
        super( text, icon );
        putValue(SHORT_DESCRIPTION, desc);
        putValue(MNEMONIC_KEY, mnemonic);
    }
    
    AboutDlg aboutDlg = null;
    public void actionPerformed( ActionEvent ev ) {
        if ( aboutDlg == null ) {
            aboutDlg = new AboutDlg( null, true );
            aboutDlg.showDialog();
        }
    }    
}
