// $Id: ODMGXAWrapper.java,v 1.2 2003/02/28 20:34:34 kaimio Exp $


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
