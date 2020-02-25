package ch.specchio.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;

public class DataVisTransferHandler extends TransferHandler {
    private String type;
    private Object dataObject;
    public DataVisTransferHandler(String type){
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getDataObject() {
        return dataObject;
    }

    public void setDataObject(Object dataObject) {
        this.dataObject = dataObject;
    }

    protected DataVisTransferHandler() {
        super();
    }

    @Override
    public void setDragImage(Image img) {
        super.setDragImage(img);
    }

    @Override
    public Image getDragImage() {
        return super.getDragImage();
    }

    @Override
    public void setDragImageOffset(Point p) {
        super.setDragImageOffset(p);
    }

    @Override
    public Point getDragImageOffset() {
        return super.getDragImageOffset();
    }

    @Override
    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        super.exportAsDrag(comp, e, action);
    }

    @Override
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
        super.exportToClipboard(comp, clip, action);
    }

    @Override
    public boolean importData(TransferSupport support) {
        return super.importData(support);
    }

    @Override
    public boolean importData(JComponent comp, Transferable t) {
        return super.importData(comp, t);
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return super.canImport(support);
    }

    @Override
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        return super.canImport(comp, transferFlavors);
    }

    @Override
    public int getSourceActions(JComponent c) {
        return super.getSourceActions(c);
    }

    @Override
    public Icon getVisualRepresentation(Transferable t) {
        return super.getVisualRepresentation(t);
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        return super.createTransferable(c);
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        super.exportDone(source, data, action);
    }
}
