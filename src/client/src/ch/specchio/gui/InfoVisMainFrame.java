package ch.specchio.gui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InfoVisMainFrame extends JFrame implements ActionListener {

    private TextPanel textPanel;
    private JButton jButton;
    private Toolbar toolbar;

    public InfoVisMainFrame(){
        super("InfoVis Toolbox");

        setLayout(new BorderLayout());

        textPanel = new TextPanel();
        jButton = new JButton("CLICK ME");
        toolbar = new Toolbar();

        add(textPanel, BorderLayout.CENTER);
        add(jButton, BorderLayout.SOUTH);
        add(toolbar, BorderLayout.NORTH);

        jButton.addActionListener(this);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000,1000);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        textPanel.appendTextArea("HELLO");
        textPanel.appendTextArea("\n");
    }
}
