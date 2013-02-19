/******************************************************************************
* Title: Universal IDE - DocumentSizeFilter.java
* Author: Mike Schoonover
* Date: 06/4/11
*
* Purpose:
*
* Used by EditorFrame class.
*
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package basicide;

import java.awt.Toolkit;
import javax.swing.text.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class DocumentSizeFilter
//
// Determines how to handle text insertion.
//


public class DocumentSizeFilter extends DocumentFilter {
    int maxCharacters;
    boolean DEBUG = false;

//-----------------------------------------------------------------------------
// DocumentSizeFilter::DocumentSizeFilter (constuctor)
//


public DocumentSizeFilter(int maxChars)
{
    maxCharacters = maxChars;

}//end of DocumentSizeFilter::DocumentSizeFilter (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DocumentSizeFilter::insertString
//

public void insertString(FilterBypass fb, int offs,
                         String str, AttributeSet a)
                                            throws BadLocationException
{

    if (DEBUG) {
        System.out.println("in DocumentSizeFilter's insertString method");
    }

    //This rejects the entire insertion if it would make
    //the contents too long. Another option would be
    //to truncate the inserted string so the contents
    //would be exactly maxCharacters in length.
    if ((fb.getDocument().getLength() + str.length()) <= maxCharacters) {
            super.insertString(fb, offs, str, a);
    }
    else {
            Toolkit.getDefaultToolkit().beep();
    }

}//end of DocumentSizeFilter::insertString
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DocumentSizeFilter::replace
//

@Override
public void replace(FilterBypass fb, int offs,
                        int length, String str, AttributeSet a)
                                                throws BadLocationException {

    if (DEBUG) {
        System.out.println("in DocumentSizeFilter's replace method");
    }
    //This rejects the entire replacement if it would make
    //the contents too long. Another option would be
    //to truncate the replacement string so the contents
    //would be exactly maxCharacters in length.
    if ((fb.getDocument().getLength() + str.length()
         - length) <= maxCharacters) {
            super.replace(fb, offs, length, str, a);
    }
    else {
            Toolkit.getDefaultToolkit().beep();
    }

}//end of DocumentSizeFilter::replace
//-----------------------------------------------------------------------------

}//end of class DocumentSizeFilter
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------s
