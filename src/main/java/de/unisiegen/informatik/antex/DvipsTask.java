package de.unisiegen.informatik.antex;

import java.io.File;
import java.util.LinkedList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Delete;

/**
 * dvips Ant task.
 * 
 * @author Benedikt Meurer
 */
public class DvipsTask extends AbstractTask {
	private boolean cleanup;
	
	/**
	 * Initialize the dvips Ant task.
	 */
	public void init() throws BuildException {
		super.init();
		this.cleanup = false;
	}
	
	/**
	 * Check if cleanup mode is enabled.
	 * 
	 * @return <code>true</code> if cleanup mode is enabled, <code>false</code> if disabled.
	 */
	public boolean isCleanup() {
		return this.cleanup;
	}
	
	/**
	 * Enable or disable cleanup mode.
	 * 
	 * @param cleanup <code>true</code> to enable cleanup mode, <code>false</code> to disable.
	 */
	public void setCleanup(boolean cleanup) {
		this.cleanup = cleanup;
	}
	
	/**
	 * Execute the dvips Ant task.
	 * 
	 * @throws BuildException in case of an error.
	 */
	public void execute() throws BuildException {
		super.execute();
		
		// collect our files
		File[] files = getFiles();
		
		// figure out the base names for the files
		String[] baseNames = new String[files.length];
		for (int i = 0; i < files.length; ++i) {
			String fileName = files[i].getName();
			int indexOfDotDvi = fileName.lastIndexOf(".dvi");
			if (indexOfDotDvi <= 0 || indexOfDotDvi + 4 != fileName.length()) {
				throw new BuildException("Unsupported DVI file " + fileName);
			}
			baseNames[i] = fileName.substring(0, indexOfDotDvi);
		}

		// run dvips for each input file
		for (int i = 0; i < files.length; ++i) {
			File file = files[i];
			
			// verbose logging
			logVerbose("Converting DVI file " + file.getName() + " to PostScript");
			
			// prepare and run the dvips command
			LinkedList commandline = new LinkedList();
			commandline.add(SystemUtils.executableName("dvips"));
			if (!isVerbose()) {
				commandline.add("-q");
			}
			commandline.add("-o");
			commandline.add(baseNames[i] + ".ps");
			commandline.add(file.getName());
			launch(commandline, file.getParentFile());
			
			// verbose logging
			logVerbose("Successfully converted DVI file " + file.getName() + " to PostScript");
		}
		
		// check if we should cleanup
		if (isCleanup()) {
			// execute delete tasks to cleanup
			for (int i = 0; i < files.length; ++i) {
				Delete delete = (Delete)getProject().createTask("delete");
				delete.setFile(files[i]);
				delete.setVerbose(isVerbose());
				delete.execute();
			}
		}
	}
}
