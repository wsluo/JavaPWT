/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javapwt;

import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.NewImage;
import ij.io.FileSaver;
import ij.process.Blitter;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.apache.commons.math.stat.descriptive.rank.Percentile;

/**
 *
 * @author wawnx
 */
public class JavaPWT extends JFrame {

    public static String BATCH_TRACKING_TASK = "BATCH_TRACKING";
    public static String BATCH_SNAPSHOTTING_TASK = "BATCH_SNAPSHATTING";
    public static String BATCH_ASSEMBLING_TASK = "BATCH_ASSEMBLING";
    public static MyThreadPoolExecutor mtpe;
    public static int snapshotted = 0;
    public static int completedCount = 0;
    private boolean previewModeTrackingBatch = false;
    private static String currentTask = null;
    private File currentPreviewedVideo = null;
    private File currentPreviewedImage = null;
    private LinkedList<PreviewStatus> fileList = null;
    private motionDetector detector = null;
//Dialogs
    private JDialog trackingPrefDialog;
    private JDialog batchTrackingPreviewDialog;
    private JDialog analysisPrefDialog;
    private JDialog motionDetectionPrefDialog;
    private JDialog batchCountingPreviewDialog;
//Menu
    private JMenuBar menuBar;
//Menu: tracking menu
    private JMenu trackingMenu;
    private JMenuItem trackingItem;
    private JMenuItem trackingPrefItem;
    private JMenuItem batchTrackingPreviewItem;
    private JMenuItem batchTrackingItem;
//Menu: analysis menu
    private JMenu analysisMenu;
    private JMenuItem analysisItem;
    private JMenuItem analysisPrefItem;
    private JMenuItem compareItem;
    private JMenuItem analyzeEveryFolderItem;
    //Menu: motion detection menu
    private JMenu motionDetectionMenu;
    private JMenuItem motionDetectionItem;
    private JMenuItem motionDetectionPrefItem;
    private JMenuItem batchCountingPreviewItem;
    private JMenuItem motionDetectionViewReportMenuItem;
    //Buttons
    private JButton videoPreviewButton;
    private JButton imagePreviewButton;
    private JButton saveTrackingSettingButton;
    private JButton saveBatchTrackingPreviewButton;
    private JButton saveAnalysisSettingButton;
    private JButton saveMotionDetectionSettingButton;
    private JButton saveBatchCountingPreviewButton;
    private JButton previousTrackingPreviewButton;
    private JButton nextTrackingPreviewButton;
    private JButton nextNonPreviewedTrackingPreviewButton;
    private JButton previousCountingPreviewButton;
    private JButton nextCountingPreviewButton;
    private JButton nextNonPreviewedCountingPreviewButton;
    //Text&Label&TextField
    private JTextField frameRateText;
    private JTextField calibFactorText;
    private JTextField durationText;
    private JTextField smoothingWindowText;
    private JTextField calcStepText;
    private JTextField binSpacingText;
    private JTextField maxBinText;
    private JTextField minSizeText;
    private JTextField maxSizeText;
    private JTextField maxShiftText;
    private JTextField maxSizeChangeText;
    private JTextField minTrackLengthText;
    private JTextField drawIntervalText;
    private JTextField levelText;
    private JTextField minAreaText;
    private JTextField maxAreaText;
    private JTextField threshText;
    private JTextField difThreshText;
    private JTextField difSizeText;
    private JTextField nPieceXText;
    private JTextField nPieceYText;
    private JLabel batchTrackingPreviewImageLabel;
    private JLabel batchCountingPreviewImageLabel;
    private JLabel batchTrackingPreviewInfoLabel;
    private JLabel batchCountingPreviewInfoLabel;
    private JLabel currentTrackingPreviewInfoLabel;
    private JLabel currentCountingPreviewInfoLabel;
    private JLabel frameRateLabel;
    private JLabel calibFactorLabel;
    private JLabel durationLabel;
    private JLabel smoothingWindowLabel;
    private JLabel calcStepLabel;
    private JLabel binSpacingLabel;
    private JLabel maxBinLabel;
    private JLabel minSizeLabel;
    private JLabel maxSizeLabel;
    private JLabel maxShiftLabel;
    private JLabel maxSizeChangeLabel;
    private JLabel minTrackLengthLabel;
    private JLabel drawIntervalLabel;
    private JLabel levelLabel;
    private JLabel minAreaLabel;
    private JLabel maxAreaLabel;
    private JLabel threshLabel;
    private JLabel difThreshLabel;
    private JLabel difSizeLabel;
    private JLabel nPieceXLabel;
    private JLabel nPieceYLabel;
    private JPanel changeTrackingPreviewPanel;
    private JPanel changeCountingPreviewPanel;
    private JPanel sizeSettingPanel;
    private JPanel trackingSettingPanel;
    private JPanel segmentationSettingPanel;
    private JPanel trackingButtonPanel;
    private JPanel calibPanel;
    private JPanel stepPanel;
    private JPanel histPanel;
    private JPanel analysisButtonPanel;
    private JPanel assembleSettingPanel;
    private JPanel difCountSettingPanel;
    private JPanel motionDetectionThresholdPanel;
    private JPanel analysisPrefPanel;
    private JPanel trackingPrefPanel;
    private JPanel batchTrackingPreviewPanel;
    private JPanel trackingPreviewImageDisplayPanel;
    private JPanel countingPreviewImageDisplayPanel;
    private JPanel motionDetectionPrefPanel;
    private JPanel motionDetectionButtonPanel;
    private JPanel batchCountingPreviewPanel;
    private JSlider levelSlider;
    private JSlider threshSlider;
    private String directory;
    private ImageWindow previewWindow;
    private ImagePlus samplePlus;
    private ImagePlus previewPlus;

