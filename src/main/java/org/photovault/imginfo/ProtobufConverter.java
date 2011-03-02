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

package org.photovault.imginfo;

import com.google.protobuf.Message;

/**
 * Interface for implementing converters from custom value type to Protobuf
 * messages
 * @author Harri Kaimio
 * @since 0.6.0
 */
public interface ProtobufConverter<T> {
    /**
     * Create a message based on object
     * @param obj Theobject to convert
     * @return Message matching obj
     */
    Message createMessage( T obj );
    /**
     * Create an object from message
     * @param msg Message to convert
     * @return An object basedon msg.
     */
    T createObject( Message msg );
}
