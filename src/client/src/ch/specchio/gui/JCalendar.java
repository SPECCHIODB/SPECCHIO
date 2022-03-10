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
import javax.swing.event.*;

import java.text.*;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This class displays a panel through which a user can select a date
 * and/or time. A time-only selection can be used to select a
 * duration, as long as the duration is no longer than 23 hours, 59
 * minutes and 59 seconds.
 * <p>
 * The date is selected using a calendar display. The time is selected
 * using a date spinner.
 * <p>
 * In reality, both date and time are part of the same Date returned
 * by getDate() or getCalendar(). If you are selecting only the date,
 * you should ignore the time portion of the Date or Calendar. If you
 * are selecting only the time, ignore the date portion.
 * <p>
 * You can set the pattern used to display the time in the date
 * spinner. The pattern is the same as used by SimpleDateFormat. The
 * default format displays hours, minutes and seconds in a
 * locale-specific way (some locales use AM/PM, some use a 24-hour
 * clock). If you want to get a time duration, you will want to use a
 * pattern such as "HH:mm:ss" to eliminate the possibility of an AM/PM
 * field appearing. You can also use setTimePattern() to reduce the
 * precision of the time obtained (e.g. "HH:mm").
 * <p>
 * You may specify a font for each of the elements that make up the
 * calendar. If you do not specify a font (or specify a null font), a
 * reasonable default will be generated based on the current Look
 * &amp; Feel.
 * <p>
 * When the calendar has focus, the following key bindings are
 * supported:
 * <ul>
 * <li>Left Arrow - Move back a month.
 * <li>Right Arrow - Move forward a month.
 * <li>Shift Left Arrow - Move back a year.
 * <li>Shift Right Arrow - Move forward a year.
 * <li>Delete - Unselect all dates (only if isNullAllowed() is true).
 * <li>Backspace - Same as Delete.
 * </ul>
 * This is in addition to using Tab and Enter to move through and
 * select the buttons.
 * <p>
 * The time field is divided into hour, minute, second and AM/PM
 * portions. You can select any portion and use the spinner arrows on
 * the right side of the field to increment or decrement that portion.
 * However, the entire time is being incremented or decremented, so
 * that incrementing 1:59:59 by one second will generate 2:00:00.
 * <p>
 * Due to a design limitation in JFormatedTextField, incrementing
 * 24:59:59 will <b>not</b> increment the day. A value in a
 * JFormatedTextField (which is what the time field is), only
 * calculates a date from the fields displayed. Since usually we
 * display a HH:mm:ss pattern, the JFormattedTextField will set the
 * date to a default value, not influenced by the date in the calendar
 * <p>
 * It is possible to pass in a time pattern that displays more than just
 * the time -- this is not advisable since the date portion displayed
 * in the JFormatedTextField will be ignored by the JCalendar
 * component.
 * <p>
 * When the time field has focus, the up/down arrow keys increment or
 * decrement the currently selected time portion, just like the
 * spinner keys. The left and right arrow keys can be used to move to
 * the next or previous portion.
 *
 * @see Calendar
 * @see Date
 * @see DateFormat
 * @see SimpleDateFormat
 * @author Antonio Freixas
 */

// Copyright (c) 2003 Antonio Freixas
// All Rights Reserved.

