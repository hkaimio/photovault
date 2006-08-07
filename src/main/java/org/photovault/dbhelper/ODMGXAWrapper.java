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


package org.photovault.dbhelper;

import org.odmg.*;
import org.apache.ojb.odmg.*;


/**
   ODMGXAWrapper is a simple wrapper for ODMG transactions. When created it checks if there is
   currently an open transaction and if there is not it creates a new one. When calling commit() method
   the the object commits transaction if the transacttion is owned by this object. If it is not then nothing will be done.
*/

public class ODMGXAWrapper {
    Transaction tx = null;
    boolean ownsTx = false;
    static Implementation odmg = ODMG.getODMGImplementation();

    public ODMGXAWrapper() {
	tx = odmg.currentTransaction();
	if ( tx == null ) {
	    tx = odmg.newTransaction();
	    tx.begin();
	    ownsTx = true;
	}
    }

    public void commit() {
	if ( ownsTx ) {
	    tx.commit();
	}
    }

    public void abort() {
	tx.abort();
    }

    public void lock( Object obj, int type ) {
	tx.lock( obj, type );
    }
}
