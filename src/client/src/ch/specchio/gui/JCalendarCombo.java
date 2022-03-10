//**********************************************************************
// Package
//**********************************************************************

package ch.specchio.gui;

//**********************************************************************
// Import list
//**********************************************************************

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import java.beans.*;

import javax.swing.plaf.metal.MetalComboBoxUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
//import com.sun.java.swing.plaf.motif.MotifComboBoxUI;
//import com.sun.java.swing.plaf.windows.WindowsComboBoxUI;
// For Skin L&F
//import com.l2fprod.gui.plaf.skin.SkinComboBoxUI;

import java.text.*;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.EventListener;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.accessibility.*;

/**
 * This class is a combo box that allows you to select a date either
 * by typing one in (if the combo box is editable) or through a
 * JCalendar pop-up. Normally, a combo box expects to use a a popup
 * that contains a list. We've perverted this a bit; while you can
 * select a variety of dates, the list always has one item which
 * changes to match the selected date.
 * <p>
 * You may specify a font for each of the elements that make up the
 * pop-up calendar. If you do not specify a font (or specify a null
 * font), a reasonable default will be generated based on the current
 * Look &amp; Feel.
 * <p>
 * The JCalendarCombo uses a non-mutable ComboBoModel. This means that
 * the following methods will generate an exception:
 * <ul>
 * <li>addItem()
 * <li>insertItemAt()
 * <li>removeItem()
 * <li>removeItemAt()
 * <li>removeAllItems()
 * </ul>
 * You would be ill-advised to to call setModel() with your own
 * ComboBoxModel unless you use some variant of a
 * JCalendarCombo.CalendarComboBoxModel class.
 * <p>
 * The combo box listeners work pretty much as for normal combo boxes.
 * The item changed event reports the date "unselected" (the previous
 * date) and "selected" (the new date set), but as Strings. It's
 * probably better to add a DateListener and ignore the ItemListener
 * and ActionListener.
 * <p>
 * The following key bindings should work (some may depend on the
 * selected Look-and-Feel):
 * <ul>
 * <li>Left Arrow - Move back a month.
 * <li>Right Arrow - Move forward a month.
 * <li>Shift Left Arrow - Move back a year.
 * <li>Shift Right Arrow - Move forward a year.
 * <li>Down Arrow - Display the calendar pop-up (if not visible).
 * <li>Up Arrow - Apply any change and hide the calendar pop-up (if visible);
 * <li>Escape - Cancel any changes and hide the calendar pop-up (if visible).
 * <li>Delete - Unselect all dates only if isNullAllowed() is true).
 * <li>BackSpace - Same as Delete.
 * <li>Return - Accept the selected date and hide the calendar pop-up
 *     (if visible).
 * </ul>
 * <p>
 * This component tries to adapt it's Look-and-Feel to match the
 * current Look-and-Feel. But it can only do so for Look-and-Feels's
 * that it knows about. To adapt it for new Look-and-Feels, create a
 * sub-class:
 * <pre><code>
 *  public class MyCalendarCombo extends JCalendarCombo {
 *    public void updateUI() {
 *      ComboBoxUI cui = (ComboBoxUI)UIManager.getUI(this);
 *      if (cui instanceof SomeLAFComboBoxUI) {
 *        cui = new SomeLAFCalComboBoxUI();
 *      }
 *      else {
 *        super.updateUI();
 *      }
 *    }
 *    private class SomeLAFCalComboBoxUI extends SomeLAFComboBoxUI {
 *      return new CalendarComboPopup();
 *    }
 *  }
 * </code></pre>
 * If it can't figure out an appropriate Look-and-Feel, it uses the
 * Metal Look-and-Feel.
 *
 * @author Antonio Freixas
 */

// Copyright (c) 2003 Antonio Freixas
// All Rights Reserved.

