/******************************************************************************
* Title: Universal IDE - ProjectFrame.java
* Author: Mike Schoonover
* Date: 06/13/11
*
* Purpose:
*
* A JInternalFrame which allows the user to manage files in a project.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package basicide;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;


//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class ProjectFrame
//
//

public class ProjectFrame extends JInternalFrame implements
        TreeSelectionListener, TreeModelListener, ActionListener
{

    JTextPane textPane;
    AbstractDocument doc;
    DefaultMutableTreeNode rootNode;
    DefaultTreeModel treeModel;

    Settings settings;

    private int newNodeSuffix = 1;
    private JEditorPane htmlPane;
    private JTree tree;
    private URL helpURL;
    private static boolean DEBUG = false;

    //Optionally play with line styles.  Possible values are
    //"Angled" (the default), "Horizontal", and "None".
    private static boolean playWithLineStyle = false;
    private static String lineStyle = "Horizontal";


    private static String ADD_NODE_COMMAND = "add node";
    private static String REMOVE_NODE_COMMAND = "remove node";
    private static String CLEAR_NODE_COMMAND = "clear node(s)";

//-----------------------------------------------------------------------------
// ProjectFrame::ProjectFrame (constructor)
//
//

public ProjectFrame(String pTitle, boolean pResizable, boolean pCloseable,
              boolean pMaximizable, boolean pIconifiable, Settings pSettings) {

    super(pTitle, pResizable, pCloseable, pMaximizable, pIconifiable);

    settings = pSettings;

}//end of ProjectFrame::ProjectFrame (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ProjectFrame::init
//

public void init(){

    // add a JPanel to the content pane in order to use JPanel's familiar
    //features
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
    getContentPane().add(mainPanel);

    // create a panel to hold the split pane of tree/viewer
    // adding the split pane directly to main panel caused weird layout
    JPanel treeStuffPanel = new JPanel();
    treeStuffPanel.setLayout(new GridLayout(1,0));
    mainPanel.add(treeStuffPanel);

    //create the root node and all its children
    createRootNode();

    // Force the tree model to the DefaultTreeModel so useful methods are sure
    // to be available.
    // The true value sets asksAllowsChildren so that nodes set up as branches
    // (folders) will always be branches even if they have no children and leaf
    // nodes will always be leafs and cannot have children. By setting true,
    // the tree respects the allowsChildren setting of each node -- this value
    // is specified when the node is created so it can be force to be a branch
    // or a node.
    // If asksAllowChildren is set to false, then branches change to leaf nodes
    // when all their children are deleted and leaf nodes change to branches if
    // a child is added to them.
    // We want to control what is a branch (folder) node and what is a leaf so
    // we set to true.

    treeModel = new DefaultTreeModel(rootNode, true);

    //don't use this -- add this class as the listener instead of creating a
    //new listening object
    //treeModel.addTreeModelListener(new MyTreeModelListener());

    tree = new JTree(treeModel);
    tree.setEditable(true);
    tree.setShowsRootHandles(true);

    //create a tree that allows one selection at a time
    tree = new JTree(treeModel);
    tree.getSelectionModel().setSelectionMode
                                    (TreeSelectionModel.SINGLE_TREE_SELECTION);

    //listen for when the selection changes
    tree.addTreeSelectionListener(this);

    if (playWithLineStyle) {
        System.out.println("line style = " + lineStyle);
        tree.putClientProperty("JTree.lineStyle", lineStyle);
    }

    //create the scroll pane and add the tree to it
    JScrollPane treeView = new JScrollPane(tree);

    //create the HTML viewing pane
    htmlPane = new JEditorPane();
    htmlPane.setEditable(false);
    initHelp();
    JScrollPane htmlView = new JScrollPane(htmlPane);

    //add the scroll panes to a split pane
    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    splitPane.setTopComponent(treeView);
    splitPane.setBottomComponent(htmlView);

    Dimension minimumSize = new Dimension(100, 50);
    htmlView.setMinimumSize(minimumSize);
    treeView.setMinimumSize(minimumSize);
    splitPane.setDividerLocation(300);
    splitPane.setPreferredSize(new Dimension(500, 300));

    //add the split pane to the appropriate panel
    treeStuffPanel.add(splitPane);

    //create the control panel
    JButton addButton = new JButton("Add");
    addButton.setActionCommand(ADD_NODE_COMMAND);
    addButton.addActionListener(this);

    JButton removeButton = new JButton("Remove");
    removeButton.setActionCommand(REMOVE_NODE_COMMAND);
    removeButton.addActionListener(this);

    JButton clearButton = new JButton("Clear");
    clearButton.setActionCommand(CLEAR_NODE_COMMAND);
    clearButton.addActionListener(this);

    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
    panel.add(addButton);
    panel.add(removeButton);
    panel.add(clearButton);
    mainPanel.add(panel);

    //put extra space at the botom of the panel
    mainPanel.add(Box.createVerticalGlue());

    MouseListener ml = new TreeMouseListener();
    tree.addMouseListener(ml);

    /*
    //debug mks
    JButton addButton1 = new JButton("Add");
    JButton removeButton1 = new JButton("Remove");
    JButton clearButton1 = new JButton("Clear");
    JPanel panel1 = new JPanel();
    panel1.setLayout(new BoxLayout(panel1, BoxLayout.LINE_AXIS));
    panel1.add(addButton1);
    panel1.add(removeButton1);
    panel1.add(clearButton1);
    mainPanel.add(panel1);
    //debug mks end
     *
     */

}//end of ProjectFrame::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class TreeMouseListener
//

