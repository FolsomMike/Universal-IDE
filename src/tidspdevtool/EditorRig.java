/******************************************************************************
* Title: TI DSP Dev Tool - EditorRig.java File
* Author: Mike Schoonover
* Date: 06/26/11
*
* Purpose:
*
* This class handles a panel containing a scrolling editor pane and all
* associated components such as status windows, status labels, undo controls,
* etc.
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

//-----------------------------------------------------------------------------

package tidspdevtool;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class EditorRig
//
//

public class EditorRig extends JPanel{

    EditorFrame editorFrame;
    EditorTabPane editorTabPane;

    String fullPath; //full path and filename of file loaded into text pane

    static final int MAX_CHARACTERS = 1000000;
    String newline = "\n";

    //undo manager -- handles undo/redo actions
    //each EditorRig, and thus each document, will have its own undo manager so
    //changes can be tracked separately for each
    public UndoManager undo = new UndoManager();

    JTextPane textPane;
    AbstractDocument doc = null;
    JPanel toolPanel;
    TextSearcher textSearcher;
    HashMap<Object, Action> actions;

    private boolean docModified; //true if the document has been modified

//-----------------------------------------------------------------------------
// EditorRig::EditorRig (constructor)
//

public EditorRig(EditorFrame pEditorFrame, EditorTabPane pEditorTabPane)
{

    editorFrame = pEditorFrame; editorTabPane = pEditorTabPane;

}//end of EditorRig::EditorRig (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorRig::init
//
// Creates a scrolling editor pane with associated components.
//
// The rig is contained in a JPanel which is returned to the caller
//

public void init(UndoableEditListener pUndoableEditListener)
{

    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

    //create the text pane and configure it
    textPane = new JTextPane();
    textPane.setAlignmentX(Component.LEFT_ALIGNMENT);
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
    scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
    Settings.setSizes(scrollPane, 1000, 535);

    //create a panel to hold search tools and status messages
    toolPanel = new JPanel();
    toolPanel.setLayout(new BoxLayout(toolPanel, BoxLayout.LINE_AXIS));
    toolPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    Settings.setSizes(toolPanel, 1000, 33);

    //create a text search panel and add it to the tool panel
    textSearcher = new TextSearcher(textPane);
    textSearcher.init();
    textSearcher.setAlignmentY(Component.BOTTOM_ALIGNMENT);
    toolPanel.add(textSearcher);

    //create a status message panel and add it to the tool panel
    JPanel statusPanel = new JPanel(new GridLayout(1, 1));
    toolPanel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
    CaretListenerLabel caretListenerLabel =
                                    new CaretListenerLabel("Caret Status");
    statusPanel.add(caretListenerLabel);
    toolPanel.add(statusPanel);

    //add the components to the editor rig panel
    add(scrollPane);
    add(toolPanel);

    //Start watching for undoable edits and caret changes.
    doc.addUndoableEditListener(pUndoableEditListener);

    textPane.addCaretListener(caretListenerLabel);
    doc.addDocumentListener(new MyDocumentListener(this));

    // create a list of actions available for the text editor component
    actions = createActionTable(textPane);

    //add key bindings -- connects keyboard shortcuts to actions
    addBindings();

}//end of EditorRig::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorRig::addBindings
//
// Connect keyboard shortcuts to actions available in the editor kit currently
// being used by the textPane.
//
// Also adds custom actions provided by this program, such as ctrl-F and F3
// for text searching.
//
// Note that the ActionMap is already in place for the JTextPane via its
// current EditorKit. This just puts the names for those actions into the
// InputMap to connect the key stroke with the action.
//
// See EditorRig.createActionTable and the comments at the top of
// EditorFrame.java for more details.
//

private void addBindings()
{

    //Add a couple of Emacs key bindings for navigation.

    InputMap inputMap = textPane.getInputMap();

    KeyStroke key;

    // example of using an action from the textpane's editor kit
    //Ctrl-p to go up one line
    //key = KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK);
    //inputMap.put(key, DefaultEditorKit.upAction);

    //add custom actions not part of the text pane's editor kit

    //Ctrl-f to invoke the find function
    key = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK);
    inputMap.put(key, new FindTextAction());

    //F3 to invoke the "find next" function
    key = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0);
    inputMap.put(key, new FindNextAction());

}//end of EditorRig::addBindings
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorRig::createEditMenu
//
// Creates the edit menu using custom actions and actions available from the
// editor kit for the text editor component.
//
// EditorRig.createActionTable should be called first to create a HashMap of
// the actions available for the JTextPane via the EditorKit in use at any
// given time.
//
// See EditorRig.createActionTable and the comments at the top of
// EditorFrame.java for more details.
//

public JMenu createEditMenu(
                      AbstractAction pUndoAction, AbstractAction pRedoAction) {

    JMenu menu = new JMenu("Edit");

    //use the custom actions for the menu
    menu.add(pUndoAction);
    menu.add(pRedoAction);

    menu.addSeparator();

    //These actions come from the default editor kit.
    //Get the ones we want and stick them in the menu.

    menu.add(getActionByName(DefaultEditorKit.cutAction));
    menu.add(getActionByName(DefaultEditorKit.copyAction));
    menu.add(getActionByName(DefaultEditorKit.pasteAction));

    menu.addSeparator();

    menu.add(getActionByName(DefaultEditorKit.selectAllAction));

    return menu;

}//end of EditorRig::createEditMenu
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorRig::createStyleMenu
//
// Creates the style menu using actions available from the editor kit for
// the text editor component.
//

public JMenu createStyleMenu() {

    JMenu menu = new JMenu("Style");

    Action action;

    action = new StyledEditorKit.BoldAction();
    action.putValue(Action.NAME, "Bold");
    //this is how you add a mnemonic to an action? doesn't work here
    //action.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_Z);
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

}//end of EditorRig::createStyleMenu
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorRig::initSampleDocument
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
            doc.insertString(doc.getLength(), initString[i] +
                                                            newline, attrs[i]);
        }
    }
    catch (BadLocationException ble) {
        System.err.println("Couldn't insert initial text.");
    }

}//end of EditorRig::initSampleDocument
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorRig::initSampleAttributes
//
// This is just for demo purposes and is used by initSampleDocument to apply
// various types of text attributes to a bit of sample text.
//
// An array of attribute sets is created, each entry has a different type of
// attributes in order to display a variety of text styles.
//

protected SimpleAttributeSet[] initSampleAttributes(int length) {

    //create an array of attribute sets

    SimpleAttributeSet[] attrs = new SimpleAttributeSet[length];

    // create set for array index 0 and set some attributes
    attrs[0] = new SimpleAttributeSet();
    StyleConstants.setFontFamily(attrs[0], "SansSerif");
    StyleConstants.setFontSize(attrs[0], 16);

    //create another set, using first set as starting point, change attribute(s)
    attrs[1] = new SimpleAttributeSet(attrs[0]);
    StyleConstants.setBold(attrs[1], true);

    //create another set, using first set as starting point, change attribute(s)
    attrs[2] = new SimpleAttributeSet(attrs[0]);
    StyleConstants.setItalic(attrs[2], true);

    //create another set, using first set as starting point, change attribute(s)
    attrs[3] = new SimpleAttributeSet(attrs[0]);
    StyleConstants.setFontSize(attrs[3], 20);

    //create another set, using first set as starting point, change attribute(s)
    attrs[4] = new SimpleAttributeSet(attrs[0]);
    StyleConstants.setFontSize(attrs[4], 12);

    //create another set, using first set as starting point, change attribute(s)
    attrs[5] = new SimpleAttributeSet(attrs[0]);
    StyleConstants.setForeground(attrs[5], Color.red);

    return attrs;

}//end of EditorRig::initSampleAttributes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorRig::createActionTable
//
// Creates a hash table with a list of actions and their names as retrieved
// from the editor kit of pTextComponent.
//
// Note that the ActionMap is already in place for the JTextPane via its
// current EditorKit. This just gets a list of those actions. The programmer
// then adds to the JTextPanes InputMap to connect keystrokes to actions.
//
// JTextComponent objects have built in actions supplied by the EditorKit in
// use at any given time. Each Action of has a string name.  These can be
// retrieved and used to build menus or to connect with keystrokes to allow the
// user to execute the various actions. They can also be added to menus.
//
// See EditorRig.addBindings and EditorRig.createEditMenu and the comments at
// the top of EditorFrame.java for more details.
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

}//end of EditorRig::createActionTable
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorRig::getActionByName
//
// Returns the action in the actions hash table which is associated with the
// string pName.
//
// See createActionTable for details on the hash table.
//

private Action getActionByName(String pName)
{

    return actions.get(pName);

}//end of EditorRig::getActionByName
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorRig::loadFile
//
// Loads a file into textPane from disk.
//

public void loadFile(String pFilepath)
{

    fullPath = pFilepath;

    try {

        FileInputStream fr = new FileInputStream(pFilepath);
        InputStreamReader isr = new InputStreamReader(fr, "UTF-8");
        BufferedReader reader = new BufferedReader(isr);
        StringBuilder buffer = new StringBuilder();

        int x = 0;

        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line).append("\n");
        }

        reader.close();

        textPane.setText(buffer.toString());

    }
    catch (IOException e) {

        //display error? set fullpath blank? empty the text pane?

    }

    //Font font = new Font("Serif", Font.PLAIN, 14);
    Font font = new Font("Monospaced", Font.PLAIN, 14);

    setJTextPaneFont(textPane, font, Color.BLACK);

    textPane.setCaretPosition(0);

    //when the file is loaded, the document listeners fire and the modified
    //flag gets set -- unset it as the document is yet unchanged by the user
    clearDocumentModifiedFlag();

}//end of EditorRig::loadFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorRig::saveFile
//
// Saves a file from textPane to disk and clears the docModified flag.
//

public void saveFile()
{

    BufferedWriter out;

    try
    {

        out = new BufferedWriter(new FileWriter(fullPath));
        textPane.write(out);

        clearDocumentModifiedFlag();

    } catch (IOException e){

    }

}//end of EditorRig::saveFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorRig::clearDocumentModifiedFlag
//
// Clears the flag to note that the document has been not been modified since
// the last save or load.
//
// Also clears any visual flags.
//

private void clearDocumentModifiedFlag() {

    docModified = false;

    //tell the editor frame to remove the visual flag
    editorFrame.removeDocModifiedMarker(this);

}//end of EditorRig::clearDocumentModifiedFlag
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorRig::flagDocumentAsModified
//
// Sets a flag to note that the document has been modified.
// Adds an asterisk after the document's name in the pane's tab so the user
// can tell which documents have been modified.
//

void flagDocumentAsModified() {

    docModified = true;

    //tell the editor frame to display a visual flag of some sort
    editorFrame.addDocModifiedFlag(this);

}//end of EditorRig::flagDocumentAsModified
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorRig::isDocumentModified
//
// Returns the docModified flag which will be true if the document has been
// modified since the last load or save.
//

public boolean isDocumentModified() {

    return(docModified);

}//end of EditorRig::isDocumentModified
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorRig::prepareToClose
//
// This function should be called when the editor is about to be closed. If
// the document has been modified, the user will be given a chance to save it
// or cancel the closing.
//
// Returns true if the file has not been modified or if the user chooses to
// close (saving or not saving).
// Returns false if the user chooses to cancel.
//

public boolean prepareToClose() {

    //always allow close if document is unmodified
    if (!isDocumentModified()) {return(true);}

    //make each modified file the selected tab before querying user
    editorTabPane.setSelectedComponent(this);

    //if the document associated with this tab has been modified, ask user if
    //it is to be saved before the tab is closed

    int saveFileResponse = JOptionPane.showConfirmDialog(
            this,
            "The file has been modified. Save changes?",
            "File Changed Warning",
            JOptionPane.YES_NO_CANCEL_OPTION);

    //do not save or close tab if user cancels
    if (saveFileResponse == JOptionPane.CANCEL_OPTION) {return(false);}

    //save the file if user chose "Yes"
    if (saveFileResponse == JOptionPane.YES_OPTION){
        saveFile();
    }

    return(true);

}//end of EditorRig::prepareToClose
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorRig::setJTextPaneFont
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
    // JTextPane.getCharacterAttributes returns an AttributeSet object which
    // must be converted to an MutableAttributeSet object for use by the
    // StyleConstants functions
    // an example from the web used
    //      MutableAttributeSet attrs = jtp.getInputAttributes();
    // which returned a Mutable set which avoided the conversion to mutable --
    // but it is not explained (adequately) in the help what getInputAttributes
    // returns so the below seemed to be the more precise method

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

}//end of EditorRig::setJTextPaneFont
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EditorRig::findTabContainingDoc
//
// Sets the font for the entire contents of pJTextPane.
//
// WIP -- need to add ability to change font from current position onward?
//

public void findTabContainingDoc(JTextPane jtp, Font font, Color c) {


}//end of EditorRig::findTabContaingingDoc
//-----------------------------------------------------------------------------

/*

//-----------------------------------------------------------------------------
// EditorRig::findText

// debug mks -- delete this function

public void GetTextToFindAndFind(
                    String pTextToFind, int pIgnoreCase, int pFindCounter){

    // findCounter = 0 or 1. 0 represents find and 1 represents findCounter.
    String currentText = "";
    String replacer = "";
    StringReader readText;
    BufferedReader readBuffer;

    if(pFindCounter ==0){

        if(pTextToFind == null){
            JOptionPane.showMessageDialog(
                                        null, "Please Enter Text.", "Error", 0);
        }
        else if(pTextToFind.isEmpty()){
            JOptionPane.showMessageDialog(
                                        null, "Please Enter Text.", "Error", 0);
        }
    else{
    // Use any Character. But I a suggest to use a character from an
    //Encrypted file.

    replacer = "¥";

    currentText = textPane.getText();

    if(pIgnoreCase==1){
        currentText = currentText.toLowerCase();
        pTextToFind = pTextToFind.toLowerCase();
    }

    int counter = 0;

    readText = new StringReader(currentText);
    readBuffer = new BufferedReader(readText);

    try {
        String Line = readBuffer.readLine();
        int found = 0;
        while(Line!=null || found != 1){
            if(Line.contains(pTextToFind)){
                Line = null;
                found = 1;
            }
            if(Line!=null){
                Line = readBuffer.readLine();
                counter = counter + 1;
            }
        }
        if(found == 1){
            textPane.setSelectionStart(
                           currentText.indexOf(pTextToFind) - counter);
            textPane.setSelectionEnd(currentText.indexOf(
                        pTextToFind) + pTextToFind.length() - counter);
            int counter2 = 1;
            while(counter2!=pTextToFind.length()){
                replacer = replacer + "¥";
                counter2 = counter2 + 1;
            }
            currentText =
                       currentText.replaceFirst(pTextToFind, replacer);
            pFindCounter = 1;
        }
        else{
            JOptionPane.showMessageDialog(
                                    null, "No Matches.", "Message", 0);
            }

        }
        catch (IOException e) {
            //obsolete call -> e.printStackTrace();
    }  catch(NullPointerException e){
        JOptionPane.showMessageDialog(
                                    null, "No Matches.", "Message", 0);
    }
    }
}
else{
int counter = 0;
readText = new StringReader(currentText);
readBuffer = new BufferedReader(readText);
try {
    String line = readBuffer.readLine();
    int found = 0;
    while(line!=null || found != 1){
        if(line.contains(pTextToFind)){
            line = null;
            found = 1;
        }
        if(line!=null){
            line = readBuffer.readLine();
            counter = counter + 1;
        }
    }
    if(found == 1){
        textPane.setSelectionStart(currentText.indexOf(pTextToFind) -
                                                              counter);
        textPane.setSelectionEnd(currentText.indexOf(pTextToFind) +
                                       pTextToFind.length() - counter);
        currentText = currentText.replaceFirst(pTextToFind, replacer);
        }
    else{
        JOptionPane.showMessageDialog(
                                    null, "No Matches.", "Message", 0);
        }
    }
     catch(IOException e){
        //obsolete call -> e.printStackTrace();
    } catch(NullPointerException e){
        JOptionPane.showMessageDialog(
                                    null, "No Matches.", "Message", 0);
        }
    }


}//end of EditorRig::findText
//-----------------------------------------------------------------------------

*/


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
// class FindTextAction
//
// Gives focus to the text find window and clears it out so the user can type
// in text to search for. The TextSearcher class will respond to the "enter"
// key and search for the text.
//

