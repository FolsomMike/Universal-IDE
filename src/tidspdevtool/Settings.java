/******************************************************************************
* Title: TI DSP Dev Tool - Settings.java File
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
import java.awt.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Settings
//
//

public class Settings extends Object{


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

    loadFile();

}//end of Settings::Settings (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::loadFile
//
// Loads values from file.
//

private void loadFile()
{

    IniFile ini = null;


    try {

        ini = new IniFile("Settings/Settings.ini", "UTF-8");

        setProjectFullPath(
                        ini.readString("General", "Current Project Path", ""));

        File project = new File(getProjectFullPath());

        projectPath = project.getParent();

        projectName = project.getName();

    }
    catch(IOException e){
        Settings.errorMsg("Error loading Settings file");
    }

}//end of Settings::loadFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::saveFile
//
// Saves values to a file.
//

public void saveFile()
{

    IniFile ini = null;

    try {

        ini = new IniFile("Settings/Settings.ini", "UTF-8");

        ini.writeString(
                      "General", "Current Project Path", getProjectFullPath());

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