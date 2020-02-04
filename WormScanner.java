import gnu.io.CommPortIdentifier;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import camera.Camera;
import camera.CameraException;
import camera.util.CameraInfo;
import config.CameraConfiguration;
import config.ProgramConfiguration;
import config.StageConfiguration;
import stage.Stage;
import stage.MotorizedStageIdentifier;
import stage.MovementListener;
import util.ProgramInfo;
import util.units.Steps;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.process.Blitter;
import ij.process.ImageProcessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.GraphicsEnvironment;

import javax.imageio.ImageIO;
import javax.swing.border.LineBorder;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.sf.jlibdc1394.gui.InternalFrameCamSettings;
import net.sf.jlibdc1394.impl.cmu.JDC1394CamPortCMU;
import net.sf.jlibdc1394.JDC1394Cam;
import net.sf.jlibdc1394.JDC1394CamPort;

/**
 *
 * Works on 6cm, 10cm, 6-well plates.
 *
 */

public class WormScanner implements MovementListener {
	
	//The gridSelector
	private static GridSelector gridSelector;

	// The camera providing the input display.
	private Camera camera;
	
	// The motorized stage for worm tracking.
	private Stage stage = null;

	// The counter for image sufix
	private int imageCounter;

	// The number of steps per pixel on the X axis
	private double stepsPerPixelsX;

	// The number of steps per pixel on the Y axis
	private double stepsPerPixelsY;

	// The Y home position
	private final int homeY = 0;

	// the X scan-ready position
	private int xScanReady;

	// the Y scan-ready position
	private int yScanReady;

	// the current plate, null if none
	private String plate;

	// The directory where the images are to be placed
	private String directory;

	// The current configuration file name, or null if error when loading one
	private String currentConfigurationFilename;

	// Number of pictures needed horizontally for current plate and configuration
	private int timesHorizontally;

	// Number of pictures needed vertically for current plate and configuration
	private int timesVertically;

	// Remember errors, if null then no errors detected currently
	private String errors;

	// the start time of scanning
	private long startTime;

	// the internal frame controls
	public InternalFrameCamSettings frameCamControls;

	/** The default X resolution */
	public static final int DEFAULT_CAMERA_X_RESOLUTION = 640;

	/** The default Y resolution */
	public static final int DEFAULT_CAMERA_Y_RESOLUTION = 480;

	/** The default resolution. */
	public static final Dimension DEFAULT_CAMERA_RESOLUTION = new Dimension( DEFAULT_CAMERA_X_RESOLUTION, DEFAULT_CAMERA_Y_RESOLUTION );

	/** The location of the default configuration file */
	public static String DEFAULT_CONFIGURATION_FILENAME = "settings" + File.separator + "worm_08x.cfg";

	/** The location of the properties file */
	public static String PROPERTIES_FILENAME = "settings" + File.separator + "configuration.properties";

	/** constant for 6cm plate action as well as label for visual component (such as radiobutton) */
	public static final String PLATE_SIX = "6 cm";

	/** constant for 10cm plate action as well as label for visual component (such as radiobutton) */
	public static final String PLATE_NINE = "10 cm";

	/** constant for 6-well plate action as well as label for visual component (such as radiobutton) */
	public static final String PLATE_SIXWELL = "6-well";

	/** constant for preferred directory when no absolute path was specified */
	public static final String PREFERRED_DIRECTORY = File.separator + "scanned" + File.separator;

	/** Default constructor */
	public WormScanner() {
		imageCounter = 0;
		timesHorizontally = 0;
		timesVertically = 0;
		plate = null;
		directory = null;
		currentConfigurationFilename = null;
		errors = null;
		startTime = 0;

		// load the settings
		Properties properties = new Properties();
		try {
			FileInputStream fileInputStream = new FileInputStream( PROPERTIES_FILENAME );
			properties.load( fileInputStream );
			fileInputStream.close();
		}
		catch( FileNotFoundException fnfe ) {
			errors = "Could not find the settings file (" + PROPERTIES_FILENAME + ")";
			return;
		}
		catch( IOException ioe ) {
			errors = "Problem when reading the settings file (" + PROPERTIES_FILENAME + ") , the exception is: " + ioe;
			return;
		}; // try

		// load the last plate used
		String tmpPlate = properties.getProperty( "plate", null );
		// set the value of the plate only when a valid name was in the plate property
		plate = null;
		plate = PLATE_SIX.equals( tmpPlate ) == true     ? PLATE_SIX     : plate;
		plate = PLATE_NINE.equals( tmpPlate ) == true    ? PLATE_NINE    : plate;
		plate = PLATE_SIXWELL.equals( tmpPlate ) == true ? PLATE_SIXWELL : plate;

		// loads camera configuration and sets the camera
		String configurationFilename = properties.getProperty( "last_configuration_utilized", DEFAULT_CONFIGURATION_FILENAME );
		errors = loadCameraConfiguration( configurationFilename );
		if( errors != null ) {
			System.out.println( "error, " + errors );
			return;
		}; // if
		File file = new File( configurationFilename );
		currentConfigurationFilename = file.getAbsolutePath();
		
		// Initialize the camera, if it is null
		if( camera == null ) {
			System.out.println( "Initializing the first available camera." );
			System.out.println( "The preferred camera was not initialized as expected." );
			String[] cameras = CameraInfo.getConnectedCameras();
			try {
				if( cameras != null && cameras.length > 0 ) {
					camera = new Camera( cameras[ 0 ] );
					// Set the default camera resolution.
					for( Dimension resolution : camera.getResolutions() ) {
						if( resolution.equals(DEFAULT_CAMERA_RESOLUTION) ) {
							camera.setResolution( resolution );
						}; // if
					}; // for
				}
			} 
			catch( camera.CameraException e ) {
				errors = "Camera Error - camera could not be connected (error is: " + e + ")";
				return;
			}; // try
		}
		else {
			// set up camera settings for 1394 camera
			boolean reconectCMU1394 = false;
			if( CameraInfo.CMU1394.equals( camera.getCamera() ) == true ) {
				camera.disconnect();
				reconectCMU1394 = true;
			}; // if
			JDC1394Cam cam = null;
			JDC1394CamPort port = new JDC1394CamPortCMU();
			try {
				port.checkLink();
				cam = port.selectCamera( 0 );
			}
			catch( Exception e ) {
				e.printStackTrace();
			}; // try
			if( cam != null ) {
				frameCamControls = new InternalFrameCamSettings( cam );
				frameCamControls.setVisible( true );
			}; // if

			if( reconectCMU1394 == true ) {
				try {
					camera.setCamera( CameraInfo.CMU1394 );
				}
				catch( Exception e ) {
					e.printStackTrace();
				}; // try
			}; // if
		}; // if

		if( camera == null ) {
			errors = "Error: Camera could not initialize!"; 
			return;
		}; // if

		// Initialize the stage
		if( stage == null ) {
			stage = new Stage();
			stage.addMovementListener( this );
		}; // if
		if( stage == null ) {
			errors = "Error: Stage could not be initialized !";
			return;
		}; // if

		stepsPerPixelsX = 0;
		stepsPerPixelsY = 0;

		boolean errorFlag = loadConfiguration( configurationFilename );
		if( errorFlag == true ) {
			// the variable 'errors' would be set already inside of loadConfiguration
			System.out.println( "Error, " + errors );
			return;
		}; // if

		updateXScanReady( PLATE_SIXWELL.equals( plate ) ? 1 : 0 );
		updateYScanReady( PLATE_SIXWELL.equals( plate ) ? 1 : 0 );

		if( stage != null && stage.isConnected() ) {
			try {
				stage.moveTo( xScanReady, yScanReady, false, 20000 );
			}
			catch( Exception e ) {
				System.out.println( "\t" + e );
			}; // try
		}
		else {
			System.out.println( "Stage is not-connected" );
		}; // if

	}; // constructor


