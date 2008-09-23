/*
  Copyright (c) 2008 Harri Kaimio
  
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

import org.photovault.replication.*;
import java.util.UUID;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

/**
 Representation of the version history of a folder. Object of this class is 
 associated with every folder object.
 <p>
 The class is a simple wrapper on top of {@link AnnotatedClassHistory}, mostly 
 to support Hibernate persistence layer reference integrity (as the types of 
 referenced objects are based on generics parameters, Hibernate cannot associate 
 those with correct tables automatically.
 
 @author Harri Kaimio
 @since 0.6.0
 */
@Entity
@DiscriminatorValue( "folder" )
public class FolderHistory extends AnnotatedClassHistory<PhotoFolder> {

    /**
     Current version of the folder
     */
    Change<PhotoFolder> currentVersion;
    
    protected FolderHistory() {}
    
    /**
     Createsa new folder history
     @param f The folder
     */
    public FolderHistory( PhotoFolder f ) {
        super( f );
        setTargetUuid( f.getUuid() );
    }
    
    @Override
    protected PhotoFolder createTarget() {
        PhotoFolder f = new PhotoFolder();
        f.setUuid( getTargetUuid() );
        f.setHistory( this );
        return f;
    }

    @Override
    @Transient
    public UUID getGlobalId() {
        return getOwner().getUuid();
    }


    @Override
    protected void setVersion( Change<PhotoFolder> version ) {
        currentVersion= version;
    }

    @Override
    @Transient
    protected Change<PhotoFolder> getVersion() {
        return currentVersion;
    }
    
    @OneToOne( mappedBy="history" )
    @Cascade( CascadeType.ALL )
    @Override 
    public PhotoFolder getOwner() {
        return super.getOwner();
    }
    
    @Override
    public void setOwner( PhotoFolder owner ) {
        super.setOwner( owner );
    }

}
