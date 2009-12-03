package de.unisiegen.informatik.antex;

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
}
