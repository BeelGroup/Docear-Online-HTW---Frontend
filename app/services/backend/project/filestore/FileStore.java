package services.backend.project.filestore;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public interface FileStore {
    /**
     * Creates a new file in the file store.
     * The behaviour for recreating an existing file is undefined.
     * @param path hierarchical path for the file
     * @return the stream to write the file
     * @throws IOException
     */
    DataOutputStream create(String path) throws IOException;

    /**
     * Reads an existing file from the file store.
     * @param path hierarchical path for the file
     * @return a stream to read the data
     * @throws IOException
     * @throws FileNotFoundException
     */
    DataInputStream open(String path) throws IOException, FileNotFoundException;
    
    /**
     * Moves a file to another location
     * @param fromPath hierarchical path for existing file
     * @param toPath hierarchical path for new file
     * @throws IOException
     * @throws FileNotFoundException
     */
    void move(String fromPath, String toPath) throws IOException, FileNotFoundException;
    
    
    void delete(String path) throws IOException, FileNotFoundException;
}
