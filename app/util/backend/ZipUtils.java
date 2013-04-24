package util.backend;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import play.Logger;

public class ZipUtils {

	/**
	 * 
	 * @param zipStream
	 * @param outFileName
	 * @return stream of mindmap file or null if none is present 
	 * @throws IOException
	 */
	public static InputStream getMindMapInputStream(InputStream zipStream, final StringBuilder outFileName) throws IOException {
		ZipInputStream zin = new ZipInputStream(zipStream);
		ZipEntry entry;
		String name;
		try {
			while ((entry = zin.getNextEntry()) != null) {

				name = entry.getName();
				if (!name.endsWith(".inf")) {
					Logger.debug("ZipUtils.extractMindmap => map with name " + name + " found!");
					outFileName.append(name);
					return zin;
				}

			}

		} finally {
			zin.close();
		}
		return null;
	}
}
