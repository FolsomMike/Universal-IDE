/******************************************************************************
* Title: Universal IDE - CodeHandler.java
* Author: Mike Schoonover
* Date: 2/19/13
*
* Purpose:
*
* This base class manages software source code processing tasks such as
* assembling, linking, listing, debugging, and simulating. Subclasses should
* provide the specific actions required for the type of source code being
* handled.
*
* It will install its own menu on the menu bar passed via the constructor and
* handle all actions for that menu.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package codehandler;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

//-----------------------------------------------------------------------------
// class CodeHandler
//

public class CodeHandler implements ActionListener
{

    String projectPath;

    JMenuBar menuBar;
    JMenu dspMenu;
    JMenuItem assembleProject;

    ActionListener actionListener;

//-----------------------------------------------------------------------------
// CodeHandler::CodeHandler (constructor)
//
// Creates a basic code handler with common features used for handling all
// types of code.
//

public CodeHandler(JMenuBar pMenuBar, String pProjectPath,
                                               ActionListener pActionListener)
{

    menuBar = pMenuBar; projectPath = pProjectPath;
    actionListener = pActionListener;

}//end of CodeHandler::CodeHandler (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CodeHandler::init
//
// Initializes new objects. Should be called immediately after instantiation.
//
// The base class should override this method to provide extended setup -- it
// should call the init method here in the base class before peforming its own
// setup.
//

public void init()
{

    //add a menu specific to this class's actions to the menu bar
    addMenuToMenuBar();

}//end of CodeHandler::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CodeHandler::addMenuToMenuBar
//
// Adds a menu specific to this class's action to the menu bar. This class and
// its subclasses will act as the action listener for the menu items.
//

public void addMenuToMenuBar()
{

    //DSP menu
    dspMenu = new JMenu("Code Tools");
    dspMenu.setToolTipText("Assembling, compiling, linking, simulating...");
    menuBar.add(dspMenu);

    //DSP/Assemble Project menu item
    assembleProject = new JMenuItem("Assemble Project");
    assembleProject.setToolTipText(
                                "Assembles the source files in the project.");
    assembleProject.addActionListener(this);
    dspMenu.add(assembleProject);

}//end of CodeHandler::addMenuToMenuBar
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CodeHandler::setProjectPath
//
// Sets projectPath to pProjectPath
//


public void setProjectPath(String pProjectPath)
{

    projectPath = pProjectPath;

}//end of CodeHandler::setProjectPath
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CodeHandler::actionPerformed
//
// Responds to button events, menu events, etc.
//

@Override
public void actionPerformed(ActionEvent e)
{

    //allow user to choose a different project and then load it
    if ("Assemble Project".equals(e.getActionCommand())) {
        assembleProject();
        return;
    }

}//end of CodeHandler::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CodeHandler::assembleProject
//
// Child classes should override this method to provide appropriate processing.
//


public void assembleProject()
{


}//end of CodeHandler::assembleProject
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CodeHandler::errorMsg
//
// Displays an error dialog with message pMessage.
//

static void errorMsg(String pMessage)
{

    JOptionPane.showMessageDialog(null, pMessage,
                                            "Error", JOptionPane.ERROR_MESSAGE);

}//end of CodeHandler::errorMsg
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CodeHandler::waitSleep
//
// Sleeps for pTime milliseconds.
//

public void waitSleep(int pTime)
{

    try {Thread.sleep(pTime);} catch (InterruptedException e) { }

}//end of CodeHandler::waitSleep
//-----------------------------------------------------------------------------

}//end of class CodeHandler
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
