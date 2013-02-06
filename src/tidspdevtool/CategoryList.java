/******************************************************************************
* Title: TI DSP Dev Tool - FileList.java File
* Author: Mike Schoonover
* Date: 02/5/13
*
* Purpose:
*
* This is a base class for handling lists of objects which are managed by
* trees.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package tidspdevtool;

//-----------------------------------------------------------------------------

import java.io.File;
import java.util.ArrayList;

//-----------------------------------------------------------------------------
// class CategoryList
//
//

public class CategoryList extends Object{

    public ArrayList<File> list = new ArrayList<File>();


//-----------------------------------------------------------------------------
// CategoryList::clearList
//
// Clears the list of all entries.
//

public void clearList()
{

    list.clear();

}//end of CategoryList::clearList
//-----------------------------------------------------------------------------

}//end of class CategoryList
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------