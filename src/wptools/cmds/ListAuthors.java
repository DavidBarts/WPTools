/*
 * This software is distributed under the Creative Commons Attribution 4.0
 * International license. See LICENSE.TXT in the main directory of this
 * repository for more information.
 */

package wptools.cmds;

import java.net.MalformedURLException;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import wptools.lib.*;

public class ListAuthors {
	private static CommandLine cmdLine;
	private static Properties props;

	public static void main(String[] args) {
		Misc.setMyName("ListAuthors");
		
		// Parse command-line options
		Options options = new Options();
		options.addOption("url");
		options.addOption("username");
		options.addOption("blogid");
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
		int blogid = props.getInt("blogid");
		Object[] params = new Object[] { blogid, props.get("username"),
				Misc.getPassword() };
		Object[] results = null;
		try {
			results = (Object[]) conn.execute("wp.getAuthors", params);
		} catch (XmlRpcException e) {
			Misc.die(e.getMessage());
		}
		
		// Issue warning and exit now if nothing returned
		if (results.length == 0) {
			Misc.die("warning - no results returned", 0);
		}
		
		// Print a header unless we're in bare mode.
		if (!cmdLine.hasOption("bare")) {
			System.out.format("Authors at %s :%n", url);
			System.out.format("%8s %-30s %s%n", "ID", "AUTHOR", "NAME");
		}

		// Dump results
		for (Object rresult : results) {
			Map<String, Object> result = (Map<String, Object>) rresult;
			System.out.format("%8s %-30s %s%n", result.get("user_id"),
				result.get("user_login"), result.get("display_name"));
		}

		// Print a footer unless we're in bare mode
		if (!cmdLine.hasOption("bare")) {
			int nr = results.length;
			System.out.format("%d author%s total.%n", nr, nr == 1 ? "" : "s");
		}
	}
}
