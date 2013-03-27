package util.backend;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import play.Logger;

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

	public static InputStream getMindMapInputStream(InputStream zipStream,final StringBuilder outFileName) throws IOException {
        //File mindmapFile = null;
        ZipInputStream zin = new ZipInputStream(zipStream);
        ZipEntry entry;
        String name;
        while ((entry = zin.getNextEntry()) != null)
        {
        	
            name = entry.getName();
            if(!name.endsWith(".inf")) {
            	Logger.debug("ZipUtils.extractMindmap => map with name "+name+" found!");
            	outFileName.append(name);
            	return zin;
                //mindmapFile = extractFileToTempFolder(zin,name);
                //break;
            }

        }
        zin.close();
        return null;
	}
}
