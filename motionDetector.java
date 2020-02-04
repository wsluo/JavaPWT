/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javapwt;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.process.Blitter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.TypeConverter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

/**
 *
 * @author wawnx
 */

public final class motionDetector {

	/** filename to use for saving results */
	public static final String N_LIVE_RESULTS_TXT = "nlive.txt";
	
	/** filename prefix to use for historical files of results */
	public static final String HISTORICAL = "historical.";
	
	/** constant used in results file */
	public static final String WORM_DETAILS = "#worm-details:";
	
	/** constant used in results file */
	public static final String INSPECTED_BY_HUMAN = "Inspected-by-human";
	
    private final int picSizeX = 640;
    private final int picSizeY = 480;
    private final int ICON_PADDING = 0;
    private int pieceX = 7;
    private int pieceY = 9;
    private int croppedX = 0;
    private int croppedY = 0;
    private int currentPage;
    private int buttonSize = 250;
    private int autoCount=0;
    private int difSize=100;
    double single_level = 0.2;
    double dif_level = 0.1;
    int minSegArea = 400;
    int maxSegArea = 4000;
    private SequentialLabeling sq_sub = null;
    private SequentialLabeling sq = null;
    private LinkedList<BinaryRegion> roiList = null;
    ImagePlus assembled1=null;
    ImagePlus assembled2=null;
    ImagePlus substractedImage=null;
    ImagePlus sub1_bw=null;
    ImagePlus assembled2_bw=null;
    private JDialog inspectDialog;
    private JPanel buttonPanel;
    private JPanel operationPanel;
    private JPanel panel;
    private JButton previousPageButton;
    private JButton nextPageButton;
    private JButton saveButton;
    private JButton viewPlateButton;
    private JLabel summaryLabel;
    private JLabel countLabel;
    private JLabel pageLabel;
    private JButton[] eachButton=null;
    private List<WormInfo> wormsList=null;
    private List<ImageIcon> wormColorIconList = null;
    private JPopupMenu popupMenu = null;
    private String folder1;
    private String folder2;
    private final JFrame parentFrame; 
   
    
    /**
	 * Constructor 
	 */
	public motionDetector( JFrame frame ) {
		super();
		parentFrame = frame;
	}

	public void reset(){
        sq_sub = null;
        sq = null;
        roiList = null;
        assembled1=null;
        assembled2=null;
        substractedImage = null;
        sub1_bw=null;
        assembled2_bw=null;
        wormsList=null;
        wormColorIconList = null;
        autoCount = 0;
    }
    
	
    public void setSetting( MotionDetectionSetting setting ){
        this.pieceX = setting.getPieceX();
        this.pieceY = setting.getPieceY();
        this.single_level = setting.getThresh();
        this.minSegArea = setting.getMinArea();
        this.maxSegArea = setting.getMaxArea();
        this.dif_level=setting.getDifThresh();
        this.difSize=setting.getDifSize();
    }
    
    
    public void setFolders(String folder1, String folder2){
		if( folder1.endsWith("__1" ) == true && folder2.endsWith( "__1" ) == false ) {
            this.folder1 = folder2;
            this.folder2 = folder1;
		}; // if
		if( folder1.endsWith("__1" ) == false && folder2.endsWith( "__1" ) == true ) {
            this.folder1 = folder1;
            this.folder2 = folder2;
		}; // if
		if( this.folder1 == null || this.folder2 == null ) {
			JOptionPane.showMessageDialog( inspectDialog, "Unable to determine the before/after folders: " + folder1 + " , " + folder2, "Eror", JOptionPane.ERROR_MESSAGE );
			return;
		}; // if
		//reset global variables to null       
		reset();
    }
    
    
    public void detect(){
        initComponents();
        
        // see whether we can load settings specific to this folder (folder1)
        File file = new File( folder1 + File.separator + MotionDetectionSetting.PROPERTIES_FILENAME );
        if( file.exists() == true ) {
        	System.out.println( "using folder-specific motion-settings" );
        	MotionDetectionSetting settings = MotionDetectionSetting.loadMotionDetectionSettingFromFile( folder1 );
        	if( settings != null ) {
        		setSetting( settings );
        	}
        	else {
        		JOptionPane.showMessageDialog( inspectDialog, 
        				"The motion-settings file has errors", "Eror in motion settings", JOptionPane.ERROR_MESSAGE );
        	}; // if
        }; // if

        assembled1 = assembleImage(folder1);
        assembled2 = assembleImage(folder2);

        //image1-image2
        substractedImage = NewImage.createByteImage("substractedImage", assembled1.getWidth(), assembled1.getHeight(), 1, NewImage.FILL_BLACK);
        ImageProcessor ipSub1 = substractedImage.getProcessor();
        ipSub1.copyBits(assembled1.getProcessor(), 0, 0, Blitter.ADD);
        ipSub1.copyBits(assembled2.getProcessor(), 0, 0, Blitter.SUBTRACT);
 
        getRegions();
        // 'do' press a page button for "Previous"
        pageButtonActionPerformed( new ActionEvent( parentFrame, 7, "Previous"));
    }
    