public class JCalendarCombo
    extends JComboBox
{

//**********************************************************************
// Public Constants
//**********************************************************************

/**
 * Used to indicate that this component should display the date.
 */

public static final int DISPLAY_DATE = JCalendar.DISPLAY_DATE;

/**
 * Used to indicate that this component should display the time.
 */

public static final int DISPLAY_TIME = JCalendar.DISPLAY_TIME;

//**********************************************************************
// Private Members
//**********************************************************************

// The currently displayed popup

static JCalendarCombo currentPopup = null;

// The locale to use

private Locale locale;

// The date format used to display the selected date

private DateFormat dateFormat;

// Date formats used for parsing

private DateFormat parseFormat[];
private DateFormat timePatternFormat;

// The components

private Window parentWindow;
private JWindow oldWindow;
private JWindow calendarWindow;
private JCalendar calendarPanel;

// True if the calendar panel is displayed

private boolean isCalendarDisplayed = false;

// Keep track of the original date in case the date change is canceled

private Date originalDate = null;

// Cache the date so we can tell when to take down the calendar

private Calendar cacheCalendar = null;

// These maps are used to bind keyboard keys to methods. These maps
// are added to the maps used by JCalendar.

private static InputMap inputMap = new InputMap();
private static InputMap spinnerInputMap = new InputMap();
private static ActionMap actionMap = new ActionMap();

// The input map maps a key to a name. The action map maps a name to
// an action. The actions below map the action to a method call

private static Action setNullDate = new AbstractAction("setNullDate") {
    public void actionPerformed(ActionEvent e) {
	((JCalendar)e.getSource()).setDate(null);
	((JCalendar)e.getSource()).getJCalendarComboParent().hideCalendar();
    }
};

private static Action cancel = new AbstractAction("cancel") {
    public void actionPerformed(ActionEvent e) {
	JCalendar cal = (JCalendar)e.getSource();
	JCalendarCombo calCombo = cal.getJCalendarComboParent();

	cal.setDate(calCombo.originalDate);
	calCombo.firePopupMenuCanceled();
	calCombo.hideCalendar();
    }
};

private static Action apply = new AbstractAction("apply") {
    public void actionPerformed(ActionEvent e) {
	JCalendar cal = (JCalendar)e.getSource();
	JCalendarCombo calCombo = cal.getJCalendarComboParent();
	calCombo.hideCalendar();
    }
};
//**********************************************************************
// Static Constructors
//**********************************************************************

static
{
    // Set up the input map that will be shared by all instances

    inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
    inputMap.put(KeyStroke.getKeyStroke("UP"), "apply");
    inputMap.put(KeyStroke.getKeyStroke("ENTER"), "apply");
    inputMap.setParent(JCalendar.inputMap);

    actionMap.put("cancel", cancel);
    actionMap.put("apply", apply);
    actionMap.setParent(JCalendar.actionMap);
}

//**********************************************************************
// Constructors
//**********************************************************************

/**
 * Create an instance of JCalendarCombo using the default calendar and
 * locale. Display the date but not the time. Don't display today's
 * date.
 */

public
JCalendarCombo()
{
    this(Calendar.getInstance(),
	 Locale.getDefault(),
	 DISPLAY_DATE,
	 false,
	 null);
}

/**
 * Create an instance of JCalendarCombo using the default calendar and
 * locale. Display a calendar and a time spinner as requested (to
 * display both use DISPLAY_DATE | DISPLAY_TIME). Display today's
 * date if requested. Use the default pattern to display the time in the
 * time spinner field (if there is one).
 *
 * @param selectedComponents Use DISPLAY_DATE, DISPLAY_TIME or
 * 	(DISPLAY_DATE | DISPLAY_TIME).
 * @param isTodayDisplayed True if today's date should be displayed.
 */

public
JCalendarCombo(
    int selectedComponents,
    boolean isTodayDisplayed)
{
    this(Calendar.getInstance(),
	 Locale.getDefault(),
	 selectedComponents,
	 isTodayDisplayed,
	 null);
}

/**
 * Create an instance of JCalendarCombo using the given calendar and
 * locale. Display a calendar and a time spinner as requested (to
 * display both use DISPLAY_DATE | DISPLAY_TIME). Display today's
 * date if requested. Use the default pattern to display the time in the
 * time spinner field (if there is one).
 *
 * @param calendar The calendar to use.
 * @param locale The locale to use.
 * @param selectedComponents Use DISPLAY_DATE, DISPLAY_TIME or
 * 	(DISPLAY_DATE | DISPLAY_TIME).
 * @param isTodayDisplayed True if today's date should be displayed.
 */

public
JCalendarCombo(
    Calendar calendar,
    Locale locale,
    int selectedComponents,
    boolean isTodayDisplayed)
{
    this(calendar,
	 locale,
	 selectedComponents,
	 isTodayDisplayed,
	 null);
}

/**
 * Create an instance of JCalendarCombo using the given calendar and
 * locale. Display a calendar and a time spinner as requested (to
 * display both use DISPLAY_DATE | DISPLAY_TIME). Display today's
 * date if requested.  Use the default pattern to display the time in the
 * time spinner field (if there is one).
 *
 * @param calendar The calendar to use.
 * @param locale The locale to use.
 * @param selectedComponents Use DISPLAY_DATE, DISPLAY_TIME or
 * 	(DISPLAY_DATE | DISPLAY_TIME).
 * @param isTodayDisplayed True if today's date should be displayed.
 * @see DateFormat
 * @see SimpleDateFormat
 */

public
JCalendarCombo(
    Calendar calendar,
    Locale locale,
    int selectedComponents,
    boolean isTodayDisplayed,
    String timePattern)
{
    super();
    calendarPanel =
	new JCalendar(
	    calendar,
	    locale,
	    selectedComponents,
	    isTodayDisplayed,
	    timePattern);
    this.locale = locale;

    // Set up the parse formats

    parseFormat = new DateFormat[12];

    parseFormat[0] =
	DateFormat.getDateTimeInstance(
	    DateFormat.SHORT, DateFormat.SHORT, locale);
    parseFormat[1] =
	DateFormat.getDateTimeInstance(
	    DateFormat.MEDIUM, DateFormat.MEDIUM, locale);
    parseFormat[2] =
	DateFormat.getDateTimeInstance(
	    DateFormat.LONG, DateFormat.LONG, locale);
    parseFormat[3] =
	DateFormat.getDateTimeInstance(
	    DateFormat.FULL, DateFormat.FULL, locale);

    parseFormat[4] = DateFormat.getDateInstance(DateFormat.SHORT, locale);
    parseFormat[5] = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
    parseFormat[6] = DateFormat.getDateInstance(DateFormat.LONG, locale);
    parseFormat[7] = DateFormat.getDateInstance(DateFormat.FULL, locale);

    parseFormat[8] = DateFormat.getTimeInstance(DateFormat.SHORT, locale);
    parseFormat[9] = DateFormat.getTimeInstance(DateFormat.MEDIUM, locale);
    parseFormat[10] = DateFormat.getTimeInstance(DateFormat.LONG, locale);
    parseFormat[11] = DateFormat.getTimeInstance(DateFormat.FULL, locale);

    // Add a format if the timePattern was specified

    if (timePattern != null) {
	timePatternFormat = new SimpleDateFormat(timePattern, locale);
    }

    if (selectedComponents == DISPLAY_DATE) {
	setDateFormat(DateFormat.getDateInstance(DateFormat.FULL, locale));
    }
    else if (selectedComponents == DISPLAY_TIME) {
	setDateFormat(DateFormat.getTimeInstance(DateFormat.FULL, locale));
	if (timePattern != null) {
	    timePatternFormat = new SimpleDateFormat(timePattern, locale);
	}
    }
    else {
	setDateFormat
	    (DateFormat.getDateTimeInstance(
		DateFormat.FULL, DateFormat.FULL, locale));
    }

    super.setModel(new CalendarComboBoxModel());
    super.setKeySelectionManager(new CalendarKeySelectionManager());

    createCalendarComponents();
}

//**********************************************************************
// Public
//**********************************************************************

/**
 * Add a date listener. This listener will receive events each time
 * the selected date changes.
 *
 * @param listener The date listener to add.
 */

public void
addDateListener(
    DateListener listener)
{
    calendarPanel.addDateListener(listener);
}

/**
 * Remove a date listener.
 *
 * @param listener The date listener to remove.
 */

public void
removeDateListener(
    DateListener listener)
{
    calendarPanel.removeDateListener(listener);
}

/**
 * Get whether a null date is allowed.
 *
 * @return The whether a null date is allowed.
 */

public boolean
isNullAllowed()
{
    return calendarPanel.isNullAllowed();
}

/**
 * Set whether a null date is allowed. A null date means that no date
 * is selected. The user can select a null date by pressing DELETE
 * anywhere within the calendar.
 * <p>
 * If nulls are not allowed, a setDate(null) will be ignored without
 * error. The DELETE key will do nothing.
 * <p>
 * If you switch from allowing nulls to not allowing nulls and the
 * current date is null, it will remain null until a date is selected.
 * <p>
 * The component default is to allow nulls.
 *
 * @param isNullAllowed The whether a null date is allowed.
 */

public void
setNullAllowed(
    boolean isNullAllowed)
{
    calendarPanel.setNullAllowed(isNullAllowed);
}

/**
 * Get the date currently displayed by the calendar panel. If null,
 * then no date was selected.
 *
 * @return The date currently displayed.
 * @see #getCalendar
 */

public Date
getDate()
{
    return calendarPanel.getDate();
}

/**
 * Set the calendar panel to display the given date. This will fire a
 * DateEvent.
 *
 * @param date The date to set.
 */

public void
setDate(
    Date date)
{
    calendarPanel.setDate(date);
}

/**
 * Get the date format used to display the selected date in the combo
 * box's text field.
 *
 * @return The date format used to display the selected date in the
 *	combo box's text field.
 */

public DateFormat
getDateFormat()
{
    return dateFormat;
}

/**
 * Set the date format used to display the selected date in the combo
 * box's text field. Nulls are not allowed.
 *
 * @param dateFormat The date format used to display the selected date
 * 	in the combo box's text field.
 * @throws java.lang.NullPointerException
 */

public void
setDateFormat(
    DateFormat dateFormat)
    throws NullPointerException
{
    if (dateFormat == null) {
	throw new NullPointerException("Date format cannot be null.");
    }

    this.dateFormat = dateFormat;
}

/**
 * Get a copy of the calendar used by this JCalendar. This calendar
 * will be set to the currently selected date, so it is an alternative
 * to the getDate() method.
 *
 * @return A copy of the calendar used by JCalendar.
 * @see #getDate
 */

public Calendar
getCalendar()
{
    return calendarPanel.getCalendar();
}

/**
 * Return the locale used by this JCalendar.
 *
 * @return The locale used by this JCalendar.
 */

public Locale
getLocale()
{
    return locale;
}

/**
 * Returns true if today's date is displayed at the bottom of the
 * calendar.
 *
 * @return True if today's date is displayed at the bottom of the
 * 	calendar.
 */

public boolean
isTodayDisplayed()
{
    return calendarPanel.isTodayDisplayed();
}

/**
 * Get the title font.
 *
 * @return The title font.
 */

public Font
getTitleFont()
{
    return calendarPanel.getTitleFont();
}

/**
 * If the font is set to null, then the title font (for the Month Year
 * title) will default to the L&amp;F's Label default font.
 * <p>
 * Otherwise, the title font is set as given.
 *
 * @param font The font to set.
 */

public void
setTitleFont(
    Font font)
{
    calendarPanel.setTitleFont(font);
}

/**
 * Get the day-of-week font (Mon, Tue, etc.).
 *
 * @return The day-of-week font.
 */

public Font
getDayOfWeekFont()
{
    return calendarPanel.getDayOfWeekFont();
}

/**
 * If the font is set to null, then the day-of-week font (Mon, Tue,
 * etc.) will default to 9/11th's of the L&amp;F's Label default font.
 * <p>
 * Otherwise, the day-of-week font is set as given.
 *
 * @param font The font to set.
 */

public void
setDayOfWeekFont(
    Font font)
{
    calendarPanel.setDayOfWeekFont(font);
}

/**
 * Get the day font.
 *
 * @return The day font.
 */

public Font
getDayFont()
{
    return calendarPanel.getDayFont();
}

/**
 * If the font is set to null, then the day font will default to
 * 9/11th's of the L&amp;F's Button default font.
 * <p>
 * Otherwise, the day font is set as given.
 *
 * @param font The font to set.
 */

public void
setDayFont(
    Font font)
{
    calendarPanel.setDayFont(font);
}

/**
 * Get the time spinner font.
 *
 * @return The time spinner font.
 */

public Font
getTimeFont()
{
    return calendarPanel.getTimeFont();
}

/**
 * If the font is set to null, then the time spinner font will default
 * to the L&amp;F's Spinner default font.
 * <p>
 * Otherwise, the time spinner font is set as given.
 *
 * @param font The font to set.
 */

public void
setTimeFont(
    Font font)
{
    calendarPanel.setTimeFont(font);
}

/**
 * Get the font used to display today's date as text.
 *
 * @return The font used to display today's date.
 */

public Font
getTodayFont()
{
    return calendarPanel.getTodayFont();
}

/**
 * If the font is set to null, then the font used to display today's
 * date as text will default to the L&amp;F's Label default font.
 * <p>
 * Otherwise, the font used to display today's date is set as given.
 *
 * @param font The font to set.
 */

public void
setTodayFont(
    Font font)
{
    calendarPanel.setTodayFont(font);
}

/**
 * Sets the selected item in the combo box display area to the object
 * in the argument. The object should be a String representation of a
 * date.
 * <p>
 * If this constitutes a change in the selected item, ItemListeners
 * added to the combo box will be notified with one or two ItemEvents.
 * If there is a current selected item, an ItemEvent will be fired and
 * the state change will be ItemEvent.DESELECTED. If anObject is in
 * the list and is not currently selected then an ItemEvent will be
 * fired and the state change will be ItemEvent.SELECTED.
 * <p>ActionListeners added to the combo box will be notified with an
 * ActionEvent when this method is called (assuming the date actually
 * changed).
 *
 * @param anObject The object to select.
 */

public void
setSelectedItem(
    Object anObject)
{
    getModel().setSelectedItem(anObject);
}

/**
 * This method is ignored. You cannot change the KeySelectionManager
 * for JCalendarCombo.
 *
 * @param aManager The new key selection manager.
 */

public void
setKeySelectionManager(
    JComboBox.KeySelectionManager aManager)
{
//  In JDK1.5, this method is called by BasicComboBoxUI. It did not
//  use to get called, so we were able to throw an exception.
//     throw new UnsupportedOperationException(
// 	"The KeySelectionManager for a JCalendarCombo cannot be changed.");
}

/**
 * Resets the UI property to a value from the current look and feel.
 * Read the class documentation for instructions on how to override
 * this to make the JCalendarCombo support a new Look-and-Feel.
 */

public void
updateUI() {
    ComboBoxUI cui = (ComboBoxUI)UIManager.getUI(this);
    if (cui instanceof BasicComboBoxUI) {
	cui = new WindowsDateComboBoxUI();
    }
    else if (cui instanceof MetalComboBoxUI) {
	cui = new MetalDateComboBoxUI();
    }
    else if (cui instanceof BasicComboBoxUI) {
	cui = new MotifDateComboBoxUI();
    }
// For Skin L&F
//     else if (cui instanceof SkinComboBoxUI) {
//  	cui = new SkinDateComboBoxUI();
//     }
    else {
	cui = new MetalDateComboBoxUI();
    }

    setUI(cui);
}

// The inherited equals and hashCode methods are acceptable as they
// are generally not used for components.

/**
 * {@inheritDoc}
 */

protected String
paramString()
{
    int selectedComponents = calendarPanel.getSelectedComponents();
    String curDate;
    if ((selectedComponents & DISPLAY_DATE) == DISPLAY_DATE) {
	curDate = DateFormat.getDateInstance(
	    DateFormat.FULL, locale).format(getDate());
    }
    else if ((selectedComponents & DISPLAY_TIME) == DISPLAY_TIME) {
	curDate = DateFormat.getTimeInstance(
	    DateFormat.FULL, locale).format(getDate());
    }
    else {
	curDate = DateFormat.getDateTimeInstance(
	    DateFormat.FULL, DateFormat.FULL, locale).format(getDate());
    }

    return super.paramString() + ",selectedDate=" + curDate;
}

//**********************************************************************
// Protected
//**********************************************************************

/**
 * Given a date in String form, convert it to a Date object. If no
 * conversion is possible, return null. This method tries to parse
 * the string using DateFormat and SHORT, MEDIUM, LONG and FULL forms.
 * If none of these work, a null date is returned.
 *
 * @param string The date in String form.
 * @return The equivalent Date object or null.
 */

protected Date
stringToDate(
    String string)
{
    if (string == null || string.length() < 1) return null;

    Date date = null;
    if (timePatternFormat != null) {
	try {
	    date = timePatternFormat.parse(string);
	}
	catch (ParseException e) { }
    }

    try {
	date = dateFormat.parse(string);
    }
    catch (ParseException e) { }

    if (date == null) {
	for (int i = 0; i < parseFormat.length; i++) {
	    try {
		date = parseFormat[i].parse(string);
	    }
	    catch (ParseException e) { }
	    if (date != null) break;
	}
    }

    return date;
}

//**********************************************************************
// Private
//**********************************************************************

/**
 * Set up the calendar combo box layout and components. These are not
 * date specific.
 */

private void
createCalendarComponents()
{
    // Set the combo parent of the calendar panel

    calendarPanel.setJCalendarComboParent(this);

    // Add some extra key bindings to the calendar panel. Note that
    // this input map has a parent InputMap from JCalendar

    calendarPanel.setInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, inputMap);
    calendarPanel.setActionMap(actionMap);

    // Add some extra key bindings to the spinner in the calendar
    // panel (this doesn't seem to work!)

    if ((calendarPanel.getSelectedComponents() & DISPLAY_TIME) > 0) {
	InputMap sim = new InputMap();
	sim.put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
	sim.put(KeyStroke.getKeyStroke("ENTER"), "apply");

	ActionMap sam = new ActionMap();
	sam.put(
	    "cancel",
	    new AbstractAction("cancel") {
	    public void actionPerformed(ActionEvent e) {
		JCalendarCombo calCombo = JCalendarCombo.this;
		calendarPanel.setDate(calCombo.originalDate);
		calCombo.firePopupMenuCanceled();
		calCombo.hideCalendar();
	    }});
	sam.put(
	    "apply",
	    new AbstractAction("apply") {
	    public void actionPerformed(ActionEvent e) {
		JCalendarCombo calCombo = JCalendarCombo.this;
		calCombo.hideCalendar();
	    }});

	calendarPanel.addSpinnerMaps(sim, sam);
    }

    // Add a border for the calendar panel

    Border border = (Border)UIManager.get("PopupMenu.border");
    if (border == null) {
	border = new BevelBorder(BevelBorder.RAISED);
    }

    calendarPanel.setBorder(border);

    // Add a listener for date events

    calendarPanel.addDateListener(new CalDateListener());

    // Add a listener for ancestor events

    addAncestorListener(new ComboAncestorListener());
}

