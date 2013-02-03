/******************************************************************************
* Title: TI DSP Dev Tool - Project.java File
* Author: Mike Schoonover
* Date: 06/25/11
*
* Purpose:
*
* This class handles a "project" -- the settings and values used to define
* the project's files and configuration.
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

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Project
//
//

public class Project extends Object{


    Settings settings;

    int numFiles = 0;

    FileList sourceCodeFileList;
    FileList linkerFileList;
    FileList docFileList;

//-----------------------------------------------------------------------------
// Project::Project (constructor)
//

Project(Settings pSettings)
{

    settings = pSettings;

    sourceCodeFileList = new FileList();
    linkerFileList = new FileList();
    docFileList = new FileList();

    loadFile();

}//end of Project::Project (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Project::loadFile
//
// Loads values from file.
//

private void loadFile()
{

    IniFile ini;

    try {

        //open the project file
        ini = new IniFile(settings.getProjectFullPath(), "UTF-8");

        sourceCodeFileList.loadFile(ini, "Source Code Files");
        linkerFileList.loadFile(ini, "Linker Command Files");
        docFileList.loadFile(ini, "Documentation and Note Files");

    }
    catch(IOException e){
        Settings.errorMsg("Error loading Project file");
    }

}//end of Project::loadFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Project::saveFile
//
// Saves values to a file.
//

public void saveFile()
{

    IniFile ini;

    try {

        //open the project file
        ini = new IniFile(settings.getProjectFullPath(), "UTF-8");

        sourceCodeFileList.saveFile(ini, "Source Code Files");
        linkerFileList.saveFile(ini, "Linker Command Files");
        docFileList.saveFile(ini, "Documentation and Note Files");

        //force save of data to file
        ini.save();

    }
    catch(IOException e){
        Settings.errorMsg("Error saving Project file");
    }

}//end of Project::saveFile
//-----------------------------------------------------------------------------

}//end of class Project
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------