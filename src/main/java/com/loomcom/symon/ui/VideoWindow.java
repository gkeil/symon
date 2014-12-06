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


import com.loomcom.symon.devices.Crtc;
import com.loomcom.symon.devices.DeviceChangeListener;
import com.loomcom.symon.exceptions.MemoryAccessException;

import javax.swing.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.*;

/**
 * VideoWindow represents a graphics framebuffer backed by a 6545 CRTC.
 * Each time the window's VideoPanel is repainted, the video memory is
 * scanned and converted to the appropriate bitmap representation.
 * <p>
 * The graphical representation of each character is derived from a
 * character generator ROM image. For this simulation, the Commodore PET
 * character generator ROM was chosen, but any character generator ROM
 * could be used in its place.
 * <p>
 * It may be convenient to think of this as the View (in the MVC
 * pattern sense) to the Crtc's Model and Controller. Whenever the CRTC
 * updates state in a way that may require the view to update, it calls
 * the <tt>deviceStateChange</tt> callback on this Window.
 */
public class VideoWindow extends JFrame implements DeviceChangeListener {

    private static final Logger logger = Logger.getLogger(VideoWindow.class.getName());

    
    /*
     *  Screen Constants
     */
    private static final int CHAR_WIDTH = 8;
    private static final int CHAR_HEIGHT = 8;
    
	public static final int HORIZONTAL_FREQ = 15625;			// NTSC-PAL horizontal frequency
	
	
	/*
	 * Pixel color stuff
	 */
	public static final int BACKGROUND_COLOR =	0xFF000000;		
	public static final int PIXEL_COLOR		 =	0xFF00C000;
	public static final int CURSOR_COLOR	 =	0xFF00FF00;
	
	private static final int bitsPerPixel 	= 2;	// 1 bit per pixel
	private static final int colorTableSize = 4;	// only 2 colors
	private static final byte[] red   = { 0x00, 	  0x00, 	  0x00, 	  0x00 };
	private static final byte[] green = { 0x00, (byte)0x80, (byte)0xC0, (byte)0xFF };	
	private static final byte[] blue  = { 0x00, 	  0x00, 	  0x00, 	  0x00 };
	 
	public static final IndexColorModel PixelColorModel = 	
				new IndexColorModel(bitsPerPixel, colorTableSize, red, green, blue);
	
    
    // Members
        
    private final int scaleX, scaleY;
    private final boolean shouldScale;

    private BufferedImage image;
    private int[] charRom;

    private int horizontalDisplayed;			// characters to be displayed in one row
    private int verticalDisplayed;				// number of character rows
    private int scanLinesPerRow;				// 
    private int cursorBlinkRate;
    private boolean hideCursor;

	private int pixels_per_line;				// number of pixels in a visible row			
	private int lines_per_frame; 				// total number of visible scan lines
	
	private PixelLinePanel	screenPane;			// Pane holding teh lines					
    
    private Dimension dimensions;
    private Crtc crtc;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> cursorBlinker;
    
    private ScreenScanning	screenScan;			// handles screen scan and refresh
    

    /**
     * VideoWindow Constructor
     * @param crtc
     * @param scaleX
     * @param scaleY
     * @throws IOException
     */
    
    public VideoWindow(Crtc crtc, int scaleX, int scaleY) throws IOException {
        crtc.registerListener(this);

        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.crtc = crtc;
        this.charRom = loadCharRom("/ascii.rom");
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.shouldScale = (scaleX > 1 || scaleY > 1);
        this.cursorBlinkRate = crtc.getCursorBlinkRate();

        if (cursorBlinkRate > 0) {
            this.cursorBlinker = scheduler.scheduleAtFixedRate(new CursorBlinker(),
                                                               cursorBlinkRate,
                                                               cursorBlinkRate,
                                                               TimeUnit.MILLISECONDS);
        }

        // Capture some state from the CRTC that will define the
        // window size. When these values change, the window will
        // need to re-pack and redraw.
        this.horizontalDisplayed = crtc.getHorizontalDisplayed();
        this.verticalDisplayed = crtc.getVerticalDisplayed();
        this.scanLinesPerRow = crtc.getScanLinesPerRow();

        // set the screen geometry
        this.pixels_per_line = horizontalDisplayed * CHAR_WIDTH;
        this.lines_per_frame = verticalDisplayed * scanLinesPerRow;
        
       
        // setup some frame properties
        this.setBounds(100, 100, pixels_per_line, lines_per_frame); 
        this.setTitle("Composite Video");
        
		// frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		

        // setup the panel with lines of pixels and add to the frame
		screenPane = new PixelLinePanel(pixels_per_line, lines_per_frame );		// Create Pixel Pane
		this.add(screenPane);													// add to frame

		//this.setResizable(false);

		
		/*
		 * Start the show
		 * Upon creation, ScreenScanning object triggers the 
		 * threads for screen updating
		 */
		
		screenScan = new ScreenScanning();
        				
		
    }	// end constructor

    
    
