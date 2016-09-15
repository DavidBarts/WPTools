/*
 * This software is distributed under the Creative Commons Attribution 4.0
 * International license. See LICENSE.TXT in the main directory of this
 * repository for more information.
 */

package wptools.cmds;

import wptools.lib.*;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;

/**
 * List some posts in reverse chronological order. The scope of the
 * listing is by default limited.
 * 
 * @author David Barts <david.w.barts@gmail.com>
 *
 */
public class ListPosts {
	private static CommandLine cmdLine;
	private static Properties props;
	
	// Standard filter we apply to listings when retrieving them
	private static Map<String, Object> stdFilter =
		new HashMap<String, Object>() {{
			put("orderby", "date");
			put("order", "DESC");
		}};
	
	// Standard fields we retrieve
	private static final String[] STD_FIELDS = { "post_id", "post_title",
		"post_modified", "post_author" };

	public static void main(String[] args) {
		// Define our name
		Misc.setMyName("ListPosts");
		
		// Parse command-line options
		Options options = new Options();
		options.addOption("url");
		options.addOption("username");
		options.addOption("blogid");
		options.addOption("properties");
		options.addOption("group");
		options.addOption("bare");
		options.addOption("c", "count", true, "Maximum return result count.");
		cmdLine = options.parse(args);
		
		// Load properties from wherever (file or command line)
		props = new Properties(cmdLine);
		
		// Try to get a "connection" (actually just a client object; HTTP is connectionless).
		XmlRpcClient conn = null;
		String url = props.get("url");
		try {
			conn = Misc.xmlRpcService(url, props, cmdLine);
		} catch (MalformedURLException e) {
			Misc.die(e.getMessage());
		}
		
		// If a count was specified, add it.
		if (cmdLine.hasOption("count")) {
			String rcount = cmdLine.getOptionValue("count");
			int count = 0;
			try {
				count = Integer.parseInt(rcount);
			} catch (NumberFormatException e) {
				Misc.die("illegal count: " + rcount);
			}
			stdFilter.put("number", count);
		}

		// Issue query
		int blogid = props.getInt("blogid");
		Object[] params = new Object[] { blogid,
			props.get("username"), Misc.getPassword(), stdFilter,
			STD_FIELDS };
		Object[] results = null;
		try {
			results = (Object[]) conn.execute("wp.getPosts", params);
		} catch (XmlRpcException e) {
			Misc.die(e.getMessage());
		}
		
		// Issue warning and exit now if nothing returned
		if (results.length == 0) {
			Misc.die("warning - no results returned", 0);
		}
		
		// Print a header unless we're in bare mode.
		if (!cmdLine.hasOption("bare")) {
			System.out.format("Entries in blog %d at %s :%n", blogid, url);
			System.out.format("%8s %8s %-40s %s%n", "ID", "AUTHOR", "TITLE",
				"TIME");
		}
		
		// List the results
		for (Object rresult : results) {
			Map<String, Object> result = (Map<String, Object>) rresult;
			System.out.format("%8s %8s %-40s %s%n", result.get("post_id"),
				result.get("post_author"),
				Misc.truncateString((String) result.get("post_title"), 40),
				Misc.formatDate((Date) result.get("post_modified"), "J"));
		}
		
		// Print a footer unless we're in bare mode.
		if(!cmdLine.hasOption("bare")) {
			int n = results.length;
			System.out.format("%d entr%s listed.%n", n, n == 1 ? "y" : "ies");
		}
	}
}
