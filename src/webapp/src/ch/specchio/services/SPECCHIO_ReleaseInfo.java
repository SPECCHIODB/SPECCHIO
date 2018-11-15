/* Created by JReleaseInfo AntTask from Open Source Competence Group */
/* Creation date Tue Jul 10 16:59:46 CEST 2018 */
package ch.specchio.services;

import java.util.Date;
/*
 * $Id: JReleaseInfoViewer.txt,v 1.8 2005/08/06 15:06:40 tcotting Exp $
 */
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;

import java.lang.reflect.Method;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;


/**
 * A simple application which uses the generated JReleaseInfo file as
 * argument. It creates a table with the properties in the JReleaseInfo
 * file. Properties are read with introspection.<br />
 * You can add this viewer to your java library jar-file, setting
 * the Main-Class Manifest-Attribute to this viewer and providing
 * a "double-click view" of relevant build-data.
 *
 * @author Thomas Cotting, Open Source Competence Group, www.oscg.ch
 */
class JReleaseInfoSwingViewer extends JFrame {

   /** ContentPanel. */
   private JPanel pnlContent = null;

   /** ScrollPanel holding table. */
   private JScrollPane scrlPane = null;

   /** Table showing properties. */
   private JTable tblProps = null;

   /** Status line. */
   private JLabel lblStatus = null;

   /** Projectname in title */
   private String project = null;

   /** Version in title */
   private String version = null;

   /** Map of properties */
   private Map props = null;

   /**
    * Constructor.
    * @param c Class 
    */
   public JReleaseInfoSwingViewer(String project, String version, Map props) {
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      this.project = project;
      this.version = version;
      this.props   = props;
      initialize();
   }
   
   /**
    * Center the frame in the middle of the screen.
    * @param frame of window
    */
   private void centerFrame(JFrame frame) {
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      Dimension frameSize = frame.getSize();
      frame.setLocation((screenSize.width / 2) - (frameSize.width / 2), (screenSize.height / 2) - (frameSize.height / 2));
   }

   /**
    * This method initializes the frame.
    *
    * @return void
    */
   private void initialize() {
      setSize(500, 200);
      centerFrame(this);

      scrlPane = new JScrollPane();

      tblProps = getTable();
      scrlPane.setViewportView(tblProps);

      // Status Bar
      lblStatus = new JLabel();
      lblStatus.setFont(new Font("SansSerif", Font.PLAIN, 11));
      lblStatus.setText("JReleaseInfo Viewer by Open Source Competence Group, www.oscg.ch");

      // Frame
      pnlContent = new JPanel();
      pnlContent.setLayout(new BorderLayout());
      pnlContent.add(scrlPane, BorderLayout.CENTER);
      pnlContent.add(lblStatus, BorderLayout.SOUTH);

      this.setContentPane(pnlContent);

      // Prepare the title       
      String title = "Release Information";
      if (project != null) {
         String prefix = project;
         if (version != null) {
            prefix = prefix + " " + version;
         }

         this.setTitle(prefix + " - " + title);
      } else {
         this.setTitle(title);
      }
   }

   /**
    * Create a table with two columns.
    * @return table
    */
   private JTable getTable() {
      JTable tbl = null;
      Vector cols = new Vector();
      cols.add("Property");
      cols.add("Value");
      Vector rows = new Vector();

      try {
         rows.addAll(props.values());

         tbl = new JTable(rows, cols);
         tbl.getColumn("Property").setPreferredWidth(150);
         tbl.getColumn("Value").setPreferredWidth(350);
         tbl.setEnabled(false);
      } catch (Exception e) {
         e.printStackTrace();
      }

      return tbl;
   }

   /**
    * Process Window close event.
    * @param e WindowEvent
    */
   protected void processWindowEvent(WindowEvent e) {
      super.processWindowEvent(e);
      if (e.getID() == WindowEvent.WINDOW_CLOSING) {
         System.exit(0);
      }
   }

}

class JReleaseInfoTextViewer {
   /** Projectname in title */
   private String project = null;

   /** Version in title */
   private String version = null;

   /** Map of properties */
   private Map props = null;

   /**
    * Constructor.
    * @param c Class 
    */
   public JReleaseInfoTextViewer(String project, String version, Map props ) {
      this.project = project;
      this.version = version;
      this.props   = props;
   }

   /**
    * This method prints the information.
    *
    * @return void
    */
   public void print() {
      String spaces = "                                        ";
      // Prepare the title       
      System.out.println("-------------------------------------------------------------");
      String title = "Release Information";
      if (project != null) {
         String prefix = project;
         if (version != null) {
            prefix = prefix + " " + version;
         }
         System.out.println(prefix + " - " + title);
      } 
      else {
         System.out.println(title);
      }
      System.out.println("-------------------------------------------------------------");
      printTable("Property", "Value", spaces);
      System.out.println("-------------------------------------------------------------");
      
      try {
         Iterator it = props.keySet().iterator();
         while (it.hasNext()) {
            String key = (String) it.next();
            Vector v = (Vector)props.get(key);
            printTable(key, v.get(1).toString(), spaces);
         }
	     System.out.println("-------------------------------------------------------------");
      } catch (Exception e) {
         e.printStackTrace();
      }
      
   }

