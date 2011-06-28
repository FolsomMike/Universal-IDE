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
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class EditorFrame
//
//

class EditorFrame extends JInternalFrame implements ChangeListener
{

EditorTabPane editorTabPane;

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
editorRig.init();

editorTabPane.addTab(pFileName, pFullpath, editorRig);

//Set up the menu bar.

JMenu editMenu = editorRig.createEditMenu();
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


}//end of class EditorFrame
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
