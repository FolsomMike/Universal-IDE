/******************************************************************************
* Title: Universal IDE - TMS320VC5441CodeHandler.java
* Author: Mike Schoonover
* Date: 2/19/13
*
* Purpose:
*
* This class manages software source code processing tasks such as
* assembling, linking, listing, debugging, and simulating for a Texas
* Instrument TMS320VC5441 Quad Core DSP.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package codehandler;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuBar;

//-----------------------------------------------------------------------------
// class TMS320VC5441CodeHandler
//

public class TMS320VC5441CodeHandler extends CodeHandler
{

    String assembleBatchFilename;
    String assembleResultsFilename;
    String copyHexFileBatchFilename;
    String copyHexFileResultsFilename;
    
//-----------------------------------------------------------------------------
// TMS320VC5441CodeHandler::TMS320VC5441CodeHandler (constructor)
//
// Creates a basic code handler with common features used for handling all
// types of code.
//

public TMS320VC5441CodeHandler(JMenuBar pMenuBar, String pProjectPath,
                                                ActionListener pActionListener)
{

    super(pMenuBar, pProjectPath, pActionListener);

}//end of TMS320VC5441CodeHandler::TMS320VC5441CodeHandler (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TMS320VC5441CodeHandler::init
//
// Initializes new objects. Should be called immediately after instantiation.
//

@Override
public void init()
{

    super.init(); //call parent class init first

    //wip mks -- read these from the project file? allow user to set them?
    //the batch files should be in the project folder
    
    assembleBatchFilename = "aa Assemble Capulin UT DSP.bat";
    assembleResultsFilename = "results.txt";

    copyHexFileBatchFilename = "ab Copy Hex File to Chart Program.bat";
    copyHexFileResultsFilename = "ab Copy Hex File Results.txt";
    
}//end of TMS320VC5441CodeHandler::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TMS320VC5441CodeHandler::actionPerformed
//
// Responds to button events, menu events, etc.
//

@Override
public void actionPerformed(ActionEvent e)
{

    //catch any events handled by this sub-class first
    //if the parent class has any duplicate catches, returning here in the
    //if-then block below will prevent the parent class from acting on them
    //if it is desirous for the parent class to also act on a duplicate, then
    //don't return in the if-then block

    //example event catch
    if ("yada yada yada".equals(e.getActionCommand())) {
        //call some function
        return;
    }

    //pass action onto parent class so it process as well
    super.actionPerformed(e);

}//end of TMS320VC5441CodeHandler::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TMS320VC5441CodeHandler::assembleProject
//
// Assembles the source code and displays the status results page.
//

@Override
public void assembleProject()
{

    runBatchFileAndDisplayResults(
                               assembleResultsFilename, assembleBatchFilename);
    
}//end of TMS320VC5441CodeHandler::assembleProject
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TMS320VC5441CodeHandler::copyHexFileToTargetFolder
//
// Assembles the source code and displays the status results page.
//

@Override
public void copyHexFileToTargetFolder()
{

    runBatchFileAndDisplayResults(
                         copyHexFileResultsFilename, copyHexFileBatchFilename);
    
}//end of TMS320VC5441CodeHandler::copyHexFileToTargetFolder
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TMS320VC5441CodeHandler::runBatchFileAndDisplayResults
//
// Deletes any existing results file named pResultsFilename, executes batch
// file named pBatchFilename, and loads the new results file.
//

public void runBatchFileAndDisplayResults(String pResultsFilename,
                                                        String pBatchFilename)
{

    String resultsFullPath =
                    projectPath + File.separator + pResultsFilename;

    File resultsFile = new File(resultsFullPath);

    //attempt to delete the file holding the previous resemble results; bail
    //out if this cannot be done -- user must fix the issue first -- note that
    //this will delete the file even if it is set read-only
    if (resultsFile.exists() && !resultsFile.delete()){
        CodeHandler.errorMsg(
             "Cannot delete " + pResultsFilename + ". Operation aborted.");
        return;
    }

    //run the batch file to perform the assembly
    runProjectBatchFile(pBatchFilename);

    //alert user if the assembly process did not create a results file
    if(!resultsFile.exists()){
        CodeHandler.errorMsg(
                    "No results file was created, so it cannot be displayed.");
        return;
    }

    //tell the listening object to load and display the results file
    actionListener.actionPerformed(new ActionEvent(this,
            ActionEvent.ACTION_PERFORMED, "Load file: " + resultsFullPath));

}//end of TMS320VC5441CodeHandler::runBatchFileAndDisplayResults
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TMS320VC5441CodeHandler::runProjectBatchFile
//
// Runs the batch file pBatchFilename.
//
// Reference:
//
// use Runtime.getRuntime().exec("cmd /c start build.bat"); to display the
// process in a window
//
// use Runtime.getRuntime().exec("cmd /c build.bat"); to not display the
// process
//
// Use to wait for process to finish:
//
// try{
//    Process p = Runtime.getRuntime().exec("cmd /c build.bat");
//      p.waitFor();
// }catch( IOException ex ){
//  Validate the case the file can't be accesed (not enough permissions)
// }catch( InterruptedException ex ){
//  Validate the case the process is being stopped by some external situation
// }
//
// Use to get output from the process (we send that to a file and display it
//  afterwards instead):
//
// Runtime runtime = Runtime.getRuntime();
// try {
//    Process p1 = runtime.exec("cmd /c start D:\\temp\\a.bat");
//    InputStream is = p1.getInputStream();
//    int i = 0;
//    while( (i = is.read() ) != -1) {
//        System.out.print((char)i);
//    }
//  } catch(IOException ioException) {
//    System.out.println(ioException.getMessage() );
//  }
//

private void runProjectBatchFile(String pBatchFilename)
{

    try {

        Runtime rt = Runtime.getRuntime();

        //the entire command has to be in quotes to use the && which allows
        //multiple commands (&& only executes the second command if the first
        // one succeeded, & executes both regardless)
        //this command changes directory to the project and executes the
        //batch file; the path and batch file need quotes due to spaces, so
        //there are quotes inside of quotes, but that works

        String command = "cmd /c \"cd \"" + projectPath + "\"";
        command += " && \"" + pBatchFilename + "\"\"";

        //execute the batch file and wait for it to finish to make sure there
        //is time for the result file to be created (if there is one)
        Process p = rt.exec(command);
        p.waitFor();

    }
    catch( InterruptedException ex ){
        //validate the case the process is being stopped by external situation
    }
    catch (IOException ex) {
        Logger.getLogger(
           TMS320VC5441CodeHandler.class.getName()).log(Level.SEVERE, null, ex);
    }

}//end of TMS320VC5441CodeHandler::runProjectBatchFile
//-----------------------------------------------------------------------------

}//end of class TMS320VC5441CodeHandler
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