	/** Updates value of x-scan-ready coordinate 
	 * @param  the plate number (for 6-well plate), other cases use zero
	 */
	protected void updateXScanReady( int plateNumber ) {
		//-System.out.println( "update-x-ready, platenumber: " + plateNumber + " , on a " + plate );
		if( plateNumber == 0 ) {
			xScanReady = getHomeX() + (int) ( DEFAULT_CAMERA_X_RESOLUTION * ( timesHorizontally / 2.0 - 0.5 ) * getStepsPerPixelsX() );
			return;
		}; // if
		
		if( plateNumber > 6 ) {
			System.out.println( "Error, invalid plateNumber (" + plateNumber + ")" );
			return;
		}; // if

		// quick hack: positioning for focusing purposes
		if( plateNumber > 0 ) {
System.out.println( "Positioning for focusing on well: " + plateNumber );
			if( plateNumber > 3 ) {
				plateNumber = plateNumber - 3;
			}; // if
			xScanReady = getHomeX() - (int) ( ( DEFAULT_CAMERA_X_RESOLUTION / 2.0 ) * getStepsPerPixelsX() ) + 40000 * ( 3 - plateNumber );
		
			xScanReady = xScanReady - (int) ( ( getTimesHorizontally() / 2.0 - 1.0 ) * DEFAULT_CAMERA_X_RESOLUTION * getStepsPerPixelsX() );
			return;
		}; // if

		// continuation of the hack: scanning
		plateNumber = -plateNumber;
		if( plateNumber > 3 ) {
			plateNumber = plateNumber - 3;
		}; // if
		xScanReady = getHomeX() - (int) ( ( DEFAULT_CAMERA_X_RESOLUTION / 2.0 ) * getStepsPerPixelsX() ) + 40000 * ( 3 - plateNumber );
		
	}; // updateXScanReady


	/** Updates value of y-scan-ready coordinate
	 * @param  the plate number (for 6-well plate), other cases use zero
	 */
	protected void updateYScanReady( int plateNumber ) {
		//-System.out.println( "update-y-ready, platenumber: " + plateNumber + " , on a " + plate );
		if( plateNumber == 0 ) {
			yScanReady = homeY + (int) ( DEFAULT_CAMERA_Y_RESOLUTION * ( timesVertically / 2.0 - 0.5 ) * getStepsPerPixelsY() );
			return;
		}; // if
		
		// quick hack
		if( plateNumber > 6 ) {
			System.out.println( "Error, invalid plateNumber (" + plateNumber + ")" );
			return;
		}; // if

		if( plateNumber > 0 ) {
		yScanReady = homeY + (int) ( DEFAULT_CAMERA_Y_RESOLUTION * ( timesVertically / 1.0 - 0.5 ) * getStepsPerPixelsY() );
		if( plateNumber >= 4 && plateNumber <= 6 ) {
			yScanReady -= 40000;
		}; // if
	
		yScanReady = yScanReady - (int) ( ( getTimesVertically() / 2.0 - 0.5 ) * DEFAULT_CAMERA_Y_RESOLUTION * getStepsPerPixelsY() );
		return;
		}; // if

		// hack continues
		plateNumber = -plateNumber;
		yScanReady = homeY + (int) ( DEFAULT_CAMERA_Y_RESOLUTION * ( timesVertically / 1.0 - 0.5 ) * getStepsPerPixelsY() );
		if( plateNumber >= 4 && plateNumber <= 6 ) {
			yScanReady -= 40000;
		}; // if
	
	}; // updateYScanReady

	/** 
	 * Get the X for scan-ready position
	 * @param  the plate number (for 6-well plate), other cases use zero
	 * @return  the x scan-ready position
	 */
	public int getXScanReady( int plateNumber ) { 
		if( plateNumber == 0 ) {
			return xScanReady; 
		}; // if
		updateXScanReady( plateNumber );
		return xScanReady; 
	}; // getXScanReady


	/** 
	 * Get the Y for scan-ready position
	 * @param  the plate number (for 6-well plate), other cases use zero
	 * @return  the y scan-ready position
	 */
	public int getYScanReady( int plateNumber ) { 
		if( plateNumber == 0 ) {
			return yScanReady; 
		}; // if
		updateYScanReady( plateNumber );
		return yScanReady;
	}; // getYScanReady


	/**
	 * Get the home X value for the current plate
	 * @return  the home x value
	 */
	public int getHomeX() {
		if( PLATE_SIXWELL.equals( plate ) == true ) {
			return -20000;
		};
		return 0;
	}; // getHomeX


	/** 
	 * Loads configuration and connects the stage
	 * @param  configurationFilename  the configuration filename
	 * @return  false if things are ok; 
	 *          otherwise it returns true and 'errors' will contain the error
	 */
	public boolean loadConfiguration( String configurationFilename ) {
		// Open configuraton , errors should not be fatal here
		File configurationFile = new File( configurationFilename );
		return loadConfiguration( configurationFile );
	}; // loadConfiguration