public class JCalendar
    extends JPanel
{

//**********************************************************************
// Public Constants
//**********************************************************************

/**
 * Used to indicate that this component should display the date.
 */

public static final int DISPLAY_DATE = 0x01;

/**
 * Used to indicate that this component should display the time.
 */

public static final int DISPLAY_TIME = 0x02;

//**********************************************************************
// Private Constants
//**********************************************************************

private static final int MONTH_DECR_BUTTON	= 0;
private static final int MONTH_INCR_BUTTON	= 1;
private static final int YEAR_DECR_BUTTON	= 2;
private static final int YEAR_INCR_BUTTON	= 3;

//**********************************************************************
// Private Members
//**********************************************************************

// This determines the components being displayed: the calendar, the
// time spinner, or both

private int selectedComponents;

// The calendar containing a selected day. The selected day may not be
// always be displayed

private Calendar selectedCalendar;
private int selectedYear = -1;
private int selectedMonth = -1;
private int selectedDay = -1;
private int selectedHour = -1;
private int selectedMinute = -1;
private int selectedSecond = -1;

// The calendar we display

private Calendar displayCalendar;
private int displayYear;
private int displayMonth;

// The locale to use

private Locale locale;

// True if we display today's date

private boolean isTodayDisplayed = false;

// A null date is equivalent to having no date selected. Note that the
// constructor selects the current date (today)

private boolean isNullAllowed = true;
private boolean isNullDate = true;

// The time pattern used to set the format of the time spinner

private String timePattern;

// Components

private JButton yearDecrButton;
private JButton monthDecrButton;
private JLabel monthYearLabel;
private JButton monthIncrButton;
private JButton yearIncrButton;

private JLabel[] dayOfWeekLabels;
private JToggleButton[][] dayButtons;
private JToggleButton offScreenButton;
private ButtonGroup dayGroup;

private SpinnerDateModel spinnerDateModel;
private JSpinner spinner;

private JLabel todaysLabel;

// Fonts

private Font titleFont;
private Font dayOfWeekFont;
private Font dayFont;
private Font timeFont;
private Font todayFont;

// Date formats

private DateFormat formatMonth;
private DateFormat formatWeekDay;

private String lastMonth;
private String lastYear;

// These maps are used to bind keyboard keys to methods

static InputMap inputMap = new InputMap();
static ActionMap actionMap = new ActionMap();

// The input map maps a key to a name. The action map maps a name to
// an action. The actions below map the action to a method call

private static Action yearBackward = new AbstractAction("yearBackward") {
    public void actionPerformed(ActionEvent e) {
	((JCalendar)e.getSource()).yearBackward();
    }
};

private static Action yearForward = new AbstractAction("yearForward") {
    public void actionPerformed(ActionEvent e) {
	((JCalendar)e.getSource()).yearForward();
    }
};

private static Action monthBackward = new AbstractAction("montBackward") {
    public void actionPerformed(ActionEvent e) {
	((JCalendar)e.getSource()).monthBackward();
    }
};

private static Action monthForward = new AbstractAction("monthForward") {
    public void actionPerformed(ActionEvent e) {
	((JCalendar)e.getSource()).monthForward();
    }
};

private static Action setNullDate = new AbstractAction("setNullDate") {
    public void actionPerformed(ActionEvent e) {
	((JCalendar)e.getSource()).setDate(null);
    }
};

// The resource bundle

private static ResourceBundle bundle =
    ResourceBundle.getBundle("Bundle");

//**********************************************************************
// Static Constructors
//**********************************************************************

static
{
    // Set up the input map that will be shared by all instances

    inputMap.put(KeyStroke.getKeyStroke("BACK_SPACE"), "setNullDate");
    inputMap.put(KeyStroke.getKeyStroke("DELETE"), "setNullDate");
    inputMap.put(KeyStroke.getKeyStroke("shift LEFT"), "yearBackward");
    inputMap.put(KeyStroke.getKeyStroke("shift RIGHT"), "yearForward");
    inputMap.put(KeyStroke.getKeyStroke("LEFT"), "monthBackward");
    inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "monthForward");

    actionMap.put("setNullDate", setNullDate);
    actionMap.put("yearBackward", yearBackward);
    actionMap.put("yearForward", yearForward);
    actionMap.put("monthBackward", monthBackward);
    actionMap.put("monthForward", monthForward);
}

//**********************************************************************
// Constructors
//**********************************************************************

/**
 * Create an instance of JCalendar using the default calendar and
 * locale. Display the date but not the time. Don't display today's
 * date at the bottom of the panel.
 */

public
JCalendar()
{
    this(Calendar.getInstance(),
	 Locale.getDefault(),
	 DISPLAY_DATE,
	 false,
	 null);
}

/**
 * Create an instance of JCalendar using the default calendar and
 * locale. Display a calendar and/or a time spinner as requested (to
 * display both use DISPLAY_DATE | DISPLAY_TIME). Display today's
 * date if requested. Use the defult pattern to display the time in the
 * time spinner field (if there is one).
 *
 * @param selectedComponents Use DISPLAY_DATE, DISPLAY_TIME or
 * 	(DISPLAY_DATE | DISPLAY_TIME).
 * @param isTodayDisplayed True if today's date should be displayed at
 * 	the bottom of the panel.
 */