    /**
     * ScreenScanning
	 * This class handles screen updates
	 * Refresh is done one line at a time, at a rate specified
	 * by HORIZONTAL_FREQ. 
	 * A SwingWorker thread is created. teh doInBackground() reads from memory, 
	 * and creates a line image. The line update is done by EDT in teh Process(9 method.  
	 * 
	 * @author Guillermo Keil
	 *
	 */
	
	private class ScreenScanning {
		
		// target pixel 
		private int activeLine = 0;						// line to be updated
		private int charLine   = 0;						// row relative line
				
		private ScheduledExecutorService scheduler;		// to handle periodic update	
		private ScheduledFuture<?>	ScreenRefresh;
		
		private PixelUpdate updateTask;				// swing worker thread
		private TimeBase	tb;						// Timer task to trigger periodic updates
		
		private Object syncObj		= new Object();			// used to sync threads.		
		private boolean	updateNow 	= false;				// screen update trigger
		
		/* *************
		 * Constructor
		 * *************/
		
		public ScreenScanning() {
		
			// create the Swing Worker object, and start it
			updateTask = new PixelUpdate();
			updateTask.execute();
			
			// create the time base object
			tb = new TimeBase();			
			
			// create Executor Service
			scheduler =	Executors.newSingleThreadScheduledExecutor();
			
			// schedule the update task to be run periodically, and start with 0 delay
			//ScreenRefresh = scheduler.scheduleAtFixedRate(tb, 0, 1000000/HORIZONTAL_FREQ, TimeUnit.MICROSECONDS);
			ScreenRefresh = scheduler.scheduleAtFixedRate(tb, 0, (long)64, TimeUnit.MICROSECONDS);
			
			// TODO Remove later
			// investigate contents of ROM array
			int chr = 0x00;
			int idx = 0;
			
			for ( int chline = 0; chline < scanLinesPerRow; chline++) {
				// get the pixels info to be set on screen
				int romOffset = (chr & 0xff) * (CHAR_HEIGHT * CHAR_WIDTH) + chline * CHAR_WIDTH;
	        
				for (int px = 0; px < CHAR_WIDTH; px++ ) {
	        	
					//char cc = ( charRom[romOffset+px] != 0 ) ? 'X' : '.'; 
					char cc = ( charRom[(chr & 0xff) * (CHAR_HEIGHT * CHAR_WIDTH) + idx++] != 0 ) ? 'X' : '.';
					System.out.print(cc);
	        	
				}
				System.out.println();
			}
			
			
			
		}	// end constructor
		
		
		/**
		 * TimeBase
		 * 
		 * This class implements a runnable object that is called
		 * at the Horizontal freq. rate. When executed it notifies
		 * doInBackground() that it is time to a scan line update.
		 * This is necessary because the SwingWorker threads can be 
		 * executed only once. So it cannot directly triggered by the 
		 * scheduled executor.  
		 * 
		 * @author Guillermo Keil 
		 *
		 */
		
		private class TimeBase implements Runnable {

			@Override
			public void run() {
				
				System.out.println("In  TimeBase.run()start :" + System.nanoTime());
				// wait for intrinsic lock 
				synchronized( syncObj ) {
					updateNow = true;
					syncObj.notifyAll();
				}	// end synchronized block
				
			}	//end run
			
		}	// end TimeBase
		
		
		/**
		 * PixelUpdate
		 * This class implements the Swing Worker thread that updates screen based on LinePos
		 * doInBackground() regenerates the line image based on characters and cursor.
		 *   
		 *  
		 * @author Guillermo Keil
		 *
		 */		
		private class PixelUpdate extends SwingWorker<Object, Integer> {
			
