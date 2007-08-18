/*
  Copyright (c) 2007, Harri Kaimio
  
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

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.photovault.command.CommandException;
import org.photovault.command.DataAccessCommand;
import org.photovault.common.PhotovaultException;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.image.ChannelMapOperation;

/**
 Command that creates a new instance to database based on image file and/or
 information provided via function calls. USer of this class can specify a {@link 
 PhotoInfo} - if one is given the instance will be added to this photo. Otherwise
 a new PhotoInfo object is created and the instance added to it.
 
 */
public class CreateImageInstanceCommand extends DataAccessCommand
        implements ImageInstanceModifier {
    
    /** 
     Creates a new instance of CreateImageInstanceCommand
     @param volume Volume in wjich the instance will be created.
     @param imageFile Instance file
     @param photo The photo whose instance we are creating.
     @param instanceType Type of the instance.
     */
    public CreateImageInstanceCommand( VolumeBase volume, File imageFile,
            PhotoInfo photo, int instanceType ) throws PhotovaultException, IOException {
        instance = ImageInstance.create( volume, imageFile );
        instance.instanceType = instanceType;
        instance.readImageFile();
        photoId = photo.getId();
    }
    
    /** 
     Creates a new instance of CreateImageInstanceCommand
     @param volume Volume in wjich the instance will be created.
     @param imageFile Instance file
     */
    public CreateImageInstanceCommand( VolumeBase volume, File imageFile ) {
        instance = ImageInstance.create( volume, imageFile );        
    }
    
    /** 
     Creates a new instance of CreateImageInstanceCommand
     @uuid UUID for the created instance.
     */
    public CreateImageInstanceCommand( UUID uuid ) {
        instance = ImageInstance.create( uuid );
    }

    /**
     Get the created instance
     @return The ImageInstance created (this object is either detached or in 
     command handler's persistence context.
     */
    public ImageInstance getImageInstance() {
        return instance;
    }

    /**
     Get the PhotoInfo that owns the instance 
     @return The PhotoInfo with this command applied (this object is either 
     detached or in command handler's persistence context.
     */    
    public PhotoInfo getChangedPhoto() {
        return photo;
    }
    
    public void execute() throws CommandException {
        PhotoInfoDAO photoDAO = daoFactory.getPhotoInfoDAO();
        ImageInstanceDAO instDAO = daoFactory.getImageInstanceDAO();
        /*
         First, make sure that there is no instance with this name in database.
         This should not happen but seems to be possible...
         */
        ImageInstance existingInstance = 
                instDAO.getExistingInstance( instance.getVolume(), instance.getFname() );
        if ( existingInstance != null ) {
            PhotoInfo existingPhoto = existingInstance.getPhoto();
            existingPhoto.removeInstance( existingInstance );
            instDAO.makeTransient( existingInstance );
        }
        if ( photoId != null ) {
            photo = photoDAO.findById( photoId, false );
        } else {
            photo = PhotoInfo.create();
            photo = photoDAO.makePersistent( photo );
        }
        photo.addInstance( instance );
        if ( photoId == null ) {
            photo.updateFromOriginalFile();
        }
        // instDAO.makePersistent( instance );
    }

    public void setColorChannelMapping(ChannelMapOperation cm) {
        instance.setColorChannelMapping( cm );
    }

    public void setCropBounds(Rectangle2D cropBounds) {
        instance.setCropBounds( cropBounds );
    }

    public void setFileSize(long s) {
        instance.setFileSize( s );
    }

    public void setHash(byte[] hash) {
        instance.setHash( hash );
    }

    public void setHeight(int v) {
        instance.setHeight( v );
    }

    public void setImageFile(File v) {
        instance.setImageFile( v );
    }

    public void setInstanceType(int v) {
        instance.setInstanceType( v );
    }

    public void setPhoto(PhotoInfo photo) {
        instance.setPhoto( photo );
        photoId = photo.getId();
    }

    public void setRawSettings(RawConversionSettings s) {
        instance.setRawSettings( s );
    }

    public void setRotated(double v) {
        instance.setRotated( v );
    }

    public void setUUID(UUID uuid) {
        instance.setUUID( uuid );
    }

    public void setVolume(VolumeBase v) {
        instance.setVolume( v );
        instance.setVolume_id( v.getName() );
    }

    public void setWidth(int v) {
        instance.setWidth( v );
    }
    
    /**
     Id of the photo fo rwhich we are creating this instance
     */
    Integer photoId = null;
    
    /**
     The instance we are currently creating
     */
    ImageInstance instance;
    
    /**
     The photo in persistence context in which the command is executed.
     */
    PhotoInfo photo;
}