	/** 
	 * Loads configuration and connects the stage
	 * @param  configurationFile  the configuration file (java.io.File)
	 * @return  false if things are ok;
	 *          otherwise it returns true and 'errors' will contain the error
	 */
	public boolean loadConfiguration( File configurationFile ) {
		SubnodeConfiguration node = null;
		try {
				XMLConfiguration configuration = new XMLConfiguration( configurationFile );
				configuration.setThrowExceptionOnMissing( true );
				node = configuration.configurationAt( "worm-tracker" );
				if( node == null ) {
					errors = "Error, node is null, it happened when loading the configuration (" + configurationFile.getAbsolutePath() + ")";
					return true;
				}; // if

				ProgramConfiguration programConfiguration = new ProgramConfiguration( node );
				String version = programConfiguration.loadVersion();
				if( version == null ) {
					errors = "Error, configuration version is null";
					return true;
				}; // if
				if( ProgramInfo.VERSION_COMPARATOR.compare( version, ProgramInfo.VERSION ) < 0 ) {
					System.out.println( "Configuration Warning ... The loaded configuration is out of date. You are running " + ProgramInfo.ID + " and the configuration corresponds to version " + version + "!\n Please re-save your configuration." );
				}; // if
		
				StageConfiguration stageConfiguration = new StageConfiguration( node );

				// Load the x-axis steps/pixels.
				try {
					stepsPerPixelsX = stageConfiguration.loadStepsPerPixelsX();
				} 
				catch( Exception e ) { 
					errors = "Error when reading StepsPerPixelsX : " + e; 
					return true;
				}; // try

				// Load the y-axis steps/pixels.
				try {
					stepsPerPixelsY = stageConfiguration.loadStepsPerPixelsY();
				} 
				catch( Exception e ) { 
					errors = "Error when reading StepsPerPixelsY : " + e; 
					return true;
				}; // try

		 		// Load whether we are synchronizing (i.e., waiting on) stage responses.
				try {
					boolean isSync = stageConfiguration.loadSync();
					stage.setSync( isSync );
					//System.out.println( "stage, synchronizing, waiting on, stage responses: " + isSync );
				}
				catch( Exception e ) { 
					errors = "Error when reading Sync : " + e; 
					return true;
				}; // try

		 		// Load the timeout (in milliseconds) for synchronizing (i.e., waiting on) stage responses.
				try {
					long timeout = stageConfiguration.loadSyncTimeout();
					stage.setSyncTimeout( timeout );
					//System.out.println( "stage, syncTimeout : " + timeout );
				}
				catch( Exception e ) { 
					errors = "Error when reading SyncTimeout : " + e; 
					return true;
				}; // try

				// Load whether we are moving the stage to absolute locations (or by relative distances).
				try {
					boolean isMoveAbsolute = stageConfiguration.loadMoveAbsolute();
					if( isMoveAbsolute == true ) {
						stage.setMoveAbsolute( true );
					}
					//System.out.println( "stage, MoveAbsolute : " + isMoveAbsolute );
				}
				catch( Exception e ) { 
					errors = "Error when reading MoveAbsolute : " + e; 
					return true;
				}; // try

				// Load the stage type.
				MotorizedStageIdentifier id = stageConfiguration.loadType();
				if( id == null ) {
					errors = "Stage, error when loading the type (stage ID)";
					return true;
				}; // if
				//System.out.println( "stage, id = " + id );
				stage.setStageID( id );
				if( stage.isConnected() ) {
					//System.out.println( "connected stage" );
				}; // if

				// Load the stage port.
				CommPortIdentifier port = stageConfiguration.loadPort();
				if( port == null ) {
					errors = "Error when loading the Port";
					return true;
				}; // if

				//System.out.println( "stage, port : " + port );
				stage.setPort( port );
				if( stage.isConnected() ) {
					//System.out.println( "connected stage" );
				}; // if
		
				// Load the stage acceleration.
				//try {
				//	int acceleration = stageConfiguration.loadAcceleration();
				//	System.out.println( "stage, configuration-acceleration : " + acceleration );
				//	if( stage.isConnected() ) {
				//		stage.setAcceleration( acceleration );
				//		System.out.println( "stage, acceleration set " );
				//		System.out.println( "stage, acceleration: " + stage.getAcceleration( ) );
				//	}; // if
				//}
				//catch( Exception e ) { 
				//	errors = "Error when reading acceleration : " + e; 
				//	return true;
				//}; // try

				// Load the stage speed.
				//try {
				//	int speed = stageConfiguration.loadSpeed();
				//	System.out.println( "stage, configuration-speed : " + speed );
				//	if( stage.isConnected() ) {
				//		stage.setSpeed( speed );
				//		System.out.println( "stage, speed set " );
				//		stage.updateSpeed( 500000 );
				//	}; // if
				//	System.out.println( "stage, speed: " + stage.getSpeed() );
				//}
				//catch( Exception e ) { 
				//	errors = "Error when reading speed : " + e; 
				//	return true;
				//}; // try

		} 
		catch( Exception e ) {
			errors = "Configuration Error - The program configuration \""
						+ configurationFile.getAbsolutePath() + "\" cannot be loaded.";
			return true;
		}; // try

		updateScanningVariables();
		return false;
	}; // loadConfiguration

	
	/**
	 * Updates variables used for scanning such as number of horizontal/vertical pics
	 */
	public void updateScanningVariables() {
		if( plate == null ) {
			System.out.println( "Can't update scanning variables because we do not know which plate is being used, bye" );
			return;
		}; // if
		if( stepsPerPixelsX == 0 ) {
			System.out.println( "Can't update scanning variables because stepsPerPixelsX is zero" );
			return;
		}; // if
		if( stepsPerPixelsY == 0 ) {
			System.out.println( "Can't update scanning variables because stepsPerPixelsY is zero" );
			return;
		}; // if
//System.out.println( "----  " );
//System.out.println( "---- stepsPerPixelsX : " + stepsPerPixelsX );
//System.out.println( "---- stepsPerPixelsY : " + stepsPerPixelsY );
//System.out.println( "---- plate : " + plate );
		int wantedArea = 0;
		double area = 0;

		if( PLATE_SIX.equals( plate ) ) {
			wantedArea = 50500;
		}; // if

		if( PLATE_NINE.equals( plate ) ) {
			wantedArea = 90000;
		}; // if

		if( PLATE_SIXWELL.equals( plate ) ) {
			wantedArea = 40000;
		}; // if

		timesHorizontally = (int) Math.ceil( wantedArea / ( stepsPerPixelsX * DEFAULT_CAMERA_X_RESOLUTION ) );
		timesVertically = (int) Math.ceil( wantedArea / ( stepsPerPixelsY * DEFAULT_CAMERA_Y_RESOLUTION ) );
//System.out.println( "----  horizontally " + timesHorizontally );
//System.out.println( "----  vertically   " + timesVertically );
//System.out.println( "----  " );
		updateXScanReady( PLATE_SIXWELL.equals( plate ) ? 1 : 0 );
		updateYScanReady( PLATE_SIXWELL.equals( plate ) ? 1 : 0 );
	}; // updateScanningVariables


	/**
	 * Get the number of pics to obtain horizontally for current plate, configuration
	 * @return  number of pics to obtain horizontally
	 */
	public int getTimesHorizontally() {
		return timesHorizontally;
	}; // getTimesHorizontally


	/**
	 * Get the number of pics to obtain vertically for current plate, configuration
	 * @return  number of pics to obtain vertically
	 */
	public int getTimesVertically() {
		return timesVertically;
	}; // getTimesVertically


	/**
	 * Loads the camera configuration; initializes the Camera object, if possible as listed in the default configuration
	 * @param  filename  the configuration filename
	 * @return  error message if any, otherwise null means things are ok
	 */
	public String loadCameraConfiguration( String filename ) {
		File configurationFile = new File( filename );
		if( configurationFile.exists() == false ) {
			return "The configuration file does not exist (" + filename + ")";
		}; // if

		// Open the configuraton
		SubnodeConfiguration node = null;
		try {
				XMLConfiguration configuration = new XMLConfiguration( configurationFile );
				node = configuration.configurationAt( "worm-tracker" );
		} 
		catch( Exception e ) {
				return "Configuration Error - The program configuration \""
				+ configurationFile.getAbsolutePath() 
				+ "\" cannot be loaded (location: " + filename + " )";
		}; // try
			
		if( node == null ) {
			return "Error, could not find camera information in the configuration file (" + filename + ")";
		}; // if

		// Load the camera.
		try {
			CameraConfiguration cameraConfiguration = new CameraConfiguration( node );
			CameraConfiguration.Display displayConfiguration = cameraConfiguration.getDisplay();
			camera = new Camera( displayConfiguration.loadCameraId(),
							new Dimension( displayConfiguration.loadResolutionWidth(),
									displayConfiguration.loadResolutionHeight() ),
							displayConfiguration.loadFrameRate() );
		} 
		catch( Exception e ) {
			return "Error, problem when initializing the camera; this is likely to happen when the jlib jar file is not in the classpath (Exception is: " + e + ")";
		}; // try				

		return null;
	}; // loadCameraConfiguration


	/**
	 * Moves to HOME location (without taking a snapshot)
	 */
	public void moveHome() {
		if( stage == null ) {
			System.out.println( "Stage is null, leaving moveHome()" );
			return;
		}; // if
		try {
//			System.out.println( "--attempt to move to HOME, " + getHomeX() + " , " + homeY );
			imageCounter = -99;
			stage.moveTo( getHomeX(), homeY, true, 200000 );
		}
		catch( Exception e ) {
			e.printStackTrace();
		}; // try
	}; // moveHome


	/**
	 * Moves to x,y Scan-ready location (without taking a snapshot)
	 */
	public void moveToScanReady() {
		if( stage == null ) {
			System.out.println( "Stage is null, leaving moveToScanReady()" );
			return;
		}; // if
		try {
			imageCounter = -99;
			stage.moveTo( xScanReady, yScanReady, true, 800000 );
		}
		catch( Exception e ) {
			e.printStackTrace();
		}; // try
	}; // moveToScanReady()


	/**
	 * Closes the stage
	 */
	public void closeStage() {
		if( stage != null ) {
			//System.out.println( "Close the stage" );
			stage.close();
		}; // if
	}; // closeStage


	/**
	 * This method gets called after a 'move' ; we use it to take a snapshot
	 * @param  movedBy  the moved-by steps
	 * @param  movedTo  the moved-to steps position
	 */
	public void newMove( Steps movedBy, Steps movedTo ) {
		// System.out.println( "newMove movedBy: " + movedBy + ", movedTo " + movedTo );
//		System.out.println( "newMove(before snapshot, imageCounter: " + imageCounter + " )" );
		snapshot();
	}; // newMove

