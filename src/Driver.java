import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Driver {

	public static final String searchPrefix = "https://itunes.apple.com/search?";
	public static final String searchPostfix = "&entity=album";
	public static final String lookupPostfix = "&entity=song";
	public static final String lookupPrefix = "https://itunes.apple.com/lookup?id=";
	public static final String library = "/Volumes/Samsung USB/Dropbox/iTunes/Music/";
	public static final String searchResult = "results.json";
	public static final String checkString = "checked";
	public static final String FLAG = "FLAGGED";
	private static JSONParser parser;
	private static BufferedReader sysin;
	private static String prevAlbum = "";
	private static String prevPath = "";
	private static PrintWriter flagOut;

	public static void main(String[] args) throws IOException, CannotReadException, TagException, ReadOnlyFileException,
			InvalidAudioFrameException, ParseException, CannotWriteException {
		sysin = new BufferedReader(new InputStreamReader(System.in));
		flagOut = new PrintWriter(new BufferedWriter(new FileWriter("flaggedList.txt")));
//		URL url = new URL(searchPrefix + "term=jack+johnson");
//		saveFile(url, searchResult);
		mp3Finder(new File(library));
		sysin.close();
		flagOut.close();
	}

	private static void mp3Finder(File f) throws IOException, CannotReadException, TagException, ReadOnlyFileException,
			InvalidAudioFrameException, ParseException, CannotWriteException {
		File[] fileList = f.listFiles();
		for (File cur : fileList) {
			if (cur.isDirectory())
				mp3Finder(cur);
			else if (cur.getName().substring(cur.getName().lastIndexOf('.')).equalsIgnoreCase(".mp3")) {
				System.out.println(cur.getName());
				AudioFile aFile = AudioFileIO.read(cur);
				Tag tag = aFile.getTag();
				int total = aFile.getAudioHeader().getTrackLength();
				int min = total / 60;
				int sec = total % 60;
				System.out.println((min < 10 ? "0" + min : "" + min) + ":" + (sec < 10 ? "0" + sec : "" + sec) + "\n");
				JSONObject item = null;
				if (checkModified(tag))
					item = checkHelper(tag);
				else if (cur.getParentFile().getAbsolutePath().equals(prevPath))
					item = lookUpItunes(tag);
				else
					item = searchItunes(tag);
				prevPath = "";
				prevAlbum = "";
				if (item != null) {
					tag = find((JSONArray) (item.get("results")), tag);
					aFile.setTag(tag);
					AudioFileIO.write(aFile);
					prevPath = cur.getParentFile().getAbsolutePath();
				} else
					tag.setField(FieldKey.CUSTOM1, FLAG);
				aFile.setTag(tag);
				AudioFileIO.write(aFile);
				System.out.println("\n\n");
			}
		}
	}

	private static boolean checkModified(Tag tag) throws KeyNotFoundException, FieldDataInvalidException, IOException {
		if (tag.getFirst(FieldKey.CATALOG_NO).compareTo("0") >= 0)
			return true;
		if (tag.getFirst(FieldKey.CUSTOM1).equals(FLAG)) {
			return true;
		}
		return false;
	}

	private static JSONObject checkHelper(Tag tag) throws NumberFormatException, IOException, ParseException {
		if (tag.getFirst(FieldKey.CATALOG_NO).compareTo("0") >= 0)
			return null;
		if (tag.getFirst(FieldKey.CUSTOM1).equals(FLAG)) {
			System.out.println("Enter the itunes ID:");
			String line = sysin.readLine();
			if (line.length() == 0) {
				System.out.println("Error!");
				flagOut.println(tag.getFirst(FieldKey.ALBUM) + " - " + tag.getFirst(FieldKey.TITLE) + " - "
					+ tag.getFirst(FieldKey.ARTIST));
				return null;
			}
			return lookUpItunes(line);
		}
		return null;
	}

	private static Tag find(JSONArray item, Tag tag)
			throws NumberFormatException, IOException, KeyNotFoundException, FieldDataInvalidException {

		int index = -1;
		int distance = Integer.MAX_VALUE;
		JSONObject found = null;
		for (int i = 0; i < item.size(); i++) {
			JSONObject jobj = (JSONObject) item.get(i);
			if (!jobj.get("wrapperType").toString().equals("track"))
				continue;
			int trackNumber = Integer.parseInt(jobj.get("trackNumber").toString());
			System.out.print(trackNumber + ". ");
			if (trackNumber < 10)
				System.out.print(" ");
			System.out.print(jobj.get("trackName"));
			for (int j = jobj.get("trackName").toString().length(); j < 70; j++)
				System.out.print(" ");
			System.out.print(" " + jobj.get("artistName"));
			for (int j = jobj.get("artistName").toString().length(); j < 70; j++)
				System.out.print(" ");

			int total = Integer.parseInt(jobj.get("trackTimeMillis").toString()) / 1000;
			int min = total / 60;
			int sec = total % 60;
			System.out.print((min < 10 ? "0" + min : "" + min) + ":" + (sec < 10 ? "0" + sec : "" + sec) + " ");

			int d = levenshteinDistance(jobj.get("trackName").toString(), tag.getFirst(FieldKey.TITLE));
			System.out.println(d);

			if (d < distance) {
				index = i;
				distance = d;
				found = jobj;
			}

			// System.out.println(jobj.toString());

			// System.out.println(jobj.keySet());

		}

		if (found == null) {
			tag.setField(FieldKey.CUSTOM1, FLAG);
			return tag;
		}

		System.out.println("Is " + found.get("trackNumber") + " " + found.get("trackName") + " the song?");
		System.out.println("If not enter the correct entry number:");
		String line = sysin.readLine();
		if (line.length() > 0) {
			int x = Integer.parseInt(line);
			if (x == -1) {
				tag.setField(FieldKey.CUSTOM1, FLAG);
				return tag;
			}
			index = x;
		}

		JSONObject cur = (JSONObject) item.get(index);

		for (FieldKey fk : FieldKey.values())
			tag.deleteField(fk);
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
		prevAlbum = cur.get("collectionId").toString();

		return tag;

	}

	private static JSONObject lookUpItunes(Tag tag) throws MalformedURLException, IOException, ParseException {

		JSONObject result = saveFile(new URL(lookupPrefix + prevAlbum + "&entity=song"), searchResult);
		return result;
	}

	private static JSONObject lookUpItunes(JSONObject jo) throws MalformedURLException, IOException, ParseException {
		String s = jo.get("collectionId").toString();
		JSONObject result = saveFile(new URL(lookupPrefix + s + "&entity=song"), searchResult);
		return result;
	}

	private static JSONObject lookUpItunes(String s) throws MalformedURLException, IOException, ParseException {
		JSONObject result = saveFile(new URL(lookupPrefix + s + "&entity=song"), searchResult);
		return result;
	}

	private static JSONObject searchItunes(Tag tag)
			throws MalformedURLException, IOException, ParseException, KeyNotFoundException, FieldDataInvalidException {
		String s = tag.getFirst(FieldKey.ALBUM);
		JSONObject result = saveFile(new URL(searchPrefix + "term=" + getString(s) + searchPostfix), searchResult);
		if (result != null) {

			JSONArray ja = (JSONArray) (result.get("results"));

			int distance = Integer.MAX_VALUE;
			JSONObject found = null;

			for (int i = 0; i < ja.size(); i++) {
				JSONObject jobj = (JSONObject) ja.get(i);
				System.out.print(i + ". ");
				if (i < 10)
					System.out.print(" ");
				System.out.print(" " + jobj.get("collectionName"));
				for (int j = jobj.get("collectionName").toString().length(); j < 70; j++)
					System.out.print(" ");
				System.out.print(" " + jobj.get("artistName"));
				for (int j = jobj.get("artistName").toString().length(); j < 70; j++)
					System.out.print(" ");

				int d = levenshteinDistance(jobj.get("collectionName").toString(), tag.getFirst(FieldKey.ALBUM));
				System.out.println(d);

				if (d < distance) {
					distance = d;
					found = jobj;
				}

			}

			if (found == null) {
				tag.setField(FieldKey.CUSTOM1, FLAG);
				return null;
			}

			System.out.println("Is " + found.get("collectionName") + " by " + found.get("artistName") + " the album?");
			System.out.println("If not enter the correct entry number:");
			String line = sysin.readLine();
			if (line.length() > 0) {
				int x = Integer.parseInt(line);
				if (x == -1)
					return null;
				return lookUpItunes((JSONObject) ja.get(x));
			}

			return lookUpItunes(found);

		}
		return result;
	}

	private static String getString(String s) {
		for (int i = 0; i < s.length(); i++) {
			while (s.contains("(") && s.contains(")"))
				s = s.substring(0, s.indexOf('(')) + s.substring(s.indexOf(')') + 1);
			while (s.contains("  "))
				s = s.replaceAll("  ", " ");
			s = s.replaceAll(" ", "+");
			if (s.charAt(i) != '+' && !(Character.isLetterOrDigit(s.charAt(i))))
				s = s.substring(0, i) + s.substring(i + 1);
		}
		System.out.println("Searched String: " + s);
		return s;

	}

	private static JSONObject saveFile(URL url, String file) throws IOException, ParseException {
		// System.out.println("opening connection");
		InputStream in = url.openStream();
		FileOutputStream fos = new FileOutputStream(new File(file));

		// System.out.println("reading file...");
		int length = -1;
		byte[] buffer = new byte[1024];// buffer for portion of data from
		// connection
		while ((length = in.read(buffer)) > -1) {
			fos.write(buffer, 0, length);
		}

		fos.close();
		in.close();
		System.out.println("\nfile was downloaded\n");
		parser = new JSONParser();
		return (JSONObject) parser.parse(new FileReader(searchResult));
	}

	private static int levenshteinDistance(CharSequence lhs, CharSequence rhs) {
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

}
