package ch.specchio.gui;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.metadata.MDE_Spectrum_Controller;
import ch.specchio.types.ConflictTable;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.spectral_node_object;
import ch.specchio.types.spectrum_node;


import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.*;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

public class DataSelectionPanel3 extends JPanel implements TreeSelectionListener {
    private SPECCHIOClient specchioClient;
    private Frame frameRef;
    private SpectralDataBrowser hierarchySelect;
    private ArrayList<Integer> selectedIds;
    private JTextArea textArea;
    private MDE_Spectrum_Controller mdeSpectrumController;
    private SpectrumMetadataCategoryList categoryList;

    public DataSelectionPanel3(SPECCHIOClient specchioClient, Frame frameReference){
        this.specchioClient = specchioClient;
        this.frameRef = frameReference;
        // SPECTRUM CONTROLLER
        mdeSpectrumController = new MDE_Spectrum_Controller(specchioClient);
        mdeSpectrumController.set_hierarchy_ids(new ArrayList<>(0));
        // CATEGORY LIST
        categoryList = new SpectrumMetadataCategoryList(mdeSpectrumController.getFormFactory());
        categoryList.getFormDescriptor();
         // DEFINE LAYOUT
        setLayout(new BorderLayout());
        initComp();
    }

    private void initComp() {
        // COMPONENTS

        // HIERARCHY TREE-SELECTION
        hierarchySelect = new SpectralDataBrowser(specchioClient, true);
        hierarchySelect.build_tree();
        hierarchySelect.tree.addTreeSelectionListener(this);
        hierarchySelect.tree.setDragEnabled(true);

        // TEXT PANEL
        textArea = new JTextArea();
        textArea.setDragEnabled(true);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        // TRANSFER HANDLERS
        FileTransferHandler transferHandler = new FileTransferHandler(textArea);
        textArea.setTransferHandler(transferHandler);
        hierarchySelect.tree.setTransferHandler(transferHandler);


        // ARRAYLIST FOR SELECTED SPECTRUM IDS
        selectedIds = new ArrayList<>();

        // ADD TO LAYOUT
        add(hierarchySelect, BorderLayout.NORTH);
        add(new JScrollPane(textArea), BorderLayout.CENTER);

    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        selectedIds = hierarchySelect.get_selected_spectrum_ids();
        System.out.println("NUMBER OF SELECTED SPECTRA = " + selectedIds.size());
    }
}

class FileTransferHandler extends TransferHandler {
    JTextArea textArea;
    DataFlavor localFileFlavor = new DataFlavor(ArrayList.class, "An ArrayList Object");
   // String localFileType = localFileFlavor.

    public FileTransferHandler(JTextArea ta) {
        textArea = ta;

    }

    public boolean importData(JComponent c, Transferable t) {
        if (!canImport(c, t.getTransferDataFlavors()))
            return false;

        try {
            ArrayList<Integer> myIds = (ArrayList<Integer>) t.getTransferData(localFileFlavor);

            textArea.setText("");
            for (int j = 0; j < myIds.size(); j++) {
                textArea.append(myIds.get(j).toString() + " ");
            }

            return true;
        } catch (UnsupportedFlavorException ufe) {
            System.err.println("importData: unsupported data flavor - " +
                    ufe.getMessage());
        } catch (IOException ioe) {
            System.err.println("importData IO exception: " + ioe.getMessage());
        }
        return false;
    }

    /**
     * for the JTree
     */
    public Transferable createTransferable(JComponent c) {
        if (c instanceof JTextArea) {
            return null;
        }

        JTree tree = (JTree) c;

        Set<Integer> ids = new TreeSet<Integer>();
        // process all selected nodes
        TreePath[] paths = tree.getSelectionPaths();

        // paths can be null when collapsing tree event happened
        if (paths != null) {
            for (int i = 0; i < paths.length; i++) {
                SpectralDataBrowser.SpectralDataBrowserNode bn = (SpectralDataBrowser.SpectralDataBrowserNode) paths[i].getLastPathComponent();
                spectral_node_object sn = bn.getNode();

                if (sn instanceof spectrum_node) {
                    ids.add(sn.getId()); // avoid server call
                } else {
                    ids.addAll(SPECCHIOApplication.getInstance().getClient().getSpectrumIdsForNode(bn.getNode()));
                }

            }
        }
        return new TransferableArrayList(new ArrayList<Integer>(ids));
    }

    public int getSourceActions(JComponent c) {
        return COPY;
    }

    public boolean canImport(JComponent c, DataFlavor[] flavors) {
        if (localFileFlavor == null)
            return false;

        for (int j = 0; j < flavors.length; j++) {
            if (localFileFlavor.equals(flavors[j]))
                return true;
        }
        return false;
    }

    public class TransferableArrayList implements Transferable {

        DataFlavor arrayListFlavor = new DataFlavor(ArrayList.class, "An ArrayList Object");
        DataFlavor[] supportedFlavors = {arrayListFlavor, DataFlavor.stringFlavor};

        private final ArrayList<Integer> selectedIds;

        public TransferableArrayList(ArrayList<Integer> selectedIds) {
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
            if (flavor.equals(arrayListFlavor)) {
                return selectedIds;
            } else if (flavor.equals(DataFlavor.stringFlavor)) {
                return selectedIds.toString();
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }
    }
}