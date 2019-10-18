package ch.specchio.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.constants.UserRoles;
import ch.specchio.types.database_node;
import ch.specchio.types.spectral_node_object;


public class DataRemoverDialog extends JFrame implements ActionListener, TreeSelectionListener{

	private static final long serialVersionUID = 1L;
	
	GridbagLayouter l;
	GridBagConstraints constraints;
	
	SpectralDataBrowser sdb;
	JTextField hierarchy_name;
	String table_name;
	
	SPECCHIOClient specchio_client;
	

	class RemoveTreeNodes extends SwingWorker<Integer, String>
	{
		ArrayList<SpectralDataBrowser.SpectralDataBrowserNode> sdb_nodes;
		
		public RemoveTreeNodes(ArrayList<SpectralDataBrowser.SpectralDataBrowserNode> sdb_nodes)
		{
			this.sdb_nodes = sdb_nodes;
		}

		@Override
		protected Integer doInBackground() throws Exception {
			
			DefaultTreeModel model = (DefaultTreeModel)sdb.tree.getModel();
			
			// remove them backwards, but that does not solve the problem of exceptions like "java.lang.ArrayIndexOutOfBoundsException: 58 >= 58" ...
			for(int i=sdb_nodes.size()-1;i>=0;i--)
			{
				model.removeNodeFromParent(sdb_nodes.get(i));
			}		
			return null;
		}
		
		
	}
			
	
	class removing_thread extends Thread
	{
		

		

		ProgressReportDialog pr = new ProgressReportDialog(DataRemoverDialog.this, "Removing data ...", false, 20);
		
		ArrayList<spectral_node_object> nodes = new ArrayList<spectral_node_object>();
		
		ArrayList<SpectralDataBrowser.SpectralDataBrowserNode> sdb_nodes = new ArrayList<SpectralDataBrowser.SpectralDataBrowserNode>();
		
		public removing_thread()
		{

		}
		
		public void addNode(SpectralDataBrowser.SpectralDataBrowserNode sdb_node)
		{
			sdb_nodes.add(sdb_node);
			nodes.add(sdb_node.getNode());
		}
		
		synchronized public void run()
		{
			pr.set_indeterminate(true);
			pr.setVisible(true);
			
			try {
				Instant start = Instant.now();
				// ask the server to remove the node from the database
				pr.set_operation("Deleting data. Please wait.");
								
				specchio_client.removeSpectralNodes(nodes);
				pr.set_operation("Cleaning up...");
				specchio_client.clearMetaparameterRedundancyList();
				
				Instant end = Instant.now();
				System.out.println("Deletion time: " + Duration.between(start, end).getSeconds());
				
				
				// remove the node from the local tree control
				removeTreeNodes(sdb_nodes);
			}
			catch (SPECCHIOClientException ex) {
				
				JTextArea msg = new JTextArea(ex.getMessage() + "\n" + ex.getDetails());
				msg.setLineWrap(true);
				msg.setWrapStyleWord(true);
				Dimension dim = new Dimension(250,150);

				JScrollPane scrollPane = new JScrollPane(msg);		
				scrollPane.setPreferredSize(dim);
				
				
				JOptionPane.showMessageDialog(
		    			DataRemoverDialog.this,
		    			scrollPane,
		    			"Error",
		    			JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
		    		);
		    }
			
			pr.setVisible(false);
			
		}
				
		
	}

	
	DataRemoverDialog() throws SPECCHIOClientException
	{		
		super("Data Remover");
		
		// get a reference to the application's client object
		this.specchio_client = SPECCHIOApplication.getInstance().getClient();
		
		constraints = new GridBagConstraints();
		
		// some default values. subclasses can always overwrite these
		constraints.gridwidth = 1;
		constraints.insets = new Insets(4, 4, 4, 4);
		constraints.gridheight = 1;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		// build GUI
		
		this.setLayout(new BorderLayout());
		
		// create browser and add to control panel
		sdb = new SpectralDataBrowser(specchio_client, !specchio_client.isLoggedInWithRole(UserRoles.ADMIN));
		
		sdb.build_tree();
		this.add("Center", sdb);
		
		// add tree listener
		sdb.tree.addTreeSelectionListener(this);
			
		// create new panel for fields and buttons
		JPanel control = new JPanel();
		l = new GridbagLayouter(control);

		JButton remove = new JButton("Remove");
		remove.setActionCommand("remove");
		remove.addActionListener(this);		
		
		constraints.gridy = 0;
		l.insertComponent(remove, constraints);
		
		JButton cancel = new JButton("Close");
		cancel.setActionCommand("close");
		cancel.addActionListener(this);	
			
		constraints.gridy = 1;
		l.insertComponent(cancel, constraints);
				
		// add control panel to dialog
		this.add("East", control);
				
		pack();
		
		
		
	}
	
	
	public void actionPerformed(ActionEvent e) 
	{
		if ("remove".equals(e.getActionCommand())) {
			
			// process all selected nodes
			TreePath[] paths = sdb.tree.getSelectionPaths();
			
			// paths can be null when collapsing tree event happened
			if(paths != null) 
			{
				removing_thread st = new removing_thread();
				for(int i = 0; i < paths.length; i++)
				{
					SpectralDataBrowser.SpectralDataBrowserNode sdb_node =
							(SpectralDataBrowser.SpectralDataBrowserNode)paths[i].getLastPathComponent();
					
					if (sdb_node.getNode() instanceof database_node) {
						JOptionPane.showMessageDialog(this, "The root node cannot be removed.", "Error", JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon);
					} else {
						
						st.addNode(sdb_node);
					}
				}
				st.start();
			}
				
		}
		
		if ("close".equals(e.getActionCommand())) {
			setVisible(false);
		}


		
	}
	
	
	private void removeTreeNodes(ArrayList<SpectralDataBrowser.SpectralDataBrowserNode> sdb_nodes)
	{

		RemoveTreeNodes rtn = new RemoveTreeNodes(sdb_nodes);
		rtn.execute();
		
	}
	

	
    /** Required by TreeSelectionListener interface. */
	public void valueChanged(TreeSelectionEvent e) {
		
	}


}


