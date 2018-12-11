package ch.specchio.gui;


import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.types.Campaign;
import ch.specchio.types.campaign_node;
import ch.specchio.types.database_node;
import ch.specchio.types.hierarchy_node;
import ch.specchio.types.spectral_node_object;
import ch.specchio.types.spectrum_node;

public class SpectralDataBrowser extends JScrollPane implements ActionListener, TreeWillExpandListener, TreeExpansionListener
{
	private static final long serialVersionUID = 1L;
	public SpectralJTree tree;
	public SpectralDataBrowserNode root;
	private int directly_select_number_of_spectra = 0;
	
	boolean restrict_to_view; // restrict shown nodes to views (i.e. user sees only own data)
	
	public JComboBox order_by_box;
	
	String order_by = "Acquisition Time";
	private JPanel panel;
	JScrollPane tree_scroll_pane;
	
	private SPECCHIOClient specchio_client;
	
	public SpectralDataBrowser(SPECCHIOClient specchio_client, boolean restrict_to_view)
	{
		this.specchio_client = specchio_client;
		this.restrict_to_view = restrict_to_view;
		
		
		// build GUI (without the tree)
		
		// create panel for level selection and for tree
		tree = new SpectralJTree();
		tree.addTreeWillExpandListener(this);
		tree.addTreeExpansionListener(this);		
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		tree_scroll_pane = new JScrollPane(tree);
				
		panel = new JPanel();		
		order_by_box = new JComboBox();
		
		combo_table_data c = new combo_table_data("Sampling Date", "Acquisition Time");
		order_by_box.addItem(c);
		
		c = new combo_table_data("Filename", "File Name");
		order_by_box.addItem(c);	
		
		c = new combo_table_data("Insert order", "Loading Time");
		order_by_box.addItem(c);	
		
		order_by_box.addActionListener(this);
		
		JPanel order_by_panel = new JPanel();
		order_by_panel.setLayout(new BorderLayout());
		order_by_panel.add(order_by_box, BorderLayout.EAST);
		order_by_panel.add(new JLabel("Order by:"), BorderLayout.WEST);
		
		
		JButton refresh = new JButton("Refresh");
		refresh.setActionCommand("refresh");
		refresh.addActionListener(this);				
		
		panel.setLayout(new BorderLayout());
		panel.add(order_by_panel, BorderLayout.NORTH);
		panel.add(tree_scroll_pane, BorderLayout.CENTER);
		panel.add(refresh, BorderLayout.SOUTH);		
		
		this.getViewport().add(panel);	

	}
	
	public void set_view_restriction(boolean restrict_to_view) throws SPECCHIOClientException
	{
		if (this.restrict_to_view != restrict_to_view) {
			this.restrict_to_view = restrict_to_view;
			build_tree();
		}
	}
	
	public void build_tree(int campaign_id) throws SPECCHIOClientException
	{	
		// this call recursively builds the tree!
		campaign_node cn = specchio_client.getCampaignNode(campaign_id, order_by, restrict_to_view);
		build_tree(cn);	
	}
	
	// show all campaigns of the database
	public void build_tree() throws SPECCHIOClientException
	{
		database_node dn = specchio_client.getDatabaseNode(order_by, restrict_to_view);
		build_tree(dn);		
	}
	
	void build_tree(spectral_node_object node)
	{
		root = new SpectralDataBrowserNode(this, node);	
				
		DefaultTreeModel model = new DefaultTreeModel(root);
		root.setTree(model);
		root.defineChildNodes();
		tree.setModel(model);
		
	}

	public void treeCollapsed(TreeExpansionEvent arg0) {
		
	}
	
	
	public Campaign get_selected_campaign() throws SPECCHIOClientException
	{
		Campaign campaign = null;	
		campaign_node bn = getCampaignNode();
			
			if (bn != null) {
				campaign = specchio_client.getCampaign(bn.getId());
			}
		return campaign;
	}
	
	
	public spectral_node_object get_selected_node() {
		
		TreePath path = tree.getSelectionPath();
		if (path != null) {
			SpectralDataBrowserNode bn = (SpectralDataBrowserNode)path.getLastPathComponent();
			return (bn != null)? bn.getNode() : null;
		} else {
			return null;
		}
		
	}

	public boolean onlyCampaignNodeIsSelected()
	{
		
		TreePath[] paths = tree.getSelectionPaths();
		
		if(paths != null && paths.length == 1)
		{			
			spectral_node_object sn = ((SpectralDataBrowserNode)paths[0].getLastPathComponent()).getNode();
			
			if(sn instanceof campaign_node)
				return true;
		}
		
		return false;
	}
	
