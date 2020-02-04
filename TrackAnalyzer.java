/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javapwt;

import ij.gui.Plot;
import ij.measure.ResultsTable;
import java.awt.Color;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math.stat.descriptive.rank.Percentile;

/**
 *
 * @author wawnx
 */
public class TrackAnalyzer {

    private double frameRate = 7.50;
    private double calibFactor = 47;
    private double smoothingWindow = 5;
    private double calcStep = 5;
    private double binSpacing = 0.01;
    private double maxBin = 0.5;
    double speedThresh = 0.015;
    private double duration=30.0;
    private int nFrame=(int)(frameRate*duration);
    private String filename;
    private LinkedList list = null;
    private double[] allSpeedPoints = null;
    private int numPoints = 0;
    private ResultsTable rt = null;

    public TrackAnalyzer(String name) {
        filename = name;
    }

    public TrackAnalyzer(String name, AnalysisSetting setting) {
        filename = name;
        this.frameRate = setting.getFrameRate();
        this.calibFactor = setting.getCalibFactor();
        this.duration=setting.getDuration();
        this.smoothingWindow = setting.getSmoothingWindow();
        this.calcStep = setting.getCalcStep();
        this.binSpacing = setting.getBinSpacing();
        this.maxBin = setting.getMaxBin();
    }

    public void analyzeMultipleTracks(String[] names) {
        rt = new ResultsTable();
                
        rt.setHeading(0, "n");
        rt.setHeading(1, "Avg Speed");
        rt.setHeading(2, "Avg Speed(cutoff)");
        rt.setHeading(3, "25%");
        rt.setHeading(4, "50%");
        rt.setHeading(5, "75%");
        rt.setHeading(6, "95%");
        rt.setHeading(7, "99%");
        rt.setPrecision(5);
        double[]pooledHist=new double[(int) (maxBin / binSpacing)];
        
        for (int i = 0; i < names.length; i++) {
            double[] speed = analyzeTrack(names[i]);
            double[] a = calcHistogram(speed);
            for(int k=0;k<(int) (maxBin / binSpacing);k++){
                pooledHist[k]+=a[k];
            }
            double avg = calcAvgSpeed(a);
            double avg_cut = calcAvgSpeed(speed, speedThresh);
            Percentile p = new Percentile();
            p.setData(speed);

            rt.incrementCounter();
            rt.setLabel(names[i], i);
            rt.addValue(0, speed.length);
            rt.addValue(1, avg);
            rt.addValue(2, avg_cut);
            rt.addValue(3, p.evaluate(25));
            rt.addValue(4, p.evaluate(50));
            rt.addValue(5, p.evaluate(75));
            rt.addValue(6, p.evaluate(95));
            rt.addValue(7, p.evaluate(99));
            
        
                       
        }

        
         for(int k=0;k<(int) (maxBin / binSpacing);k++){
                pooledHist[k]/=names.length;
            }
        
        int columnCounter = rt.getLastColumn();
        rt.incrementCounter();
        rt.incrementCounter();
        int counter = rt.getCounter();
        rt.setLabel("Mean", counter - 2);
        rt.setLabel("CV", counter - 1);
        for (int i = 1; i <= columnCounter; i++) {
            float[] c = rt.getColumn(i);
            float mean = 0;
            for (int j = 0; j < c.length - 2; j++) {
                mean += c[j];
            }
            mean /= c.length - 2;
            double sum = 0;
            for (int j = 0; j < c.length - 2; j++) {
                sum += (c[j] - mean) * (c[j] - mean);
            }
            sum /= c.length - 3;
            sum = Math.sqrt(sum);

            rt.setValue(i, counter - 2, mean);
            rt.setValue(i, counter - 1, sum / mean);
        }


        rt.show("title");
        drawHistogram(pooledHist, "average");
        double[] cdf = calcCDF(pooledHist);
        drawCDF(cdf, "average");

    }

