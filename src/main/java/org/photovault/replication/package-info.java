/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 Usage of replication services
 
 - Create an enum for listing the fields in the replicable object
 
 - Subclass ChangeSupport, implement setField and getField methods for adjusting 
 fields

 Editing the object
 
 - Create a new change:
 
 Change c = o.createChange();
 
 - Make needed modifications in the change
 
 - Freeze the change:
 
 c.freeze();
 
 This will also apply the change to o.
 
 Merging conflicts
 
 Calling freeze() will throw an exception if there is more than one head. In this 
 case, you need to merge the additional heads to this change:
 
 o.getHeads();
 
 c.merge( head );
 
 for ( Conflict co : c.getConflicts() ) {
     c.resolve();
 }
 */
package org.photovault.replication;