	public campaign_node getCampaignNode()
	{
		
		TreePath path = tree.getSelectionPath();
		if (path != null) {
			// find the campaign node at the root of the selection
			SpectralDataBrowserNode bn = (SpectralDataBrowserNode)path.getLastPathComponent();
			while (bn != null && !(bn.getNode() instanceof campaign_node)) {
				path = path.getParentPath();
				bn = (path != null)? (SpectralDataBrowserNode)path.getLastPathComponent() : null;
			}
			
			return (campaign_node) bn.getNode();
		}
		
		return null;

	}
	
	
	public ArrayList<Integer> get_top_hierarchy_ids_of_campaign() throws SPECCHIOClientException
	{
		ArrayList<Integer> ids = new ArrayList<Integer>();
		
		campaign_node n = getCampaignNode();
		List<spectral_node_object> children = null;
		if(n != null)
		{
			children = this.specchio_client.getChildrenOfNode(n);
		}
		
		if (children != null)
		{
			for(spectral_node_object c : children)
			{
				ids.add(c.getId());
			}
			
		}
		
		return ids;
	}
	
	public ArrayList<Integer> get_selected_spectrum_ids() throws SPECCHIOClientException
	{
		
		Set<Integer> ids = new TreeSet<Integer>();
		directly_select_number_of_spectra = 0;
		
		// process all selected nodes
		TreePath[] paths = tree.getSelectionPaths();
		
		// paths can be null when collapsing tree event happened
		if(paths != null) 
		{
			for(int i = 0; i < paths.length; i++)
			{
				SpectralDataBrowserNode bn = (SpectralDataBrowserNode)paths[i].getLastPathComponent();
				spectral_node_object sn = bn.getNode();
				
		        	if(sn instanceof spectrum_node)
		    		{
		        		ids.add(sn.getId()); // avoid server call
		        		directly_select_number_of_spectra++;
		    		}
		        	else
		        	{
		        		ids.addAll(specchio_client.getSpectrumIdsForNode(bn.getNode()));
		        	}
				
			}
		}
		
		return new ArrayList<Integer>(ids);
	}	
	
	public ArrayList<Integer> get_selected_hierarchy_ids()
	{
		
		ArrayList<Integer> ids = new ArrayList<Integer>();
		
		// process all selected nodes
		TreePath[] paths = tree.getSelectionPaths();
		
		// paths can be null when collapsing tree event happened
		if(paths != null) 
		{
			for(int i = 0; i < paths.length; i++)
			{
				SpectralDataBrowserNode bn = (SpectralDataBrowserNode)paths[i].getLastPathComponent();
				spectral_node_object sn = bn.getNode();
				
				if(sn instanceof hierarchy_node)
				{
					// add this hierarchy nodes id to the list
					ids.add(sn.getId());
				}
			}
		}
		
		return ids;
	}	
	
	
	public void actionPerformed(ActionEvent e) {
		
		if("refresh".equals(e.getActionCommand()))
		{
			try {
				reload_tree();
			}
			catch (SPECCHIOClientException ex) {
	  			ErrorDialog error = new ErrorDialog(
			    		SPECCHIOApplication.getInstance().get_frame(),
		    			"Error",
		    			ex.getUserMessage(),
		    			ex
			    	);
		  			error.setVisible(true);
			}
		}
		
		if("comboBoxChanged".equals(e.getActionCommand()))
		{	 	
			try {
				this.order_by = ((combo_table_data) this.order_by_box.getSelectedItem()).getValue();
				reload_tree();
			}
			catch (SPECCHIOClientException ex) {
	  			ErrorDialog error = new ErrorDialog(
			    		SPECCHIOApplication.getInstance().get_frame(),
		    			"Error",
		    			ex.getUserMessage(),
		    			ex
			    	);
		  			error.setVisible(true);
			}
		}
	}
	
	public String get_order_by_field()
	{
		return this.order_by;
	}
	
	public SPECCHIOClient get_specchio_client() {
		
		return specchio_client;
		
	}
	
	public void reload_tree() throws SPECCHIOClientException
	{		
		this.build_tree();
	}


	/**
	 * This class represents a node in the JTree.
	 */
	public class SpectralDataBrowserNode extends DefaultMutableTreeNode {
	
		/**
		 * Serialisation version.
		 */
		private static final long serialVersionUID = 1L;
		
		/** the spectral data browser to which this node belongs */
		private SpectralDataBrowser browser;
		//private JTree tree;
		private  TreeModel model;
		
		/** have this node's children been downloaded from the server? */
		private boolean areChildrenDefined = false;
		
		
		/**
		 * Constructor.
		 * 
		 * @param sn	the spectral node object for this tree node
		 */
		public SpectralDataBrowserNode(SpectralDataBrowser browser, spectral_node_object sn) {		
			this(browser, null, sn);			
		}
		
		public void setTree(TreeModel model) {
			this.model = model;	
			
			// set model in children
			Enumeration list = this.children();
			
			while(list.hasMoreElements())
			{
				SpectralDataBrowserNode child = (SpectralDataBrowserNode) list.nextElement();
				child.setTree(model);
			}
			
		}

		public SpectralDataBrowserNode(SpectralDataBrowser browser, TreeModel model, spectral_node_object sn) {
			
			this.browser = browser;
			this.model = model;
			this.areChildrenDefined = false;
			super.setUserObject(sn);
			
			setAllowsChildren(true);
			
		}		
		
        @Override
        public boolean isLeaf() {
        	
        	if(userObject instanceof spectrum_node)
    		{
        		return true;
    		}
        	
            return false;
        }		
		
		
		