public
JCalendar(
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
 * Create an instance of JCalendar using the given calendar and
 * locale. Display a calendar and/or a time spinner as requested (to
 * display both use DISPLAY_DATE | DISPLAY_TIME). Display today's
 * date if requested. Use the default pattern to display the time in the
 * time spinner field (if there is one).
 *
 * @param calendar The calendar to use.
 * @param locale The locale to use.
 * @param selectedComponents Use DISPLAY_DATE, DISPLAY_TIME or
 * 	(DISPLAY_DATE | DISPLAY_TIME).
 * @param isTodayDisplayed True if today's date should be displayed at
 * 	the bottom of the panel.
 */

public
JCalendar(
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
 * Create an instance of JCalendar using the given calendar and
 * locale. Display a calendar and/or a time spinner as requested (to
 * display both use DISPLAY_DATE | DISPLAY_TIME). Display today's
 * date if requested. Set the pattern used to display the time in the time
 * spinner field (if there is one). If null, use the default MEDIUM
 * format for the given locale. Patterns are from DateFormat and
 * SimpleDateFormat.
 *
 * @param calendar The calendar to use.
 * @param locale The locale to use.
 * @param selectedComponents Use DISPLAY_DATE, DISPLAY_TIME or
 * 	(DISPLAY_DATE | DISPLAY_TIME).
 * @param isTodayDisplayed True if today's date should be displayed at
 * 	the bottom of the panel.
 * @param timePattern The pattern used to display the time in the time
 * 	spinner field.
 * @see DateFormat
 * @see SimpleDateFormat
 */

public
JCalendar(
    Calendar calendar,
    Locale locale,
    int selectedComponents,
    boolean isTodayDisplayed,
    String timePattern)
{
    this.selectedCalendar = (Calendar)calendar.clone();
    this.displayCalendar = (Calendar)selectedCalendar.clone();
    this.selectedComponents = selectedComponents;
    if ((selectedComponents & (DISPLAY_DATE | DISPLAY_TIME)) == 0) {
 	throw new IllegalStateException(
	    bundle.getString("IllegalStateException"));
    }

    this.locale = locale;
    this.isTodayDisplayed = isTodayDisplayed;

    if ((selectedComponents & DISPLAY_TIME) > 0) {
	if (timePattern == null) {
	    DateFormat timeFormat =
		DateFormat.getTimeInstance(DateFormat.MEDIUM, locale);
	    this.timePattern = "HH:mm:ss";
	    if (timeFormat instanceof SimpleDateFormat) {
		this.timePattern = ((SimpleDateFormat)timeFormat).toPattern();
	    }
	}
	else {
	    this.timePattern = timePattern;
	}
    }

    createCalendarComponents();
    setDate(new Date());
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
    listenerList.add(DateListener.class, listener);
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
    listenerList.remove(DateListener.class, listener);
}

/**
 * Get whether a null date is allowed.
 *
 * @return Whether a null date is allowed.
 */

public boolean
isNullAllowed()
{
    return isNullAllowed;
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
 * @param isNullAllowed Whether a null date is allowed.
 */

public void
setNullAllowed(
    boolean isNullAllowed)
{
    this.isNullAllowed = isNullAllowed;
}

/**
 * Get the date currently displayed by the calendar panel. If no date
 * is selected, null is returned.
 *
 * @return The date currently displayed.
 * @see #getCalendar
 */

public Date
getDate()
{
    if (isNullDate) return null;
    return selectedCalendar.getTime();
}

/**
 * Set the calendar panel to display the given date. This will fire a
 * DateEvent. The date may be null. If isNullAllowed() is true, then
 * all dates will be unselected. If isNullAllowed() is false, a null
 * date is ignored.
 *
 * @param date The date to set.
 */

public void
setDate(
    Date date)
{
    if (date == null) {

	// Ignore nulls if nulls aren't allowed

	if (!isNullAllowed) return;

	if (!isNullDate) {
	    isNullDate = true;
	    selectedYear = -1;
	    selectedMonth = -1;
	    selectedDay = -1;
	    selectedCalendar.set(Calendar.YEAR, 9999);
	    selectedCalendar.set(Calendar.MONTH, 9);
	    selectedCalendar.set(Calendar.DATE, 9);
	    selectedCalendar.set(Calendar.HOUR_OF_DAY, 0);
	    selectedCalendar.set(Calendar.MINUTE, 0);
	    selectedCalendar.set(Calendar.SECOND, 0);
	    updateCalendarComponents();
	    fireDateChange();
	}
    }

    else {
	int oldYear = selectedYear;
	int oldMonth = selectedMonth;
	int oldDay = selectedDay;
	int oldHour = selectedHour;
	int oldMinute = selectedMinute;
	int oldSecond = selectedSecond;

	selectedCalendar.setTime(date);
	selectedYear = selectedCalendar.get(Calendar.YEAR);
	selectedMonth = selectedCalendar.get(Calendar.MONTH);
	selectedDay = selectedCalendar.get(Calendar.DATE);
	selectedHour = selectedCalendar.get(Calendar.HOUR_OF_DAY);
	selectedMinute = selectedCalendar.get(Calendar.MINUTE);
	selectedSecond = selectedCalendar.get(Calendar.SECOND);

	if ((((selectedComponents & DISPLAY_DATE) > 0) &&
	     oldDay != selectedDay ||
	     oldMonth != selectedMonth ||
	     oldYear != selectedYear) ||

	    (((selectedComponents & DISPLAY_TIME) > 0) &&
	     oldHour != selectedHour ||
	     oldMinute != selectedMinute ||
	     oldSecond != selectedSecond)) {

	    isNullDate = false;
	    displayCalendar.setTime(date);
	    updateCalendarComponents();
	    fireDateChange();
	}
    }
}

/**
 * Reset the displayed date without changing the selected date. No
 * event occurs. A null date will reset to today's date. The display
 * date simply selects the calendar page (month/year) to display.
 *
 * @param date The date to display.
 */

public void
setDisplayDate(
    Date date)
{
    if (date == null) date = new Date();

    displayCalendar.setTime(date);
    int oldMonth = displayCalendar.get(Calendar.MONTH);
    int oldYear = displayCalendar.get(Calendar.YEAR);
    if (oldMonth != displayMonth || oldYear != displayYear) {
	updateCalendarComponents();
    }
}

/**
 * Get the pattern used to display the time in the time selection
 * spinner. This is null if the time is not displayed.
 *
 * @return The pattern used to display the time in the time selection
 * 	spinner.
 */

public String
getTimePattern()
{
    if ((selectedComponents & DISPLAY_TIME) != 0) {
	return timePattern;
    }
    return null;
}

/**
 * Get a copy of the calendar used by this JCalendar. This calendar
 * will be set to the currently selected date, so it is an alternative
 * to the getDate() method. If no date is selected (getDate() returns
 * null), the calendar's selected date is 9/9/9999 and should not be
 * used.
 *
 * @return A copy of the calendar used by JCalendar.
 * @see #getDate
 */

public Calendar
getCalendar()
{
    return (Calendar)selectedCalendar.clone();
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
 * Return the components being displayed:
 * <code>
 *   (getSelectedComponents() & DISPLAY_DATE) > 0
 * </code>
 * means that the date calendar is being displayed.
 * <code>
 *  (getSelectedComponents() & DISPLAY_TIME) > 0
 * </code>
 * menas that the time spinner field is being displayed.
 *
 * @return The selected components.
 */

public int
getSelectedComponents()
{
    return selectedComponents;
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
    return isTodayDisplayed;
}

/**
 * Get the title font.
 *
 * @return The title font.
 */

public Font
getTitleFont()
{
    return titleFont;
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
    if (font == null && titleFont != null ||
	!font.equals(titleFont)) {

	titleFont = font;
	if (isDisplayable()) setupTitleFont();
    }
}

/**
 * Get the day-of-week font (Mon, Tue, etc.).
 *
 * @return The day-of-week font.
 */

public Font
getDayOfWeekFont()
{
    return dayOfWeekFont;
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
    if (font == null && dayOfWeekFont != null ||
	!font.equals(dayOfWeekFont)) {

	dayOfWeekFont = font;
	if (isDisplayable()) setupDayOfWeekFonts();
    }
}

/**
 * Get the day font.
 *
 * @return The day font.
 */

public Font
getDayFont()
{
    return dayFont;
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
    if (font == null && dayFont != null ||
	!font.equals(dayFont)) {

	dayFont = font;
	if (isDisplayable()) setupDayFonts();
    }
}

/**
 * Get the time spinner font.
 *
 * @return The time spinner font.
 */

public Font
getTimeFont()
{
    return timeFont;
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
    if (font == null && timeFont != null ||
	!font.equals(timeFont)) {

	timeFont = font;
	if (isDisplayable()) setupTimeFont();
    }
}

/**
 * Get the font used to display today's date as text.
 *
 * @return The font used to display today's date.
 */

public Font
getTodayFont()
{
    return todayFont;
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
    if (font == null && todayFont != null ||
	!font.equals(todayFont)) {

	todayFont = font;
	if (isDisplayable()) setupTodayFont();
    }
}

/**
 * {@inheritDoc}
 */

public void
setEnabled(
    boolean b)
{
    if (b != isEnabled()) {
	super.setEnabled(b);

	if ((selectedComponents & DISPLAY_DATE) > 0) {
	    yearDecrButton.setEnabled(b);
	    monthDecrButton.setEnabled(b);
	    monthYearLabel.setEnabled(b);
	    monthIncrButton.setEnabled(b);
	    yearIncrButton.setEnabled(b);

	    for (int day = 0; day < 7; day++) {
		dayOfWeekLabels[day].setEnabled(b);
	    }

	    for (int row = 0; row < 6; row++) {
		for (int day = 0; day < 7; day++) {
		    if (dayButtons[row][day].getText().length() > 0) {
			dayButtons[row][day].setEnabled(b);
		    }
		}
	    }
	}

	if ((selectedComponents & DISPLAY_TIME) > 0) {
	    spinner.setEnabled(b);
	}
    }
}

/**
 * {@inheritDoc}
 */

public void
addNotify()
{
    // We don't try to do anything with the fonts until we know the
    // component is displayable

    setupTitleFont();
    setupDayOfWeekFonts();
    setupDayFonts();
    setupTimeFont();
    setupTodayFont();

    super.addNotify();
}

// The default equals and hashCode methods are acceptable. In
// general, two components are never equal unless they are the same
// component.

/**
 * {@inheritDoc}
 */

protected String
paramString()
{
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
// Package Public
//**********************************************************************

private JCalendarCombo comboCalendar;

/**
 * Get the JCalendarCombo component which uses this JCalendar in its
 * combo popup.
 *
 * @return The JCalendarCombo component which uses this JCalendar in
 * 	its combo popup.
 * @see JCalendarCombo
 */

JCalendarCombo
getJCalendarComboParent()
{
    return comboCalendar;
}

/**
 * Set the JCalendarCombo component which uses this JCalendar in its
 * combo popup.
 *
 * @param comboCalendar The JCalendarCombo component which uses this
 * 	JCalendar in its combo popup.
 * @see JCalendarCombo
 */

void
setJCalendarComboParent(
    JCalendarCombo comboCalendar)
{
    this.comboCalendar = comboCalendar;
}

/**
 * Add the given input/action maps to the spinner.
 */

void
addSpinnerMaps(
    InputMap sim,
    ActionMap sam)
{
    sim.setParent(spinner.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));
    spinner.setInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, sim);
    sam.setParent(spinner.getActionMap());
    spinner.setActionMap(sam);
}

//**********************************************************************
// Protected
//**********************************************************************

/**
 * Fire a date change. This notifies all listeners that the date has
 * changed.
 */

protected void
fireDateChange()
{
    // Guaranteed to return a non-null array

    Object[] listeners = listenerList.getListenerList();

    // Process the listeners last to first, notifying
    // those that are interested in this event

    for (int i = listeners.length - 2; i >= 0; i -= 2) {
	if (listeners[i] == DateListener.class) {
	    DateEvent dateEvent;
	    if (isNullDate) {
		dateEvent = new DateEvent(this, null);
	    }
	    else {
		dateEvent = new DateEvent(this, selectedCalendar);
	    }
	    ((DateListener)listeners[i + 1]).dateChanged(dateEvent);
	}
    }
}

/**
 * Set the title's font.
 */

protected void
setupTitleFont()
{
    Font font;
    if (monthYearLabel == null) return;

    // If not null, use what the user gave us

    if (titleFont != null) {
	monthYearLabel.setFont(titleFont);
    }

    // Otherwise, use the L&F default for a label

    else {
	font = UIManager.getFont("Label.font");
	monthYearLabel.setFont(font);
    }
}

/**
 * Set the day-of-week labels' font.
 */

protected void
setupDayOfWeekFonts()
{
    Font font;
    if (dayOfWeekLabels == null) return;

    // If not null, use what the user gave us

    font = dayOfWeekFont;

    // Otherwise, use 9/11 of the L&F default for a label

    if (font == null) {
	font = UIManager.getFont("Label.font");
	font = font.deriveFont((float)(font.getSize2D() * 9.0 / 11.0));
    }

    // Set the day of week labels' font

    for (int day = 0; day < 7; day++) {
	dayOfWeekLabels[day].setFont(font);
    }
}

/**
 * Set the day labels' font.
 */

protected void
setupDayFonts()
{
    Font font;
    if (dayButtons == null) return;

    // If null, use what the user gave us

    font = dayFont;

    // Otherwise, use 9/11 of the L&F default for a button

    if (font == null) {
	font = UIManager.getFont("Button.font");
	font = font.deriveFont((float)(font.getSize2D() * 9.0 / 11.0));
    }

    // Set the day labels' font

    for (int row = 0; row < 6; row++) {
	for (int day = 0; day < 7; day++) {
	    dayButtons[row][day].setFont(font);
	}
    }
}

/**
 * Set the time spinner's font.
 */

protected void
setupTimeFont()
{
    Font font;
    if (spinner == null) return;

    // If not null, use what the user gave us

    if (timeFont != null) {
	spinner.setFont(timeFont);
    }
    else {
	font = UIManager.getFont("Spinner.font");
	spinner.setFont(font);
    }
}

/**
 * Set the font used to display today's date as text.
 */

protected void
setupTodayFont()
{
    Font font;
    if (todaysLabel == null) return;

    // If not null, use what the user gave us

    if (todayFont != null) {
	todaysLabel.setFont(todayFont);
    }
    else {
	font = UIManager.getFont("Label.font");
	todaysLabel.setFont(font);
    }
}

//**********************************************************************
// Private
//**********************************************************************

/**
 * Set up the calendar panel with the basic layout and components.
 * These are not date specific.
 */

private void
createCalendarComponents()
{
    // The date panel will hold the calendar and/or the time spinner

    JPanel datePanel = new JPanel(new BorderLayout(2, 2));

    // Create the calendar if we are displaying a calendar

    if ((selectedComponents & DISPLAY_DATE) > 0) {
	formatMonth = new SimpleDateFormat("MMM", locale);
	formatWeekDay = new SimpleDateFormat("EEE", locale);

	// Set up the shared keyboard bindings

	setInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, inputMap);
	setActionMap(actionMap);

	// Set up the decrement buttons

	yearDecrButton =
	    new JButton(
		new ButtonAction(
		    "YearDecrButton",
		    "YearDecrButtonMnemonic", "YearDecrButtonAccelerator",
		    "YearDecrButtonImage",
		    "YearDecrButtonShort", "YearDecrButtonLong",
		    YEAR_DECR_BUTTON));
	monthDecrButton =
	    new JButton(
		new ButtonAction(
		    "MonthDecrButton",
		    "MonthDecrButtonMnemonic", "MonthDecrButtonAccelerator",
		    "MonthDecrButtonImage",
		    "MonthDecrButtonShort", "MonthDecrButtonLong",
		    MONTH_DECR_BUTTON));
	JPanel decrPanel =
	    new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
	decrPanel.add(yearDecrButton);
	decrPanel.add(monthDecrButton);

	// Set up the month/year label

	monthYearLabel = new JLabel();
	monthYearLabel.setHorizontalAlignment(JLabel.CENTER);

	// Set up the increment buttons

	monthIncrButton =
	    new JButton(
		new ButtonAction(
		    "MonthIncrButton",
		    "MonthIncrButtonMnemonic", "MonthIncrButtonAccelerator",
		    "MonthIncrButtonImage",
		    "MonthIncrButtonShort", "MonthIncrButtonLong",
		    MONTH_INCR_BUTTON));
	yearIncrButton =
	    new JButton(
		new ButtonAction(
		    "YearIncrButton",
		    "YearIncrButtonMnemonic", "YearIncrButtonAccelerator",
		    "YearIncrButtonImage",
		    "YearIncrButtonShort", "YearIncrButtonLong",
		    YEAR_INCR_BUTTON));
	JPanel incrPanel =
	    new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
	incrPanel.add(monthIncrButton);
	incrPanel.add(yearIncrButton);

	// Put them all together

	JPanel monthYearNavigator = new JPanel(new BorderLayout(2, 2));
	monthYearNavigator.add(decrPanel, BorderLayout.WEST);
	monthYearNavigator.add(monthYearLabel);
	monthYearNavigator.add(incrPanel, BorderLayout.EAST);

	// Set up the day panel

	JPanel dayPanel = new JPanel(new GridLayout(7, 7));
	int firstDay = displayCalendar.getFirstDayOfWeek();

	// Get the week day labels. The following technique is used so
	// that we can start the calendar on the right day of the week and
	// we can get the week day labels properly localized

	Calendar temp = Calendar.getInstance(locale);
	temp.set(2000, Calendar.MARCH, 15);
	while (temp.get(Calendar.DAY_OF_WEEK) != firstDay) {
	    temp.add(Calendar.DATE, 1);
	}
	dayOfWeekLabels = new JLabel[7];
	for (int i = 0; i < 7; i++) {
	    Date date = temp.getTime();
	    String dayOfWeek = formatWeekDay.format(date);
	    dayOfWeekLabels[i] = new JLabel(dayOfWeek);
	    dayOfWeekLabels[i].setHorizontalAlignment(JLabel.CENTER);
	    dayPanel.add(dayOfWeekLabels[i]);
	    temp.add(Calendar.DATE, 1);
	}

	// Add all the day buttons

	dayButtons = new JToggleButton[6][7];
	dayGroup = new ButtonGroup();
	DayListener dayListener = new DayListener();
	for (int row = 0; row < 6; row++) {
	    for (int day = 0; day < 7; day++) {
		dayButtons[row][day] = new JToggleButton();
		dayButtons[row][day].addItemListener(dayListener);
		dayPanel.add(dayButtons[row][day]);
		dayGroup.add(dayButtons[row][day]);
	    }
	}

	// We add this special button to the button group, so we have a
	// way of unselecting all the visible buttons

	offScreenButton = new JToggleButton("X");
	dayGroup.add(offScreenButton);

	// Combine the navigators and days

	datePanel.add(monthYearNavigator, BorderLayout.NORTH);
	datePanel.add(dayPanel);
    }

    // Create the time spinner field if we are displaying the time

    if ((selectedComponents & DISPLAY_TIME) > 0) {

	// Create the time component

	spinnerDateModel = new SpinnerDateModel();
	spinnerDateModel.addChangeListener(new TimeListener());
	spinner = new JSpinner(spinnerDateModel);

	JSpinner.DateEditor dateEditor =
	    new JSpinner.DateEditor(spinner, timePattern);
	dateEditor.getTextField().setEditable(false);
	dateEditor.getTextField().setHorizontalAlignment(JTextField.CENTER);
	spinner.setEditor(dateEditor);

	// Set the input/action maps for the spinner. (Only BACK_SPACE
	// seems to work!)

	InputMap sim = new InputMap();
	sim.put(KeyStroke.getKeyStroke("BACK_SPACE"), "setNullDate");
	sim.put(KeyStroke.getKeyStroke("DELETE"), "setNullDate");
	sim.setParent(spinner.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));

	ActionMap sam = new ActionMap();
	sam.put(
	    "setNullDate",
	    new AbstractAction("setNullDate") {
		public void actionPerformed(ActionEvent e) {
		    JCalendar.this.setDate(null);
		}});
	sam.setParent(spinner.getActionMap());

	spinner.setInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, sim);
	spinner.setActionMap(sam);

	// Create a special panel for the time display

	JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
	timePanel.add(spinner);

	// Now add it to the bottom

	datePanel.add(timePanel, BorderLayout.SOUTH);
    }

    setLayout(new BorderLayout(2, 2));
    add(datePanel);

    // Add today's date at the bottom of the calendar/time, if needed

    if (isTodayDisplayed) {
	Object[] args = {
	    new Date()
	};
	String todaysDate =
	    MessageFormat.format(bundle.getString("Today"), args);
	todaysLabel = new JLabel(todaysDate);
	todaysLabel.setHorizontalAlignment(JLabel.CENTER);

	// Add today's date at the very bottom

	add(todaysLabel, BorderLayout.SOUTH);
    }
}

