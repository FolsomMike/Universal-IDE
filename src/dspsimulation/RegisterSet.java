/******************************************************************************
* Title: Universal IDE - RegisterSet.java
* Author: Mike Schoonover
* Date: 2/3/13
*
* Purpose:
*
* This class manages a collection of registers for a simulated DSP.
* The instructions are added to a list in this class.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package dspsimulation;

//-----------------------------------------------------------------------------

import java.util.ArrayList;

// class RegisterSet
//

class RegisterSet
{

    ArrayList list;

//-----------------------------------------------------------------------------
// RegisterSet::RegisterSet (constructor)
//

RegisterSet()
{

}//end of RegisterSet::RegisterSet (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RegisterSet::init
//
// Initializes new objects. Should be called immediately after instantiation.
//

public void init()
{

    //create the list to hold the registers
    list = new ArrayList();

}//end of RegisterSet::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RegisterSet::addRegister
//
// Adds register pNew to the collection.
//

public void addRegister(Register pNew)
{

    list.add(pNew);

}//end of RegisterSet::addRegister
//-----------------------------------------------------------------------------


}//end of class RegisterSet
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
