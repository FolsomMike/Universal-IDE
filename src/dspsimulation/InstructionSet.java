/******************************************************************************
* Title: Universal IDE - InstructionSet.java
* Author: Mike Schoonover
* Date: 2/3/13
*
* Purpose:
*
* This class manages a collection of instructions for a simulated DSP.
* The instructions are added to a list in this class which will then scan them
* to find which matches the mnemonic being parsed.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package dspsimulation;

import java.util.ArrayList;

//-----------------------------------------------------------------------------
// class InstructionSet
//

class InstructionSet
{

    ArrayList list;

//-----------------------------------------------------------------------------
// InstructionSet::InstructionSet (constructor)
//

InstructionSet()
{

}//end of InstructionSet::InstructionSet (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// InstructionSet::init
//
// Initializes new objects. Should be called immediately after instantiation.
//

public void init()
{

    //create the list to hold the instructions
    list = new ArrayList();

}//end of InstructionSet::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// InstructionSet::addInstruction
//
// Adds instruction pNew to the collection.
//

public void addInstruction(Instruction pNew)
{

    list.add(pNew);

}//end of InstructionSet::addInstruction
//-----------------------------------------------------------------------------


}//end of class InstructionSet
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
