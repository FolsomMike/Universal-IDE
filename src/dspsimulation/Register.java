/******************************************************************************
* Title: Universal IDE - Register.java
* Author: Mike Schoonover
* Date: 2/3/13
*
* Purpose:
*
* This class provides base functionality for a DSP data register.  Each type of
* register should be a sub-class of this base class.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package dspsimulation;

//-----------------------------------------------------------------------------
// class Register
//

class Register
{

    //the register's text name and a suitable abbreviation

    String name, shortName;

    //this value holds the register data -- it is generally has more bits than
    //the actual register, so a mask is used to strip off any extra upper bits
    //which might be set by calculation overflow

    long value;

    //width of the data bus used to read and write to the register
    //all values will be truncated to this width when read from or written to
    //the register

    int busWidth;

    //number of bits in the register -- including any guard bits which the
    //register has -- the guard bits are upper bits which protect against
    //overflow but are not read or written directly unless the register or
    //written value are shifted

    int numBits;

    //mask used to strip off upper bits of value used to contain a register
    //the value usually has more bits that the register actually holds, so
    //the value is masked to remove any overflow bits

    long bitMask;

    //mask used to strip off upper bits of value to fit the specified bus
    //width -- the value is truncated when it is read or written to remove
    //any extra overflow bits

    long busMask;

    static int DECIMAL = 0;
    static int HEX = 1;
    static int BINARY = 2;

//-----------------------------------------------------------------------------
// Register::Register (constructor)
//
// Sets up a new register with name pName, name abbreviation pShortName, and
// which contains pNumBits number of bits and is accessed via a bus with bit
// width pBusWidth.
//

Register(String pName, String pShortName, int pNumBits, int pBusWidth)
{

    name = pName; shortName = pShortName;
    numBits = pNumBits; busWidth = pBusWidth;

}//end of Register::Register (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Register::init
//
// Initializes new objects. Should be called immediately after instantiation.
//

public void init()
{

    //create a bit mask with a 1 in every valid bit position for use in masking
    //off overflow bits

    bitMask = 0;
    for (int i=0; i<numBits; i++){

        bitMask = bitMask << 1;
        bitMask += 1;

    }

    //create a bit mask with a 1 in every valid bit position of the bus -- used
    //to truncate register value to fit the bus by stripping off any upper
    //overflow bits

    busMask = 0;
    for (int i=0; i<numBits; i++){

        busMask = busMask << 1;
        busMask += 1;

    }

}//end of Register::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Register::write
//
// Writes a data value to the register. Any bits above the width of the data
// bus are zeroed as would be true in the actual DSP.
//
// The value is shifted left for positive pShift or right for negative pShift
// before being stored.
//

public void write(int pShift)
{

    //mask value first and then shift it for writing

    long lValue = value & busMask;

    lValue = shift(lValue, pShift);

    value = lValue;

}//end of Register::write
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Register::read
//
// Reads the data value from the register. Any bits above the width of the data
// bus are zeroed as would be true in the actual DSP.
//
// The value is shifted left for positive pShift or right for negative pShift
// before being returned.
//
// Returns the truncated, shifted value.
//

public long read(int pShift)
{

    //shift value first and then mask it for reading

    long lValue = shift(value, pShift);

    lValue = lValue & busMask;

    return(lValue);

}//end of Register::read
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Register::shift
//
// The value is shifted left for positive pShift or right for negative pShift
// before being returned.
//
// Returns the shifted value.
//

public long shift(long pValue, int pShift)
{

    //shift left for positive pShift
    if (pShift > 0){
        pValue = pValue << pShift;
    }

    //shift right for negative pShift
    if (pShift < 0){
        pValue = pValue >> Math.abs(pShift);
    }

    return(pValue);

}//end of Register::shift
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Register::toString
//
// Returns a decimal, hexadecimal, or binary string of the entire value
// (not masked to the bus width) ready for display. If the return string is in
// hexadecimal and has an odd number of characters, a zero will be prepended
// for consistent appearance.
//
// Number base is selected by pBase.
// Decimal numbers will be appended with 'd'.
// Hex numbers will be appended with 'h'.
// Binary numbers will be appended with 'b'.
//

public String toString(int pBase)
{

    String s = null;

    if(pBase == Register.DECIMAL){
        s = Long.toString(value, 10) + "d";
    }
    else
    if(pBase == Register.HEX){
        s = Long.toString(value, 16);
        if (s.length() % 2 != 0) {s = " " + s;}
        s = s + "h";
    }
    else
    if(pBase == Register.BINARY){
        s = Long.toString(value, 2) + "b";
    }

    return(s);

}//end of Register::toString
//-----------------------------------------------------------------------------

}//end of class Register
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
