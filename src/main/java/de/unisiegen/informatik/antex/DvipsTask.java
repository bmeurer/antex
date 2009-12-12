package de.unisiegen.informatik.antex;

import java.io.File;
import java.util.LinkedList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtils;

/**
 * dvips Ant task.
 * 
 * @author Benedikt Meurer
 */
public class DvipsTask extends AbstractSimpleTask {
	/**
	 * Initialize the dvips Ant task.
	 */
	public void init() throws BuildException {
		super.init();
	}

	/**
	 * Maps the DVI file <code>path</code> to a PostScript path (if DVI actually).
	 * 
	 * @param path the input path.
	 * 
	 * @return the output path for the input <code>path</code>.
	 */
	protected String mapFileName(String path) {
		String outPath = SystemUtils.translateFileExtension(path, "dvi", "ps");
		if (outPath != null) {
			return outPath;
		}
		return super.mapFileName(path);
	}
	
	/**
	 * Run dvips on <code>infile</code> to produce <code>outfile</code>.
	 * 
	 * @param infile the input file.
	 * @param outfile the output file.
	 * 
	 * @throws BuildException in case of an error.
	 */
	protected void execute(File infile, File outfile) throws BuildException {
		// verbose logging
		logVerbose("Converting DVI file " + infile.getName() + " to PostScript");
		
		// prepare and run the dvips command
		LinkedList commandline = new LinkedList();
		commandline.add(SystemUtils.executableName("dvips"));
		if (!isVerbose()) {
			commandline.add("-q");
		}
		commandline.add("-o");
		commandline.add(FileUtils.translatePath(outfile.getPath()));
		commandline.add(FileUtils.translatePath(infile.getPath()));
		launch(commandline, outfile.getParentFile());
		
		// verbose logging
		logVerbose("Successfully converted DVI file " + infile.getName() + " to PostScript");
	}
}
