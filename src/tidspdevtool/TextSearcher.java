/******************************************************************************
* Title: TI DSP Dev Tool - TextSearcher.java File
* Author: Mike Schoonover
* Date: 2/10/13
*
* Purpose:
*
* This class handles searching and search & replace for a JTextPane. It
* displays a panel which can display a text target box, a replace text box,
* forward, back, next controls, etc. It has methods which perform the actual
* search/search & replace.
*
* See notes at the top of EditorFrame.java for a detailed overview of the
* relationship between EditorFrame, EditorRig, EditorTabPane, the JTextPane,
* and the EditorKit used by the JTextPane.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package tidspdevtool;

import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class TextSearcher
//

public class TextSearcher extends JPanel{

    JTextField searchTextField;
    JTextField replaceTextField;

//-----------------------------------------------------------------------------
// TextSearcher::TextSearcher (constructor)
//

public TextSearcher()
{

}//end of TextSearcher::TextSearcher (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TextSearcher::init
//

public void init()
{

    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    Settings.setSizes(this, 300, 20);

    add(new JLabel("Find"));
    add(Box.createRigidArea(new Dimension(5,0))); //spacer
    searchTextField = new JTextField(20);
    add(searchTextField);
    add(Box.createRigidArea(new Dimension(10,0))); //spacer
    add(new JLabel("Replace with"));
    add(Box.createRigidArea(new Dimension(5,0))); //spacer
    replaceTextField = new JTextField(20);
    add(replaceTextField);
    add(Box.createRigidArea(new Dimension(15,0))); //spacer

}//end of TextSearcher::init
//-----------------------------------------------------------------------------

}//end of class TextSearcher
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------


/*

// used by the find and replace methods
// the last character position of any already found text
private int findPosn = 0;
// the last text searched for
private String findText = null;
// case sensitive find/replace
private boolean findCase = false;
// user must confirm text replacement
private boolean replaceConfirm = true;

The txt field in the following code is the JTextPane.

// finds next occurrence of text in a string
// @param find the text to locate in the string
public void doFindText(String find) {
int nextPosn = 0;
if (!find.equals(findText) ) // *** new find word
findPosn = 0; // *** start from top
nextPosn = nextIndex( txt.getText(), find, findPosn, findCase );
if ( nextPosn >= 0 ) {
txt.setSelectionStart( nextPosn ); // position cursor at word
start
txt.setSelectionEnd( nextPosn + find.length() );
findPosn = nextPosn + find.length() + 1; // reset for next
search
findText = find; // save word & case
} else {
findPosn = nextPosn; // set to -1 if not found
JOptionPane.showMessageDialog(this, find + " not Found!" );
}
}

// finds and replaces <B>ALL</B> occurrences of text in a string
// @param find the text to locate in the string
// @param replace the text to replace the find text with - if the find
// text exists
public void doReplaceWords(String find, String replace) {
int nextPosn = 0;
StringBuffer str = new StringBuffer();
findPosn = 0; // *** begin at start of text
while (nextPosn >= 0) {
nextPosn = nextIndex( txt.getText(), find, findPosn, findCase );

if ( nextPosn >= 0 ) { // if text is found
int rtn = JOptionPane.YES_OPTION; // default YES for confirm
dialog
txt.grabFocus();
txt.setSelectionStart( nextPosn ); // posn cursor at word start
txt.setSelectionEnd( nextPosn + find.length() ); //select found
text
if ( replaceConfirm ) { // user replace confirmation
rtn = JOptionPane.showConfirmDialog(this, "Found: " + find +
"\nReplace with: " + replace, "Text Find & Replace",
JOptionPane.YES_NO_CANCEL_OPTION);
}
// if don't want confirm or selected yes
if ( !replaceConfirm || rtn == JOptionPane.YES_OPTION ) {
txt.replaceRange( replace, nextPosn, nextPosn + find.length() );
} else if ( rtn == javax.swing.JOptionPane.CANCEL_OPTION )
return; // cancelled replace - exit method
findPosn = nextPosn + find.length(); // set for next search
}
}
}

// returns next posn of word in text - forward search
// @return next indexed position of start of found text or -1
// @param input the string to search
// @param find the string to find
// @param start the character position to start the search
// @param caseSensitive true for case sensitive. false to ignore case
//
public int nextIndex(String input, String find, int start, boolean
caseSensitive ) {
int textPosn = -1;
if ( input != null && find != null && start < input.length() ) {
if ( caseSensitive == true ) { // indexOf() returns -1 if not found
textPosn = input.indexOf( find, start );
} else {
textPosn = input.toLowerCase().indexOf( find.toLowerCase(),
start );
}
}
return textPosn;
}


*/