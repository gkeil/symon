package com.loomcom.symon.ui;

public class TimeTrack {

	static final int MAX_TOPS = 10;
	static long st[] = new long[MAX_TOPS];
	
	
	static void startTime(int top) {
		
		if ( top < MAX_TOPS )
			st[top] = System.nanoTime();
	}
	
	static void showDeltaTime( int top ) {
		
		if ( top < MAX_TOPS )
			System.out.println( "Delta " + top + "is " + (System.nanoTime() - st[top]));
		
		
	}
	
	
}
