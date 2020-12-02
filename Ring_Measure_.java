import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Line;
import ij.gui.PlotWindow;
import ij.gui.WaitForUserDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

/*
 * ImageJ Plugin to identify and measure single beads in a field in 2 colour (green and red). Each
 * bead has a series of line profiles placed across it to measure the intensity of the line and
 * identify the 2 largest peaks. Output is in the form of a text file which can be imported easily 
 * into Excel, R or Graphpad
 * Written by D. Kelly
 * March 2020
 */
public class Ring_Measure_ implements PlugIn{

	String greenFilename;
	String redFilename;
	ImagePlus greenImage;
	int greenImageID;
	ImagePlus redImage;
	int redImageID;
	int counter = 0;
	boolean firstRun = true;
	
	public void run(String arg) {
		IJ.run("Set Measurements...", "area mean min center bounding shape feret's redirect=None decimal=2");
		ClearROI();
		OpenImage();
		defineGreenRing();
		IJ.resetThreshold(greenImage);
		new WaitForUserDialog("Finished", "Plugin Finished").show();
	}
		
	public void OpenImage(){
		/*
		 * Open and process the images ready
		 * for analysis
		 */
		
		//Set the measurements required for the plugin
		IJ.run("Set Measurements...", "area mean min centroid center bounding fit shape feret's redirect=None decimal=2");
		new WaitForUserDialog("Open Image", "Drag and drop both images into taskbar").show();
			
			
		//Select Green Image
		new WaitForUserDialog("Select Green", "Click on Green Image and Click OK").show();	
		greenImage = WindowManager.getCurrentImage();
		IJ.run(greenImage, "Enhance Contrast", "saturated=0.35");  //Autoscale image
		greenImageID = greenImage.getID();
		greenFilename = greenImage.getShortTitle(); 	//Get Green Image file name
		
		//Select Red Image
		new WaitForUserDialog("Select Red", "Click on Red Image and Click OK").show();	
		redImage = WindowManager.getCurrentImage();
		IJ.run(redImage, "Enhance Contrast", "saturated=0.35");  //Autoscale image
		redImageID = redImage.getID();
		redFilename = redImage.getShortTitle(); 	//Get red Image file name
	}
	
	public void defineGreenRing(){
		/*
		 * Find the bead using the green channel excluding those
		 * beads that fall outside the selection criteria. Once
		 * selected they are passed to the line profile function 
		 * to produce the peaks.
		 */
		IJ.selectWindow(greenImageID);
		//Measure the beads and produce a binary mask
		IJ.setAutoThreshold(greenImage, "Yen dark");
		IJ.run(greenImage, "Analyze Particles...", " show=Masks exclude include");
		ImagePlus maskImage = WindowManager.getCurrentImage();
		IJ.run(maskImage, "Watershed", "");  //Split touching beads
		IJ.setAutoThreshold(maskImage, "Default");
		IJ.run(maskImage, "Analyze Particles...", "size=10-Infinity show=Nothing add");
		
		/*
		 * Add all the qualifying beads to the ROI Manager for criteria checking
		 * and passing onto lineprofiler
		 */
		RoiManager rm = new RoiManager();
		rm = RoiManager.getInstance();	
		int numROI = rm.getCount();
		if (numROI>0){
			for(int a=0;a<numROI;a++){
				rm.select(a);
				rm.runCommand(greenImage,"Measure");
				ResultsTable rt = new ResultsTable();
				rt = Analyzer.getResultsTable();
				double isItRound=rt.getValueAsDouble(34, 0);
				double centreX=rt.getValueAsDouble(8, 0);
				double centreY=rt.getValueAsDouble(9, 0);
				double startX = rt.getValueAsDouble(11, 0);
				double startY = rt.getValueAsDouble(12, 0);
				double theWidth = rt.getValueAsDouble(13, 0);
				double theHeight = rt.getValueAsDouble(14, 0);
				if(isItRound>=0.99){
					LineProfiler(theWidth, theHeight, centreX, centreY, startX, startY);
				}
				ClearResults();
			}
		}
		
		
	}
	