    //constructor
    public JavaPWT() {
        super("Parallel Tracker");
        initComponents();
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                System.exit(0);
            }
        });
    }

    private void initComponents() {
        try {

            //construct dialogs            
            trackingPrefDialog = new JDialog(this, "Tracking Prefs");
            analysisPrefDialog = new JDialog(this, "Analysis Prefs");
            motionDetectionPrefDialog = new JDialog(this, "Prefs");
            batchTrackingPreviewDialog = new JDialog(this, "Batch Tracking Preview");
            batchCountingPreviewDialog = new JDialog(this, "Batch Counting Preview");

            //construct panels
            analysisPrefPanel = new JPanel(new GridBagLayout());
            trackingPrefPanel = new JPanel(new GridBagLayout());
            motionDetectionPrefPanel = new JPanel(new GridBagLayout());

            //batch tracking preview panels
            batchTrackingPreviewPanel = new JPanel(new GridBagLayout());
            trackingPreviewImageDisplayPanel = new JPanel(new GridBagLayout());
            changeTrackingPreviewPanel = new JPanel(new GridBagLayout());

            //batch counting preview panels
            batchCountingPreviewPanel = new JPanel(new GridBagLayout());
            countingPreviewImageDisplayPanel = new JPanel(new GridBagLayout());
            changeCountingPreviewPanel = new JPanel(new GridBagLayout());

            //tracking setting panels
            sizeSettingPanel = new JPanel(new GridBagLayout());
            trackingSettingPanel = new JPanel(new GridBagLayout());
            segmentationSettingPanel = new JPanel(new GridBagLayout());
            trackingButtonPanel = new JPanel(new GridBagLayout());

            //analysis setting panels
            calibPanel = new JPanel(new GridBagLayout());
            stepPanel = new JPanel(new GridBagLayout());
            histPanel = new JPanel(new GridBagLayout());
            analysisButtonPanel = new JPanel(new GridBagLayout());

            //motion detection setting panel
            assembleSettingPanel = new JPanel(new GridBagLayout());
            motionDetectionThresholdPanel = new JPanel(new GridBagLayout());
            motionDetectionButtonPanel = new JPanel(new GridBagLayout());
            difCountSettingPanel = new JPanel(new GridBagLayout());


            //analysis text
            frameRateText = new JTextField(5);
            calibFactorText = new JTextField(5);
            durationText = new JTextField(5);
            smoothingWindowText = new JTextField(5);
            calcStepText = new JTextField(5);
            binSpacingText = new JTextField(5);
            maxBinText = new JTextField(5);

            //tracking text
            minSizeText = new JTextField(5);
            maxSizeText = new JTextField(5);
            maxShiftText = new JTextField(5);
            maxSizeChangeText = new JTextField(5);
            minTrackLengthText = new JTextField(5);
            drawIntervalText = new JTextField(5);
            levelText = new JTextField(5);

            //motionDetection text
            minAreaText = new JTextField(5);
            maxAreaText = new JTextField(5);
            threshText = new JTextField(5);
            nPieceXText = new JTextField(5);
            nPieceYText = new JTextField(5);
            difThreshText = new JTextField(5);
            difSizeText = new JTextField(5);


            levelSlider = new JSlider(JSlider.HORIZONTAL, 10, 90, 55);
            threshSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 20);
            levelSlider.addChangeListener(new ChangeListener() {

                public void stateChanged(ChangeEvent ce) {
                    levelSliderStateChanged(ce);
                }
            });
            levelSlider.setMajorTickSpacing(10);
            levelSlider.setMinorTickSpacing(2);
            levelSlider.setPaintTicks(true);
            levelSlider.setPaintLabels(true);


            threshSlider.addChangeListener(new ChangeListener() {

                public void stateChanged(ChangeEvent ce) {
                    threshSliderStateChanged(ce);
                }
            });
            threshSlider.setMajorTickSpacing(10);
            threshSlider.setMinorTickSpacing(2);
            threshSlider.setPaintTicks(true);
            threshSlider.setPaintLabels(true);

            /*
             * Hashtable labelTable = new Hashtable(); for (int i = 0; i < 101;
             * i += 10) { labelTable.put(new Integer(i), new
             * JLabel(String.valueOf(i / 100.0))); }
             *
             * threshSlider.setLabelTable(labelTable);
             */

            //tracking labels
            minSizeLabel = new JLabel("Min Area");
            maxSizeLabel = new JLabel("Max Area");
            maxShiftLabel = new JLabel("Max Inter-frame Pixel Shift");
            maxSizeChangeLabel = new JLabel("Max Inter-frame Size Change");
            minTrackLengthLabel = new JLabel("Min Track Length");
            drawIntervalLabel = new JLabel("Drawing Interval(frame)");
            levelLabel = new JLabel("Thresholding Level");

            //batch tracking preview labels
            batchTrackingPreviewImageLabel = new JLabel();
            batchTrackingPreviewImageLabel.setMinimumSize(new Dimension(640, 480)); //for image display
            batchTrackingPreviewInfoLabel = new JLabel();
            currentTrackingPreviewInfoLabel = new JLabel();
            currentTrackingPreviewInfoLabel.setSize(new Dimension(640, 60));

            //batch counting preview labels
            batchCountingPreviewImageLabel = new JLabel();
            batchCountingPreviewImageLabel.setMinimumSize(new Dimension(640, 640)); //for image display
            batchCountingPreviewInfoLabel = new JLabel();
            currentCountingPreviewInfoLabel = new JLabel();
            currentCountingPreviewInfoLabel.setSize(new Dimension(640, 60));

            //analysis labels
            frameRateLabel = new JLabel("Frame Rate");
            calibFactorLabel = new JLabel("Calib Factor");
            durationLabel = new JLabel("Duration");
            smoothingWindowLabel = new JLabel("Smoothing Window Size");
            calcStepLabel = new JLabel("Speed Calculation Step");
            binSpacingLabel = new JLabel("Bin Spacing");
            maxBinLabel = new JLabel("Max Bin");
            //motion detection labels

            minAreaLabel = new JLabel("Min Area");
            maxAreaLabel = new JLabel("Max Area");
            threshLabel = new JLabel("Thresholding Level");
            nPieceXLabel = new JLabel("number of pieces(X)");
            nPieceYLabel = new JLabel("number of pieces(Y)");
            difThreshLabel = new JLabel("Thresholding Level");
            difSizeLabel = new JLabel("Min Size of Diff Particle");

            //buttons
            videoPreviewButton = new JButton("Preview");
            imagePreviewButton = new JButton("Preview");
            saveTrackingSettingButton = new JButton("Save");
            saveBatchTrackingPreviewButton = new JButton("Save");
            saveAnalysisSettingButton = new JButton("Save");
            saveMotionDetectionSettingButton = new JButton("Save");
            saveBatchCountingPreviewButton = new JButton("Save");


            nextNonPreviewedTrackingPreviewButton = new JButton("Next un-previewed");
            previousTrackingPreviewButton = new JButton("Previous");
            nextTrackingPreviewButton = new JButton("Next");

            nextNonPreviewedCountingPreviewButton = new JButton("Next un-previewed");
            previousCountingPreviewButton = new JButton("Previous");
            nextCountingPreviewButton = new JButton("Next");


            //set trackingSettingPanel          
            sizeSettingPanel.add(minSizeLabel, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            sizeSettingPanel.add(minSizeText, new GBC(1, 0).setInsets(5, 50, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.EAST));
            sizeSettingPanel.add(maxSizeLabel, new GBC(0, 1).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            sizeSettingPanel.add(maxSizeText, new GBC(1, 1).setFill(GBC.HORIZONTAL).setInsets(5, 50, 5, 5).setAnchor(GBC.EAST));
            sizeSettingPanel.add(maxShiftLabel, new GBC(0, 2).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            sizeSettingPanel.add(maxShiftText, new GBC(1, 2).setInsets(5, 50, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.EAST));
            sizeSettingPanel.add(maxSizeChangeLabel, new GBC(0, 3).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            sizeSettingPanel.add(maxSizeChangeText, new GBC(1, 3).setInsets(5, 50, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.EAST));
            sizeSettingPanel.setBorder(BorderFactory.createTitledBorder("Size Setting"));


            trackingSettingPanel.add(minTrackLengthLabel, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            trackingSettingPanel.add(minTrackLengthText, new GBC(1, 0).setInsets(5, 50, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.EAST));
            trackingSettingPanel.add(drawIntervalLabel, new GBC(0, 1).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            trackingSettingPanel.add(drawIntervalText, new GBC(1, 1).setInsets(5, 50, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.EAST));

            trackingSettingPanel.setBorder(BorderFactory.createTitledBorder("Tracking Setting"));


            segmentationSettingPanel.add(levelLabel, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            segmentationSettingPanel.add(levelSlider, new GBC(1, 0).setInsets(5, 50, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.EAST));

            segmentationSettingPanel.setBorder(BorderFactory.createTitledBorder("Segmentation Setting"));


            trackingButtonPanel.add(videoPreviewButton, new GBC(0, 0).setInsets(5, 5, 5, 5));
            trackingButtonPanel.add(saveTrackingSettingButton, new GBC(1, 0).setInsets(5, 5, 5, 5));
            //trackingButtonPanel.add(saveBatchTrackingPreviewButton, new GBC(0, 1).setInsets(5, 5, 5, 5));


            trackingPrefPanel.add(sizeSettingPanel, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            trackingPrefPanel.add(trackingSettingPanel, new GBC(0, 1).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            trackingPrefPanel.add(segmentationSettingPanel, new GBC(0, 2).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            trackingPrefPanel.add(trackingButtonPanel, new GBC(0, 3).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));




            saveTrackingSettingButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    saveTrackingSettingActionPerformed(evt);
                }
            });

            videoPreviewButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    videoPreviewActionPerformed(evt);
                }
            });

            saveBatchTrackingPreviewButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    saveBatchTrackingPreviewActionPerformed(evt);
                }
            });


            previousTrackingPreviewButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    changeTrackingPreviewActionPerformed(evt);
                }
            });

            nextTrackingPreviewButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    changeTrackingPreviewActionPerformed(evt);
                }
            });

            nextNonPreviewedTrackingPreviewButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    changeTrackingPreviewActionPerformed(evt);
                }
            });


            loadTrackingSetting("");
            //trackingPrefDialog.add(trackingPrefPanel); do it when loading it because the panel is shared between two dialogs...

            //batch tracking preview panel
            changeTrackingPreviewPanel.add(saveBatchTrackingPreviewButton, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            changeTrackingPreviewPanel.add(previousTrackingPreviewButton, new GBC(1, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            changeTrackingPreviewPanel.add(nextTrackingPreviewButton, new GBC(2, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            changeTrackingPreviewPanel.add(nextNonPreviewedTrackingPreviewButton, new GBC(3, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));

            trackingPreviewImageDisplayPanel.add(batchTrackingPreviewImageLabel, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            trackingPreviewImageDisplayPanel.add(changeTrackingPreviewPanel, new GBC(0, 1).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            trackingPreviewImageDisplayPanel.add(batchTrackingPreviewInfoLabel, new GBC(0, 2).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            trackingPreviewImageDisplayPanel.add(currentTrackingPreviewInfoLabel, new GBC(0, 3).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));


            //batch counting preview panel
            changeCountingPreviewPanel.add(saveBatchCountingPreviewButton, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            changeCountingPreviewPanel.add(previousCountingPreviewButton, new GBC(1, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            changeCountingPreviewPanel.add(nextCountingPreviewButton, new GBC(2, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            changeCountingPreviewPanel.add(nextNonPreviewedCountingPreviewButton, new GBC(3, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));

            countingPreviewImageDisplayPanel.add(batchCountingPreviewImageLabel, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            countingPreviewImageDisplayPanel.add(changeCountingPreviewPanel, new GBC(0, 1).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            countingPreviewImageDisplayPanel.add(batchCountingPreviewInfoLabel, new GBC(0, 2).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            countingPreviewImageDisplayPanel.add(currentCountingPreviewInfoLabel, new GBC(0, 3).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));

            //analysis panel
            calibPanel.add(frameRateLabel, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            calibPanel.add(frameRateText, new GBC(1, 0).setInsets(5, 50, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.EAST));
            calibPanel.add(calibFactorLabel, new GBC(0, 1).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            calibPanel.add(calibFactorText, new GBC(1, 1).setInsets(5, 50, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.EAST));
            calibPanel.add(durationLabel, new GBC(0, 2).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            calibPanel.add(durationText, new GBC(1, 2).setInsets(5, 50, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.EAST));

            calibPanel.setBorder(BorderFactory.createTitledBorder("Calibration Setting"));

            stepPanel.add(smoothingWindowLabel, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            stepPanel.add(smoothingWindowText, new GBC(1, 0).setInsets(5, 50, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.EAST));
            stepPanel.add(calcStepLabel, new GBC(0, 1).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            stepPanel.add(calcStepText, new GBC(1, 1).setInsets(5, 50, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.EAST));

            stepPanel.setBorder(BorderFactory.createTitledBorder("Step"));

            histPanel.add(binSpacingLabel, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            histPanel.add(binSpacingText, new GBC(1, 0).setInsets(5, 50, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.EAST));
            histPanel.add(maxBinLabel, new GBC(0, 1).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            histPanel.add(maxBinText, new GBC(1, 1).setInsets(5, 50, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.EAST));

            histPanel.setBorder(BorderFactory.createTitledBorder("Binning Setting"));

            analysisButtonPanel.add(saveAnalysisSettingButton, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));

            analysisPrefPanel.add(calibPanel, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            analysisPrefPanel.add(stepPanel, new GBC(0, 1).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            analysisPrefPanel.add(histPanel, new GBC(0, 2).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            analysisPrefPanel.add(analysisButtonPanel, new GBC(0, 3).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));

            saveAnalysisSettingButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    saveAnalysisSettingActionPerformed(evt);
                }
            });


            loadAnalysisSetting("");
            analysisPrefDialog.add(analysisPrefPanel);


            //motion detection panel
            assembleSettingPanel.add(nPieceXLabel, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            assembleSettingPanel.add(nPieceXText, new GBC(1, 0).setInsets(5, 50, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.EAST));
            assembleSettingPanel.add(nPieceYLabel, new GBC(0, 1).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            assembleSettingPanel.add(nPieceYText, new GBC(1, 1).setInsets(5, 50, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.EAST));

            assembleSettingPanel.setBorder(BorderFactory.createTitledBorder("Assemble Setting"));

            motionDetectionThresholdPanel.add(minAreaLabel, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            motionDetectionThresholdPanel.add(minAreaText, new GBC(1, 0).setInsets(5, 50, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.EAST));
            motionDetectionThresholdPanel.add(maxAreaLabel, new GBC(0, 1).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            motionDetectionThresholdPanel.add(maxAreaText, new GBC(1, 1).setInsets(5, 50, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.EAST));
            motionDetectionThresholdPanel.add(threshLabel, new GBC(0, 2).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            motionDetectionThresholdPanel.add(threshSlider, new GBC(1, 2).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.EAST));
            //motionDetectionThresholdPanel.add(threshText, new GBC(1, 2).setInsets(5, 50, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.EAST));

            motionDetectionThresholdPanel.setBorder(BorderFactory.createTitledBorder("Thresholds"));


            difCountSettingPanel.add(difThreshLabel, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            difCountSettingPanel.add(difThreshText, new GBC(1, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.EAST));
            difCountSettingPanel.add(difSizeLabel, new GBC(0, 1).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            difCountSettingPanel.add(difSizeText, new GBC(1, 1).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.EAST));
            difCountSettingPanel.setBorder(BorderFactory.createTitledBorder("Thresholds for auto count"));

            motionDetectionButtonPanel.add(saveMotionDetectionSettingButton, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
            motionDetectionButtonPanel.add(imagePreviewButton, new GBC(1, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));

            motionDetectionPrefPanel.add(assembleSettingPanel, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            motionDetectionPrefPanel.add(difCountSettingPanel, new GBC(0, 1).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            motionDetectionPrefPanel.add(motionDetectionThresholdPanel, new GBC(0, 2).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));

            motionDetectionPrefPanel.add(motionDetectionButtonPanel, new GBC(0, 3).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));

            loadMotionDetectionSetting("");
            //motionDetectionPrefDialog.add(motionDetectionPrefPanel);

            saveMotionDetectionSettingButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    saveMotionDetectionSettingActionPerformed(evt);
                }
            });

            imagePreviewButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    imagePreviewActionPerformed(evt);
                }
            });

            saveBatchCountingPreviewButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    saveBatchCountingPreviewActionPerformed(evt);
                }
            });

            previousCountingPreviewButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    changeCountingPreviewActionPerformed(evt);
                }
            });

            nextCountingPreviewButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    changeCountingPreviewActionPerformed(evt);
                }
            });
            nextNonPreviewedCountingPreviewButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    changeCountingPreviewActionPerformed(evt);
                }
            });


            //-----------------------------------------------------------------------------------------------//
            //menus
            menuBar = new JMenuBar();
            //tracking menu
            trackingMenu = new JMenu("Tracking");
            trackingItem = new JMenuItem("Open files");
            trackingPrefItem = new JMenuItem("Tracking Prefs");
            batchTrackingItem = new JMenuItem("BFS Tracking");
            batchTrackingPreviewItem = new JMenuItem("Preview Batch");
            trackingMenu.add(trackingItem);
            trackingMenu.add(trackingPrefItem);
            trackingMenu.add(batchTrackingItem);
            trackingMenu.add(batchTrackingPreviewItem);
            menuBar.add(trackingMenu);


            trackingItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    trackingItemActionPerformed(evt);
                }
            });

            trackingPrefItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    trackingPrefItemActionPerformed(evt);
                }
            });

            batchTrackingItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    batchTrackingItemActionPerformed(evt);
                }
            });

            batchTrackingPreviewItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    batchTrackingPreviewItemActionPerformed(evt);
                }
            });

            //analysis menu
            analysisMenu = new JMenu("Track Analysis");
            analysisItem = new JMenuItem("Open track files");
            analysisPrefItem = new JMenuItem("Analysis Prefs");
            compareItem = new JMenuItem("Compare Multi");
            analyzeEveryFolderItem = new JMenuItem("BFS Analysis");
            analysisMenu.add(analysisItem);
            analysisMenu.add(analysisPrefItem);
            //analysisMenu.add(compareItem);
            analysisMenu.add(analyzeEveryFolderItem);
            menuBar.add(analysisMenu);


            analysisItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    analysisItemActionPerformed(evt);
                }
            });


            analysisPrefItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    analysisPrefItemActionPerformed(evt);
                }
            });

            analyzeEveryFolderItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    analyzeEveryFolderItemActionPerformed(evt);
                }
            });
            //motionDetection
            motionDetectionMenu = new JMenu("Motion Detection");
            motionDetectionItem = new JMenuItem("Open a well");
            motionDetectionPrefItem = new JMenuItem("Prefs");
            batchCountingPreviewItem = new JMenuItem("Batch Preview");
            motionDetectionViewReportMenuItem = new JMenuItem( "View Report" );

            motionDetectionMenu.add(motionDetectionItem);
            motionDetectionMenu.add(motionDetectionPrefItem);
            motionDetectionMenu.add(batchCountingPreviewItem);
            motionDetectionMenu.add(motionDetectionViewReportMenuItem);
            menuBar.add(motionDetectionMenu);


            motionDetectionItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    motionDetectionItemActionPerformed(evt);
                }
            });


            motionDetectionPrefItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    motionDetectionPrefItemActionPerformed(evt);
                }
            });

            batchCountingPreviewItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    batchCountingPreviewItemActionPerformed(evt);
                }
            });
            
            motionDetectionViewReportMenuItem.addActionListener( new ActionListener() {
            	public void actionPerformed( ActionEvent actionEvent ) {
            		motionDetectionViewReportActionPerformed( actionEvent );
            	}
            });

            //display frame
            this.setJMenuBar(menuBar);
            this.pack();
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
            this.setLocation(300, 300);
            this.setVisible(true);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(JavaPWT.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(JavaPWT.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(JavaPWT.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(JavaPWT.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private TrackingSetting getTrackingSetting() {
        TrackingSetting setting = new TrackingSetting();
        setting.setMinSize(Integer.parseInt(minSizeText.getText().trim()));
        setting.setMaxSize(Integer.parseInt(maxSizeText.getText().trim()));
        setting.setMaxShift(Double.parseDouble(maxShiftText.getText().trim()));
        setting.setMaxSizeChange(Double.parseDouble(maxSizeChangeText.getText().trim()));
        setting.setDrawInterval(Integer.parseInt(drawIntervalText.getText().trim()));
        setting.setLevel((double) levelSlider.getValue() / (double) 100);
        setting.setMinTrackLength(Integer.parseInt(minTrackLengthText.getText().trim()));
        return setting;
    }

    private void loadTrackingSetting(String folderpath) {
        String filepath = folderpath + File.separator + "tracking.properties";
        if (filepath.startsWith("\\")) {
            filepath = filepath.replace("\\", "");
        }
        File in = new File(filepath);
        if (!in.exists()) {
            System.out.println("cannot find setting file");
            return;
        }
        System.out.println("loading tracking setting from " + filepath);
        try {
            Properties property = new Properties();
            property.load(new FileInputStream(filepath));
            minSizeText.setText(property.getProperty("minSize"));
            maxSizeText.setText(property.getProperty("maxSize"));
            maxSizeChangeText.setText(property.getProperty("maxSizeChange"));
            maxShiftText.setText(property.getProperty("maxShift"));
            minTrackLengthText.setText(property.getProperty("minTrackLength"));
            drawIntervalText.setText(property.getProperty("drawInterval"));
            levelText.setText(property.getProperty("level"));
            levelSlider.setValue(Integer.parseInt(property.getProperty("level")));
        } catch (IOException ex) {
            Logger.getLogger(Tracker.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void saveTrackingSetting(String path) {
        try {
            Properties property = new Properties();
            property.setProperty("minSize", minSizeText.getText().trim());
            property.setProperty("maxSize", maxSizeText.getText().trim());
            property.setProperty("maxSizeChange", maxSizeChangeText.getText().trim());
            property.setProperty("maxShift", maxShiftText.getText().trim());
            property.setProperty("minTrackLength", minTrackLengthText.getText().trim());
            property.setProperty("drawInterval", drawIntervalText.getText().trim());
            property.setProperty("level", String.valueOf(levelSlider.getValue()));
            if (!path.isEmpty()) {
                property.store(new FileOutputStream(path + File.separator + "tracking.properties"), "tracking.properties");
            } else {
                property.store(new FileOutputStream("tracking.properties"), "tracking.properties");
            }
        } catch (IOException ex) {
            Logger.getLogger(Tracker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private AnalysisSetting getAnalysisSetting() {
        AnalysisSetting setting = new AnalysisSetting();
        setting.setFrameRate(Double.parseDouble(frameRateText.getText().trim()));
        setting.setCalibFactor(Double.parseDouble(calibFactorText.getText().trim()));
        setting.setDuration(Double.parseDouble(durationText.getText().trim()));
        setting.setSmoothingWindow(Double.parseDouble(smoothingWindowText.getText().trim()));
        setting.setCalcStep(Double.parseDouble(calcStepText.getText().trim()));
        setting.setBinSpacing(Double.parseDouble(binSpacingText.getText().trim()));
        setting.setMaxBin(Double.parseDouble(maxBinText.getText().trim()));
        return setting;
    }

    private void loadAnalysisSetting(String folderpath) {
        String filepath = folderpath + File.separator + "analysis.properties";
        if (filepath.startsWith("\\")) {
            filepath = filepath.replace("\\", "");
        }
        File in = new File(filepath);
        System.out.println(filepath);
        if (!in.exists()) {
            return;
        }
        try {
            Properties property = new Properties();
            property.load(new FileInputStream(filepath));

            frameRateText.setText(property.getProperty("frameRate"));
            calibFactorText.setText(property.getProperty("calibFactor"));
            durationText.setText(property.getProperty("duration"));
            smoothingWindowText.setText(property.getProperty("smoothingWindow"));
            calcStepText.setText(property.getProperty("calcStep"));
            binSpacingText.setText(property.getProperty("binSpacing"));
            maxBinText.setText(property.getProperty("maxBin"));
        } catch (IOException ex) {
            Logger.getLogger(Tracker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void saveAnalysisSetting(String path) {
        try {
            Properties property = new Properties();
            property.setProperty("frameRate", frameRateText.getText().trim());
            property.setProperty("calibFactor", calibFactorText.getText().trim());
            property.setProperty("duration", durationText.getText().trim());
            property.setProperty("smoothingWindow", smoothingWindowText.getText().trim());
            property.setProperty("calcStep", calcStepText.getText().trim());
            property.setProperty("binSpacing", binSpacingText.getText().trim());
            property.setProperty("maxBin", maxBinText.getText().trim());
            if (!path.isEmpty()) {
                property.store(new FileOutputStream(path + File.separator + "analysis.properties"), "analysis.properties");
            } else {
                property.store(new FileOutputStream("analysis.properties"), "analysis.properties");
            }
        } catch (IOException ex) {
            Logger.getLogger(Tracker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private MotionDetectionSetting getMotionDetectionSetting() {
        MotionDetectionSetting setting = new MotionDetectionSetting();
        setting.setMinArea(Integer.parseInt(minAreaText.getText().trim()));
        setting.setMaxArea(Integer.parseInt(maxAreaText.getText().trim()));
        setting.setPieceX(Integer.parseInt(nPieceXText.getText().trim()));
        setting.setPieceY(Integer.parseInt(nPieceYText.getText().trim()));
        //setting.setThresh(Double.parseDouble(threshText.getText().trim()));
        setting.setThresh(threshSlider.getValue());
        setting.setDifSize(Integer.parseInt(difSizeText.getText().trim()));
        setting.setDifThresh(Double.parseDouble(difThreshText.getText().trim()));
        return setting;
    }

    private void saveDefaultIndicator(String path, boolean def) {
        try {
            Properties property = new Properties();
            if (def) {
                property.setProperty("default", "yes");
            } else {
                property.setProperty("default", "no");
            }
            property.store(new FileOutputStream(path + File.separator + "defaultIndicator.properties"), "defaultIndicator.properties");

        } catch (IOException ex) {
            Logger.getLogger(Tracker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String loadDefaultIndicator(String path) {
        File in = new File(path + File.separator + "defaultIndicator.properties");


        if (!in.exists()) {
            System.out.println(in.getAbsolutePath() + " not exists");
            return null;
        }
        try {
            Properties property = new Properties();
            property.load(new FileInputStream(path + File.separator + "defaultIndicator.properties"));

            String def = property.getProperty("default");
            System.out.println(path + def);
            return def;

        } catch (IOException ex) {
            Logger.getLogger(Tracker.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    private void saveMotionDetectionSetting(String path) {
        try {
            Properties property = new Properties();
            property.setProperty("minArea", minAreaText.getText().trim());
            property.setProperty("maxArea", maxAreaText.getText().trim());
            //property.setProperty("thresh", threshText.getText().trim());
            property.setProperty("thresh", String.valueOf(threshSlider.getValue()));
            property.setProperty("nPieceX", nPieceXText.getText().trim());
            property.setProperty("nPieceY", nPieceYText.getText().trim());
            property.setProperty("difThresh", difThreshText.getText().trim());
            property.setProperty("difSize", difSizeText.getText().trim());
            if (!path.isEmpty()) {
                property.store(new FileOutputStream(path + File.separator + "motionDetection.properties"), "motionDetection.properties");
            } else {
                property.store(new FileOutputStream("motionDetection.properties"), "motionDetection.properties");
            }
        } catch (IOException ex) {
            Logger.getLogger(Tracker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loadMotionDetectionSetting(String folderpath) {
        String filepath = folderpath + File.separator + "motionDetection.properties";
        if (filepath.startsWith("\\")) {
            filepath = filepath.replace("\\", "");
        }

        File in = new File(filepath);
        if (!in.exists()) {
            return;
        }
        try {
            Properties property = new Properties();
            property.load(new FileInputStream(filepath));

            minAreaText.setText(property.getProperty("minArea"));
            maxAreaText.setText(property.getProperty("maxArea"));
            threshSlider.setValue(Integer.parseInt(property.getProperty("thresh")));
            threshText.setText(property.getProperty("thresh"));
            nPieceXText.setText(property.getProperty("nPieceX"));
            nPieceYText.setText(property.getProperty("nPieceY"));
            difSizeText.setText(property.getProperty("difSize"));
            difThreshText.setText(property.getProperty("difThresh"));
        } catch (IOException ex) {
            Logger.getLogger(Tracker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void levelSliderStateChanged(ChangeEvent ce) {
        if (samplePlus != null) {
            ImagePlus grb = samplePlus.duplicate();

            previewModeTrackingBatch = batchTrackingPreviewDialog.isShowing();

            binarize(grb, (int) ((double) levelSlider.getValue() / (double) 100 * 255));
            if (!previewModeTrackingBatch && trackingPrefDialog.isShowing()) {
                if (previewWindow == null) {
                    previewWindow = new ImageWindow(grb);
                    previewWindow.setTitle("Preview");
                } else {
                    previewWindow.setImage(grb);
                    previewWindow.show();
                }
            } else {
                batchTrackingPreviewImageLabel.setIcon(new ImageIcon(grb.getBufferedImage()));
            }

        }
    }

    private void threshSliderStateChanged(ChangeEvent ce) {
        if (previewPlus != null) {
            ImagePlus grb = previewPlus.duplicate();

            binarize(grb, (int) ((double) threshSlider.getValue() / (double) 100 * 255));
            //System.out.println((int) ((double) threshSlider.getValue() / (double) 100 * 255));
            if (motionDetectionPrefDialog.isShowing()) {
                if (previewWindow == null) {
                    previewWindow = new ImageWindow(grb);
                    previewWindow.setTitle("Preview");
                } else {

                    previewWindow.setImage(grb);
                    previewWindow.updateImage(grb);
                    previewWindow.show();
                }
            } else {
                batchCountingPreviewImageLabel.setIcon(new ImageIcon(grb.getProcessor().resize(640, 640).getBufferedImage()));
            }

        }
    }

    private ImagePlus grabFirstFrame(File video) {
        FrameReader frameReader = new FrameReader("file:\\" + video.getAbsolutePath());
        int i = 0;
        double mean = 0;
        ImagePlus grb = null;
        while (mean == 0) {
            grb = frameReader.grab(i);
            i++;
            ImageStatistics is = grb.getStatistics();
            mean = is.mean;
        }
        return grb;
    }

    private void binarize(ImagePlus grb, int level) {
        if (grb.getBitDepth() != 8) {
            ImageConverter icv = new ImageConverter(grb);
            icv.convertToGray8();
        }

        grb.getProcessor().invertLut();
        grb.getProcessor().threshold(level);
    }

    private void saveBatchTrackingPreviewActionPerformed(ActionEvent evt) {
        boolean alreadyPreviewed = checkPreviewed(currentPreviewedVideo);
        saveTrackingSetting(currentPreviewedVideo.getParentFile().getAbsolutePath());//save setting in the current previewed folder
        saveDefaultIndicator(currentPreviewedVideo.getParentFile().getAbsolutePath(), false); //it's not default

        //update the fileList, indicate it has been previewed
        for (int j = 0; j < fileList.size(); j++) {
            String filepath = fileList.get(j).getPath();
            if (filepath.equals(currentPreviewedVideo.getAbsolutePath())) { //also change the preview status in fileList to true
                fileList.get(j).setPreviewStatus(true);
            }
        }
        //update previewedCount info label
        int previewedCount = 0;
        for (int s = 0; s < fileList.size(); s++) {
            if (fileList.get(s).getPreviewStatus()) {
                previewedCount++;
            }
        }

        batchTrackingPreviewInfoLabel.setText(previewedCount + " out of " + fileList.size() + " videos have been previewed");
        if (previewedCount == fileList.size()) {
            nextNonPreviewedTrackingPreviewButton.setEnabled(false);
            if (!alreadyPreviewed) {
                JOptionPane.showMessageDialog(this, "All videos selected have been previewed", "Message", JOptionPane.INFORMATION_MESSAGE);
            }
        }
        //update current preview video info label
        currentTrackingPreviewInfoLabel.setText("<html>" + currentPreviewedVideo.getAbsolutePath() + "<br> <font color=\"#FF0000\"> has already been previewed at </font> " + lastPreviewedTime(currentPreviewedVideo) + "</html>");
        //batchTrackingPreviewDialog.pack();
    }

    //when the preview button is pressed
    private void videoPreviewActionPerformed(ActionEvent evt) {
        JFileChooser openFileChooser = new JFileChooser();
        openFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        openFileChooser.setMultiSelectionEnabled(false);
        openFileChooser.setFileFilter(new myFilter(".avi"));
        if (directory != null) {
            openFileChooser.setCurrentDirectory(new File(directory));
        }
        int state = openFileChooser.showOpenDialog(null);
        if (state == JFileChooser.APPROVE_OPTION) {
            File file = openFileChooser.getSelectedFile();
            ImagePlus grb = grabFirstFrame(file);
            //convert to grayscale
            samplePlus = grb.duplicate();

            binarize(grb, (int) ((double) levelSlider.getValue() / (double) 100 * 255));

            if (previewWindow == null) {
                previewWindow = new ImageWindow(grb);
                previewWindow.setTitle("Preview" + file.getAbsolutePath());
            } else {
                previewWindow.setImage(grb);
                previewWindow.setTitle("Preview" + file.getAbsolutePath());
                previewWindow.show();
            }
        } else {
            return;
        }
    }

    private String lastPreviewedTime(File video) {
        String filename = video.getParentFile().getAbsolutePath() + File.separator + "defaultIndicator.properties";
        File indicator = new File(filename);
        long time = indicator.lastModified();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        return (cal.getTime().toLocaleString());
    }

    private void changeTrackingPreviewActionPerformed(ActionEvent evt) {
        if (evt.getSource() == nextNonPreviewedTrackingPreviewButton) {
            if (fileList.getFirst().getPath().equals(currentPreviewedVideo.getAbsolutePath())) {
                fileList.addLast(fileList.removeFirst());
            }
            boolean encounter = false;
            while (true) {
                PreviewStatus next = fileList.removeFirst();
                currentPreviewedVideo = new File(next.getPath());
                encounter = checkPreviewed(currentPreviewedVideo);
                fileList.addLast(next);
                if (!encounter) {
                    break;
                }
            }
        }
        if (evt.getSource() == previousTrackingPreviewButton) {
            if (fileList.getLast().getPath().equals(currentPreviewedVideo.getAbsolutePath())) {
                fileList.addFirst(fileList.removeLast());
            }
            PreviewStatus previous = fileList.removeLast();
            currentPreviewedVideo = new File(previous.getPath());
            fileList.addFirst(previous);
        }
        if (evt.getSource() == nextTrackingPreviewButton) {
            if (fileList.getFirst().getPath().equals(currentPreviewedVideo.getAbsolutePath())) {
                fileList.addLast(fileList.removeFirst());
            }

            PreviewStatus next = fileList.removeFirst();
            currentPreviewedVideo = new File(next.getPath());
            fileList.addLast(next);

        }
        //if the setting file is in the folder, load it
        loadTrackingSetting(currentPreviewedVideo.getParentFile().getAbsolutePath());
        samplePlus = new ImagePlus(currentPreviewedVideo.getAbsolutePath().replace(".avi", ".jpeg"));
        ImagePlus grb = samplePlus.duplicate();
        binarize(grb, (int) ((double) levelSlider.getValue() / (double) 100 * 255));
        batchTrackingPreviewImageLabel.setIcon(new ImageIcon(grb.getBufferedImage()));
        //check whether the file has been previewed
        currentTrackingPreviewInfoLabel.setText("<html>" + currentPreviewedVideo.getAbsolutePath() + "<br> has not been previewed</html>");

        if (!(evt.getSource() == nextNonPreviewedTrackingPreviewButton)) {
            if (checkPreviewed(currentPreviewedVideo)) {
                System.out.println(currentPreviewedVideo.getAbsolutePath() + " has already been previewed at " + lastPreviewedTime(currentPreviewedVideo));
                currentTrackingPreviewInfoLabel.setText("<html>" + currentPreviewedVideo.getAbsolutePath() + "<br> <font color=\"#FF0000\"> has already been previewed at </font> " + lastPreviewedTime(currentPreviewedVideo) + "</html>");
                //batchTrackingPreviewDialog.pack();
            }
        }
    }

    private void imagePreviewActionPerformed(ActionEvent evt) {
        JFileChooser openFileChooser = new JFileChooser();
        openFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        openFileChooser.setMultiSelectionEnabled(false);
        //openFileChooser.setFileFilter(new myFilter(".avi"));
        if (directory != null) {
            openFileChooser.setCurrentDirectory(new File(directory));
        }
        int state = openFileChooser.showOpenDialog(null);
        if (state == JFileChooser.APPROVE_OPTION) {
            File file = openFileChooser.getSelectedFile();


            ImagePlus grb = assembleImage(file.getAbsolutePath());
            //convert to grayscale
            previewPlus = grb.duplicate();


            if (grb.getBitDepth() != 8) {
                ImageConverter icv = new ImageConverter(grb);
                icv.convertToGray8();
            }

            grb.getProcessor().invertLut();
            grb.getProcessor().threshold((int) ((double) threshSlider.getValue() / (double) 100 * 255));

            if (previewWindow == null) {
                previewWindow = new ImageWindow(grb);
                previewWindow.setTitle("Preview" + file.getAbsolutePath());
            } else {
                previewWindow.setImage(grb);
                previewWindow.setTitle("Preview" + file.getAbsolutePath());
                previewWindow.show();
            }
        } else {
            return;
        }
    }

    private void saveBatchCountingPreviewActionPerformed(ActionEvent evt) {

        boolean alreadyPreviewed = checkPreviewed(currentPreviewedImage);
        saveMotionDetectionSetting(currentPreviewedImage.getParentFile().getAbsolutePath());//save setting in the current previewed folder
        saveDefaultIndicator(currentPreviewedImage.getParentFile().getAbsolutePath(), false); //it's not default

        //update the fileList, indicate it has been previewed
        for (int j = 0; j < fileList.size(); j++) {
            String filepath = fileList.get(j).getPath();
            if (filepath.equals(currentPreviewedImage.getAbsolutePath())) { //also change the preview status in fileList to true
                fileList.get(j).setPreviewStatus(true);
            }
        }
        //update previewedCount info label
        int previewedCount = 0;
        for (int s = 0; s < fileList.size(); s++) {
            if (fileList.get(s).getPreviewStatus()) {
                previewedCount++;
            }
        }

        batchCountingPreviewInfoLabel.setText(previewedCount + " out of " + fileList.size() + " images have been previewed");
        if (previewedCount == fileList.size()) {
            nextNonPreviewedCountingPreviewButton.setEnabled(false);
            if (!alreadyPreviewed) {
                JOptionPane.showMessageDialog(this, "All images selected have been previewed", "Message", JOptionPane.INFORMATION_MESSAGE);
            }
        }
        //update current preview video info label
        currentCountingPreviewInfoLabel.setText("<html>" + currentPreviewedImage.getAbsolutePath() + "<br> <font color=\"#FF0000\"> has already been previewed at </font> " + lastPreviewedTime(currentPreviewedImage) + "</html>");

    }

    private void changeCountingPreviewActionPerformed(ActionEvent evt) {
        if (evt.getSource() == nextNonPreviewedCountingPreviewButton) {
            if (fileList.getFirst().getPath().equals(currentPreviewedImage.getAbsolutePath())) {
                fileList.addLast(fileList.removeFirst());
            }
            boolean encounter = false;
            while (true) {
                PreviewStatus next = fileList.removeFirst();
                currentPreviewedImage = new File(next.getPath());
                encounter = checkPreviewed(currentPreviewedImage);
                fileList.addLast(next);
                if (!encounter) {
                    break;
                }
            }
        }
        if (evt.getSource() == previousCountingPreviewButton) {
            if (fileList.getLast().getPath().equals(currentPreviewedImage.getAbsolutePath())) {
                fileList.addFirst(fileList.removeLast());
            }
            PreviewStatus previous = fileList.removeLast();
            currentPreviewedImage = new File(previous.getPath());
            fileList.addFirst(previous);
        }
        if (evt.getSource() == nextCountingPreviewButton) {
            if (fileList.getFirst().getPath().equals(currentPreviewedImage.getAbsolutePath())) {
                fileList.addLast(fileList.removeFirst());
            }

            PreviewStatus next = fileList.removeFirst();
            currentPreviewedImage = new File(next.getPath());
            fileList.addLast(next);

        }
        //if the setting file is in the folder, load it
        loadMotionDetectionSetting(currentPreviewedImage.getParentFile().getAbsolutePath());
        previewPlus = new ImagePlus(currentPreviewedImage.getAbsolutePath());
        ImagePlus grb = previewPlus.duplicate();
        binarize(grb, (int) ((double) threshSlider.getValue() / (double) 100 * 255));
        batchCountingPreviewImageLabel.setIcon(new ImageIcon(grb.getProcessor().resize(640, 640).getBufferedImage()));
        //check whether the file has been previewed
        currentCountingPreviewInfoLabel.setText("<html>" + currentPreviewedImage.getAbsolutePath() + "<br> has not been previewed</html>");

        if (!(evt.getSource() == nextNonPreviewedCountingPreviewButton)) {
            if (checkPreviewed(currentPreviewedImage)) {
                System.out.println(currentPreviewedImage.getAbsolutePath() + " has already been previewed at " + lastPreviewedTime(currentPreviewedImage));
                currentCountingPreviewInfoLabel.setText("<html>" + currentPreviewedImage.getAbsolutePath() + "<br> <font color=\"#FF0000\"> has already been previewed at </font> " + lastPreviewedTime(currentPreviewedImage) + "</html>");
                //batchTrackingPreviewDialog.pack();
            }
        }

    }

    //when open file in tracking menu is pressed
    private void trackingItemActionPerformed(ActionEvent evt) {
        JFileChooser openFileChooser = new JFileChooser();
        openFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        openFileChooser.setMultiSelectionEnabled(true);
        openFileChooser.setFileFilter(new myFilter(".avi"));
        if (directory != null) {
            openFileChooser.setCurrentDirectory(new File(directory));
        }
        int state = openFileChooser.showOpenDialog(null);
        if (state == JFileChooser.APPROVE_OPTION) {
            File[] files = openFileChooser.getSelectedFiles();
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (file.isDirectory()) {
                    directory = file.getParent();
                    File[] lists = file.listFiles(new myFilter(".avi")); //get all of the files in that directory
                    for (int k = 0; k < lists.length; k++) {
                        File listFile = lists[k];
                        String path = listFile.getAbsolutePath();
                        String str = path.substring(0, path.lastIndexOf("."));
                        Tracker tracker = new Tracker(str, ".avi", getTrackingSetting(), true);
                        tracker.runner = new Thread(tracker);
                        tracker.runner.start();

                    }

                } else if (file.isFile()) {
                    directory = file.getParent();
                    String path = file.getAbsolutePath();
                    String str = path.substring(0, path.lastIndexOf("."));
                    Tracker tracker = new Tracker(str, ".avi", getTrackingSetting(), true);
                    tracker.runner = new Thread(tracker);
                    tracker.runner.start();
                }
            }

        } else {
            return;
        }  //no folder is selected

    }

    //when open file item in analysis menu is pressed
    private void analysisItemActionPerformed(ActionEvent evt) {
        JFileChooser openFileChooser = new JFileChooser();
        openFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        openFileChooser.setMultiSelectionEnabled(true);
        if (directory != null) {
            openFileChooser.setCurrentDirectory(new File(directory));
        }
        openFileChooser.setFileFilter(new myFilter(".file"));
        int state = openFileChooser.showOpenDialog(null);
        if (state == JFileChooser.APPROVE_OPTION) {
            File[] files = openFileChooser.getSelectedFiles();
            String[] names = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                directory = file.getParent();
                String path = file.getAbsolutePath();
                String str = path.substring(0, path.lastIndexOf("."));
                names[i] = str;
                //TrackAnalyzer analyzer = new TrackAnalyzer(str, getAnalysisSetting());
                //analyzer.analyzeTrack(str);            
            }
            TrackAnalyzer analyzer = new TrackAnalyzer(null, getAnalysisSetting());
            analyzer.analyzeMultipleTracks(names);

        } else {
            return;
        }

    }

    private void analyzeEveryFolderItemActionPerformed(ActionEvent evt) {
        JFileChooser openFileChooser = new JFileChooser();
        openFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); //only directory can be chosen
        openFileChooser.setMultiSelectionEnabled(true);
        //openFileChooser.setFileFilter(new myFilter(".avi"));
        if (directory != null) {
            openFileChooser.setCurrentDirectory(new File(directory));
        }
        int state = openFileChooser.showOpenDialog(null);
        if (state == JFileChooser.APPROVE_OPTION) {
            File[] dirlist = openFileChooser.getSelectedFiles();

            FileWriter TrackingResultWriter = null;
            String rootPath = null;
            if (dirlist.length > 1) {      //if there are more than one mother folders selected,create result file in parentfolder
                rootPath = dirlist[0].getParentFile().getAbsolutePath();

            } else {
                rootPath = dirlist[0].getAbsolutePath();      //if there is only one root folder selected, create result file in it       
            }
            directory = dirlist[0].getParentFile().getAbsolutePath();
            File trackingresult = new File(rootPath + File.separator + "TrackingResults.txt");

            //if TrackingResults.txt already exists
            if (trackingresult.exists()) {
                Object[] options = {"Yes, overwrite",
                    "Keep a copy of the old file", "Cancel"};

                int n = JOptionPane.showOptionDialog(this, "It seems that you have already analyzed this folder. Would you like to overwrite?", "Be careful",
                        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]); //show options

                if (n == JOptionPane.CLOSED_OPTION || n == JOptionPane.CANCEL_OPTION) {
                    return;               //do nothing
                } else if (n == JOptionPane.YES_OPTION) {
                    //overwrite, delete the old file
                    trackingresult.delete();
                } else if (n == JOptionPane.NO_OPTION) {
                    Date date = new Date();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-E-hh-mm-ss");
                    trackingresult.renameTo(new File(rootPath + File.separator + simpleDateFormat.format(date) + "_TrackingResults.txt"));
                }
            }//if exists()

            try {
                TrackingResultWriter = new FileWriter(rootPath + File.separator + "TrackingResults.txt", true);
            } catch (IOException ex) {
                Logger.getLogger(JavaPWT.class.getName()).log(Level.SEVERE, null, ex);
            }


            BufferedWriter bufferedWriter = new BufferedWriter(TrackingResultWriter);
            try {
                bufferedWriter.append("#date" + "\t" + "filepath" + "\t" + "whether default" + "\t" + "avg" + "\t" + "avg cut" + "\t" + "p25" + "\t" + "p50" + "\t" + "p75" + "\t" + "p95" + "\t" + "p99");
                bufferedWriter.newLine();
            } catch (IOException ex) {
                Logger.getLogger(JavaPWT.class.getName()).log(Level.SEVERE, null, ex);
            }

            for (int k = 0; k < dirlist.length; k++) {
                File file = dirlist[k];
                LinkedList list = new LinkedList();
                File files[] = file.listFiles();

                list.addAll(Arrays.asList(files));

                File tmp;
                while (!list.isEmpty()) {
                    tmp = (File) list.removeFirst();
                    if (tmp.isDirectory()) {
                        files = tmp.listFiles();

                        if (files == null) {
                            continue;
                        } else {
                            list.addAll(Arrays.asList(files));
                        }
                    } else { //not a directory
                        if (tmp.getName().endsWith(".file")) {
                            String path = tmp.getAbsolutePath();
                            //get the setting file 
                            File parent = tmp.getParentFile();
                            saveAnalysisSetting(parent.getAbsolutePath());//save the analysis setting                          
                            //Analyzing
                            TrackAnalyzer analyzer = new TrackAnalyzer(null, getAnalysisSetting());
                            double[] allSpeedPoints = analyzer.analyzeTrack(path);
                            double[] a = analyzer.calcHistogram(allSpeedPoints);
                            double[] cdf = analyzer.calcCDF(a);
                            double avg = analyzer.calcAvgSpeed(a);
                            double avg_cut = analyzer.calcAvgSpeed(allSpeedPoints, analyzer.speedThresh);
                            Percentile p = new Percentile();
                            p.setData(allSpeedPoints);
                            double p25 = p.evaluate(25);
                            double p50 = p.evaluate(50);
                            double p75 = p.evaluate(75);
                            double p95 = p.evaluate(95);
                            double p99 = p.evaluate(99);
                            //save to the big file
                            String def = loadDefaultIndicator(parent.getAbsolutePath());
                            if (def == null) {
                                def = "yes";
                            }
                            Date date = new Date();
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd E hh:mm:ss");
                            try { //save date,path,whether default,avg, avg cut, p25, p50, p75, p95, p99
                                bufferedWriter.append(simpleDateFormat.format(date) + "\t" + tmp.getAbsolutePath() + "\t" + def + "\t" + allSpeedPoints.length + "\t" + avg + "\t" + avg_cut + "\t" + p25 + "\t" + p50 + "\t" + p75 + "\t" + p95 + "\t" + p99);
                                bufferedWriter.newLine();
                            } catch (IOException ex) {
                                Logger.getLogger(JavaPWT.class.getName()).log(Level.SEVERE, null, ex);
                            }

                            System.out.println(tmp.getAbsolutePath());
                        }

                    }//not a directory
                }
            }
            try {
                bufferedWriter.close();
                TrackingResultWriter.close();
            } catch (IOException ex) {
                Logger.getLogger(JavaPWT.class.getName()).log(Level.SEVERE, null, ex);
            }


        } //if approve
        else {
            return;
        }


    }

   //when open file item in motion detection menu is pressed 
    private void motionDetectionItemActionPerformed(ActionEvent evt) {
        JFileChooser openFileChooser = new JFileChooser( "c:\\data" );
        openFileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
        openFileChooser.setMultiSelectionEnabled( false );
        if (directory != null) {
            openFileChooser.setCurrentDirectory( new File( directory ) );
        }
        int state = openFileChooser.showOpenDialog( this );
        if( state == JFileChooser.APPROVE_OPTION ) {
            File file1 = openFileChooser.getSelectedFile();
            String path1 = file1.getAbsolutePath();
            String path2 = null;
            directory = file1.getParent();
            System.out.println(path1);
            if (!path1.endsWith("__1")) {
                path2 = path1 + "__1";
            } //end of !endwith __1
            else {
                path2 = path1.substring(0, file1.getAbsolutePath().length() - "__1".length() );
            }//end of endwith __1
            if (!new File(path2).exists()) {
                JOptionPane.showMessageDialog(this, "No Results To Save!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (detector == null) {
                detector = new motionDetector( this );
            }
            detector.setFolders(path1, path2);
            detector.setSetting(getMotionDetectionSetting());
            detector.detect();
        }
    }

    private void saveTrackingSettingActionPerformed(ActionEvent evt) {
        saveTrackingSetting("");
    }

    private void saveAnalysisSettingActionPerformed(ActionEvent evt) {
        saveAnalysisSetting("");
    }

    private void saveMotionDetectionSettingActionPerformed(ActionEvent evt) {
        saveMotionDetectionSetting("");
    }

    private void displayDialog(JDialog Dialog) {
        Dialog.setLocationRelativeTo(this);
        Dialog.setSize(Dialog.getPreferredSize());
        Dialog.pack();
        Dialog.setVisible(true);
    }

    private void trackingPrefItemActionPerformed(ActionEvent evt) {
        //this way (adding components on demand) the same panel is shared...
        videoPreviewButton.setVisible(true);
        saveTrackingSettingButton.setVisible(true);
        saveBatchTrackingPreviewButton.setVisible(false);
        trackingPrefDialog.add(trackingPrefPanel);
        loadTrackingSetting("");
        displayDialog(trackingPrefDialog);
    }

    private int BFSCount(File[] selectedFiles, String suffix) {
        int count = 0;
        for (int k = 0; k < selectedFiles.length; k++) {
            File file = selectedFiles[k];
            LinkedList list = new LinkedList();
            File files[] = file.listFiles();
            list.addAll(Arrays.asList(files));
            File tmp;
            while (!list.isEmpty()) {
                tmp = (File) list.removeFirst();
                if (tmp.isDirectory()) {
                    files = tmp.listFiles();
                    if (files == null) {
                        continue;
                    } else {
                        list.addAll(Arrays.asList(files));
                    }
                } else {
                    if (tmp.getName().endsWith(suffix)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void batchTrackingItemActionPerformed(ActionEvent evt) {
        JFileChooser openFileChooser = new JFileChooser();
        openFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); //only directory can be chosen
        openFileChooser.setMultiSelectionEnabled(true);
        //openFileChooser.setFileFilter(new myFilter(".avi"));
        if (directory != null) {
            openFileChooser.setCurrentDirectory(new File(directory));
        }
        int state = openFileChooser.showOpenDialog(null);
        if (state == JFileChooser.APPROVE_OPTION) {
            currentTask = JavaPWT.BATCH_TRACKING_TASK;
            mtpe = new MyThreadPoolExecutor(10, 10, 2, TimeUnit.HOURS, new ArrayBlockingQueue<Runnable>(3000)); //the threadpool limits the number of thread being executed concurrently (preventing out of memory error)
            File[] dirlist = openFileChooser.getSelectedFiles();
            directory = dirlist[0].getParentFile().getAbsolutePath();

            final int totalVideoCount = BFSCount(dirlist, ".avi");
            SwingProgressBar spb = new SwingProgressBar(totalVideoCount, "Tracking Progress");
            spb.start();

            for (int k = 0; k < dirlist.length; k++) {
                File file = dirlist[k];
                LinkedList list = new LinkedList();
                File files[] = file.listFiles();
                list.addAll(Arrays.asList(files));
                File tmp;
                while (!list.isEmpty()) {
                    tmp = (File) list.removeFirst();
                    if (tmp.isDirectory()) {
                        files = tmp.listFiles();
                        if (files == null) {
                            continue;
                        } else {
                            list.addAll(Arrays.asList(files));
                        }
                    } else {
                        if (tmp.getName().endsWith(".avi")) {
                            String path = tmp.getAbsolutePath();
                            String str = path.substring(0, path.lastIndexOf("."));

                            //get the setting file 
                            File parent = tmp.getParentFile();
                            String trackingSettingPath = parent.getAbsolutePath() + File.separator + "tracking.properties";
                            File trackingSetting = new File(trackingSettingPath);
                            if (!trackingSetting.exists()) { //if there is no saved setting file, use the default; create a file indicating that it is default
                                saveTrackingSetting(parent.getAbsolutePath());
                                saveDefaultIndicator(parent.getAbsolutePath(), true);
                            } else {//if there is saved setting file,load it and use it
                                loadTrackingSetting(parent.getAbsolutePath());
                            }

                            Tracker tracker = new Tracker(str, ".avi", getTrackingSetting(), false);
                            mtpe.runTask(tracker);
                            //tracker.runner.start();
                            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                            int threadCount = bean.getThreadCount();
                            System.out.println("threadCount:" + threadCount);
                            System.out.println(tmp.getAbsolutePath());
                        }

                    }
                }
            }
            mtpe.shutdown();

        } else {
            return;
        }
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        int threadCount = bean.getThreadCount();
        System.out.println("threadCount:" + threadCount);
    }

    private void batchTrackingPreviewItemActionPerformed(ActionEvent evt) {
        JFileChooser openFileChooser = new JFileChooser();
        openFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); //only directory can be chosen
        openFileChooser.setMultiSelectionEnabled(true);
        //openFileChooser.setFileFilter(new myFilter(".avi"));
        if (directory != null) {
            openFileChooser.setCurrentDirectory(new File(directory));
        }
        int state = openFileChooser.showOpenDialog(null);
        if (state == JFileChooser.APPROVE_OPTION) {

            currentTask = JavaPWT.BATCH_SNAPSHOTTING_TASK;

            batchTrackingPreviewPanel.add(trackingPrefPanel, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            batchTrackingPreviewPanel.add(trackingPreviewImageDisplayPanel, new GBC(1, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            batchTrackingPreviewDialog.add(batchTrackingPreviewPanel);
            //display the hidden save button
            videoPreviewButton.setVisible(false);
            saveTrackingSettingButton.setVisible(false);
            saveBatchTrackingPreviewButton.setVisible(true);

            File[] dirlist = openFileChooser.getSelectedFiles();
            int totalVideoCount = BFSCount(dirlist, ".avi"); //count total number of videos
            System.out.println("totalnumber of videos: " + totalVideoCount);

            //create snapshot for everyone
            fileList = new LinkedList();
            SwingProgressBar spb = new SwingProgressBar(totalVideoCount, "Creating snapshots");
            spb.start();
            snapshotted = 0;

            for (int k = 0; k < dirlist.length; k++) {
                File file = dirlist[k];
                LinkedList list = new LinkedList();
                File files[] = file.listFiles();
                list.addAll(Arrays.asList(files));
                File tmp;
                while (!list.isEmpty()) {
                    tmp = (File) list.removeFirst();
                    if (tmp.isDirectory()) {
                        files = tmp.listFiles();
                        if (files == null) {
                            continue;
                        } else {
                            list.addAll(Arrays.asList(files));
                        }
                    } else {
                        if (tmp.getName().endsWith(".avi")) { //save the first frame image in hard disk
                            //check whether the video has already been previewed
                            boolean previewed = checkPreviewed(tmp);
                            fileList.add(new PreviewStatus(tmp.getAbsolutePath(), previewed));//add to the list 
                            if (!new File(tmp.getAbsolutePath().replace(".avi", ".jpeg")).exists()) { //create snapshot image in the folder if not exists
                                ImagePlus firstFrame = grabFirstFrame(tmp);
                                FileSaver fileSaver = new FileSaver(firstFrame);
                                fileSaver.saveAsJpeg(tmp.getAbsolutePath().replace(".avi", ".jpeg"));
                            }
                            snapshotted++;
                        }
                    }
                }
            }

            spb.finish();

            //no video; finish
            if (fileList.size() == 0) {
                JOptionPane.showMessageDialog(this, "No video file Detected", "Message", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int previewedCount = 0;
            for (int s = 0; s < fileList.size(); s++) {
                if (fileList.get(s).getPreviewStatus()) {
                    previewedCount++;
                }
            }

            batchTrackingPreviewInfoLabel.setText(previewedCount + " out of " + fileList.size() + " videos have been previewed");
            if (previewedCount == fileList.size()) {
                nextNonPreviewedTrackingPreviewButton.setEnabled(false);
                JOptionPane.showMessageDialog(this, "All videos selected have been previewed", "Message", JOptionPane.INFORMATION_MESSAGE);
            } else if (fileList.size() >= 2) {
                nextNonPreviewedTrackingPreviewButton.setEnabled(true);
            }

            //disable next and previous button if there is only one video
            previousTrackingPreviewButton.setEnabled(fileList.size() >=2);
            nextTrackingPreviewButton.setEnabled(fileList.size() >=2);
            
            currentPreviewedVideo = new File(fileList.get(0).getPath());

            //if the setting file is in the folder, load it
            loadTrackingSetting(currentPreviewedVideo.getParentFile().getAbsolutePath());


            samplePlus = new ImagePlus(currentPreviewedVideo.getAbsolutePath().replace(".avi", ".jpeg"));
            ImagePlus grb = samplePlus.duplicate();
            binarize(grb, (int) ((double) levelSlider.getValue() / (double) 100 * 255));
            batchTrackingPreviewImageLabel.setIcon(new ImageIcon(grb.getBufferedImage()));

            currentTrackingPreviewInfoLabel.setText("<html>" + currentPreviewedVideo.getAbsolutePath() + "<br> has not been previewed</html>");

            if (checkPreviewed(currentPreviewedVideo)) {
                System.out.println(currentPreviewedVideo.getAbsolutePath() + " has already been previewed at " + lastPreviewedTime(currentPreviewedVideo));
                currentTrackingPreviewInfoLabel.setText("<html>" + currentPreviewedVideo.getAbsolutePath() + "<br> <font color=\"#FF0000\"> has already been previewed at </font> " + lastPreviewedTime(currentPreviewedVideo) + "</html>");
            }
            displayDialog(batchTrackingPreviewDialog);

        } else {
            return;
        }
    }

    private boolean checkPreviewed(File tmp) {
        String defaultString = loadDefaultIndicator(tmp.getParentFile().getAbsolutePath());
        Boolean previewed = false;
        if (defaultString == null || defaultString.equals("yes")) {
            previewed = false;
        } else if (defaultString.equals("no")) {
            previewed = true;
        }
        return previewed;
    }

    private void analysisPrefItemActionPerformed(ActionEvent evt) {
        displayDialog(analysisPrefDialog);
    }

    private void motionDetectionPrefItemActionPerformed(ActionEvent evt) {
        imagePreviewButton.setVisible(true);
        saveMotionDetectionSettingButton.setVisible(true);
        saveBatchCountingPreviewButton.setVisible(false);
        motionDetectionPrefDialog.add(motionDetectionPrefPanel);
        loadMotionDetectionSetting(""); //load the default
        displayDialog(motionDetectionPrefDialog);
    }

    private void batchCountingPreviewItemActionPerformed(ActionEvent evt) {
        JFileChooser openFileChooser = new JFileChooser();
        openFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); //only directory can be chosen
        openFileChooser.setMultiSelectionEnabled(true);
        //openFileChooser.setFileFilter(new myFilter(".avi"));
        if (directory != null) {
            openFileChooser.setCurrentDirectory(new File(directory));
        }
        int state = openFileChooser.showOpenDialog(null);
        if (state == JFileChooser.APPROVE_OPTION) {

            currentTask = JavaPWT.BATCH_ASSEMBLING_TASK;
            mtpe = new MyThreadPoolExecutor(10, 10, 2, TimeUnit.HOURS, new ArrayBlockingQueue<Runnable>(3000));

            batchCountingPreviewPanel.add(motionDetectionPrefPanel, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            batchCountingPreviewPanel.add(countingPreviewImageDisplayPanel, new GBC(1, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL));
            batchCountingPreviewDialog.add(batchCountingPreviewPanel);

            //display the hidden save button
            imagePreviewButton.setVisible(false);
            saveMotionDetectionSettingButton.setVisible(false);
            saveBatchCountingPreviewButton.setVisible(true);

            File[] dirlist = openFileChooser.getSelectedFiles();
            int totalWellCount = BFSCount(dirlist, "thelog.txt"); //count total number of videos
            System.out.println("totalnumber of videos: " + totalWellCount);

            fileList = new LinkedList();
            SwingProgressBar spb = new SwingProgressBar(totalWellCount, "Assembling");
            spb.start();
   long time1 = System.currentTimeMillis();
            for (int k = 0; k < dirlist.length; k++) {
                File file = dirlist[k];
                LinkedList list = new LinkedList();
                File files[] = file.listFiles();
                list.addAll(Arrays.asList(files));
                File tmp;
                while (!list.isEmpty()) {
                    tmp = (File) list.removeFirst();
                    if (tmp.isDirectory()) {
                        files = tmp.listFiles();
                        if (files == null) {
                            continue;
                        } else {
                            list.addAll(Arrays.asList(files));
                        }
                    } else {
                        if (tmp.getName().endsWith("thelog.txt")) { //save the first frame image in hard disk
                            //check whether the video has already been previewed
                            boolean previewed = checkPreviewed(tmp);
                            fileList.add(new PreviewStatus(tmp.getParentFile().getAbsolutePath() + File.separator + "assembled.jpeg", previewed));//add to the list 
                            final String folder = tmp.getParentFile().getAbsolutePath();
                            //assembleImage(folder);
                            //create assembled image
                            mtpe.runTask(new Runnable() {
                                public void run() {
                                    System.out.println("I am doing" + folder);
                                    assembleImage(folder);
                                }
                            });
                        }
                    }
                }
            }
            mtpe.shutdown();
            //wait for all thread to finish
            try {
                mtpe.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException ex) {
                Logger.getLogger(JavaPWT.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                spb.finish();
            }
               long time2 = System.currentTimeMillis();
               
               
               System.out.println("the whole assembly takes"+ (time2-time1));
            //remove underscored folder
            for (int s = 0; s < fileList.size(); s++) {
                if (fileList.get(s).getPath().contains("__")) {
                    fileList.remove(s);
                    s--;
                }
            }

            //no well; finish
            if (fileList.size() == 0) {
                JOptionPane.showMessageDialog(this, "No well folder detected", "Message", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int previewedCount = 0;
            for (int s = 0; s < fileList.size(); s++) {
                if (fileList.get(s).getPreviewStatus()) {
                    previewedCount++;
                }
            }

            batchCountingPreviewInfoLabel.setText(previewedCount + " out of " + fileList.size() + " images have been previewed");
            if (previewedCount == fileList.size()) {
                nextNonPreviewedCountingPreviewButton.setEnabled(false);
                JOptionPane.showMessageDialog(this, "All image folders selected have been previewed", "Message", JOptionPane.INFORMATION_MESSAGE);
            } else if (fileList.size() >= 2) {
                nextNonPreviewedCountingPreviewButton.setEnabled(true);
            }

            //disable next and previous button if there is only one video  
                previousCountingPreviewButton.setEnabled(fileList.size() >= 2);
                nextCountingPreviewButton.setEnabled(fileList.size()>=2);

            currentPreviewedImage = new File(fileList.get(0).getPath());
            //if the setting file is in the folder, load it
            loadMotionDetectionSetting(currentPreviewedImage.getParentFile().getAbsolutePath());
            System.out.println("preview image:" + currentPreviewedImage.getAbsolutePath());
            previewPlus = new ImagePlus(currentPreviewedImage.getAbsolutePath());
            ImagePlus grb = previewPlus.duplicate();
            binarize(grb, (int) ((double) threshSlider.getValue() / (double) 100 * 255));
            batchCountingPreviewImageLabel.setIcon(new ImageIcon(grb.getProcessor().resize(640, 640).getBufferedImage()));

            currentCountingPreviewInfoLabel.setText("<html>" + currentPreviewedImage.getAbsolutePath() + "<br> has not been previewed</html>");
            if (checkPreviewed(currentPreviewedImage)) {
                System.out.println(currentPreviewedImage.getAbsolutePath() + " has already been previewed at " + lastPreviewedTime(currentPreviewedImage));
                currentCountingPreviewInfoLabel.setText("<html>" + currentPreviewedImage.getAbsolutePath() + "<br> <font color=\"#FF0000\"> has already been previewed at </font> " + lastPreviewedTime(currentPreviewedImage) + "</html>");
            }
            displayDialog(batchCountingPreviewDialog);
        } else {
            return;
        }
    }
    
     private void motionDetectionViewReportActionPerformed( ActionEvent actionEvent ) {
    	//TODO
    	System.out.println( "motionDetectionViewReportActionPerformed" );
    	JFileChooser fileChooser = new JFileChooser( System.getProperty( "user.dir" ) );
    	fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
    	int returnValue = fileChooser.showOpenDialog( this );
		if( returnValue == JFileChooser.APPROVE_OPTION ) {
			File dir = fileChooser.getSelectedFile();
			System.out.println( "report: " + dir.getAbsolutePath() );
			ResultProcessor resultProcessor = new ResultProcessor();
			resultProcessor.recursivelyProcessDirectory( dir.getAbsolutePath() );
			JOptionPane.showMessageDialog( this, "Finished Results Processing.\n" + dir.getAbsolutePath() );
		}; // if
    }

    public ImagePlus assembleImage(String name) {
        System.out.println("assembleImage()");
        long time1 = System.currentTimeMillis();
        File det = new File(name + File.separator + "assembled.jpeg");
        if (det.exists()) {
            ImagePlus assembled = new ImagePlus(name + File.separator + "assembled.jpeg");
            System.out.println("loaded assembled image!");
            return assembled;
        }
        ImagePlus assembled = NewImage.createByteImage("assembled", Integer.parseInt(nPieceXText.getText().trim()) * 640, Integer.parseInt(nPieceYText.getText().trim()) * 480, 1, NewImage.FILL_BLACK);
        ImageProcessor ipAssembled = assembled.getProcessor();
        System.out.println("Directory: " + name);
        for (int i = 1; i <= Integer.parseInt(nPieceXText.getText().trim()); i++) {
            for (int j = 1; j <= Integer.parseInt(nPieceYText.getText().trim()); j++) {
                String st = String.valueOf(i + Integer.parseInt(nPieceXText.getText().trim()) * (j - 1));
                String path = name + File.separator + "piece_" + st + ".jpeg";
                ImagePlus tempIP = new ImagePlus(path.toString());
                ipAssembled.copyBits(tempIP.getProcessor(), 640 * (i - 1), 480 * (j - 1), Blitter.ADD);
            }
        }
        FileSaver saver = new FileSaver(assembled);
        saver.saveAsJpeg(name + File.separator + "assembled.jpeg");
        long time2 = System.currentTimeMillis();
        System.out.println("Time for assembling:" + (time2 - time1));

        return assembled;
        //iw.show();
    }

    public static int getTaskProgress() {
        if (currentTask.equals(JavaPWT.BATCH_TRACKING_TASK)) {
            return (int) (JavaPWT.mtpe.getCompletedTaskCount());
        }
        if (currentTask.equals(JavaPWT.BATCH_SNAPSHOTTING_TASK)) {
            return JavaPWT.snapshotted;
        }
        if (currentTask.equals(JavaPWT.BATCH_ASSEMBLING_TASK)) {
            return (int) (JavaPWT.mtpe.getCompletedTaskCount());
        }

        return -1;
    }

    public static void main(String[] args) {
        JavaPWT test = new JavaPWT();
    }
}

class SwingProgressBar {

    final static int interval = 100;
    int maximum;
    JLabel label;
    JProgressBar pb;
    javax.swing.Timer timer;
    JFrame frame;

    public SwingProgressBar(int max, String name) {
        maximum = max;
        frame = new JFrame(name);

        pb = new JProgressBar(0, maximum);
        pb.setValue(0);
        pb.setStringPainted(true);

        label = new JLabel("Processing...");

        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(label, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        panel.add(pb, new GBC(0, 1).setInsets(5, 50, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));

        frame.setContentPane(panel);
        frame.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((screenSize.width) / 2, (screenSize.height) / 2);
        frame.setVisible(true);

        //Create a timer.
        timer = new javax.swing.Timer(interval, new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                //if(maximum==JavaPWT.snapshotted)
                if (maximum == JavaPWT.getTaskProgress()) {
                    Toolkit.getDefaultToolkit().beep();
                    timer.stop();
                    pb.setValue(0);
                    String str = "<html>" + "<font color=\"#FF0000\">" + "<b>"
                            + "Completed." + "</b>" + "</font>" + "</html>";
                    label.setText(str);
                }
                pb.setValue(JavaPWT.getTaskProgress());
                //pb.setValue(JavaPWT.snapshotted);
            }
        });
    }

    public void start() { //start the timer; the bar will be updated at certain time interval
        timer.start();
    }

    public void setProgress(int i) {
        pb.setValue(i);
    }

    public void finish() {
        frame.setVisible(false);
    }
}
