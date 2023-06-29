//**********************************************************************
// Package
//**********************************************************************

package ch.specchio.gui;

//**********************************************************************
// Import list
//**********************************************************************

import org.joda.time.DateTime;

import java.util.EventObject;
import java.util.Calendar;

/**
 * This class holds information related to a date change in a
 * calendar.
 *
 * @see JCalendar
 * @see JCalendarCombo
 * @author Antonio Freixas
 */

// Copyright (c) 2003 Antonio Freixas
// All Rights Reserved.

public class DateEvent
    extends EventObject
{

//**********************************************************************
// Private Members
//**********************************************************************

private DateTime selectedDate;

//**********************************************************************
// Constructors
//**********************************************************************

/**
 * Create a date event.
 *
 * @param source The object on which the event occurred.
 * @param selectedDate The selected date.
 */

public
DateEvent(
    Object source,
    DateTime selectedDate)
{
    super(source);
    this.selectedDate = selectedDate;
}

//**********************************************************************
// Public
//**********************************************************************

/**
 * Return the selected date.
 *
 * @return The selected date.
 */

public DateTime
getSelectedDate()
{
    return selectedDate;
}

//The default equals and hashCode methods are acceptable.

/**
 * {@inheritDoc}
 */

public String
toString()
{
    return
	super.toString() + ",selectedDate=" +
	//selectedDate.getTime().toString();
            selectedDate.toString();
}

}
