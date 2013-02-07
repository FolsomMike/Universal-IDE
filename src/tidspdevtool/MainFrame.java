/******************************************************************************
* Title: TI DSP Dev Tool - Main Source File
* Author: Mike Schoonover
* Date: 2/3/13
*
* Purpose:
*
* This class manages the main JFrame window of the application.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package tidspdevtool;

//-----------------------------------------------------------------------------

import DSPSimulation.Chip;
import SpecificChips.TMS320VC5441;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.DecimalFormat;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

//-----------------------------------------------------------------------------
// class MainFrame
//
// Creates a main window JFrame.  Creates all other objects needed by the
// program.
//
// Listens for events generated by the main window.  Calls clean up functions
// on program exit.
//

class MainFrame extends JFrame implements WindowListener, ActionListener,
                        ChangeListener, ComponentListener, DocumentListener{

    Settings settings;
    MainMenu mainMenu;
    Project project;

    Chip chip;

    JDialog measureDialog;
    GridBagLayout gridBag;

    int initialWidth, initialHeight;

    DecimalFormat[] decimalFormats;

//-----------------------------------------------------------------------------
// MainFrame::MainFrame (constructor)
//

public MainFrame(String pTitle)
{

    super(pTitle);

}//end of MainFrame::MainFrame (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::init
//
// Initializes new objects. Should be called immediately after instantiation.
//

public void init()
{

    //choose which chip to simulate
    chip = new TMS320VC5441();

    //turn off default bold for Metal look and feel
    UIManager.put("swing.boldMetal", Boolean.FALSE);

    //force "look and feel" to the System's default style
    // getCrossPlatformLookAndFeelClassName() -- this is the "Metal" look
    // getSystemLookAndFeelClassName() -- uses the look from the System
    //   (the System style matches Windows, Mac, Linux, etc.)

    try {
        UIManager.setLookAndFeel(
            UIManager.getCrossPlatformLookAndFeelClassName());
    }
    catch (Exception e) {}

    //makes sure all frames are created with the look and feel specified above
    JFrame.setDefaultLookAndFeelDecorated(true);

    //create project, settings, load value from file
    loadSettings();

    //create a main menu, passing this as the object to be installed as
    //the action and item listener for the menu
    mainMenu = new MainMenu(this);
    mainMenu.init();
    setJMenuBar(mainMenu);

    //create various decimal formats
    decimalFormats = new DecimalFormat[1];
    decimalFormats[0] = new  DecimalFormat("0000000");

    // Add internal frame to desktop
    JDesktopPane desktop = new JDesktopPane();
    desktop.setOpaque(true);
    desktop.setBackground(Color.LIGHT_GRAY);

    //create the program's main window
    addComponentListener(this);
    addWindowListener(this);

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    //add the desktop panel to the main frame
    //the desktop panel handles child windows such that and IDE can be created
    getContentPane().add(desktop, BorderLayout.CENTER);
    Settings.setSizes(this, 1024, 725);

    //create a main menu, passing settings as the object to be installed as
    //the action and item listener for the menu
    //mainFrame.setJMenuBar(mainMenu = new MainMenu(settings));

    setVisible(true);

    //store the height and width of the main window after it has been set up so
    //it can be restored to this size if an attempt is made to resize it
    initialWidth = getWidth();
    initialHeight = getHeight();

    // Create an internal frame
    boolean resizable = true;
    boolean closeable = true;
    boolean maximizable  = true;
    boolean iconifiable = true;

    int width, height;

    ProjectFrame projectFrame =
        new ProjectFrame("Project", resizable, closeable, maximizable,
            iconifiable, settings);
    projectFrame.init();

    settings.projectFrame = projectFrame;

    // set an initial size for the project window
    width = 200; height = 500; projectFrame.setSize(width, height);

    // by default, internal frames are not visible; make it visible
    projectFrame.setVisible(true);

    desktop.add(projectFrame);

    //force layout so location and width of the project window can be retrieved
    pack();

    int editorWindowX = projectFrame.getX() + projectFrame.getWidth();

    String title = "Editor";
    EditorFrame editorFrame =
        new EditorFrame(title, resizable, closeable, maximizable, iconifiable);
    editorFrame.init();

    settings.editorFrame = editorFrame;

    // position the editor window just right of the project window
    editorFrame.setLocation(new Point(editorWindowX, 0));

    // set an initial size for the editor window
    width = 1000; height = 680; editorFrame.setSize(width, height);

    // by default, internal frames are not visible; make it visible
    editorFrame.setVisible(true);

    desktop.add(editorFrame);

    editorFrame.loadFile(
                "Capulin UT DSP.asm", "ASM Source Files//Capulin UT DSP.asm");

    editorFrame.loadFile(
                    "Documentation.txt", "ASM Source Files//Documentation.txt");

    //force layout of GUI
    pack();

    //force garbage collection before beginning any time sensitive tasks
    System.gc();

}//end of MainFrame::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::loadSettings
//
// Loads global values, project file, and all other settings required upon
// program startup.
//

private void loadSettings()
{

    settings = new Settings();
    settings.init();

    project = new Project(settings);
    project.init();

    settings.mainFrame = this;
    settings.sourceCodeFileList = project.sourceCodeFileList.list;
    settings.linkerFileList = project.linkerFileList.list;
    settings.docFileList = project.docFileList.list;

}//end of MainFrame::loadSettings
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::actionPerformed
//
// Responds to button events, menu events, etc.
//

@Override
public void actionPerformed(ActionEvent e)
{

    //this part handles saving all data
    if ("Load Project".equals(e.getActionCommand())) {
        project.chooseProject();
        return;
    }

}//end of MainFrame::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::stateChanged
//
// Responds to value changes in spinners, etc.
//
// You can tell which item was changed by using similar to:
//
// Object source = e.getSource();
//
// For simplicities sake, the following just updates all controls any time any
// one control is changed.
//

@Override
public void stateChanged(ChangeEvent e)
{


}//end of MainFrame::stateChanged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::(various component listener functions)
//
// These functions are implemented per requirements of interface
// ComponentListener but do nothing at the present time.  As code is added to
// each function, it should be moved from this section and formatted properly.
//

@Override
public void componentHidden(ComponentEvent e){}
@Override
public void componentShown(ComponentEvent e){}
@Override
public void componentMoved(ComponentEvent e){}
@Override
public void componentResized(ComponentEvent e){}

//end of MainFrame::(various component listener functions)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::insertUpdate
//
// Responds to value changes in text areas and fields, specifically when a
// character is inserted. NOTE: This will get called after EVERY character
// inserted - updateAllSettings will get called every time but this shouldn't
// be a problem.
//

@Override
public void insertUpdate(DocumentEvent ev)
{

}//end of MainFrame::insertUpdate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::removeUpdate
//
// Responds to value changes in text areas and fields, specifically when a
// character is removed. NOTE: This will get called after EVERY character
// removed - updateAllSettings will get called every time but this shouldn't
// be a problem.
//

@Override
public void removeUpdate(DocumentEvent ev)
{

}//end of MainFrame::removeUpdate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::changedUpdate
//
// Responds to value changes in text areas and fields, specifically when the
// style of the text is changed.
//

@Override
public void changedUpdate(DocumentEvent ev)
{

}//end of MainFrame::changedUpdate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::windowClosing
//
// Handles actions necessary when the window is closing
//

@Override
public void windowClosing(WindowEvent e)
{

    settings.saveFile();
    project.saveFile();

}//end of MainFrame::windowClosing
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::(various window listener functions)
//
// These functions are implemented per requirements of interface WindowListener
// but do nothing at the present time.  As code is added to each function, it
// should be moved from this section and formatted properly.
//

@Override
public void windowActivated(WindowEvent e){}
@Override
public void windowDeactivated(WindowEvent e){}
@Override
public void windowOpened(WindowEvent e){}
//@Override
//public void windowClosing(WindowEvent e){}
@Override
public void windowClosed(WindowEvent e){}
@Override
public void windowIconified(WindowEvent e){}
@Override
public void windowDeiconified(WindowEvent e){}

//end of MainFrame::(various window listener functions)
//-----------------------------------------------------------------------------

}//end of class MainFrame
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
