/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javapwt;


import ij.ImagePlus;

import java.io.*;
import java.io.IOException;

import java.awt.Graphics;
import java.awt.Image;

import java.awt.image.BufferedImage;

import javax.media.Buffer;
import javax.media.CannotRealizeException;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.MediaTimeSetEvent;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.PrefetchCompleteEvent;


import javax.media.control.FrameGrabbingControl;
import javax.media.control.FramePositioningControl;
import javax.media.format.VideoFormat;
import javax.media.util.BufferToImage;

/**
 *
 * @author wawnx
 */
public class FrameReader implements ControllerListener {

    private boolean stateTransitionOK = true;
    private Object waitSync = new Object();
    private boolean error = false;
    private boolean failedInitialization = false;
    private Player player = null;
    private FramePositioningControl framePositioningControl = null;
    private FrameGrabbingControl frameGrabbingControl = null;

    public FrameReader(String filename) {
        MediaLocator mediaLocator = new MediaLocator(filename);
        Manager.setHint(Manager.PLUGIN_PLAYER, true);
        
        if(mediaLocator==null){
            System.out.println("Cannot build media locator from:"+filename);
            failedInitialization=true;
        }

        try {
          player = Manager.createRealizedPlayer(mediaLocator);
           
        } catch (IOException ioe) {
            failedInitialization = true;
            ioe.printStackTrace();
        } catch (NoPlayerException npe) {
            failedInitialization = true;
            npe.printStackTrace();
        } catch (CannotRealizeException cre) {
            failedInitialization = true;
            cre.printStackTrace();
        }; // try

        player.addControllerListener(this);

        try {
            framePositioningControl = (FramePositioningControl) player.getControl("javax.media.control.FramePositioningControl");
        } catch (Exception e) {
            failedInitialization = true;
            System.out.println("Exception (Snappy::framePositioningControl)");
            e.printStackTrace();
        }; // try

        if (framePositioningControl == null) {
            failedInitialization = true;
            System.out.println("FramePositioningControl failed");
        }; // if

        FrameGrabbingControl frameGrabbingControl = (FrameGrabbingControl) player.getControl("javax.media.control.FrameGrabbingControl");
        if (frameGrabbingControl == null) {
            failedInitialization = true;
            System.out.println("FrameGrabbingControl failed");
        }; // if

        //System.err.println( "\tbefore prefetch " );
        try {
            player.prefetch();
        } catch (Exception e) {
            failedInitialization = true;
            System.out.printf("failed prefetching");
            e.printStackTrace();
        }; // try

        //System.err.println( "\tbefore player.prefetch " );
        if (!waitForState(player.Prefetched)) {
            failedInitialization = true;
            System.out.printf("failed prefetch");
        }; // if

    }
    
    public Player getPlayer(){
        return player;
    }

    public ImagePlus grab(int frame) {
        if(failedInitialization){
            System.out.println("initialization failed. cannot grab");
            return null;
        }
        
        frameGrabbingControl = (FrameGrabbingControl) player.getControl("javax.media.control.FrameGrabbingControl");
        
        
        Buffer buffer = null;
        VideoFormat videoFormat = null;
        BufferToImage bufferToImage = null;
        BufferedImage bufferedImage = null;
        Graphics graphics = null;
        File outputFile = null;
        Image image = null;
        BufferedWriter outfile = null;
        int acturalFrame = framePositioningControl.seek(frame);
        if (acturalFrame != frame) {
            System.out.println("frame"+frame+"cannot be read");
            return null;
        }
   
        if(frameGrabbingControl==null){
            System.out.println("null grabber");
        }
        
        buffer = frameGrabbingControl.grabFrame();
        if(buffer==null){
            System.out.println("empty buffer");
        }
        else{
            System.out.println("success grab");
        }
        videoFormat = (VideoFormat) buffer.getFormat();
        bufferToImage = new BufferToImage(videoFormat);
        image = bufferToImage.createImage(buffer);
        ImagePlus grabbed = new ImagePlus("grabbed", image);
        return grabbed;
    }

    boolean waitForState(int state) {
        synchronized (waitSync) {
            try {
                while (player.getState() != state && stateTransitionOK) {
                    System.out.println( "Player state: " + player.getState() );
                    waitSync.wait();                  
                }; // while
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            } catch (Exception e) {
                System.out.println("waiting and then exception happens!");
                e.printStackTrace();
            }; // try
        }; // synchronized
        return stateTransitionOK;
    }

     // waitForState
    
    @Override
    public void controllerUpdate(ControllerEvent controllerEvent) {
            System.out.println(controllerEvent.toString());
        
        if (error == true) {
            System.out.println("error in controllerUpdate");
            return;
        }; // if
        try {
            //System.out.println( controllerEvent );
            if (controllerEvent instanceof javax.media.ControllerErrorEvent) {
                throw new Exception("ControllerErrorEvent, sorry, cannot process this video, bye");
            }; // if
            if (controllerEvent instanceof PrefetchCompleteEvent) {
                synchronized (waitSync) {
                    System.out.println("prefetch complete");
                    stateTransitionOK = true;
                    waitSync.notifyAll();               
                }
            }; // if
             if(controllerEvent instanceof EndOfMediaEvent)
		{   player.stop();
                    player.close();
	     };
             if(controllerEvent instanceof MediaTimeSetEvent){
  
             }
             
        } catch (Exception e) {
            System.out.println("Exception in the controllerUpdate");
            error = true;
            synchronized (waitSync) {
                stateTransitionOK = true;
                waitSync.notifyAll();
            }
        }; // try
    }
; // controllerUpdate
}
