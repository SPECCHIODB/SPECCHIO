package ch.specchio.gui;

import ch.specchio.client.SPECCHIOClientException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;

public class dateFormatterSelection extends JPanel implements ActionListener {

    final ButtonGroup group;
    public int selected_formatter = 0;

    public dateFormatterSelection(ArrayList<DateTime> valid_dts) throws SPECCHIOClientException
    {

        GridBagConstraints constraints = new GridBagConstraints();

        // some default values. subclasses can always overwrite these
        constraints.gridwidth = 1;
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;


        // build GUI

        this.setLayout(new BorderLayout());


        // create new panel for fields and buttons
        JPanel box = new JPanel();

        box.setLayout(new BoxLayout(box, BoxLayout.PAGE_AXIS));
        box.add(new JLabel("Please select the correct date: [YYYY-MM-DD]"));


        group = new ButtonGroup();

        ArrayList<JRadioButton> date_format_buttons = new ArrayList<>();

        Iterator<DateTime> it = valid_dts.iterator();
        Integer cnt = 0;

        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

        while(it.hasNext()){

            DateTime dt = it.next();
            String ds =  fmt.print(dt);

            JRadioButton radioButton = new JRadioButton(ds);
            radioButton.setActionCommand(cnt.toString());
            date_format_buttons.add(radioButton);
            group.add(radioButton);
            box.add(radioButton);
            cnt++;
        }

        date_format_buttons.get(0).setSelected(true); // default selection

        JButton ok = new JButton("Apply");
        ok.setActionCommand("OK");
        ok.addActionListener(this);

        JPanel pane = new JPanel(new BorderLayout());
        pane.add(box, BorderLayout.PAGE_START);
        pane.add(new JLabel("Note: the selected format will be used to parse all other files during this loading process"), BorderLayout.CENTER);
        pane.add(ok, BorderLayout.PAGE_END);


        // add control panel to dialog
        this.add("East", pane);


    }



    @Override
    public void actionPerformed(ActionEvent e) {

        String command =e.getActionCommand();

        //pick one of many
        if (command == "OK") {

            ButtonModel selection = group.getSelection();
            this.selected_formatter = Integer.parseInt(selection.getActionCommand());

            Container parent = this.getRootPane();

            JDialog parent_dialog = (JDialog) parent.getParent();

            parent_dialog.setVisible(false);

        }

    }
}