/**
 * Update the calendar panel to display the currently selected date.
 */

private void
updateCalendarComponents()
{
    if ((selectedComponents & DISPLAY_DATE) > 0) {

	// Unselect all visible dates

	offScreenButton.setSelected(true);

	// Get the display date. We only need the month and year

	displayMonth = displayCalendar.get(Calendar.MONTH);
	displayYear = displayCalendar.get(Calendar.YEAR);

	// Get the localized display month name and year

	String month = formatMonth.format(displayCalendar.getTime());
	String year = Integer.toString(displayYear);

	{
	    Object[] args = {
		month,
		year
	    };
	    monthYearLabel.setText(
		MessageFormat.format(
		    bundle.getString("MonthYearTitle"), args));
	}

	// If the month or year have changed, we need to re-lay out
	// the days. Otherwise, we don't

	if (!month.equals(lastMonth) || !year.equals(lastYear)) {

	    // Create a temporary calendar that we will use to
	    // determine where day 1 goes and how many days there are
	    // in this month

	    Calendar temp = (Calendar)displayCalendar.clone();
	    temp.set(Calendar.DATE, 1);

	    int dayOfWeek = temp.get(Calendar.DAY_OF_WEEK);
	    int firstDay = temp.getFirstDayOfWeek();

	    // Determine how many blank slots occur before day 1 of this
	    // month

	    int dayPtr;
	    for (dayPtr = 0; dayPtr < 7; dayPtr++) {
		int curDay = ((firstDay - 1) + dayPtr) % 7 + 1;
		if (curDay != dayOfWeek) {
		    dayButtons[0][dayPtr].setText("");
		    dayButtons[0][dayPtr].setEnabled(false);
		}
		else {
		    break;
		}
	    }

	    // Determine the number of days in this month

	    int maxDays = temp.getActualMaximum(Calendar.DATE);

	    // Fill in the days

	    int row = 0;
	    for (int day = 1; day <= maxDays; day++) {
		dayButtons[row][dayPtr].setText(Integer.toString(day));
		dayButtons[row][dayPtr].setEnabled(true);

		// If this is the selected date, select the button;
		// otherwise, deselect it

		if (day == selectedDay &&
		    displayMonth == selectedMonth &&
		    displayYear == selectedYear) {
		    dayButtons[row][dayPtr].setSelected(true);
		}
		else {
		    dayButtons[row][dayPtr].getModel().setSelected(false);
		}

		// Wrap as needed

		dayPtr = (dayPtr + 1) % 7;
		if (dayPtr == 0) row++;
	    }

	    // Set the blanks slots after the last day

	    while (row < 6) {
		dayButtons[row][dayPtr].setText("");
		dayButtons[row][dayPtr].setEnabled(false);
		dayButtons[row][dayPtr].getModel().setSelected(false);
		dayPtr = (dayPtr + 1) % 7;
		if (dayPtr == 0) row++;
	    }
	}
    }

    // Update the time component, if displayed

    if ((selectedComponents & DISPLAY_TIME) > 0) {

	// If no date is selected, we set the date used by the time
	// field to today @ noon. We also make the field insensitive
	// -- the user must pick a non-null date before being able to
	// change the time (unless all we have is a time field)

	if (isNullDate) {
	    Calendar temp = (Calendar)selectedCalendar.clone();
	    temp.setTime(new Date());
	    temp.set(Calendar.HOUR, 12);
	    temp.set(Calendar.MINUTE, 0);
	    temp.set(Calendar.SECOND, 0);
	    spinnerDateModel.setValue(temp.getTime());
	    spinner.setEnabled((selectedComponents & DISPLAY_DATE) == 0);
	}

	// If a date is selected, use it

	else {
	    spinner.setEnabled(JCalendar.this.isEnabled());
	    spinnerDateModel.setValue(selectedCalendar.getTime());
	    spinnerDateModel.setStart(null);
	    spinnerDateModel.setEnd(null);
	    spinner.revalidate();
	}
    }
}

