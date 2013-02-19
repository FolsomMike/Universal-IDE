/******************************************************************************
* Title: TI DSP Dev Tool - EditorTabPane.java File
* Author: Mike Schoonover
* Date: 06/26/11
*
* Purpose:
*
* This class handles a tabbed pane containing editor windows.
*
* See notes at the top of EditorFrame.java for a detailed overview of the
* relationship between EditorFrame, EditorRig, EditorTabPane, the JTextPane,
* and the EditorKit used by the JTextPane.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package tidspdevtool;


import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class EditorTabPane
//
//

public class EditorTabPane extends JTabbedPane{

    Settings settings;
    ChangeListener lChangeListener;

    private final int tabNumber = 5;
    private JMenuItem tabComponentsItem;
    private JMenuItem scrollLayoutItem;


    //public String getEditorTabPaneFullPath(){return EditorTabPaneFullPath;}
    //public void setEditorTabPaneFullPath(String pS){
    //    EditorTabPaneFullPath = pS;
    //}

    public ArrayList<File> EditorTabPane = new ArrayList<File>();

//-----------------------------------------------------------------------------
// EditorTabPane::EditorTabPane (constructor)
//

EditorTabPane(ChangeListener pChangeListener)
{

    lChangeListener = pChangeListener;

}//end of EditorTabPane::EditorTabPane (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorTabPane::init
//
// Sets up the component.
//

public void init()
{

    setName("Editor Tab Panel");
    addChangeListener(lChangeListener);
    initMenu();
    removeAll();
    setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

}//end of EditorTabPane::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorTabPane::debugTest
//
// This is a debug testing function which installs some blank tabs on the panel.
//
// External object can call this after calling the init function.
//

public void debugTest() {

    removeAll();

    for (int i = 0; i < tabNumber; i++) {
        String title = "Tab " + i;
        //adds a pane with a label on it
        add(title, new JLabel(title));
        //installs a component to draw the tab's title and icons
        initTabComponent(i);
        }

    tabComponentsItem.setSelected(true);
    setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
    scrollLayoutItem.setSelected(false);

}//end of EditorTabPane::debugTest
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorTabPane::addTab
//
// Adds a tab with name pFileName and mouseover tip pFullPath and adds
// pPanel to the tab.
//

public void addTab(String pFileName, String pFullPath, JPanel pPanel) {

    addTab(pFileName, null, pPanel, pFullPath);
    initTabComponent(getTabCount()-1);

}//end of EditorTabPane::addTab
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorTabPane::initTabComponent
//
// Creates a tab and adds it to the tab pane.
//

private void initTabComponent(int i) {

    setTabComponentAt(i, new TabDecorator(this));

}//end of EditorTabPane::initTabComponent
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorTabPane::initMenu
//
// Sets up the menu.
//

private void initMenu() {

    JMenuBar menuBar = new JMenuBar();

    //create Options menu

    tabComponentsItem = new JCheckBoxMenuItem("Use TabComponents", true);
    tabComponentsItem.setAccelerator(KeyStroke.getKeyStroke(
                                          KeyEvent.VK_T, InputEvent.ALT_MASK));

    tabComponentsItem.addActionListener(

        new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < getTabCount(); i++) {
                    if (tabComponentsItem.isSelected()) {
                        //installs a component to draw the tab's title and icons
                        initTabComponent(i);
                    }
                    else {
                        //sets JTabbedPane class to draw the tab title
                        setTabComponentAt(i, null);
                    }
                }
            }
        }
    );

    scrollLayoutItem = new JCheckBoxMenuItem("Set ScrollLayout");
    scrollLayoutItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_MASK));

    scrollLayoutItem.addActionListener(
        new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (getTabLayoutPolicy() == JTabbedPane.WRAP_TAB_LAYOUT) {
                    setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
                }
                else {
                    setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
                }
            }
        }
    );

    JMenuItem resetItem = new JMenuItem("Reset JTabbedPane");
    resetItem.setAccelerator(KeyStroke.getKeyStroke(
                                         KeyEvent.VK_R, InputEvent.ALT_MASK));

    resetItem.addActionListener(
        new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                debugTest();
            }
        }
    );

    JMenu optionsMenu = new JMenu("Options");
    optionsMenu.add(tabComponentsItem);
    optionsMenu.add(scrollLayoutItem);
    optionsMenu.add(resetItem);
    menuBar.add(optionsMenu);

    //debug mks - this object was originally a JPanel -- now is a JTabbedPane
    // can't set a menu here anymore -- needs to be moved to parent component
    //setJMenuBar(menuBar);

}//end of EditorTabPane::initMenu
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorFrame::prepareToClose
//
// This function should be called when the editor is about to be closed. If
// any document has been modified, the user will be given a chance to save it
// or cancel the closing.
//
// User will be asked if it is desired to save each modified file.
//
// Returns true if the files have not been modified or if the user chooses to
// close for each request (saving or not saving the file).
// Returns false if the user chooses to cancel at any prompt.
//

public boolean prepareToClose() {

    boolean whatToDo = false;

    int totalTabs = getTabCount();

    for(int i = 0; i < totalTabs; i++){

       EditorRig rig = (EditorRig)getComponentAt(i);

       //check for modified file and allow user to handle it
       whatToDo = rig.prepareToClose();

       //bail out and return false if user canceled at any save prompt
       if (!whatToDo) {break;}

    }

    return(whatToDo);

}//end of EditorFrame::prepareToClose
//-----------------------------------------------------------------------------

}//end of class EditorTabPane
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------