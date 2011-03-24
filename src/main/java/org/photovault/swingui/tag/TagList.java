/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.photovault.swingui.tag;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.photovault.swingui.tag.TagController.TagInfo;

/**
 *
 * @author harri
 */
public class TagList extends JList {

    private TagController ctrl;

    public TagList( TagController tc ) {
        super();
        ctrl = tc;
        setLayoutOrientation( JList.HORIZONTAL_WRAP);
        setVisibleRowCount( -1 );

        final JPopupMenu tagPopup = new JPopupMenu();
        tagPopup.add(  "Test" );
        tagPopup.add( new JMenuItem( new AddToAllTagAction( this ) ) );
        tagPopup.add( new JMenuItem( new RemoveTagAction( this ) ) );
        tagPopup.add( new JSeparator() );
        tagPopup.add( new JMenuItem( new AddTagAction( this ) ) );

        addMouseListener( new MouseAdapter() {

            @Override
            public void mousePressed( MouseEvent e ) {
                maybeShowPopup( e );
            }

            @Override
            public void mouseReleased( MouseEvent e ) {
                maybeShowPopup( e );
            }

            private void maybeShowPopup( MouseEvent e ) {
                if ( e.isPopupTrigger() ) {
                    tagPopup.show( e.getComponent(),
                            e.getX(), e.getY() );
                }
            }
        } );

    }

    private static class AddToAllTagAction extends AbstractAction
        implements ListSelectionListener {

        TagList list;

        public AddToAllTagAction( TagList l ) {
            super( "Add to all", null );
            list = l;
            list.addListSelectionListener( this );
            setEnabled( true );
        }

        public void actionPerformed( ActionEvent e ) {
            TagInfo ti = (TagInfo) list.getSelectedValue();
            if ( ti != null ) {
                list.ctrl.addTag( ti.tag );
            }
        }

        public void valueChanged( ListSelectionEvent e ) {
            TagInfo ti = (TagInfo) list.getSelectedValue();
            setEnabled ( (ti != null) && (ti.isForAll == false) );
        }
    }

    private static class AddTagAction extends AbstractAction {
        TagList list;
        AddTagDlg dlg = null;

        public AddTagAction( TagList l ) {
            super( "Add tag...", null );
            list = l;
        }

        public void actionPerformed( ActionEvent e ) {
            if ( dlg == null ) {
                dlg = new AddTagDlg( null, true );
            }
            dlg.setVisible( true );
            if ( dlg.getReturnStatus() == dlg.RET_OK ) {
                list.ctrl.addTag( dlg.getTag() );
            }
        }

    }

    private static class RemoveTagAction extends AbstractAction
            implements ListSelectionListener {

        TagList list;

        public RemoveTagAction( TagList l ) {
            super( "Remove",null );
            list = l;
            setEnabled( true );
        }

        public void actionPerformed( ActionEvent e ) {
            TagInfo ti = (TagInfo) list.getSelectedValue();
            if ( ti != null ) {
                list.ctrl.removeTag( ti.tag );
            }
        }

        public void valueChanged( ListSelectionEvent e ) {
            TagInfo ti = (TagInfo) list.getSelectedValue();
            setEnabled( ti != null );
        }
    }

}
