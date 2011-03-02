/*
  Copyright (c) 2011 Harri Kaimio

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

package org.photovault.replication;

import com.google.protobuf.ExtensionRegistry;
import java.util.Map;
import java.util.Map.Entry;
import org.photovault.common.Types;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.photovault.common.ProtobufHelper;
import static org.photovault.common.ProtobufHelper.*;
import org.photovault.replication.ChangeProtos.Change.Builder;

/**
 * Base class for serializing changes to Protobuf messages.
 * @author Harri Kaimio
 * @since 0.6.0
 */
public class ProtobufChangeSerializer implements ChangeSerializer {

    static private Log log = LogFactory.getLog( ProtobufChangeSerializer.class );

    protected ExtensionRegistry extensions = null;

    public ProtobufChangeSerializer() {
        extensions = ExtensionRegistry.newInstance();
    }

    public byte[] serializeChange( ChangeDTO dto ) {
        ChangeProtos.Change.Builder b = ChangeProtos.Change.newBuilder();
        b.setTargetUUID( ProtobufHelper.uuidBuf( dto.getTargetUuid() ) );
        b.setTargetClassName( dto.getTargetClassName() );
        for ( Object p: dto.parentIds ) {
            b.addParentIds( ProtobufHelper.uuidBuf( (UUID) p ) );
        }
        for ( Object o: dto.changedFields.entrySet() ) {
            Map.Entry<String, FieldChange> e = (Entry<String, FieldChange>) o;
            FieldChange fc = e.getValue();
            if ( fc instanceof ValueChange ) {
                serializeValueChange( (ValueChange) fc,b);
            } else if ( fc instanceof SetChange ) {
                serializeSetChange( (SetChange) fc,b);
            }
        }
        ChangeProtos.Change c = b.build();
        log.debug( "Created change " + TextFormat.printToString( c ) );
        return c.toByteArray();

    }

    public ChangeDTO deserializeChange( byte[] serialized ) {
        ChangeProtos.Change ch = null;
        try {
            ch = ChangeProtos.Change.parseFrom( serialized, extensions );
        } catch ( InvalidProtocolBufferException ex ) {
            return null;
        }
        ChangeDTO dto = new ChangeDTO();
        dto.targetUuid = uuid( ch.getTargetUUID() );
        dto.targetClassName = ch.getTargetClassName();
        for ( Types.UUID id: ch.getParentIdsList() ) {
            dto.parentIds.add( uuid( id ) );
        }
        String lastFname = "";
        FieldChange fc = null;
        for ( ChangeProtos.FieldChange fcp : ch.getFieldChangesList() ) {
            String fname = fcp.getFieldName();
            if ( !lastFname.equals( fname ) ) {
                fc = null;
            }
            switch ( fcp.getType() ) {
                case VALUE_CHANGE:
                    fc = parseValueChange( fcp.getValueChange(), fname,
                            (ValueChange) fc);
                    break;
                case SET_CHANGE:
                    fc = msgToSetChange( fcp.getSetChange(), fname );
                    break;

            }
            dto.changedFields.put( fname, fc );
        }

        return dto;
    }

    /**
     * Create a ValueChange message for given piece of data. The base class
     * method handles integers, doubles, booleans, strings and UUIDs, for other
     * types initialization of the message is left for derived classes.
     * @param val The value to store in the message
     * @return Message containing val.
     */
    private ChangeProtos.ValueChange.Builder valueChangeToMsg( Object val ) {
        ChangeProtos.ValueChange.Builder vb =
                ChangeProtos.ValueChange.newBuilder();
        // vb.setValueType( e.getValue().getClass().getName() );
        if ( val == null ) {
            vb.setType( 0 );
        } else if ( val instanceof Integer ) {
            vb.setIntValue( (Integer) val );
            vb.setType( ChangeProtos.ValueChange.INTVALUE_FIELD_NUMBER );
        } else if ( val instanceof Double ) {
            vb.setDoubleValue( (Double) val );
            vb.setType( ChangeProtos.ValueChange.DOUBLEVALUE_FIELD_NUMBER );
        } else if ( val instanceof String ) {
            vb.setStringValue( (String) val );
            vb.setType( ChangeProtos.ValueChange.STRINGVALUE_FIELD_NUMBER );
        } else if ( val instanceof Boolean ) {
            vb.setBoolValue( (Boolean) val );
            vb.setType( ChangeProtos.ValueChange.BOOLVALUE_FIELD_NUMBER );
        } else if ( val instanceof UUID ) {
            vb.setUuidValue( ProtobufHelper.uuidBuf( (UUID) val ) );
            vb.setType( ChangeProtos.ValueChange.UUIDVALUE_FIELD_NUMBER );
        } else {
            initValueChangeBuilderExt( vb, val );
        }
        return vb;
    }