    public void initComponents() {
        popupMenu = new JPopupMenu();
        summaryLabel = new JLabel();
        countLabel = new JLabel("");
        pageLabel = new JLabel("");
        inspectDialog=new JDialog( parentFrame, "inspect: " + folder1, false );
        inspectDialog.setTitle("inspect: " + folder1 );
        previousPageButton = new JButton("Previous");
        nextPageButton = new JButton("Next");
        previousPageButton.setActionCommand("Previous");
        nextPageButton.setActionCommand("Next");
        saveButton = new JButton("Save");
        viewPlateButton = new JButton( "View Plate" );
        operationPanel = new JPanel(new GridBagLayout());
        buttonPanel = new JPanel(new GridBagLayout());
        panel = new JPanel(new GridBagLayout());

        eachButton = new JButton[9];
        for (int i = 0; i < 9; i++) {
            eachButton[i] = new JButton();
            eachButton[i].setHorizontalTextPosition(SwingConstants.CENTER);
            eachButton[i].setVerticalTextPosition(SwingConstants.BOTTOM);
            eachButton[i].setMinimumSize(new Dimension(buttonSize, buttonSize));
            eachButton[i].setPreferredSize(new Dimension(buttonSize, buttonSize));
            eachButton[i].setActionCommand("" + i); //set the tag
            eachButton[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    eachButtonActionPerformed(evt);
                }
            });

