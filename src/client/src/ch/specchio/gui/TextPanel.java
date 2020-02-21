package ch.specchio.gui;

import javax.swing.*;
import java.awt.*;

public class TextPanel extends JPanel {

    private JTextArea textArea;

    public TextPanel(){
        textArea = new JTextArea();
        textArea.setTransferHandler(new TransferHandler("node"));

        setLayout(new BorderLayout());
        add(new JScrollPane(textArea), BorderLayout.CENTER);
    }

    public void appendTextArea(String text){
        textArea.append(text);
    }
}
