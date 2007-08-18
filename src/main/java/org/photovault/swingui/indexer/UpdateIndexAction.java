/*
  Copyright (c) 2006 Harri Kaimio
  
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

package org.photovault.swingui.indexer;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.photovault.common.PVDatabase;
import org.photovault.common.PhotovaultSettings;
import org.photovault.imginfo.ExternalVolume;
import org.photovault.imginfo.VolumeBase;
import org.photovault.imginfo.indexer.ExtVolIndexer;
import org.photovault.imginfo.indexer.ExtVolIndexerEvent;
import org.photovault.imginfo.indexer.ExtVolIndexerListener;
import org.photovault.swingui.StatusChangeEvent;
import org.photovault.swingui.StatusChangeListener;
import org.photovault.swingui.framework.AbstractController;

/**
 * Class control the updating of indexed external volumes. When this action is 
 * initiated it starts to go through all external volumes sequentially and updates
 *their indexes using separate threads.
 *
 * @author Harri Kaimio
 */
public class UpdateIndexAction extends AbstractAction implements ExtVolIndexerListener {
    
    /** Creates a new instance of UpdateIndexAction */
    public UpdateIndexAction( AbstractController ctrl, String text, ImageIcon icon, String desc, 
            int mnemonic) {
        super( text, icon );
        this.ctrl = ctrl;
	putValue(SHORT_DESCRIPTION, desc);
        putValue(MNEMONIC_KEY, new Integer( mnemonic) );
    }
    
    AbstractController ctrl;
    
    /**
     * List of volumes to index. After indexing of a volume has started it will be 
     * removed from this list
     */
    Vector volumes = null;
    
    /**
     * Starts the index updating process. After calling this function the action 
     * will stay disabled as long as the opeation is in progress.
     */
    public void actionPerformed(ActionEvent actionEvent) {
        /*
         * Get a list of external volumes
         */
        
        PhotovaultSettings settings = PhotovaultSettings.getSettings();
        PVDatabase db = settings.getCurrentDatabase();
        List allVolumes = db.getVolumes();
        volumes = new Vector();
        Iterator iter = allVolumes.iterator();
        while ( iter.hasNext() ) {
            VolumeBase vol = (VolumeBase) iter.next();
            if ( vol instanceof ExternalVolume ) {
                volumes.add( vol );
            }
        }
        
        setEnabled( false );
        indexNextVolume();
    }

    /**
     Initiate indexing of next volume if there are still unindexed volumes.
     The mothod creates a new ExtVolIndexer and runs it in its own thread. 
     Since this method modifies UI status it must be called from AWT event 
     thread.
     */
    private void indexNextVolume() {
        if ( volumes.size() > 0 ) {
            errorFiles.clear();
            vol = (ExternalVolume)volumes.get(0);
            ExtVolIndexer indexer = new ExtVolIndexer( vol );
            indexer.setCommandHandler( ctrl.getCommandHandler() );
            volumes.remove( vol );
            indexer.addIndexerListener( this );
            Thread t = new Thread( indexer );
            t.start();
            percentIndexed = 0;
            StatusChangeEvent e = new StatusChangeEvent( this, "Indexing " 
                    + vol.getBaseDir() );
            fireStatusChangeEvent( e );
        } else {
            // Nothing more to index
            StatusChangeEvent e = new StatusChangeEvent( this, "" );
            fireStatusChangeEvent( e );
            setEnabled( true );
        }
    }
    
    int percentIndexed = 0;
    /**
     Volume currently under work.
     */
    ExternalVolume vol = null;
    
    List errorFiles = new ArrayList();
    
    public void fileIndexed(ExtVolIndexerEvent e) {
        ExtVolIndexer indexer = (ExtVolIndexer) e.getSource();
        int newPercentIndexed = indexer.getPercentComplete();
        if ( e.getResult() == ExtVolIndexerEvent.RESULT_ERROR ) {
            StringBuffer errorBuf = new StringBuffer( "Error indexing file" );
            if ( e.getIndexedFile() != null ) {
                errorBuf.append( " " ).append( e.getIndexedFile().getAbsolutePath() );
                errorFiles.add( e.getIndexedFile() );
            }
            StatusChangeEvent statusEvent = new StatusChangeEvent( this, 
                    errorBuf.toString() );
            fireStatusChangeEvent( statusEvent );
            
        } else if ( newPercentIndexed > percentIndexed ) {
            StatusChangeEvent statusEvent = new StatusChangeEvent( this, "Indexing " 
                    + vol.getBaseDir() + " - " + newPercentIndexed + "%");
            fireStatusChangeEvent( statusEvent );
        }
    }

    public void indexingComplete(ExtVolIndexer indexer) {
        if ( errorFiles.size() > 0 ) {
            showErrorDialog();
        }
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                indexNextVolume();
            }
        });
    }

    public void indexingError(String message) {
        final String finalMessage = "Error while indexing " + 
                        vol.getBaseDir() + ":\n" + message;
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog( null, finalMessage, "Indexing error", 
                        JOptionPane.ERROR_MESSAGE );
                indexNextVolume();
            }
        });
    }

    /**
     Shows an error dialog that informas about files that the indexer was not 
     able to index (i.e. files stored in {@link errorFiles}).
     <p>
     The dialog is shown in AWT thread, so this method can be called from any 
     thread.
     **/
    private void showErrorDialog() {
        StringBuffer msgBuf = new StringBuffer( "Could not read the following files:" );
        Iterator iter = errorFiles.iterator();
        while ( iter.hasNext() ) {
            File f = (File) iter.next();
            msgBuf.append( "\n" ).append( f.getAbsolutePath() );
        }
        final String msg = msgBuf.toString();
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog( null, msg, "Indexing error", 
                        JOptionPane.ERROR_MESSAGE );
            }
        });
        
    }
    
    Vector statusChangeListeners = new Vector();
    
    public void addStatusChangeListener( StatusChangeListener l ) {
        statusChangeListeners.add( l );
    }
    
    public void removeStatusChangeListener( StatusChangeListener l ) {
        statusChangeListeners.remove( l );
    }
    
    protected void fireStatusChangeEvent( StatusChangeEvent e ) {
        Iterator iter = statusChangeListeners.iterator();
        while ( iter.hasNext() ) {
            StatusChangeListener l = (StatusChangeListener) iter.next();
            l.statusChanged( e );
        }
    }

}
