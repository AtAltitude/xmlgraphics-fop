
package org.apache.fop.viewer;

/*
  originally contributed by
  Juergen Verwohlt: Juergen.Verwohlt@af-software.de,
  Rainer Steinkuhle: Rainer.Steinkuhle@af-software.de,
  Stanislav Gorkhover: Stanislav.Gorkhover@af-software.de
 */


import java.awt.*;
import java.awt.print.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import java.beans.*;

import org.apache.fop.apps.AWTCommandLine;
import org.apache.fop.layout.*;
import org.apache.fop.render.awt.*;




/**
 * Frame and User Interface for Preview
 */
public class PreviewDialog extends JFrame implements ProgressListener {

  protected Translator res;

  protected int currentPage = 0;
  protected int pageCount = 0;

  protected AWTRenderer renderer;

  protected IconToolBar toolBar = new IconToolBar();

  protected Command printAction;
  protected Command firstPageAction;
  protected Command previousPageAction;
  protected Command nextPageAction;
  protected Command lastPageAction;





  protected JLabel zoomLabel = new JLabel(); //{public float getAlignmentY() { return 0.0f; }};
  protected JComboBox scale = new JComboBox() {public float getAlignmentY() { return 0.5f; }};

  protected JScrollPane previewArea = new JScrollPane();
  // protected JLabel statusBar = new JLabel();
  protected JPanel statusBar = new JPanel();
  protected GridBagLayout statusBarLayout = new GridBagLayout();

  protected JLabel statisticsStatus = new JLabel();
  protected JLabel processStatus = new JLabel();
  protected JLabel infoStatus = new JLabel();
  protected DocumentPanel docPanel;




