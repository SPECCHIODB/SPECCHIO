package ch.specchio.gui;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.types.Campaign;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class DataSelectionPanel extends JPanel implements ActionListener, TreeSelectionListener, ChangeListener, MouseMotionListener, MouseListener {
    private SpectralDataBrowser hierarchySelect;
    private JCheckBox show_only_my_data;
    private JTabbedPane data_selection_tabs;
    private DataPanel dataPanel;
    private ArrayList<Integer> selectedIds;
    private Campaign selectedCampaign;
    private boolean wasMoved;
    private boolean wasDragged;

    public DataSelectionPanel(){
//        Dimension dim = getPreferredSize();
//        dim.width = 250;
//        setPreferredSize(dim);

        setLayout(new BorderLayout());

        SPECCHIOClient specchioClient = SPECCHIOApplication.getInstance().getClient();

        hierarchySelect = new SpectralDataBrowser(specchioClient, true);
        data_selection_tabs = new JTabbedPane();
        setupHierarchyBrowser();

        dataPanel = new DataPanel();
        selectedIds = new ArrayList<>();
        wasMoved = false;
        wasDragged = false;

        add(hierarchySelect, BorderLayout.NORTH);
        add(dataPanel, BorderLayout.CENTER);

    }

    private void setupHierarchyBrowser() {
        hierarchySelect.build_tree();

        // add tree listener
        hierarchySelect.tree.addTreeSelectionListener(this);
        hierarchySelect.order_by_box.addActionListener(this);
        hierarchySelect.tree.setDragEnabled(true);
        hierarchySelect.tree.setTransferHandler(new DataVisTransferHandler("spectrumIdentifiers"));
        hierarchySelect.tree.addMouseListener(this);
        hierarchySelect.tree.addMouseMotionListener(this);

        show_only_my_data = new JCheckBox("Show only my data.");
        show_only_my_data.addChangeListener(this);

        JPanel sdb_panel = new JPanel();
        sdb_panel.setLayout(new BorderLayout());
        sdb_panel.add("North", show_only_my_data);
        sdb_panel.add("Center", hierarchySelect);

        data_selection_tabs.addTab("Browser", sdb_panel);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
       selectedIds = hierarchySelect.get_selected_spectrum_ids();
       selectedCampaign = hierarchySelect.get_selected_campaign();
//       for(int i : ids){
//           System.out.println(i);
//       }
    }

    @Override
    public void stateChanged(ChangeEvent e) {

    }

    public DataPanel getDataPanel() {
        return dataPanel;
    }

    public void setDataPanel(DataPanel dataPanel) {
        this.dataPanel = dataPanel;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        wasDragged = true;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        wasMoved = true;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if(wasMoved && wasDragged){
            if(selectedIds.size() > 0){
//                hierarchySelect.tree.getTransferHandler().
                //System.out.println(selectedCampaign.getName());
            }
            wasMoved = false;
            wasDragged = false;
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