protected class FindTextAction extends AbstractAction{

//-----------------------------------------------------------------------------
// FindTextAction (constructor)
//

public FindTextAction() {

    //action triggered by "ctrl-F"
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(
                                 KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK ));

}//end of FindTextAction (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FindTextAction::actionPerformed
//
// Called when the action is triggered by menu, mnemonic, or accelerator key
// stroke.
//

public void actionPerformed(ActionEvent e) {

    //set cursor and focus in the text-to-find entry box
    textSearcher.prepareTextFindField();

}//end of FindTextAction::actionPerformed
//-----------------------------------------------------------------------------

}//end of class FindTextAction
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class FindNextAction
//
// Calls TextSearcher.findNext to search for the next occurrance of the phrase
// in the text-to-find box. Starts searching just after the location of the
// last phrase found.
//

protected class FindNextAction extends AbstractAction{

//-----------------------------------------------------------------------------
// FindNextAction (constructor)
//

public FindNextAction() {

    //action triggered by "ctrl-F"
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));

}//end of FindNextAction (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FindNextAction::actionPerformed
//
// Called when the action is triggered by menu, mnemonic, or accelerator key
// stroke.
//

public void actionPerformed(ActionEvent e) {

    //find the next occurrance of the search phrase
    textSearcher.findNext();

}//end of FindNextAction::actionPerformed
//-----------------------------------------------------------------------------

}//end of class FindNextAction
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class MyDocumentListener
//
// And this one listens for any changes to the document.
//

