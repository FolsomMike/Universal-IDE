/******************************************************************************
* Title: TI DSP Dev Tool - EditorFrame.java File
* Author: Mike Schoonover
* Date: 06/4/11
*
* Purpose:
*
* A JInternalFrame which allows the user to edit a source or text file.
*
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package tidspdevtool;

import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.undo.*;
import java.io.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class EditorFrame
//
//

class EditorFrame extends JInternalFrame
{

JTextPane textPane;
AbstractDocument doc;
static final int MAX_CHARACTERS = 1000000;
JTextArea changeLog;
String newline = "\n";
HashMap<Object, Action> actions;

//undo helpers
protected UndoAction undoAction;
protected RedoAction redoAction;
protected UndoManager undo = new UndoManager();


//-----------------------------------------------------------------------------
// EditorFrame::EditorFrame (constructor)
//
//

public EditorFrame(String pTitle, boolean pResizable, boolean pCloseable,
                                   boolean pMaximizable, boolean pIconifiable) {

super(pTitle, pResizable, pCloseable, pMaximizable, pIconifiable);


//Create the text pane and configure it.
textPane = new JTextPane();
textPane.setCaretPosition(0);
textPane.setMargin(new Insets(5,5,5,5));
StyledDocument styledDoc = textPane.getStyledDocument();

if (styledDoc instanceof AbstractDocument) {
    doc = (AbstractDocument)styledDoc;
    doc.setDocumentFilter(new DocumentSizeFilter(MAX_CHARACTERS));
    }
else {
    System.err.println("Text pane's document isn't an AbstractDocument!");
    System.exit(-1);
    }

JScrollPane scrollPane = new JScrollPane(textPane);
scrollPane.setPreferredSize(new Dimension(300, 350));

//Create the text area for the status log and configure it.
changeLog = new JTextArea(5, 2);
changeLog.setEditable(false);
JScrollPane scrollPaneForLog = new JScrollPane(changeLog);
scrollPaneForLog.setPreferredSize(new Dimension(300, 50));

//Create a split pane for the change log and the text area.
JSplitPane splitPane = new JSplitPane(
                                       JSplitPane.VERTICAL_SPLIT,
                                       scrollPane, scrollPaneForLog);
splitPane.setOneTouchExpandable(true);

//Create the status area.
JPanel statusPane = new JPanel(new GridLayout(1, 1));
CaretListenerLabel caretListenerLabel = new CaretListenerLabel("Caret Status");
statusPane.add(caretListenerLabel);

//Add the components.
getContentPane().add(splitPane, BorderLayout.CENTER);
getContentPane().add(statusPane, BorderLayout.PAGE_END);

//Set up the menu bar.

// create a list of actions available for the text editor component
actions = createActionTable(textPane);

// create the menus using custom actions and those available for the text
// editor compontent

JMenu editMenu = createEditMenu();
JMenu styleMenu = createStyleMenu();
JMenuBar mb = new JMenuBar();
mb.add(editMenu);
mb.add(styleMenu);
setJMenuBar(mb);

//Add some key bindings.
addBindings();

//Put the initial text into the text pane.
//initSampleDocument();
//textPane.setCaretPosition(0);

//Start watching for undoable edits and caret changes.
doc.addUndoableEditListener(new MyUndoableEditListener());
textPane.addCaretListener(caretListenerLabel);
doc.addDocumentListener(new MyDocumentListener());

}//end of EditorFrame::EditorFrame (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorFrame::addBindings
//
// Connect keyboard shortcuts to actions.
//
// These shortcuts are compatible with the "Emacs" type of text editors.
//

private void addBindings()
{

//Add a couple of Emacs key bindings for navigation.

InputMap inputMap = textPane.getInputMap();

//Ctrl-b to go backward one character
KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_B, Event.CTRL_MASK);
inputMap.put(key, DefaultEditorKit.backwardAction);

//Ctrl-f to go forward one character
key = KeyStroke.getKeyStroke(KeyEvent.VK_F, Event.CTRL_MASK);
inputMap.put(key, DefaultEditorKit.forwardAction);

//Ctrl-p to go up one line
key = KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK);
inputMap.put(key, DefaultEditorKit.upAction);

//Ctrl-n to go down one line
key = KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK);
inputMap.put(key, DefaultEditorKit.downAction);

}//end of EditorFrame::addBindings
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorFrame::createEditMenu
//
// Creates the edit menu using custom actions and actions available from the
// editor kit for the text editor component.
//

private JMenu createEditMenu() {

JMenu menu = new JMenu("Edit");

//Undo and redo are actions of our own creation.
undoAction = new UndoAction();
menu.add(undoAction);

redoAction = new RedoAction();
menu.add(redoAction);

menu.addSeparator();

//These actions come from the default editor kit.
//Get the ones we want and stick them in the menu.

menu.add(getActionByName(DefaultEditorKit.cutAction));
menu.add(getActionByName(DefaultEditorKit.copyAction));
menu.add(getActionByName(DefaultEditorKit.pasteAction));

menu.addSeparator();

menu.add(getActionByName(DefaultEditorKit.selectAllAction));

return menu;

}//end of EditorFrame::createStyleMenu
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorFrame::createStyleMenu
//
// Creates the style menu using actions available from the editor kit for
// the text editor component.
//

private JMenu createStyleMenu() {

JMenu menu = new JMenu("Style");

Action action;

action = new StyledEditorKit.BoldAction();
action.putValue(Action.NAME, "Bold");
menu.add(action);

action = new StyledEditorKit.ItalicAction();
action.putValue(Action.NAME, "Italic");
menu.add(action);

action = new StyledEditorKit.UnderlineAction();
action.putValue(Action.NAME, "Underline");
menu.add(action);

menu.addSeparator();

menu.add(new StyledEditorKit.FontSizeAction("12", 12));
menu.add(new StyledEditorKit.FontSizeAction("14", 14));
menu.add(new StyledEditorKit.FontSizeAction("18", 18));

menu.addSeparator();

menu.add(new StyledEditorKit.FontFamilyAction("Serif", "Serif"));
menu.add(new StyledEditorKit.FontFamilyAction("SansSerif", "SansSerif"));

menu.addSeparator();

menu.add(new StyledEditorKit.ForegroundAction("Red", Color.red));
menu.add(new StyledEditorKit.ForegroundAction("Green", Color.green));
menu.add(new StyledEditorKit.ForegroundAction("Blue", Color.blue));
menu.add(new StyledEditorKit.ForegroundAction("Black", Color.black));

return menu;

}//end of EditorFrame::createStyleMenu
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorFrame::initSampleDocument
//
// Inserts a sample document into the text editor using various font styles.
// Used for educational/demo purposes only.
//

public final void initSampleDocument() {

String initString[] =
    {
     "Use the mouse to place the caret.",
     "Use the edit menu to cut, copy, paste, and select text.",
     "Also to undo and redo changes.",
     "Use the style menu to change the style of the text.",
     "Use the arrow keys on the keyboard or these emacs key bindings to move the caret:",
     "Ctrl-f, Ctrl-b, Ctrl-n, Ctrl-p."
    };

SimpleAttributeSet[] attrs = initSampleAttributes(initString.length);

try {

    for (int i = 0; i < initString.length; i ++) {
        doc.insertString(doc.getLength(), initString[i] + newline, attrs[i]);
        }

    }
catch (BadLocationException ble) {
    System.err.println("Couldn't insert initial text.");
    }

}//end of EditorFrame::initSampleDocument
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorFrame::initSampleAttributes
//
// This is just for demo purposes and is used by initSampleDocument to apply
// various types of text attributes to a bit of sample text.
//
// An array of attribute sets is created, each entry has a different type of
// attributes in order to display a variety of text styles.
//

protected SimpleAttributeSet[] initSampleAttributes(int length) {

// create an array of attribute sets

SimpleAttributeSet[] attrs = new SimpleAttributeSet[length];

// create set for array index 0 and set some attributes
attrs[0] = new SimpleAttributeSet();
StyleConstants.setFontFamily(attrs[0], "SansSerif");
StyleConstants.setFontSize(attrs[0], 16);

// create another set, using first set as starting point, change attribute(s)
attrs[1] = new SimpleAttributeSet(attrs[0]);
StyleConstants.setBold(attrs[1], true);

// create another set, using first set as starting point, change attribute(s)
attrs[2] = new SimpleAttributeSet(attrs[0]);
StyleConstants.setItalic(attrs[2], true);

// create another set, using first set as starting point, change attribute(s)
attrs[3] = new SimpleAttributeSet(attrs[0]);
StyleConstants.setFontSize(attrs[3], 20);

// create another set, using first set as starting point, change attribute(s)
attrs[4] = new SimpleAttributeSet(attrs[0]);
StyleConstants.setFontSize(attrs[4], 12);

// create another set, using first set as starting point, change attribute(s)
attrs[5] = new SimpleAttributeSet(attrs[0]);
StyleConstants.setForeground(attrs[5], Color.red);

return attrs;

}//end of EditorFrame::initSampleAttributes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorFrame::createActionTable
//
// Creates a hash table with a list of actions and their names as retrieved
// from the editor kit of pTextComponent.
//
// JTextComponent objects have built in actions, each of which has a string
// name.  These can be retrieved and used to build menus or to connect with
// keystrokes to allow the user to execute the various actions.
//

private HashMap<Object, Action> createActionTable(JTextComponent pTextComponent)
{

HashMap<Object, Action> actionsList = new HashMap<Object, Action>();

//get a list of available actions from the text component
Action[] actionsArray = pTextComponent.getActions();

//insert the action/acton name pairs into the hash map

for (int i = 0; i < actionsArray.length; i++) {
    Action a = actionsArray[i];
    actionsList.put(a.getValue(Action.NAME), a);
    }

return actionsList;

}//end of EditorFrame::createActionTable
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorFrame::getActionByName
//
// Returns the action in the actions hash table which is associated with the
// string pName.
//
// See createActionTable for details on the hash table.
//

private Action getActionByName(String pName)
{

return actions.get(pName);

}//end of EditorFrame::getActionByName
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorFrame::loadFile
//
// Loads a file from disk.
//

public void loadFile(String pFilepath)
{

try {
    FileInputStream fr = new FileInputStream(pFilepath);
    InputStreamReader isr = new InputStreamReader(fr, "UTF-8");
    BufferedReader reader = new BufferedReader(isr);
    StringBuilder buffer = new StringBuilder();

    int x = 0;

    String line = null;
    while ((line = reader.readLine()) != null) {
        buffer.append(line).append("\n");
	}

    reader.close();

    textPane.setText(buffer.toString());

    }
catch (IOException e) {

    }

//Font font = new Font("Serif", Font.PLAIN, 14);
Font font = new Font("Monospaced", Font.PLAIN, 14);

setJTextPaneFont(textPane, font, Color.BLACK);

textPane.setCaretPosition(0);

}//end of EditorFrame::loadFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorFrame::setJTextPaneFont
//
// Sets the font for the entire contents of pJTextPane.
//
// WIP -- need to add ability to change font from current position onward?
//

public void setJTextPaneFont(JTextPane jtp, Font font, Color c) {

//create a tabSet to set the tab spacing

int x = 32, d = 32;

TabSet tabSet = new TabSet(new TabStop[] {
    new TabStop(x), new TabStop(x+=d), new TabStop(x+=d), new TabStop(x+=d),
    new TabStop(x), new TabStop(x+=d), new TabStop(x+=d), new TabStop(x+=d),
    new TabStop(x), new TabStop(x+=d), new TabStop(x+=d), new TabStop(x+=d),
    new TabStop(x), new TabStop(x+=d), new TabStop(x+=d), new TabStop(x+=d),
    new TabStop(x), new TabStop(x+=d), new TabStop(x+=d), new TabStop(x+=d),
    new TabStop(x), new TabStop(x+=d), new TabStop(x+=d), new TabStop(x+=d),
    new TabStop(x), new TabStop(x+=d), new TabStop(x+=d), new TabStop(x+=d),
    new TabStop(x), new TabStop(x+=d), new TabStop(x+=d), new TabStop(x+=d)    
    });

// before setting new character attributes, the existing ones are retrieved
// JTextPane.getCharacterAttributes returns an AttributeSet object which must
// be converted to an MutableAttributeSet object for use by the
// StyleConstants functions
// an example from the web used
//      MutableAttributeSet attrs = jtp.getInputAttributes();
// which returned a Mutable set which avoided the conversion to mutable -- but
// it is not explained (adequately) in the help what getInputAttributes returns
// so the below seemed to be the more precise method

AttributeSet iattrs = jtp.getCharacterAttributes();
MutableAttributeSet attrs = new SimpleAttributeSet();
attrs.addAttributes(iattrs);

// Set the font family, size, and style, based on properties of
// the Font object. Note that JTextPane supports a number of
// character attributes beyond those supported by the Font class.
// For example, underline, strike-through, super- and sub-script.
StyleConstants.setFontFamily(attrs, font.getFamily());
StyleConstants.setFontSize(attrs, font.getSize());
StyleConstants.setItalic(attrs, (font.getStyle() & Font.ITALIC) != 0);
StyleConstants.setBold(attrs, (font.getStyle() & Font.BOLD) != 0);

// Set the font color
StyleConstants.setForeground(attrs, c);

// Retrieve the pane's document object
StyledDocument sDoc = jtp.getStyledDocument();

// Replace the style for the entire document. We exceed the length
// of the document by 1 so that text entered at the end of the
// document uses the attributes.

sDoc.setCharacterAttributes(0, doc.getLength() + 1, attrs, false);

// set paragraph attributes to adjust the tab spacing
// see notes above for getCharacterAttributes for more info on converting
// from non-mutable to mutable attribute set

iattrs = jtp.getParagraphAttributes();
attrs = new SimpleAttributeSet();
attrs.addAttributes(iattrs);

StyleConstants.setTabSet(attrs, tabSet);

sDoc.setParagraphAttributes(0, doc.getLength() + 1, attrs, false);

}//end of EditorFrame::setJTextPaneFont
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class CaretListenerLabel
//
// This listens for and reports caret movements.
//

protected class CaretListenerLabel extends JLabel implements CaretListener {

//-----------------------------------------------------------------------------
// CaretListenerLabel::CaretListenerLabel (constructor)
//

public CaretListenerLabel(String label) {

super(label);

}//end of CaretListenerLabel::CaretListenerLabel (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CaretListenerLabel::caretUpdate
//
//Might not be invoked from the event dispatch thread.
//

public void caretUpdate(CaretEvent e) {

displaySelectionInfo(e.getDot(), e.getMark());

}//end of CaretListenerLabel::caretUpdate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CaretListenerLabel::displaySelectionInfo
//
// This method can be invoked from any thread.  It invokes the setText
// and modelToView methods, which must run on the event dispatch thread. We use
// invokeLater to schedule the code for execution on the event dispatch thread.
//

protected void displaySelectionInfo(final int dot, final int mark)
{

SwingUtilities.invokeLater(
    new Runnable() {

        public void run() {
            if (dot == mark) {  // no selection
                try {
                    Rectangle caretCoords = textPane.modelToView(dot);
                    //Convert it to view coordinates.
                    setText("caret: text position: " + dot
                                 + ", view location = [" + caretCoords.x + ", "
                                               + caretCoords.y + "]" + newline);
                    }
                catch (BadLocationException ble) {
                    setText("caret: text position: " + dot + newline);
                    }
                }// if (dot == mark)
            else if (dot < mark) {
                setText("selection from: " + dot + " to " + mark + newline);
                }
            else {
                setText("selection from: " + mark + " to " + dot + newline);
                }
            } //public void run()
        }// new Runnable()
    ); //SwingUtilities.invokeLater

}//end of CaretListenerLabel::displaySelectionInfo
//-----------------------------------------------------------------------------

}//end of class CaretListenerLabel
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class MyUndoableEditListener
//
// This one listens for edits that can be undone.
//

protected class MyUndoableEditListener implements UndoableEditListener {

//-----------------------------------------------------------------------------
// MyUndoableEditListener::undoableEditHappened
//

public void undoableEditHappened(UndoableEditEvent e) {

//Remember the edit and update the menus.

undo.addEdit(e.getEdit());
undoAction.updateUndoState();
redoAction.updateRedoState();

}//end of MyUndoableEditListener::undoableEditHappened
//-----------------------------------------------------------------------------

}//end of class MyUndoableEditListener
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class MyDocumentListener
//
// And this one listens for any changes to the document.
//

protected class MyDocumentListener implements DocumentListener {


//-----------------------------------------------------------------------------
// MyDocumentListener::(various listener functions)
//
// These functions are implemented per requirements of an interface and have
// minimal code at the present time.  As code is added to each function, it
// should be moved from this section and formatted properly.
//

public void insertUpdate(DocumentEvent e)
{
displayEditInfo(e);
}

public void removeUpdate(DocumentEvent e)
{
displayEditInfo(e);
}

public void changedUpdate(DocumentEvent e)
{
displayEditInfo(e);
}

//end of MyDocumentListener::(various listener functions)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MyDocumentListener::displayEditInfo
//
// Displays a running log of all edit changes to the document.
//

private void displayEditInfo(DocumentEvent e) {

//display a log of all changes made in a separate text area

/*

Document document = e.getDocument();
int changeLength = e.getLength();
changeLog.append(e.getType().toString() + ": " +
                changeLength + " character" +
                ((changeLength == 1) ? ". " : "s. ") +
                " Text length = " + document.getLength() +
                "." + newline);
 
 */

}//end of MyDocumentListener::displayEditInfo
//-----------------------------------------------------------------------------

}//end of class MyDocumentListener
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class UndoAction
//
//

