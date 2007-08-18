/*
  Copyright (c) 2007 Harri Kaimio
 
  This file is part of Photovault.
 
  Photovault is free software; you can redistribute it and/or modify it
  under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.
 
  Photovault is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even therrore implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  General Public License for more details.
 
  You should have received a copy of the GNU General Public License
  along with Photovault; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.photovault.imginfo;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.photovault.command.CommandException;
import org.photovault.command.DataAccessCommand;

/**
  Command for deleting instances from database
 */
public class DeleteImageInstanceCommand extends DataAccessCommand {
    
    Set<ImageInstance.InstanceId> ids = new HashSet<ImageInstance.InstanceId>();
    
    /** 
     Creates a new instance of DeleteImageInstanceCommand 
     @param instance The instance that will be deleted by this command
     @param deleteFiles If true, the instance files will be deleted when executing the
     command
     */
    public DeleteImageInstanceCommand( ImageInstance instance, boolean deleteFile ) {
        ids.add( instance.getHibernateId() );
        deleteFiles = deleteFile;
    }
    
    /** Creates a new instance of DeleteImageInstanceCommand */
    public DeleteImageInstanceCommand( ImageInstance[] instances, boolean deleteFiles ) {
        for ( ImageInstance i : instances ) {
            ids.add( i.getHibernateId() );
        }
        this.deleteFiles = deleteFiles;
    }
    
    boolean deleteFiles = true;

    /** Creates a new instance of DeleteImageInstanceCommand */
    public DeleteImageInstanceCommand( Collection<ImageInstance> instances, boolean deleteFiles ) {
        for ( ImageInstance i : instances ) {
            ids.add( i.getHibernateId() );
        }
        this.deleteFiles = deleteFiles;
    }

    public void execute() throws CommandException {
        ImageInstanceDAO instDAO = daoFactory.getImageInstanceDAO();
        for ( ImageInstance.InstanceId id : ids ) {
            ImageInstance i = instDAO.findById( id, false );
            PhotoInfo p = i.getPhoto();
            p.removeInstance( i );
            File f = i.getImageFile();
            if ( deleteFiles && f != null && f.exists() ) {
                f.delete();
            }
            instDAO.makeTransient( i );
        }
    }
    
    
}
