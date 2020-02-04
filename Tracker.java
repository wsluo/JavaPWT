package javapwt;

import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.io.FileSaver;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.awt.Color;
import java.awt.Graphics;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author wawnx
 */
public class Tracker implements Runnable {

    Graphics graphics;
    Thread runner = null;
    private int currentFrame = 0;
    private FrameReader frameReader = null;
    private String name = null;
    private ImagePlus showPlus = null;
    private ImageWindow showWindow=null;
    private LinkedList<LinkedList> activeList = new LinkedList();
    private int systemMeasurements = Measurements.AREA + Measurements.MEAN + Measurements.MIN_MAX + Measurements.CENTROID;
    private int options = ParticleAnalyzer.SHOW_RESULTS + ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES;
    private boolean displayWindow=false;
    
    //default preferences
    private int minSize = 60;
    private int maxSize = 300;
    private double maxShift = 20;
    private double maxSizeChange = 20;
    private int minTrackLength = 100;
    private int drawInterval = 10;
    private double level = 0.6;
    int validFrame=0;
    
    

    public Tracker(String filename, String suffix) {
        frameReader = new FrameReader("file:\\" + filename + suffix);
        name = filename;
        //runner=new Thread(this);
        if (frameReader == null) {
            System.out.println("null frame reader");
        }
    }
    
        public Tracker(String filename, String suffix,TrackingSetting setting,Boolean show) {          
        minSize=setting.getMinSize();
        maxSize=setting.getMaxSize();
        maxShift=setting.getMaxShift();
        maxSizeChange=setting.getMaxSizeChange();
        minTrackLength=setting.getMinTrackLength();
        drawInterval=setting.getDrawInterval();
        level=setting.getlevel();
        displayWindow=show;
        //runner=new Thread(this);
        frameReader = new FrameReader("file:\\" + filename + suffix);
        name = filename;
        if (frameReader == null) {
            System.out.println("null frame reader");
        }
    }
       //PROBLEM: BFS 处理单个文件的时候自动load