class UndoAction extends AbstractAction {

//-----------------------------------------------------------------------------
// UndoAction::UndoAction (constructor)
//

public UndoAction()
{

super("Undo");
setEnabled(false);

}//end of UndoAction::UndoAction (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UndoAction::actionPerformed
//

public void actionPerformed(ActionEvent e)
{

try {
    undo.undo();
    }
catch (CannotUndoException ex) {
    System.out.println("Unable to undo: " + ex);
    //for debugging add this -> ex.printStackTrace();
    }

updateUndoState();
redoAction.updateRedoState();

}//end of UndoAction::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UndoAction::updateUndoState
//

protected void updateUndoState()
{

if (undo.canUndo()) {
    setEnabled(true);
    putValue(Action.NAME, undo.getUndoPresentationName());
    }
else {
    setEnabled(false);
    putValue(Action.NAME, "Undo");
    }

}//end of UndoAction::updateUndoState
//-----------------------------------------------------------------------------

}//end of class UndoAction
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class RedoAction
//
//

class RedoAction extends AbstractAction {


//-----------------------------------------------------------------------------
// RedoAction::RedoAction (constructor)
//


public RedoAction()
{

super("Redo");

setEnabled(false);

}//end of RedoAction::RedoAction (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RedoAction::actionPerformed
//

public void actionPerformed(ActionEvent e)
{

try {
    undo.redo();
    }
catch (CannotRedoException ex) {
        System.out.println("Unable to redo: " + ex);
        //for debugging add this -> ex.printStackTrace();
        }

updateRedoState();
undoAction.updateUndoState();

}//end of RedoAction::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RedoAction::updateRedoState
//

protected void updateRedoState()
{

if (undo.canRedo()) {
    setEnabled(true);
    putValue(Action.NAME, undo.getRedoPresentationName());
    }
else {
    setEnabled(false);
    putValue(Action.NAME, "Redo");
    }

}//end of RedoAction::updateRedoState
//-----------------------------------------------------------------------------

}//end of class RedoAction
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

}//end of class EditorFrame
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

