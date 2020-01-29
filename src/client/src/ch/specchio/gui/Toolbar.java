package ch.specchio.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Toolbar extends JPanel implements ActionListener {
    private JButton button1;
    private JButton button2;

    public Toolbar(){
        button1 = new JButton("Hello");
        button2 = new JButton("Bye");

        button1.addActionListener(this);
        button2.addActionListener(this);

        setLayout(new FlowLayout(FlowLayout.LEFT));
        add(button1);
        add(button2);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {

    }
}