    public double[] analyzeTrack(String name) {
        list = readTracks(name);
        if (list == null) {
            System.out.println("cannot read track:" + name);
            return null;
        }
        //reset and calc the total number of points
        numPoints = 0;
        for (int i = 0; i < list.size(); i++) {
            numPoints += ((LinkedList) list.get(i)).size();
        }
        allSpeedPoints = new double[numPoints];
        int copyStart = 0;
        for (int i = 0; i < list.size(); i++) {
            LinkedList track = (LinkedList) list.get(i);
            //ArrayList speed = new ArrayList(track.size());
            double[] speed = new double[track.size()];
            double[] X = getX(track);
            double[] Y = getY(track);
            speed = calcSpeed(X, Y);
            System.arraycopy(speed, 0, allSpeedPoints, copyStart, speed.length);
            //allSpeedPoints.addAll(speed);
            copyStart += speed.length;
        }
        double[] a = calcHistogram(allSpeedPoints);
        double[] cdf = calcCDF(a);
        double avg = calcAvgSpeed(a);
        //drawCDF(cdf, name);
        //drawHistogram(a, name);

        return allSpeedPoints;
    }

    public void drawCDF(double[] cdf, String name) {
        double[] bins = new double[(int) (maxBin / binSpacing) + 1];
        bins[0] = 0;
        for (int i = 0; i < maxBin / binSpacing; i++) {
            bins[i + 1] = binSpacing * i + binSpacing / 2;
        }
        Plot myplot = new Plot("CDF plot " + name, "speed", "cumulative probability", bins, cdf);
        myplot.setSize(500, 300);
        myplot.setLimits(0, maxBin, 0, 1.1);
        myplot.setColor(Color.red);
        myplot.show();
    }

    public void drawHistogram(double[] hist, String name) {
        double[] bins = new double[(int) (maxBin / binSpacing)];
        double max = 0;
        for (int i = 0; i < maxBin / binSpacing; i++) {
            bins[i] = binSpacing * i + binSpacing / 2;
            if (hist[i] >= max) {
                max = hist[i];
            }
        }
        //Plot myplot = new Plot("histogram " + name, "speed", "percantage", bins, hist);
        Plot myplot = new Plot("histogram " + name, "speed", "percantage", new double[0], new double[0]);
        myplot.setSize(300, 300);

        myplot.setLimits(-0.01, maxBin, 0, Math.ceil(max / 0.02) * 0.02);
        myplot.setColor(Color.red);
        myplot.addPoints(bins, hist, Plot.LINE);
        //myplot.setLineWidth((int)(300*0.7/(maxBin/binSpacing)));
        for (int i = 0; i < maxBin / binSpacing; i++) {
            if (hist[i] > 0) {
                myplot.drawLine(binSpacing * i, 0, binSpacing * i, hist[i]);
            }
        }
        myplot.show();
    }

    public double calcAvgSpeed(double[] hist) {
        double avgSpeed = 0;
        for (int i = 0; i < hist.length; i++) {
            avgSpeed += (binSpacing * i + binSpacing / 2) * hist[i];
        }

        System.out.println("avgSpeed:" + avgSpeed);
        System.out.println("n:" + allSpeedPoints.length);

        Percentile test = new Percentile(50);

        return avgSpeed;
    }

    public double calcAvgSpeed(double[] speed, double cutoff) {
        double avg = 0;
        int num = 0;
        for (int i = 0; i < speed.length; i++) {
            if (speed[i] >= cutoff) {
                avg += speed[i];
                num++;
            }
        }
        avg /= num;
        return avg;
    }

    public double[] calcHistogram(double[] l) {
        if (l == null || l.length == 0) {
            return null;
        }
        int nBins = (int) (maxBin / binSpacing);
        double[] hist = new double[nBins];
        for (int i = 0; i < l.length; i++) {
            double val = l[i];
            // System.out.println(val);
            if ((int) (Math.floor(val / binSpacing)) >= nBins) {
                hist[nBins - 1]++;
            } else {
                hist[(int) (Math.floor(val / binSpacing))]++;
            }
        }
        //divide by sum
        for (int i = 0; i < nBins; i++) {
            hist[i] /= l.length;
        }
        return hist;
    }

    public double[] calcCDF(double[] l) {
        if (l == null || l.length == 0) {
            return null;
        }
        double[] cdf = new double[l.length + 1];
        double sum = 0;
        for (int i = 0; i < l.length; i++) {
            sum += l[i];
            cdf[i + 1] = sum;
        }
        return cdf;
    }