protected class MyDocumentListener implements DocumentListener {


    EditorRig editorRig;

//-----------------------------------------------------------------------------
// MyDocumentListener::MyDocumentListener (constructor)
//

public MyDocumentListener(EditorRig pEditorRig)
{

    editorRig = pEditorRig;

}//end of MyDocumentListener::MyDocumentListener (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MyDocumentListener::(various listener functions)
//
// These functions are implemented per requirements of an interface and have
// minimal code at the present time.  As code is added to each function, it
// should be moved from this section and formatted properly.
//

public void insertUpdate(DocumentEvent e)
{

    flagDocumentAsModified(); //set a flag to show that a change has occurred

    displayEditInfo(e);
}

public void removeUpdate(DocumentEvent e)
{

    flagDocumentAsModified(); //set a flag to show that a change has occurred

    displayEditInfo(e);
}

public void changedUpdate(DocumentEvent e)
{

    flagDocumentAsModified(); //set a flag to show that a change has occurred

    displayEditInfo(e);
}

//end of MyDocumentListener::(various listener functions)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MyDocumentListener::flagDocumentAsModified
//
// Sets flag(s) and visual(s) to note that the document has been modified.
//

private void flagDocumentAsModified() {

    editorRig.flagDocumentAsModified();

}//end of MyDocumentListener::flagDocumentAsModified
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

}//end of class EditorRig
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
