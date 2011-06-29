/******************************************************************************
* Title: TI DSP Dev Tool - EditorFrame.java File
* Author: Mike Schoonover
* Date: 06/4/11
*
* Purpose:
*
* A JInternalFrame which allows the user to edit a source or text file.
*
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package tidspdevtool;

import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.undo.*;
import javax.swing.text.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class EditorFrame
//
//

class EditorFrame extends JInternalFrame implements ChangeListener
{

EditorTabPane editorTabPane;

private UndoAction undoAction;
private RedoAction redoAction;
MyUndoableEditListener myUndoableEditListener;

//undo manager -- handles undo/redo actions
public UndoManager undo = new UndoManager();


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
EditorRig editorRig = new EditorRig();
editorRig.init(myUndoableEditListener);

editorTabPane.addTab(pFileName, pFullpath, editorRig);

//Set up the menu bar.

JMenu editMenu = editorRig.createEditMenu(undoAction, redoAction);
JMenu styleMenu = editorRig.createStyleMenu();
JMenuBar mb = new JMenuBar();
mb.add(editMenu);
mb.add(styleMenu);
setJMenuBar(mb);

//get the EditorRig component on the last tab in the list, which would be the
//one just created above
((EditorRig)editorTabPane.getComponentAt(editorTabPane.getTabCount()-1)).
                                                           loadFile(pFullpath);

}//end of EditorFrame::loadFile
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

int i;

if (e.getSource() instanceof EditorTabPane){
    EditorTabPane etp;
    etp = (EditorTabPane)e.getSource();
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
    
//do nothing if triggering object is not an AbstractDocument
//if (!(e.getSource() instanceof AbstractDocument)) return;

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

//Remember the edit and update the menus.

undo.addEdit(e.getEdit());
undoAction.updateUndoState(undo);
redoAction.updateRedoState(undo);

int i = 0;
i++;

}//end of MyUndoableEditListener::undoableEditHappened
//-----------------------------------------------------------------------------

}//end of class MyUndoableEditListener
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

}//end of class EditorFrame
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
