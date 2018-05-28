/*
 * This software is distributed under the Creative Commons Attribution 4.0
 * International license. See LICENSE.TXT in the main directory of this
 * repository for more information.
 */

package wptools.cmds;

import java.nio.charset.Charset;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import wptools.lib.*;

/**
 * Given an HTML fragment of the sort expected by NewPost (and returned
 * by GetPost), write to standard output an HTML document that can be
 * used to preview it. This is far easier to perform than the reverse
 * process of turning an HTML document into a fragment useful to NewPost.
 *
 * @author David Barts <david.w.barts@gmail.com>
 *
 */
public class FragToHtml {
    private static CommandLine cmdLine;

    public static void main(String[] args) {
        // Define our name
        Misc.setMyName("FragToHtml");

        // Parse command-line options
        org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();
        options.addOption("?", "help", false, "Print this help message.");
        options.addOption(null, "content", true, "File to read post content from.");
        options.addOption(null, "title", true, "Specify post title.");
        try {
            cmdLine = ((new DefaultParser()).parse(options, args));
        } catch (ParseException e) {
            Misc.die(e.getMessage(), 2);
        }
        if (cmdLine.hasOption("help")) {
            (new HelpFormatter()).printHelp(Misc.getMyName(), options);
            System.exit(0);
        }

        // Get post title
        String title = cmdLine.getOptionValue("title");
        if (title == null)
            title = "Untitled";
        title = htmlEncode(title);

        // Emit HTML
        System.out.println("<html>");
        System.out.println("  <head>");
        System.out.println("    <meta http-equiv=\"content-type\" content=\"text/html; charset="
            + Charset.defaultCharset().name() +"\"/>");
        System.out.println("    <title>Preview of " + title + "</title>");
        System.out.println("  </head><body>");
        System.out.println("    <h1>" + title + "</h1>");
        emitBody(Misc.readBody(cmdLine));
        System.out.println("  </body>");
        System.out.println("</html>");
    }

    private static String htmlEncode(String raw) {
        int length = raw.length();
        StringBuilder buf = new StringBuilder(length);
        for (int i=0; i<length; i++) {
            char ch = raw.charAt(i);
            switch (ch) {
            case '<':
                buf.append("&lt;");
                break;
            case '>':
                buf.append("&gt;");
                break;
            case '&':
                buf.append("&amp;");
                break;
            default:
                buf.append(ch);
            }
        }
        return buf.toString();
    }

    private static void emitBody(String body) {
        // Get a buffer to play with.
        StringBuilder buf = new StringBuilder(body);

        // Normalize newlines.
        gsub(buf, "\r\n", "\n");
        gsub(buf, "\r", "\n");
        int len = buf.length();
        if (len > 0 && buf.charAt(len-1) != '\n')
            buf.append('\n');

        // If we don't start with a tag or a double newline, we need
        // to force a leading paragraph tag.
        if (buf.length() > 0 && buf.charAt(0) != '<' && buf.indexOf("\n\n") != 0)
            buf.insert(0, "<p>");

        // Add paragraph tags at double line breaks.
        gsub(buf, "\n\n", "\n<p>");

        // Localize newlines, if needed.
        String localNl = System.lineSeparator();
        if (!"\n".equals(localNl))
            gsub(buf, "\n", localNl);

        // Emit the output.
        System.out.print(buf.toString());
    }

    private static void gsub(StringBuilder buf, String old, String repl) {
        int len = old.length();
        int delta = repl.length() - len + 1;
        int pos = 0;
        while (true) {
            pos = buf.indexOf(old, pos);
            if (pos < 0)
                break;
            buf.replace(pos, pos + len, repl);
            pos += delta;
        }
    }
}