/**
 * Move backward one year.
 */

private void
yearBackward()
{
    displayCalendar.add(Calendar.YEAR, -1);
    updateCalendarComponents();
}

/**
 * Move forward one year.
 */

private void
yearForward()
{
    displayCalendar.add(Calendar.YEAR, 1);
    updateCalendarComponents();
}

/**
 * Move backward one month.
 */

private void
monthBackward()
{
    displayCalendar.add(Calendar.MONTH, -1);
    updateCalendarComponents();
}

/**
 * Move forward one month.
 */

private void
monthForward()
{
    displayCalendar.add(Calendar.MONTH, 1);
    updateCalendarComponents();
}

//**********************************************************************
// Inner Classes
//**********************************************************************

//**********************************************************************
// ButtonAction
//**********************************************************************

// This inner class is used to define the action associated with a
// button.

private class ButtonAction
      extends AbstractAction
{

public final static String SMALL = "16.gif";
public final static String LARGE = "24.gif";

ButtonAction(
    String name,
    String mnemonic,
    String accelerator,
    String image,
    String shortDescription,
    String longDescription,
    int actionId)
{
    if (name != null) {
	putValue(Action.NAME, bundle.getString(name));
    }

    if (mnemonic != null) {
	String mnemonicString = bundle.getString(mnemonic);
	if (mnemonicString != null && mnemonicString.length() > 0) {
	    putValue(Action.MNEMONIC_KEY,
		     new Integer(bundle.getString(mnemonic).charAt(0)));
	}
    }

    if (accelerator != null) {
	String acceleratorString = bundle.getString(accelerator);
	if (accelerator != null && acceleratorString.length() > 0) {
	    putValue(
		Action.ACCELERATOR_KEY,
		KeyStroke.getKeyStroke(acceleratorString));
	}
    }

    if (image != null) {
	String imageName = bundle.getString(image);
	if (imageName != null && imageName.length() > 0) {
	    imageName = "images/" + imageName + SMALL;
	    URL url = this.getClass().getResource(imageName);
	    if (url != null) {
		putValue(Action.SMALL_ICON, new ImageIcon(url));
	    }
	}
    }

    if (shortDescription != null) {
	String shortString = bundle.getString(shortDescription);
	if (shortString != null & shortString.length() > 0) {
	    putValue(Action.SHORT_DESCRIPTION, shortString);
	}
    }

    if (longDescription != null) {
	String longString = bundle.getString(longDescription);
	if (longString != null && longString.length() > 0) {
	    putValue(Action.LONG_DESCRIPTION, longString);
	}
    }

    putValue("buttonAction", new Integer(actionId));
}

public void
actionPerformed(
    ActionEvent e)
{
    Integer value = (Integer)getValue("buttonAction");
    switch (value.intValue()) {
    case YEAR_DECR_BUTTON:
	yearBackward();
	break;
    case YEAR_INCR_BUTTON:
	yearForward();
	break;
    case MONTH_DECR_BUTTON:
	monthBackward();
	break;
    case MONTH_INCR_BUTTON:
	monthForward();
	break;
    }
}

}

