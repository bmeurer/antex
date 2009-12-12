package de.unisiegen.informatik.antex;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtils;


/**
 * System utilities.
 * 
 * @author Benedikt Meurer
 */
public class SystemUtils {
	/**
	 * Generate the system specific executable name.
	 * 
	 * @param name the executable name.
	 * 
	 * @return the system specific executable <code>name</code>.
	 */
	public static String executableName(String name) {
		String osname = System.getProperty("os.name");
		if (osname.contains("Windows")) {
			name = name + ".exe";
		}
		return name;
	}
	
	/**
	 * Rename the <code>source</code> file to the <code>dest</code> file.
	 * 
	 * @param source the source file.
	 * @param dest the dest file.
	 * 
	 * @throws BuildException in case of an error.
	 */
	public static void renameFile(File source, File dest) throws BuildException {
		try {
			FileUtils fileUtils = FileUtils.getFileUtils();
			fileUtils.rename(source, dest);
		}
		catch (IOException e) {
			throw new BuildException("Failed to rename " + source.getPath() + " to " + dest.getPath(), e);
		}
	}
	
	/**
	 * Translate the file name extension of <code>path</code> to <code>newExt</code> if it
	 * matches <code>oldExt</code>. Otherwise <code>null</code> is returned.
	 * 
	 * @param path the file name.
	 * @param oldExt the old extension.
	 * @param newExt the new extension.
	 * 
	 * @return the translated <code>path</code>, or <code>null</code> if no match.
	 */
	public static String translateFileExtension(String path, String oldExt, String newExt) {
		int dotIndex = path.length() - (oldExt.length() + 1);
		if (dotIndex > 0 && path.charAt(dotIndex) == '.') {
			String ext = path.substring(dotIndex + 1);
			if (ext.equalsIgnoreCase(oldExt)) {
				return path.substring(0, dotIndex + 1) + newExt;
			}
		}
		return null;
	}
}
