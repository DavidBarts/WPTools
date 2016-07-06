/*
 * This software is distributed under the Creative Commons Attribution 4.0
 * International license. See LICENSE.TXT in the main directory of this
 * repository for more information.
 */

package wptools.lib;

import java.util.*;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

/**
 * A subclass of the Commons CLI Options class which knows about our
 * standard options.
 * 
 * @author david.w.barts@gmail.com
 *
 */
public class Options extends org.apache.commons.cli.Options {
	
	private static final Map<String, Option> OMAP =
		Collections.unmodifiableMap(new HashMap<String, Option>() {{ 
			put("url", new Option("u", "url", true, "URL to blog XML-RPC service."));
			put("username", new Option("U", "username", true, "Username to log in as."));
			put("blogid", new Option("b", "blogid", true, "ID of blog."));
			put("properties", new Option("p", "properties", true, "Name of properties file."));
			put("group", new Option("g", "group", true, "Properties group to use."));
			put("bare", new Option("B", "bare", false, "Suppress headers and footers."));
		}});
		
	public Options () {
		super();
		addOption("?", "help", false, "Print this help message.");
	}
	
	public Options addOption(String name) {
		return (Options) addOption(OMAP.get(name));
	}
	
	public CommandLine parse(String[] args) {
		CommandLine ret =  null;
		try {
			ret = ((new DefaultParser()).parse(this, args));
		} catch (ParseException e) {
			Misc.die(e.getMessage(), 2);
		}
		if (ret.hasOption("help")) {
			(new HelpFormatter()).printHelp(Misc.getMyName(), this);
			System.exit(0);
		}
		return ret;
	}
}
