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
// class EditorRig
//
//

public class EditorRig extends JPanel{

static final int MAX_CHARACTERS = 1000000;
String newline = "\n";

//undo helpers
protected UndoAction undoAction;
protected RedoAction redoAction;
protected UndoManager undo = new UndoManager();

JTextPane textPane;
AbstractDocument doc = null;
JTextArea changeLog;
HashMap<Object, Action> actions;

//-----------------------------------------------------------------------------
// EditorRig::EditorRig (constructor)
//

public EditorRig()
{




}//end of EditorRig::EditorRig (constructor)
//-----------------------------------------------------------------------------











}//end of class EditorRig
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

