/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javapwt;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Modified by Aleman-Meza (September-2012)
 * @author wawnx
 */
public class MotionDetectionSetting {

	/** name of properties file */
    public static final String PROPERTIES_FILENAME = "motionDetection.properties";
	
    private int minArea = 400;
    private int  maxArea = 4000;
    private int nPieceX = 7;
    private int nPieceY = 9;
    private double thresh = 0.2;
    private double difThresh=0.1;
    private int difSize=100;
   
   public int getDifSize(){
       return difSize;
   }
   public void setDifSize(int x){
       difSize=x;
   }
   
   public double getDifThresh(){
       return difThresh;
   }
    
   public void setDifThresh(double x){
       difThresh=x;
   }
    
    public int getMinArea(){
        return minArea;
    }
    public void setMinArea(int x){
        minArea=x;
    }
    
    public int getMaxArea(){
        return maxArea;
    }
    public void setMaxArea(int x){
        maxArea=x;
    }
    
    
    public double getThresh(){
        return thresh;
    }
    
    public void setThresh(double x){
        thresh=x;
    }
    
    public int getPieceX(){
        return nPieceX;
    }
    public void setPieceX(int x){
        nPieceX=x;
    }
      
    public int getPieceY(){
        return nPieceY;
    }
    public void setPieceY(int x){
        nPieceY=x;
    }
    
    /**
     * Loads motion detection settings from a folder
     * @param  folder  the folder containing the settings file
     * @return  a newly created motion detection setting object, or null when something goes wrong
     */
    public static MotionDetectionSetting loadMotionDetectionSettingFromFile( String folder ) {
    	if( folder == null ) {
    		return null;
    	}; // if
    	if( folder.endsWith( File.separator ) == false ) {
    		folder += File.separator;
    	}; // if
        File file = new File( folder + PROPERTIES_FILENAME );
        if( file.exists() == false ) {
            return null;
        }; // if
        MotionDetectionSetting ret = new MotionDetectionSetting();
        try {
            Properties property = new Properties();
            property.load( new FileInputStream( file ) );
            ret.setMinArea( Integer.parseInt( property.getProperty( "minArea" ) ) );
            ret.setMaxArea( Integer.parseInt( property.getProperty( "maxArea" ) ) );
            ret.setThresh( Double.parseDouble( property.getProperty( "thresh" ) ) );
            ret.setPieceX( Integer.parseInt( property.getProperty( "nPieceX" ) ) );
            ret.setPieceY( Integer.parseInt( property.getProperty( "nPieceY" ) ) );
            ret.setDifSize( Integer.parseInt( property.getProperty("difSize") ) );
            ret.setDifThresh( Double.parseDouble( property.getProperty( "difThresh" ) ) );
        } 
        catch( Exception ex ) {
            Logger.getLogger(Tracker.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }; // try
        return ret;
    }; // loadMotionDetectionSettingFromFile
    
    
}
