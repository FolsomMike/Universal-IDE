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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class TextSearcher
//

public class TextSearcher extends JPanel implements ActionListener{

    JTextField searchTextField;
    JTextField replaceTextField;
    JCheckBox matchCaseCheckBox;
    JTextPane textPane;

    int searchStart = 0;

//-----------------------------------------------------------------------------
// TextSearcher::TextSearcher (constructor)
//
// The JTextPane to be searched is passed via pTextPane.
//

public TextSearcher(JTextPane pTextPane)
{

    textPane = pTextPane;

}//end of TextSearcher::TextSearcher (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TextSearcher::init
//

public void init()
{

    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    Settings.setSizes(this, 375, 20);

    add(new JLabel("Find"));
    add(Box.createRigidArea(new Dimension(5,0))); //spacer
    searchTextField = new JTextField(20);
    searchTextField.setActionCommand("<Enter> for Find Phrase Box");
    searchTextField.addActionListener(this);
    add(searchTextField);

    add(Box.createRigidArea(new Dimension(10,0))); //spacer

    add(new JLabel("Replace with"));
    add(Box.createRigidArea(new Dimension(5,0))); //spacer
    replaceTextField = new JTextField(20);
    replaceTextField.setActionCommand("<Enter> for Replacement Phrase Box");
    replaceTextField.addActionListener(this);
    add(replaceTextField);

    add(Box.createRigidArea(new Dimension(5,0))); //spacer

    matchCaseCheckBox = new JCheckBox("Match Case");
    add(matchCaseCheckBox);

    add(Box.createRigidArea(new Dimension(15,0))); //spacer

}//end of TextSearcher::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TextSearcher::prepareTextFindField
//
// Clears the text find field and gives it focus so the user can type in the
// phrase to be searched for. When the user presses the <enter> key while
// the box has focus, TextSearcher.actionPerformed will be called.
//

public void prepareTextFindField()
{

    searchTextField.setText("");
    searchTextField.requestFocusInWindow();

}//end of TextSearcher::prepareTextFindField
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TextSearcher::findNext
//
// Searches textPane for the phrase in the searchTextField starting at the
// position in the document just after the location where the last search
// phrase was found.
//
// The current text in searchTextField will be searched for, so if the user
// changes it the search will be for the new text but will still start from the
// position where the last phrase was found. By leaving the text unchanged, the
// user can search repeatedly for the same phrase.
//

public void findNext()
{

    //search for the phrase; pass false to signify this is not an initial search
    findText(false);

}//end of TextSearcher::findNext
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TextSearcher::actionPerformed
//
// Responds to user actions.
//

public void actionPerformed(ActionEvent e)
{

    //handle "Enter" key in the find text window -- perform initial search
    if ("<Enter> for Find Phrase Box".equals(e.getActionCommand())) {
        findText(true);
        return;
    }


}//end of TextSearcher::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TextSearcher::findText
//
// Searches the text of textPane for the phrase in searchTextField.
//
// If this is the first search for the phrase, pInitialSearch should be passed
// as true. Initial searches always start from the top of the document.
//
// Note that each time a search is performed (even a repeat of the previous
// search), a new string of all the text is retrieved from textPane. This
// ensures that any changes made to the textPane will be part of the next
// search, but will this work for very large documents or will it be too slow?
//
// Multiple examples on the web use String.indexOf to search for the text
// phrase. As that method does not have an "ignore case" option, those examples
// convert the entire text from the text pane to upper or lower and also do the
// same to the search phrase if the search is to ignore case. Converting to
// upper or lower case returns an entire new string. The old string will get
// released from memory, but this seems like a memory hogging method. Instead,
// this class uses the String.regionMatches method which can ignore case. It
// does require that the program step through the target text character by
// character; this may take too much time on very large documents.
//

private void findText(boolean pInitialSearch)
{

    String document = textPane.getText();
    String phrase = searchTextField.getText();
    boolean ignoreCase = !matchCaseCheckBox.isSelected();

    int docLength = document.length();
    int phraseLength = phrase.length();

    //prepare for first time search of the phrase
    if (pInitialSearch){

       searchStart = 0; //initial searches always start at top of document

    }

    //protect against starting the search past the end of the document, such as
    //might happen if the user shortens the document
    if (searchStart > docLength) searchStart = 0;

    boolean found = false;
    int i;

    for (i = searchStart; i <= (docLength - phraseLength); i++) {
        if (document.regionMatches(ignoreCase, i, phrase, 0, phraseLength)) {
            found = true;
            replaceTextField.setText("" + i);
            break;
       }
    }

    //if phrase not found, clean up and do nothing more
    if (!found){
        searchStart = 0; //start next search from top of document
        return;
    }

    //set starting point for subsequent searches
    searchStart = i + phraseLength;

    boolean b = textPane.isEditable();

    //highlight the found phrase in the document
    //textPane.setSelectionColor(MColor.BLUE);
    textPane.requestFocusInWindow();
    textPane.setCaretPosition(i);
    textPane.moveCaretPosition(i + phraseLength);

}//end of TextSearcher::findText
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



