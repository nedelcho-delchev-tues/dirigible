package org.eclipse.dirigible.components.registry.watcher;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
public class RecursiveFolderWatcher implements DisposableBean {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(RecursiveFolderWatcher.class);

    private Path sourceDir;
    private Path targetDir;
    private WatchService watchService;
    private ExecutorService executorService;
    private final Map<WatchKey, Path> keyToPathMap = new HashMap<>();
    /** The repository. */
    private IRepository repository;

    @Autowired
    public RecursiveFolderWatcher(IRepository repository) {
        this.repository = repository;
    }

    /** Perform initial sync of all files and folders */
    private void initialSync() throws IOException {
        logger.info("Performing initial sync...");
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                syncFile(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = sourceDir.relativize(dir);
                Path targetSubDir = targetDir.resolve(relative);
                Files.createDirectories(targetSubDir);
                return FileVisitResult.CONTINUE;
            }
        });
        logger.info("Initial sync complete.");
    }

    /** Register directory and all sub-directories */
    private void registerAll(final Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /** Register single directory */
    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
        keyToPathMap.put(key, dir);
    }

    public void startWatching() throws IOException, InterruptedException {
        logger.info("Recursively watching: " + sourceDir);

        while (true) {
            WatchKey key = watchService.take();
            Path dir = keyToPathMap.get(key);
            if (dir == null) {
                logger.error("WatchKey not recognized!");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == OVERFLOW)
                    continue;

                Path name = (Path) event.context();
                Path sourcePath = dir.resolve(name);

                if (kind == ENTRY_CREATE) {
                    if (Files.isDirectory(sourcePath)) {
                        // Register new directory
                        registerAll(sourcePath);
                        // Also sync its contents
                        try {
                            Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
                                @Override
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                    syncFile(file);
                                    return FileVisitResult.CONTINUE;
                                }

                                @Override
                                public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes attrs) throws IOException {
                                    Path relative = sourceDir.relativize(d);
                                    Path targetSubDir = targetDir.resolve(relative);
                                    Files.createDirectories(targetSubDir);
                                    return FileVisitResult.CONTINUE;
                                }
                            });
                        } catch (IOException e) {
                            logger.error("Failed to sync new folder: " + sourcePath, e);
                        }
                    } else {
                        syncFile(sourcePath);
                    }
                } else if (kind == ENTRY_MODIFY) {
                    syncFile(sourcePath);
                } else if (kind == ENTRY_DELETE) {
                    deleteFile(sourcePath);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                keyToPathMap.remove(key);
                if (keyToPathMap.isEmpty()) {
                    break;
                }
            }
        }
    }

    private void syncFile(Path sourceFile) {
        try {
            if (Files.isDirectory(sourceFile))
                return;
            Path relative = sourceDir.relativize(sourceFile);
            Path targetFile = targetDir.resolve(relative);
            Files.createDirectories(targetFile.getParent());
            Files.copy(sourceFile, targetFile, REPLACE_EXISTING, COPY_ATTRIBUTES);
            logger.info("Synced: " + sourceFile + " → " + targetFile);
        } catch (IOException e) {
            logger.error("Failed to sync: " + sourceFile, e);
        }
    }

    private void deleteFile(Path sourceFile) {
        try {
            Path relative = sourceDir.relativize(sourceFile);
            Path targetFile = targetDir.resolve(relative);

            if (Files.isDirectory(targetFile)) {
                // Recursively delete directory and contents
                Files.walkFileTree(targetFile, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.deleteIfExists(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.deleteIfExists(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
                logger.info("Deleted directory: " + targetFile);
            } else {
                Files.deleteIfExists(targetFile);
                logger.info("Deleted file: " + targetFile);
            }
        } catch (IOException e) {
            logger.error("Failed to delete: " + sourceFile, e);
        }
    }

    /**
     * Initialize.
     *
     */
    public synchronized void initialize() {
        logger.debug("Initializing the External Registry file watcher...");

        executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> {
            try {
                String source = Configuration.get("DIRIGIBLE_REGISTRY_EXTERNAL_FOLDER");
                if (source != null) {
                    String target = this.repository.getInternalResourcePath(IRepositoryStructure.PATH_REGISTRY_PUBLIC);

                    this.sourceDir = Paths.get(source)
                                          .toAbsolutePath();
                    this.targetDir = Paths.get(target)
                                          .toAbsolutePath();

                    if (!Files.exists(sourceDir)) {
                        throw new IllegalArgumentException("Source folder does not exist: " + sourceDir);
                    }
                    if (!Files.exists(targetDir)) {
                        Files.createDirectories(targetDir);
                    }

                    if (this.watchService != null) {
                        logger.warn("[{}] has been initialized already. Existing watcher will be closes and a new one will be created.",
                                this);
                        destroy();
                    }

                    this.watchService = FileSystems.getDefault()
                                                   .newWatchService();

                    // Initial sync before watching
                    initialSync();

                    // Register watchers recursively
                    registerAll(sourceDir);

                    // Start actual watching
                    this.startWatching();
                }
            } catch (IOException | InterruptedException e) {
                logger.error("Error during initializing the Etenral Registry Watcher", e);
            }
        });
        logger.debug("Done initializing the External Registry file watcher.");
    }

    @Override
    public void destroy() throws IOException {
        logger.info("Destroying [{}}", this);

        watchService.close();
        watchService = null;

        executorService.shutdown();
        executorService = null;
    }
}
