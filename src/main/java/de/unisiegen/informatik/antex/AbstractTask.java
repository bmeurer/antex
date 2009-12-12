package de.unisiegen.informatik.antex;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.taskdefs.Mkdir;
import org.apache.tools.ant.taskdefs.Move;
import org.apache.tools.ant.taskdefs.Echo.EchoLevel;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.FileResource;

/**
 * Abstract base class for Ant tasks.
 * 
 * @author Benedikt Meurer
 */
public abstract class AbstractTask extends Task {
	private File destdir;
	private File file;
	private List fileSets;
	private boolean verbose;
	
	/**
	 * Initialize the latex task.
	 */
	public void init() throws BuildException {
		super.init();
		this.destdir = getProject().getBaseDir();
		this.file = null;
		this.fileSets = new LinkedList();
		this.verbose = false;
	}
	
	/**
	 * Return the location to store the output files.
	 * 
	 * @return the location to store the output files.
	 */
	public File getDestdir() {
		return this.destdir;
	}
	
	/**
	 * Set the location to store the output files.
	 * 
	 * @param destdir the location to store the output files.
	 */
	public void setDestdir(File destdir) {
		this.destdir = destdir;
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
	 * Execute this AbstractTask.
	 * 
	 * @throws BuildException in case of an error.
	 */
	public void execute() throws BuildException {
		super.execute();
		
		// ensure that the destdir exists
		Mkdir mkdir = (Mkdir)getProject().createTask("mkdir");
		mkdir.setDir(getDestdir());
		mkdir.setOwningTarget(getOwningTarget());
		mkdir.execute();
	}
	
	/**
	 * Returns an array with all absolute input files.
	 * 
	 * @return array with files.
	 * 
	 * @throws BuildException if no input files are specified.
	 */
	protected File[] getFiles() throws BuildException {
		HashSet files = new HashSet();
		File file = getFile();
		List fileSets = getFilesets();
		if (!fileSets.isEmpty()) {
			for (Iterator it = fileSets.iterator(); it.hasNext(); ) {
				FileSet fileSet = (FileSet)it.next();
				for (Iterator fit = fileSet.iterator(); fit.hasNext(); ) {
					files.add(((FileResource)it.next()).getFile().getAbsoluteFile());
				}
			}
		}
		else if (file != null) {
			files.add(file.getAbsoluteFile());
		}
		return (File[])files.toArray(new File[0]);
	}
	
	/**
	 * Launch the given <code>commandline</code> using the Ant <code>Execute</code> class.
	 * 
	 * @param commandline the string list representing the command line to run.
	 * @param workingDirectory the working directory to use for execution.
	 * 
	 * @throws BuildException in case of an error.
	 */
	protected void launch(List commandline, File workingDirectory) throws BuildException {
		String[] cmdline = (String[])commandline.toArray(new String[0]);
		ExecuteStreamHandler handler = new LogStreamHandler(this,
				(isVerbose() ? EchoLevel.INFO.getLevel() : EchoLevel.VERBOSE.getLevel()),
				EchoLevel.ERR.getLevel());
		Execute execute = new Execute(handler);
		execute.setAntRun(getProject());
		execute.setCommandline(cmdline);
		execute.setWorkingDirectory(workingDirectory);
		try {
			int exitValue = execute.execute();
			if (Execute.isFailure(exitValue)) {
				throw new BuildException(cmdline[0] + " terminated with exit code " + exitValue);
			}
		}
		catch (IOException e) {
			throw new BuildException("Failed to execute " + cmdline[0], e);
		}
	}
	
	/**
	 * Log verbose messages.
	 * 
	 * @param msg the verbose message to log.
	 */
	protected void logVerbose(String msg) {
		int msgLevel = isVerbose() ? EchoLevel.INFO.getLevel() : EchoLevel.VERBOSE.getLevel();
		log(msg, msgLevel);
	}
	
	/**
	 * Rename the <code>source</code> file to the <code>dest</code> file.
	 * 
	 * @param source the source file.
	 * @param dest the dest file.
	 * 
	 * @throws BuildException in case of an error.
	 */
	protected void renameFile(File source, File dest) throws BuildException {
		Move move = (Move)getProject().createTask("move");
		move.setFile(source);
		move.setTofile(dest);
		move.setVerbose(isVerbose());
		move.setOwningTarget(getOwningTarget());
		move.execute();		
	}
}
