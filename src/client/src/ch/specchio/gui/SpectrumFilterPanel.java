package ch.specchio.gui;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.metadata.*;
import ch.specchio.query_builder.*;
import ch.specchio.types.*;
import com.jidesoft.swing.RangeSlider;
import org.apache.commons.lang3.ObjectUtils;
//import org.freixas.jcalendar.DateEvent;
//import org.freixas.jcalendar.DateListener;
//import org.freixas.jcalendar.JCalendar;
//import org.freixas.jcalendar.JCalendarCombo;
import org.joda.time.DateTime;
import org.w3c.dom.ranges.Range;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.*;

public class SpectrumFilterPanel extends JPanel {
    
    private java.awt.Frame ownerReference;
    private MDE_Controller mdeController;
    private QueryController queryController;
    private MDE_Form mdeForm;
    private QueryForm qeForm;
    Hashtable<String, SpectrumQueryCategoryContainer>  spectrumQueryCategoryContainers;
    private ArrayList<Category> availableCats;
    private ArrayList<attribute> availableAttr;
    private HashMap<String, attribute> attributeMap;
    private ArrayList<String> categoryStrings;
    private ArrayList<String> attributeStrings;
    
    public SpectrumFilterPanel(java.awt.Frame myOwner, MDE_Controller mdeController, QueryController myController){
        super();
        spectrumQueryCategoryContainers = new Hashtable<String, SpectrumQueryCategoryContainer>();
        this.ownerReference = myOwner;
        this.mdeController = mdeController;
        this.queryController = myController;
        this.availableCats = new ArrayList<>();
        this.availableAttr = new ArrayList<>();
        this.attributeStrings = new ArrayList<>();
        this.categoryStrings = new ArrayList<>();
        this.attributeMap = new HashMap<>();

        this.mdeForm = mdeController.getForm();
        this.qeForm = myController.getForm();

//        setForm(this.qeForm);
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public void updateCategories(ArrayList<Category> newCategories, ArrayList<attribute> newAttributes){
        this.availableCats = newCategories;
        this.availableAttr = newAttributes;
        for(attribute att : availableAttr){
            this.attributeMap.put(att.getName(), att);
            this.attributeStrings.add(att.getName());
        }
        for(Category cat : availableCats) {
            categoryStrings.add(cat.name);
        }
        update();
    }


    private void fireConditionChanged(QueryField queryFieldfield, Object value) {
        queryController.ConditionChange(queryFieldfield, value);
    }

    private void fireConditionChanged(QueryField queryFieldfieldLower, Object valueLower, QueryField queryFieldfieldHigher, Object valueHigher) {
        queryController.ConditionChange(queryFieldfieldLower, valueLower, queryFieldfieldHigher, valueHigher);
    }
    /**
     * Set the query form to be displayed by the panel.
     *
     * @param form	the form
     *
     */
    public void setForm(QueryForm form) {

        // remove all of the existing components
        removeAll();

        if (form != null) {

            // create and add panels for each category container
            for (QueryCategoryContainer qcc : form.getCategoryContainers()) {
                SpectrumFilterPanel.SpectrumQueryCategoryContainer panel = new SpectrumFilterPanel.SpectrumQueryCategoryContainer(qcc, this.queryController.getSpecchio_client());
                spectrumQueryCategoryContainers.put(qcc.getCategoryName(), panel);
                add(panel);
            }

        }

        // save a reference to the new form
        //this.form = form;

        // force re-draw
        revalidate();
        repaint();

    }


    /**
     * Category container panel.
     */
    private class SpectrumQueryCategoryContainer extends JPanel {

        /** serialisation version identifier */
        private static final long serialVersionUID = 1L;

        /** metadata category container */
        private QueryCategoryContainer qcc;

        /** inner panel */
        private JPanel fieldPanel;

        /** component factory */
        private SpectrumFilterPanel.SpectrumQueryComponentFactory factory;

        /** the client object */
        public SPECCHIOClient specchioClient;

        ArrayList<SpectrumFilterPanel.SpectrumQueryComponent> spectrumQueryComponents;


        /**
         * Constructor.
         *
         */
        public SpectrumQueryCategoryContainer(QueryCategoryContainer qcc, SPECCHIOClient specchioClient) {

            super();

            // save a reference to the parameters
            this.qcc = qcc;

            this.specchioClient = specchioClient;
            spectrumQueryComponents = new ArrayList<SpectrumFilterPanel.SpectrumQueryComponent>();

            // add a border with the category name
            Border blackline = BorderFactory.createLineBorder(Color.BLACK);
            TitledBorder tb = BorderFactory.createTitledBorder(blackline, qcc.getCategoryName());
            setBorder(tb);

            // create a panel to hold the fields
            fieldPanel = new JPanel();
            fieldPanel.setLayout(new AlignedBoxLayout(fieldPanel, AlignedBoxLayout.Y_AXIS));
            add(fieldPanel);

            // add fields
            factory = new SpectrumFilterPanel.SpectrumQueryComponentFactory(this, specchioClient);
            ListIterator<QueryField> iter = qcc.getFields().listIterator();
            while (iter.hasNext()) {

                QueryField field = iter.next();


                // create a component for this field
                SpectrumFilterPanel.SpectrumQueryComponent c = null;
                if (field.getClientProperty("upper_bounds_cond") != null) {
                    // this field and the next form a lower and upper bound pair
                    c = factory.newComponent(field, iter.next());
                } else {
                    // this field is a solo field
                    c = factory.newComponent(field);
                }

                if (c != null) {
                    // add the new component to the panel
                    fieldPanel.add(c);
                    spectrumQueryComponents.add(c);
                }

            }

        }

    }


    /**
     * Factory class for creating spectrum metadata components
     */
    private class SpectrumQueryComponentFactory {

        /** the category container panel */
        private SpectrumFilterPanel.SpectrumQueryCategoryContainer container;

        /** the client object */
        public SPECCHIOClient specchioClient;

        /**
         * Constructor.
         *
         * @param container	the category container panel to which components will belong
         */
        public SpectrumQueryComponentFactory(SpectrumFilterPanel.SpectrumQueryCategoryContainer container, SPECCHIOClient specchioClient) {

            // save a reference to the parameters for later
            this.container = container;
            this.specchioClient = specchioClient;
        }


        /**
         * Create a new component for a query field.
         *
         * @param field	the query field to be represented by the new component
         *
         * @return a SpectrumQueryComponent corresponding to the query field
         */
        public SpectrumFilterPanel.SpectrumQueryComponent newComponent(QueryField field) {

            if (field instanceof SpectrumQueryField) {
                return newFieldComponent((SpectrumQueryField)field);
            } else if (field instanceof EAVQueryField) {
                return newEavComponent((EAVQueryField)field);
            } else {
                // this should never happen
                return null;
            }

        }


        /**
         * Create a new component for a query field with a lower and upper bound.
         *
         * @param lower	the lower bound field
         * @param upper	the upper bound field
         *
         * @return a SpectrumQueryComponent corresponding to the query fields
         */
        public SpectrumFilterPanel.SpectrumQueryComponent newComponent(QueryField lower, QueryField upper) {

            if (lower instanceof SpectrumQueryField) {
                return newFieldComponent((SpectrumQueryField)lower, (SpectrumQueryField)upper);
            } else if (lower instanceof EAVQueryField) {
                if(attributeStrings.contains(lower.getLabel())) {
                    return newEavComponent((EAVQueryField) lower, (EAVQueryField) upper);
                }else{
                    return null;
                }
            } else {
                // this should never happen
                return null;
            }

        }


        /**
         * Create a new component for an EAV field.
         *
         * @param field	the metadata field to be represented by the new component
         *
         * @return a new SpectrumEavQueryComponent corresponding to the new field
         */
        private SpectrumFilterPanel.SpectrumEavQueryComponent newEavComponent(EAVQueryField field) {

            if ("string_val".equals(field.get_fieldname())) {
                return new SpectrumFilterPanel.SpectrumStringEavQueryComponent(container, field);
            } else if ("int_val".equals(field.get_fieldname())) {
                return new SpectrumFilterPanel.SpectrumIntEavQueryComponent(container, field);
            } else if ("double_val".equals(field.get_fieldname())) {
                return new SpectrumFilterPanel.SpectrumDoubleEavQueryComponent(container, field);
            } else if ("datetime_val".equals(field.get_fieldname())) {
                return new SpectrumFilterPanel.SpectrumDateEavQueryComponent(container, field);
            } else if ("taxonomy_id".equals(field.get_fieldname())) {
                return new SpectrumFilterPanel.SpectrumTaxonomyEavQueryComponent(container, field, specchioClient);
            } else if ("spatial_val".equals(field.get_fieldname())) {
                return new SpectrumFilterPanel.SpectrumSpatialEavQueryComponent(container, field, specchioClient);
            } else {
                // this should never happen unless a new field is added to eav table
                return null;
            }

        }


        /**
         * Create a new component for an EAV field with a lower and upper bound.
         *
         * @param lower	the lower bound field
         * @param upper	the upper bound field
         *
         * @return a SpectrumQueryComponent corresponding to the query fields
         */
        public SpectrumFilterPanel.SpectrumQueryComponent newEavComponent(EAVQueryField lower, EAVQueryField upper) {

            if ("int_val".equals(lower.get_fieldname())) {
                return new SpectrumFilterPanel.SpectrumIntEavQueryComponent(container, lower, upper);
            } else if ("double_val".equals(lower.get_fieldname())) {
                return new SpectrumFilterPanel.SpectrumDoubleEavQueryComponent(container, lower, upper);
            } else if ("datetime_val".equals(lower.get_fieldname())) {
                return new SpectrumFilterPanel.SpectrumDateEavQueryComponent(container, lower, upper);
            } else {
                // this should never happen
                return null;
            }

        }


        /**
         * Create a new component for a non-EAV field.
         *
         * @param field	the metadata field to be represented by the new component
         *
         * @return a new SpectrumFieldQueryComponent corresponding to the new field
         */
        private SpectrumFilterPanel.SpectrumFieldQueryComponent newFieldComponent(SpectrumQueryField field) {

            return new SpectrumFilterPanel.SpectrumFieldQueryComponent(container, field);

        }


        /**
         * Create a new component for a non-EAV field with a lower and upper bound.
         *
         * @param lower	the lower bound field
         * @param upper	the upper bound field
         *
         * @return a SpectrumQueryComponent corresponding to the query fields
         */
        public SpectrumFilterPanel.SpectrumQueryComponent newFieldComponent(SpectrumQueryField lower, SpectrumQueryField upper) {

            return new SpectrumFilterPanel.SpectrumFieldQueryComponent(container, lower, upper);

        }

    }


    /**
     * Base class for all spectrum query fields.
     */
    private abstract class SpectrumQueryComponent extends JPanel implements AlignedBoxLayout.AlignedBox {

        /** serialisation version identifier */
        private static final long serialVersionUID = 1L;

        /** the category container panel to which this component belongs */
        private SpectrumFilterPanel.SpectrumQueryCategoryContainer container;

        /** the fields that make up this component */
        private QueryField fields[];

        /** the label of this component */
        private JLabel label;

        /** the panel that contains the controls */
        private JPanel controlPanel;

        /** the client object */
        public SPECCHIOClient specchioClient;


        /**
         * Constructor for a range query.
         *
         * @param container	the category container panel to which this component belongs
         * @param fields	the fields that make up this component
         */
        protected SpectrumQueryComponent(SpectrumFilterPanel.SpectrumQueryCategoryContainer container, QueryField ... fields) {

            // save a reference to the parameters
            this.container = container;
            this.fields = fields;

            // create a label for this component
            if (fields.length > 0) {
                label = new JLabel(fields[0].getLabel());
            } else {
                label = new JLabel("");
            }
            label.setHorizontalAlignment(JLabel.RIGHT);
            add(label);

            // create a panel for the controls
            controlPanel = new JPanel();
            add(controlPanel);

        }


        /**
         * Constructor without a range.
         *
         * @param container	the category container panel to which this component belongs
         * @param field		the query field
         */
        protected SpectrumQueryComponent(SpectrumFilterPanel.SpectrumQueryCategoryContainer container, QueryField field) {

            this(container, field, null);

        }


        /**
         * Get the panel that contains the controls.
         *
         * @return a reference to the panel
         */
        public JPanel getControlPanel() {

            return controlPanel;

        }


        /**
         * Get a reference to a field.
         *
         * @param i		the index of the field to retrieve
         *
         * @return a reference to the i'th field of this component
         */
        public QueryField getField(int i) {

            return fields[i];

        }


        /**
         * Get the horizontal alignment position.
         *
         * @return the preferred width of the label
         */
        public int getXAlignmentPosition() {

            Dimension dim = label.getPreferredSize();

            return dim.width;

        }

        /**
         * Get the vertical alignment position. Not used.
         *
         * @return 0
         */
        public int getYAlignmentPosition() {

            return 0;

        }

    }


    /**
     * Component for non-EAV query fields.
     */
    private class SpectrumFieldQueryComponent extends SpectrumFilterPanel.SpectrumQueryComponent implements ActionListener {

        /** serialisation version identifier */
        private static final long serialVersionUID = 1L;

        /** the combo box */
        private JComboBox box;


        /**
         * Constructor.
         *
         * @param container	the category container panel to which this component belongs
         * @param field		the query field represented by this component
         */
        public SpectrumFieldQueryComponent(SpectrumFilterPanel.SpectrumQueryCategoryContainer container, SpectrumQueryField field) {

            super(container, field);

            // build combobox
            box = new JComboBox();
            box.addActionListener(this);
            getControlPanel().add(box);

            // add a "nil" item for no selection
            combo_table_data nil_item = new combo_table_data("NIL", 0);
            box.addItem(nil_item);

            // add all of the possible values for this field
            CategoryTable items = ((SpectrumQueryField) field).getCategoryValues();
            Enumeration<Integer> e = items.keys();
            while(e.hasMoreElements())
            {
                Integer key = e.nextElement();
                String value = items.get(key);
                combo_table_data cdt = new combo_table_data(value, key);
                box.addItem(cdt);
            }

            // start with "nil" selected
            box.setSelectedItem(nil_item);

        }


        /**
         * Constructor for range queries. There are currently no range queries possible for
         * spectrum fields, so this constructor just behaves the same as the single-field
         * constructor using the "lower" field.
         *
         * @param container	the category container panel to which this component belongs
         * @param lower		the lower bound query field
         * @param upper		the upper bound query field (ignored)
         */
        public SpectrumFieldQueryComponent(SpectrumFilterPanel.SpectrumQueryCategoryContainer container, SpectrumQueryField lower, SpectrumQueryField upper) {

            this(container, lower);

        }


        /**
         * Combo box selection handler.
         *
         * @param event	the event to be handled
         */
        public void actionPerformed(ActionEvent event) {

            combo_table_data cdt = (combo_table_data) box.getSelectedItem();
            fireConditionChanged(getField(0), cdt.id);

        }

    }


    /**
     * Base class for EAV query components.
     */
    private abstract class SpectrumEavQueryComponent extends SpectrumFilterPanel.SpectrumQueryComponent {

        /** serialisation version identifier */
        private static final long serialVersionUID = 1L;


        /**
         * Constructor.
         *
         * @param container	the category container panel to which this component belongs
         * @param field		the query field represented by this component
         */
        public SpectrumEavQueryComponent(SpectrumFilterPanel.SpectrumQueryCategoryContainer container, EAVQueryField field) {

            super(container, field);

        }


        /**
         * Constructor for range queries.
         *
         * @param container	the category container panel to which this component belongs
         * @param lower		the lower bound query field
         * @param upper		the upper bound query field
         */
        public SpectrumEavQueryComponent(SpectrumFilterPanel.SpectrumQueryCategoryContainer container, EAVQueryField lower, EAVQueryField upper) {

            super(container, lower, upper);

        }

    }


    /**
     * Query component for string-valued fields.
     */
    private class SpectrumStringEavQueryComponent extends SpectrumFilterPanel.SpectrumEavQueryComponent implements DocumentListener {

        /** serialisation version identifier */
        private static final long serialVersionUID = 1L;

        /** input field */
        KeylistenerTextField inputField;


        /**
         * Constructor.
         *
         * @param container	the category container panel to which this component belongs
         * @param field		the query field represented by this component
         */
        public SpectrumStringEavQueryComponent(SpectrumFilterPanel.SpectrumQueryCategoryContainer container, EAVQueryField field) {

            super(container, field);

            // create the input field
            inputField = new  KeylistenerTextField(23);
            inputField.getDocument().addDocumentListener(this);
            getControlPanel().add(inputField);

        }


        /**
         * Handle a document update event
         *
         * @param event	the event to be handled
         */
        @Override
        public void changedUpdate(DocumentEvent event) {

            fireConditionChanged(getField(0), inputField.getText());

        }


        /**
         * Handle an insert event.
         *
         * @param event	the event to be handled
         */
        @Override
        public void insertUpdate(DocumentEvent event) {

            fireConditionChanged(getField(0), inputField.getText());

        }


        /**
         * Handle a remove event.
         *
         * @param event	the event to be handled
         */
        @Override
        public void removeUpdate(DocumentEvent event) {

            fireConditionChanged(getField(0), inputField.getText());

        }

    }


    /**
     * Base class for number-valued fields.
     */
    private abstract class SpectrumNumericEavQueryComponent extends SpectrumFilterPanel.SpectrumEavQueryComponent implements DocumentListener, ChangeListener, MouseListener {

        /** serialisation version identifier */
        private static final long serialVersionUID = 1L;

        /** lower bound input field */
        private KeylistenerTextFieldNumeric lowerBoundField;

        /** upper bound input field */
        private KeylistenerTextFieldNumeric upperBoundField;

        /** name for the "owner" property */
        private static final String OWNER = "owner";

        private RangeSlider rangeSlider;


        /**
         * Constructor.
         *
         * @param container	the category container panel to which this component belongs
         * @param lower		the lower bound query field
         * @param upper		the upper bound query field
         */
        public SpectrumNumericEavQueryComponent(SpectrumFilterPanel.SpectrumQueryCategoryContainer container, EAVQueryField lower, EAVQueryField upper) {

            super(container, lower, upper);
            rangeSlider = null;
            try {
                attribute thisAttribute = attributeMap.get(lower.getLabel());
                String valueIndicator = thisAttribute.getDefaultStorageField();
                int minVal = Integer.MIN_VALUE;
                int maxVal = Integer.MAX_VALUE;
                int val;
                switch (valueIndicator) {
                    case "int_val":
                        val = Integer.valueOf(thisAttribute.getMIN_INT_VAL());
                        minVal = (val != -999) ? val : 0;
                        maxVal = Integer.valueOf(thisAttribute.getMAX_INT_VAL());
                        break;
                    case "double_val":
                        val = (int) Math.floor(Double.valueOf(thisAttribute.getMIN_DOUBLE_VAL()));
                        minVal = (val != -999) ? val : 0;
                        maxVal = (int) Math.ceil(Double.valueOf(thisAttribute.getMAX_DOUBLE_VAL()));
                        break;
                    default:
                        break;
                }
                if (minVal > Integer.MIN_VALUE && maxVal < Integer.MAX_VALUE) {
                    rangeSlider = new RangeSlider(minVal, maxVal, minVal, maxVal);
                    rangeSlider.setPaintTicks(true);
                    rangeSlider.setPaintLabels(true);
                    NiceScale niceScale = new NiceScale(minVal, maxVal);

//                    double range = maxVal - minVal;
//                    int exponent = (int) Math.log10(range);
//                    double magnitude = Math.pow(10, exponent);
//                    double tickNumber = magnitude / 10;
//                    int tickSpacing = (int) Math.round(tickNumber);
                    rangeSlider.setMajorTickSpacing((int)niceScale.getTickSpacing());
                    rangeSlider.addChangeListener(this);
                    rangeSlider.addMouseListener(this);
                    getControlPanel().add(rangeSlider);
                }

            } catch (NullPointerException ex){
                System.out.println("COULDN'T CREATE RANGESLIDER");
            }

            lowerBoundField = new KeylistenerTextFieldNumeric(10);
            lowerBoundField.getDocument().addDocumentListener(this);
            lowerBoundField.getDocument().putProperty(OWNER, lowerBoundField);

            if (upper != null) {

                // add a dash to separate the fields
//                JLabel dash = new JLabel(" - ");
//                getControlPanel().add(dash);


                // create the upper input field
                upperBoundField = new KeylistenerTextFieldNumeric(10);
                upperBoundField.getDocument().addDocumentListener(this);
                upperBoundField.getDocument().putProperty(OWNER, upperBoundField);
            }else {

                upperBoundField = null;

            }

            if (rangeSlider == null) {
                getControlPanel().add(lowerBoundField);
                if(upperBoundField != null){
                    getControlPanel().add(upperBoundField);
                }
            }

        }


        /**
         * Handle a document update.
         *
         * @param event	the event to be handled
         */
        private void documentUpdated(DocumentEvent event) {

            Object owner = event.getDocument().getProperty(OWNER);
            if (owner != null) {
                if (owner == lowerBoundField) {
                    // the lower bound was changed
                    fireConditionChanged(getField(0), getValidatedNumber(lowerBoundField.getText()));
                } else if (owner == upperBoundField) {
                    // the upper bound was changed
                    fireConditionChanged(getField(1), getValidatedNumber(upperBoundField.getText()));
                }
            }

        }


        /**
         * Handle a document update event
         *
         * @param event	the event to be handled
         */
        @Override
        public void changedUpdate(DocumentEvent event) {

            documentUpdated(event);

        }

        /**
         * Validate an input string.
         *
         * @param s	the input string
         *
         * @return an object corresponding to the input string, or null if the string is not valid input
         */
        protected abstract Number getValidatedNumber(String s);


        /**
         * Handle an insert event.
         *
         * @param event	the event to be handled
         */
        @Override
        public void insertUpdate(DocumentEvent event) {

            documentUpdated(event);

        }


        /**
         * Handle a remove event.
         *
         * @param event	the event to be handled
         */
        @Override
        public void removeUpdate(DocumentEvent event) {

            documentUpdated(event);

        }


        /**
         * Set whether or not the input fields will accept entry of
         * floating point numbers.
         *
         * @param floating	true to allow entry of floating-point numbers, false to allow entry of integers only
         */
        public void setAllowFloats(boolean floating) {

            lowerBoundField.setAllowFloats(floating);
            upperBoundField.setAllowFloats(floating);

        }

        @Override
        public void stateChanged(ChangeEvent e) {

        }

        @Override
        public void mouseClicked(MouseEvent e) {

        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if(e.getSource() instanceof RangeSlider){
                fireConditionChanged(getField(0), rangeSlider.getLowValue(), getField(1), rangeSlider.getHighValue());
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }
    }


    /**
     * Query component for integer-valued fields.
     */
    private class SpectrumIntEavQueryComponent extends SpectrumFilterPanel.SpectrumNumericEavQueryComponent implements DocumentListener {

        /** serialisation version identifier */
        private static final long serialVersionUID = 1L;


        /**
         * Constructor for a range query.
         *
         * @param container	the category container panel to which this component belongs
         * @param lower		the lower bound query field
         * @param upper		the upper bound query field
         */
        public SpectrumIntEavQueryComponent(SpectrumFilterPanel.SpectrumQueryCategoryContainer container, EAVQueryField lower, EAVQueryField upper) {

            super(container, lower, upper);

            setAllowFloats(false);

        }


        /**
         * Constructor without a range.
         *
         * @param container	the category container panel to which this component belongs
         * @param field		the query field
         */
        public SpectrumIntEavQueryComponent(SpectrumFilterPanel.SpectrumQueryCategoryContainer container, EAVQueryField field) {

            this(container, field, null);

        }


        /**
         * Convert a string into an Integer object.
         *
         * @param s	the input string
         *
         * @return an Integer object corresponding to the string, or null if the string is not a valid integer
         */
        protected Number getValidatedNumber(String s) {

            Integer i = null;
            try {
                i = Integer.parseInt(s);
            }
            catch (NumberFormatException ex) {
                // don't need to do anything because i is already null
            }

            return i;

        }

    }


    /**
     * Query component for double-valued fields.
     */
    private class SpectrumDoubleEavQueryComponent extends SpectrumFilterPanel.SpectrumNumericEavQueryComponent implements DocumentListener {

        /** serialisation version identifier */
        private static final long serialVersionUID = 1L;


        /**
         * Constructor.
         *
         * @param container	the category container panel to which this component belongs
         * @param lower		the lower bound query field
         * @param upper		the upper bound query field
         */
        public SpectrumDoubleEavQueryComponent(SpectrumFilterPanel.SpectrumQueryCategoryContainer container, EAVQueryField lower, EAVQueryField upper) {

            super(container, lower, upper);

            setAllowFloats(true);

        }


        /**
         * Constructor without a range.
         *
         * @param container	the category container panel to which this component belongs
         * @param field		the query field
         */
        public SpectrumDoubleEavQueryComponent(SpectrumFilterPanel.SpectrumQueryCategoryContainer container, EAVQueryField field) {

            this(container, field, null);

        }


        /**
         * Convert a string into an Double object.
         *
         * @param s	the input string
         *
         * @return a Double object corresponding to the string, or null if the string is not a valid integer
         */
        protected Number getValidatedNumber(String s) {

            Double d = null;
            try {
                d = Double.parseDouble(s);
            }
            catch (NumberFormatException ex) {
                // don't need to do anything because d is already null
            }

            return d;

        }

    }


    /**
     * class for date fields.
     */
    private class SpectrumDateEavQueryComponent extends SpectrumFilterPanel.SpectrumEavQueryComponent {

        /** serialisation version identifier */
        private static final long serialVersionUID = 1L;

        /** start date combo */
        private JCalendarCombo startDateField;

        /** end date combo */
        private JCalendarCombo endDateField;


        /**
         * Constructor for a range query.
         *
         * @param container	the category container panel to which this component belongs
         * @param lower		the lower bound query field
         * @param upper		the upper bound query field
         */
        public SpectrumDateEavQueryComponent(SpectrumFilterPanel.SpectrumQueryCategoryContainer container, EAVQueryField lower, EAVQueryField upper) {

            super(container, lower, upper);

            // create the start date combo
            startDateField = new JCalendarCombo(JCalendar.DISPLAY_DATE | JCalendar.DISPLAY_TIME, false);
            startDateField.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
            startDateField.addDateListener(new SpectrumFilterPanel.SpectrumDateEavQueryComponent.JCalendarComboListener(startDateField, lower));
            getControlPanel().add(startDateField);

            if (upper != null) {

                // create a dash to separate the fields
                JLabel dash = new JLabel(" - ");
                getControlPanel().add(dash);

                // create the end date combo
                endDateField = new JCalendarCombo(JCalendar.DISPLAY_DATE | JCalendar.DISPLAY_TIME, false);
                endDateField.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
                endDateField.addDateListener(new SpectrumFilterPanel.SpectrumDateEavQueryComponent.JCalendarComboListener(endDateField, upper));
                getControlPanel().add(endDateField);

            } else {

                endDateField = null;

            }

        }


        /**
         * Constructor without a range.
         *
         * @param container	the category container panel to which this component belongs
         * @param field		the query field
         */
        public SpectrumDateEavQueryComponent(SpectrumFilterPanel.SpectrumQueryCategoryContainer container, EAVQueryField field) {

            this(container, field, null);

        }


        /**
         * Format a date for transmission to the query controller.
         *
         * @param date	the date
         *
         * @return a string representing the input date
         */
        public String formatDate(Date date) {

            TimeZone tz = TimeZone.getDefault();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            formatter.setTimeZone(tz);

            return formatter.format(date);

        }

        /**
         * Format a date for transmission to the query controller.
         *
         * @param date	the date
         *
         * @return a string representing the input date
         */
        public String formatDate(DateTime date) {

            TimeZone tz = TimeZone.getDefault();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            formatter.setTimeZone(tz);

            return formatter.format(date);

        }


        /**
         * Date change listener.
         */
        private class JCalendarComboListener implements DateListener {

            /** the calendar control to which this object is listening */
            private JCalendarCombo cal;

            /** the field represented by this control */
            private EAVQueryField field;

            /**
             * Constructor.
             *
             * @param cal	the combo box to which to listen
             */
            public JCalendarComboListener(JCalendarCombo cal, EAVQueryField field) {

                this.cal = cal;
                this.field = field;

            }

            /**
             * Handle a change of date.
             *
             * @param event	the event to be handled
             */
            @Override
            public void dateChanged(DateEvent event) {

                fireConditionChanged(field, formatDate(cal.getDate()));

            }

        }



    }


    /**
     * class for taxomony fields.
     */
    private class SpectrumTaxonomyEavQueryComponent extends SpectrumFilterPanel.SpectrumEavQueryComponent implements ActionListener {

        /** serialisation version identifier */
        private static final long serialVersionUID = 1L;
        private JButton selectButton;


        /**
         * Constructor for a range query.
         *
         * @param container	the category container panel to which this component belongs
         * @param field		query field
         * @param specchioClient		specchio client
         */
        public SpectrumTaxonomyEavQueryComponent(SpectrumFilterPanel.SpectrumQueryCategoryContainer container, EAVQueryField field, SPECCHIOClient specchioClient) {

            super(container, field, null);
            this.specchioClient = specchioClient;

            // create taxonomy field
            String displayString = "NIL";
            selectButton = new JButton(displayString);

            selectButton.setActionCommand("SELECT");
            selectButton.addActionListener(this);

            getControlPanel().add(selectButton);
        }



        @Override
        public void actionPerformed(ActionEvent event) {
            if ("SELECT".equals(event.getActionCommand())) {

                try {

                    QueryField f = this.getField(0);

                    String tax_name = f.getLabel();

                    attribute attr = specchioClient.getAttributesNameHash().get(tax_name);

                    MetaTaxonomy mp = (MetaTaxonomy) MetaTaxonomy.newInstance(attr);

                    // show the taxonomy selection dialog
                    TaxonomySelectionDialog d = new TaxonomySelectionDialog(ownerReference, specchioClient, mp);
                    d.setLocation(selectButton.getLocationOnScreen());
                    d.setVisible(true);

                    int tax_id = d.getSelectedTaxonomyId();
                    if (tax_id > 0) {
                        Long taxonomy_id = (long) tax_id;

                        // update value of taxonomy button
                        TaxonomyNodeObject taxonomy;
                        taxonomy = specchioClient.getTaxonomyNode(tax_id);
                        selectButton.setText(taxonomy.getName());
                        selectButton.setToolTipText(taxonomy.getDescription());


                        // notify listeners of the change
                        fireConditionChanged(f, tax_id);
                    }

                } catch (SPECCHIOClientException ex) {
                    ErrorDialog error = new ErrorDialog(ownerReference, "Could not retrieve taxonomy", ex.getUserMessage(), ex);
                    error.setVisible(true);
                }

            }

        }



    }


    /**
     * class for spatial fields.
     */
    private class SpectrumSpatialEavQueryComponent extends SpectrumFilterPanel.SpectrumEavQueryComponent implements TableModelListener {

        /** serialisation version identifier */
        private static final long serialVersionUID = 1L;
        private ch.specchio.gui.EditableTableModel table_model;
        private JTable table;


        /**
         * Constructor for a range query.
         * @param container	the category container panel to which this component belongs
         * @param field		query field
         * @param specchioClient		specchio client
         */
        public SpectrumSpatialEavQueryComponent(SpectrumFilterPanel.SpectrumQueryCategoryContainer container, EAVQueryField field, SPECCHIOClient specchioClient) {

            super(container, field, null);
            this.specchioClient = specchioClient;

            // create a table with 4 fields for the corner coordinates of a rectangle (simple polygon version)

            table_model = new EditableTableModel();
            table_model.setEditDisabledCell(0);

            // Create column
            table_model.addColumn("Corners");
            table_model.addColumn("Latitude");
            table_model.addColumn("Longitude");

            table_model.addRow(new Object[]{"Upper Left", "90", "180"});
            table_model.addRow(new Object[]{"Lower Right", "-90", "-180"});

//			table_model.setValueAt("Upper Left", 0, 0);
//			table_model.setValueAt("Lower Right", 1, 0);

            table_model.addTableModelListener(this);
            table = new JTable(table_model);


            String displayString = "";
            JTextField text = new JTextField(displayString, 30);
            Dimension size = text.getPreferredSize();

            size.height = size.height + 10 * table_model.getRowCount();

            table.setPreferredScrollableViewportSize(size);
            table.setFillsViewportHeight(true);

            JScrollPane scrollPane = new JScrollPane(table);

            getControlPanel().add(scrollPane);
        }



        @Override
        public void tableChanged(TableModelEvent arg0) {
            // create 2D double array
            ArrayListWrapper<Point2D> coords = new ArrayListWrapper<Point2D>();

            Vector<Vector<String>> vector = this.table_model.getDataVector();

            for(int row=0;row<vector.size();row++)
            {
                Vector<String> entry = vector.get(row);

                String lat_str = (String) entry.get(1);
                String lon_str = (String) entry.get(2);

                Point2D coord = new Point2D(0.0, 0.0);

                // convert to double
                try
                {
                    coord.setY(Double.valueOf(lat_str));
                } catch(NullPointerException e)
                {

                }
                catch(NumberFormatException e)
                {


                    // should show a warning to the user, e.g. colour the field red ...
                }

                try
                {
                    coord.setX(Double.valueOf(lon_str));
                } catch(NullPointerException e)
                {

                }
                catch(NumberFormatException e)
                {


                    // should show a warning to the user, e.g. colour the field red ...
                }

                coords.getList().add(coord);

            }

            // build proper rectangle from UL and LR points
            ArrayListWrapper<Point2D> rect_coords = new ArrayListWrapper<Point2D>();

            Point2D UL = coords.getList().get(0);
            Point2D LR = coords.getList().get(1);
            Point2D UR = new Point2D(UL.getY(), LR.getX());
            Point2D LL = new Point2D(LR.getY(), UL.getX());

            rect_coords.getList().add(UL);
            rect_coords.getList().add(LR);
            rect_coords.getList().add(UR);
            rect_coords.getList().add(LL);

            MetaSpatialPolygon mp = new MetaSpatialPolygon();
            try {
                mp.setValue(rect_coords);
            } catch (MetaParameterFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            fireConditionChanged(this.getField(0), mp.getEAVValue());

        }



    }


    public void update() {

        QueryForm form = queryController.getForm();

        // remove all of the existing components
        removeAll();

        if (form != null & categoryStrings.size() > 0) {
            // create and add panels for each category container
            for (QueryCategoryContainer qcc : form.getCategoryContainers()) {
                if (categoryStrings.contains(qcc.getCategoryName())) {
                    SpectrumFilterPanel.SpectrumQueryCategoryContainer panel = new SpectrumFilterPanel.SpectrumQueryCategoryContainer(qcc, this.queryController.getSpecchio_client());
                    spectrumQueryCategoryContainers.put(qcc.getCategoryName(), panel);
                    add(panel);
                }
            }

        }

        // force re-draw
        revalidate();
        repaint();


    }


}