class TreeMouseListener extends MouseAdapter
{


//-----------------------------------------------------------------------------
// TreeMouseListener::mousePressed
//

@Override
public void mousePressed(MouseEvent e) {

    int selRow = tree.getRowForLocation(e.getX(), e.getY());
     TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
     if(selRow != -1) {
         if(e.getClickCount() == 1) {
    //             mySingleClick(selRow, selPath);
         }
         else if(e.getClickCount() == 2) {
             DefaultMutableTreeNode node =
                     (DefaultMutableTreeNode)selPath.getLastPathComponent();

             try{
                FileInfo fi = (FileInfo)node.getUserObject();
                //load the double-clicked file into an editor pane
                settings.editorFrame.loadFile(fi.fileName, fi.fullPath);
             }
             catch(ClassCastException cce){
                 //do nothing if user clicks on a node which is not a file,
                 //such as a folder node
                 return;
             }

         }
     }

}//end of TreeMouseListener::mousePressed
//-----------------------------------------------------------------------------

}//end of class TreeMouseListener
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class TreeBranchCategory
//
// Provides a base class for a category of items used as an Object for a
// tree branch node.
//

private class TreeBranchCategory {

    String name;
    ArrayList<File> list;

//-----------------------------------------------------------------------------
// FileCategory::toString
//

@Override
public String toString() {

    return name;

}//end of FileCategory::toString
//-----------------------------------------------------------------------------

}//end of class TreeBranchCategory
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class FileCategory
//
// Stores info for a file category.
//

private class FileCategory extends TreeBranchCategory {

//-----------------------------------------------------------------------------
// FileCategory::FileCategory (constructor)
//

public FileCategory(String pName, ArrayList<File> pList) {

    name = pName; list = pList;

}//end of FileCategory::FileCategory (constructor)
//-----------------------------------------------------------------------------

}//end of class FileCategory
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class FileInfo
//
// Stores info for a file.
//

private class FileInfo {

    public String fileName;
    public String fullPath;
    public URL url;

//-----------------------------------------------------------------------------
// FileInfo::FileInfo (constructor)
//

public FileInfo(String pFileName, String pFullPath) {

    fileName = pFileName;
    fullPath = pFullPath;
    url = getClass().getResource(pFullPath);

    if (url == null) {System.err.println("Couldn't find file: " + fileName);}

}//end of FileInfo::FileInfo (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FileInfo::toString
//

@Override
public String toString() {

    return fileName;

}//end of FileInfo::toString
//-----------------------------------------------------------------------------

}//end of class FileInfo
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ProjectFrame::initHelp
//

private void initHelp()
{

    String s = "TreeDemoHelp.html";
    helpURL = getClass().getResource(s);
    if (helpURL == null) {
        System.err.println("Couldn't open help file: " + s);
    }
    else if (DEBUG) {System.out.println("Help URL is " + helpURL);}

    // this was the original method of displaying the help file
    //displayURL(helpURL);

    //here is a way to load a file using a string to specify a local file as
    //a URL
    try{
        htmlPane.setPage(
         "file:" +
         "/C:/Users/Mike/Documents/7%20-%20Java%20Projects/TI%20DSP%20Dev%20"
         + "Tool/src/tidspdevtool/TreeDemoHelp.html");
    }
    catch (IOException e) {
        System.err.println("Attempted to read a bad URL.");
    }

}//end of ProjectFrame::initHelp
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ProjectFrame::displayURL
//

