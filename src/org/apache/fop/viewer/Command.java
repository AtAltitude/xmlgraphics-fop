package org.apache.fop.viewer;
/*
  originally contributed by
  Juergen Verwohlt: Juergen.Verwohlt@af-software.de,
  Rainer Steinkuhle: Rainer.Steinkuhle@af-software.de,
  Stanislav Gorkhover: Stanislav.Gorkhover@af-software.de
 */

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import java.io.File;


/**
 * Klasse f�r UI-Kommandos. Die Kommandos k�nnen in das Men�system oder
 * in eine Toolbar eingef�gt werden.<br>
 * <code>Commands</code> unterst�tzen mehrsprachigkeit.<br>
 * Durch �berschreiben der Methode <code>doit<code> kann die Klasse customisiert werden.
 * �ber die Methode <code>undoit</code> kann Undo-Funktionalit�t unterst�tzt werden.<br>
 *
 * @author Juergen.Verwohlt@af-software.de
 * @version 1.0 18.03.99
 */
public class Command extends AbstractAction {

  public static String IMAGE_DIR = "../viewer/images/";

  public Command(String name) {
    this(name, (ImageIcon)null);
  }

  public Command(String name, ImageIcon anIcon) {
    super(name, anIcon);
  }


  public Command(String name, ImageIcon anIcon, String path) {
    this(name, anIcon);
    File f = new File (IMAGE_DIR + path + ".gif");
    if (!f.exists()) {
      System.err.println("Icon not found: " + f.getAbsolutePath());
    }

  }

  public Command(String name, String iconName) {
    this(name, new ImageIcon(IMAGE_DIR + iconName + ".gif"), iconName);
  }

  public void actionPerformed(ActionEvent e) {
    doit();
  }

  public void doit() {
    System.err.println("Not implemented.");
  }

  public void undoit() {
    System.err.println("Not implemented.");
  }
}
