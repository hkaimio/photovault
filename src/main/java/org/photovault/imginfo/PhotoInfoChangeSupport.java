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

package org.photovault.imginfo;

import java.util.Map;
import java.util.UUID;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.photovault.dcraw.ColorProfileDesc;
import org.photovault.dcraw.RawSettingsFactory;
import org.photovault.replication.AnnotatedClassHistory;
import org.photovault.replication.Change;
import org.photovault.replication.DTOResolver;
import org.photovault.replication.FieldDescriptor;

/**
 *
 * @author harri
 */
@Entity
@DiscriminatorValue( "photo" )
public class PhotoInfoChangeSupport extends AnnotatedClassHistory<PhotoInfo> {
    
    private static Log log = LogFactory.getLog( ChangePhotoInfoCommand.class );
    
    Change<PhotoInfo, String> currentVersion;
    
    public PhotoInfoChangeSupport( PhotoInfo p ) {
        super( p );
        setTargetUuid( p.getUuid() );
    }
    
    public PhotoInfoChangeSupport() {
        super();
    }
    
    
    PhotoInfoChangeSupport( PhotoInfo p, OriginalImageDescriptor img ) {
        this( p );
        Change <PhotoInfo, String> initialChange = createChange();
        // initialChange.setField( PhotoInfoFields, log);
        initialChange.freeze();
    }
    
    @OneToOne( mappedBy="history" )
    @Cascade( CascadeType.ALL )
    @Override
    public PhotoInfo getOwner() {
        return super.getOwner();
    }
    
    @Override
    public void setOwner( PhotoInfo p ) {
        super.setOwner( p );
    }
    
    @Override
    @Transient
    public UUID getGlobalId() {
        return getOwner().getUuid();
    }

    @Override
    protected void setVersion( Change<PhotoInfo, String> version ) {
        currentVersion = version;
    }

    @Override
    @Transient
    protected Change<PhotoInfo, String> getVersion() {
        return currentVersion;
    }
    
  private void setRawField( RawSettingsFactory settings, PhotoInfoFields field, Object newValue ) {
        switch ( field ) {
            case RAW_BLACK_LEVEL:
                settings.setBlack( (Integer)newValue );
                break;
            case RAW_WHITE_LEVEL:
                settings.setWhite( (Integer) newValue );
                break;
            case RAW_CTEMP:
                settings.setColorTemp( (Double) newValue );
                break;
            case RAW_EV_CORR:
                settings.setEvCorr( (Double) newValue );
                break;
            case RAW_GREEN:
                settings.setGreenGain( (Double) newValue );
                break;
            case RAW_HLIGHT_COMP:
                settings.setHlightComp( (Double) newValue );
                break;
            case RAW_COLOR_PROFILE:
                settings.setColorProfile( (ColorProfileDesc) newValue);
                break;
        }
    }
  
    static private Map<String,FieldDescriptor<PhotoInfo>> fieldDescriptors;

    static class PhotoFieldDesc extends FieldDescriptor<PhotoInfo> {
        
        Class<? extends DTOResolver> resolverClass = null;
        PhotoInfoFields field = null;
        
        PhotoFieldDesc( String name, PhotoInfoFields field, Class<? extends DTOResolver> resolverClass ) {
            super( name );
            this.resolverClass = resolverClass;
            this.field = field;
        }
        
        @Override
        public Class<? extends DTOResolver> getDtoResolverClass() {
            return resolverClass;
        }

        @Override
        public Object getValue( PhotoInfo target ) {
            return field.getFieldValue( target, field );
        }

        @Override
        public void setValue( PhotoInfo target, Object newValue ) {
            
        }
        
        
    }
    
    /*
     Initialize field descriptors
     */
//    static {
//        for ( PhotoInfoFields f : EnumSet.allOf( PhotoInfoFields.class ) ) {
//            f.ge
//        }
//    }
//    
    @Override
    protected FieldDescriptor<PhotoInfo> getFieldDescriptor( String field ) {
        return fieldDescriptors.get( field );
    }

    @Override
    @Transient
    protected Map<String, FieldDescriptor<PhotoInfo>> getFieldDescriptors() {
        return null;
    }

    @Override
    protected PhotoInfo createTarget() {
        PhotoInfo p = PhotoInfo.create( getTargetUuid() );
        p.setHistory( this );
        return p;
    }
        
    
}
