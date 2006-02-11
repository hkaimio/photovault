// UIUtils.java

package org.photovault.swingui;

import javax.swing.*;
import java.awt.*;

/**
   UIUtils includes several static utility functions for creating UI structures
*/

class UIUtils {
    public static void addLabelTextRows(JLabel[] labels,
					JTextField[] textFields,
					GridBagLayout gridbag,
					Container container) {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
	c.insets = new Insets( 2, 2, 2, 2 );
	int numLabels = labels.length;
	
        for (int i = 0; i < numLabels; i++) {
	    c.anchor = GridBagConstraints.EAST;
            c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
            c.fill = GridBagConstraints.NONE;      //reset to default
            c.weightx = 0.0;                       //reset to default
            gridbag.setConstraints(labels[i], c);
            container.add(labels[i]);

	    c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.REMAINDER;     //end row
            c.weightx = 1.0;
            gridbag.setConstraints(textFields[i], c);
            container.add(textFields[i]);
        }
    }
}