	/**
	 * Takes a snapshot (as long as imageCounter is greater than zero)
	 */
	public void snapshot() {
		if( imageCounter > 0 ) {
			if( imageCounter == 1 ) {
				startTime = System.currentTimeMillis();
			}; // if
			String filename = "piece_" + imageCounter + ".jpeg";
			File file = null;
			if( directory == null ) {
				System.err.println( "Directory should not be null, big problem, bye" );
				return;
			}; // if

			// sleeep before the taking
			try {
				Thread.currentThread().sleep( 40 );
			}
			catch( InterruptedException ie ) {
				ie.printStackTrace();
			}; // try

			file = new File( directory, filename );

			// verification, just in case
			if( file.exists() ) {
				System.out.println( " File exists! (" + directory + File.separator + filename + ")" );
				System.exit( 0 );
			}; // if

			if( file != null ) {
				try {
					//camera.saveImage( file, "BMP" );
					camera.saveImage( file, "JPEG" );
System.out.println( " \t " + filename );
				} 
				catch( CameraException e ) {
					new Exception( "Camera Error - The current camera image cannot be saved filename(" + filename + ") ", e);
				}; // try

				// append into the log file
				try {
					Steps location = stage.getLocation();
					File logFile = new File( directory, "thelog.txt" );
					FileWriter fileWriter = new FileWriter( logFile, true );
					BufferedWriter bufferedWriter = new BufferedWriter( fileWriter );
					PrintWriter printWriter = new PrintWriter( bufferedWriter );
					// the first time, we output timestamp
					if( imageCounter == 1 ) {
						printWriter.println( "#\t" + new Date() );
						printWriter.println( "#StepsPerPixelsX\t" + getStepsPerPixelsX() );
						printWriter.println( "#StepsPerPixelsY\t" + getStepsPerPixelsY() );
					}; // if
					printWriter.println( filename + "\t" + location.getX() + "\t" + location.y );
					
					if(gridSelector==null){
					if( imageCounter == ( getTimesVertically() * getTimesHorizontally() ) ) {
						long totalTime = System.currentTimeMillis() - startTime;
						printWriter.println( "#\ttime(miliseconds) " + totalTime );
					}; // 
				}
					
					else
					{
						if( imageCounter == ( (gridSelector.getEndX()-gridSelector.getStartX()+1) * (gridSelector.getEndY()-gridSelector.getStartY()+1) ) ) {
						long totalTime = System.currentTimeMillis() - startTime;
						printWriter.println( "#\ttime(miliseconds) " + totalTime );
					}; // if}
						
					}
					printWriter.close();
				}
				catch( IOException e ) {
					e.printStackTrace();
				}; // try

			}; // if
			try {
				Thread.currentThread().sleep( 40 );
			}
			catch( InterruptedException ie ) {
				ie.printStackTrace();
			}; // try
		}; // if
	}; // snapshot


	/** 
	 * Sets the image counter, which is used as sufix for filename of images
	 * @param  i  the number
	 */
	public void setImageCounter( int i ) {
		imageCounter = i;
	}; // setImageCounter


	/**
	 * Moves to a location and indirectly obtains a snapshot 
	 * (when the newMove listener method is called)
	 * @param  theX  the x position
	 * @param  theY  the y position
	 */
	public void take( int theX, int theY ) {
		if( stage == null ) {
			System.out.println( "take( " + theX + " , " + theY + " ) leaving because stage is null" );
			return;
		}; // if
		try {
			stage.moveTo( theX, theY, true, 200000 );
		}
		catch( Exception e ) {
			e.printStackTrace();
		}; // try
	}; // take


	public void newRoll( double x, double y ) { /* do nothing */ }; // newRoll
	public void newRollX( double x ) { /* do nothing */ }; // newRollY
	public void newRollY( double y ) { /* do nothing */ }; // newRollY
	public void halt( Steps location ) { /* do nothing */ }; // halt


	/**
	 * Initialize the list of ports.
	 * TODO : it seems that this method is not needed at all; not using it now
	 */
	private void initPorts() {
		// Identify the current port.
		CommPortIdentifier currId = stage.getPort();
		//String currName = (currId != null) ? currId.getName(): null;

		// Close the current port.
		try {
			stage.setPort( null );
		} 
		catch( Exception e ) {
			// Ignore it.
		}; // try

		// Search for ports.
		Enumeration ports = CommPortIdentifier.getPortIdentifiers();
		if( ports != null ) {
			while( ports.hasMoreElements() ) {
				// Get the port identifier.
				CommPortIdentifier id = (CommPortIdentifier) ports.nextElement();
				System.out.println( "port: " + id );
			}
		}
	}; // initPorts

	public void connected( Steps location ) {
		//System.out.println( "connected: " + location );
	}; // connected


	/**
	 * Gets the directory
	 * @return  the directory
	 */
	public String getDirectory() {
		File file = new File( directory );
		return file.getAbsolutePath();
	}; // getDirectory

	/**
	 * Sets the directory where the images are to be placed
	 * @param  dir  the directory
	 * @return  null if no problems; otherwise it returns a String error message
	 */
	public String setDirectory( String dir ) {
		if( dir != null ) {
			dir = dir.trim();
		}; // if

		directory = dir;

		if( directory == null || "".equals( dir ) ) {
			// we choose a directory name using date of today
			Calendar rightNow = Calendar.getInstance();
			File fileish = null;
			int k = 1;
			String theDirectory = PREFERRED_DIRECTORY;
			theDirectory += rightNow.get( Calendar.YEAR ) + "-";
						if( rightNow.get( Calendar.DAY_OF_MONTH ) < 10 ) {
							theDirectory += "0";
						}; // if
						theDirectory += rightNow.get( Calendar.DAY_OF_MONTH );
						theDirectory += "-of-";
						if( rightNow.get( Calendar.MONTH ) == Calendar.JANUARY ) {
							theDirectory += "January";
						}; // if
						if( rightNow.get( Calendar.MONTH ) == Calendar.FEBRUARY ) {
							theDirectory += "February";
						}; // if
						if( rightNow.get( Calendar.MONTH ) == Calendar.MARCH ) {
							theDirectory += "March";
						}; // if
						if( rightNow.get( Calendar.MONTH ) == Calendar.APRIL ) {
							theDirectory += "April";
						}; // if
						if( rightNow.get( Calendar.MONTH ) == Calendar.MAY ) {
							theDirectory += "May";
						}; // if
						if( rightNow.get( Calendar.MONTH ) == Calendar.JUNE ) {
							theDirectory += "June";
						}; // if
						if( rightNow.get( Calendar.MONTH ) == Calendar.JULY ) {
							theDirectory += "July";
						}; // if
						if( rightNow.get( Calendar.MONTH ) == Calendar.AUGUST ) {
							theDirectory += "August";
						}; // if
						if( rightNow.get( Calendar.MONTH ) == Calendar.SEPTEMBER ) {
							theDirectory += "September";
						}; // if
						if( rightNow.get( Calendar.MONTH ) == Calendar.OCTOBER ) {
							theDirectory += "October";
						}; // if
						if( rightNow.get( Calendar.MONTH ) == Calendar.NOVEMBER ) {
							theDirectory += "November";
						}; // if
						if( rightNow.get( Calendar.MONTH ) == Calendar.DECEMBER ) {
							theDirectory += "December";
						}; // if

			return setDirectory( theDirectory );
		}; // if

		File file = new File( directory );
		// verify whether the directory does not exist, and 
		// whether it should go in the default directory
		if( file.exists() == false && file.isAbsolute() == false ) {
			String theDirectory = directory;
			if( directory.startsWith( PREFERRED_DIRECTORY ) == false ) {
				theDirectory = PREFERRED_DIRECTORY + directory;
			}; // if
			if( "/".equals( File.separator ) == false ) {
				theDirectory = "C:" + theDirectory;
			}; // if
			return setDirectory( theDirectory );
		}; // if

		// when the directory does not exist, it gets created
		if( file.exists() == false ) {
			boolean ret = file.mkdirs();
			return ret == true ? null : "Error when creating directory (" + directory + ")";
		}; // if

		// at this piont, it exists, now verify that it has to be a directory
		if( file.isDirectory() == false ) {
			return "Error, it seems that '" + dir + "' is not a directory.";
		}; // if

		// at this point, it exists, and it is a directory
		String[] files = file.list();
		if( files.length == 0 ) { 
			return null;
		}; // if
			
		// at this point, it exists, and it is a directory, but it is not empty
		// so, create a new one with other name, incrementing automatically if it ends in number
		String numberString = "";
		for( int i = directory.length() - 1; i >= 0; i-- ) {
			char c = directory.charAt( i );
			if( c == '0' || c == '1' || c == '2' || c == '3' || c == '4'
			||  c == '5' || c == '6' || c == '7' || c == '8' || c == '9' ) {
				numberString = c + numberString;
			}
			else {
				break;
			}; // if
		}; // for
		if( "".equals( numberString ) == true ) {
				return setDirectory( directory + "__1" );
		}; // if
			
		Integer tmp = null;
		try {
			tmp = new Integer( numberString );
		}
		catch( Exception e ) {
			tmp = null;
		}; // try
		if( tmp == null ) {
			// this is super unlikely to happen, 
			// if so, recurse with null so that directory name is automatically assigned
			return setDirectory( null );
		}; // if
		// gotta verify that the number we are using does not cause an existing directory
//			do {
		directory = directory.substring( 0, directory.length() - numberString.length() );
		directory += ( tmp.intValue() + 1 );
		return setDirectory( directory );
	}; // setDirectory


