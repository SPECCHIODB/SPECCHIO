package ch.specchio.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileSystemView;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;

import ch.specchio.client.SPECCHIOPreferencesStore;
import ch.specchio.file.reader.campaign.VirtualFile;
import ch.specchio.file.reader.campaign.VirtualFileSystemView;
import ch.specchio.types.Campaign;
import ch.specchio.types.CustomFileView;
import ch.specchio.types.DropboxPath;



/**
 * Panel for selecting and adding paths to a campaign.
 */
public class CampaignPathPanel extends JPanel implements ActionListener, ListSelectionListener {
	
	/** serialisation version identifier */
	private static final long serialVersionUID = 1L;
	
	/** the component that owns this panel */
	private Component owner;
	
	/** show only paths that exist on the local machine */
	private boolean local;
	
	/** path selection list box */
	private JList pathListField;
	
	/** path selection list model */
	private DefaultListModel pathListModel;
	
	/** "add path" button */
	private JButton addPathButton;
	private JButton addDropboxPathButton;
	
	/** "remove path" button */
	private JButton removePathButton;
	
	/** the list of list data listeners */
	private List<ListDataListener> listeners;
	
	/** text for the "add path" button */
	private static final String ADD_PATH = "New path";
	
	private static final String ADD_DROPBOX_PATH = "New Dropbox path";
	
