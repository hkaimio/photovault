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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
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

/**
 * Class control the updating of indexed external volumes. When this action is 
 * initiated it starts to go through all external volumes sequentially and updates
 *their indexes using separate threads.
 *
 * @author Harri Kaimio
 */
public class UpdateIndexAction extends AbstractAction implements ExtVolIndexerListener {
    
    /** Creates a new instance of UpdateIndexAction */
    public UpdateIndexAction( String text, ImageIcon icon, String desc, 
            int mnemonic) {
        super( text, icon );
	putValue(SHORT_DESCRIPTION, desc);
        putValue(MNEMONIC_KEY, new Integer( mnemonic) );
    }
    
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
            vol = (ExternalVolume)volumes.get(0);
            ExtVolIndexer indexer = new ExtVolIndexer( vol );
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
    
    public void fileIndexed(ExtVolIndexerEvent e) {
        ExtVolIndexer indexer = (ExtVolIndexer) e.getSource();
        int newPercentIndexed = indexer.getPercentComplete();
        if ( newPercentIndexed > percentIndexed ) {
            StatusChangeEvent statusEvent = new StatusChangeEvent( this, "Indexing " 
                    + vol.getBaseDir() + " - " + newPercentIndexed + "%");
            fireStatusChangeEvent( statusEvent );
        }
    }

    public void indexingComplete(ExtVolIndexer indexer) {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                indexNextVolume();
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
