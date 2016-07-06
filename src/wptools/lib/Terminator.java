package wptools.lib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;

/**
 * A class to deal with the convoluted terms/taxonomies interface we
 * must contend with when creating or editing (but not retrieving!)
 * posts. Ah, orthogonality.
 * 
 * @author David Barts <david.w.barts@gmail.com>
 */
 public class Terminator {
	 private HashMap<String, String> taxMap;
	 private XmlRpcClient client;
	 private int blogid;
	 private String username;
	 private String password;
	 
	 public Terminator(XmlRpcClient client, int blogid, String username, String password)
			 throws XmlRpcException {
		 super();
		 this.client = client;
		 this.blogid = blogid;
		 this.username = username;
		 this.password = password;
		 taxMap = new HashMap<String, String>();
		 for (String taxonomy : getTaxonomies()) {
			 for (String term: getTerms(taxonomy)) {
				 String existing = taxMap.get(term);
				 if (existing != null) {
					 Misc.error(
						 String.format("warning - '%s' in %s duplicates %s, ignoring",
							 term, taxonomy, existing));
				 } else {
					 taxMap.put(term, taxonomy);
				 }
			 }
		 }
	 }
	 
	 /**
	  * Given a term, normalize it, i.e. make its case match an existing term.
	  * @param term    Term to normalize
	  * @return normalized term
	  * @throws IllegalArgumentException if term not found or ambiguous
	  */
	 public String normalize(String term) throws IllegalArgumentException {
		 String ret = null;
		 int matches = 0;
		 for (String normalized : taxMap.keySet()) {
			 if (term.equalsIgnoreCase(normalized)) {
				 matches++;
				 ret = normalized;
			 }
		 }
		 switch (matches) {
		 case 0:
			 throw new IllegalArgumentException("Term '" + term + "' not found.");
		 case 1:
			 return ret;
		 default:
			 throw new IllegalArgumentException("Term '" + term + "' is ambiguous.");
		 }
	 }
	 
	 /**
	  * Add a term to a map of terms we're building for an XML-RPC call.
	  * @param terms   Map we are building.
	  * @param term    Term to add
	  * @throws IllegalArgumentException if term not found
	  */
	 private void add(Map<String, String> terms, String term)
			 throws IllegalArgumentException {
		 String taxonomy = taxMap.get(term);
		 if (taxonomy == null)
			 throw new IllegalArgumentException("Term '" + term + "' not found.");
		 terms.put(taxonomy, term);
	 }
	 
	public Map<String, String> getTermsMap(String[] rawTerms) {
		HashMap<String, String> ret = new HashMap<String, String>();
		for (String rawTerm : rawTerms) {
			try {
				// first try for an exact match
				add(ret, rawTerm);
			} catch (IllegalArgumentException e) {
				try {
					// failing that, try an inexact one
					add(ret, normalize(rawTerm));
				} catch (IllegalArgumentException e2) {
					Misc.die("unknown or ambiguous term: " + rawTerm);
				}
			}
		}
		return ret;
	}
		
	 private List<String> getNames(Object[] results) {
		 ArrayList<String> ret = new ArrayList<String>();
		 for (Object rresult : results) {
			 Map<String, Object> result = (Map<String, Object>) rresult;
			 ret.add((String) result.get("name"));
		 }
		 return ret;
	 }
	 
	 private List<String> getTaxonomies() throws XmlRpcException {
		 Object[] params = new Object[]{ blogid, username, password };
		 return getNames(
			 (Object[]) client.execute("wp.getTaxonomies", params));
	 }
	 
	 private List<String> getTerms(String taxonomy) throws XmlRpcException {
		 Object[] params = new Object[]{ blogid, username, password, taxonomy };
		 return getNames(
			 (Object[]) client.execute("wp.getTerms", params));
	 }
}