			// Dummy doInBackground for time measuring
			protected Object doInBackground() throws Exception {
				return null;
			}
			/** 
			 * doInBackground()
			 * The update computation is don here. At the end calls publish( Point )
			 *  			
			 */
			/*@Override
			protected Object doInBackground() throws Exception {
				
				// create the working variables before the loop start to minimize overhead
				// during periodic refresh
				int row = 0;				// row in screen
				int memOffset = 0;			// offset in video memory
				int address;				// absolute video memory address
				int romOffset = 0;			// offset into character rom memory
				int chr;					// character to display
				
				// keep updating line images 
				while(true) {
					
					// wait until it is time to update
					synchronized( syncObj ) {
						
						while ( !updateNow ) {
							syncObj.wait();
						}	
						updateNow = false;
					}	// end synchronized block
					
					
					//System.out.println("In doInBack start :" + System.nanoTime());
					
					
					// 
					// create the image in the active line
					
					row = activeLine / scanLinesPerRow;	// get the row we are drawing
					
					// TODO Cursor stuff
					
					// Update lines based on char online CHAR_HEIGH lines
					if ( charLine < CHAR_HEIGHT) {
						
						// iterate on the columns
						for ( int col = 0 ; col < horizontalDisplayed; col++) {

							// get the char for this column
							memOffset = row * horizontalDisplayed + col;						
							address = crtc.getStartAddress() + memOffset;
							chr = crtc.getCharAtAddress(address);

							// get the pixels info to be set on screen
							romOffset = (chr & 0xff) * (CHAR_HEIGHT * CHAR_WIDTH) + charLine * CHAR_WIDTH;

							for (int px = 0; px < CHAR_WIDTH; px++ ) {

								screenPane.setPixel( col* CHAR_WIDTH + px,		// x pos
										activeLine,								// y pos 
										( charRom[romOffset+px] != 0 )  );		// pixel set or clear


							}	// end for px


						}	// end for col
										
					}	// end if
					
					// notify EDT to update a line
					publish( new Integer( activeLine) );

					//TODO System.out.println("active=" + activeLine+" charline=" + charLine);
					
					// prepare for next update
					activeLine++;						// point to next line
					if (activeLine >= lines_per_frame)
						activeLine = 0;					// start new frame
					
					
					charLine++;							// next line of this row
					if ( charLine >= scanLinesPerRow )	{
						charLine = 0;					// start a new row

			
					}
				

					//System.out.println("In doInBack end :" + System.nanoTime());
				
				}	// end while(true)
				
				
				
			}	// end doInBackground
*/
			/** 
			 * process()
			 * This method is called from the EDT
			 * It will do the actual Swing Object update. 
			 * 
			 */
			@Override
			protected void process(List<Integer> chunks) {
								
				// repaint all the line that need update 
				for ( Integer line : chunks )
				{
					// repaint the target line 
					screenPane.getLinePanel(line).repaint();
					
				}
								
				super.process(chunks);
			}	// end process
				
			
		} // end PixelUpdate
		
		
	}	// end ScreenScanning
	
    
    
    
    
    
    
