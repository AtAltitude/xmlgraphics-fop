package org.apache.fop.viewer;


/**
 * Definition f�r die �bersetzer-Klassen.
 *
 * @version 03.12.99
 * @author Stanislav.Gorkhover@af-software.de
 *
 */
public interface Translator {

  /**
   * �bersetzt ein Wort.
   */
  public String getString(String key);
  /**
   * Ein Translator soll die fehlenden keys hervorheben k�nnen.
   */
  public void setMissingEmphasized(boolean b);
  /**
   * Gibt an ob die �bersetzungsquelle gefunden ist.
   */
  public boolean isSourceFound();
  /**
   * Gibt an ob ein Key in der �bersetzungsquelle vorhanden ist.
   */
  public boolean contains(String key);
}