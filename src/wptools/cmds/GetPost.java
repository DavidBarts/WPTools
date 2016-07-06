package wptools.cmds;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;

import wptools.lib.*;

public class GetPost {

	// Build the map that governs how we format headers.
	private interface HeaderFormatter {
		public String format(Object value);
	}
	
	private static final HeaderFormatter FORMAT_LOCAL = new HeaderFormatter() {
		public String format(Object value) {
			return Misc.formatDate((Date) value, "J");
		}
	};
	private static final HeaderFormatter FORMAT_UTC = new HeaderFormatter() {
		public String format(Object value) {
			return Misc.formatDate((Date) value, "Z");
		}
	};
	private static final HeaderFormatter FORMAT_TERMS = new HeaderFormatter() {
		public String format(Object value) {
			StringBuilder ret = new StringBuilder();
			boolean comma = false;
			for (Object oterm : (Object []) value) {
				Map<String, Object> term = (Map<String, Object>) oterm;
				if (comma)
					ret.append(", ");
				else
					comma = true;
				ret.append((String) term.get("taxonomy"));
				ret.append('.');
				ret.append((String) term.get("name"));
			}
			return ret.toString();
		}
	};
	
	private static final Map<String, HeaderFormatter> HEADER_PRINTING_MAP =
		Collections.unmodifiableMap(new HashMap<String, HeaderFormatter>() {{ 
			put("post_date", FORMAT_LOCAL);
			put("post_date_gmt", FORMAT_UTC);
			put("post_modified", FORMAT_LOCAL);
			put("post_modified_gmt", FORMAT_UTC);
			put("terms", FORMAT_TERMS);
		}});
	
	// Blog fields which don't correspond to the normal rules for printing
	// names of. Typically capitalization-related
	private static final Map<String, String> NAME_EXCEPTIONS =
		Collections.unmodifiableMap(new HashMap<String, String>() {{
			put("post_id", "Post-ID");
			put("post_date_gmt", "Post-Date-GMT");
			put("post_modified_gmt", "Post-Modified-GMT");
			put("post_mime_type", "Post-MIME-Type");
			put("guid", "GUID");
		}});

	// Standard blog entry fields.
	private static final String[] STD_FIELDS = { "post_id", "post_modified",
		"post_title", "terms", "post_content", "post_author" };
	
	// All blog entry fields
	private static final String[] ALL_FIELDS = { "post_id", "post_title",
		"post_date", "post_date_gmt", "post_modified", "post_modified_gmt",
		"post_status", "post_type", "post_format", "post_name",
		"post_author", "post_password", "post_excerpt", "post_content",
		"post_parent", "post_mime_type", "link", "guid", "menu_order",
		"comment_status", "ping_status", "sticky", "terms" };
	static {
		Arrays.sort(ALL_FIELDS);
	}
	
	private static final String CONTENT_FIELD = "post_content";
	private static final String[] CONTENT_FIELD_ONLY = { CONTENT_FIELD };
	
	private static CommandLine cmdLine;
	private static Properties props;
	private static int estat;
	private static String password;
	
	public static void main(String[] args) {
		// Define our name
		Misc.setMyName("GetPost");

		// Parse command-line options
		Options options = new Options();
		options.addOption("url");
		options.addOption("username");
		options.addOption("blogid");
		options.addOption("properties");
		options.addOption("group");
		options.addOption("bare");
		options.addOption("f", "full", false, "Display all headers.");
		cmdLine = options.parse(args);

		// Reject attempts to get nothing
		if (cmdLine.getArgs().length == 0)
			Misc.die("expecting one or more post IDs");

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

		// List the posts
		estat = 0;
		Misc.disableSslAuth();
		password = Misc.getPassword();
		boolean needblank = false;
		for (String rpostid : cmdLine.getArgs()) {
			int postid = 0;
			try {
				postid = Integer.parseInt(rpostid);
			} catch (NumberFormatException e) {
				Misc.error("bad post id: " + rpostid);
				estat = 1;
				continue;
			}
			if (needblank)
				System.out.println();
			else
				needblank = true;
			try {
			    listPost(conn, postid);
			} catch (XmlRpcException e) {
				Misc.error(rpostid + " - " + e.getMessage());
				estat = 1;
			}
		}
		
		// AMF...
		System.exit(estat);
	}
	
	/**
	 * Given a post ID, list a post.
	 * @param postid  Post ID.
	 */
	private static void listPost(XmlRpcClient conn, int postid) throws XmlRpcException {
		String[] fields = null;
		if (cmdLine.hasOption("bare"))
			fields = CONTENT_FIELD_ONLY;
		else if (cmdLine.hasOption("full"))
			fields = ALL_FIELDS;
		else
			fields = STD_FIELDS;
		Object[] params = new Object[]{ props.get("blogid"),
			props.get("username"), password, postid, fields };
		Map<String, Object> result = (Map<String, Object>) conn.execute("wp.getPost", params);
		
		if (!cmdLine.hasOption("bare"))
			printHeaders(result);
		System.out.println(result.get(CONTENT_FIELD));
	}
	
	/**
	 * Print the post headers.
	 * @param result  Result as returned from WordPress.
	 */
	private static void printHeaders(Map<String, Object> result) {
		for (String key : ALL_FIELDS) {
			if (!result.containsKey(key) || key.equals(CONTENT_FIELD))
				continue;
			Object hval = null;
			if (HEADER_PRINTING_MAP.containsKey(key)) {
				hval = HEADER_PRINTING_MAP.get(key).format(result.get(key));
			} else {
				hval = result.get(key);
				if (hval instanceof String && ((String) hval).length() == 0)
					hval = "(none)";
			}
			System.out.println(headerName(key) + ": " + hval.toString());
		}
		System.out.println();
	}
	
	/**
	 * Get a printable header name.
	 * @param internal The internal name that WordPress uses.
	 * @return         The name we print.
	 */
	private static String headerName(String internal) {
		// Check in the exceptions map first.
		String exc = NAME_EXCEPTIONS.get(internal);
		if (exc != null)
			return exc;
		
		// Else we format it the normal way.
		StringBuilder ret = new StringBuilder();
		boolean dash = false;
		for(String s : internal.split("_")) {
			if (dash)
				ret.append("-");
			else
				dash = true;
			if (s.length() > 0) {
				char[] sa = s.toCharArray();
				sa[0] = Character.toUpperCase(sa[0]);
				ret.append(sa);
			}
		}
		return ret.toString();
	}
}
