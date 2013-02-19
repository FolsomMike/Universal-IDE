/******************************************************************************
* Title: TI DSP Dev Tool - TabDecorator.java File
* Author: Mike Schoonover
* Date: 06/26/11
*
* Purpose:
*
* This class handles a label and a button for use as a decorated tab.
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
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class TabDecorator
//
// Component for use as a tab button in a JTabbedPane.
//
// Displays a JLabel for the tab's title and a JButton with an 'x' icon to
// provide a method to close the tab.
//

public class TabDecorator extends JPanel {

    private final JTabbedPane pane;

    //create a mouse listener and override some of its methods
    private final static MouseListener
                                buttonMouseListener = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(true);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(false);
            }
        }
    };

//-----------------------------------------------------------------------------
// TabDecorator::TabDecorator (constructor)
//

public TabDecorator(final JTabbedPane pPane) {

    //unset default FlowLayout' gaps
    super(new FlowLayout(FlowLayout.LEFT, 0, 0));

    if (pPane == null) {
        throw new NullPointerException("TabbedPane is null");
    }

    pane = pPane;
    setOpaque(false);

    //make JLabel read titles from JTabbedPane
    JLabel label;

    label = new JLabel() {

        //this is a weird way of overriding a method of a class?
        //didn't know this was possible -- further study required!

         @Override
         public String getText() {
             int i = pane.indexOfTabComponent(TabDecorator.this);
             if (i != -1) {
                 return pane.getTitleAt(i);
             }
             return null;
         }
     };

    add(label);
    //add more space between the label and the button
    label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
    //tab button to display the 'x' which can be clicked on to close the tab
    TabButton button = new TabButton();
    button.init();
    add(button);
    //add more space to the top of the component
    setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

}// end of TabDecorator::TabDecorator (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// inner class TabButton
//
// This is the button which displays an 'x' which the user can click on to
// close the tab.
//

private class TabButton extends JButton implements ActionListener {

//-----------------------------------------------------------------------------
// TabButton::TabButton (constructor)
//

public TabButton() {

}// end of TabButton::TabButton (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TabButton::init
//
// Initializes new objects. Should be called immediately after instantiation.
//

public void init() {

    int size = 17;
    setPreferredSize(new Dimension(size, size));
    setToolTipText("close this tab");
    //Make the button looks the same for all Laf's
    setUI(new BasicButtonUI());
    //Make it transparent
    setContentAreaFilled(false);
    //No need to be focusable
    setFocusable(false);
    setBorder(BorderFactory.createEtchedBorder());
    setBorderPainted(false);
    //Making nice rollover effect
    //we use the same listener for all buttons
    addMouseListener(buttonMouseListener);
    setRolloverEnabled(true);
    //Close the proper tab by clicking the button
    addActionListener(this);

}// end of TabButton::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TabButton::actionPerformed
//
// Handles the user clicking on the button (the 'x').
//

public void actionPerformed(ActionEvent e) {

    //get index of tab decorated with this button
    int i = pane.indexOfTabComponent(TabDecorator.this);

    if (i == -1) {return;} //bail out if tab not found for some reason

    //get access to the component handling the document for this tab
    EditorRig rig = (EditorRig)pane.getComponentAt(i);

    //all the rig to clean up, save the document etc.
    if (rig.prepareToClose()) {pane.remove(i);}

}// end of TabButton::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TabButton::updateUI
//
// We don't want to update UI for this button.
//

@Override
public void updateUI() {

}// end of TabButton::updateUI
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TabButton::paintComponent
//
// Paints a custom icon for the button.
//

@Override
protected void paintComponent(Graphics g) {

    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g.create();

    //paint the 'x'

    //shift the image for pressed buttons
    if (getModel().isPressed()) {
        g2.translate(1, 1);
    }

    g2.setStroke(new BasicStroke(2));
    g2.setColor(Color.BLACK);

    if (getModel().isRollover()) {
        g2.setColor(Color.MAGENTA);
    }

    int delta = 6;
    g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
    g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
    g2.dispose();

}// end of TabButton::paintComponent
//-----------------------------------------------------------------------------

}//end of inner class TabButton
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

}//end of class TabDecorator
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
