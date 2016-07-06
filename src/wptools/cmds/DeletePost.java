/*
 * This software is distributed under the Creative Commons Attribution 4.0
 * International license. See LICENSE.TXT in the main directory of this
 * repository for more information.
 */

package wptools.cmds;

import java.net.MalformedURLException;
import org.apache.commons.cli.CommandLine;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;

import wptools.lib.Misc;
import wptools.lib.Options;
import wptools.lib.Properties;

/**
 * Delete one or more posts.
 * 
 * @author David Barts <david.w.barts@gmail.com>
 *
 */
public class DeletePost {
	private static CommandLine cmdLine;
	private static Properties props;
	
	public static void main(String[] args) {
		// Define our name
		Misc.setMyName("DeletePost");

		// Parse command-line options
		Options options = new Options();
		options.addOption("url");
		options.addOption("username");
		options.addOption("blogid");
		options.addOption("properties");
		options.addOption("group");
		cmdLine = options.parse(args);

		// Load properties from wherever (file or command line)
		props = new Properties(cmdLine);
		
		// Get client object
		String url = props.get("url");
		String username = props.get("username");
		int blogid = props.getInt("blogid");
		Misc.disableSslAuth();
		XmlRpcClient client = null;
		try {
		    client = Misc.xmlRpcService(url);
		} catch (MalformedURLException e) {
			Misc.die(e.getMessage());
		}
		
		// Delete
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
			Object[] params = new Object[] { blogid, username, password, postid };
			try {
				client.execute("wp.deletePost", params);
			} catch (XmlRpcException e) {
				Misc.error(rpostid + " - " + e.getMessage());
				estat = 1;
				continue;
			}
			System.out.println("Post " + rpostid + " deleted.");
		}
		System.exit(estat);
	}
}
