package de.unisiegen.informatik.antex;

import java.io.File;
import java.util.LinkedList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtils;

/**
 * ps2pdf Ant task.
 * 
 * @author Benedikt Meurer
 */
public class Ps2pdfTask extends AbstractSimpleTask {
	/**
	 * Initialize the ps2pdf Ant task.
	 */
	public void init() throws BuildException {
		super.init();
	}
	
	/**
	 * Maps the PostScript file <code>path</code> to a PDF path (if PostScript actually).
	 * 
	 * @param path the input path.
	 * 
	 * @return the output path for the input <code>path</code>.
	 */
	protected String mapFileName(String path) {
		String outPath = SystemUtils.translateFileExtension(path, "ps", "pdf");
		if (outPath != null) {
			return outPath;
		}
		return super.mapFileName(path);
	}
	
	/**
	 * Execute ps2pdf on <code>infile</code> to produce <code>outfile</code>.
	 * 
	 * @param infile the input file.
	 * @param outfile the output file.
	 * 
	 * @throws BuildException in case of an error.
	 */
	protected void execute(File infile, File outfile) throws BuildException {
		// verbose logging
		logVerbose("Converting PostScript file " + infile.getName() + " to PDF");
		
		// prepare and run the ps2pdf command
		LinkedList commandline = new LinkedList();
		commandline.add(SystemUtils.executableName("ps2pdf"));
		if (!isVerbose()) {
			commandline.add("-q");
		}
		commandline.add(FileUtils.translatePath(infile.getPath()));
		commandline.add(FileUtils.translatePath(outfile.getPath()));
		launch(commandline, outfile.getParentFile());
		
		// verbose logging
		logVerbose("Successfully converted PostScript file " + infile.getName() + " to PDF");
	}
}
