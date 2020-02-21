package ch.specchio.gui;

import javax.swing.*;

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
}