    public void writeTracks() {
        try {
            FileOutputStream fs = new FileOutputStream(name + ".file");
            ObjectOutputStream os = new ObjectOutputStream(fs);
            os.writeObject(activeList);
            os.writeInt(validFrame);           
            System.out.println("write num of lists:" + activeList.size());
            System.out.println("write num of speedpoints:" + activeList.get(0).size());
            os.close();
        } catch (IOException ex) {
            Logger.getLogger(Tracker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    
    public int trackFrame(int i) {
        //grab frame
        ImagePlus grb = frameReader.grab(i);
        if (grb == null) {
            frameReader.getPlayer().stop();
            frameReader.getPlayer().close();
            return -1;
        }
        ImagePlus grabbed = grb.duplicate();
        //convert to grayscale
        if (grabbed.getBitDepth() != 8) {
            ImageConverter icv = new ImageConverter(grabbed);
            icv.convertToGray8();
        }
        
           ImageStatistics is = grabbed.getStatistics();
           double mean = is.mean;
           if(mean!=0){validFrame++;}

        grabbed.getProcessor().invertLut();
        grabbed.getProcessor().threshold((int) (level * 255));
        // grabbed.getProcessor().autoThreshold();

        grabbed.getProcessor().setThreshold(0, 0, ImageProcessor.NO_LUT_UPDATE);//for particle analyzer

        ResultsTable rt = new ResultsTable();
        ParticleAnalyzer analyzer = new ParticleAnalyzer(options, systemMeasurements, rt, minSize, maxSize, 0.0, 1.0);
        analyzer.analyze(grabbed);
        updateActiveTracks(rt);
        
        if (showPlus == null) {
            showPlus = grb;
            showWindow=new ImageWindow(showPlus);
            showWindow.setTitle(name);
            if(displayWindow==false){
            showWindow.setVisible(false);}
            grb.getProcessor().setColor(java.awt.Color.RED);
        } else {
            if (i % drawInterval == 1) {
                //grb.getProcessor().setColor(java.awt.Color.RED);
                showPlus.setImage(grb);
                showPlus.getProcessor().setColor(Color.red);
                drawTracks();
                System.out.println(graphics == null);
                showWindow.setImage(showPlus);
                
                //Outputing video with tracks
                   // FileSaver saver = new FileSaver(showPlus);
                    //saver.saveAsJpeg("F:/tracks" + File.separator + "frame_"+i+".jpeg");
                //graphics.drawImage(showPlus.getImage(), 0, 0, this);
            }
        }
        // rt.show("result");
        rt.reset();
        return 1;
    }

    public void trackWorms() {
        int i = 0;
        while (true) {
            int x = trackFrame(i);
            if (x == -1) {
                break;
            }
            i++;
        }//end of while
        validateTracks();
    }

    //finally, remove tracks that are too short
    public void validateTracks() {
        for (int i = 0; i < activeList.size(); i++) {
            LinkedList activeTrack = (LinkedList) activeList.get(i);
            if (activeTrack.size() < minTrackLength) {
                activeList.remove(i);
                i--;
            }
        }

    }

    public void drawTracks() {
        if (activeList == null) {
            return;
        }
        for (int i = 0; i < activeList.size(); i++) {
            LinkedList activeTrack = activeList.get(i);
            double[]lastPoint=(double[])activeTrack.get(activeTrack.size()-1);
            if(lastPoint[3]==0){
                continue;
            }
            for (int j = 0; j < activeTrack.size(); j++) {
                double[] point = (double[]) activeTrack.get(j);
                double X = point[0];
                double Y = point[1];
                showPlus.getProcessor().drawDot((int) X, (int) Y);
            }
        }
    }

    public void updateActiveTracks(ResultsTable rt) {
        for (int i = 0; i < activeList.size(); i++) {
            LinkedList activeTrack = (LinkedList) activeList.get(i);
            double[] lastPoint = (double[]) activeTrack.get(activeTrack.size() - 1); //get the last point
            double lastX = lastPoint[0];
            double lastY = lastPoint[1];
            double lastSize = lastPoint[2];
            double active=lastPoint[3];
            if(active==0){continue;}

            double minShift = 100000;
            int minRow = -1;

            for (int row = 0; row < rt.getCounter(); row++) {  //examine all of the tracks
                double X = rt.getValue("X", row);
                double Y = rt.getValue("Y", row);
                double shift = Math.sqrt((lastX - X) * (lastX - X) + (lastY - Y) * (lastY - Y));
                if (shift < minShift) {
                    minShift = shift;
                    minRow = row;
                }
            }

            if (minShift < maxShift && Math.abs(rt.getValue("Area", minRow) - lastSize) < maxSizeChange) { //updatable           
                    double[] newPoint = new double[4];
                    newPoint[0] = rt.getValue("X", minRow);
                    newPoint[1] = rt.getValue("Y", minRow);
                    newPoint[2] = rt.getValue("Area", minRow);
                    newPoint[3]= 1;
                    activeTrack.add(newPoint);
                    activeList.set(i, activeTrack);
                    rt.deleteRow(minRow);       
            } else if (activeTrack.size() < minTrackLength) { //delete dead tracks that are no longer active & too short
                activeList.remove(i);
                i--;
            }
            else{ //mark dead tracks as dead
                lastPoint[3]=0;
                activeTrack.set(activeTrack.size()-1,lastPoint);
                activeList.set(i, activeTrack);
            }
        }//for: activeList

        if (rt.getCounter() > 0) { //create new tracks for remaining rows in result
            for (int k = 0; k < rt.getCounter(); k++) {
                double[] newPoint = new double[4];
                newPoint[0] = rt.getValue("X", k);
                newPoint[1] = rt.getValue("Y", k);
                newPoint[2] = rt.getValue("Area", k);
                newPoint[3]=1;
                LinkedList newTrack = new LinkedList();
                newTrack.add(newPoint);
                activeList.add(newTrack);
            }
        }

    }

//    @Override
  //  public void paint(Graphics g){
    //    this.getGraphics().drawImage(showPlus.getImage(), 0, 0, this);
    //}
    
    @Override
    public void run() {
        System.out.println(Thread.currentThread());
        System.out.println(this.runner);//runner=currenthread when threadpool is not used; otherwise runner is not initialized at all
        
        //while (runner!=null & Thread.currentThread() == this.runner) 
        while(true)
        {   
            int state = trackFrame(currentFrame);
            if (state == -1) {
                break;
            }
            currentFrame++;
        }
        if(currentFrame!=0){
        this.validateTracks();
        this.writeTracks();
        System.out.println("total frames"+currentFrame);
        System.out.println("valid frames"+validFrame);}
        else{
            System.out.println("(0 Frame)Nothing is tracked.");
        }
    }
}
