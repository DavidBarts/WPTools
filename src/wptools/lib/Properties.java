/*
 * This software is distributed under the Creative Commons Attribution 4.0
 * International license. See LICENSE.TXT in the main directory of this
 * repository for more information.
 */

package wptools.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.apache.commons.cli.CommandLine;

/**
 * Properties, which can come from either a standard properties file
 * or from command-line options.
 * 
 * @author David Barts <david.w.barts@gmail.com>
 *
 */
public class Properties {
	private static final String DEFAULT_PROPS = "wptools.properties";
	private java.util.Properties rawProps;
	private CommandLine rawCmdLine;
	
	public Properties(CommandLine args) {
		super();
		rawCmdLine = args;
		rawProps = new java.util.Properties();
		String specified = args.getOptionValue("properties");
		File dprops = new File(System.getProperty("user.home"), DEFAULT_PROPS);
		if (specified != null && !(new File(specified).exists())) {
			Misc.die("'" + specified + "' not found");
		} else if(!dprops.exists()) {
			return;
		}
		if (specified == null)
			specified = dprops.getPath();
		try (BufferedReader rdr = new BufferedReader(new FileReader(specified))) {
			rawProps.load(rdr);
		} catch (IOException e) {
			Misc.die(e.getMessage());
		}
	}
	
	public String get(String key) {
		String ret = rawCmdLine.getOptionValue(key);
		if (ret != null)
			return ret;
		String group = rawCmdLine.getOptionValue("group");
		ret = rawProps.getProperty(group == null ? key : group + "." + key);
		if (ret == null)
			Misc.die(key + " not specified anywhere!");
		return ret;
	}
	
	public int getInt(String key) {
		String ret = get(key);
		try {
			return Integer.parseInt(ret);
		} catch (NumberFormatException e) {
			Misc.die(String.format("invalid value for property %s: %s", key, ret));
		}
		return 0;  /* not reached */
	}
}

	