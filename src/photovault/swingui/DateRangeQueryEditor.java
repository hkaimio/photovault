// DateRangeQueryEditor

package photovault.swingui;

import imginfo.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;

public class DateRangeQueryEditor extends JPanel {

    /**
       Constructor
    */
    public DateRangeQueryEditor() {
	createUI();
    }

    protected void createUI() {
	DateFormat df = DateFormat.getDateInstance();
	startDate = new JFormattedTextField( df );
	startDate.setColumns( 10 );
	startDate.setValue( new Date( ));
	endDate = new JFormattedTextField( df );
	endDate.setColumns( 10 );
	endDate.setValue( new Date( ));

	JLabel startLabel = new JLabel( "Start date" );
	JLabel endLabel = new JLabel( "End date" );

	JLabel[] labels = {startLabel, endLabel};
	JTextField[] fields = {startDate, endDate};
	GridBagLayout layout = new GridBagLayout();
	setLayout( layout );
	UIUtils.addLabelTextRows( labels, fields, layout, this );
    }

    /**
       Returns the start date of the range of null if the range has unbounded start
    */
    public Date getStartDate() {
	return (Date) startDate.getValue();
    }

    /**
       Returns the end date of the range of null if the range has unbounded end
    */
    public Date getEndDate() {
	return (Date) endDate.getValue();
    }
    
    JFormattedTextField startDate = null;
    JFormattedTextField endDate = null;
}