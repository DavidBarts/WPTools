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
 * Create a new post.
 * 
 * @author David Barts <david.w.barts@gmail.com>
 *
 */
public class NewPost {
	private static CommandLine cmdLine;
	private static Properties props;
	
	public static void main(String[] args) {
		// Define our name
		Misc.setMyName("NewPost");

		// Parse command-line options
		NewEditOptions options = new NewEditOptions();
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
		
		// Build the map of content parameters
		HashMap<String, Object> contentParams = new HashMap<String, Object>();
		options.addToMap(contentParams, cmdLine);
		contentParams.put("post_content", Misc.readBody());
		
		// Post the article
		String password = Misc.getPassword();
		Object[] params = new Object[] { blogid, username, password, contentParams };
		String postid = null;
		try {
			postid = (String) client.execute("wp.newPost", params);
		} catch (XmlRpcException e) {
			Misc.die(e.getMessage());
		}
		System.out.println("Post " + postid + " created.");
	}
}
