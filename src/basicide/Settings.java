/******************************************************************************
* Title: Universal IDE - Settings.java
* Author: Mike Schoonover
* Date: 06/25/11
*
* Purpose:
*
* This class handles values used throughout the program.  It has functions to
* save and load the values from a file.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package basicide;


import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Settings
//
//

public class Settings extends Object{

    public JFrame mainFrame;
    public ProjectFrame projectFrame;
    EditorFrame editorFrame;

    public ArrayList<File> sourceCodeFileList;
    public ArrayList<File> linkerFileList;
    public ArrayList<File> docFileList;

    //path to the project folder and the project file name
    private String projectFullPath = "";
    //just the path to the project folder
    private String projectPath = "";
    //just the project name (and thus the project file)
    private String projectName = "";

    public String getProjectFullPath(){return projectFullPath;}
    public void setProjectFullPath(String pS){projectFullPath = pS;}

    public String getProjectPath(){return projectPath;}
    public void setProjectPath(String pS){projectPath = pS;}

    public String getProjectName(){return projectName;}
    public void setProjectName(String pS){projectName = pS;}

//-----------------------------------------------------------------------------
// Settings::Settings (constructor)
//

Settings()
{

}//end of Settings::Settings (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::init
//
// Initializes new objects. Should be called immediately after instantiation.
//

public void init()
{

    loadFile();

}//end of Settings::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::loadFile
//
// Loads values from file.
//

private void loadFile()
{

    IniFile ini;


    try {

        ini = new IniFile("Settings/Settings.ini", "UTF-8");

        setNamesAndPaths(ini.readString("General", "Current Project Path", ""));

    }
    catch(IOException e){
        Settings.errorMsg("Error loading Settings file");
    }

}//end of Settings::loadFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::setNamesAndPaths
//
// Sets the project's full path, path, and name.
//

public void setNamesAndPaths(String pFullPath)
{

    setProjectFullPath(pFullPath);

    File project = new File(getProjectFullPath());

    setProjectPath(project.getParent());

    setProjectName(project.getName());

}//end of Settings::setNamesAndPaths
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::saveFile
//
// Saves values to a file.
//

public void saveFile()
{

    IniFile ini;

    try {

        ini = new IniFile("Settings/Settings.ini", "UTF-8");

        ini.writeString(
                      "General", "Current Project Path", getProjectFullPath());

        ini.save(); //write the file to disk

    }
    catch(IOException e){
        Settings.errorMsg("Error saving Globals file");
    }

}//end of Settings::saveFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::errorMsg
//
// Displays an error dialog with message pMessage.
//

static void errorMsg(String pMessage)
{

    JOptionPane.showMessageDialog(null, pMessage,
                                            "Error", JOptionPane.ERROR_MESSAGE);

}//end of Settings::errorMsg
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::setSizes
//
// Sets the min, max, and preferred sizes of pComponent to pWidth and pHeight.
//

static void setSizes(Component pComponent, int pWidth, int pHeight)
{

    pComponent.setMinimumSize(new Dimension(pWidth, pHeight));
    pComponent.setPreferredSize(new Dimension(pWidth, pHeight));
    pComponent.setMaximumSize(new Dimension(pWidth, pHeight));

}//end of Settings::setSizes
//-----------------------------------------------------------------------------

}//end of class Settings
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------