	/** 
	 * Gets the stage status
	 * @return  the stage status
	 */
	public String getStageStatus() {
		if( stage == null ) {
			return "Stage is offline";
		}; // if
		String locationString = "";
		try {
			Steps location = stage.getLocation();
			locationString = " (x: " + location.getX() + ", y: " + location.getY() + ")";
		}
		catch( Exception e ) {
			locationString = ", Exception: " + e;
		}; // try
		return "Stage is on" + locationString;
	}; // getStageStatus

	public double getStepsPerPixelsX() {
		return stepsPerPixelsX;
	}; // getStepsPerPixelsX

	public double getStepsPerPixelsY() {
		return stepsPerPixelsY;
	}; // getStepsPerPixelsY


	/** 
	 * Get the camera
	 * @param  the camera
	 */
	public Camera getCamera() {
		return camera;
	}; // getCamera


	/**
	 * Get the plate name
	 * @return  the plate name, either of PLATE_SIX, PLATE_NINE, PLATE_SIXWELL, or null
	 */
	public String getPlate() {
		 return plate;
	}; // getPlate


	/** 
	 * Get the current configuration file name
	 * @return  the current configuration file name, null if there was an error when loading it previously
	 */
	public String getCurrentConfigurationFilename() {
		return currentConfigurationFilename;
	}; // getCurrentConfigurationFilename


	/**
	 * Get the errors
	 * @return  null if no errors, otherwise it returns the errors
	 */
	public String errors() {
		return errors;
	}; // errors


	/** 
	 * Changes the plate selection
	 * @param  newplate  the new plate
	 */
	public void changePlate( String newplate ) {
		String previousPlate = plate;
		if(gridSelector!=null){
		gridSelector=null;	
		}

		if( PLATE_SIX.equals( newplate ) == true ) {
			plate = PLATE_SIX;
		}; // if

		if( PLATE_NINE.equals( newplate ) == true ) {
			plate = PLATE_NINE;
		}; // if

		if( PLATE_SIXWELL.equals( newplate ) == true ) {
			plate = PLATE_SIXWELL;
		}; // if

		// update the 'plate' value in the properties, if needed
		if( plate != null && plate.equals( previousPlate ) == false ) {
			Properties properties = new Properties();
			try {
				FileInputStream fileInputStream = new FileInputStream( PROPERTIES_FILENAME );
				properties.load( fileInputStream );
				fileInputStream.close();
			}
			catch( FileNotFoundException fnfe ) {
				errors = "Could not find the settings file (" + PROPERTIES_FILENAME + ")";
			}
			catch( IOException ioe ) {
				errors = "Problem when reading the settings file (" + PROPERTIES_FILENAME + ") , the exception is: " + ioe;
			}; // try
			if( errors != null ) {
				 System.out.println( errors );
			}; // if
			
			properties.setProperty( "plate", plate );
			try {
				FileOutputStream fileOutputStream = new FileOutputStream( PROPERTIES_FILENAME );
				properties.store( fileOutputStream, null );
				fileOutputStream.close();
			}
			catch( FileNotFoundException fnfe ) {
				errors = "Could not find the settings file (" + PROPERTIES_FILENAME + ")";
			}
			catch( IOException ioe ) {
				errors = "Problem when writing the settings file (" + PROPERTIES_FILENAME + ") , the exception is: " + ioe;
			}; // try
			if( errors != null ) {
				 System.out.println( errors );
			}; // if
		}; // if

		updateScanningVariables();
	}; // changePlate


