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

package org.photovault.folder;

import org.photovault.command.CommandException;
import org.photovault.command.DataAccessCommand;

/**
  Command for creating a new {@link PhotoFolder}.
 */
public class CreatePhotoFolderCommand extends DataAccessCommand {
    Integer parentId = null;
    String name = null;
    String description = null;
    PhotoFolder createdFolder = null;
    
    /** 
     Creates a new instance of CreatePhotoFolderCommand 
     @param parent Parent for the new folder
     @param folderName Name for the folder that will be created.
     */
    public CreatePhotoFolderCommand( PhotoFolder parent, String folderName, 
            String folderDescription ) {
        if ( parent != null ) {
            parentId = parent.getFolderId();
        }
        name = folderName;
        description = folderDescription;

    }

    public void execute() throws CommandException {
        PhotoFolderDAO folderDAO = daoFactory.getPhotoFolderDAO();
        PhotoFolder parent = null;
        if ( parentId != null ) {
            parent = folderDAO.findById( parentId, false );
        }
        PhotoFolder newFolder = PhotoFolder.create( name, parent );
        newFolder.setDescription( description );
        folderDAO.makePersistent( newFolder );
        createdFolder = newFolder;
    }
    
    /**
     Get the created folder
     @param Detached instance of the created folder of <code>null</code> if 
     it has not yet been created.
     */
    public PhotoFolder getCreatedFolder() {
        return createdFolder;
    }
    
}
