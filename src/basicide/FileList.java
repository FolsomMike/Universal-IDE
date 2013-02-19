/******************************************************************************
* Title: Universal IDE - FileList.java
* Author: Mike Schoonover
* Date: 06/25/11
*
* Purpose:
*
* This class handles a list of files with functions to load and save to an
* IniFile.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package basicide;


import java.io.*;
import java.util.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class FileList
//
//

public class FileList extends CategoryList{


//-----------------------------------------------------------------------------
// FileList::FileList (constructor)
//

FileList()
{

}//end of FileList::FileList (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FileList::loadFile
//
// Loads file paths from file pIniFile which are listed under pCategory.
//

public void loadFile(IniFile pIni, String pCategory) throws IOException
{

    //clear out any entries already in the list in case this is a reload
    clearList();

    int numFiles;

    numFiles = pIni.readInt(pCategory, "Number of Files", 0);

    String key, file;

    for (int i = 0; i < numFiles; i++){

        key = "File " + i;

        file = pIni.readString(pCategory, key, "");

        list.add(new File(file));

    }

}//end of FileList::loadFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FileList::saveFile
//
// Saves values to a file.
//

public void saveFile(IniFile pIni, String pCategory) throws IOException
{

    int numFiles = 0;

    String key;
    Iterator i;
    File file;

    for (i = list.iterator(); i.hasNext();){

        file = (File)i.next();

        key = "File " + numFiles++;

        pIni.writeString(pCategory, key, file.getPath());

    }

    //save the number of files in the list
    pIni.writeInt(pCategory, "Number of Files", numFiles);

}//end of FileList::saveFile
//-----------------------------------------------------------------------------

}//end of class FileList
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------