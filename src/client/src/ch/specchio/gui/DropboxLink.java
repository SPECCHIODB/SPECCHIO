package ch.specchio.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuth;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.users.FullAccount;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.client.SPECCHIOPreferencesStore;

public class DropboxLink extends JFrame implements ActionListener {
		
		private static final long serialVersionUID = 1L;
		SPECCHIOClient specchio_client;
		
		String authorizeUrl;
		
		GridbagLayouter l;
		GridBagConstraints constraints;
		private JTextField auth_code_field;
		private JButton store;
		private DbxWebAuth dbxWebAuth;
		private DbxRequestConfig config;

		public DropboxLink() throws SPECCHIOClientException {
			super("Dropbox Link");
			
			
			constraints = new GridBagConstraints();
			
			// some default values. subclasses can always overwrite these
			constraints.gridwidth = 1;
			constraints.insets = new Insets(4, 4, 4, 4);
			constraints.gridheight = 1;
			constraints.anchor = GridBagConstraints.WEST;
			constraints.fill = GridBagConstraints.HORIZONTAL;
			
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			
			JPanel panel = new JPanel();
			
			l = new GridbagLayouter(panel);
			
			
			// build GUI
			constraints.gridx = 0;	
			constraints.gridy = 0;	
			
			l.insertComponent(new JLabel("1: Click this link to start the authentication"), constraints);
			
			JButton button = new JButton();
			
        	DbxAppInfo dbxAppInfo = new DbxAppInfo("7gj3n1ol65mgq89", "fshdzufbmdx64zd");
        	config = DbxRequestConfig.newBuilder("SPECCHIO").build();

        	DbxWebAuth.Request authRequest = DbxWebAuth.newRequestBuilder()
                    .build();
        	dbxWebAuth = new DbxWebAuth(config, dbxAppInfo);
        	
        	authorizeUrl = dbxWebAuth.authorize(authRequest);
        	
        	String text = "<html>" + "<FONT color=\"#000099\"><U>" + authorizeUrl + "</U></FONT>" + " </HTML>";

            button.setBorderPainted(false);
            button.setOpaque(false);        	
        	button.setText(text);
        	button.addActionListener(new OpenUrlAction());
        	
        	constraints.gridx = 1;	
        	l.insertComponent(button, constraints);
        	
        	
        	constraints.gridx = 0;		
        	constraints.gridy++;	
        	l.insertComponent(new JLabel("2: Paste Dropbox authentication code here and press <Enter>"), constraints);
        	
        	constraints.gridx = 1;		        	
			auth_code_field = new JTextField();
			auth_code_field.setActionCommand("auth_code_field");
			auth_code_field.addActionListener((ActionListener) this);
			auth_code_field.setToolTipText("This code is provided by Dropbox after allowing SPECCHIO to access your Dropbox");        	
        	
			l.insertComponent(auth_code_field, constraints);
			
			
			this.setLayout(new BorderLayout());
        	
			this.add("Center", panel);
			
			JPanel panel2 = new JPanel();
			
			store = new JButton("OK");
			store.setActionCommand("store");
			store.addActionListener(this);
			store.setEnabled(false);
			panel2.add(store);
			
			JButton cancel = new JButton("Cancel");
			cancel.setActionCommand("cancel");
			cancel.addActionListener(this);					
			panel2.add(cancel);
			
			this.add("South", panel2);
			
			pack();
			
			this.setVisible(true);
			
		}

		@Override
		public void actionPerformed(ActionEvent e) {

			if ("cancel".equals(e.getActionCommand())) {
				this.setVisible(false);
			} 
			if ("store".equals(e.getActionCommand())) {
				System.out.println("store");

				try {


					String auth_code = auth_code_field.getText();

					DbxAuthFinish authFinish = dbxWebAuth.finishFromCode(auth_code);
					String authAccessToken = authFinish.getAccessToken();

					DbxClientV2 client = new DbxClientV2(config, authAccessToken);  
					FullAccount account = client.users().getCurrentAccount();

					// store this account and token in the preferences
					SPECCHIOPreferencesStore prefs_store = new SPECCHIOPreferencesStore();

					Preferences prefs = prefs_store.getPreferences();

					Preferences node = prefs.node("Dropbox");
					node.put(account.getEmail(), authAccessToken);

					this.setVisible(false);
					
		    		  JOptionPane.showMessageDialog(
		    				  SPECCHIOApplication.getInstance().get_frame(),
		    				  "Dropbox account for " + account.getEmail() + " has been added to SPECCHIO Preferences.",
		    				  "Info",
		    				  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
		    			);

					
				} catch (BackingStoreException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (DbxException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
		     		  JOptionPane.showMessageDialog(
		     				  SPECCHIOApplication.getInstance().get_frame(),
		     				 e1.getMessage(),
		     				  "Dropbox Error",
		     				  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon);					
				}
				
			}
			
			if ("auth_code_field".equals(e.getActionCommand())) {
				store.setEnabled(true);
			} 
			
		}
		
		   class OpenUrlAction implements ActionListener {
			      @Override public void actionPerformed(ActionEvent e) {
			    	  try {
						SPECCHIOApplication.openInDesktop(new URI(authorizeUrl));
					} catch (URISyntaxException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
			      }
			    }		
			
}
