/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javapwt;

/**
 *
 * @author wawnx
 */
public class TrackingSetting {
    private int minSize = 60;
    private int maxSize = 300;
    private double maxShift = 20;
    private double maxSizeChange = 20;
    private int minTrackLength = 100;
    private int drawInterval = 10;
    private double level = 0.6;
    
    
    public int getMinSize(){
        return minSize;
    }
    public int getMaxSize(){
        return maxSize;
    }
    
    public double getMaxShift(){
        return maxShift;
    }
    
    public double getMaxSizeChange(){
        return maxSizeChange;
    }
    
    public int getMinTrackLength(){
        return minTrackLength;
    }
    
    public int getDrawInterval(){
        return drawInterval;
    }
    
    public double getlevel(){
        return level;
    }
    
    public void setMinSize(int x){
        minSize=x;
    }
    
    public void setMaxSize(int x){
        maxSize=x;
    }
    
    public void setMaxShift(double x){
        maxShift=x;
    }
    
    public void setMaxSizeChange(double x){
        maxSizeChange=x;
    }
    
    public void setMinTrackLength(int x){
        minTrackLength=x;
    }
    
    public void setDrawInterval(int x){
        drawInterval=x;
    }
    
    public void setLevel(double x){
        level=x;
    }
    

    
}