	public void LineProfiler(double theWidth, double theHeight, double centreX, double centreY, double startX, double startY){
		/*
		 *Applies a line profile to the selected bead based on its centroid location. The lines are
		 *orientated North-South, East-West and Forward Slash and Backward Slash 
		 */
		String lineDir = null;
		IJ.selectWindow(greenImageID);		
		int X =(int) centreX;
		int Y =(int) centreY;
		int wide = (int) theWidth;
		int high = (int) theHeight;
		
		counter++;
		String colour ="g";
		//Vertical Line
		lineDir = "v";
		greenImage.setRoi(new Line(X,((Y-high/2)-5),X,((Y+high/2)+5)));
		IJ.run(greenImage, "Plot Profile", "");
		ImagePlus imp = WindowManager.getCurrentImage();
		extractLinedata(colour,lineDir);
		imp.changes=false;
		imp.close();
		redImage.setRoi(new Line(X,((Y-high/2)-5),X,((Y+high/2)+5)));
		IJ.run(redImage, "Plot Profile", "");
		imp = WindowManager.getCurrentImage();
		colour="r";
		extractLinedata(colour,lineDir);
		imp.changes=false;
		imp.close();
		
		//Horizontal Line
		colour="g";
		lineDir = "h";
		greenImage.setRoi(new Line(((X-wide/2)-5),Y,((X+wide/2)+5),Y));
		IJ.run(greenImage, "Plot Profile", "");
		imp = WindowManager.getCurrentImage();
		extractLinedata(colour,lineDir);
		imp.changes=false;
		imp.close();
		colour="r";
		redImage.setRoi(new Line(((X-wide/2)-5),Y,((X+wide/2)+5),Y));
		IJ.run(redImage, "Plot Profile", "");
		imp = WindowManager.getCurrentImage();
		extractLinedata(colour,lineDir);
		imp.changes=false;
		imp.close();
		
		// Back Slash Line
		colour="g";
		lineDir = "b";
		greenImage.setRoi(new Line((startX+5),(startY+5),((startX+wide)-5),((startY+high)-5)));
		IJ.run(greenImage, "Plot Profile", "");
		imp = WindowManager.getCurrentImage();
		extractLinedata(colour,lineDir);
		imp.changes=false;
		imp.close();
		colour="r";
		redImage.setRoi(new Line((startX+5),(startY+5),((startX+wide)-5),((startY+high)-5)));
		IJ.run(redImage, "Plot Profile", "");
		imp = WindowManager.getCurrentImage();
		extractLinedata(colour,lineDir);
		imp.changes=false;
		imp.close();
		
		//Forward Slash Line
		colour="g";
		lineDir = "f";
		greenImage.setRoi(new Line(((startX)+5),((startY+high)-5),((startX+wide)-5),(startY+5)));
		IJ.run(greenImage, "Plot Profile", "");
		imp = WindowManager.getCurrentImage();
		extractLinedata(colour,lineDir);		
		imp.changes=false;
		imp.close();
		colour="r";
		redImage.setRoi(new Line(((startX)+5),((startY+high)-5),((startX+wide)-5),(startY+5)));
		IJ.run(redImage, "Plot Profile", "");
		imp = WindowManager.getCurrentImage();
		extractLinedata(colour,lineDir);					
		imp.changes=false;
		imp.close();		
				
		
		int greenX = X;
		int greenY = Y;
		setImageNumbers(greenX, greenY);
	}
	
	public void extractLinedata(String colour,String lineDir){
		/*
		 * Measure the Line profile and send the results to the
		 * outputtext function
		 */
		String gName = "Plot of " + greenFilename;
		String rName = "Plot of " + redFilename;
		Frame frame = null;
		if(colour.contentEquals("g")) {
			frame = WindowManager.getFrame(gName);
		}
		if(colour.contentEquals("r")) {
			frame = WindowManager.getFrame(rName);
		}
		
		float[] peakvals = null;
		
		if (frame!=null && (frame instanceof PlotWindow)) {
	        PlotWindow tw = (PlotWindow)frame;
	        ResultsTable table = tw.getResultsTable();
	        if (table!= null) {
	        	//GetPeaks
	        	peakvals = table.getColumn(1);
	        	double [] twoPeaks = GetTwinPeaks(peakvals);
	        	OutputText(colour,peakvals, twoPeaks, lineDir);
	        }
	    }
	 
	}
	
