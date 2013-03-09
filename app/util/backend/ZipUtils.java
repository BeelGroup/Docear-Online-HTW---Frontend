package util.backend;

import org.apache.commons.io.FileUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils
{
	private static final int  BUFFER_SIZE = 4096;

	private static void extractFile(ZipInputStream in, File outdir, String name) throws IOException
	{
		byte[] buffer = new byte[BUFFER_SIZE];
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(outdir,name)));
		int count = -1;
		while ((count = in.read(buffer)) != -1)
			out.write(buffer, 0, count);
		out.close();
	}

	private static void mkdirs(File outdir,String path)
	{
		File d = new File(outdir, path);
		if( !d.exists() )
			d.mkdirs();
	}

	private static String dirpart(String name)
	{
		int s = name.lastIndexOf( File.separatorChar );
		return s == -1 ? null : name.substring( 0, s );
	}

	/***
	 * Extract zipfile to outdir with complete directory structure
	 * @param zipfile Input .zip file
	 * @param outdir Output directory
	 */
    public static void extract(File zipfile, File outdir) throws IOException {
        ZipInputStream zin = new ZipInputStream(new FileInputStream(zipfile));
        ZipEntry entry;
        String name, dir;
        while ((entry = zin.getNextEntry()) != null)
        {
            name = entry.getName();
            if( entry.isDirectory() )
            {
                mkdirs(outdir,name);
                continue;
            }
            /* this part is necessary because file entry can come before
             * directory entry where is file located
             * i.e.:
             *   /foo/foo.txt
             *   /foo/
             */
            dir = dirpart(name);
            if( dir != null )
                mkdirs(outdir,dir);

            extractFile(zin, outdir, name);
        }
        zin.close();
    }

	private static File extractFileToTempFolder(ZipInputStream in, String name) throws IOException
	{

		
		File f = new File(FileUtils.getTempDirectoryPath() +name);//TODO Julius, please test for platform independence on MS Windows
		if(f.exists()) {
			f.delete();
			f.createNewFile();
		}
		byte[] buffer = new byte[BUFFER_SIZE];
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
		int count = -1;
		while ((count = in.read(buffer)) != -1)
			out.write(buffer, 0, count);
		out.close();

		return f;
	}

	public static File extractMindmap(InputStream zipStream) throws IOException {
        File mindmapFile = null;
        ZipInputStream zin = new ZipInputStream(zipStream);
        ZipEntry entry;
        String name;
        while ((entry = zin.getNextEntry()) != null)
        {
            name = entry.getName();
            if(!name.endsWith(".inf")) {
                mindmapFile = extractFileToTempFolder(zin,name);
                break;
            }

        }
        zin.close();
        return mindmapFile;
	}
}