/**
 * Make the calendar panel invisible.
 */

private void
hideCalendar()
{
    if (isCalendarDisplayed) {

	// If it's visible, make it invisible

	firePopupMenuWillBecomeInvisible();

	calendarWindow.setVisible(false);
	isCalendarDisplayed = false;
	requestFocus();
	if (currentPopup == this) currentPopup = null;
    }
}

/**
 * Make the calendar panel visible.
 */

private void
showCalendar()
{
    if (!isCalendarDisplayed) {

	// If another JCalendarCombo popup is visible, hide it

	if (currentPopup != null) currentPopup.hideCalendar();

	firePopupMenuWillBecomeVisible();

	// If the combo box is editable, we need to convert the edit
	// field into a date before bringing up the calendar. If the
	// combo box is not editable, then the calendar already
	// contains the selected date

	if (isEditable()) {
	    setSelectedItem(getEditor().getItem());
	    getEditor().selectAll();
	}

	Window oldParentWindow = parentWindow;
	parentWindow = SwingUtilities.getWindowAncestor(this);
	if (parentWindow == null) return;

	// If we don't have a window or if we need to reparent the
	// window...

	if (calendarWindow == null || parentWindow != oldParentWindow) {

	    // If a window exists, get rid of it

	    if (calendarWindow != null) calendarWindow.dispose();

	    // Create the new window and add the calendar panel

	    calendarWindow = new JWindow(parentWindow);
	    calendarWindow.getContentPane().add(calendarPanel);
	    calendarWindow.pack();
	}

	// Now position the window properly

	Point fieldLocation = getLocationOnScreen();
	Dimension fieldSize = getSize();
	Dimension windowSize = calendarWindow.getSize();
	Dimension screenSize = getToolkit().getScreenSize();

	int x = fieldLocation.x + (fieldSize.width - windowSize.width);
	int y = fieldLocation.y + fieldSize.height;

	// Adjust the x position to keep the calendar window on the
	// screen as much as possible

//	if (x + windowSize.width > screenSize.width) {
//	    x = screenSize.width - windowSize.width;
//	}
	if (x < 0) x = 0;

	// Adjust the y position to keep the calendar window on the
	// screen. We are already set to display below the text field;
	// if that doesn't work, we display it above

//	if (y + windowSize.height > screenSize.height) {
//	    y = fieldLocation.y - windowSize.height;
//	}

	// Reset the displayed date to include the selected date (or
	// today's date if the selected date is null)

	originalDate = calendarPanel.getDate();
	calendarPanel.setDisplayDate(originalDate);
	cacheCalendar = calendarPanel.getCalendar();

	// Make it visible

	calendarWindow.setLocation(x, y);
	calendarWindow.setVisible(true);
	isCalendarDisplayed = true;
	currentPopup = this;
    }
}

