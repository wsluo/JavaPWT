/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javapwt;

/*
 * Filename: ResultProcessor.java
 */


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;


/**
 * Reads nlive.txt and writes out a report to disk
 */

public class ResultProcessor {
	
	/**
	 * Recursively processes a directory and all its sub-directories
	 * @param  directory  the directory
	 */
	public void recursivelyProcessDirectory( String directory ) {
		if( directory.endsWith( File.separator ) == false ) {
			directory += File.separator;
		}; // if
		List<String> resultsList = new ArrayList<String>();
		recursivelyProcessDirectory( directory, resultsList );
		// save the new contents
		try {
			FileWriter fileWriter = new FileWriter( directory + "nlive_report.txt" );
			BufferedWriter bufferedWriter = new BufferedWriter( fileWriter );
			PrintWriter printWriter = new PrintWriter( bufferedWriter );
			for( String line : resultsList ) {
				printWriter.println( line );
			}; // for
			printWriter.close();
		}
		catch( IOException ioe ) {
			ioe.printStackTrace();
		}; // try
	}; // recursivelyProcessDirectory

	
	/**
	 * Recursively processes a directory and all its sub-directories
	 * @param  directory  the directory
	 * @param  resultsList  the list into which write out the results
	 */
	private void recursivelyProcessDirectory( String directory, List<String> resultsList ) {
		File dir = new File( directory );
		if( dir.exists() == false ) {
			System.out.println( "Directory does not exist: " + directory );
			return;
		}; // if
		
		if( dir.isDirectory() == false ) {
			System.out.println( "This is not a directory: " + directory );
			return;
		}; // if

		// get the sub-directories
		List<String> subdirectoriesList = new ArrayList<String>();
		File[] folders = dir.listFiles();
		for( File eachFolder : folders ) {
			if( eachFolder.isDirectory() == false ) {
				continue;
			}; // if
			subdirectoriesList.add( eachFolder.getAbsolutePath() );
		}; // for

		String error = getFolderResults( dir.getAbsolutePath(), resultsList );
		if( error != null ) {
			System.out.println( "... done: " + dir.getAbsolutePath() + " " + error );
		}; // if

		// recursion happens here
		for( String subdirectory : subdirectoriesList ) {
			recursivelyProcessDirectory( subdirectory, resultsList );
		}; // for
	}; // recursivelyProcessDirectory


    /**
     * Gets the results of a folder
resultsList.add(dirName+"\t"+component[0]+"\t"+component[1]+"\t"+"\tinspected");
     * @param  dirName  the directory name
     * @param  resultsList  list into which put the results
     * @return  null when everything goes OK, otherwise an error message
     */
    public String getFolderResults(String dirName, List<String> resultsList ){
    	// special case: we skip this folder when there is a similar folder with suffix __1
    	File similar = new File( dirName + "__1" );
    	if( similar.exists() == true && similar.isDirectory() == true ) {
    		// we just leave
    		System.out.println( "skipping: " + dirName );
    		return null;
    	}; // if
    	
    	// see whether there is an assembled image
    	boolean assembledFlag = false;
    	boolean rawImagesFlag = false;
		File imgFile = new File( dirName + File.separator + "assembled.jpeg" );
		if( imgFile.exists() ) {
			assembledFlag = true;
			rawImagesFlag = true; // we assume the raw images were there
		}
		else {
			// see whether there are 'raw' images (we only check a few)
			boolean foundAll = true;
			for( int i = 1; i < 10; i++ ) {
				File rawImage = new File( dirName + File.separator + "piece_" + i  + ".jpeg" );
				foundAll = foundAll && rawImage.exists();
			}
			rawImagesFlag = foundAll;
		}; // if
System.out.println( "assembledFlag \t " + assembledFlag );
System.out.println( "rawImagesFlag \t " + rawImagesFlag );
		List<String> linesList = null;
		boolean inspectedByHumanFlag = false;
		try {
			BufferedReader br = new BufferedReader( new FileReader( dirName + File.separator + motionDetector.N_LIVE_RESULTS_TXT ) );
			String line = null;
			while( ( line = br.readLine() ) != null ) {
				if( linesList != null ) {
					linesList.add( line );
				}; // if
				if( motionDetector.WORM_DETAILS.equalsIgnoreCase( line ) == true ) {
					linesList = new ArrayList<String>();
				}; // if
				if( motionDetector.INSPECTED_BY_HUMAN.equalsIgnoreCase( line ) == true ) {
					inspectedByHumanFlag = true;
				}; // if
			}; // while
		}
		catch( FileNotFoundException fnfe ) {
			if( assembledFlag == false && rawImagesFlag == false ) {
				// no data, just leave
				return null;
			}; // if
			if( rawImagesFlag == true && assembledFlag == false ) {
				resultsList.add( dirName + "\t" + "N/A" + "\t" + "raw-data never processed" );
				return null;
			}; // if
			resultsList.add( dirName + "\t" + "N/A" + "\t" + "data partially processed" );
			return null;
		}
		catch( IOException e ) {
			System.out.println( e );
			return e.toString();
		}; // try
		int count = 0;
		for( String each : linesList ) {
			if( each.startsWith( "#" ) == true ) {
				continue;
			}; // if
			String[] pieces = each.split( "\t" );
			if( pieces.length != 6 ) {
				System.out.println( "\tignoring line " + each );
				continue;
			}; // if
			count += new Integer( pieces[ 0 ] );
		}; // for
		if( inspectedByHumanFlag == true ) {
			resultsList.add( dirName + "\t" + count + "\t" + motionDetector.INSPECTED_BY_HUMAN );
		}
		else {
			resultsList.add( dirName + "\t" + count + "\t" + "processed but not inspected by human" );
		}; // if
		return null;
	}; // getFolderResults
}; // class ResultProcessor
