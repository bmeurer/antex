package de.unisiegen.informatik.antex;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.taskdefs.Echo.EchoLevel;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.FileResource;

/**
 * Abstract base class for Ant tasks.
 * 
 * @author Benedikt Meurer
 */
public abstract class AbstractTask extends Task {
	private File file;
	private List fileSets;
	private boolean verbose;
	
	/**
	 * Initialize the latex task.
	 */
	public void init() throws BuildException {
		super.init();
		this.file = null;
		this.fileSets = new LinkedList();
		this.verbose = false;
	}
	
	/**
	 * Return the TeX file.
	 * 
	 * @return the TeX file.
	 */
	public File getFile() {
		return this.file;
	}
	
	/**
	 * Set the TeX file.
	 * 
	 * @param file the TeX file.
	 */
	public void setFile(File file) {
		this.file = file;
	}
	
	/**
	 * Add a new file set to the latex task.
	 * 
	 * @param fileSet the new FileSet to add.
	 */
	public void addFileset(FileSet fileSet) {
		this.fileSets.add(fileSet);
	}
	
	/**
	 * Retrieve the list of FileSets for this task.
	 * 
	 * @return the list of FileSets for this task.
	 */
	public List getFilesets() {
		return this.fileSets;
	}
	
	/**
	 * Check if verbose mode is enabled.
	 * 
	 * @return <code>true</code> if verbose mode is enabled, <code>false</code> if disabled.
	 */
	public boolean isVerbose() {
		return this.verbose;
	}
	
	/**
	 * Enable or disable verbose mode.
	 * 
	 * @param verbose <code>true</code> to enable verbose mode, <code>false</code> to disable.
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	
	/**
	 * Returns an array with all absolute input files.
	 * 
	 * @return array with files.
	 * 
	 * @throws BuildException if no input files are specified.
	 */
	protected File[] getFiles() throws BuildException {
		File[] files = null;
		File file = getFile();
		List fileSets = getFilesets();
		if (!fileSets.isEmpty()) {
			int numFiles = 0;
			for (Iterator it = fileSets.iterator(); it.hasNext(); ) {
				numFiles += ((FileSet)it.next()).size();
			}
			if (numFiles > 0) {
				int i = 0;
				files = new File[numFiles];
				for (Iterator it = fileSets.iterator(); it.hasNext(); ) {
					FileSet fileSet = (FileSet)it.next();
					for (Iterator fit = fileSet.iterator(); fit.hasNext(); ) {
						FileResource fileResource = (FileResource)fit.next();
						file = fileResource.getFile();
						if (!file.isAbsolute()) {
							file = new File(fileSet.getDir(getProject()), file.getPath());
						}
						files[i++] = file;
					}
				}
			}
		}
		else if (file != null) {
			// translate to absolute file first
			if (!file.isAbsolute()) {
				file = new File(getProject().getBaseDir(), file.getPath());
			}
			files = new File[] { file };
		}
		if (files == null) {
			throw new BuildException("No input files given");
		}
		return files;
	}
	
	/**
	 * Launch the given <code>commandline</code> using the Ant <code>Execute</code> class.
	 * 
	 * @param commandline the command line to run.
	 * @param workingDirectory the working directory to use for execution.
	 * 
	 * @throws BuildException in case of an error.
	 */
	protected void launch(String[] commandline, File workingDirectory) throws BuildException {
		ExecuteStreamHandler handler;
		if (isVerbose()) {
			handler = new LogStreamHandler(this, EchoLevel.INFO.getLevel(), EchoLevel.ERR.getLevel());
		}
		else {
			handler = new ExecuteStreamHandler() {
				public void stop() {}
				public void start() throws IOException {}
				public void setProcessOutputStream(InputStream arg0) throws IOException {}
				public void setProcessInputStream(OutputStream arg0) throws IOException {}
				public void setProcessErrorStream(InputStream arg0) throws IOException {}
			};
		}
		Execute execute = new Execute(handler);
		execute.setAntRun(getProject());
		execute.setCommandline(commandline);
		execute.setWorkingDirectory(workingDirectory);
		try {
			int exitValue = execute.execute();
			if (Execute.isFailure(exitValue)) {
				throw new BuildException(commandline[0] + " terminated with exit code " + exitValue);
			}
		}
		catch (IOException e) {
			throw new BuildException("Failed to execute " + commandline[0], e);
		}
	}
}
