/******************************************************************************
* Title: TI DSP Dev Tool - Globals.java File
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

package tidspdevtool;


import java.io.*;
import java.util.*;
import javax.swing.JOptionPane;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Globals
//
//

public class Globals extends Object{


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
// Globals::Globals (constructor)
//

Globals()
{

loadFile();

}//end of Globals::Globals (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Globals::loadFile
//
// Loads values from file.
//

private void loadFile()
{

IniFile ini = null;


try {

    ini = new IniFile("Settings/Globals.ini", "UTF-8");

    setProjectFullPath(ini.readString("General", "Current Project Path", ""));

    File project = new File(getProjectFullPath());

    projectPath = project.getParent();

    projectName = project.getName();

    }
catch(IOException e){
    Globals.errorMsg("Error loading Globals file");
    }

}//end of Globals::loadFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Globals::saveFile
//
// Saves values to a file.
//

public void saveFile()
{

IniFile ini = null;

try {

    ini = new IniFile("Settings/Globals.ini");

    ini.writeString("General", "Current Project Path", getProjectFullPath());

    }
catch(IOException e){
    Globals.errorMsg("Error saving Globals file");
    }


}//end of Globals::saveFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Globals::errorMsg
//
// Displays an error dialog with message pMessage.
//

static void errorMsg(String pMessage)
{

JOptionPane.showMessageDialog(null, pMessage,
                                            "Error", JOptionPane.ERROR_MESSAGE);

}//end of Globals::errorMsg
//-----------------------------------------------------------------------------

}//end of class Globals
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------