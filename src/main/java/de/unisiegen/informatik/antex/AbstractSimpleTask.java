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
 * Abstract base class for simple Ant tasks (i.e. pdfopt, ps2pdf).
 * 
 * @author Benedikt Meurer
 */
public abstract class AbstractSimpleTask extends AbstractTask {
	private boolean cleanup;
	private LinkedList deletes;
	private LinkedList mappers;
	private File tofile;
	
	/**
	 * Initialize this Ant task.
	 */
	public void init() throws BuildException {
		super.init();
		this.cleanup = false;
		this.deletes = new LinkedList();
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
	 * Ant callback to create nested <code>&lt;delete&gt;</code>s.
	 * 
	 * @return a newly allocated Delete.
	 */
	public Delete createDelete() {
		Delete delete = (Delete)getProject().createTask("delete");
		delete.setOwningTarget(getOwningTarget());
		delete.setVerbose(isVerbose());
		this.deletes.add(delete);
		return delete;
	}
	
	/**
	 * Add a fully configured <code>&lt;mapper&gt;</code> for this task.
	 * 
	 * @param mapper the fully configured Mapper.
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
	 * Fallback output filename mapper.
	 * 
	 * @param path the input path.
	 * 
	 * @return the output path for the input <code>path</code>.
	 */
	protected String mapFileName(String path) {
		return path;
	}
	
	/**
	 * Process <code>infile</code> to produce the <code>outfile</code>.
	 * 
	 * @param infile the input file.
	 * @param outfile the output file.
	 * 
	 * @throws BuildException in case of an error.
	 */
	protected abstract void execute(File infile, File outfile) throws BuildException;
	
	/**
	 * Execute this Ant task.
	 * 
	 * @throws BuildException in case of an error.
	 */
	public final void execute() throws BuildException {
		super.execute();
		
		// collect our files
		File[] files = getFiles();
		
		// figure out the basedir and FileUtils
		File baseDir = getProject().getBaseDir();
		FileUtils fileUtils = FileUtils.getFileUtils();
		
		// prepare the deletes for this task
		LinkedList deletes = new LinkedList(this.deletes);
		
		// process each input file to produce output
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
				
				// use the fallback mapper
				if (outfile == null) {
					outfile = fileUtils.resolveFile(getDestdir(), mapFileName(infilePath));
				}
			}
			
			// check if we need to do anything after all
			if (fileUtils.fileNameEquals(infile, outfile) || !fileUtils.isUpToDate(infile, outfile)) {
				// create the directories for the outfile
				SystemUtils.createLeadingDirectories(outfile);
				
				// create a temporary file for the actual task (delete on exit)
				File tmpfile = fileUtils.createTempFile("tmp", outfile.getName(), outfile.getParentFile(), true);

				// perform the actual processing
				execute(infile, tmpfile);

				// rename tmpfile to outfile
				SystemUtils.renameFile(tmpfile, outfile);
			}
			
			// schedule deletion of the input file if requested (and different from outfile)
			if (!fileUtils.fileNameEquals(infile, outfile) && isCleanup()) {
				Delete delete = (Delete)getProject().createTask("delete");
				delete.setFile(infile);
				delete.setOwningTarget(getOwningTarget());
				delete.setVerbose(isVerbose());
				deletes.add(delete);
			}
		}

		// run all Delete tasks
		for (Iterator it = deletes.iterator(); it.hasNext(); ) {
			Delete delete = (Delete)it.next();
			delete.execute();
		}
	}	
}