/**
 * Toggle the display of the calendar panel.
 */

private void
toggleCalendar()
{
    if (isCalendarDisplayed) {
	hideCalendar();
    }
    else {
	showCalendar();
    }
}

//**********************************************************************
// Inner Classes
//**********************************************************************

////////////////////////////////////////////////////////////////////////
// Class MetalDateComboBoxUI
////////////////////////////////////////////////////////////////////////

// Override the MetalComboBoxUI to add a Calendar pop-up

private class MetalDateComboBoxUI
      extends MetalComboBoxUI
{

protected ComboPopup createPopup() {
    return new CalendarComboPopup();
}

}

////////////////////////////////////////////////////////////////////////
// Class WindowsDateComboBoxUI
////////////////////////////////////////////////////////////////////////

// Override the WindowsComboBoxUI to add a Calendar pop-up

class WindowsDateComboBoxUI
    extends BasicComboBoxUI
{

protected ComboPopup createPopup() {
    return new CalendarComboPopup();
}

}

////////////////////////////////////////////////////////////////////////
// Class MotifDateComboBoxUI
////////////////////////////////////////////////////////////////////////

// Override the MotifComboBoxUI to add a Calendar pop-up

class MotifDateComboBoxUI
    extends BasicComboBoxUI
{

protected ComboPopup createPopup() {
    return new CalendarComboPopup();
}

}

