/*
 * This software is distributed under the Creative Commons Attribution 4.0
 * International license. See LICENSE.TXT in the main directory of this
 * repository for more information.
 */

package wptools.cmds;

import wptools.lib.*;
import java.net.MalformedURLException;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;

/**
 * List all known blogs.
 * 
 * @author David Barts <david.w.barts@gmail.com>
 *
 */
public class ListBlogs {
	private static CommandLine cmdLine;
	private static Properties props;

	public static void main(String[] args) {
		// Define our name
		Misc.setMyName("ListBlogs");

		// Parse command-line options
		Options options = new Options();
		options.addOption("url");
		options.addOption("username");
		options.addOption("properties");
		options.addOption("group");
		options.addOption("bare");
		cmdLine = options.parse(args);

		// Load properties from wherever (file or command line)
		props = new Properties(cmdLine);

		// Try to get a "connection" (actually just a client object; HTTP is connectionless).
		XmlRpcClient conn = null;
		String url = props.get("url");
		try {
			conn = Misc.xmlRpcService(url);
		} catch (MalformedURLException e) {
			Misc.die(e.getMessage());
		}

		// Issue query
		Misc.disableSslAuth();
		Object[] params = new Object[]{props.get("username"), Misc.getPassword()};
		Object[] results = null;
		try {
			results = (Object[]) conn.execute("wp.getUsersBlogs", params);
		} catch (XmlRpcException e) {
			Misc.die(e.getMessage());
		}

		// Issue warning and exit now if nothing returned
		if (results.length == 0) {
			Misc.die("warning - no results returned", 0);
		}

		// Print a header unless we're in bare mode.
		if (!cmdLine.hasOption("bare")) {
			System.out.format("Blogs at %s :%n", url);
			System.out.format("%8s %s%n", "ID", "BLOG");
		}

		// Dump results
		for (Object rresult : results) {
			Map<String, Object> result = (Map<String, Object>) rresult;
			System.out.format("%8s %s%n", result.get("blogid"), result.get("blogName"));
		}

		// Print a footer unless we're in bare mode
		if (!cmdLine.hasOption("bare")) {
			int nr = results.length;
			System.out.format("%d blog%s total.%n", nr, nr == 1 ? "" : "s");
		}
	}
}