            eachButton[ i].addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent mouseEvent) {
                    mouseClickedOnEachButton(mouseEvent);
                }
            });
        }
        final int INST = 3;
        buttonPanel.add(eachButton[0], new GBC(0, 0).setInsets(INST, INST, INST, INST).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        buttonPanel.add(eachButton[1], new GBC(1, 0).setInsets(INST, INST, INST, INST).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        buttonPanel.add(eachButton[2], new GBC(2, 0).setInsets(INST, INST, INST, INST).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        buttonPanel.add(eachButton[3], new GBC(0, 1).setInsets(INST, INST, INST, INST).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        buttonPanel.add(eachButton[4], new GBC(1, 1).setInsets(INST, INST, INST, INST).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        buttonPanel.add(eachButton[5], new GBC(2, 1).setInsets(INST, INST, INST, INST).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        buttonPanel.add(eachButton[6], new GBC(0, 2).setInsets(INST, INST, INST, INST).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        buttonPanel.add(eachButton[7], new GBC(1, 2).setInsets(INST, INST, INST, INST).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        buttonPanel.add(eachButton[8], new GBC(2, 2).setInsets(INST, INST, INST, INST).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));

        operationPanel.add(previousPageButton, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        operationPanel.add(nextPageButton, new GBC(0, 1).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        operationPanel.add(pageLabel, new GBC(0, 2).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.CENTER));
        operationPanel.add(saveButton, new GBC(0, 3).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        operationPanel.add(viewPlateButton, new GBC(0, 4).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        operationPanel.add(countLabel, new GBC(0, 5).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        

        panel.add(buttonPanel, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        panel.add(operationPanel, new GBC(1, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        panel.add(summaryLabel, new GBC(0, 2).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        inspectDialog.add( panel );

        currentPage = 0;
        
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            	viewPlate( false );
            	saveInspectionResults( true );
            }
        });
        saveButton.setMnemonic( KeyEvent.VK_S );
        
        previousPageButton.setMnemonic( KeyEvent.VK_P );
        previousPageButton.addActionListener( new ActionListener() 
        {
            public void actionPerformed(ActionEvent evt) {
                pageButtonActionPerformed(evt);
            }
        });

        nextPageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                pageButtonActionPerformed(evt);
            }
        });
        nextPageButton.setMnemonic( KeyEvent.VK_N );

        viewPlateButton.addActionListener( new ActionListener() {
        	public void actionPerformed( ActionEvent actionEvent ) {
        		viewPlate( true );
        	}
        });
        viewPlateButton.setMnemonic( KeyEvent.VK_V );
        //pop menu
        JMenuItem menuItem = new JMenuItem("0 Alive");
        menuItem.setActionCommand("000");
        
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                menuItemActionPerformed(actionEvent);
            }
        });
        popupMenu.setBorderPainted(true);
        popupMenu.add(menuItem);
        popupMenu.addSeparator();
        menuItem = new JMenuItem("1 Alive");
        menuItem.setActionCommand("100");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                menuItemActionPerformed(actionEvent);
            }
        });
        popupMenu.add(menuItem);
        menuItem = new JMenuItem("2 Alive");
        menuItem.setActionCommand("200");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                menuItemActionPerformed(actionEvent);
            }
        });
        popupMenu.add(menuItem);
        menuItem = new JMenuItem("3 Alive");
        menuItem.setActionCommand("300");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                menuItemActionPerformed(actionEvent);
            }
        });
        popupMenu.add(menuItem);
        menuItem = new JMenuItem("4 Alive");
        menuItem.setActionCommand("400");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                menuItemActionPerformed(actionEvent);
            }
        });
        popupMenu.add(menuItem);

        menuItem = new JMenuItem("5 Alive");
        menuItem.setActionCommand("500");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                menuItemActionPerformed(actionEvent);
            }
        });
        popupMenu.add(menuItem);

        menuItem = new JMenuItem("6 Alive");
        menuItem.setActionCommand("600");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                menuItemActionPerformed(actionEvent);
            }
        });
        popupMenu.add(menuItem);
    }

    
    private void pageButtonActionPerformed(ActionEvent actionEvent) {
        //System.out.println( "page-change button, currentPage:" + currentPage );
        if ("Previous".equals(actionEvent.getActionCommand()) == true) {
            currentPage--;
            if (currentPage < 0) {
                currentPage = 0;
            }; // if
        }; // if

        if ("Next".equals(actionEvent.getActionCommand()) == true) {
            currentPage++;
        }; // if
        if (((currentPage + 1) * 9) > wormsList.size()) {
            currentPage = wormsList.size() / 9;
        }; // if
        //System.out.println( "wormslist.size:" + wormsList.size() );
        //System.out.println( "current: " + currentPage );
        //System.out.println( "(int) total/9 = " + wormsList.size() + "/9 = " + ( wormsList.size() / 9 ) );
        int ceiling = (int) Math.ceil(wormsList.size() / 9.0);
        //System.out.println( "(ceiling) total/9 = " + ceiling );

        previousPageButton.setEnabled(currentPage != 0);
        nextPageButton.setEnabled((currentPage + 1) < ceiling);
        fillIcons();
    }

    
    private void eachButtonActionPerformed(ActionEvent actionEvent) {
        int buttonIndex = Integer.parseInt(actionEvent.getActionCommand());
        int i = currentPage * 9 + buttonIndex;
        WormInfo worm = wormsList.get(i);
        if (worm.deleted) {
            eachButton[buttonIndex].setText(worm.nLive + " Moving");
            worm.deleted = false;
        } else {
            eachButton[buttonIndex].setText("DELETED");
            worm.deleted = true;
        }
        updateCountLabel();
    }

    
    
    private void menuItemActionPerformed(ActionEvent actionEvent) {
        JMenuItem tmpMenuItem = (JMenuItem) actionEvent.getSource();
        JPopupMenu tmpPopupMenu = (JPopupMenu) tmpMenuItem.getParent();
        JButton clickedButton = (JButton) tmpPopupMenu.getInvoker();
        //System.out.println( "menu-item:" + actionEvent.getActionCommand() );
        //System.out.println( "button:" + clickedButton.getActionCommand() );
        //System.out.println( "currentPage:" + currentPage );
        int buttonIndex = Integer.parseInt(clickedButton.getActionCommand());
        int index = currentPage * 9 + buttonIndex;

        WormInfo worm = wormsList.get(index);

        if ("000".equals(actionEvent.getActionCommand()) == true) {
            worm.nLive = 0;
        }; // if
        if ("100".equals(actionEvent.getActionCommand()) == true) {
            worm.nLive = 1;
            worm.deleted=false;
        }; // if
        if ("200".equals(actionEvent.getActionCommand()) == true) {
            worm.nLive = 2;
            worm.deleted=false;
        }; // if
        if ("300".equals(actionEvent.getActionCommand()) == true) {
            worm.nLive = 3;
            worm.deleted=false;
        }; // if
        if ("400".equals(actionEvent.getActionCommand()) == true) {
            worm.nLive = 4;
            worm.deleted=false;
        }; // if
        if ("500".equals(actionEvent.getActionCommand()) == true) {
            worm.nLive = 5;
            worm.deleted=false;
        }; // if
        if ("600".equals(actionEvent.getActionCommand()) == true) {
            worm.nLive = 6;
            worm.deleted=false;
        }; // if
        
        eachButton[ buttonIndex].setText(worm.nLive + " Moving");
        updateCountLabel();
        //System.out.println(wormsList.get(index).nLive);
    }; // menuItemActionPerformed
    
    	
    private void mouseClickedOnEachButton(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
            popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
        }; // if
    }; // mouseClickedOnEachButton
        
    
    /**
     * Processes the differential image, sub1_bw is created 
     */
    public void processDiffImage() {
        //binarize the differential image
        sub1_bw = substractedImage.duplicate();
        for (int i = 0; i < sub1_bw.getWidth(); i++) {
            for (int j = 0; j < sub1_bw.getHeight(); j++) {
                if (substractedImage.getProcessor().getPixelValue(i, j) >= (255 * dif_level)) {  //thresholding
                    sub1_bw.getProcessor().putPixel(i, j, 255);
                } else {
                    sub1_bw.getProcessor().putPixel(i, j, 0);
                }
            }
        }
        imClearBorder.imclearborder(sub1_bw);
        //Label and measure
        sq_sub = new SequentialLabeling(sub1_bw.getProcessor());
        sq_sub.applyLabeling();
        sq_sub.collectRegions();
        //for collecting selected image

        List<BinaryRegion> list = sq_sub.regions;  // the infomation of all binary regions is kept in list
        Iterator<BinaryRegion> brIterator = list.iterator(); 
        while (brIterator.hasNext()) {
            BinaryRegion br = (BinaryRegion) brIterator.next();
            double area = br.getSize();
            if (area > difSize && area< maxSegArea) {  //filtering according to area
                autoCount++;
            } else { //clear small particles from the binary image
                Rectangle rec = br.getBoundingBox();
                int label = br.getLabel();
                int x = rec.x;
                int y = rec.y;
                int height = rec.height;
                int width = rec.width;
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        if (sq_sub.labels[(y + j) * sub1_bw.getWidth() + x + i] == label) //excluding other objects in the same bounding box
                        {
                            sub1_bw.getProcessor().putPixel(x + i, y + j, 0);
                        }
                    }
                }
            }
        }//end of while
    }

    
    /**
     * Updates the count-label 
     */
    protected void updateCountLabel() {
    	int totalLiveWorms = 0;
        for (int i = 0; i < wormsList.size(); i++) {
            WormInfo worm = wormsList.get(i);
            if (!worm.deleted) {
            	totalLiveWorms += worm.nLive;
            }
        }
        countLabel.setText(totalLiveWorms + " moving worms");
    }
    
    
    public void fillIcons() {
        int ceiling = (int) Math.ceil(wormsList.size() / 9.0);
        summaryLabel.setText( folder1 + "                    Automatic Worm Count : " + autoCount );
        pageLabel.setText( "page " + (currentPage + 1) + " of " + ceiling );
        updateCountLabel();

        //System.out.println("current page:" + currentPage);
        int i = currentPage * 9;
        for (int buttonIndex = 0; buttonIndex < eachButton.length; buttonIndex++) {
            if (i < wormsList.size()) {
                WormInfo worm = wormsList.get( i );
                eachButton[ buttonIndex].setEnabled(true);
                eachButton[ buttonIndex].setText("");
                eachButton[ buttonIndex].setIcon( wormColorIconList.get( i ) );
                //System.out.println("worm index: " + i + " , alive: " + worm.nLive );
                eachButton[ buttonIndex].setPreferredSize(new Dimension(buttonSize, buttonSize));
                if (!worm.deleted) {
                    eachButton[buttonIndex].setText(worm.nLive + " moving"); //display number of living worms 
                } else {
                    eachButton[buttonIndex].setText("DELETED");
                }
                //eachButton[ buttonIndex ].setPreferredSize( new Dimension( worm.width, worm.height + 8 ) );
                eachButton[ buttonIndex].setMinimumSize(new Dimension(worm.width, worm.height + 8));
            } else {
                eachButton[ buttonIndex].setText("-----");
                eachButton[ buttonIndex].setIcon(null);
                eachButton[ buttonIndex].setPreferredSize(new Dimension(20, 20));
                eachButton[ buttonIndex].setEnabled(false);
            }; // if
            eachButton[ buttonIndex].setMargin(new Insets(2, 2, 2, 2));
            eachButton[ buttonIndex].invalidate();

            i++;
        }; // for     

        inspectDialog.validate();
        inspectDialog.repaint();
        inspectDialog.pack();
        inspectDialog.setVisible(true);
    }

    
    /**
     * Processes image2
     */
    public void processImage2() {
        assembled2_bw = assembled2.duplicate();
        for (int i = 0; i < assembled2_bw.getWidth(); i++) {
            for (int j = 0; j < assembled2_bw.getHeight(); j++) {
                if (assembled2.getProcessor().getPixelValue(i, j) <= (255 * single_level)) {  //thresholding
                    assembled2_bw.getProcessor().putPixel(i, j, 255);
                } else {
                    assembled2_bw.getProcessor().putPixel(i, j, 0);
                }
            }
        }
        //ImageWindow iw = new ImageWindow(assembled2_bw);
        //iw.show();
        imClearBorder.imclearborder(assembled2_bw);
        //Label and measure
        sq = new SequentialLabeling(assembled2_bw.getProcessor());
        sq.applyLabeling();
        sq.collectRegions();
        //for collecting selected image
        roiList = new LinkedList<BinaryRegion>();

        List<BinaryRegion> list2 = sq.regions;  // the infomation of all binary regions is kept in list
        Iterator<BinaryRegion> brIterator2 = list2.iterator(); //iterating the list
        while (brIterator2.hasNext()) {
            BinaryRegion br = (BinaryRegion) brIterator2.next();
            double area = br.getSize();
            if (area > minSegArea && area < maxSegArea) {  //filtering according to area
                roiList.add(br);
            }
        }//end of while
    }

    
    /**
     * Populates the 'wormsList' containing identified worms 
     */
    public void getRegions() {
        processDiffImage();
        //binarize image 2
        processImage2();
        wormsList = new ArrayList<WormInfo>();
        // two cases, text file with worms exists or it does not
        boolean readFromFileFlag = false;
		String directory = folder2;
		if( directory.endsWith( File.separator ) == false ) {
			directory += File.separator;
		}; // if
		// see if there is a results file already
		File resultsFile = new File( directory + N_LIVE_RESULTS_TXT );
		if( resultsFile.exists() == true ) {
			// case1: read worms from text file
			List<String> linesList = null;
			try {
				BufferedReader bufferedReader = new BufferedReader( new FileReader( resultsFile ) );
				String line = null;
				if( bufferedReader.ready() ) {
					while( ( line = bufferedReader.readLine() ) != null ) {
						if( linesList != null ) {
							linesList.add( line );
						}; // if
						if( WORM_DETAILS.equalsIgnoreCase( line ) == true ) {
							linesList = new ArrayList<String>();
						}; // if
					}; // while
				}
				else {
					JOptionPane.showMessageDialog( inspectDialog, "Unable to read " + N_LIVE_RESULTS_TXT + " file, please try again!", "ERROR", JOptionPane.ERROR_MESSAGE );
					return;
				}; // if
				bufferedReader.close();
			}
			catch( FileNotFoundException fnfe ) {
				JOptionPane.showMessageDialog( inspectDialog, "File not found: " + resultsFile.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE );
				return;
			}
			catch( IOException ioe ) {
				JOptionPane.showMessageDialog( inspectDialog, "File input/output error with file: " + resultsFile.getAbsolutePath(), "Eror", JOptionPane.ERROR_MESSAGE );
				return;
			}; // try
			int count = 0;
			for( String each : linesList ) {
				if( each.startsWith( "#" ) == true ) {
					continue;
				}; // if
				String[] pieces = each.split( "\t" );
				if( pieces.length != 6 ) {
					System.out.println( "ignoring line " + each );
					continue;
				}; // if
				WormInfo worm = new WormInfo();
				worm.nLive = new Integer( pieces[ 0 ] );
				worm.pX = new Integer( pieces[ 1 ] );
				worm.pY= new Integer( pieces[ 2 ] );
				worm.width = new Integer( pieces[ 3 ] );
				worm.height = new Integer( pieces[ 4 ] );
				worm.label = new Integer( pieces[ 5 ] );
				wormsList.add( worm );
				count += worm.nLive;
			}; // for
			System.out.println( "read " + count + " moving-worms from text file " + N_LIVE_RESULTS_TXT );
			readFromFileFlag = true;
		}; // if
                
        if( readFromFileFlag == false ) {
            Iterator<BinaryRegion> roiIterator = roiList.iterator(); 
            while (roiIterator.hasNext()) {
                BinaryRegion roi = (BinaryRegion) roiIterator.next();
                Rectangle rec = roi.getBoundingBox();
                WormInfo info = new WormInfo();
                info.pX = croppedX + rec.x;
                info.pY = croppedY + rec.y;
                info.width = rec.width;
                info.height = rec.height;
                info.nLive = 0;
                info.label = roi.getLabel();
                wormsList.add(info);
            }; // while
            System.out.println(wormsList.size() + " worms were identified from image");
        }; // if
        wormColorIconList = new ArrayList<ImageIcon>();
        for( WormInfo worm : wormsList ) {
        	wormColorIconList.add( getImageIconDetail( worm ) );
        }; // for
    }; // getRegions

    
    /**
     * Assembles image from an image in specified folder but if already exists, then it just reads it from disk
     * @param  folder  the name of the folder
     * @return  the image object
     */
    public ImagePlus assembleImage( String folder ) {
        //System.out.println("assembleImage()");
        long time1 = System.currentTimeMillis();
        File det = new File( folder + File.separator + "assembled.jpeg" );
        if (det.exists()) {
            ImagePlus assembled = new ImagePlus( folder + File.separator + "assembled.jpeg" );
            System.out.println("loaded assembled image!");
            return assembled;
        }
        ImagePlus assembled = NewImage.createByteImage("assembled", pieceX * picSizeX, pieceY * picSizeY, 1, NewImage.FILL_BLACK);
        ImageProcessor ipAssembled = assembled.getProcessor();
        //System.out.println("Directory: " + folder);
        for (int i = 1; i <= pieceX; i++) {
            for (int j = 1; j <= pieceY; j++) {
                String st = String.valueOf(i + pieceX * (j - 1));
                String path = folder + File.separator + "piece_" + st + ".jpeg";
                ImagePlus tempIP = new ImagePlus(path.toString());
                ipAssembled.copyBits(tempIP.getProcessor(), picSizeX * (i - 1), picSizeY * (j - 1), Blitter.ADD);
            }
        }
        FileSaver saver = new FileSaver(assembled);
        saver.saveAsJpeg(folder + File.separator + "assembled.jpeg");
        long time2 = System.currentTimeMillis();
        System.out.println( "Time for assembling: " + (time2 - time1) + " milliseconds" );
        return assembled;
        //iw.show();
    }
  
    
    /**
     * Get the color-icon of a worm, DEPRECATED. This is now old code, use instead method getImageIconDetail
     * @deprecated (use getImageIconDetail method instead)
     * @param  worm  the worm
     * @return  image-icon that is usable inside a button
     */
    public ImageIcon getColorIcon(WormInfo worm) {
        int x = worm.pX;
        int y = worm.pY;
        int height = worm.height;
        int width = worm.width;
        int label = worm.label;

        ImagePlus previewImagePlus = NewImage.createRGBImage("previewImagePlus", width + 2 * ICON_PADDING, height + 2 * ICON_PADDING, 1, NewImage.RGB);
        boolean flag=false;
        for (int i = 0; i < width + 2 * ICON_PADDING; i++) {
            for (int j = 0; j < height + 2 * ICON_PADDING; j++) {
                int[] rgb = new int[3];
                if (assembled2_bw.getPixel(x - ICON_PADDING + i, y - ICON_PADDING + j)[0] == 255 && sq.labels[(y - ICON_PADDING + j) * substractedImage.getWidth() + x + i - ICON_PADDING] == label) {
                    rgb[0] = sub1_bw.getPixel(x - ICON_PADDING + i, y - ICON_PADDING + j)[0] + assembled2.getPixel(x - ICON_PADDING + i, y - ICON_PADDING + j)[0];
                    if(sub1_bw.getPixel(x - ICON_PADDING + i, y - ICON_PADDING + j)[0]==255){
                    flag=true;}
                } else {
                    rgb[0] = assembled2.getPixel(x - ICON_PADDING + i, y - ICON_PADDING + j)[0];
                }
                if (rgb[0] > 255) {
                    rgb[0] = 255;
                }
                rgb[1] = assembled2.getPixel(x - ICON_PADDING + i, y - ICON_PADDING + j)[0];
                rgb[2] = assembled2.getPixel(x - ICON_PADDING + i, y - ICON_PADDING + j)[0];
                previewImagePlus.getProcessor().putPixel(i, j, rgb);
            }; // for
        }; // for
        if(worm.firstView){
            if(!flag){
                worm.nLive=0;
            }
            else {
                worm.nLive=1;
            }
            worm.firstView=false;
        }
        return new ImageIcon(previewImagePlus.getBufferedImage());
    }; // getImageIcon    

    
    /**
     * @param  worm  the worm
     * @return  image-icon that is usable inside a button
     */
    public ImageIcon getImageIconDetail(WormInfo worm) {
        int x = worm.pX;
        int y = worm.pY;
        int height = worm.height;
        int width = worm.width;
        int label = worm.label;

        ImagePlus previewImagePlus = NewImage.createByteImage("previewImageIconDetail", width * 2 + 2, height * 2 + 2, 1, NewImage.GRAY8);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                previewImagePlus.getProcessor().putPixel(i, j, assembled1.getProcessor().getPixel(x + i, y + j));
            }
        }
        for (int i = 0; i < width; i++) {
            for (int j = height + 2; j < height * 2 + 2; j++) {
                previewImagePlus.getProcessor().putPixel(i, j, assembled2.getProcessor().getPixel(x + i, y + j - height));
            }
        }

        for (int i = width + 2; i < width * 2 + 2; i++) {
            for (int j = 0; j < height; j++) {
                previewImagePlus.getProcessor().putPixel(i, j, substractedImage.getProcessor().getPixel(x + i - width - 2, y + j) * 5);
            }
        }

		ImageProcessor imageProcessor = previewImagePlus.getProcessor();
		TypeConverter typeConverter = new TypeConverter( imageProcessor, false );
		ColorProcessor colorProcessor = (ColorProcessor) typeConverter.convertToRGB();

        boolean flag = false;
        for (int i = 0; i < width; i++ ) {
            for (int j = 0; j < height; j++) {
                int[] rgb = new int[ 3 ];
                if( assembled2_bw.getPixel(x + i, y + j)[0] == 255 && sq.labels[(y + j) * substractedImage.getWidth() + x + i ] == label ) {
                    rgb[ 0 ] = sub1_bw.getPixel(x + i, y + j)[0] + assembled2.getPixel(x + i, y + j)[0];
                    if(sub1_bw.getPixel(x + i, y + j)[0]==255) {
                    	flag=true;
                    }
                } else {
                    rgb[0] = assembled2.getPixel(x + i, y + j)[0];
                }
                if (rgb[0] > 255) {
                    rgb[0] = 255;
                }
                rgb[1] = assembled2.getPixel(x + i, y + j)[0];
                rgb[2] = assembled2.getPixel(x + i, y + j)[0];
                colorProcessor.putPixel( i + width + 2, j + height + 2, rgb);
            }; // for
        }; // for
        if(worm.firstView){
            if(!flag){
                worm.nLive=0;
            }
            else {
                worm.nLive=1;
            }
            worm.firstView=false;
        }
   
		colorProcessor.setValue( Color.BLUE.getRGB() );
		colorProcessor.drawLine(0, height, width * 2 + 2, height );
		colorProcessor.drawLine(0, height + 1, width * 2 + 2, height + 1 );
		colorProcessor.drawLine( width, 0, width, height * 2 + 1);
		colorProcessor.drawLine( width + 1, 0, width + 1, height * 2 + 1);
		ImagePlus imagePlus = new ImagePlus( "color", colorProcessor );
        return new ImageIcon( imagePlus.getBufferedImage() );
    }

    
	/**
	 * displays the plate with colorful annotations on worms
	 * @param  displayFlag  when true, it shows the image
	 */
	protected void viewPlate( boolean displayFlag ) {
		String directory = folder2;
		ImagePlus assembled = assembled2;
		if( directory.endsWith( File.separator ) == false ) {
			directory += File.separator;
		}; // if
		
		// crate the image
		ImageProcessor imageProcessor = assembled.getProcessor();
		TypeConverter typeConverter = new TypeConverter( imageProcessor, false );
		ColorProcessor colorProcessor = (ColorProcessor) typeConverter.convertToRGB();
		final int oneColor = Color.BLUE.getRGB();
		final int strokeWidth = 8;
		for( WormInfo worm : wormsList ) {
			if( worm.deleted == true ) {
				continue;
			}; // if
			int number = worm.nLive;
			int color = oneColor;
			Roi roi = new Roi( worm.pX - strokeWidth, worm.pY - strokeWidth, worm.width + strokeWidth + 6, worm.height + strokeWidth + 6 );
			colorProcessor.setRoi( roi );
			roi.setStrokeWidth( strokeWidth );
			colorProcessor.setValue( color );
			roi.drawPixels( colorProcessor );
			int y = worm.pY - strokeWidth - 2;
			colorProcessor.drawString( "" + number, worm.pX - strokeWidth - 2, y );
		}; // for
		
		ImagePlus imagePlus = new ImagePlus( directory, colorProcessor );
		if( displayFlag == true ) {
			imagePlus.show();
		}; // if
		FileSaver fileSaver = new FileSaver( imagePlus );
		fileSaver.saveAsJpeg( directory + "assembled_colors.jpeg" );
	}; // viewPlate
	
	
	/**
	 * Saves the inspection results
	 * @param  humanInspectionFlag  whether it was inspected by human
	 */
	protected void saveInspectionResults( boolean humanInspectionFlag ) {
		String folder = folder2; 
		if( folder == null ) {
			JOptionPane.showMessageDialog( inspectDialog, "Warning, unable to save results, folder is null!", "Unable to save results!", JOptionPane.ERROR_MESSAGE );
			return;
		}; // if
		if( folder.endsWith( File.separator ) == false ) {
			folder += File.separator;
		}; // if
		// get the info to be saved into a list
		List<String> linesList = new ArrayList<String>();
    	int totalLiveWorms = 0;
		for( WormInfo worm : wormsList ) {
			if( worm.deleted == false ) {
				linesList.add( worm.toString() );
		    	totalLiveWorms += worm.nLive;
			}; // if
		}; // for
		
		// see if there is historical file, and if so, which number
		File historicalFile = null;
		int number = 0;
		do {
			number++;
			String filename = folder + HISTORICAL + number + "." + N_LIVE_RESULTS_TXT;
			historicalFile = new File( filename );
		} while( historicalFile.exists() );

		// see if there is a results file already
		File resultsFile = new File( folder + N_LIVE_RESULTS_TXT );
		if( resultsFile.exists() == true ) {
			File oldResultsFile = new File( folder + N_LIVE_RESULTS_TXT );
			boolean renamedFlag = oldResultsFile.renameTo( historicalFile );
			//out.println( "renamed ? " + renamedFlag );
			if( renamedFlag == false ) {
				JOptionPane.showMessageDialog( inspectDialog, "Error, unable to rename file " + N_LIVE_RESULTS_TXT + " to a historical filename.", "Cannot save!", JOptionPane.ERROR_MESSAGE );
				return;
			}; // if
		}; // if
		
		// save the new contents
		try {
			FileWriter fileWriter = new FileWriter( folder + N_LIVE_RESULTS_TXT );
			BufferedWriter bufferedWriter = new BufferedWriter( fileWriter );
			PrintWriter printWriter = new PrintWriter( bufferedWriter );
			printWriter.println( "totalLiveWorms=" + totalLiveWorms );
			if( humanInspectionFlag == true ) {
				printWriter.println( INSPECTED_BY_HUMAN );
			}; // if
			printWriter.println( "#Date: " + new Date() );
			printWriter.println( "#seetings-used-when-inspected:" );
			printWriter.println( "single_level=" + single_level );
			printWriter.println( "minSegArea=" + minSegArea );
			printWriter.println( "maxSegArea=" + maxSegArea );
			printWriter.println( "dif_level=" + dif_level );
			printWriter.println( "difSize=" + difSize );
			printWriter.println( WORM_DETAILS );
			printWriter.println( WormInfo.HEADER );
			for( String line : linesList ) {
				printWriter.println( line );
			}; // for
			printWriter.close();
		}
		catch( IOException ioe ) {
			ioe.printStackTrace();
			JOptionPane.showMessageDialog( inspectDialog, "Error when saving " + N_LIVE_RESULTS_TXT + " as follows:<br>" + ioe, "I/O Error", JOptionPane.ERROR_MESSAGE );
			return;
		}; // try
	}; // saveInspectionResults
}


class WormInfo {
    public int nLive;
    public int pX;
    public int pY;
    public int width;
    public int height;
    public boolean deleted;
    public int label;
    public boolean firstView;
    
    public static final String HEADER = "#nLive\tX\tY\tWidth\tHeight\tLabel";

    /** Default constructor */
    public WormInfo() {
        this.nLive = 0;
        this.pX = 0;
        this.pY = 0;
        this.width = 0;
        this.height = 0;
        this.deleted = false;
        this.label = -1;
        this.firstView=true;
    }
    
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return nLive + "\t" + pX + "\t" + pY + "\t" + width + "\t" + height + "\t" + label;
	}
	
}