	public double [] GetTwinPeaks(float[]peakvals){
		/*
		 * Calculates the 2 peaks corrresponding to the points
		 * at which the line profile crosses the edge of the bead
		 */
		double [] twoPeaks = new double [2];
		
		double biggestFirstHalf = 0;
		double biggestSecondHalf=0;
		int halfwayPoint = peakvals.length/2;
		/*
		 * Find largest peak value in line profile
		 * in first half of profile
		 */
		for(int x=0;x<halfwayPoint;x++){
			double currentVal = peakvals[x];
			if (currentVal>biggestFirstHalf){
				biggestFirstHalf=currentVal;
			}
		}
		
		/*
		 * Find largest peak value in line profile
		 * in second half of profile
		 */
		for(int x=halfwayPoint;x<peakvals.length;x++){
			double currentVal = peakvals[x];
			if(currentVal>biggestSecondHalf){
				biggestSecondHalf = currentVal;
			}
		}
		twoPeaks[0]=biggestFirstHalf;
		twoPeaks[1]=biggestSecondHalf;
		
		return twoPeaks;
	}
	
	
	public void ClearROI(){
		/*
		 * Method to clear all ROI from the 
		 * ROI manager
		 */
		RoiManager rm = new RoiManager(); 
		ImagePlus imp = null;
		rm = RoiManager.getInstance();
		int numroi = rm.getCount();
		if (numroi>0){
			rm.runCommand(imp,"Deselect");
			rm.runCommand(imp,"Delete");
		}
	}
	
	public void ClearResults(){
		/*
		 * Function to clear the results
		 * table before the next measurement
		 */
		ResultsTable emptyrt = new ResultsTable();	
		emptyrt = Analyzer.getResultsTable();
		int valnums = emptyrt.getCounter();
		for(int a=0;a<valnums;a++){
			IJ.deleteRows(0, a);
		}
	}
	
	public void OutputText(String colour, float[] peakvals, double[] twopeaks, String lineDir){
		/*
		 * Outputs the calculated peak vales to a text file
		 * which can be opened in Excel or R 
		 */
		String CreateName = "C:\\Temp\\Results.txt";
		String FILE_NAME = CreateName;
		try{
			FileWriter fileWriter = new FileWriter(FILE_NAME,true);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			String theDirection = null;
			if(lineDir.contentEquals("v")) {
				theDirection = " Vertical = ";
			}
			if(lineDir.contentEquals("h")) {
				theDirection = " Horizontal = ";
			}
			if(lineDir.contentEquals("f")) {
				theDirection = " Forwardslash = ";
			}
			if(lineDir.contentEquals("b")) {
				theDirection = " Backslash = ";
			}
			
			bufferedWriter.newLine();
			if (firstRun==true){
				bufferedWriter.write(" File= " + greenFilename + " Cell " + counter);
				firstRun = false;
			}
			bufferedWriter.newLine();
			if (colour.equals("g")){
				bufferedWriter.write("Green Profile Cell " + counter + theDirection + twopeaks[0] + " and " + twopeaks [1]);
			}
			if (colour.equals("r")){
				bufferedWriter.write("Red Profile Cell " + counter + theDirection + twopeaks[0] + " and " + twopeaks [1]);
			}
			
			//Section to output all the values on the line profile, uncomment it to use
		//	for(int e = 0; e<peakvals.length;e++){
				//bufferedWriter.newLine();
		//		bufferedWriter.write(" " + peakvals[e]);
		//	}
			
			bufferedWriter.close();
		}
		catch(IOException ex) {
            System.out.println(
                "Error writing to file '"
                + FILE_NAME + "'");
        }
	}
	
	
	public void setImageNumbers(int greenX, int greenY){
		/*
		 * Places a number next to the counted bead so that
		 * user can identify which set of profiles came from 
		 * which bead.
		 */
		IJ.selectWindow(greenImageID);
		IJ.setForegroundColor(255, 255, 255);
		ImageProcessor ip = greenImage.getProcessor();
		Font font = new Font("SansSerif", Font.PLAIN, 18);
		ip.setFont(font);
		ip.setColor(new Color(255, 255, 0));
		String cellnumber = String.valueOf(counter);			
		ip.drawString(cellnumber, greenX, greenY);
		greenImage.updateAndDraw();
			
	}
}
