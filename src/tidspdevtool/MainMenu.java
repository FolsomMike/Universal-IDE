/******************************************************************************
* Title: TI DSP Dev Tool - MainMenu.java
* Author: Mike Schoonover
* Date: 2/5/13
*
* Purpose:
*
* This class contains a menu bar which can be added to the main frame.
* It does not handle the menu actions, but passes those to a handler specified
* by the owner.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package tidspdevtool;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;


//-----------------------------------------------------------------------------
// class MainMenu
//

class MainMenu extends JMenuBar
{

    ActionListener actionListener;

    JMenu fileMenu;

    JMenuItem loadFile;
    JMenuItem saveFile;
    JMenuItem loadProject;
    JMenuItem saveProject;
    JMenuItem newProject;
    JMenuItem exit;


//-----------------------------------------------------------------------------
// MainMenu::MainMenu (constructor)
//
// Creating object should pass an ActionListener which will respond to the
// menu actions.
//

MainMenu(ActionListener pActionListener)
{

    actionListener = pActionListener;

}//end of MainMenu::MainMenu (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainMenu::init
//
// Initializes new objects. Should be called immediately after instantiation.
//

public void init()
{

    //File menu
    fileMenu = new JMenu("File");
    fileMenu.setMnemonic(KeyEvent.VK_F);
    fileMenu.setToolTipText("File");
    add(fileMenu);

    //File/Load File menu item
    loadFile = new JMenuItem("Load File");
    loadFile.setMnemonic(KeyEvent.VK_L);
    loadFile.setToolTipText("Load File");
    loadFile.addActionListener(actionListener);
    fileMenu.add(loadFile);

    //File/Save File menu item
    saveFile = new JMenuItem("Save File");
    saveFile.setMnemonic(KeyEvent.VK_S);
    saveFile.setToolTipText("Save File");
    saveFile.addActionListener(actionListener);
    fileMenu.add(saveFile);

    //File/Load Project menu item
    loadProject = new JMenuItem("Load Project");
    loadProject.setMnemonic(KeyEvent.VK_P);
    loadProject.setToolTipText("Load Project");
    loadProject.addActionListener(actionListener);
    fileMenu.add(loadProject);

    //File/New Project menu item
    newProject = new JMenuItem("New Project");
    newProject.setMnemonic(KeyEvent.VK_R);
    newProject.setToolTipText("New Project");
    newProject.addActionListener(actionListener);
    fileMenu.add(newProject);

}//end of MainMenu::init
//-----------------------------------------------------------------------------


}//end of class MainMenu
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
