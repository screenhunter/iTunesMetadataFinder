import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Printer {

	public static void printAlbum(JSONArray item, Tag tag) {

		for (int i = 0; i < item.size(); i++) {
			JSONObject jobj = (JSONObject) item.get(i);
			
			if (!jobj.get("wrapperType").toString().equals("track"))
				continue;
			
			int trackNumber = Integer.parseInt(jobj.get("trackNumber").toString());
			printSpace(trackNumber + ". ", 4);
			printSpace(jobj.get("trackName"), 70);
			printSpace(jobj.get("artistName"), 70);

			int[] time = Toolbox.timeMaker(Integer.parseInt(jobj.get("trackTimeMillis").toString()) / 1000);
			printSpace(createTimeString(time[0], time[1]), 6);

			int d = Toolbox.levenshteinDistance(jobj.get("trackName").toString(), tag.getFirst(FieldKey.TITLE));
			System.out.println(d);
		}

	}

	public static void printSearchResults(JSONArray ja, Tag tag) {

		for (int i = 0; i < ja.size(); i++) {
			JSONObject jobj = (JSONObject) ja.get(i);
			
			printSpace(i + ". ", 4);
			printSpace(jobj.get("collectionName"), 70);
			printSpace(jobj.get("artistName"), 70);

			int d = Toolbox.levenshteinDistance(jobj.get("collectionName").toString(), tag.getFirst(FieldKey.ALBUM));
			System.out.println(d);
		}
		
	}
	
	public static void printMp3Data(AudioFile aFile) {
		int[] time = Toolbox.timeMaker(aFile.getAudioHeader().getTrackLength());
		System.out.println(aFile.getFile().getName());
		System.out.println(createTimeString(time[0], time[1]) + "\n");
	}
	
	public static String createTimeString(int min, int sec) {
		return (min < 10 ? "0" + min : "" + min) + ":" + (sec < 10 ? "0" + sec : "" + sec);
	}
	
	private static void printSpace(Object o, int size) {
		String s = o.toString();
		System.out.print(s);
		for (int j = s.length(); j < size; j++)
			System.out.print(" ");
	}

}
