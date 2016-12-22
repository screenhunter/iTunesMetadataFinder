import java.io.File;
import java.io.IOException;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.json.simple.parser.ParseException;

public class Driver {

	public static void main(String[] args) throws IOException, CannotReadException, TagException, ReadOnlyFileException,
			InvalidAudioFrameException, ParseException, CannotWriteException {
		Crawler.mp3Finder(new File(Toolbox.library));
		Toolbox.closeSystemInput();
	}

}