////////////////////////////////////////////////////////////////////////
// Class SkinDateComboBoxUI
////////////////////////////////////////////////////////////////////////

// Override the SkinComboBoxUI to add a Calendar pop-up

// For Skin L&F
// class SkinDateComboBoxUI
//     extends SkinComboBoxUI
// {

// protected ComboPopup createPopup() {
//     return new CalendarComboPopup();
// }

// }

////////////////////////////////////////////////////////////////////////
// Class CalDateListener
////////////////////////////////////////////////////////////////////////

// This class listens for calendar events in the calendarPanel and
// passes the selection change to the combo box. The calendar pop-up
// is also made invisible if the date (not just the time) changes

private class CalDateListener
    implements DateListener
{

public void
dateChanged(
    DateEvent e)
{
    Calendar cal = e.getSelectedDate();

    if (cal == null) {
	setSelectedItem(null);
    }
    else {
	setSelectedItem(dateFormat.format(e.getSelectedDate().getTime()));
    }

    // Hide the calendar only if the day changes (ignore the time)

    if (cal == null && cacheCalendar == null) return;
    if (cal != null && cacheCalendar != null) {
	if (cal.get(Calendar.YEAR) == cacheCalendar.get(Calendar.YEAR) &&
	    cal.get(Calendar.MONTH) == cacheCalendar.get(Calendar.MONTH) &&
	    cal.get(Calendar.DATE) == cacheCalendar.get(Calendar.DATE)) {
	    return;
	}
    }
    hideCalendar();
}

}

