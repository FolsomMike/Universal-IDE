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
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

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

    CustomFileFilter fileFilter = new CustomFileFilter();

//-----------------------------------------------------------------------------
// Project::Project (constructor)
//

Project(Settings pSettings)
{

    settings = pSettings;

}//end of Project::Project (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Project::init
//
// Initializes new objects. Should be called immediately after instantiation.
//

public void init()
{

    sourceCodeFileList = new FileList();
    linkerFileList = new FileList();
    docFileList = new FileList();

    loadFile();

}//end of Project::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Project::chooseProject
//
// Allows the user to browse to a new project and choose it as the current
// project. Loads all files, data and settings for the chosen project.
//

public void chooseProject()
{

    final JFileChooser fc = new JFileChooser(settings.getProjectPath());

    fileFilter.setExtension(".prj");
    fileFilter.setDescription("Project Files");

    fc.setFileFilter(fileFilter);

    int returnVal = fc.showOpenDialog(settings.mainFrame);

    //bail out if user did not select a file
    if (returnVal != JFileChooser.APPROVE_OPTION) {return;}

    File newFile = fc.getSelectedFile();

    settings.setNamesAndPaths(newFile.getPath());

    loadFile();

    settings.projectFrame.setNewRootNode();

}//end of Project::chooseProject
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

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class CustomFileFilter
//
// This class is used to filter files displayed in a file chooser.
//
// Only folders and files with the specified extension are allowed. The folders
// are allowed so the user can navigate to a different location.
//

class CustomFileFilter extends FileFilter
{

    String extension = "";
    String description = "";

    public void setExtension(String pExt){extension = pExt.toLowerCase();}
    public void setDescription(String pDes){description = pDes;}

//-----------------------------------------------------------------------------
// CustomFileFilter::accept
//
// Returns true if the file is a folder or the name parameter meets the filter
// requirements, false otherwise.
//
// Note that the extension will have been set to lower case by setExtension.
//

@Override
public boolean accept(File pFile)
{

    //allow display of all folders
    if (pFile.isDirectory()) {
        return true;
    }

    //the file satisfies the filter if it ends with the extension value
    if (pFile.getName().toLowerCase().endsWith(extension)) {
        return(true);
    } else {
        return(false);
    }

}//end of CustomFileFilter::accept
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CustomFileFilter::getDescription
//
// Returns a description of the filter.
//

@Override
public String getDescription()
{

    return(description);

}//end of CustomFileFilter::getDescription
//-----------------------------------------------------------------------------

}//end of class getDescription
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

}//end of class Project
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------