package ch.specchio.types;

import java.io.File;

import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;

public class CustomFileView extends FileView {

	  public String getName(File f) { return null; }
	  public String getDescription(File f) { return null; }
	  public String getTypeDescription(File f) { return null; }

	  public Icon getIcon(File f) {
	    Icon icon = null;

	    if(f.isDirectory()) icon = FileSystemView.getFileSystemView().getSystemIcon(f);
	    else           icon = FileSystemView.getFileSystemView().getSystemIcon(f);

	    return icon;
	  }
	  
	  public Boolean isTraversable(File f) {
	    Boolean b = null;

	    if(f.isDirectory())
	    	b = new Boolean(true);
	    else
	    	b = new Boolean(false);

	    
	    return b;
	  }
	  
	  
//	  private boolean isImage(File f) {
//	    String suffix = getSuffix(f);
//	    boolean isImage = false;
//
//	    if(suffix != null) {
//	      isImage = suffix.equals("gif") || 
//	            suffix.equals("bmp") ||
//	              suffix.equals("jpg");
//	    }
//	    return isImage;
//	  }
	  
	  
//	  private String getSuffix(File file) {
//	    String filestr = file.getPath(), suffix = null;
//	    int i = filestr.lastIndexOf('.');
//
//	    if(i > 0 && i < filestr.length()) {
//	      suffix = filestr.substring(i+1).toLowerCase();  
//	    }
//	    return suffix;
//	  }
	}