    public double[] calcSpeed(double[] X, double[] Y) {
        double actualFrameRate=nFrame/duration;
        //ArrayList speed = null;
        double[] speed = null;
        if (X == null || Y == null || X.length != Y.length) {
            return speed;
        }

        int n = X.length;

        speed = new double[n];

        double[] smoothX = smooth(X, (int) (Math.floor(smoothingWindow / 2)));
        double[] smoothY = smooth(Y, (int) (Math.floor(smoothingWindow / 2)));
        double[] difX = dif(smoothX, (int) (Math.floor(calcStep / 2)));
        double[] difY = dif(smoothY, (int) (Math.floor(calcStep / 2)));

        for (int i = 0; i < n; i++) {
            speed[i] = (Math.sqrt(difX[i] * difX[i] + difY[i] * difY[i])) * actualFrameRate / calibFactor;
        }

        return speed;
    }

    public double[] getX(LinkedList list) {
        if (list != null) {
            double[] X = new double[list.size()];

            for (int i = 0; i < list.size(); i++) {
                double[] unit = (double[]) list.get(i);
                X[i] = unit[0];
            }
            return X;
        }
        return null;
    }

    public double[] getY(LinkedList list) {
        if (list != null) {
            double[] Y = new double[list.size()];

            for (int i = 0; i < list.size(); i++) {
                double[] unit = (double[]) list.get(i);
                Y[i] = unit[1];
            }
            return Y;
        }
        return null;
    }

    private double[] dif(double[] X, int halfstep) {
        double[] S = null;
        if (X == null) {
            return S;
        }
        int n = X.length;

        S = new double[n];
        for (int i = halfstep; i < n - halfstep; i++) {
            S[i] = (X[i + halfstep] - X[i - halfstep]) / (2 * (double) halfstep);
        }

        for (int i = 1; i < halfstep; i++) {
            int width = i;
            S[i] = (X[i + width] - X[i - width]) / (2 * (double) width);
        }

        for (int i = n - halfstep; i < n - 1; i++) {
            int width = n - 1 - i;
            S[i] = (X[i + width] - X[i - width]) / (2 * (double) width);
        }

        S[0] = X[1] - X[0];
        S[n - 1] = X[n - 1] - X[n - 2];
        return S;
    }

    private double[] smooth(double[] D, int halfwidth) {
        double[] S = null;
        if (D == null) {
            return S;
        }
        int n = D.length;

        S = new double[n];
        for (int i = halfwidth; i < n - halfwidth; i++) {
            double sum = 0;
            for (int j = i - halfwidth; j <= i + halfwidth; j++) {
                sum += D[j];
            }
            sum /= (halfwidth * 2 + 1);
            S[i] = sum;
        }

        for (int i = 0; i < halfwidth; i++) {
            int width = i;
            double sum = 0;
            for (int j = i - width; j <= i + width; j++) {
                sum += D[j];
            }
            sum /= (width * 2 + 1);
            S[i] = sum;
        }

        for (int i = n - halfwidth; i < n; i++) {
            int width = n - 1 - i;
            double sum = 0;
            for (int j = i - width; j <= i + width; j++) {
                sum += D[j];
            }
            sum /= (width * 2 + 1);
            S[i] = sum;

        }

        return S;
    }

    public LinkedList readTracks(String name) {
        list = null;
        FileInputStream fs = null;
        ObjectInputStream oi = null;
        try {
            if (!name.endsWith(".file")) { //append suffix
                name = name + ".file";
            }
            System.out.println(name);
            fs = new FileInputStream(name);
            oi = new ObjectInputStream(fs);
            Object one = oi.readObject();
            nFrame=oi.readInt();
            System.out.println("read valid frame:"+nFrame);
            list = (LinkedList) one;
            System.out.println("read:" + list.size());
            //System.out.println("read:" + ((LinkedList) list.get(0)).size());
            oi.close();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Tracker.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Tracker.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fs.close();
                oi.close();
            } catch (IOException ex) {
                Logger.getLogger(Tracker.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return list;
    }
}
