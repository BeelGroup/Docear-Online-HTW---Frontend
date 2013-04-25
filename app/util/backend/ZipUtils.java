package util.backend;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;

import play.Logger;

public class ZipUtils {

	/**
	 * 
	 * @param zipStream
	 * @param outFileName
	 * @return stream of mindmap file or null if none is present
	 * @throws IOException
	 */
	public static byte[] getMindMapInputStream(InputStream zipStream, final StringBuilder outFileName) throws IOException {
		ZipInputStream zin = null;
		byte[] filebytes = null;
		try {
			zin = new ZipInputStream(zipStream);
			ZipEntry entry;
			String name;
			while ((entry = zin.getNextEntry()) != null) {

				name = entry.getName();
				if (!name.endsWith(".inf")) {
					Logger.debug("ZipUtils.extractMindmap => map with name " + name + " found!");
					outFileName.append(name);
					filebytes = IOUtils.toByteArray(zin);
				}

			}
		} finally {
			IOUtils.closeQuietly(zin);
		}

		return filebytes;
	}
}