    /**
     * Analyzes a ValueChangemessage and based on whether it affects the same
     * field as previously handled message either appends the change to existing
     * ValueChange or creates a new one.
     * @param vcp the ValueChange message
     * @param fname Field name (with potential subproperty after colon) for the
     * value
     * @param lastChange The last ValueChange returned by calling this method.
     * @return Either lastChange modified by value in vcp or a new ValueChange.
     */
    private ValueChange parseValueChange( ChangeProtos.ValueChange vcp,
            String fname, ValueChange lastChange ) {
        ValueChange ch = lastChange;
        String field = fname;
        String prop = "";
        int fnameEnd = fname.indexOf( "." );
        if ( fnameEnd > 0 ) {
            field = fname.substring( 0, fnameEnd );
            prop = fname.substring( fnameEnd+1 );
        }
        if ( lastChange == null || !field.equals( lastChange.getName() ) ) {
            // this is the first property for a new field
            ch = new ValueChange( fname, null );
        }
        Object val = msgToValueChange( vcp );
        ch.addPropChange( prop, val );

        return ch;

    }

    /**
     * Create a new instance of object that matches value stored in vcp
     * @param vcp The message used to construct the obejct
     * @return Object matching vcp.
     */
    private Object msgToValueChange( ChangeProtos.ValueChange vcp ) {
        Object val = null;
        switch ( vcp.getType() ) {
            case 0:
                // null value
                break;
            case ChangeProtos.ValueChange.BOOLVALUE_FIELD_NUMBER:
                val = vcp.getBoolValue();
                break;
            case ChangeProtos.ValueChange.DOUBLEVALUE_FIELD_NUMBER:
                val = vcp.getDoubleValue();
                break;
            case ChangeProtos.ValueChange.INTVALUE_FIELD_NUMBER:
                val = vcp.getIntValue();
                break;
            case ChangeProtos.ValueChange.STRINGVALUE_FIELD_NUMBER:
                val = vcp.getStringValue();
                break;
            case ChangeProtos.ValueChange.UUIDVALUE_FIELD_NUMBER:
                val = ProtobufHelper.uuid( vcp.getUuidValue() );
                break;
            default:
                val = msgToValueChangeExt( vcp );
        }
        return val;
    }

    /**
     * Add messages that describe certain {@link ValueChange} to builder.
     * @param fc The change to add
     * @param b Builder to which the change messages are added.
     */
    private void serializeValueChange( ValueChange fc, Builder b ) {
        for ( Map.Entry<String, Object> e : fc.getPropChanges().entrySet() ) {
            ChangeProtos.FieldChange.Builder cb = ChangeProtos.FieldChange.newBuilder();
            cb.setType( ChangeProtos.FieldChangeType.VALUE_CHANGE );
            cb.setFieldName( e.getKey() );
            Object val = e.getValue();
            ChangeProtos.ValueChange.Builder vb = valueChangeToMsg( val );

            cb.setValueChange( vb );
            b.addFieldChanges( cb );
        }
    }

    /**
     * Convert a Protobuf message describing set change to {@link SetChange}.
     * @param scb The message
     * @param fname Name of field that is changed.
     * @return SetChange based on scb
     */
    private SetChange msgToSetChange( ChangeProtos.SetChange scb, String fname ) {
        SetChange sc = new SetChange( fname );
        for ( ChangeProtos.ValueChange vbc : scb.getAddedList() ) {
            Object val = msgToValueChange( vbc );
            sc.addItem( val );
        }
        for ( ChangeProtos.ValueChange vbc : scb.getRemovedList() ) {
            Object val = msgToValueChange( vbc );
            sc.removeItem( val );
        }
        return sc;
    }

    /**
     * Add message describing a set change to Protobuf builder.
     * @param fc
     * @param b
     */
    private void serializeSetChange( SetChange fc, Builder b ) {
        ChangeProtos.FieldChange.Builder cb = ChangeProtos.FieldChange.
                newBuilder();
        cb.setType( ChangeProtos.FieldChangeType.SET_CHANGE );
        cb.setFieldName( fc.getName() );
        ChangeProtos.SetChange.Builder sb = ChangeProtos.SetChange.newBuilder();
        for ( Object o : fc.getAddedItems() ) {
            ChangeProtos.ValueChange.Builder vb = valueChangeToMsg( o );
            sb.addAdded( vb );
        }
        for ( Object o : fc.getRemovedItems() ) {
            ChangeProtos.ValueChange.Builder vb = valueChangeToMsg( o );
            sb.addRemoved( vb );
        }
        cb.setSetChange( sb );
        b.addFieldChanges( cb );
    }


    /**
     * Extension point for derived classes to hande conversion from ValueChange
     * Protobuf messages to custom types not handled by this base class.
     * @param v Message to be converted
     * @return Object described by the message.
     */
    protected Object msgToValueChangeExt( ChangeProtos.ValueChange v ) {
        return null;
    }

    /**
     * Extension point for derived classes to hande conversion of custom types
     * not supported by this base classto ValueChange messages. The overridden
     * method must set the type field to match extension number used for type of
     * vb and the related extension field to correct value.
     * @param vb Builder that must be initialized to match value
     * @param value Value that must be converted to ValueChange message
     */
    protected void initValueChangeBuilderExt(
            ChangeProtos.ValueChange.Builder vb, Object value ) {

    }

}
