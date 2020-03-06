package ch.specchio.gui;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.metadata.MDE_Spectrum_Controller;
import ch.specchio.queries.Query;
import ch.specchio.queries.QueryCondition;
import ch.specchio.queries.QueryConditionChangeInterface;
import ch.specchio.queries.QueryConditionObject;
import ch.specchio.query_builder.QueryController;
import ch.specchio.query_builder.QueryField;
import ch.specchio.types.*;
import javafx.collections.ListChangeListener;
import org.w3c.dom.ranges.Range;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.ListIterator;

public class DataSelectionPanel extends JPanel implements ActionListener, TreeSelectionListener, ListSelectionListener, ChangeListener, MouseMotionListener, MouseListener, QueryConditionChangeInterface {
    private SpectralDataBrowser hierarchySelect;
    private JCheckBox show_only_my_data;
    private JTabbedPane data_selection_tabs;
    private DataPanel dataPanel;
    private ArrayList<Integer> selectedIds;
    private Campaign selectedCampaign;
    private boolean wasMoved;
    private boolean wasDragged;
    private SpectrumMetadataPanel spMePa;
    private SPECCHIOClient specchioClient;
    private Frame parentFrame;
    private MDE_Spectrum_Controller specCtrl;
    private SpectrumMetadataCategoryList category_list;
    private QueryController qc;
    private SpectrumQueryPanel query_condition_panel;
    private JScrollPane scroll_pane;
    private Query query;
    private ArrayList<QueryConditionChangeInterface> change_listeners = new ArrayList<QueryConditionChangeInterface>();


    public DataSelectionPanel(SPECCHIOClient specchioClient, Frame frameReference){
        setLayout(new BorderLayout());

        this.specchioClient = specchioClient;
        this.parentFrame = frameReference;
        this.specCtrl = new MDE_Spectrum_Controller(specchioClient);

        hierarchySelect = new SpectralDataBrowser(specchioClient, true);
        data_selection_tabs = new JTabbedPane();
        setupHierarchyBrowser();

        category_list = new SpectrumMetadataCategoryList(specCtrl.getFormFactory());
        category_list.addListSelectionListener(this);

        qc = new QueryController(specchioClient, "Standard", category_list.getFormDescriptor());
        qc.addChangeListener(this);
        query_condition_panel = new SpectrumQueryPanel(parentFrame, qc);
        scroll_pane = new JScrollPane(query_condition_panel);
        scroll_pane.getVerticalScrollBar().setUnitIncrement(10);
        query = new Query("spectrum");
        query.addColumn("spectrum_id");
        query.setOrderBy(hierarchySelect.get_order_by_field());


        selectedIds = new ArrayList<>();
        wasMoved = false;
        wasDragged = false;

        data_selection_tabs.addTab("Query conditions", scroll_pane);
        add(hierarchySelect, BorderLayout.NORTH);
//        add(scroll_pane, BorderLayout.CENTER);

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
        System.out.println("Number of selected spectra = " + selectedIds.size());
        specCtrl.set_spectrum_ids(selectedIds);

//       ConflictTable conflictTable = specchioClient.getEavMetadataConflicts(MetaParameter.SPECTRUM_LEVEL, selectedIds);

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


    @Override
    public void valueChanged(ListSelectionEvent e) {
        data_selection_tabs.remove(scroll_pane);

        qc.updateForm(category_list.getFormDescriptor());
        query_condition_panel.update();

        scroll_pane = new JScrollPane(query_condition_panel);
        scroll_pane.getVerticalScrollBar().setUnitIncrement(10);
        data_selection_tabs.addTab("Query conditions", scroll_pane);


    }

    @Override
    public void changed(Object source) {
        // TODO write changelistener for DataSelectionPanel
    }

}
