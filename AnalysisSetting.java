/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javapwt;

/**
 *
 * @author wawnx
 */
public class AnalysisSetting {
    
    private double frameRate = (double) 7.50;
    private double calibFactor = (double) 47;
    private double smoothingWindow = 5;
    private double calcStep = 5;
    private double binSpacing = 0.01;
    private double maxBin = 0.5;
    private double speedThresh = 0.015;
    private double duration=30.0;
    
    public double getFrameRate(){
        return frameRate;
    }
    public void setFrameRate(double x){
        frameRate=x;
    }
    public double getCalibFactor(){
        return calibFactor;
    }
    public void setCalibFactor(double x){
        calibFactor=x;
    }    
    
     public double getDuration(){
        return duration;
    }
    public void setDuration(double x){
        duration=x;
    }    
    public double getSmoothingWindow(){
        return smoothingWindow;
    }
    public void setSmoothingWindow(double x){
        smoothingWindow=x;
    }
    public double getCalcStep(){
        return calcStep;
    }
    public void setCalcStep(double x){
        calcStep=x;
    }    
    public double getBinSpacing(){
        return binSpacing;
    }
    public void setBinSpacing(double x){
        binSpacing=x;
    }
    public double getMaxBin(){
        return maxBin;
    }
    public void setMaxBin(double x){
        maxBin=x;
    }
}
