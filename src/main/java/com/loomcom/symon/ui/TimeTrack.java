package com.loomcom.symon.ui;

/**
 * This class is intended to provide Time tracking tools.
 * All the members and methods provided are static so that
 * they can used without the need of creating TimeTracking objects.
 * Time line is compossed of nanoTime stored in a array of longs
 * @author GKEIL
 *
 */
public class TimeTrack {

	/*
	 **************************
	 * TimeTrack class members
	 **************************/
	
	private static final int MAX_TOPS = 100;			// max independent time lines
	private static long st[] = new long[MAX_TOPS];		// array of longs that holds the
	private static String stDesc[] = new String[MAX_TOPS];	// String array to store a description of the moment
	private static int event = 0;						// position in array to to set next time 
	
	
	/*
	 * **************************
	 * Timetracking provide methods
	 *******************************/
	
	
	/**
	 * resetTimeline()
	 * This methods reset TimeTrack variables to start a new time line
	 */
	public static void resetTimeLine() {
		
		event = 0;							// reset event number
	
		for ( int i=0; i < MAX_TOPS; i++) {	// clear tops array
			st[i] = 0;
			stDesc[i] = "";
		
		}	// end for
		
	}	// end resetTimeLine
	
	/**
	 * top()
	 * This methods stores a new time value on the next available position in the array
	 * Time table is constructed by storing succesive time values
	 */
	public static void top( String moment) {
	
		if ( event < MAX_TOPS ) {
			st[event++] = System.nanoTime();		// store a time value and point to next position
			stDesc[event++] = moment;				// store corresponding moment description
		}
		
	}	// end top
	

	/**
	 * getTiemLineLength()
	 * Returns the number of time values stored in the time line
	 */
	
	public static int getTimeLineLength() {
		
		return event;		// the are events time values stored
		
	}	// end getTimeLineLength
	
	
	
	
	
		
}	// end TimeTracking class
