package de.unisiegen.informatik.antex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Echo.EchoLevel;
import org.apache.tools.ant.types.FileSet;

/**
 * Latex Ant task.
 * 
 * @author Benedikt Meurer
 */
public class LatexTask extends AbstractTask {
	private static String[] TEMPORARY_FILE_PATTERNS = new String[] {
		"*.aux", "*.log", "*.toc",
		"*.lof", "*.lot", "*.bbl", "*.blg", "*.out", "*.ilg", "*.gil",
		"*.gxs", "*.gxg", "*.glx", "*.glg", "*.gil", "*.gls", "*.glo",
		"*.hst", "*.ver",
		"*.ind", "*.idx", "*.lor", "*.los", "*.tmp", "*.lg", "*.4tc",
		"*.xal", "*.xgl", "*.4ct", "*.tpt", "*.xref", "*.idv", "WARNING*",
		"*.lol"
	};
 	private boolean cleanup;
 	private List deletes;
	private boolean pdf;
	
	/**
	 * Initialize the latex task.
	 */
	public void init() throws BuildException {
		super.init();
		this.cleanup = false;
		this.deletes = new LinkedList();
		this.pdf = true;
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
	 * Check if PDF mode is enabled.
	 * 
	 * @return <code>true</code> if PDF mode is enabled, <code>false</code> if disabled.
	 */
	public boolean isPdf() {
		return this.pdf;
	}
	
	/**
	 * Enable or disable PDF mode.
	 * 
	 * @param pdf <code>true</code> to enable PDF mode, <code>false</code> to disable.
	 */
	public void setPdf(boolean pdf) {
		this.pdf = pdf;
	}
	
	/**
	 * Ant callback to create nested <code>&lt;delete&gt;</code>s.
	 * 
	 * @return a newly allocated Delete.
	 */
	public Delete createDelete() {
		Delete delete = (Delete)getProject().createTask("delete");
		delete.setVerbose(isVerbose());
		this.deletes.add(delete);
		return delete;
	}
	
	/**
	 * Execute this LaTeX task.
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
			int indexOfDotTex = fileName.lastIndexOf(".tex");
			if (indexOfDotTex <= 0 || indexOfDotTex + 4 != fileName.length()) {
				throw new BuildException("Unsupported LaTeX file " + fileName);
			}
			baseNames[i] = fileName.substring(0, indexOfDotTex);
		}

		// execute LaTeX until all files report success
		boolean[] fileStati = new boolean[files.length];
		boolean finished = false;
		for (int tries = 0; !finished; ++tries) {
			// process the LaTeX files
			finished = true;
			for (int i = 0; i < files.length; ++i) {
				if (!fileStati[i]) {
					if (tries == 5) {
						log("Giving up after 4 attempts to fix unresolved references in LaTeX file " + files[i].getName(), EchoLevel.ERR.getLevel());
						break;
					}
					
					if (!executeLatex(files[i], baseNames[i])) {
						finished = false;
					}
					else {
						fileStati[i] = true;
					}
				}
			}
		}
		
		// check if we finished successfully
		if (finished) {
			// add default deletes if cleanup is specified
			List deletes = new LinkedList(this.deletes);
			if (isCleanup()) {
				// verbose logging
				if (isVerbose()) {
					log("Adding default delete patterns");
				}

				// add the default patterns for each LaTeX file
				Delete delete = (Delete)getProject().createTask("delete");
				for (int i = 0; i < files.length; ++i) {
					FileSet fileSet = new FileSet();
					fileSet.setDir(files[i].getParentFile());
					for (int j = 0; j < TEMPORARY_FILE_PATTERNS.length; ++j) {
						fileSet.createInclude().setName(TEMPORARY_FILE_PATTERNS[j]);
					}
					delete.addFileset(fileSet);
				}
				delete.setVerbose(isVerbose());
				deletes.add(delete);
			}

			// run all Delete tasks
			for (Iterator it = deletes.iterator(); it.hasNext(); ) {
				Delete delete = (Delete)it.next();
				delete.execute();
			}
		}
		else {
			log("Skipping deletes as there were unresolved references", EchoLevel.WARN.getLevel());
		}
	}

	/**
	 * Execute the LaTeX interpreter on the specified <code>file</code>.
	 * 
	 * @param file the LaTeX file.
	 * @param baseName the base name of the LaTeX file (w/o the extension).
	 * 
	 * @return <code>false</code> if LaTeX must be run again because of unresolved
	 *         references.
	 * 
	 * @throws BuildException if an error is reported by LaTeX.
	 */
	private boolean executeLatex(File file, String baseName) throws BuildException {
		// figure out the base directory
		File baseDirectory = file.getParentFile();
		
		// verbose logging
		if (isVerbose()) {
			log("Processing LaTeX file " + file.getName());
		}
		
		// prepare and run the latex command
		String[] commandline = new String[5];
		commandline[0] = SystemUtils.executableName(isPdf() ? "pdflatex" : "latex");
		commandline[1] = "-file-line-error";
		commandline[2] = "-halt-on-error";
		commandline[3] = "-interaction=errorstopmode";
		commandline[4] = file.getName();
		launch(commandline, baseDirectory);
		
		// verbose logging
		if (isVerbose()) {
			log("Checking for unresolved references in LaTeX file " + file.getName());
		}
		
		// open the <basename>.log file and check for unresolved references
		String logFileName = baseName + ".log";
		boolean logFileIndicatesRerun = false;
		try {
			String line;
			Pattern pattern = Pattern.compile("(Rerun (LaTeX|to get cross-references right)|Package glosstex Warning: Term |There were undefined references|Package natbib Warning: Citation\\(s\\) may have changed)");
			BufferedReader reader = new BufferedReader(new FileReader(new File(baseDirectory, logFileName)));
			while ((line = reader.readLine()) != null) {
				Matcher matcher = pattern.matcher(line);
				if (matcher.find()) {
					logFileIndicatesRerun = true;
					break;
				}
			}
			reader.close();
		}
		catch (Exception e) {
			throw new BuildException("Failed to inspect log file " + logFileName, e);
		}
				
		// verbose logging
		if (isVerbose()) {
			if (logFileIndicatesRerun) {
				log("Log file " + logFileName + " indicates rerun for LaTeX file " + file.getName());
			}
			else {
				log("Successfully processed LaTeX file " + file.getName());
			}
		}
		
		return !logFileIndicatesRerun;
	}
}
