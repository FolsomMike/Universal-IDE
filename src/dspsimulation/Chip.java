/******************************************************************************
* Title: Universal IDE - Chip.java
* Author: Mike Schoonover
* Date: 2/3/13
*
* Purpose:
*
* This class provides base functionality for a chip to be simulated. Each type
* of chip should be a sub-class of this base class.
*
* This base class provides a RegisterSet, an InstructionSet, and MemorySet
* objects for handling a list of each type.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package dspsimulation;

//-----------------------------------------------------------------------------
//

public class Chip
{

    //the name and abbreviated name for the chip
    String name, shortName;

    RegisterSet registerSet;
    InstructionSet instructionSet;

//-----------------------------------------------------------------------------
// Chip::Chip (constructor)
//
// Creates a basic chip with name pName and abbreviated name pShortName.
//

public Chip(String pName, String pShortName)
{

    name = pName; shortName = pShortName;

}//end of Chip::Chip (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Chip::init
//
// Initializes new objects. Should be called immediately after instantiation.
//
// The base class should override this method to provide extended setup -- it
// should call the init method here in the base class before peforming its own
// setup.
//

public void init()
{

    registerSet = new RegisterSet();
    instructionSet = new InstructionSet();

}//end of Chip::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Chip::execute
//
// This method performs the functions expected of the instruction and operands
// in pFullInstruction.
//
// Each sub-class should override this method to provide unique functionality.
//

public void execute(String pFullInstruction)
{

}//end of Chip::execute
//-----------------------------------------------------------------------------

}//end of class Chip
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