  public PreviewDialog(AWTRenderer aRenderer, Translator aRes) {
    res = aRes;
    renderer = aRenderer;

    printAction        = new Command(res.getString("Print"), "Print") { public void doit() {print();}};
    firstPageAction    = new Command(res.getString("First page"),   "firstpg") { public void doit() {goToFirstPage(null);}};
    previousPageAction = new Command(res.getString("Previous page"), "prevpg") { public void doit() {goToPreviousPage(null);}};
    nextPageAction     = new Command(res.getString("Next page"),     "nextpg") { public void doit() {goToNextPage(null);}};
    lastPageAction     = new Command(res.getString("Last page"),     "lastpg") { public void doit() {goToLastPage(null);}};

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    this.setSize(new Dimension(379, 476));
    previewArea.setMinimumSize(new Dimension(50, 50));

    this.setTitle("FOP: AWT-" + res.getString("Preview"));

    scale.addItem("25");
    scale.addItem("50");
    scale.addItem("75");
    scale.addItem("100");
    scale.addItem("150");
    scale.addItem("200");

    scale.setMaximumSize(new Dimension(80, 24));
    scale.setPreferredSize(new Dimension(80, 24));

    scale.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        scale_actionPerformed(e);
      }
    });

    scale.setSelectedItem("100");
    renderer.setScaleFactor(100.0);

    zoomLabel.setText(res.getString("Zoom"));

    this.setJMenuBar(setupMenue());

    this.getContentPane().add(toolBar, BorderLayout.NORTH);

    toolBar.add(printAction);
    toolBar.addSeparator();
    toolBar.add(firstPageAction);
    toolBar.add(previousPageAction);
    toolBar.add(nextPageAction);
    toolBar.add(lastPageAction);
    toolBar.addSeparator();
    toolBar.add(zoomLabel, null);
    toolBar.addSeparator();
    toolBar.add(scale, null);

    this.getContentPane().add(previewArea, BorderLayout.CENTER);
    this.getContentPane().add(statusBar, BorderLayout.SOUTH);


    statisticsStatus.setBorder(BorderFactory.createEtchedBorder());
    processStatus.setBorder(BorderFactory.createEtchedBorder());
    infoStatus.setBorder(BorderFactory.createEtchedBorder());

    statusBar.setLayout(statusBarLayout);

    processStatus.setPreferredSize(new Dimension(200, 21));
    statisticsStatus.setPreferredSize(new Dimension(100, 21));
    infoStatus.setPreferredSize(new Dimension(100, 21));
    processStatus.setMinimumSize(new Dimension(200, 21));
    statisticsStatus.setMinimumSize(new Dimension(100, 21));
    infoStatus.setMinimumSize(new Dimension(100, 21));
    statusBar.add(processStatus, new GridBagConstraints(0, 0, 2, 1, 2.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 5), 0, 0));
    statusBar.add(statisticsStatus, new GridBagConstraints(2, 0, 1, 2, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 5), 0, 0));
    statusBar.add(infoStatus, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));


    docPanel = new DocumentPanel(renderer, this);

    previewArea.setSize(docPanel.getSize());
    previewArea.getViewport().add(docPanel);
  }


  JMenuBar setupMenue() {
    JMenuBar  menuBar;
    JMenuItem menuItem;
    JMenu     menu;
    JMenu     subMenu;

    menuBar = new JMenuBar();
      menu = new JMenu(res.getString("File"));
        subMenu = new JMenu("OutputFormat");
          subMenu.add(new Command("mHTML"));
          subMenu.add(new Command("mPDF"));
          subMenu.add(new Command("mRTF"));
          subMenu.add(new Command("mTEXT"));
        // menu.add(subMenu);
        // menu.addSeparator();
        menu.add(new Command(res.getString("Print")) {public void doit(){print();}});
        menu.addSeparator();
        menu.add(new Command(res.getString("Close")){ public void doit() {dispose();}} );
        menu.addSeparator();
        menu.add(new Command(res.getString("Exit")){ public void doit() {System.exit(0);}} );
      menuBar.add(menu);
      menu = new JMenu(res.getString("View"));
        menu.add(new Command(res.getString("First page")) { public void doit() {goToFirstPage(null);}} );
        menu.add(new Command(res.getString("Previous page")) { public void doit() {goToPreviousPage(null);}} );
        menu.add(new Command(res.getString("Next page")) { public void doit() {goToNextPage(null);}} );
        menu.add(new Command(res.getString("Last page")) { public void doit() {goToLastPage(null);}} );
        menu.addSeparator();
        subMenu = new JMenu(res.getString("Zoom"));
          subMenu.add(new Command("25%") { public void doit() {setScale(25.0);}} );
          subMenu.add(new Command("50%") { public void doit() {setScale(50.0);}} );
          subMenu.add(new Command("75%") { public void doit() {setScale(75.0);}} );
          subMenu.add(new Command("100%") { public void doit() {setScale(100.0);}} );
          subMenu.add(new Command("150%") { public void doit() {setScale(150.0);}} );
          subMenu.add(new Command("200%") { public void doit() {setScale(200.0);}} );
        menu.add(subMenu);
        menu.addSeparator();
        menu.add(new Command(res.getString("Default zoom")) { public void doit() {setScale(100.0);}} );
      menuBar.add(menu);
      menu = new JMenu(res.getString("Help"));
        menu.add(new Command(res.getString("Index")));
        menu.addSeparator();
        menu.add(new Command(res.getString("Introduction")));
        menu.addSeparator();
        menu.add(new Command(res.getString("About")){ public void doit() {startHelpAbout(null);}} );
      menuBar.add(menu);
    return menuBar;
  }

  //Aktion Hilfe | Info durchgef�hrt

  public void startHelpAbout(ActionEvent e) {
    PreviewDialogAboutBox dlg = new PreviewDialogAboutBox(this);
    Dimension dlgSize = dlg.getPreferredSize();
    Dimension frmSize = getSize();
    Point loc = getLocation();
    dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
    dlg.setModal(true);
    dlg.show();
  }

  void goToPage(int number) {
    docPanel.setPageNumber(number);
    repaint();
    previewArea.repaint();
    statisticsStatus.setText(res.getString("Page") + " " + (currentPage + 1) + " " + res.getString("of") + " " +
                             pageCount);
  }

  /**
   * Shows the previous page.
   */
  void goToPreviousPage(ActionEvent e) {
    if (currentPage <= 0)
      return;
    currentPage--;
    goToPage(currentPage);
  }


  /**
   * Shows the next page.
   */
  void goToNextPage(ActionEvent e) {
    if (currentPage >= pageCount - 1)
      return;
    currentPage++;
    goToPage(currentPage);
  }

  /**
   * Shows the last page.
   */
  void goToLastPage(ActionEvent e) {

    if (currentPage == pageCount - 1) return;
    currentPage = pageCount - 1;

    goToPage(currentPage);
  }

  /**
   * Shows the first page.
   */
  void goToFirstPage(ActionEvent e) {
    if (currentPage == 0)
      return;
    currentPage = 0;
    goToPage(currentPage);
  }

  void print() {

    PrinterJob pj = PrinterJob.getPrinterJob();
    // Nicht n�tig, Pageable get a Printable.
    // pj.setPrintable(renderer);
    pj.setPageable(renderer);

    if (pj.printDialog()) {
      try {
        pj.print();
      } catch(PrinterException pe) {
        pe.printStackTrace();
      }
    }
  }

  public void setScale(double scaleFactor) {

    if (scaleFactor == 25.0)
      scale.setSelectedIndex(0);
    else if (scaleFactor == 50.0)
      scale.setSelectedIndex(1);
    else if (scaleFactor == 75.0)
      scale.setSelectedIndex(2);
    else if (scaleFactor == 100.0)
      scale.setSelectedIndex(3);
    else if (scaleFactor == 150.0)
      scale.setSelectedIndex(4);
    else if (scaleFactor == 200.0)
      scale.setSelectedIndex(5);

    renderer.setScaleFactor(scaleFactor);
    previewArea.invalidate();
    previewArea.repaint();
  }

  void scale_actionPerformed(ActionEvent e) {
    setScale(new Double((String)scale.getSelectedItem()).doubleValue());
  }


  public void setPageCount(int aPageCount) {
    pageCount = aPageCount;
    statisticsStatus.setText(res.getString("Page") + " " + (currentPage + 1) +
                             " " + res.getString("of") + " " + pageCount);
  }


  public void progress(int percentage) {
    processStatus.setText(percentage + "%");
  }

  public void progress(int percentage, String message) {
    processStatus.setText(message + " " + percentage + "%");
  }

  public void progress(String message) {
    processStatus.setText(message);
  }


}  // class PreviewDialog


