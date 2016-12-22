import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class QueryMaker {

	public static JSONObject lookUpItunes(JSONObject jo) throws MalformedURLException, IOException, ParseException {
		String s = jo.get("collectionId").toString();
		return lookUpItunes(s);
	}

	public static JSONObject lookUpItunes(String s) throws MalformedURLException, IOException, ParseException {
		JSONObject result = Crawler.saveFile(new URL(Toolbox.lookupPrefix + s + "&entity=song"),
				Toolbox.SEARCH_RESULT_FILE);
		return result;
	}

	public static JSONObject searchItunes(Tag tag)
			throws MalformedURLException, IOException, ParseException, KeyNotFoundException, FieldDataInvalidException {

		String s = tag.getFirst(FieldKey.ALBUM);
		JSONObject result = Crawler.saveFile(new URL(Toolbox.getSearchableString(s)), Toolbox.SEARCH_RESULT_FILE);

		if (result == null)
			return null;

		JSONArray ja = (JSONArray) (result.get("results"));

		Printer.printSearchResults(ja, tag);
		int index = findClosestResult(ja, tag);
		
		if (index == -1) {
			return Toolbox.setFlaggedCase(tag);
		}
		
		JSONObject found = (JSONObject) ja.get(index);

		System.out.println("Is " + found.get("collectionName") + " by " + found.get("artistName") + " the album?");
		System.out.println("If not enter the correct entry number:");
		
		String line = Toolbox.getSystemInput().readLine();
		
		if (line.length() > 0) {
			int x = Integer.parseInt(line);
			if (x == -1)
				return Toolbox.setFlaggedCase(tag);
			found = (JSONObject) ja.get(x);
		}

		return lookUpItunes(found);

	}

	public static void find(JSONArray item, Tag tag)
			throws NumberFormatException, IOException, KeyNotFoundException, FieldDataInvalidException {

		Printer.printAlbum(item, tag);
		int index = findClosestSong(item, tag);

		if (index == -1) {
			Toolbox.setFlaggedCase(tag);
			return;
		}

		JSONObject found = (JSONObject) item.get(index);

		System.out.println("Is " + found.get("trackNumber") + " " + found.get("trackName") + " the song?");
		System.out.println("If not enter the correct entry number:");
		String line = Toolbox.getSystemInput().readLine();
		if (line.length() > 0) {
			int x = Integer.parseInt(line);
			if (x == -1) {
				Toolbox.setFlaggedCase(tag);
				return;
			}
			index = x;
		}

		found = (JSONObject) item.get(index);

		for (FieldKey fk : FieldKey.values())
			tag.deleteField(fk);
		Toolbox.setMp3Tags(tag, found);
		Crawler.setPrevPath(found.get("collectionId").toString());

	}

	private static int findClosestResult(JSONArray ja, Tag tag) {

		int distance = Integer.MAX_VALUE;
		int index = -1;

		for (int i = 0; i < ja.size(); i++) {
			JSONObject jobj = (JSONObject) ja.get(i);

			int d = Toolbox.levenshteinDistance(jobj.get("collectionName").toString(), tag.getFirst(FieldKey.ALBUM));

			if (d < distance) {
				distance = d;
				index = i;
			}

		}

		return index;

	}

	private static int findClosestSong(JSONArray item, Tag tag) {

		int index = -1;
		int distance = Integer.MAX_VALUE;

		for (int i = 0; i < item.size(); i++) {
			JSONObject jobj = (JSONObject) item.get(i);
			if (!jobj.get("wrapperType").toString().equals("track"))
				continue;

			int d = Toolbox.levenshteinDistance(jobj.get("trackName").toString(), tag.getFirst(FieldKey.TITLE));

			if (d < distance) {
				index = i;
				distance = d;
			}

		}

		return index;

	}

}