	/** Runs the GUI, it ignores command line arguments */
	public static void main( String[] args ) {
		final WormScanner wormScanner = new WormScanner();
		final JButton quitButton = new JButton( "Quit" );
		final JPanel panel = new JPanel();

		// Setup the frame.
		final JFrame frame = new JFrame() { 
			protected void processWindowEvent( WindowEvent e ) { 
				super.processWindowEvent( e ) ; 
				if( e.getID() == WindowEvent.WINDOW_CLOSING ) {
					quitButton.doClick();
				}; // if
			} 
		};
		frame.setTitle( "WormScanner v0.3" );
		Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		frame.setSize( screen.width * 3/4, screen.height * 3/4 );

		// the effect added into the camera in the caliberation
		final CaliberationEffect effect = new CaliberationEffect( 1 );

		// build the menu
		final JMenuItem loadConfigurationMenuItem = new JMenuItem( "Load Configuration ..." );
		final JMenuItem startCaliberationMenuItem = new JMenuItem( "Start Caliberation" );
		final JMenuItem finishCaliberationMenuItem = new JMenuItem( "Finish Caliberation" );
		final JMenuItem cancelCaliberationMenuItem = new JMenuItem( "Cancel Caliberation" );
		final JMenuItem alignmentMenuItem = new JMenuItem( "Take alignment photos" );
		final JMenuItem gridSelectionMenuItem= new JMenuItem( "Grid Selection" );
		JMenuItem quitMenuItem = new JMenuItem( "Quit" );
		JMenuBar menuBar = new JMenuBar();
		JMenu optionsMenu = new JMenu( "Options" );
		JMenu fileMenu = new JMenu( "File" );

		fileMenu.add( quitMenuItem );
		menuBar.add( fileMenu );

		menuBar.add( optionsMenu );
		optionsMenu.add( loadConfigurationMenuItem );
		optionsMenu.addSeparator();
		optionsMenu.add( startCaliberationMenuItem );
		optionsMenu.add( finishCaliberationMenuItem );
		optionsMenu.add( cancelCaliberationMenuItem );
		optionsMenu.addSeparator();
		optionsMenu.add( alignmentMenuItem );
		optionsMenu.add( gridSelectionMenuItem );

		final JLabel statusLabel = new JLabel( "Configuration: " + wormScanner.getCurrentConfigurationFilename() );

		// directory text field
		final JTextField directoryTextField = new JTextField();
		directoryTextField.setMaximumSize( new Dimension( 440, 36 ) );
		directoryTextField.setPreferredSize( new Dimension( 440, 36 ) );

		// scan 6,10cm plates button in their own panel
		final JButton scan6Button = new JButton( "Scan" );
		scan6Button.setAlignmentX( Component.CENTER_ALIGNMENT );
		final JPanel scan6Panel = new JPanel();
		scan6Panel.setLayout( new BoxLayout( scan6Panel, BoxLayout.Y_AXIS ) );
		scan6Panel.add( scan6Button );

		// scan Six-Well plates button in their own panel
		final JButton scanSixWellButton = new JButton( "Scan Well #1" );
		scanSixWellButton.setAlignmentX( Component.CENTER_ALIGNMENT );
		final JPanel scanSixWellPanel = new JPanel();
		scanSixWellPanel.setLayout( new BoxLayout( scanSixWellPanel, BoxLayout.X_AXIS ) );
		scanSixWellPanel.add( scanSixWellButton );
		final JButton skipSixWellButton = new JButton( "Skip Well #1" );
		skipSixWellButton.setAlignmentX( Component.CENTER_ALIGNMENT );
		scanSixWellPanel.add( Box.createRigidArea( new Dimension( 6, 6 ) ) );
		scanSixWellPanel.add( skipSixWellButton );
		final JButton skipAllButton = new JButton( "Skip Remaining Wells" );
		skipAllButton.setAlignmentX( Component.CENTER_ALIGNMENT );
		skipAllButton.setEnabled( false );
		scanSixWellPanel.add( Box.createRigidArea( new Dimension( 5, 5 ) ) );
		scanSixWellPanel.add( skipAllButton );
		// we use a list to 'remember' the current well to be scanned
		final List<Integer> currentWellList = new ArrayList<Integer>();
		currentWellList.add( new Integer( 1 ) );

		// action for Scan well #1
		scanSixWellButton.addActionListener(
			new ActionListener() {
				public void actionPerformed( ActionEvent event ) {
					Integer currentWell = currentWellList.get( 0 );
					System.out.println( "On my eyes: " + currentWell );
					// directory
					String originalDirectory = directoryTextField.getText();
					if( currentWell.intValue() == 1 ) {
						String directoryError = wormScanner.setDirectory( originalDirectory );
						if( directoryError != null ) {
							statusLabel.setText( "Directory error, " + directoryError );
							return;
						}; // if
						directoryTextField.setText( wormScanner.getDirectory() );
						directoryTextField.paintImmediately( 0, 0, directoryTextField.getWidth(), directoryTextField.getHeight() );
						if( "".equals( originalDirectory ) || originalDirectory == null ) {
							originalDirectory = wormScanner.getDirectory();
						}; // if
					}; // if
					wormScanner.setDirectory( originalDirectory + File.separator + currentWell );
					System.out.println( "\tscan: " + currentWell );
					int theX = 0;
					int theY = 0;
					
					
				        int startX=-1;
						int startY=-1;
						int endX=-1;
						int endY=-1;
						if(gridSelector!=null){
					    startX=gridSelector.getStartX();
					    startY=gridSelector.getStartY();
					    endX=gridSelector.getEndX();
					    endY=gridSelector.getEndY();}
					    else{
						 startX= 0;
						 startY=0;
						 endY=wormScanner.getTimesVertically()-1;
						 endX=wormScanner.getTimesHorizontally()-1;						   						    
					    }
					    
						// the movement and photo snaps happen in this loop
						for( int vertical = startY; vertical <=endY; vertical++ ) {
							theY = wormScanner.getYScanReady(-currentWell  ) - (int) ( (vertical+1) * DEFAULT_CAMERA_Y_RESOLUTION * wormScanner.getStepsPerPixelsY() );
							if( (vertical-startY) % 2 == 0 ) {
								for( int horizontal = startX; horizontal <=endX; horizontal++ ) {
									theX = wormScanner.getXScanReady( -currentWell  ) - (int) ( (horizontal-1) * DEFAULT_CAMERA_X_RESOLUTION * wormScanner.getStepsPerPixelsX() );
									wormScanner.setImageCounter( (vertical-startY) * (endX-startX+1) + 1 + horizontal-startX );
									wormScanner.take( theX, theY );
								}; // for
							}
							else {
								for( int horizontal = ( endX ); horizontal >= startX; horizontal-- ) {
									theX = wormScanner.getXScanReady( -currentWell ) - (int) ( (horizontal-1) * DEFAULT_CAMERA_X_RESOLUTION * wormScanner.getStepsPerPixelsX() );
									wormScanner.setImageCounter( (vertical-startY) * (endX-startX+1) + 1 + horizontal-startX );
									wormScanner.take( theX, theY );
								}; // for
							}; // if
						}; // for

					// directory again
					directoryTextField.setText( originalDirectory );
					directoryTextField.paintImmediately( 0, 0, directoryTextField.getWidth(), directoryTextField.getHeight() );
					// figure out next well
					int nextWell = new Integer( currentWell.intValue() + 1 );
					if( nextWell > 6 ) {
						nextWell = 1;
					}; // if
					if( nextWell == 1 ) {
						skipAllButton.setEnabled( false );
						skipAllButton.setText( "Skip Remaining Wells" );
					}
					else {
						skipAllButton.setEnabled( true );
						skipAllButton.setText( "Skip Remaining Well" + ( nextWell == 6 ? "" : "s" ) );
					}; // if
					scanSixWellButton.setText( "Scan Well #" + nextWell );
					skipSixWellButton.setText( "Skip Well #" + nextWell );
					currentWellList.clear();
					currentWellList.add( new Integer( nextWell ) );
					wormScanner.getXScanReady( nextWell );
					wormScanner.getYScanReady( nextWell );
					wormScanner.moveToScanReady();
				}; // actionPerformed
			});

		// action for Skip well #1
		skipSixWellButton.addActionListener(
			new ActionListener() {
				public void actionPerformed( ActionEvent event ) {
					Integer currentWell = currentWellList.get( 0 );
					// directory
					String originalDirectory = directoryTextField.getText();
					if( currentWell.intValue() == 1 ) {
						String directoryError = wormScanner.setDirectory( originalDirectory );
						if( directoryError != null ) {
							statusLabel.setText( "Directory error, " + directoryError );
							return;
						}; // if
						directoryTextField.setText( wormScanner.getDirectory() );
						directoryTextField.paintImmediately( 0, 0, directoryTextField.getWidth(), directoryTextField.getHeight() );
						if( "".equals( originalDirectory ) || originalDirectory == null ) {
							originalDirectory = wormScanner.getDirectory();
						}; // if
					}; // if
					wormScanner.setDirectory( originalDirectory + File.separator + currentWell );
					// directory again
					directoryTextField.setText( originalDirectory );
					directoryTextField.paintImmediately( 0, 0, directoryTextField.getWidth(), directoryTextField.getHeight() );
					// figure out next well
					int nextWell = new Integer( currentWell.intValue() + 1 );
					if( nextWell > 6 ) {
						nextWell = 1;
					}; // if
					if( nextWell == 1 ) {
						skipAllButton.setEnabled( false );
						skipAllButton.setText( "Skip Remaining Wells" );
					}
					else {
						skipAllButton.setEnabled( true );
						skipAllButton.setText( "Skip Remaining Well" + ( nextWell == 6 ? "" : "s" ) );
					}; // if
					scanSixWellButton.setText( "Scan Well #" + nextWell );
					skipSixWellButton.setText( "Skip Well #" + nextWell );
					currentWellList.clear();
					currentWellList.add( new Integer( nextWell ) );
					wormScanner.getXScanReady( nextWell );
					wormScanner.getYScanReady( nextWell );
					wormScanner.moveToScanReady();
				}; // actionPerformed
			});

		// action for Skip All Wells
		skipAllButton.addActionListener(
			new ActionListener() {
				public void actionPerformed( ActionEvent event ) {
					Integer currentWell = currentWellList.get( 0 );
					// directory
					String originalDirectory = directoryTextField.getText();
					// skipping remaining ones by creating empty directories
					for( int d = currentWell; d <= 6; d++ ) {
						wormScanner.setDirectory( originalDirectory + File.separator + d );
					}; // for
					// directory again
					directoryTextField.setText( originalDirectory );
					// figure out next well
					int nextWell = 1;
					skipAllButton.setEnabled( false );
					skipAllButton.setText( "Skip Remaining Wells" );
					scanSixWellButton.setText( "Scan Well #" + nextWell );
					skipSixWellButton.setText( "Skip Well #" + nextWell );
					currentWellList.clear();
					currentWellList.add( new Integer( nextWell ) );
					wormScanner.getXScanReady( nextWell );
					wormScanner.getYScanReady( nextWell );
					wormScanner.moveToScanReady();
				}; // actionPerformed
			});

		// caliberation start menu option
		startCaliberationMenuItem.addActionListener(
			new ActionListener() {
				public void actionPerformed( ActionEvent event ) {
					scan6Button.setEnabled( false );
					scanSixWellButton.setEnabled( false );
					skipSixWellButton.setEnabled( false );
					directoryTextField.setEnabled( false );
					wormScanner.moveHome();
					wormScanner.getCamera().addEffect( effect );
					startCaliberationMenuItem.setEnabled( false );
					finishCaliberationMenuItem.setEnabled( true );
					cancelCaliberationMenuItem.setEnabled( true );
				}
			});

		// caliberation finish menu option
		finishCaliberationMenuItem.setEnabled( false );
		finishCaliberationMenuItem.addActionListener(
			new ActionListener() {
				public void actionPerformed( ActionEvent event ) {
					wormScanner.getCamera().removeEffect( effect );
					startCaliberationMenuItem.setEnabled( true );
					finishCaliberationMenuItem.setEnabled( false );
					cancelCaliberationMenuItem.setEnabled( false );
					wormScanner.moveToScanReady();
					scan6Button.setEnabled( true );
					scanSixWellButton.setEnabled( true );
					skipSixWellButton.setEnabled( true );
					directoryTextField.setEnabled( true );
				}
			});

		// caliberation cancel menu option
		cancelCaliberationMenuItem.setEnabled( false );
		cancelCaliberationMenuItem.addActionListener(
			new ActionListener() {
				public void actionPerformed( ActionEvent event ) {
					wormScanner.getCamera().removeEffect( effect );
					wormScanner.moveToScanReady();
					startCaliberationMenuItem.setEnabled( true );
					finishCaliberationMenuItem.setEnabled( false );
					cancelCaliberationMenuItem.setEnabled( false );
					scan6Button.setEnabled( true );
					scanSixWellButton.setEnabled( true );
					skipSixWellButton.setEnabled( true );
					directoryTextField.setEnabled( true );
				}
			});

		// setup the 'load configuration' menu item
		File dir = new File( ProgramInfo.DEFAULT_CONFIGURATION_DIRECTORY );
		if( dir.exists() == false ) {
			dir = new File( "." );
		}; // if
		final File dir2 = dir;
		final JFileChooser fileChooser = new JFileChooser( dir2 );
		fileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
		loadConfigurationMenuItem.addActionListener(
			new ActionListener() {
				public void actionPerformed( ActionEvent event ) {
					fileChooser.setDialogTitle( "Load Configuration ..." );
						if( fileChooser.showOpenDialog( frame ) == JFileChooser.APPROVE_OPTION ) {
							boolean error = wormScanner.loadConfiguration( fileChooser.getSelectedFile() );
							if( error == true ) {
								directoryTextField.setText( wormScanner.errors() );
							}
							else {
								statusLabel.setText( fileChooser.getSelectedFile().getAbsolutePath() );
								if(gridSelector!=null){
								gridSelector=null;	
								}
							}; // if
						}; // if
				}
			});

		// the quit button moves the stage to 'home' and quits
		quitButton.addActionListener( 
				new ActionListener() {
					public void actionPerformed( ActionEvent event ) {
						frame.setVisible( false );
						frame.dispose();
						wormScanner.moveHome();
						wormScanner.closeStage();
						System.exit( 0 );
					}
				});
		quitButton.setAlignmentX( Component.CENTER_ALIGNMENT );

		// use quitbutton's actionperform for menu-quit
		quitMenuItem.addActionListener( 
			new ActionListener() {
				public void actionPerformed( ActionEvent event ) {
					quitButton.doClick();
				}
			});
			
			gridSelectionMenuItem.addActionListener(
			new ActionListener() {
				public void actionPerformed( ActionEvent event ) {
					if(gridSelector==null){
					gridSelector=new GridSelector(wormScanner.getTimesVertically(),wormScanner.getTimesHorizontally());}
					else{
					gridSelector.show();	
					}
					
				}
			});
			
			
			

		// the scan button for 6cm plate
		scan6Button.addActionListener( 
			new ActionListener() {
				public void actionPerformed( ActionEvent event ) {
					String directoryError = wormScanner.setDirectory( directoryTextField.getText() );
					if( directoryError != null ) {
						statusLabel.setText( "Directory error, " + directoryError );
						return;
					}; // if
					directoryTextField.setText( wormScanner.getDirectory() );
					directoryTextField.paintImmediately( 0, 0, directoryTextField.getWidth(), directoryTextField.getHeight() );

					int theX;
					int theY;

					int plateNumber = 0;
					String originalDirectory = wormScanner.getDirectory();

					// only one iteration is done for 6cmm,10mm plates
					do {
	
						// here's part of the trick of staying in the loop for 6well plate
						if( WormScanner.PLATE_SIXWELL.equals( wormScanner.getPlate() ) == true ) {
							plateNumber++;
							// sub-directories 
							wormScanner.setDirectory( originalDirectory + File.separator + plateNumber );
						}; // if

						int startX=-1;
						int startY=-1;
						int endX=-1;
						int endY=-1;
						if(gridSelector!=null){
					    startX=gridSelector.getStartX();
					    startY=gridSelector.getStartY();
					    endX=gridSelector.getEndX();
					    endY=gridSelector.getEndY();}
					    else{
						 startX= 0;
						 startY=0;
						 endY=wormScanner.getTimesVertically()-1;
						 endX=wormScanner.getTimesHorizontally()-1;						   						    
					    }
					    
						// the movement and photo snaps happen in this loop
						for( int vertical = startY; vertical <=endY; vertical++ ) {
							theY = wormScanner.getYScanReady( plateNumber ) - (int) ( (vertical+1) * DEFAULT_CAMERA_Y_RESOLUTION * wormScanner.getStepsPerPixelsY() );
							if( (vertical-startY) % 2 == 0 ) {
								for( int horizontal = startX; horizontal <=endX; horizontal++ ) {
									theX = wormScanner.getXScanReady( plateNumber ) - (int) ( (horizontal-1) * DEFAULT_CAMERA_X_RESOLUTION * wormScanner.getStepsPerPixelsX() );
									wormScanner.setImageCounter( (vertical-startY) * (endX-startX+1) + 1 + horizontal-startX );
									wormScanner.take( theX, theY );
								}; // for
							}
							else {
								for( int horizontal = ( endX ); horizontal >= startX; horizontal-- ) {
									theX = wormScanner.getXScanReady( plateNumber ) - (int) ( (horizontal-1) * DEFAULT_CAMERA_X_RESOLUTION * wormScanner.getStepsPerPixelsX() );
									wormScanner.setImageCounter( (vertical-startY) * (endX-startX+1) + 1 + horizontal-startX );
									wormScanner.take( theX, theY );
								}; // for
							}; // if
						}; // for

					} while( plateNumber > 0 && plateNumber < 6 );

					// when needed, update the x,y scan-ready coordinates
					if( WormScanner.PLATE_SIXWELL.equals( wormScanner.getPlate() ) == true ) {
						wormScanner.getXScanReady( 1 );
						wormScanner.getYScanReady( 1 );
					}; // if
					
					wormScanner.moveToScanReady();
				}
			});

		// directory panel
		final JPanel directoryScanPanel = new JPanel();
		directoryScanPanel.setLayout( new BoxLayout( directoryScanPanel, BoxLayout.Y_AXIS ) );

		// plate radio buttons
		final JRadioButton sixcmRadioButton = new JRadioButton( WormScanner.PLATE_SIX );
		JRadioButton ninecmRadioButton = new JRadioButton( WormScanner.PLATE_NINE );
		JRadioButton sixwellRadioButton = new JRadioButton( WormScanner.PLATE_SIXWELL );
		sixcmRadioButton.setActionCommand( WormScanner.PLATE_SIX );
		ninecmRadioButton.setActionCommand( WormScanner.PLATE_NINE );
		sixwellRadioButton.setActionCommand( WormScanner.PLATE_SIXWELL );
		ninecmRadioButton.addActionListener( 
			new ActionListener() {
				public void actionPerformed( ActionEvent event ) {
					wormScanner.changePlate( WormScanner.PLATE_NINE );
					Integer foundit = null;
					Component[] components = directoryScanPanel.getComponents();
					for( int index = 0; index < components.length; index++ ) {
						if( components[ index ] == scanSixWellPanel ) {
							foundit = index;
						}; // if
					}; // for
					if( foundit != null ) {
						directoryScanPanel.add( scan6Panel, foundit );
						directoryScanPanel.remove( scanSixWellPanel );
						directoryScanPanel.validate();
						directoryScanPanel.update( directoryScanPanel.getGraphics() );
					}; // if
					wormScanner.getXScanReady( 0 );
					wormScanner.getYScanReady( 0 );
					wormScanner.moveToScanReady();
				}; // actionPerformed
			});
		sixcmRadioButton.addActionListener( 
			new ActionListener() {
				public void actionPerformed( ActionEvent event ) {
					wormScanner.changePlate( WormScanner.PLATE_SIX );
					Integer foundit = null;
					Component[] components = directoryScanPanel.getComponents();
					for( int index = 0; index < components.length; index++ ) {
						if( components[ index ] == scanSixWellPanel ) {
							foundit = index;
						}; // if
					}; // for
					if( foundit != null ) {
						directoryScanPanel.add( scan6Panel, foundit );
						directoryScanPanel.remove( scanSixWellPanel );
						directoryScanPanel.validate();
						directoryScanPanel.update( directoryScanPanel.getGraphics() );
					}; // if
					wormScanner.getXScanReady( 0 );
					wormScanner.getYScanReady( 0 );
					wormScanner.moveToScanReady();
				}; // actionPerformed
			});
		sixwellRadioButton.addActionListener( 
			new ActionListener() {
				public void actionPerformed( ActionEvent event ) {
					wormScanner.changePlate( WormScanner.PLATE_SIXWELL );
					Integer foundit = null;
					Component[] components = directoryScanPanel.getComponents();
					for( int index = 0; index < components.length; index++ ) {
						if( components[ index ] == scan6Panel ) {
							foundit = index;
						}; // if
					}; // for
					if( foundit != null ) {
						directoryScanPanel.add( scanSixWellPanel, foundit );
						directoryScanPanel.remove( scan6Panel );
						directoryScanPanel.validate();
						directoryScanPanel.update( directoryScanPanel.getGraphics() );
					}; // if
					wormScanner.getXScanReady( 1 );
					wormScanner.getYScanReady( 1 );
					wormScanner.moveToScanReady();
				}; // actionPerformed
			});

		// directory label
		JLabel directoryLabel = new JLabel( "Directory:" );
		directoryLabel.setAlignmentX( Component.CENTER_ALIGNMENT );

		JLabel plateLabel = new JLabel( "Type of plate:" );

		// menu option for alignment
		alignmentMenuItem.addActionListener( 
			new ActionListener() {
				public void actionPerformed( ActionEvent actionEvent ) {
					wormScanner.setDirectory( null );
					sixcmRadioButton.doClick();
					// sleeep before the taking of photo
					try {
						Thread.currentThread().sleep( 400 );
					}
					catch( InterruptedException ie ) {
						ie.printStackTrace();
					}; // try
					System.out.println( "alignment" );
					wormScanner.setImageCounter( 1 );
					int baseX = wormScanner.getXScanReady( 0 );
					int baseY = wormScanner.getYScanReady( 0 );
					System.out.println( "alignment, x: " + baseX );
					System.out.println( "alignment, y: " + baseY );
					wormScanner.take( baseX, baseY );
					int theX = baseX - (int) ( DEFAULT_CAMERA_X_RESOLUTION * wormScanner.getStepsPerPixelsX() );
					// sleeep before the taking of photo
					try {
						Thread.currentThread().sleep( 400 );
					}
					catch( InterruptedException ie ) {
						ie.printStackTrace();
					}; // try
					wormScanner.setImageCounter( 2 );
					wormScanner.take( theX, baseY );
					// sleeep to wait for photo be written to disk
					// this may not be needed but just in case
					try {
						Thread.currentThread().sleep( 400 );
					}
					catch( InterruptedException ie ) {
						ie.printStackTrace();
					}; // try
					ImagePlus imagePlus = NewImage.createByteImage( "assembled", DEFAULT_CAMERA_X_RESOLUTION * 2, DEFAULT_CAMERA_Y_RESOLUTION, 1, NewImage.FILL_BLACK );
					ImageProcessor imageProcessor = imagePlus.getProcessor();
					System.out.println( "directory: __" + wormScanner.getDirectory() + "__" );
					ImagePlus aImagePlus = new ImagePlus( wormScanner.getDirectory() + File.separator + "piece_1.bmp" );
					ImagePlus eImagePlus = new ImagePlus( wormScanner.getDirectory() + File.separator + "piece_2.bmp" );
					imageProcessor.copyBits( aImagePlus.getProcessor(), 0, 0, Blitter.ADD );
					imageProcessor.copyBits( eImagePlus.getProcessor(), DEFAULT_CAMERA_X_RESOLUTION, 0, Blitter.ADD );
					ImagePlus displayImagePlus = new ImagePlus();
					displayImagePlus.setProcessor( imageProcessor );
					displayImagePlus.show();

				}
			});

		// everything gets added to the panel
		Dimension size = null;
		panel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );
		panel.add( Box.createRigidArea( new Dimension( 100, 10 ) ) );
		if( wormScanner.errors() == null ) {
			// group things inside directoryScanPanel 
			directoryScanPanel.add( directoryLabel );
			directoryScanPanel.add( directoryTextField );
			directoryScanPanel.add( Box.createRigidArea( new Dimension( 100, 10 ) ) );

			// add either the six-well panel or the 6,10 panel
			if( WormScanner.PLATE_SIXWELL.equals( wormScanner.getPlate() ) ) {
				directoryScanPanel.add( scanSixWellPanel );
			}
			else {
				directoryScanPanel.add( scan6Panel );
			}; // if

			// the platishPanel is for grouping components
			JPanel platishPanel = new JPanel();
			platishPanel.setLayout( new BoxLayout( platishPanel, BoxLayout.Y_AXIS ) );
			platishPanel.add( plateLabel );
			sixcmRadioButton.setSelected( WormScanner.PLATE_SIX.equals( wormScanner.getPlate() ) );
			ninecmRadioButton.setSelected( WormScanner.PLATE_NINE.equals( wormScanner.getPlate() ) );
			sixwellRadioButton.setSelected( WormScanner.PLATE_SIXWELL.equals( wormScanner.getPlate() ) );
			ButtonGroup buttonGroup = new ButtonGroup();
			buttonGroup.add( sixcmRadioButton );
			buttonGroup.add( ninecmRadioButton );
			buttonGroup.add( sixwellRadioButton );
			platishPanel.add( sixcmRadioButton );
			platishPanel.add( ninecmRadioButton );
			platishPanel.add( sixwellRadioButton );

			// the upperPanel groups directoryScanPanel and platishPanel
			JPanel upperPanel = new JPanel();
			upperPanel.setLayout( new FlowLayout( FlowLayout.CENTER, 20, 10 ) );
			upperPanel.add( directoryScanPanel );
			upperPanel.add( platishPanel );

			panel.add( upperPanel );

			// camera settings panel
			JPanel cameraSettingsPanel = new JPanel();
			cameraSettingsPanel.setLayout( new FlowLayout( FlowLayout.CENTER, 10, 5 ) );
			cameraSettingsPanel.add( wormScanner.frameCamControls );

			if( wormScanner.getCamera() != null ) {
				panel.add( wormScanner.getCamera().getDisplay() );
				size = new Dimension( wormScanner.getCamera().getResolution().width + 40, wormScanner.getCamera().getResolution().height + 294 );
			}
			else {
				JPanel displayPanel = new JPanel();
				displayPanel.setMinimumSize( new Dimension( 640, 480 ) );
				displayPanel.setPreferredSize( new Dimension( 640, 480 ) );
				displayPanel.setSize( new Dimension( 640, 480 ) );
				displayPanel.setBorder( new LineBorder( Color.BLACK ) );
				panel.add( displayPanel );
				size = new Dimension( 640 + 40, 480 + 230 );
			}; // if
			panel.add( cameraSettingsPanel );
			frame.setJMenuBar( menuBar );
		}
		else {
			JTextArea textArea = new JTextArea( wormScanner.errors() );
			textArea.setLineWrap( true );
			panel.add( textArea );
		}; // if

		// Adjust the panel size.
		if( size == null ) {
			size = new Dimension( 460, 300 );
		}; // if
		panel.setMinimumSize( size );
		panel.setPreferredSize( size );
		panel.setSize( size );

		JPanel outerPanel = new JPanel();
		outerPanel.setLayout( new BorderLayout() );
		outerPanel.add( panel, BorderLayout.CENTER );
		outerPanel.add( statusLabel, BorderLayout.PAGE_END );
		Dimension outerSize = new Dimension( size.width + 20, size.height + 40 );
		outerPanel.setMinimumSize( outerSize );
		outerPanel.setPreferredSize( outerSize );
		outerPanel.setSize( outerSize );

		// Complete the frame.
		frame.add( outerPanel );
		frame.setSize( outerPanel.getSize() );
		
		// Start the GUI.
		frame.pack();
		frame.setVisible( true );
	}; // main

}; // class WormScanner

