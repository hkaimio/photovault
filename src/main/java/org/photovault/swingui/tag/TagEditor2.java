/*
  Copyright (c) 2011 Harri Kaimio

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

package org.photovault.swingui.tag;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.photovault.imginfo.Tag;

/**
 * Editor for {@link Tag}s, with autocomplete support
 * 
 * @author Harri Kaimio
 * @since 0.6.0
 */
public class TagEditor2 extends JPanel {
    
    static private Log log = LogFactory.getLog( TagEditor2.class );
    
    private JTextField nameField;

    private AutoCompleteList suggestionList;
    
    private JComboBox categoryField;
    
    private JPopupMenu popup;
    
    private Listener listener;
    
    private TagSuggestionListModel tagSuggestionModel;
    
    private List<ActionListener> actionListeners = new ArrayList();
    
    public TagEditor2() {
        initComponents();
    }
    
    /**
     * Get the value of currently edited tag
     * @return The tag currently being edited. As tags are immutable, the method
     * returns a new tag that matches current values of name and category fields.
     */
    public Tag getTag() {
        String name = nameField.getText();
        String category = (String) categoryField.getSelectedItem();
        int cat = categoryField.getSelectedIndex();
        if ( cat == 0 ) {
            category = Tag.PERSON_TAG;
        } else if ( cat == 1 ) {
            category = Tag.TEXT_TAG;
        }
        return new Tag( category, name);
    }
    
    /**
     * Clears the tag fields
     */
    public void clearTag() {
        nameField.setText( "" );
        hidePopup();
    }
    
    /**
     * Add a new action listener, that is notified when eidting the tag is 
     * finished
     * @param l 
     */
    public void addActionListener( ActionListener l ) {
        actionListeners.add( l );
    }
    
    public void removeActionListener( ActionListener l ) {
        actionListeners.remove( l );
    }
    
    private void fireActionEvent( String command ) {
        ActionEvent e = 
                new ActionEvent( this, ActionEvent.ACTION_PERFORMED, command );
        for ( ActionListener l : actionListeners ) {
            l.actionPerformed( e );
        }
    }
    
    /**
     * Initializes the UI
     */
    private void initComponents() {
        nameField = new JTextField( 8 );
        suggestionList = new AutoCompleteList();
        tagSuggestionModel = new TagSuggestionListModel();
        suggestionList.setModel( tagSuggestionModel );
        suggestionList.setCellRenderer( new TagListCellRenderer() );

        String categories[] = { "Persons", "Tags" };
        categoryField = new JComboBox( categories );
        categoryField.setEditable( true );
        GridBagLayout layout = new GridBagLayout();
        setLayout( layout );
        add( nameField );
        add( categoryField );
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets( 0, 0, 0, 10 );
        layout.setConstraints( nameField, c );
        
        listener = new Listener();
        
        nameField.addKeyListener( listener );        
        nameField.addFocusListener( listener );
        
        suggestionList.addListSelectionListener( listener );
    }

    /**
     * Called by when the autocompleter suggests a new tag. Sets the editor 
     * fields accordingly.
     */
    private void tagSuggested() {
        Tag selected = (Tag) suggestionList.getSelectedValue();
        nameField.setText( selected.getName() );
        System.err.println( "Selected category: " + selected.getType() );
        if ( selected.getType().equals( Tag.PERSON_TAG ) ) {
            categoryField.setSelectedIndex( 0 );            
        } else if ( selected.getType().equals(Tag.TEXT_TAG ) ) {
            categoryField.setSelectedIndex( 1 );            
        } else {
            categoryField.setSelectedItem( selected.getType() );
        }
    }
    
    /**
     * Called when tag is selected, either form autocomplete or user confirms
     * a new tag.
     */
    private void tagSelected() {
        tagSuggestionModel.addTag( getTag() );
        fireActionEvent( "tag_set" );
    }

    /**
     * Show the autocomplete popup menu
     */
    private void showPopup() {
        log.debug( "showPopup()" );
        if ( nameField.isShowing() ) {
            if ( popup != null ) {
                hidePopup();
            }
            popup = new JPopupMenu();
            popup.setFocusable( false );
            popup.add( suggestionList );
            suggestionList.setPreferredSize( null );
            Dimension prefSize = suggestionList.getPreferredSize(); 
            System.err.println( "popup pref height" + prefSize.getHeight() );
            prefSize =
                    new Dimension( nameField.getWidth(), prefSize.height );
            suggestionList.setPreferredSize( prefSize );
            popup.show( nameField, 0, nameField.getHeight() );
        }
    }
    
