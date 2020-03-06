package ch.specchio.gui;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.metadata.*;
import ch.specchio.query_builder.*;
import ch.specchio.types.ConflictInfo;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.Hashtable;

public class SpectrumFilterPanel extends JPanel {
    
    private Frame ownerReference;
    private MDE_Controller mdeController;
    private QueryController queryController;
    private MDE_Form mdeForm;
    private QueryForm qeForm;
    Hashtable<String, SpectrumQueryCategoryContainer>  spectrumQueryCategoryContainer;
    
    public SpectrumFilterPanel(Frame myOwner, MDE_Controller mdeController, QueryController myController){
        super();
        spectrumQueryCategoryContainer = new Hashtable<String, SpectrumQueryCategoryContainer>();
        this.ownerReference = myOwner;
        this.mdeController = mdeController;
        this.queryController = myController;
        this.mdeForm = mdeController.getForm();
        this.qeForm = myController.getForm();

        setForm(this.mdeForm, this.qeForm);
        
//        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public void updateForm(MDE_Form form){
        mdeForm = mdeController.getForm();
        // TEST TO SEE IF CONFLICT TABLE WAS GENERATED
        for(MD_CategoryContainer mdcc : mdeForm.getContainers()){
            for(MD_Field mdf :  mdcc.getFields()){
                ConflictInfo info = mdf.getConflict();
                System.out.println("Double Min = " + info.double_val_min + " Double Max = " + info.double_val_max);
            }
        }
        System.out.println("UPDATE FILTER FORM");
        setForm(this.mdeForm, this.qeForm);
    }


    private void fireConditionChanged(QueryField queryFieldfield, Object value) {
        queryController.ConditionChange(queryFieldfield, value);
    }
    
    public void setForm(MDE_Form metadataForm, QueryForm queryForm){
        removeAll();
        
        if(queryForm != null){
            // create and add panels for each category container
            for(QueryCategoryContainer qcc : queryForm.getCategoryContainers()) {
//                double min = (EAVQueryField) qcc.getFields().get(0).getDouble_minVal();
                SpectrumQueryCategoryContainer panel = new SpectrumQueryCategoryContainer(qcc);
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
//        private SpectrumQueryComponentFactory spectrumQueryComponentFactory;
//        private SPECCHIOClient specchioClient;
//        private ArrayList<SpectrumQueryComponent> spectrumQueryComponents;

        public SpectrumQueryCategoryContainer(QueryCategoryContainer queryCategoryContainer){
            super();
            this.queryCategoryContainer = queryCategoryContainer;
//            this.specchioClient = specchioClient;
//            spectrumQueryComponents = new ArrayList<SpectrumQueryComponent>();

            // add a border with the category name
            Border blackline = BorderFactory.createLineBorder(Color.BLACK);
//            TitledBorder tb = BorderFactory.createTitledBorder(blackline, qcc.getCategoryName());
//            setBorder(tb);

            // create a panel to hold the fields
            fieldPanel = new JPanel();
            fieldPanel.setLayout(new AlignedBoxLayout(fieldPanel, AlignedBoxLayout.Y_AXIS));
            add(fieldPanel);

            //add fields
//            spectrumQueryComponentFactory = new SpectrumQueryComponentFactory(this, this.specchioClient);
            for(QueryField queryField :  this.queryCategoryContainer.getFields()){

            }

        }

    }
    
}
