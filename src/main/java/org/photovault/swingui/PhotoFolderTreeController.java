package org.photovault.swingui;

import java.awt.event.ActionEvent;
import java.util.Vector;
import javax.swing.JOptionPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import org.hibernate.Session;
import org.photovault.command.CommandException;
import org.photovault.command.CommandHandler;
import org.photovault.command.PhotovaultCommandHandler;
import org.photovault.folder.ChangePhotoFolderCommand;
import org.photovault.folder.CreatePhotoFolderCommand;
import org.photovault.folder.DeletePhotoFolderCommand;
import org.photovault.folder.PhotoFolder;
import org.photovault.folder.PhotoFolderModifiedEvent;
import org.photovault.folder.PhotoFolderDAO;
import org.photovault.folder.PhotoFolderEvent;
import org.photovault.swingui.framework.DataAccessAction;
import org.photovault.swingui.framework.DefaultEvent;
import org.photovault.swingui.framework.DefaultEventListener;
import org.photovault.swingui.framework.PersistenceController;



public class PhotoFolderTreeController extends PersistenceController implements TreeSelectionListener
{
    public PhotoFolderTree folderTree = null;
    PhotoFolderTreeModel model = null;
    PhotoFolder selected = null;
    /**
    Data access to folders in database
    TODO: CHeck where this should be managed!!!
     */
    
    PhotoFolderDAO folderDAO;
    
    public PhotoFolderTreeController() {
        super();
        folderDAO = getDAOFactory().getPhotoFolderDAO();
        model = new PhotoFolderTreeModel();
        model.setController( this );
        
        folderTree = new PhotoFolderTree(this);
        
        registerAction( PhotoFolderTree.FOLDER_NEW_CMD, new DataAccessAction() {
            public void actionPerformed(ActionEvent actionEvent, Session currentSession) {
                if (selected != null) {
                    boolean ready = false;
                    while (!ready) {
                        String newName = (String) JOptionPane.showInputDialog( folderTree, "Enter name for new folder",
                                    "New folder", JOptionPane.PLAIN_MESSAGE,
                                    null, null, "New folder" );
                        if (newName != null) {
                            if (newName.length()  > PhotoFolder.NAME_LENGTH) {
                                JOptionPane.showMessageDialog( folderTree,
                                        "Folder name cannot be longer than " + PhotoFolder.NAME_LENGTH + " characters",
                                        "Too long name", JOptionPane.ERROR_MESSAGE, null );
                            }  else {
                                CreatePhotoFolderCommand createCmd = new CreatePhotoFolderCommand( selected, newName, "" );
                                try {
                                    getCommandHandler().executeCommand( createCmd );
                                    PhotoFolder createdFolder = createCmd.getCreatedFolder();
                                    ready = true;
                                }  catch (CommandException ex) {
                                    JOptionPane.showMessageDialog( folderTree,
                                            "Error happened while creating the folder:\n"
                                                + ex.getMessage(),
                                            "Error creating folder", JOptionPane.ERROR_MESSAGE, null );
                                    
                                }
                            }
                        }  else {
                            // User pressed Cancel
                            ready = true;
                        }
                    }
                }
            }
            });
        
        registerAction( PhotoFolderTree.FOLDER_RENAME_CMD, new DataAccessAction() {
            public void actionPerformed(ActionEvent actionEvent, Session currentSession) {
                if (selected != null) {
                    String origName = selected.getName();
                    boolean ready = false;
                    while (!ready) {
                        String newName = (String) JOptionPane.showInputDialog( folderTree, "Enter new name",
                                    "Rename folder", JOptionPane.PLAIN_MESSAGE,
                                    null, null, origName );
                        if (newName != null) {
                            ChangePhotoFolderCommand changeCmd = new ChangePhotoFolderCommand( selected );
                            changeCmd.setName( newName );
                            try {
                                getCommandHandler().executeCommand( changeCmd );
                                PhotoFolder changedFolder = changeCmd.getChangedFolder();
                                PhotoFolder mergedFolder = (PhotoFolder) getPersistenceContext().merge( changedFolder  );
                                model.photoCollectionChanged( new PhotoFolderEvent(mergedFolder, mergedFolder, null) );
                            }  catch (CommandException e) {
                                JOptionPane.showMessageDialog( folderTree,
                                        "Error occurred while changing folder name: \n"
                                            + e.getMessage(),
                                        "Error changing folder name",
                                        JOptionPane.ERROR_MESSAGE, null );
                            }
                            ready = true;
                        }  else {
                            ready = true;
                        }
                    }
                }
            }
        });
        
        registerAction( PhotoFolderTree.FOLDER_DELETE_CMD, new DataAccessAction(){
            public void actionPerformed(ActionEvent actionEvent, Session currentSession) {
                if (selected != null) {
                    // Ask for confirmation
                    if (JOptionPane.showConfirmDialog( folderTree, "Delete folder " + selected.getName() + "?",
                            "Delete folder", JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE, null )
                            == JOptionPane.YES_OPTION) {
                        DeletePhotoFolderCommand deleteCmd = new DeletePhotoFolderCommand( selected );
                        try {
                            getCommandHandler().executeCommand( deleteCmd );
                        } catch (CommandException e) {
                            JOptionPane.showMessageDialog( folderTree,
                                    "Error occurred while deleting folder: \n"
                                    + e.getMessage(),
                                    "Error deleting folder",
                                    JOptionPane.ERROR_MESSAGE, null );
                            
                        }
                    }
                }
            }
        });
        
        this.registerEventListener( PhotoFolderModifiedEvent.class, new DefaultEventListener<PhotoFolder>() {
            public void handleEvent( DefaultEvent<PhotoFolder> evt ) {
                // Merge changes to current persistence context
                // and send event to the tree.
                PhotoFolder mergedFolder = (PhotoFolder) getPersistenceContext().merge( evt.getPayload() );
                model.structureChanged( new PhotoFolderEvent(selected, selected, null) );
            }
        });
    }
    
    /**
     Returns the currently selected PhotoFolder or <code>null</code> if none is selected.
     */
    public PhotoFolder getSelected() {
        return selected;
    }
    
    public void setSelected(PhotoFolder folder) {
        Vector parents = new Vector();
        parents.add( folder );
        while ( (folder = folder.getParentFolder() ) != null ) {
            parents.add( 0, folder );
        }
        TreePath path = new TreePath( parents.toArray() );
        selected = folder;
        folderTree.tree.setSelectionPath( path );
    }

    /**
     Implementation of TreeSelectionListener interface. This method is called when tree selection changes
     */
    
    public void valueChanged(TreeSelectionEvent e) {
        selected = (PhotoFolder) folderTree.tree.getLastSelectedPathComponent();
        fireEvent( new PhotoFolderTreeEvent(this, selected) );
    }        
}