   private void printTable(String key, String props, String spaces) {
      StringBuffer buf = new StringBuffer();
      buf.append(key);
      int numSpaces = 20 - key.length();
      if (numSpaces > 0) {
         buf.append(spaces.substring(0, numSpaces));
      }
      else {
         buf.append(" ");
      }
      buf.append(props);
      System.out.println(buf.toString());
   }
}

class JReleaseInfoViewer {
   /** JReleaseInfo class for introspection. */
   private Class c = null;

   /** Projectname in title */
   private String project = null;

   /** Version in title */
   private String version = null;

   /**
    * Constructor.
    * @param c Class 
    */
   public JReleaseInfoViewer(Class c) {
      this.c = c;
   }

   /**
    * Prepare the properties.
    * @return table
    * @throws IllegalAccessException
    * @throws Exception
    */
   private Map getSortedProps() throws Exception{
      Object obj = c.newInstance();
      Method[] methods = c.getDeclaredMethods();

      // Feature request from rgisler 20040430
      Map sortedProps = new TreeMap();

      for (int i = 0; i < methods.length; i++) {
         Vector v = new Vector();
         Method method = methods[i];
         String methodName = method.getName();

         // Feature request from rgisler 20040430
         if (methodName.equalsIgnoreCase("getProject")) {
            Object objP = method.invoke(obj, (Object) null);
            if (objP instanceof String) {
               project = (String)objP;
            }
         }

         if (methodName.equalsIgnoreCase("getVersion")) {
            Object objV = method.invoke(obj, (Object) null);
            if (objV instanceof String) {
               version = (String)objV;
            } else {
               version = objV.toString();
            }
         }

         if (methodName.startsWith("get")) {
            String field = methodName.substring(3);
            v.add(field);
            v.add(method.invoke(obj, (Object) null));
            sortedProps.put(field, v);
         } else if (methodName.startsWith("is")) {
            String field = methodName.substring(2);
            v.add(field);
            v.add(method.invoke(obj, (Object) null));
            sortedProps.put(field, v);
         }
      }
      return sortedProps;
   }

   /**
    * Show the info in a console or window.
    */
   public void showInfo() {
      try {
         JReleaseInfoSwingViewer swingViewer = new JReleaseInfoSwingViewer(project, version, getSortedProps());
         swingViewer.setVisible(true);
      }
      catch (Exception ex) {
         try {
            JReleaseInfoTextViewer textViewer = new JReleaseInfoTextViewer(project, version, getSortedProps());
            textViewer.print();
         } catch (Exception e) {
            System.out.println(e.getMessage());
         }
      }
   }

   /**
    * Show the info in a console.
    */
   public void showText() {
     try {
        JReleaseInfoTextViewer textViewer = new JReleaseInfoTextViewer(project, version, getSortedProps());
        textViewer.print();
     } catch (Exception e) {
        System.out.println(e.getMessage());
     }
   }
}

/**
 * This class provides information gathered from the build environment.
 * 
 * @author JReleaseInfo AntTask
 */
public class SPECCHIO_ReleaseInfo {

   /**
    * Get buildDate (set during build process to Tue Sep 18 15:27:15 CEST 2018).
    * @return Date buildDate
    */
   public static final Date getBuildDate() { return BuildConfig.BUILD_DATE; }


   /**
    * Get project (set during build process to "SPECCHIO Spectral Information System").
    * @return String project
    */
   public static final String getProject() { return BuildConfig.DESCRIPTION; }


   /**
    * Get copyright (set during build process to "${copyright}").
    * @return String copyright
    */
   public static final String getCopyright() { return BuildConfig.COPYRIGHT; }

   /**
    * Get mail (set during build process to "${mail}").
    * @return String mail
    */
   public static final String getMail() { return BuildConfig.MAIL; }
   /**
    * Get version (set during build process to "3.3.0.1").
    * @return String version
    */
   public static final String getVersion() { return BuildConfig.VERSION; }

   /**
    * Get company (set during build process to "${company}").
    * @return String company
    */
   public static final String getCompany() { return BuildConfig.COMPANY; }


   /**
    * Get buildNumber (set during build process to 376).
    * @return int buildNumber
    */
   public static final int getBuildNumber() { return 376; }


   /**
    * Get home (set during build process to "http://specchio.ch").
    * @return String home
    */
   public static final String getHome() { return BuildConfig.WEB; }
}
