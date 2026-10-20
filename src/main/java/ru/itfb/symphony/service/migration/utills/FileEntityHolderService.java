package ru.itfb.symphony.service.migration.utills;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;
import ru.itfb.symphony.service.migration.json.JsonConverter;
import ru.itfb.symphony.service.migration.model.dto.MigrationTaskDto;
import ru.itfb.symphony.service.migration.pojo.FileHolderEntity;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileEntityHolderService {

    @Value("${import.file.path}")
    private String importPathFile;

    @Value("${export.file.path}")
    private String exportPathFile;

    private final JsonConverter jsonConverter;
    private final BuildProperties buildProperties;

    public List<FileHolderEntity> takeDataFromFile(File file) {
        List<FileHolderEntity> result = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(file.getPath()))) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        String content = "";
                        try {
                            content = new String(Files.readAllBytes(path));
                        } catch (IOException e) {
                            log.error("File not found!");
                            throw new RuntimeException(e);
                        }
                        FileHolderEntity fileHolderEntity = jsonConverter.toFileHolderEntity(content);
                        if (checkFileVersionProject(fileHolderEntity)) {
                            result.add(fileHolderEntity);
                        }
                    });
        } catch (IOException e) {
            log.error("Files not found!");
            throw new RuntimeException(e);
        }
        return result;
    }

    private Boolean checkFileVersionProject(FileHolderEntity fileOrganizations) {
        String versionProject = buildProperties.getVersion();
        return fileOrganizations.getFileMetaData().getVersion().equals(versionProject);
    }

    public Boolean isZipFormatFile(File file) {
        return file.toPath().toString().endsWith(".zip");
    }

    private File createNewFile(File directory, ZipEntry entry) {
        File file = new File(directory, entry.getName());
        String directoryPath = null;
        try {
            directoryPath = directory.getCanonicalPath();
            String destFilePath = file.getCanonicalPath();
            if (!destFilePath.startsWith(directoryPath + File.separator)) {
                throw new IOException("Entry is outside of the target dir: " + entry.getName());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }

    public List<File> convertZipToFile(File zipFile) throws IOException {
        List<File> files = new ArrayList<>();
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry entry = zis.getNextEntry();
        while (entry != null) {
            File file = new File(importPathFile, UUID.randomUUID() + "-directory");
            File newFile = createNewFile(file, entry);
            File parent = newFile.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) {
                throw new IOException("Failed to create directory " + parent);
            }
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            entry = zis.getNextEntry();
            files.add(file);
        }
        zis.close();
        return files;
    }

    public Path createZipFile(MigrationTaskDto migrationTaskDto) {
        try {
            return Files.createFile(Paths.get(exportPathFile + File.separator + migrationTaskDto.getGuid() + ".zip"));
        } catch (IOException e) {
            log.error("Ошибка при создании файла архива");
            throw new RuntimeException(e);
        }
    }

    public File putFileToZip(Path zipPath, List<File> files)  {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            for (File file : files) {

                Path dataPath = file.toPath();
                Files.walk(dataPath)
                        .filter(path -> !Files.isDirectory(path))
                        .forEach(path -> {
                            ZipEntry zipEntry = new ZipEntry(dataPath.relativize(path).toString());
                            try {
                                zos.putNextEntry(zipEntry);
                                Files.copy(path, zos);
                                zos.closeEntry();
                            } catch (IOException e) {
                                System.err.println(e);
                            }
                        });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        deletedDirectories(files);
        return zipPath.toFile();
    }
    private void deletedDirectory(File directory) {
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void deletedDirectories(List<File> directories) {
        for (File directory : directories) {
            deletedDirectory(directory);
        }
    }
}