    /**
     * A panel representing the composite video output, with fast Graphics2D painting.
     *//*
    private class VideoPanel extends JPanel {
        @Override
        public void paintComponent(Graphics g) {
            try {
                for (int i = 0; i < crtc.getPageSize(); i++) {
                    int address = crtc.getStartAddress() + i;
                    int originX = (i % horizontalDisplayed) * CHAR_WIDTH;
                    int originY = (i / horizontalDisplayed) * scanLinesPerRow;
                    image.getRaster().setPixels(originX, originY, CHAR_WIDTH, scanLinesPerRow, getGlyph(address));
                }
                Graphics2D g2d = (Graphics2D) g;
                if (shouldScale) {
                    g2d.scale(scaleX, scaleY);
                }
                g2d.drawImage(image, 0, 0, null);
            } catch (MemoryAccessException ex) {
                logger.log(Level.SEVERE, "Memory Access Exception, can't paint video window! " + ex.getMessage());
            }
        }

        @Override
        public Dimension getMinimumSize() {
            return dimensions;
        }

        @Override
        public Dimension getPreferredSize() {
            return dimensions;
        }

    }
*/
    /**
     * Runnable task that blinks the cursor.
     */
    private class CursorBlinker implements Runnable {
        public void run() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (cursorBlinkRate > 0) {
                        hideCursor = !hideCursor;
                        repaint();
                    }
                }
            });
        }
    }

    
    /**
     * Called by the CRTC on state change.
     */
    public void deviceStateChanged() {

        boolean repackNeeded = false;

        // TODO: I'm not entirely happy with this pattern, and I'd like to make it a bit DRY-er.

        if (horizontalDisplayed != crtc.getHorizontalDisplayed()) {
            horizontalDisplayed = crtc.getHorizontalDisplayed();
            repackNeeded = true;
        }

        if (verticalDisplayed != crtc.getVerticalDisplayed()) {
            verticalDisplayed = crtc.getVerticalDisplayed();
            repackNeeded = true;
        }

        if (scanLinesPerRow != crtc.getScanLinesPerRow()) {
            scanLinesPerRow = crtc.getScanLinesPerRow();
            repackNeeded = true;
        }

        if (cursorBlinkRate != crtc.getCursorBlinkRate()) {
            cursorBlinkRate = crtc.getCursorBlinkRate();

            if (cursorBlinker != null) {
                cursorBlinker.cancel(true);
                cursorBlinker = null;
                hideCursor = false;
            }

            if (cursorBlinkRate > 0) {
                cursorBlinker = scheduler.scheduleAtFixedRate(new CursorBlinker(),
                                                              cursorBlinkRate,
                                                              cursorBlinkRate,
                                                              TimeUnit.MILLISECONDS);
            }
        }

        if (repackNeeded) {
            buildImage();
            invalidate();
            pack();
        }
    }

    /*private void createAndShowUi() {
    	setTitle("Composite Video");

        int borderWidth = 20;
        int borderHeight = 20;

        JPanel containerPane = new JPanel();
        containerPane.setBorder(BorderFactory.createEmptyBorder(borderHeight, borderWidth, borderHeight, borderWidth));
        containerPane.setLayout(new BorderLayout());
        containerPane.setBackground(Color.black);

        containerPane.add(new VideoPanel(), BorderLayout.CENTER);

        getContentPane().add(containerPane, BorderLayout.CENTER);
        setResizable(false);
        pack();
    }*/

    /**
     * Returns an array of pixels (including extra scanlines, if any) corresponding to the
     * Character ROM plus cursor overlay (if any). The cursor overlay simulates an XOR
     * of the Character Rom output and the 6545 Cursor output.
     *
     * @param address The address of the character being requested.
     * @return An array of integers representing the pixel data.
     */
    private int[] getGlyph(int address) throws MemoryAccessException {
        int chr = crtc.getCharAtAddress(address);
        int romOffset = (chr & 0xff) * (CHAR_HEIGHT * CHAR_WIDTH);
        int[] glyph = new int[CHAR_WIDTH * scanLinesPerRow];

        // Populate the character
        arraycopy(charRom, romOffset, glyph, 0, CHAR_WIDTH * Math.min(CHAR_HEIGHT, scanLinesPerRow));

        // Overlay the cursor
        if (!hideCursor && crtc.isCursorEnabled() && crtc.getCursorPosition() == address) {
            int cursorStart = Math.min(glyph.length, crtc.getCursorStartLine() * CHAR_WIDTH);
            int cursorStop = Math.min(glyph.length, (crtc.getCursorStopLine() + 1) * CHAR_WIDTH);

            for (int i = cursorStart; i < cursorStop; i++) {
                glyph[i] ^= 0xff;
            }
        }

        return glyph;
    }

    private void buildImage() {
        int rasterWidth = CHAR_WIDTH * horizontalDisplayed;
        int rasterHeight = scanLinesPerRow * verticalDisplayed;
        this.image = new BufferedImage(rasterWidth, rasterHeight, BufferedImage.TYPE_BYTE_BINARY);
        this.dimensions = new Dimension(rasterWidth * scaleX, rasterHeight * scaleY);
    }

    /**
     * Load a Character ROM file and convert it into an array of pixel data usable
     * by the underlying BufferedImage's Raster.
     * <p>
     * Since the BufferedImage is a TYPE_BYTE_BINARY, the data must be converted
     * into a single byte per pixel, 0 for black and 255 for white.

     * @param resource The ROM file resource to load.
     * @return An array of glyphs, each ready for insertion.
     * @throws IOException
     */
    private int[] loadCharRom(String resource) throws IOException {
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(this.getClass().getResourceAsStream(resource));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while (bis.available() > 0) {
                bos.write(bis.read());
            }
            bos.flush();
            bos.close();

            byte[] raw = bos.toByteArray();

            // Now convert the raw ROM image into a format suitable for
            // insertion directly into the BufferedImage.
            int[] converted = new int[raw.length * CHAR_WIDTH];

            int romIndex = 0;
            for (int i = 0; i < converted.length;) {
                byte charRow = raw[romIndex++];

                for (int j = 7; j >= 0; j--) {
                    converted[i++] = ((charRow & (1 << j)) == 0) ? 0 : 0xff;
                }
            }
            return converted;
        } finally {
            if (bis != null) {
                bis.close();
            }
        }
    }
}
