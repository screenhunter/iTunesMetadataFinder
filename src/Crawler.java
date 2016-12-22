import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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

public class Crawler {

	private static PrintWriter flagOut;
	private static String prevAlbum = "";
	private static String prevPath = "";

	public static String getPrevAlbum() {
		return prevAlbum;
	}

	public static void setPrevAlbum(String s) {
		prevAlbum = s;
	}

	public static String getPrevPath() {
		return prevPath;
	}

	public static void setPrevPath(String s) {
		prevPath = s;
	}

	public static JSONObject saveFile(URL url, String file) throws IOException, ParseException {
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
		JSONParser parser = new JSONParser();
		return (JSONObject) parser.parse(new FileReader(Toolbox.SEARCH_RESULT_FILE));
	}

	public static void mp3Finder(File f) throws IOException, CannotReadException, TagException, ReadOnlyFileException,
			InvalidAudioFrameException, ParseException, CannotWriteException {

		File[] fileList = f.listFiles();
		flagOut = new PrintWriter(new BufferedWriter(new FileWriter("flaggedList.txt")));
		flagOut.println("test");

		for (File cur : fileList) {
			if (cur.isDirectory())
				mp3Finder(cur);
			else if (cur.getName().substring(cur.getName().lastIndexOf('.')).equalsIgnoreCase(".mp3")) {

				AudioFile aFile = AudioFileIO.read(cur);
				Printer.printMp3Data(aFile);

				Tag tag = aFile.getTag();
				JSONObject item = null;
				if (checkModified(tag))
					item = checkHelper(tag);
				else if (cur.getParentFile().getAbsolutePath().equals(prevPath))
					item = QueryMaker.lookUpItunes(prevAlbum);
				else
					item = QueryMaker.searchItunes(tag);

				prevPath = "";
				prevAlbum = "";

				if (item != null) {
					QueryMaker.find((JSONArray) (item.get("results")), tag);
					Toolbox.writeToMp3(tag, aFile);
					prevPath = cur.getParentFile().getAbsolutePath();
				} else
					tag.setField(FieldKey.CUSTOM1, Toolbox.FLAG);

				Toolbox.writeToMp3(tag, aFile);
				System.out.println("\n\n");

			}
		}
		flagOut.close();
	}

	private static boolean checkModified(Tag tag) throws KeyNotFoundException, FieldDataInvalidException, IOException {
		if (tag.getFirst(FieldKey.CATALOG_NO).compareTo("0") >= 0)
			return true;
		if (tag.getFirst(FieldKey.CUSTOM1).equals(Toolbox.FLAG)) {
			return true;
		}
		return false;
	}

	private static JSONObject checkHelper(Tag tag) throws NumberFormatException, IOException, ParseException {
		if (tag.getFirst(FieldKey.CATALOG_NO).compareTo("0") >= 0)
			return null;
		if (tag.getFirst(FieldKey.CUSTOM1).equals(Toolbox.FLAG)) {
			System.out.println("Enter the itunes ID:");
			//String line = Toolbox.getSystemInput().readLine();
			String line = "";
			if (line.length() == 0) {
				System.out.println("Error!");
				flagOut.println(tag.getFirst(FieldKey.ALBUM) + " - " + tag.getFirst(FieldKey.TITLE) + " - "
						+ tag.getFirst(FieldKey.ARTIST));
				flagOut.flush();
				return null;
			}
			return QueryMaker.lookUpItunes(line);
		}
		return null;
	}

}