////////////////////////////////////////////////////////////////////////
// Class CalendarComboBoxModel
////////////////////////////////////////////////////////////////////////

// This is the ComboBoxModel used for calendars.

protected class CalendarComboBoxModel
    implements ComboBoxModel
{

protected EventListenerList listenerList = new EventListenerList();

// Add a ListDataListener

public void
addListDataListener(
    ListDataListener l)
{
    listenerList.add(ListDataListener.class, l);
}

// Remove a ListDataListener

public void
removeListDataListener(
    ListDataListener l)
{
    listenerList.remove(ListDataListener.class, l);
}

// Look at the calendarPanel and get its date in String form, using
// the user's selected DateFormat. We ignore the index as our
// "pretend" list has only one item -- the date in the calendar.

public Object
getElementAt(
    int index)
{
    return getSelectedItem();
}

// The list size is always 1

public int
getSize()
{
    return 1;
}

// Get the selected date in String form. Null dates are converted to
// empty strings

public Object
getSelectedItem()
{
    Date date = calendarPanel.getDate();
    if (date == null) return "";
    return dateFormat.format(date);
}

// Set the selected date. We get the source date by calling the
// toString() method of the item passed in. This gets converted to a
// Date which is applied to the calendarPanel and to the combo box
// editor (if the combo box is editable). If we note that either had
// to be changed, we fire an action event and an item change event

public void
setSelectedItem(
    Object anItem)
{
    // Get the date to set

    Date date = null;
    if (anItem != null) date = stringToDate(anItem.toString());

    // This method may be called because:
    //   - of a direct call by a programmer
    //   - of a DateEvent (the user selected a date on the calendar)
    //   - of a change in the editor (the user typed in a new date)
    // We want to fire an event only if there is a change to either
    // the calendar date or the editor date

    boolean fireEvent = false;

    Date calDate = calendarPanel.getDate();
    if (date == null && calDate != null ||
	date != null && !date.equals(calDate)) {

	fireEvent = true;
	calendarPanel.setDate(date);
    }

    if (isEditable()) {
	Object editorItem = getEditor().getItem();
	Date editorDate = null;
	if (editorItem != null) {
	    editorDate = stringToDate(editorItem.toString());
	}
	if (date == null && editorDate != null ||
	    date != null && !date.equals(editorDate)) {

	    fireEvent = true;
	    if (date == null) {
		getEditor().setItem("");
	    }
	    else {
		getEditor().setItem(dateFormat.format(date));
	    }
	}
    }

    if (fireEvent) {

	// This method will cause an Action event to be fired

	fireContentsChanged(this, -1, -1);
    }
}

// Generate contentChanged() events

private void
fireContentsChanged(
    Object source,
    int index0,
    int index1)
{
    Object[] listeners = listenerList.getListenerList();
    ListDataEvent e = null;

    for (int i = listeners.length - 2; i >= 0; i -= 2) {
	if (listeners[i] == ListDataListener.class) {
	    if (e == null) {
		e = new ListDataEvent(
		    source, ListDataEvent.CONTENTS_CHANGED, index0, index1);
	    }
	    ((ListDataListener)listeners[i+1]).contentsChanged(e);
	}
    }
}

}

