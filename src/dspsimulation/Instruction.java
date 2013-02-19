/******************************************************************************
* Title: Universal IDE - Instruction.java
* Author: Mike Schoonover
* Date: 2/3/13
*
* Purpose:
*
* This class provides base functionality for a DSP instruction. Each type of
* instruction should be a sub-class of this base class.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package dspsimulation;

//-----------------------------------------------------------------------------
// class Instruction
//

class Instruction
{

    //the actual assembler mnemonic for the instruction
    String mnemonic;

//-----------------------------------------------------------------------------
// Instruction::Instruction (constructor)
//
// Creates a basic instruction which has the assembler mnemonic pMnemonic.

Instruction(String pMnemonic)
{

    mnemonic = pMnemonic;

}//end of Instruction::Instruction (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Instruction::init
//
// Initializes new objects. Should be called immediately after instantiation.
//

public void init()
{


}//end of Instruction::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Instruction::parse
//
// Determines if pMnemonic matches the mnemonic for the instruction.
//
// Returns true if they match, false otherwise.
//

public boolean parse(String pMnemonic)
{

    return(mnemonic.equalsIgnoreCase(pMnemonic));

}//end of Instruction::parse
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Instruction::execute
//
// This method performs the instruction's function(s).
//
// Each instruction sub-class should override this method to provide unique
// functionality.
//

public void execute()
{

}//end of Instruction::execute
//-----------------------------------------------------------------------------

}//end of class Instruction
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