		/**
		 * Download the children of this node from the server. Does nothing
		 * if the children have already been downloaded.
		 */
		protected void defineChildNodes() {
			
			if (!areChildrenDefined) {
				
				try {
					// get a list of child nodes from the server
					List<spectral_node_object> children =
							browser.get_specchio_client().getChildrenOfNode((spectral_node_object)super.getUserObject());
					
					// set the flag that indicates that the children have been loaded
				    areChildrenDefined = true;			
				    
				    setAllowsChildren(children.size() > 0);
					
					// insert the children into the JTree
					for (int i = 0; i < children.size(); i++) {
						SpectralDataBrowserNode treeChild = new SpectralDataBrowserNode(this.browser, model, children.get(i));
						this.add(treeChild);
					}
					
					if(model!=null)
					{
						
						if (model instanceof DefaultTreeModel) {
							DefaultTreeModel defaultModel = (DefaultTreeModel) model;
							defaultModel.nodeStructureChanged(this);
						}		
					}
					
					// set the flag that indicates that the children have been loaded
				    areChildrenDefined = true;
				}
				catch (SPECCHIOClientException ex) {
					// nothing we can do about it
					ex.printStackTrace();
				}
				
			}
		}
		
		
		/**
		 * Get the spectral node object associated with this tree node
		 * 
		 * @return the spectral node object associated with this tree node
		 */
		public spectral_node_object getNode() {
			
			return (spectral_node_object)super.getUserObject();
			
		}
		
		
		/**
		 * Get the identifier of the spectral node object associated with this tree node
		 * 
		 * @return the identifier of the spectra node object associated with this tree node
		 */
		public int getNodeId() {
			
			return getNode().getId();
		
		}
		
	}
	
	
	public boolean onlyHierarchiesAreSelected()
	{
		if(this.directly_select_number_of_spectra == 0)
			return true;
		else
			return false;
	}


	@Override
	public void treeExpanded(TreeExpansionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void treeWillCollapse(TreeExpansionEvent arg0)
			throws ExpandVetoException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void treeWillExpand(TreeExpansionEvent arg0)
			throws ExpandVetoException {

		SpectralDataBrowserNode bn = (SpectralDataBrowserNode)arg0.getPath().getLastPathComponent();
		bn.defineChildNodes();
		
	}
	
	
	// http://www.java2s.com/Tutorials/Java/Swing/JTree/Add_a_popup_menu_to_JTree_in_Java.htm
	class SpectralJTree extends JTree implements ActionListener {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		JPopupMenu popup = new JPopupMenu();
		JMenuItem cp_name = new JMenuItem("Copy hierarchy name to clipboard");
		JMenuItem cp_path = new JMenuItem("Copy hierarchy path to clipboard");
		JMenuItem cp_id = new JMenuItem("Copy hierarchy id to clipboard");
		SpectralDataBrowserNode last_menu_clicked_node = null;
		TreePath last_menu_clicked_path = null;

		SpectralJTree() {
			cp_name.addActionListener(this);
			cp_name.setActionCommand("CopyName");
			popup.add(cp_name);
			cp_path.addActionListener(this);
			cp_path.setActionCommand("CopyPath");
			popup.add(cp_path);		
			cp_id.addActionListener(this);
			cp_id.setActionCommand("CopyID");
			popup.add(cp_id);				
			addMouseListener(new MouseAdapter() {
				// listen for both for pressed and released due to differnt platform support (In Mac OS X, the pop-up trigger is set on MOUSE_PRESSED.In Windows it is set on MOUSE_RELEASED.)
				// https://github.com/zaproxy/zaproxy/issues/131
				public void mouseReleased(MouseEvent e) {
					if (e.isPopupTrigger()) {
						popup.show((JComponent) e.getSource(), e.getX(), e.getY());
					}
				}
				public void mousePressed(MouseEvent e) {
					if (e.isPopupTrigger()) {
						// set selected path based on path of node that was clicked on
						last_menu_clicked_path = tree.getPathForLocation(e.getX(), e.getY());

						last_menu_clicked_node = (SpectralDataBrowserNode) last_menu_clicked_path.getLastPathComponent();

						popup.show((JComponent) e.getSource(), e.getX(), e.getY());
					}
				}		      
			});

		}

		  public void actionPerformed(ActionEvent ae) {
   
		    if (ae.getActionCommand().equals("CopyName")) {
		    	
				StringSelection stringSelection = new StringSelection(last_menu_clicked_node.toString());
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			    clipboard.setContents(stringSelection , null );		    		
		    }
		    
		    if (ae.getActionCommand().equals("CopyPath")) {
		    	
				StringSelection stringSelection = new StringSelection(last_menu_clicked_path.toString());
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			    clipboard.setContents(stringSelection , null );		    		
		    }		    
		    
		    if (ae.getActionCommand().equals("CopyID")) {
		    	
		    	Integer id = last_menu_clicked_node.getNodeId();
				StringSelection stringSelection = new StringSelection(id.toString());
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			    clipboard.setContents(stringSelection , null );		    		
		    }		    		    
		    
		  }	
		  
	}

}
