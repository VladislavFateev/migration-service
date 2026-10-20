package ru.itfb.symphony.service.migration.utills;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.core.io.FileSystemResource;

import java.io.*;
import java.nio.file.Files;

public class DeleteAfterReadFileSystemResource extends FileSystemResource {

    private static final String DIRECTORY_EXPORT_STREAM = "export";

    public DeleteAfterReadFileSystemResource(File file) {
        super(file);
    }

    @Override
    public InputStream getInputStream() throws FileNotFoundException {
        return new DeleteOnCloseFileInputStream(super.getFile());
    }

    private static final class DeleteOnCloseFileInputStream extends FileInputStream {

        private File file;
        DeleteOnCloseFileInputStream(File file) throws FileNotFoundException {
            super(file);
            this.file = file;
        }

        @Override
        public void close() throws IOException {
            super.close();
            if (!file.getParentFile().getName().equals(DIRECTORY_EXPORT_STREAM)) {
                FileUtils.deleteDirectory(file.getParentFile());
            } else {
                file.delete();
            }
        }
    }
}
