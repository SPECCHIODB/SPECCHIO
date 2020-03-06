package ch.specchio.gui;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.query_builder.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Hashtable;

public class SpectrumFilterPanel extends JPanel {
    
    private Frame ownerReference;
    private QueryController queryController;
    
    Hashtable<String, SpectrumQueryCategoryContainer>  spectrumQueryCategoryContainer;
    
    public SpectrumFilterPanel(Frame myOwner, QueryController myController){
        super();
        spectrumQueryCategoryContainer = new Hashtable<String, SpectrumQueryCategoryContainer>();
        this.ownerReference = myOwner;
        this.queryController = myController;
        
        setForm(queryController.getForm());
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }


    private void fireConditionChanged(QueryField queryFieldfield, Object value) {
        queryController.ConditionChange(queryFieldfield, value);
    }
    
    public void setForm(QueryForm myForm){
        removeAll();
        
        if(myForm != null){
            // create and add panels for each category container
            for(QueryCategoryContainer qcc : myForm.getCategoryContainers()) {
//                double min = (EAVQueryField) qcc.getFields().get(0).getDouble_minVal();
                SpectrumQueryCategoryContainer panel = new SpectrumQueryCategoryContainer(qcc, this.queryController);
                spectrumQueryCategoryContainer.put(qcc.getCategoryName(), panel);
                add(panel);
            }
            }
        revalidate();
        repaint();
        }

        private class SpectrumQueryCategoryContainer extends JPanel{
        private QueryCategoryContainer queryCategoryContainer;
        private JPanel fieldPanel;
        private SpectrumQueryComponentFactory spectrumQueryComponentFactory;
        private SPECCHIOClient specchioClient;
        private ArrayList<SpectrumQueryComponent> spectrumQueryComponents;

        public SpectrumQueryCategoryContainer(QueryCategoryContainer queryCategoryContainer, SPECCHIOClient specchioClient){
            super();
            this.queryCategoryContainer = queryCategoryContainer;
            this.specchioClient = specchioClient;
            spectrumQueryComponents = new ArrayList<SpectrumQueryComponent>();

            // add a border with the category name
            Border blackline = BorderFactory.createLineBorder(Color.BLACK);
            TitledBorder tb = BorderFactory.createTitledBorder(blackline, qcc.getCategoryName());
            setBorder(tb);

            // create a panel to hold the fields
            fieldPanel = new JPanel();
            fieldPanel.setLayout(new AlignedBoxLayout(fieldPanel, AlignedBoxLayout.Y_AXIS));
            add(fieldPanel);

            //add fields
            spectrumQueryComponentFactory = new SpectrumQueryComponentFactory(this, this.specchioClient);
            for(QueryField queryField :  this.queryCategoryContainer.getFields()){

            }

        }

    }
    
}
