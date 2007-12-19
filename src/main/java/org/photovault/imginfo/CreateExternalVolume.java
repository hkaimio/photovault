/*
  Copyright (c) 2007 Harri Kaimio
  
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

package org.photovault.imginfo;

import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.photovault.command.CommandException;
import org.photovault.command.DataAccessCommand;
import org.photovault.folder.PhotoFolder;
import org.photovault.folder.PhotoFolderDAO;

/**
 Command object for creating a new external volume and associating it with 
 certain folder
 @author harri
 */
public class CreateExternalVolume extends DataAccessCommand {
    private static Log log = 
            LogFactory.getLog( CreateExternalVolume.class.getName() );
    
    private File basedir;
    private PhotoFolder topFolder;
    private String volumeName;
    private ExternalVolume volume;
    
    public CreateExternalVolume( File basedir, String name, PhotoFolder topFolder ) {
        this.basedir = basedir;
        this.topFolder = topFolder;
        this.volumeName = name;
    }
    
    public ExternalVolume getCreatedVolume() {
       return volume; 
    }
    
    public void execute() throws CommandException {
        VolumeDAO volDAO = daoFactory.getVolumeDAO();
        PhotoFolderDAO folderDAO = daoFactory.getPhotoFolderDAO();
        volume = new ExternalVolume( );
        volume.setName( volumeName );
        if ( topFolder != null ) {
            volume.setFolder( folderDAO.findById( topFolder.getFolderId(), false ) );
        }
        volDAO.makePersistent( volume );
        try {
            VolumeManager.instance().initVolume( volume, basedir );
        } catch ( Exception e ) {
            log.warn( "Error in CreateExternalVolume: " + e.getMessage(), e );
        }
    }

}
