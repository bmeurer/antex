package de.unisiegen.informatik.antex;

import java.io.File;
import java.util.LinkedList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtils;

/**
 * Pdfopt Ant task.
 * 
 * @author Benedikt Meurer
 */
public class PdfoptTask extends AbstractSimpleTask {
	/**
	 * Initialize the pdfopt Ant task.
	 */
	public void init() throws BuildException {
		super.init();
	}

	/**
	 * Execute pdfopt for the <code>infile</code> and store the output to <code>outfile</code>.
	 * 
	 * @param infile the input file.
	 * @param outfile the output file.
	 * 
	 * @throws BuildException in case of an error.
	 */
	protected void execute(File infile, File outfile)	throws BuildException {
		// verbose logging
		logVerbose("Optimizing PDF file " + infile.getName());
		
		// prepare and run the pdfopt command
		LinkedList commandline = new LinkedList();
		commandline.add(SystemUtils.executableName("pdfopt"));
		commandline.add(FileUtils.translatePath(infile.getPath()));
		commandline.add(FileUtils.translatePath(outfile.getPath()));
		launch(commandline, infile.getParentFile());

		// verbose logging
		logVerbose("Successfully optimized PDF file " + infile.getName());
	}
}
