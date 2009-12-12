package de.unisiegen.informatik.antex;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.FileUtils;

/**
 * Pdfopt Ant task.
 * 
 * @author Benedikt Meurer
 */
public class PdfoptTask extends AbstractTask {
	private boolean cleanup;
	private LinkedList mappers;
	private File tofile;
	
	/**
	 * Initialize the pdfopt Ant task.
	 */
	public void init() throws BuildException {
		super.init();
		this.cleanup = false;
		this.mappers = new LinkedList();
		this.tofile = null;
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
	 * Add a fully configured <code>mapper</code> for this task.
	 * 
	 * @param mapper the fully configured <code>mapper</code>.
	 */
	public void addConfiguredMapper(Mapper mapper) {
		this.mappers.add(mapper);
	}
	
	/**
	 * Retrieve the tofile.
	 * 
	 * @return the tofile.
	 */
	public File getTofile() {
		return this.tofile;
	}
	
	/**
	 * Set the tofile.
	 * 
	 * @param tofile the tofile.
	 */
	public void setTofile(File tofile) {
		this.tofile = tofile;
	}
	
	/**
	 * Execute the pdfopt Ant task.
	 * 
	 * @throws BuildException in case of an error.
	 */
	public void execute() throws BuildException {
		super.execute();
		
		// collect our files
		File[] files = getFiles();
		
		// figure out the basedir and FileUtils
		File baseDir = getProject().getBaseDir();
		FileUtils fileUtils = FileUtils.getFileUtils();
		
		// run pdfopt for each input file
		for (int i = 0; i < files.length; ++i) {
			File infile = files[i];
			
			// figure out the output file
			File outfile = null;
			if (getFilesets().isEmpty() && getTofile() != null) {
				outfile = getTofile();
			}
			else {
				// figure out the relative path of infile (if possible)
				String infilePath = fileUtils.removeLeadingPath(baseDir, infile);

				// generate the output file using the supplied mappers
				for (Iterator it = this.mappers.iterator(); it.hasNext() && outfile == null; ) {
					FileNameMapper mapper = ((Mapper)it.next()).getImplementation();
					String[] outfilePaths = mapper.mapFileName(infilePath);
					if (outfilePaths != null && outfilePaths.length > 0) {
						outfile = fileUtils.resolveFile(getDestdir(), outfilePaths[0]);
					}
				}
				
				// fallback to the identiy mapper
				if (outfile == null) {
					outfile = fileUtils.resolveFile(getDestdir(), infilePath);
				}
			}
			
			// check if we need to do anything after all
			if (fileUtils.fileNameEquals(infile, outfile) || !fileUtils.isUpToDate(infile, outfile)) {
				// create a temporary file for pdfopt (delete on exit)
				File tmpfile = fileUtils.createTempFile("pdfopt-", outfile.getName(), outfile.getParentFile(), true);
				
				// verbose logging
				logVerbose("Optimizing PDF file " + infile.getName() + " to " + outfile.getName());
				
				// prepare and run the pdfopt command
				LinkedList commandline = new LinkedList();
				commandline.add(SystemUtils.executableName("pdfopt"));
				commandline.add(FileUtils.translatePath(infile.getPath()));
				commandline.add(FileUtils.translatePath(tmpfile.getPath()));
				launch(commandline, infile.getParentFile());

				// rename tmpfile to outfile
				renameFile(tmpfile, outfile);
				
				// verbose logging
				logVerbose("Successfully optimized PDF file " + infile.getName() + " to " + outfile.getName());
			}
			
			// delete the input file if requested (and different from outfile)
			if (!fileUtils.fileNameEquals(infile, outfile) && isCleanup()) {
				Delete delete = (Delete)getProject().createTask("delete");
				delete.setFile(infile);
				delete.setOwningTarget(getOwningTarget());
				delete.setVerbose(isVerbose());
				delete.execute();
			}
		}
	}
}
