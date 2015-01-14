package com.loomcom.symon.ui;

import javax.naming.event.EventDirContext;

public class TimeTrack {

	// this is teh stuf for multiple start time 
	private static final int MAX_TOPS = 10;
	private static long st[] = new long[MAX_TOPS];
	
	// stuff for storing times for shown later
	private static final int MAX_TIME_EVENTS = 100;
	private static long  eventSt;
	private static int eventIdx;
	private static long eventDelta[] = new long[MAX_TIME_EVENTS]; 
	private static String[] eventRefStr = new String[MAX_TIME_EVENTS];

	
	/*
	 * Multiple Start Time methods 
	 * 
	 *******************************/
	public static void startTime(int top) {
		
		if ( top < MAX_TOPS )
			st[top] = System.nanoTime();
	}
	
	
	public static void showDeltaTime( int top, String str ) {
		
		if ( top < MAX_TOPS )
			System.out.println( str +" Delta [" + top + "] is " + (System.nanoTime() - st[top]));
		
		
	}
	
	/*
	 * Multiple events stuff
	 * 
	 */
	static void resetEventList() {
		
		eventSt = System.nanoTime();
		eventIdx = 0;
	}
	
	static void addEvent(String str ) {
		
		
		if ( eventIdx < MAX_TIME_EVENTS ) {
			
			eventDelta[eventIdx]  = System.nanoTime() - eventSt;
			eventRefStr[eventIdx] = str;
			eventIdx++;
		} 
	}
	
	static void showEventsInfo() {
		
		for ( int i=0; i < eventIdx; i++)
			System.out.println( eventRefStr[i] + " " + eventDelta[i]);
	}
	
	
}