////////////////////////////////////////////////////////////////////////
// Class CalendarKeySelectionManager
////////////////////////////////////////////////////////////////////////

// We use this KeySelectionManager which has no key selections

private class CalendarKeySelectionManager
    implements JComboBox.KeySelectionManager
{

public int
selectionForKey(
    char aKey,
    ComboBoxModel aModel)
{
    // There are no key selections
    return -1;
}

}

////////////////////////////////////////////////////////////////////////
// Class CalendarComboPopup
////////////////////////////////////////////////////////////////////////

// This is the calendar pop-up

protected class CalendarComboPopup
    implements ComboPopup
{

private JList list = new JList();
private MouseListener mouseListener = null;


// There is no key listener

public KeyListener
getKeyListener()
{
    return null;
}

// We return a JList with no items

public JList
getList()
{
    return list;
}

// Create a mouse listener which is used to display the combo pop-up
// on a mouse press

public MouseListener
getMouseListener()
{
    if (mouseListener == null) {
	mouseListener = new InvocationMouseListener();
    }
    return mouseListener;
}

// There is no mouse motion listerne

public MouseMotionListener
getMouseMotionListener()
{
    return null;
}

// Determine if the pop-up is visible

public boolean
isVisible()
{
    return isCalendarDisplayed;
}
// Hide the calendar. The work is done elsewhere

public void
hide()
{
    hideCalendar();
}

// Show the calendar. The work is done elsewhere

public void
show()
{
    showCalendar();
}

// I'm not sure there's anything we need to do here

public void
uninstallingUI()
{
}

}

////////////////////////////////////////////////////////////////////////
// Class ComboAncestorListener
////////////////////////////////////////////////////////////////////////

// Hide the calendar if anything happens to its ancestor

private class ComboAncestorListener
    implements AncestorListener
{

public void
ancestorAdded(
    AncestorEvent e)
{
    hideCalendar();
}

public void
ancestorRemoved(
    AncestorEvent e)
{
    hideCalendar();
}

public void
ancestorMoved(
    AncestorEvent e)
{
    hideCalendar();
}

}

////////////////////////////////////////////////////////////////////////
// Class InvocationMouseListener
////////////////////////////////////////////////////////////////////////

// A listener to be registered upon the combo box (not its popup menu)
// to handle mouse events that affect the state of the popup menu. The
// main purpose of this listener is to make the popup menu appear and
// disappear.

private class InvocationMouseListener
    extends MouseAdapter
{
public void
mousePressed(
    MouseEvent e)
{
    if (!SwingUtilities.isLeftMouseButton(e) || !isEnabled())
	return;

    toggleCalendar();
}

}

//**********************************************************************
// End Inner Classes
//**********************************************************************

}
