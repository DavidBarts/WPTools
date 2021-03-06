/*
 * This software is distributed under the Creative Commons Attribution 4.0
 * International license. See LICENSE.TXT in the main directory of this
 * repository for more information.
 */

package wptools.cmds;

import java.net.MalformedURLException;
import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;

import wptools.lib.*;

/**
 * Edit (i.e. update, this is NOT an interactive editor) a post.
 * 
 * @author David Barts <david.w.barts@gmail.com>
 *
 */
public class EditPost {
	private static CommandLine cmdLine;
	private static Properties props;
	
	public static void main(String[] args) {
		// Define our name
		Misc.setMyName("EditPost");

		// Parse command-line options
		NewEditOptions options = new NewEditOptions();
		options.addOption("n", "no-content", false, "Leave post content alone.");
		cmdLine = options.parse(args);

		// Reject attempts to edit nothing
		int nargs = cmdLine.getArgs().length;
		if (nargs == 0)
			Misc.die("expecting a post ID");
		
		// Reject contradictory options
		if (cmdLine.hasOption("content") && cmdLine.hasOption("no-content"))
			Misc.die("--content and --no-content are mutually exclusive");

		// Load properties from wherever (file or command line)
		props = new Properties(cmdLine);
		
		// Get client object
		String url = props.get("url");
		String username = props.get("username");
		int blogid = props.getInt("blogid");
		XmlRpcClient client = null;
		try {
		    client = Misc.xmlRpcService(url, props, cmdLine);
		} catch (MalformedURLException e) {
			Misc.die(e.getMessage());
		}
		
		// Detect no-ops.
		boolean found = false;
		for (String opt : NewEditOptions.FIELDS) {
			if (cmdLine.hasOption(opt)) {
				found = true;
				break;
			}
		}
		
		// Build the map of content parameters
		HashMap<String, Object> contentParams = new HashMap<String, Object>();
		options.addToMap(contentParams, cmdLine);
		if (!cmdLine.hasOption("no-content"))
			contentParams.put("post_content", Misc.readBody(cmdLine));
		else if (!found)
			Misc.die("warning - no edits specified", 0);
		
		// Edit the posts
		String password = Misc.getPassword();
		int estat = 0;
		for (String rpostid : cmdLine.getArgs()) {
			Integer postid = null;
			try {
				postid = new Integer(rpostid);
			} catch (NumberFormatException e) {
				Misc.error(rpostid + " - illegal post ID");
				estat = 1;
				continue;
			}
			Object[] params = new Object[] { blogid, username, password, postid, contentParams };
			try {
				client.execute("wp.editPost", params);
			} catch (XmlRpcException e) {
				Misc.error(rpostid + " - " + e.getMessage());
				estat = 1;
				continue;
			}
			System.out.println("Post " + rpostid + " edited.");
		}
		System.exit(estat);
	}
}
