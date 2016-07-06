/*
 * This software is distributed under the Creative Commons Attribution 4.0
 * International license. See LICENSE.TXT in the main directory of this
 * repository for more information.
 */

package wptools.lib;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.cli.CommandLine;

/**
 * Because the NewPost and EditPost commands have lots of options,
 * all in common.
 * @author David Barts <david.w.barts@gmail.com>
 *
 */
public class NewEditOptions extends Options {
	// Exceptions to the normal rules for turning option names into
	// parameter names for XML-RPC calls.
	private static final Map<String, String> EXCEPTIONS =
		Collections.unmodifiableMap(new HashMap<String, String>() {{ 
			put("sticky", "sticky");
			put("terms", "terms_names");
			put("comment-status", "comment_status");
			put("ping-status", "ping_status");
			put("date", "post_date_gmt");  // post_date is braindamged, avoid
		}});
	
	// Arguments used to specify post fields.
	public static final String[] FIELDS = { "type", "status", "title",
		"author", "excerpt", "date", "date-gmt", "format", "name",
		"comment-status", "ping-status", "sticky", "thumbnail", "parent",
		"terms"	};
	
	// Build the map that governs how we parse headers.
	private interface HeaderParser {
		public Object parse(String value) throws ParseException;
	}
	private static final HeaderParser PARSE_INT = new HeaderParser() {
		public Object parse(String value) throws ParseException {
			try {
				return new Integer(value);
			} catch (NumberFormatException e) {
				throw new ParseException("Invalid integer: " + value, 0);
			}
		}
	};
	private static final HeaderParser PARSE_GMT_DATE = new HeaderParser() {
		public Object parse(String value) throws ParseException {
			return Misc.parseDate(value, "Z");
		}
	};
	// We have to do some bizarre stuff here due to some idiot using
	// java.lang.Date to represent an XMLRPC time (the former is tied
	// to a time zone, the latter is not). Not only that, there's also
	// apparently date/time braindamage on the Wordpress side, too.
	// C'est le groan.
	private static final HeaderParser PARSE_LOCAL_DATE = new HeaderParser() {
		public Object parse(String value) throws ParseException {
			Date ret = Misc.parseDate(value, "J");
			long time = ret.getTime();
			int offset = TimeZone.getDefault().getOffset(time);
			return new Date(time - offset);
		}
	};
	private static final HeaderParser PARSE_TERMS = new HeaderParser() {
		public Object parse(String value) throws ParseException {
			HashMap<String, ArrayList<String>> rret = new HashMap<String, ArrayList<String>>();;;
			for (String rpair : value.split(",\\s*")) {
				int equals = rpair.indexOf('.');
				if (equals <= 0 || equals == rpair.length()-1)
					throw new ParseException("Invalid terms: " + value, 0);
				String key = rpair.substring(0, equals);
				String val = rpair.substring(equals+1);
				if (!rret.containsKey(key))
					rret.put(key, new ArrayList<String>());
				rret.get(key).add(val);
			}
			HashMap<String, String[]> ret = new HashMap<String, String[]>();
			for (String key: rret.keySet()) {
				ArrayList<String> val = rret.get(key);
				ret.put(key, val.toArray(new String[val.size()]));
			}
			return ret;
		}
	};
	private static final Map<String, HeaderParser> PARSERS =
		Collections.unmodifiableMap(new HashMap<String, HeaderParser>() {{
			put("author", PARSE_INT);
			put("date", PARSE_LOCAL_DATE);
			put("date-gmt", PARSE_GMT_DATE);
			put("sticky", PARSE_INT);
			put("thumbnail", PARSE_INT);
			put("parent", PARSE_INT);
			put("terms", PARSE_TERMS);
		}});
	
	/**
	 * Constructor.
	 */
	public NewEditOptions() {
		super();
		// Standard options
		addOption("url");
		addOption("username");
		addOption("blogid");
		addOption("properties");
		addOption("group");
		// Our special ones
		for (String field : FIELDS) {
			addOption(null, field, true,
				"Specify " + optionToField(field) + " field.");
		}
	}
	
	/**
	 * Convert an option name to the name of a field we pass to Wordpress.
	 * @param option  Option name
	 * @return Field name
	 */
	public String optionToField(String option) {
		String exc = EXCEPTIONS.get(option);
		if (exc != null)
			return exc;
		return "post_" + option.replace('-', '_');
	}
	
	/**
	 * Parse arguments, doing standard processing of them.
	 */
	public CommandLine parse(String[] args) {
		CommandLine ret = super.parse(args);
		if (ret.hasOption("date") && ret.hasOption("date-gmt"))
			Misc.die("--date and --date-gmt are mutually exclusive");
		return ret;
	}
	
	/**
	 * Given a map representing a XML-RPC struct, add items to it based
	 * on the arguments we have parsed,
	 * @param targ    Map which is added to.
	 * @param cmd     CommandLine object from parsed args.
	 */
	public void addToMap(Map<String, Object> targ, CommandLine cmd) {
		for (String option : FIELDS) {
			if (!cmd.hasOption(option))
				continue;
			String unparsed = cmd.getOptionValue(option);
			String field = optionToField(option);
			HeaderParser p = PARSERS.get(option);
			if (p != null) {
				Object parsed = null;
				try {
					parsed = p.parse(unparsed);
				} catch (ParseException e) {
					Misc.die(String.format("illegal value for %s: %s",
						option, unparsed));
				}
				targ.put(field, parsed);
			} else {
				targ.put(field, unparsed);
			}
		}
	}
}
