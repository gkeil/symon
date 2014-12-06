/*
 * Copyright (c) 2014 Seth J. Morabito <web@loomcom.com>
 *                    Guillermo Keil <gkeil@arnet.com.ar>
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.loomcom.symon.ui;

import java.awt.BorderLayout;

import javax.swing.BoxLayout;
import javax.swing.JPanel;


/**
 * This Class represents a panel composed of lines of pixels.
 * This Panel contains all the lines of the screen
 * aligned from Top to Botton by means of BoxLayout Manager.  
 * 
 *   
 *  
 * @author Guillermo Keil
 *
 */

public class PixelLinePanel extends JPanel {

	
	/* **********************************
	 * Members
	 * 
	 * **********************************/
	
	private PixelLine 	lineArray[];				// array with actual line object 
	
	private int			lines_per_frame;			// number of lines in a frame
	private int			pixels_per_line;			// pixels in one line
	
	/* **********************************
	 * Constructor
	 * 
	 * **********************************/
	
	/**
	 * Creates the panel object and  creates all the lines instances.
	 * Lines objects are stored in an array and added to the panel
	 */
	public PixelLinePanel( int pixels_per_line, int lines_per_frame ) {
		
		// save geometry values
		this.lines_per_frame = lines_per_frame;
		this.pixels_per_line = pixels_per_line;
		
		// allocate array
		lineArray = new PixelLine[ this.lines_per_frame ]; 
		
		// set the layout manager.
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS) );
		
		// create and add the lines of pixels
		for ( int i= 0; i < lines_per_frame; i++) {
			
			lineArray[i] = new PixelLine( this.pixels_per_line);				// create object
			
			this.add(lineArray[i]);						// add to panel
			
		}	// end for
		
	}	// end constructor
	
	
	/* *************************************
	 * METHODS
	 * 
	 * *************************************/
	
	/**
	 * setPixel()
	 * Sets or clears  a given pixel based on the value argument
	 * It calls the setPixel of the target line object.
	 * pixel is identified by x, y screen coordinates
	 */
	public void setPixel( int x, int y, boolean value) {
		
		// Y points to the line object
		lineArray[ y ].setPixel( x, value);
		
	}	// end setPixel()
	
	/**
	 * setPixel()
	 * Sets a pixel to a given color specified in the  argument
	 * It calls the setPixel of the target line object.
	 * pixel is identified by x, y screen coordinates
	 */
	public void setPixel( int x, int y, int rgb) {
		
		// Y points to the line object
		lineArray[ y ].setPixel( x, rgb);
		
	}	// end setPixel()
	
	/**
	 * getLinePanel()
	 * Returns a reference to the PixelLine object of the given line
	 * 
	 */
	public PixelLine getLinePanel( int l ) {
		
		return lineArray[ l ];
		
	}	// end getLinePanel
	
	
}	// end class
