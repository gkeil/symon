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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;



/**
 * This Class represents a line of pixels.
 * The line is represented as a JPanel of 1 pixel height that contains 
 * a Buffered Image of the same dimensions
 * 
 *  The class provides a constructor,  and a methods to set the color
 *  of a given pixel.
 *  paint() and getxxxSize methods are overriden 
 *  
 * @author Guillermo Keil
 *
 */
public class PixelLine extends JPanel {

	
	/* *****************************************
	 * Members
	 * *****************************************/
	
	
	private BufferedImage 		lineImg;		// image associated with the line of pixel.
	private int					width;			// pixels in a line			
		
	private final Dimension lineDim = new Dimension(width, 1);	// line dimensions
	
	/* *****************************************
	 *	Constructor 
	 * *****************************************/
	
	/**
	 * Creates a new Pixel object.
	 * The associated image is also created.
	 * All the pixels are initialized to screen background.
	 */
	  
	public PixelLine( int width ) {
		
		this.width = width;			// save desired width
		
		/*
		 * create the image that represents the pixel
		 * 1x1 pixel area. 
		 * TYPE_BYTE_BINARY with a 2 bits per pixel color mode
		 */
		
		lineImg = new BufferedImage( width, 1, 
										BufferedImage.TYPE_BYTE_BINARY,
										VideoWindow.PixelColorModel);
		
		// clear all pixels
		for (int i = 0; i < width; i++) {
			
			// set pixel to background color
			lineImg.setRGB(i, 0, VideoWindow.BACKGROUND_COLOR);
		}
				
					
	}	// end constructor
	
	
	/* **************************************
	 * METHODS
	 * **************************************/
	
	/**
	 * setPixel()
	 * Sets or clears a given pixel on the line.
	 */
	
	public void setPixel( int pix, boolean value ) {
		
		// set to Back or FWD color based on argument
		// true sets to FWD, false to BACKGROUND
		
		if ( value )
			lineImg.setRGB(pix, 0, VideoWindow.PIXEL_COLOR);
		else
			lineImg.setRGB(pix, 0, VideoWindow.BACKGROUND_COLOR);
		
		// TODO remove later
		TimeTrack.addEvent(" after update in PixelLine");
		
	}	// end etPixel
	
	
	/**
	 * setPixel()
	 * This version allows to control the color intensity.
	 */
	
	public void setPixel( int pix, int rgb ) {
		
		// set the pixel color to the provided in the argument
		// the pixel will be set to the closest one in the color table		
				
		lineImg.setRGB(pix, 0, rgb);
		
	}	// end etPixel
	
	
	/**
	 * paint()
	 * This is the override of paint() in the parent class.
	 * The paint() draws the on the JPanel.
	 */
	public void paint( Graphics g ) {
		
		// paint the line of pixel
		g.drawImage(lineImg, 0, 0, null);
		
		
		
	}	// end paint
	
	/**
	 * getPreferredSize()
	 * Override to return always the size of a line
	 */
	
	public Dimension getPreferredSize() {
		
		// size of one line
		return lineDim;
		
	}	// end getPreferredSize
	
	/**
	 * getMinimumSize()
	 * Override to return always the size of a line
	 */
	
	public Dimension getMinimumSize() {
		
		// same as preferred
		return getPreferredSize();
		
	}	// end getMinimumSize
	
	/**
	 * getMaximumSize()
	 * Override to return always the size of a line
	 */
	
	public Dimension getMaimumSize() {
		
		// same as preferred
		return getPreferredSize();
		
	}	// end getMaximumSize
	
}	// end Class