private void displayURL(URL url)
{

    try {
        if (url != null) {
            htmlPane.setPage(url);
        }
        else { //null url
            htmlPane.setText("File Not Found");
            if (DEBUG){System.out.println("Attempted to display a null URL.");}
        }
    }
    catch (IOException e) {
        System.err.println("Attempted to read a bad URL: " + url);
    }

}//end of ProjectFrame::displayURL
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ProjectFrame::setNewRootNode
//
// Creates a new root node (which has the name of the project) and installs
// it as the root node in the tree. This is useful after a different project is
// loaded or created to update the tree display with the new info.
//
// This is better than erasing all the child nodes because it updates the
// root node with the name of the new project.
//

public void setNewRootNode()
{

    createRootNode(); //create root node and all the category branches
    treeModel.setRoot(rootNode);

}//end of ProjectFrame::setNewRootNode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ProjectFrame::createRootNode
//
// Creates the root node (which has the name of the project) for the project
// tree and all the children category branches.
//

private void createRootNode()
{

    rootNode = new DefaultMutableTreeNode(settings.getProjectName());
    createNodes(rootNode);

}//end of ProjectFrame::createRootNode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ProjectFrame::createNodes
//
// Creates a branch node for each category of files or other objects and
// creates leaf nodes under each branch to represent the objects belonging
// to the branch.
//

private void createNodes(DefaultMutableTreeNode pTop)
{

    createBranchNode(pTop,
                 new FileCategory("Source Code", settings.sourceCodeFileList));

    createBranchNode(pTop,
            new FileCategory("Linker Command Files", settings.linkerFileList));

    createBranchNode(pTop,
       new FileCategory("Documentation and Note Files", settings.docFileList));

}//end of ProjectFrame::createNodes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ProjectFrame::createBranchNode
//
// Creates a single branch node attached to object pBranchObject and then
// adds leaf nodes for each object in pBranchObject's array list.
//
// The branch will be added to tree node pTop.
//
// For example, the branch node can be attached to a FileCategory object which
// has an array list of files. A child leaf will then be created for each file
// in the list, each attached to FileInfo object which contains information
// for a file.
//
// Note that the FileCategory and FileInfo classes each have a toString
// function. The tree calls that function to get the name for the branch or
// the leaf.
//

private void createBranchNode(DefaultMutableTreeNode pTop,
                                            TreeBranchCategory pBranchObject)
{

    DefaultMutableTreeNode category;
    DefaultMutableTreeNode leaf;

    Iterator i;
    File file;

    //create a branch node (branch specified by the true parameter)

    category = new DefaultMutableTreeNode(pBranchObject, true);
    pTop.add(category);

    //create a leaf node for each object in the branch object's array list
    //(the branch could be an object holding a list of books, for example)
    //(node type of leaf is forced by the false parameter)

    for (i = pBranchObject.list.iterator(); i.hasNext();){

        file = (File)i.next();

        leaf = new DefaultMutableTreeNode(
                       new FileInfo(file.getName(), file.getPath()), false);
        category.add(leaf);

    }

}//end of ProjectFrame::createBranchNode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ProjectFrame::removeNodes
//
// Remove all nodes except the root node.
//

public void removeNodes()
{

    rootNode.removeAllChildren();
    treeModel.reload();

}//end of ProjectFrame::removeNodes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ProjectFrame::removeLeafNodes
//
// Remove all leaf nodes, leaving the branch nodes alone.
//
// This is useful for emptying the tree of user added objects.. For creating a
// new project it is preferable to use setNewRootNode as that will also set
// the name of the root node to the project's name.
//
// It is assumed that the DefaultTreeModel has been created with
// asksAllowsChildren set to true and branch nodes with allowsChildren set to
// true and leaf nodes with allowsChildren set to false.
//
// Note 1: This function is probably not very useful as creating a new root
//          node is generally preferable. It is left here as an example.
//
// Note 2: The enumerator is invalidated each time something is deleted from
//          the tree, so the enumerator loop is exited and restarted over and
//          over in a do loop until no leaves remain.
//