    /**
     * Hides the autocomplete popup menu
     */
    private void hidePopup() {
        if ( popup != null ) {
            popup.setVisible( false );
            popup = null;
        }
    }

    /**
     * Set the filter for possible completions
     * @param filter The new filter string
     * @param showMenu Whether or not to show the popup of possible completions
     */
    private void setSuggestionFilter( String filter, boolean showMenu ) {
        tagSuggestionModel.setFilter( nameField.getText() );
        if ( showMenu ) {
            SwingUtilities.invokeLater( new Runnable() {

                public void run() {
                    log.debug( "setSuggestionFilter (invokeLater)" );
                    showPopup();
                }
            } );
        }
    }

    /**
     * Renderer for {@link TagController.TagInfo} objects
     */
    private static class TagListCellRenderer extends JLabel 
            implements ListCellRenderer {

        public TagListCellRenderer() {
            setOpaque( true );
        }
        
        public Component getListCellRendererComponent( JList list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus ) {
            
            if ( value instanceof Tag ) {
                Tag tag = (Tag) value;
                String text = tag.getName();
                if ( tag.getType().equals( Tag.PERSON_TAG ) ) {
                    text += " (Person)";
                } 
                setText( text );
            } else {
                setText( value.toString() );
            }
            
            Color background;
            Color foreground;

            // check if this cell represents the current DnD drop location
            JList.DropLocation dropLocation = list.getDropLocation();
            if ( dropLocation != null
                    && !dropLocation.isInsert()
                    && dropLocation.getIndex() == index ) {
                
                background = Color.BLUE;
                foreground = Color.WHITE;

                // check if this cell is selected
            } else if ( isSelected ) {
                background = Color.RED;
                foreground = Color.WHITE;

                // unselected, and not the DnD drop location
            } else {
                background = Color.WHITE;
                foreground = Color.BLACK;
            };
            
            setBackground( background );
            setForeground( foreground );
            
            return this;
        }
    }
    
    /**
     * LIstener for various events from editor fields.
     */
    private class Listener implements 
            KeyListener, FocusListener, ListSelectionListener {

        public void keyTyped( KeyEvent e ) {
            log.debug( "keyTyped(" + e.getKeyCode() + ")" );
            final boolean showAutocompleteList = (e.getKeyChar() != '\n' );
            // Defer call to getText() to ensure that the field has been updated
            SwingUtilities.invokeLater( new Runnable() {
                @Override
                public void run() {
                    setSuggestionFilter( nameField.getText(), showAutocompleteList );
                }
            } );
        }
        
        public void keyPressed( KeyEvent e ) {
            log.debug( "keyPressed()" );
            switch ( e.getKeyCode() ) {
                case KeyEvent.VK_DOWN:
                    e.consume();
                    if ( popup == null ) {
                        showPopup();
                    } else {
                        suggestionList.selectNext();
                    }
                    break;

                case KeyEvent.VK_UP:
                    e.consume();
                    if ( popup == null ) {
                        showPopup();
                    } else {
                        suggestionList.selectPrev();
                    }
                    break;

                case KeyEvent.VK_ESCAPE:
                    e.consume();
                    if ( popup != null ) {
                        hidePopup();
                    }
                    break;
                case KeyEvent.VK_ENTER:
                    e.consume();
                    SwingUtilities.invokeLater( new Runnable() {

                        public void run() {
                            tagSelected();
                        }
                    } );
                    break;
                default:
                    
            }
        }

        public void keyReleased( KeyEvent e ) {
        }

        public void focusGained( FocusEvent e ) {
        }

        public void focusLost( FocusEvent e ) {
            hidePopup();
        }

        public void valueChanged( ListSelectionEvent e ) {
            int selectedIdx = suggestionList.getSelectedIndex();
            if ( selectedIdx != -1 ) {
                tagSuggested();
            }
        }
        
    }
    
    static public void main( String args[] ) {
        JFrame window = new JFrame();
        window.setSize( new Dimension( 200, 100 ));
        final TagEditor2 editor = new TagEditor2();
        editor.addActionListener( new ActionListener() {

            public void actionPerformed( ActionEvent e ) {
                System.out.println( "New tag selected: " + editor.getTag() );
            }
        });
        window.add( editor );
        window.setVisible( true );
    }
}
