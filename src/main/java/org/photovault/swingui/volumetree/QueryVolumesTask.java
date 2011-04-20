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


package org.photovault.swingui.volumetree;

import java.util.List;
import javax.swing.SwingWorker;
import org.photovault.imginfo.ExternalVolume;
import org.photovault.imginfo.VolumeBase;
import org.photovault.imginfo.VolumeDAO;

/**
 * Bacrground task for querying list of known volumes in background thread. The 
 * found volumes are added to the owner controller by calling 
 * {@link VolumeTreeController#addVolume(org.photovault.swingui.volumetree.VolumeTreeNode)} 
 * @author Harri Kaimio
 * @since 0.6.0
 */
class QueryVolumesTask extends SwingWorker<Void,VolumeTreeNode> {

    private final VolumeDAO volDao;
    private final VolumeTreeController ctrl;

    QueryVolumesTask( VolumeDAO voldao, VolumeTreeController ctrl ) {
        this.volDao = voldao;
        this.ctrl = ctrl;
    }

    @Override
    protected Void doInBackground() throws Exception {
        List<VolumeBase> allVolumes = volDao.findAll();
        for ( VolumeBase v : allVolumes ) {
            if ( v instanceof ExternalVolume ) {
                VolumeTreeNode node = new VolumeTreeNode( v.getId(), v.getName(), "" );
                publish( node );
            }
        }
        return null;
    }

    @Override
    protected void process( List<VolumeTreeNode> nodes ) {
        for ( VolumeTreeNode node : nodes ) {
            ctrl.addVolume( node );
        }
    }

}
