package ch.specchio.gui;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.metadata.MDE_Spectrum_Controller;
import ch.specchio.queries.Query;
import ch.specchio.queries.QueryConditionChangeInterface;
import ch.specchio.query_builder.QueryController;
import ch.specchio.types.Campaign;
import ch.specchio.types.spectral_node_object;
import ch.specchio.types.spectrum_node;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.TreePath;
import javax.xml.crypto.Data;
import javax.xml.soap.Text;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.IOException;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

public class DataSelectionPanel2 extends JPanel implements TreeSelectionListener, ActionListener, DragGestureListener, MouseMotionListener, MouseListener, ChangeListener {
    private SpectralDataBrowser hierarchySelect;
    private JCheckBox show_only_my_data;
    private JTabbedPane data_selection_tabs;
    private DataPanel dataPanel;
    private ArrayList<Integer> selectedIds;
    private Campaign selectedCampaign;
    private SPECCHIOClient specchioClient;
    private Frame parentFrame;
    private ArrayList<QueryConditionChangeInterface> change_listeners = new ArrayList<QueryConditionChangeInterface>();
    private JTextArea testTextArea;



    public DataSelectionPanel2(SPECCHIOClient specchioClient, Frame frameReference){
        // DEFINE LAYOUT
        setLayout(new BorderLayout());

        // REFERENCES
        this.specchioClient = specchioClient;
        this.parentFrame = frameReference;
        initComp();

    }

    private void initComp() {
        // COMPONENTS

        // HIERARCHY TREE-SELECTION
        hierarchySelect = new SpectralDataBrowser(specchioClient, true);
        data_selection_tabs = new JTabbedPane();
        setupHierarchyBrowser();

        // TEXT PANEL
        this.testTextArea = new JTextArea();
        MyDropTargetListener dropTargetListener = new MyDropTargetListener(testTextArea);
        DragSource ds = new DragSource();
        ds.createDefaultDragGestureRecognizer(hierarchySelect.tree, DnDConstants.ACTION_COPY, this);

        selectedIds = new ArrayList<>();
//        wasMoved = false;
//        wasDragged = false;

        add(hierarchySelect, BorderLayout.NORTH);
        add(testTextArea, BorderLayout.CENTER);

    }

    private void setupHierarchyBrowser() {
        hierarchySelect.build_tree();

        // add tree listener
        hierarchySelect.tree.addTreeSelectionListener(this);
        hierarchySelect.order_by_box.addActionListener(this);
//        hierarchySelect.tree.setDragEnabled(true);
        //hierarchySelect.tree.setTransferHandler(new TransferableArrayList(selectedIds));
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
    public void valueChanged(TreeSelectionEvent e) {
        selectedIds = hierarchySelect.get_selected_spectrum_ids();
        selectedCampaign = hierarchySelect.get_selected_campaign();
        System.out.println("Number of selected spectra = " + selectedIds.size());
//        specCtrl.set_spectrum_ids(selectedIds);

//       ConflictTable conflictTable = specchioClient.getEavMetadataConflicts(MetaParameter.SPECTRUM_LEVEL, selectedIds);

    }



    public DataPanel getDataPanel() {
        return dataPanel;
    }

    public void setDataPanel(DataPanel dataPanel) {
        this.dataPanel = dataPanel;
    }


    @Override
    public void dragGestureRecognized(DragGestureEvent dragGestureEvent){
        // clean old list
        this.testTextArea.setText("");
        Cursor cursor = Cursor.getDefaultCursor();
        JTree selector = (JTree) dragGestureEvent.getComponent();
//
//        Set<Integer> ids = new TreeSet<Integer>();
//
//        // process all selected nodes
//        TreePath[] paths = selector.getSelectionPaths();
//
//        // paths can be null when collapsing tree event happened
//        if(paths != null)
//        {
//            for(int i = 0; i < paths.length; i++)
//            {
//                SpectralDataBrowser.SpectralDataBrowserNode bn = (SpectralDataBrowser.SpectralDataBrowserNode)paths[i].getLastPathComponent();
//                spectral_node_object sn = bn.getNode();
//
//                if(sn instanceof spectrum_node)
//                {
//                    ids.add(sn.getId()); // avoid server call
//                }
//                else
//                {
//                    ids.addAll(specchioClient.getSpectrumIdsForNode(bn.getNode()));
//                }
//
//            }
//        }
//
//       selectedIds = new ArrayList<Integer>(ids);

        if(dragGestureEvent.getDragAction() == DnDConstants.ACTION_COPY){
            cursor = DragSource.DefaultCopyDrop;
        }
        dragGestureEvent.startDrag(cursor, new TransferableArrayList(selectedIds));

    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void stateChanged(ChangeEvent e) {

    }

    private class MyDropTargetListener extends DropTargetAdapter{
        private final DropTarget dropTarget;
        private final JTextArea textArea;

        public MyDropTargetListener(JTextArea textArea){
            this.textArea = textArea;
            this.dropTarget = new DropTarget(textArea, DnDConstants.ACTION_COPY, this, true, null);
        }

        @Override
        public void drop(DropTargetDropEvent dtde) {
            try{
                Transferable tr = dtde.getTransferable();
                ArrayList<Integer> draggedIds = (ArrayList<Integer>) tr.getTransferData(TransferableArrayList.arrayListFlavor);

                if (dtde.isDataFlavorSupported(TransferableArrayList.arrayListFlavor)){
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    this.textArea.append("SELECTED IDS = \n");
                    for(int j = 0; j < draggedIds.size(); j++){
                        this.textArea.append(draggedIds.get(j).toString() + "\n");
                    }
                    dtde.dropComplete(true);
                    return;
                }
                dtde.rejectDrop();
            } catch (Exception e){
                e.printStackTrace();
                dtde.rejectDrop();
            }
        }
    }
}

class TransferableArrayList implements Transferable{

    protected static final DataFlavor arrayListFlavor = new DataFlavor(ArrayList.class, "An ArrayList Object");
    protected static final DataFlavor[] supportedFlavors = {arrayListFlavor, DataFlavor.stringFlavor};

    private final ArrayList<Integer> selectedIds;

    public TransferableArrayList(ArrayList<Integer> selectedIds){
        this.selectedIds = selectedIds;
    }


    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return supportedFlavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(arrayListFlavor) || flavor.equals(DataFlavor.stringFlavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if(flavor.equals(arrayListFlavor)){
            return selectedIds;
        } else if (flavor.equals(DataFlavor.stringFlavor)){
            return selectedIds.toString();
        } else{
            throw new UnsupportedFlavorException(flavor);
        }
    }
}

