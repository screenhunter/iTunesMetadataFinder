import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.json.simple.JSONObject;

public class Toolbox {

	public static final String searchPrefix = "https://itunes.apple.com/search?";
	public static final String searchPostfix = "&entity=album";
	public static final String lookupPostfix = "&entity=song";
	public static final String lookupPrefix = "https://itunes.apple.com/lookup?id=";
	public static final String library = "/Volumes/Samsung USB/Dropbox/iTunes/Music/";
	public static final String SEARCH_RESULT_FILE = "results.json";
	public static final String checkString = "checked";
	public static final String FLAG = "FLAGGED";
	private static final BufferedReader sysin = new BufferedReader(new InputStreamReader(System.in));

	public static int levenshteinDistance(CharSequence lhs, CharSequence rhs) {
		int len0 = lhs.length() + 1;
		int len1 = rhs.length() + 1;

		// the array of distances
		int[] cost = new int[len0];
		int[] newcost = new int[len0];

		// initial cost of skipping prefix in String s0
		for (int i = 0; i < len0; i++)
			cost[i] = i;

		// dynamically computing the array of distances

		// transformation cost for each letter in s1
		for (int j = 1; j < len1; j++) {
			// initial cost of skipping prefix in String s1
			newcost[0] = j;

			// transformation cost for each letter in s0
			for (int i = 1; i < len0; i++) {
				// matching current letters in both strings
				int match = (lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1;

				// computing cost for each transformation
				int cost_replace = cost[i - 1] + match;
				int cost_insert = cost[i] + 1;
				int cost_delete = newcost[i - 1] + 1;

				// keep minimum cost
				newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
			}

			// swap cost/newcost arrays
			int[] swap = cost;
			cost = newcost;
			newcost = swap;
		}

		// the distance is the cost for transforming all letters in both strings
		return cost[len0 - 1];
	}

	public static BufferedReader getSystemInput() {
		return sysin;
	}

	public static void closeSystemInput() throws IOException {
		sysin.close();
	}

	public static int[] timeMaker(int total) {

		int[] time = new int[2];
		time[0] = total / 60;
		time[1] = total % 60;

		return time;

	}
	
	public static JSONObject setFlaggedCase(Tag tag) throws KeyNotFoundException, FieldDataInvalidException {
		tag.setField(FieldKey.CUSTOM1, Toolbox.FLAG);
		return null;
	}

	public static String getSearchableString(String s) {
		for (int i = 0; i < s.length(); i++) {
			while (s.contains("(") && s.contains(")"))
				s = s.substring(0, s.indexOf('(')) + s.substring(s.indexOf(')') + 1);
			while (s.contains("  "))
				s = s.replaceAll("  ", " ");
			s = s.replaceAll(" ", "+");
			if (s.charAt(i) != '+' && !(Character.isLetterOrDigit(s.charAt(i))))
				s = s.substring(0, i) + s.substring(i + 1);
		}
		return searchPrefix + "term=" + s + searchPostfix;

	}

	public static void setMp3Tags(Tag tag, JSONObject cur) throws KeyNotFoundException, FieldDataInvalidException {
		tag.setField(FieldKey.ALBUM, cur.get("collectionName").toString());
		if (cur.get("collectionArtistName") != null)
			tag.setField(FieldKey.ALBUM_ARTIST, cur.get("collectionArtistName").toString());
		tag.setField(FieldKey.TITLE, cur.get("trackName").toString());
		// String s = cur.get("artworkUrl30").toString();
		// s = s.substring(0, s.length()-11) + "1000x1000bb.jpg";
		// System.out.println(s);
		// tag.addField(ArtworkFactory.createLinkedArtworkFromURL(s));
		tag.setField(FieldKey.TRACK, cur.get("trackNumber").toString());
		tag.setField(FieldKey.ARTIST, cur.get("artistName").toString());
		tag.setField(FieldKey.GENRE, cur.get("primaryGenreName").toString());
		tag.setField(FieldKey.YEAR, cur.get("releaseDate").toString().substring(0, 4));
		tag.setField(FieldKey.DISC_NO, cur.get("discNumber").toString());
		tag.setField(FieldKey.TRACK_TOTAL, cur.get("trackCount").toString());
		tag.setField(FieldKey.DISC_TOTAL, cur.get("discCount").toString());
		tag.setField(FieldKey.COUNTRY, cur.get("country").toString());
		tag.setField(FieldKey.CATALOG_NO, cur.get("trackId").toString());
	}

	public static void writeToMp3(Tag tag, AudioFile aFile) throws CannotWriteException {
		aFile.setTag(tag);
		AudioFileIO.write(aFile);
	}

}