	/** text for the "remove path" button */
	private static final String REMOVE_PATH = "Remove path";
	
	
	/**
	 * Constructor.
	 * 
	 * @param ownerIn		the component that owns this panel
	 * @param localIn		list only the paths that exist on the local machine
	 * @param allowRemove	allow paths to be removed
	 */
	public CampaignPathPanel(Component ownerIn, boolean localIn, boolean allowRemove) {
		
		super();
		
		// save input parameters for later
		owner = ownerIn;
		local = localIn;
		
		// initialise member variables
		listeners = new LinkedList<ListDataListener>();
		
		// set up horizontal box layout
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		// add the path selection list
		pathListModel = new DefaultListModel();
		pathListField = new JList(pathListModel);
		FilePathRenderer fpr = new FilePathRenderer();
		pathListField.setCellRenderer(fpr);
		pathListField.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		pathListField.addListSelectionListener(this);
		JScrollPane pathListScrollPane = new JScrollPane(pathListField);
		add(pathListScrollPane);
		
		// create a panel for the buttons
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
		add(buttonPanel);
		
		// add the "add path" button
		if(local)
		{
			addPathButton = new JButton(ADD_PATH);
			addPathButton.setActionCommand(ADD_PATH);
			addPathButton.addActionListener(this);
			buttonPanel.add(addPathButton);
			
			// add and enable dropbox button if there are dropbox accounts linked for this user
			SPECCHIOPreferencesStore prefs_store;
			try {
				prefs_store = new SPECCHIOPreferencesStore();
				
				Preferences prefs = prefs_store.getPreferences();

				Preferences node = prefs.node("Dropbox");	
				
				if (node != null)
				{
					String[] keys = node.keys();
					
					if(node.keys().length > 0)
					{
						addDropboxPathButton = new JButton(ADD_DROPBOX_PATH);
						addDropboxPathButton.setActionCommand(ADD_DROPBOX_PATH);
						addDropboxPathButton.addActionListener(this);
						buttonPanel.add(addDropboxPathButton);
					}

				}
				
			} catch (BackingStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		
			
		}
		
		if (allowRemove) {
			// add the "remove path" button
			removePathButton = new JButton(REMOVE_PATH);
			removePathButton.setActionCommand(REMOVE_PATH);
			removePathButton.addActionListener(this);
			buttonPanel.add(removePathButton);
		}
		
	}
	
	
	/**
	 * Button handler.
	 * 
	 * @param event	the event to be handled
	 */
	public void actionPerformed(ActionEvent event) {
		
		if (ADD_PATH.equals(event.getActionCommand())) {
			
			// create a file chooser
			JFileChooser fc;
			File base = getSelectedPath();
			if (base != null) {
				fc = new JFileChooser(getSelectedPath().getParent());
			} else {
				fc = new JFileChooser();
			}
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (fc.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION) {
				
				// add the new path to the end of the list and select it
				File file = fc.getSelectedFile();
				pathListModel.addElement(file);
				pathListField.setSelectedValue(file, true);
				
				// notify the listeners of a change
				fireListDataEvent(ListDataEvent.INTERVAL_ADDED, pathListField.getSelectedIndex(), pathListField.getSelectedIndex());
				
			}
			
		} else if (REMOVE_PATH.equals(event.getActionCommand())) {
			
			int index = pathListField.getSelectedIndex();
			if (index >= 0) {
				
				// remove the selected element from the list and remove selection
				pathListModel.removeElementAt(index);
				pathListField.clearSelection();
				
				// notify the listeners of a change
				fireListDataEvent(ListDataEvent.INTERVAL_REMOVED, index, index);
				
			}
			
		} else if (ADD_DROPBOX_PATH.equals(event.getActionCommand())) {
			
			SPECCHIOPreferencesStore prefs_store;
			DbxClientV2 client;
			
			try {
				prefs_store = new SPECCHIOPreferencesStore();
	
				Preferences prefs = prefs_store.getPreferences();	
				Preferences node = prefs.node("Dropbox");  
				
				String[] accounts = node.keys();
				int account_index = 0; // default
				
				if(accounts.length > 1)
				{
					// user needs to select which dropbox account to use
					
					
					// TBD
					
				}
				else
				{
					// only single account: no need to ask user which one to use					
					account_index = 0;										
				}
				
	    		  String ACCESS_TOKEN = node.get(accounts[account_index], "");
	    		  
	    		  // Create Dropbox client
	    		  DbxRequestConfig config = DbxRequestConfig.newBuilder("specchio_dropbox_access").build();
	    		  client = new DbxClientV2(config, ACCESS_TOKEN);  

	    		  JFileChooser fc = new JFileChooser(new VirtualFileSystemView(new DropboxPath("/Dropbox", true), client));
	    		  fc.setFileView(new CustomFileView());
	    		  fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    		  int returnVal = fc.showOpenDialog(SPECCHIOApplication.getInstance().get_frame());

	    		  boolean approved = returnVal == JFileChooser.APPROVE_OPTION;


	    		  if (approved) {
	    			  File file = fc.getSelectedFile();	    
	    			  Path path = file.toPath();
	    			  System.out.println("Folder: " + "/Dropbox/"+ file.getName());    
	    			  
	    			  // add the new path to the end of the list and select it
	    			  pathListModel.addElement(file);
	    			  pathListField.setSelectedValue(file, true);

	    			  // notify the listeners of a change
	    			  fireListDataEvent(ListDataEvent.INTERVAL_ADDED, pathListField.getSelectedIndex(), pathListField.getSelectedIndex());

	    		  } else {
	    			  System.out.println("Open command cancelled by user.");
	    		  }

	    		 	    		  

	
			int x = 1;
			
			
			
			} catch (BackingStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		
		}
		
		
		
	}
	
	
	/**
	 * Register for notifications when the list data changes.
	 * 
	 * @param listener	the listener
	 */
	public void addListDataListener(ListDataListener listener) {
		
		listeners.add(listener);
		
	}
	
	
	/**
	 * Notify all listeners of a change in the contents of the group.
	 * 
	 * @param typ		ListDataEvent.CONTENTS_CHANGED, LisDataEvent.INTERVAL_ADDED or ListDataEvent.INTERVAL_REMOVED
	 * @param index0	the index of the first inserted element
	 * @param index1	the index of the last inserted element
	 */
	private void fireListDataEvent(int type, int index0, int index1) {
		
		ListDataEvent event = new ListDataEvent(this, type, index0, index1);
		for (ListDataListener listener : listeners) {
			if (type == ListDataEvent.CONTENTS_CHANGED) {
				listener.contentsChanged(event);
			} else if (type == ListDataEvent.INTERVAL_ADDED) {
				listener.intervalAdded(event);
			} else if (type == ListDataEvent.INTERVAL_REMOVED) {
				listener.intervalRemoved(event);
			}
		}
		
	}
	
	
	/**
	 * Get all of the paths listed on the panel.
	 * 
	 * @return an array containing all of the paths
	 */
	public File[] getListedPaths() {
		
		File paths[] = new File[pathListModel.getSize()];
		for (int i = 0; i < pathListModel.getSize(); i++) {
			paths[i] = (File)pathListModel.getElementAt(i);
		}
		
		return paths;
		
	}
	
	
	/**
	 * Get the selected path.
	 * 
	 * @return the selected path
	 */
	public File getSelectedPath() {
		
		return (File)pathListField.getSelectedValue();
		
	}
	
	
	/**
	 * Set the campaign whose paths will be displayed by this panel.
	 *
	 * @param c	the campaign
	 */
	public void setCampaign(Campaign c) {
		
		// clear the existing list
		pathListModel.clear();
		pathListField.clearSelection();
		
		if (c != null) {
			// list all paths from the campaign that exist on the local computer
			String fileSep = System.getProperty("file.separator");
			int i = 0;
			for (String path : c.getKnownPaths()) {
				
				if(fileSep.equals("/") && path.startsWith("\\"))
				{
					// fudge this for Unix style paths; for some reasons in a mixed environment it happened that the forward slashes were converted to backward slashes! (OPTIMISE SWAMP summer school)
					path = path.replace("\\", fileSep);				
				}
				
				File file = new File(path);
				if (local)
				{
					if (file.exists() && file.isDirectory()) {
						pathListModel.addElement(file);
						if (path.equals(c.getPath())) {
							// make this path the default selection
							pathListField.setSelectedIndex(i);
						}
						i++;
					}
				}
				else // show remote folders (not on this machine)
				{
					if (!(file.exists() && file.isDirectory())) {
						pathListModel.addElement(file);
//						if (path.equals(c.getPath())) {
//							// make this path the default selection
//							//pathListField.setSelectedIndex(i);
//						}
						i++;
					}					
					
					
				}
			}
		}
		
	}
	
	
	/**
	 * Enable or disable the controls on the panel.
	 * 
	 * @param enabled	true or false
	 */
	public void setEnabled(boolean enabled) {
		
		pathListField.setEnabled(enabled);
		if (addPathButton != null) {
			addPathButton.setEnabled(enabled);
		}
		if (removePathButton != null) {
			removePathButton.setEnabled(enabled && pathListField.getSelectedValue() != null);
		}
		
	}
	
	
	/**
	 * List selection handler.
	 * 
	 * @param event	the event to be handled
	 */
	public void valueChanged(ListSelectionEvent event) {
		
		if (removePathButton != null) {
			// enable the "remove path" button if a selection was made
			removePathButton.setEnabled(pathListField.getSelectedValue() != null);
		}
		
	}
	
	public class FilePathRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = 1L;
		/** Creates a new instance of FielPathRenderer */   public FilePathRenderer(){   } 
		public Component getListCellRendererComponent(JList list,
				Object value, int index, boolean isSelected, 
				boolean cellHasFocus) { 
			super.getListCellRendererComponent(list, value,index,isSelected,cellHasFocus);  
			
			if(value instanceof VirtualFile)
			{
				setText("Dropbox (Full implementation pending ...): " + value.toString());
			}			
			
			return this;   } }
	

}