//**********************************************************************
// DayListener
//**********************************************************************

// This is called when a day button is pressed

private class DayListener
    implements ItemListener
{

public void
itemStateChanged(
    ItemEvent e)
{
    if (e.getStateChange() == ItemEvent.SELECTED) {
	int oldDay = selectedDay;
	int oldMonth = selectedMonth;
	int oldYear = selectedYear;

	String dayString = ((JToggleButton)e.getItem()).getText();
	try {
	    selectedDay = Integer.parseInt(dayString);
	}
	catch (Exception ex) {
	    selectedDay = 1;
	}
	selectedMonth = displayMonth;
	selectedYear = displayYear;

	if (oldDay != selectedDay ||
	    oldMonth != selectedMonth ||
	    oldYear != selectedYear) {

	    isNullDate = false;
	    selectedCalendar.set(Calendar.YEAR, selectedYear);
	    selectedCalendar.set(Calendar.MONTH, selectedMonth);
	    selectedCalendar.set(Calendar.DATE, selectedDay);

	    updateCalendarComponents();
	    fireDateChange();
	}
    }
}

}

//**********************************************************************
// TimeListener
//**********************************************************************

// Called whenever the time field changes

private class TimeListener
    implements ChangeListener
{

Calendar lastTemp = null;
Calendar temp = Calendar.getInstance(locale);

public void
stateChanged(
    ChangeEvent e)
{
    Date date = spinnerDateModel.getDate();

    // We only care about the time portion of the field. If no date is
    // selected, we shouldn't be able to change the time (unless there
    // is no date to select)

    if (!isNullDate || (selectedComponents & DISPLAY_DATE) == 0) {
	temp.setTime(date);
	temp.set(Calendar.YEAR, selectedCalendar.get(Calendar.YEAR));
	temp.set(Calendar.MONTH, selectedCalendar.get(Calendar.MONTH));
	temp.set(Calendar.DATE, selectedCalendar.get(Calendar.DATE));

	// Make sure we change the time only if necessary

	if (lastTemp == null || !lastTemp.equals(temp)) {
	    setDate(temp.getTime());
	    lastTemp = (Calendar)temp.clone();
	}
    }
}

}

//**********************************************************************
// End Inner Classes
//**********************************************************************

}