public void removeLeafNodes()
{

    boolean leafFound;

    do{

        leafFound = false;

        for (Enumeration e = rootNode.breadthFirstEnumeration();
                                                e.hasMoreElements();) {

            DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) e.nextElement();

            //don't use isLeaf to find leaves -- this gets set even for branch
            //nodes if  they have no children; use getAllowsChildren instead
            //as this value is false for leaves

            if (!node.getAllowsChildren()) {

                removeNodeAndCleanUp(node);
                leafFound = true; //repeat do loop until no leaves found
                break; //exit enum loop as it is invalid after node removal

            }
        }//for (Enumeration e...
    }while(leafFound);

}//end of ProjectFrame::removeLeafNodes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ProjectFrame::removeSelectedNode
//
// Remove the currently selected node.
//

public void removeSelectedNode()
{

    TreePath currentSelection = tree.getSelectionPath();

    if (currentSelection != null) {

        //remove the node from the tree and the object it is attached to from
        //the list handled by its parent branch

        removeNodeAndCleanUp(
             (DefaultMutableTreeNode) currentSelection.getLastPathComponent());

        }

    // either there was no selection, or the root was selected
    //toolkit.beep();

}//end of ProjectFrame::removeSelectedNode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ProjectFrame::removeNodeAndCleanUp
//
// Remove pNode from the tree and cleans up the object to which it is attached
// by removing that object from the list in which it is contained.
//
// Each branch node has a list of the objects attached to the leaves under that
// branch.
//
// If pNode is a branch node, it will not be removed.
//

public void removeNodeAndCleanUp(DefaultMutableTreeNode pNode)
{

    //don't allow branch nodes to be removed -- only leaf nodes can be
    //removed this keeps our default categories in place
    //don't use isLeaf to check for branch/leaf -- this gets set even for
    //branch nodes if they have no children

    if (pNode.getAllowsChildren()) {return;}

    //get the parent, which for a leaf should be a branch

    DefaultMutableTreeNode parent = (DefaultMutableTreeNode)(pNode.getParent());

    if (parent != null) {

        //get the file list for the selected file category
        FileCategory fileCat = (FileCategory) parent.getUserObject();
        ArrayList<File> fileList = fileCat.list;

        //remove  from the list the file attached to the node to be removed
        Object nodeInfo = pNode.getUserObject();
        File f = new File(((FileInfo)nodeInfo).fullPath);
        if (fileList.contains(f)) {fileList.remove(f);}

        treeModel.removeNodeFromParent(pNode);

        }

}//end of ProjectFrame::removeNodeAndCleanUp
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ProjectFrame::addFile
//
// Allows the user to browse for a file and add it to the specified category
// in the project file list.
//

public DefaultMutableTreeNode addFile()
{

    //get the currently selected node in the tree

    DefaultMutableTreeNode parentNode;
    TreePath parentPath = tree.getSelectionPath();

    if (parentPath == null) {
        parentNode = rootNode;
    }
    else {
        parentNode =
                  (DefaultMutableTreeNode) (parentPath.getLastPathComponent());
    }

    //exit if the parent does not allow children -- an exception will be thrown
    //if an attempt is made to add a child to such a node
    //don't use isLeaf -- this gets set even for branch nodes if they have no
    //children

    if (!parentNode.getAllowsChildren()) {return(null);}

    final JFileChooser fc = new JFileChooser(settings.getProjectPath());

    int returnVal = fc.showOpenDialog(this);

    //bail out if user did not select a file
    if (returnVal != JFileChooser.APPROVE_OPTION) {return(null);}

    File newFile = fc.getSelectedFile();

    //get the file list for the selected file category
    FileCategory fileCat = (FileCategory) parentNode.getUserObject();
    ArrayList<File> fileList = fileCat.list;

    //do nothing if the file is already in the list
    if (fileList.contains(newFile)) {return(null);}

    //add it to the file list
    fileList.add(newFile);

    //create a child for adding to the file tree
    FileInfo child = new FileInfo (newFile.getName(), newFile.getPath());

    return(addObject(parentNode, child, true));

}//end of ProjectFrame::addFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ProjectFrame::addChild
//
// Add child to the currently selected node.
//

