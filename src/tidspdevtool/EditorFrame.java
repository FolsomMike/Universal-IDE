/******************************************************************************
* Title: TI DSP Dev Tool - EditorFrame.java File
* Author: Mike Schoonover
* Date: 06/4/11
*
* Purpose:
*
* A JInternalFrame which allows the user to edit a source or text file.
*
* This EditorFrame creates an EditorTabPane, each pane of which is used to
* contain an EditorRig.
*
* The EditorRig is a JPanel containing a JTextPane in a JScrollPane and any
* associated status windows, controls, text boxes which go along with a typical
* editor (such as line/column display, search boxes, etc.)
*
* The JTextPane is tied to an EditorKit. The EditorKit provides functions
* useful for different types of documents. When a text document is loaded, a
* specific editor gets installed automatically; for an HTML document a different
* type of editor gets installed. Each editor has different Actions it can
* perform on the text. The programmer can get a list of these Actions and tie
* them to the desired key commands (such as ctrl-F) or add the Actions to a menu
* for user access. When a new type of document is loaded, the editor might be
* changed so it is necessary to check the available Actions and refresh any
* key bindings and menu options. The programmer decides which (if any) actions
* are tied to which key strokes or displayed on a menu.
*
*  EditorRig.createActionTable creates a hash map of action names and the
*   associated actions available for the JTextPane's current EditorKit. This
*   makes it more convenient to add those actions to key maps and menus.
*  EditorRig::getActionByName is used to easily retrieve an action from the
*   hash map by its name.
*  EditorRig.addBindings is used to connect key commands with actions
*  EditorRig.createEditMenu is used to display actions in a menu
*
* NOTE: EditorFrame handles undo functions -- these make more sense to be
*   handled by EditorRig? Move it there? You can do it, Nicky!
*  (each editor window needs its own undo goodies, extra work done
*   here to keep those contexts separate -- stupid, no?)
*
* Reference
*
*  The Java Tutorials ~ TextComponentDemo.java
*       JTextPane with undo/redo code, JSplitPane
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package tidspdevtool;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.undo.*;


//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class EditorFrame
//
//

public class EditorFrame extends JInternalFrame implements ChangeListener
{

    EditorTabPane editorTabPane;

    private UndoAction undoAction;
    private RedoAction redoAction;
    MyUndoableEditListener myUndoableEditListener;

//-----------------------------------------------------------------------------
// EditorFrame::EditorFrame (constructor)
//
//

public EditorFrame(String pTitle, boolean pResizable, boolean pCloseable,
                                   boolean pMaximizable, boolean pIconifiable) {

    super(pTitle, pResizable, pCloseable, pMaximizable, pIconifiable);

}//end of EditorFrame::EditorFrame (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorFrame::init
//
//

public void init()
{

    //undo and redo are actions of our own creation -- these actions are used
    //by the editor pane and the menu
    undoAction = new UndoAction();
    redoAction = new RedoAction();
    myUndoableEditListener = new MyUndoableEditListener();

    editorTabPane = new EditorTabPane(this);
    editorTabPane.init();

    getContentPane().add(editorTabPane);

}//end of EditorFrame::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorFrame::loadFile
//
// Loads a file from disk into the EditorRig on the currently selected tab.
//

public void loadFile(String pFileName, String pFullpath)
{

    //create a scrolling editing pane with associated components
    EditorRig editorRig = new EditorRig(this);
    editorRig.init(myUndoableEditListener);

    editorTabPane.addTab(pFileName, pFullpath, editorRig);

    //Set up the menu bar.

    JMenu editMenu = editorRig.createEditMenu(undoAction, redoAction);
    JMenu styleMenu = editorRig.createStyleMenu();
    JMenuBar mb = new JMenuBar();
    mb.add(editMenu);
    mb.add(styleMenu);
    setJMenuBar(mb);

    //get the EditorRig component on the last tab in the list, which would be
    //the one just created above
    ((EditorRig)editorTabPane.getComponentAt(editorTabPane.getTabCount()-1)).
                                                           loadFile(pFullpath);

}//end of EditorFrame::loadFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorFrame::saveFile
//
// Saves a file to disk from the EditorRig on the currently selected tab.
//
// The file is saved to the same path from which it was loaded.
//

public void saveFile()
{

    //get the EditorRig component on the last tab in the list, which would be
    //the one just created above
    ((EditorRig)editorTabPane.getSelectedComponent()).saveFile();

}//end of EditorFrame::saveFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorFrame::saveFileAs
//
// Saves a file to disk from the EditorRig on the currently selected tab.
//
// A file browser will be displayed to allow the user to specify the file
// name and path.
//

public void saveFileAs()
{

    //wip mks -- add code

}//end of EditorFrame::saveFileAs
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorFrame::addDocModifiedFlag
//
// Adds an asterisk to the name of the tab for pEditorRig. This shows the
// user which files have been modified.
//

public void addDocModifiedFlag(EditorRig pEditorRig)
{

    int i = editorTabPane.indexOfComponent(pEditorRig);
    String tabTitle = editorTabPane.getTitleAt(i);

    if (!tabTitle.endsWith("*")){
        editorTabPane.setTitleAt(i, tabTitle + "*");
        editorTabPane.getTabComponentAt(i).revalidate();
    }

}//end of EditorFrame::addDocModifiedFlag
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorFrame::removeDocModifiedMarker
//
// Removes the asterisk in the name of the tab for pEditorRig. This shows the
// user which files have not been modified.
//

public void removeDocModifiedMarker(EditorRig pEditorRig)
{

    int i = editorTabPane.indexOfComponent(pEditorRig);
    String tabTitle = editorTabPane.getTitleAt(i);

    if (tabTitle.endsWith("*")){
        editorTabPane.setTitleAt(i, tabTitle.substring(0, tabTitle.length()-1));
        editorTabPane.getTabComponentAt(i).revalidate();
    }

}//end of EditorFrame::removeDocModifiedMarker
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorFrame::getSelectedUndoMgr
//
// Returns the undo manager for the document in the EditorRig on the currently
// selected tab.
//
// A single undo manager can handle multiple documents, but this causes
// annoying behavior -- the user can be viewing one document and using the
// undo action will undo the last change made, which might be to a different
// document altogether.
//
// NOTE: The undo manager is able to keep the changes to the different
// documents separate because calls by the documents to UndoableEditListener
// pass references to the documents themselves.  Thus the a reference to the
// modified document is stored with each change -- when the UndoManager is then
// told to undo a change, it applies each undo back to the appropriate doc.
//
// For this program, a separate undo manager is created for each EditorRig and
// thus for each document.  When any document is modified and
// UndoableEditListener is called, the changes are stored in the undo manager
// associated with the currently visible document.  Likewise, when the user
// actives an undo/redo function, the undo manager for the currently visible
// document is used.  This keeps changes for each document separate which is
// the generally expected behavior for an IDE.
//
// Returns a reference to the currently selected UndoManager if possible.
// Returns null if there is an error.
//

private UndoManager getSelectedUndoMgr()
{

    //get the undo manager for the currently selected document

    Component p = editorTabPane.getSelectedComponent();
    //do nothing if selected component is not an EditorRig
    if (!(p instanceof EditorRig)) {return(null);}
    EditorRig er = (EditorRig)p;

    return(er.undo);

}//end of EditorFrame::getSelectedUndoMgr
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorFrame::stateChanged
//
// Responds to value changes in spinners, tabbed panes etc.
//
// You can tell which item was changed by using similar to:
//
// Object source = e.getSource();
//

@Override
public void stateChanged(ChangeEvent e)
{

    if (e.getSource() instanceof EditorTabPane){

        EditorTabPane etp;
        etp = (EditorTabPane)e.getSource();

        //get the undo manager for the currently selected document
        UndoManager undo = getSelectedUndoMgr();
        if (undo == null) {return;}

        //update the menus to show the last type of change made
        undoAction.updateUndoState(undo);
        redoAction.updateRedoState(undo);

    }

}//end of EditorFrame::stateChanged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class UndoAction
//
//

class UndoAction extends AbstractAction {

//-----------------------------------------------------------------------------
// UndoAction::UndoAction (constructor)
//

public UndoAction()
{

    super("Undo");
    setEnabled(false);

}//end of UndoAction::UndoAction (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UndoAction::actionPerformed
//

public void actionPerformed(ActionEvent e)
{

    //get the undo manager for the currently selected document
    UndoManager undo = getSelectedUndoMgr();
    if (undo == null) {return;}

    try {
        undo.undo();
    }
    catch (CannotUndoException ex) {
        System.out.println("Unable to undo: " + ex);
        //for debugging add this -> ex.printStackTrace();
    }

    updateUndoState(undo);
    redoAction.updateRedoState(undo);

}//end of UndoAction::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UndoAction::updateUndoState
//

protected void updateUndoState(UndoManager pUndo)
{

    if (pUndo.canUndo()) {
        setEnabled(true);
        putValue(Action.NAME, pUndo.getUndoPresentationName());
    }
    else {
        setEnabled(false);
        putValue(Action.NAME, "Undo");
    }

}//end of UndoAction::updateUndoState
//-----------------------------------------------------------------------------

}//end of class UndoAction
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class RedoAction
//
//

class RedoAction extends AbstractAction {


//-----------------------------------------------------------------------------
// RedoAction::RedoAction (constructor)
//


public RedoAction()
{

    super("Redo");

    setEnabled(false);

}//end of RedoAction::RedoAction (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RedoAction::actionPerformed
//

public void actionPerformed(ActionEvent e)
{

    //get the undo manager for the currently selected document
    UndoManager undo = getSelectedUndoMgr();
    if (undo == null) {return;}

    try {
        undo.redo();
        }
    catch (CannotRedoException ex) {
            System.out.println("Unable to redo: " + ex);
            //for debugging add this -> ex.printStackTrace();
    }

    updateRedoState(undo);
    undoAction.updateUndoState(undo);

}//end of RedoAction::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RedoAction::updateRedoState
//

protected void updateRedoState(UndoManager pUndo)
{

    if (pUndo.canRedo()) {
        setEnabled(true);
        putValue(Action.NAME, pUndo.getRedoPresentationName());
    }
    else {
        setEnabled(false);
        putValue(Action.NAME, "Redo");
    }

}//end of RedoAction::updateRedoState
//-----------------------------------------------------------------------------

}//end of class RedoAction
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class MyUndoableEditListener
//
// This one listens for edits that can be undone.
//

protected class MyUndoableEditListener implements UndoableEditListener {

//-----------------------------------------------------------------------------
// MyUndoableEditListener::undoableEditHappened
//

public void undoableEditHappened(UndoableEditEvent e) {

    //get the undo manager for the currently selected document
    UndoManager undo = getSelectedUndoMgr();
    if (undo == null) {return;}

    //remember the edit and update the menus to show last type of change made

    undo.addEdit(e.getEdit());
    undoAction.updateUndoState(undo);
    redoAction.updateRedoState(undo);

}//end of MyUndoableEditListener::undoableEditHappened
//-----------------------------------------------------------------------------

}//end of class MyUndoableEditListener
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

}//end of class EditorFrame
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
