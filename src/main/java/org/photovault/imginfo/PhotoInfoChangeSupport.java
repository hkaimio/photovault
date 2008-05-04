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

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.photovault.common.PhotovaultException;
import org.photovault.dcraw.ColorProfileDesc;
import org.photovault.dcraw.RawSettingsFactory;
import org.photovault.image.ChannelMapOperationFactory;
import org.photovault.image.ColorCurve;
import org.photovault.replication.Change;
import org.photovault.replication.ChangeSupport;

/**
 *
 * @author harri
 */
@Entity
@DiscriminatorValue( "photo" )
public class PhotoInfoChangeSupport extends ChangeSupport<PhotoInfo, PhotoInfoFields> {
    
    private static Log log = LogFactory.getLog( ChangePhotoInfoCommand.class );
    
    Change<PhotoInfo, PhotoInfoFields> currentVersion;
    
    public PhotoInfoChangeSupport( PhotoInfo p ) {
        super( p );
        setTargetUuid( p.getUuid() );
    }
    
    protected PhotoInfoChangeSupport() { super(); }
    
    @OneToOne( mappedBy="history" )
    @Override
    public PhotoInfo getOwner() {
        return super.getOwner();
    }
    
    @Override
    @Transient
    public UUID getGlobalId() {
        return getOwner().getUuid();
    }

    @Override
    protected Object getField( PhotoInfoFields field ) {
        return field.getFieldValue(getOwner(), field);
    }

    @Override
    protected void setField( PhotoInfoFields field, Object value ) {
        Set<PhotoInfoFields> rawSettingsFields =
                EnumSet.range( PhotoInfoFields.RAW_BLACK_LEVEL, PhotoInfoFields.RAW_COLOR_PROFILE );
        Set<PhotoInfoFields> colorCurveFields =
                EnumSet.range( PhotoInfoFields.COLOR_CURVE_VALUE, PhotoInfoFields.COLOR_CURVE_SATURATION );
        RawSettingsFactory rawSettingsFactory = null;
        ChannelMapOperationFactory channelMapFactory = null;
        if ( rawSettingsFields.contains( field ) ) {
            if ( value != null ) {
                // This is a raw setting field, we must use factory for changing it
                if ( rawSettingsFactory == null ) {
                    rawSettingsFactory = new RawSettingsFactory( getOwner().getRawSettings() );
                }
                this.setRawField( rawSettingsFactory, field, value );
            }
        } else if ( colorCurveFields.contains( field ) ) {
            if ( value != null ) {
                if ( channelMapFactory == null ) {
                    channelMapFactory = new ChannelMapOperationFactory( getOwner().getColorChannelMapping() );
                }
                switch ( field ) {
                    case COLOR_CURVE_VALUE:
                        channelMapFactory.setChannelCurve( "value", (ColorCurve) value );
                        break;
                    case COLOR_CURVE_RED:
                        channelMapFactory.setChannelCurve( "red", (ColorCurve) value );
                        break;
                    case COLOR_CURVE_BLUE:
                        channelMapFactory.setChannelCurve( "blue", (ColorCurve) value );
                        break;
                    case COLOR_CURVE_GREEN:
                        channelMapFactory.setChannelCurve( "green", (ColorCurve) value );
                        break;
                    case COLOR_CURVE_SATURATION:
                        channelMapFactory.setChannelCurve( "saturation", (ColorCurve) value );
                        break;
                }
            }
        } else if ( field != PhotoInfoFields.FUZZY_SHOOT_TIME ) {
            /*
             TODO: The test above is needed beaucse of the current unclear presentation
             of fuzzy time. How to get rid of it?
             */
            try {
                PropertyUtils.setProperty( getOwner(), field.getName(), value );
            } catch ( Exception ex ) {
                log.error( "Exception while executing command", ex );
                throw new IllegalArgumentException( "Error while setting field " + field.getName() );
            } 
        }

        if ( rawSettingsFactory != null ) {
            try {
                getOwner().setRawSettings( rawSettingsFactory.create() );
            } catch ( PhotovaultException ex ) {
                log.error( "Exception while executing command", ex );
                ex.printStackTrace();
            }
        }
        if ( channelMapFactory != null ) {
            getOwner().setColorChannelMapping( channelMapFactory.create() );

        }
    }

    @Override
    protected Set<PhotoInfoFields> allFields() {
        return EnumSet.allOf( PhotoInfoFields.class );
    }

    @Override
    protected void setVersion( Change<PhotoInfo, PhotoInfoFields> version ) {
        currentVersion = version;
    }

    @Override
    @Transient
    protected Change<PhotoInfo, PhotoInfoFields> getVersion() {
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
        
    
}