public DefaultMutableTreeNode addChild(Object child)
{

    DefaultMutableTreeNode parentNode;
    TreePath parentPath = tree.getSelectionPath();

    if (parentPath == null) {
        parentNode = rootNode;
    }
    else {
        parentNode = (DefaultMutableTreeNode) (parentPath.getLastPathComponent());
    }

    return addObject(parentNode, child, true);

}//end of ProjectFrame::addChild
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ProjectFrame::addObject
//
// Add adds a child object pChild to pParent.
//

public DefaultMutableTreeNode addObject(DefaultMutableTreeNode pParent,
                                                                  Object pChild)
{

    return addObject(pParent, pChild, false);

}//end of ProjectFrame::addObject
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ProjectFrame::addObject
//
// Add adds a child object pChild to pParent and scrolls the view so that
// the child is visible if pMakeVisible is true.
//

public DefaultMutableTreeNode addObject(DefaultMutableTreeNode pParent,
                                           Object pChild, boolean pMakeVisible)
{

    DefaultMutableTreeNode childNode =
                                new DefaultMutableTreeNode(pChild, false);

    if (pParent == null) {pParent = rootNode;}

    //exit if the parent does not allow children -- an exception will be thrown
    //if an attempt is made to add a child to such a node
    //don't use isLeaf -- this gets set even for branch nodes if they have no
    //children

    if (!pParent.getAllowsChildren()) {return(null);}

    // it is key to invoke this on the TreeModel, and NOT DefaultMutableTreeNode
    // so that events will be fired to appropriate listeners
    treeModel.insertNodeInto(childNode, pParent, pParent.getChildCount());

    // scroll down to show the newly added child node
    if (pMakeVisible) {
        tree.scrollPathToVisible(new TreePath(childNode.getPath()));
    }

    return childNode;

}//end of ProjectFrame::addObject
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ProjectFrame::treeNodesChanged
//
// Function from interface TreeModelListener.
//

public void treeNodesChanged(TreeModelEvent e)
{

    DefaultMutableTreeNode node;
    node = (DefaultMutableTreeNode)(e.getTreePath().getLastPathComponent());

    // If the event lists children, then the changed node is the child of the
    // node we've already gotten.  Otherwise, the changed node and the
    // specified node are the same.

    int index = e.getChildIndices()[0];
    node = (DefaultMutableTreeNode)(node.getChildAt(index));

    htmlPane.setText("The user has finished editing the node.");
    htmlPane.setText("New value: " + node.getUserObject());

}//end of ProjectFrame::treeNodesChanged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ProjectFrame::various TreeModelListener functions
//
// Functions from interface TreeModelListener.
//

public void treeNodesInserted(TreeModelEvent e) {}
public void treeNodesRemoved(TreeModelEvent e) {}
public void treeStructureChanged(TreeModelEvent e) {}

//end of ProjectFrame::various TreeModelListener functions
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ProjectFrame::valueChanged
//
// Function from TreeSelectionListener interface.
//

public void valueChanged(TreeSelectionEvent e) {

    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                                           tree.getLastSelectedPathComponent();

    if (node == null) {return;}

    Object nodeInfo = node.getUserObject();

    //only handle non-folder nodes -- ignore the branch (folder) nodes
    //don't use isLeaf -- this gets set even for branch nodes if they have no
    //children

    if (!node.getAllowsChildren()) {
        FileInfo info = (FileInfo)nodeInfo;
        //use this to display a file -- displayURL(info.url);
        htmlPane.setText(info.fullPath);
    }

    if (DEBUG) {System.out.println(nodeInfo.toString());}

}//end of ProjectFrame::valueChanged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ProjectFrame::actionPerformed
//
// Function from ActionListener interface.
//

public void actionPerformed(ActionEvent e) {

    String command = e.getActionCommand();

    if (ADD_NODE_COMMAND.equals(command)) {
        addFile();
    }
    else
    if (REMOVE_NODE_COMMAND.equals(command)) {
        // remove button clicked
        removeSelectedNode();
    }
    else
    if (CLEAR_NODE_COMMAND.equals(command)) {
        //clear button clicked.
        removeNodes();
    }

}//end of ProjectFrame::actionPerformed
//-----------------------------------------------------------------------------

}//end of class ProjectFrame
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
