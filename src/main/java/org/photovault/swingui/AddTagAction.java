/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.photovault.swingui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import org.photovault.command.ApplyChangeCommand;
import org.photovault.command.CommandException;
import org.photovault.command.CommandHandler;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.Tag;
import org.photovault.replication.ChangeDTO;
import org.photovault.replication.VersionedObjectEditor;
import org.photovault.swingui.framework.DefaultEvent;
import org.photovault.swingui.framework.DefaultEventListener;

/**
 * A very simple action for adding a tag to selected photos. This is mostly a
 * development time placeholder UI.
 * @author Harri Kaimio
 * @since 0.6.0
 */
public class AddTagAction extends AbstractAction {

    private final PhotoViewController ctrl;

    public AddTagAction( PhotoViewController ctrl, String text, ImageIcon icon,
            String desc, int mnemonic ) {
        super( text, icon );
        this.ctrl = ctrl;
        putValue( SHORT_DESCRIPTION, desc );
        putValue( MNEMONIC_KEY, new Integer( mnemonic ) );
        putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( KeyEvent.VK_ENTER,
                ActionEvent.ALT_MASK ) );
        ctrl.registerEventListener( SelectionChangeEvent.class,
                new DefaultEventListener<SelectionChangeEvent>() {
            public void handleEvent(DefaultEvent<SelectionChangeEvent> event) {
                selectionChanged();
            }
        });

        // setEnabled( !ctrl.getSelection().isEmpty() );
    }

    public void actionPerformed( ActionEvent e ) {
        String tagStr = JOptionPane.showInputDialog( "Tag the image with:" );
        Tag tag = null;
        if ( tagStr != null ) {
            int colon = tagStr.indexOf( ":" );
            if ( colon >= 0 ) {
                String type = tagStr.substring( 0, colon );
                String name = tagStr.substring( colon + 1 );
                tag = new Tag( type, name );
            } else {
                tag = new Tag( tagStr );
            }
            Collection<PhotoInfo> selectedPhotos = (Collection<PhotoInfo>) ctrl.
                    getSelection();
            CommandHandler cmdHandler = ctrl.getCommandHandler();
            for ( PhotoInfo photo : selectedPhotos ) {
                if ( photo != null ) {
                    VersionedObjectEditor<PhotoInfo> ed =
                            new VersionedObjectEditor<PhotoInfo>(
                            photo, ctrl.getDAOFactory().getDTOResolverFactory() );
                    ed.addToSet( "tags", tag );
                    ChangeDTO ch = new ChangeDTO( ed.getChange() );
                    ApplyChangeCommand cmd = new ApplyChangeCommand( ch );
                    try {
                        cmdHandler.executeCommand( cmd );
                    } catch ( CommandException ex ) {
                        ex.printStackTrace();
                    }
                }
            }

        }

    }

    public void selectionChanged() {
        setEnabled( !ctrl.getSelection().isEmpty() );
    }
}
