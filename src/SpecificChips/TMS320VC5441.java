/******************************************************************************
* Title: TI DSP Dev Tool - Main Source File
* Author: Mike Schoonover
* Date: 2/3/13
*
* Purpose:
*
* This class provides base functionality for a TMS320VC5441 to be simulated.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package SpecificChips;

import DSPSimulation.Chip;

//-----------------------------------------------------------------------------
// class TMS320VC5441
//

public class TMS320VC5441 extends Chip
{

//-----------------------------------------------------------------------------
// TMS320VC5441::TMS320VC5441 (constructor)
//
// Creates a TMS320VC5441 simulation object.
//

public TMS320VC5441()
{

    super("TMS320VC5441", "'5441");

}//end of TMS320VC5441::TMS320VC5441 (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TMS320VC5441::init
//
// Initializes new objects. Should be called immediately after instantiation.
//

@Override
public void init()
{

    //allow base class to set up first
    super.init();

}//end of TMS320VC5441::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TMS320VC5441::execute
//
// This method performs the functions expected of the instruction and operands
// in pFullInstruction.
//

@Override
public void execute(String pFullInstruction)
{

}//end of TMS320VC5441::execute
//-----------------------------------------------------------------------------

}//end of class TMS320VC5441
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
