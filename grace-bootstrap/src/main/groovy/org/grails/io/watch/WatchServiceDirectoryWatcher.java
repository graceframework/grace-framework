/*
 * Copyright 2014-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.io.watch;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a {@link AbstractDirectoryWatcher} that uses {@link java.nio.file.WatchService}.
 * This implementation is used for Java 7 and later.
 * @author Craig Andrews
 * @since 2.4
 * @see WatchServiceDirectoryWatcher
 * @see DirectoryWatcher
 */
public class WatchServiceDirectoryWatcher extends AbstractDirectoryWatcher {

    private static final Logger logger = LoggerFactory.getLogger(WatchServiceDirectoryWatcher.class);

    private Map<WatchKey, List<String>> watchKeyToExtensionsMap = new ConcurrentHashMap<>();

    private Set<Path> individualWatchedFiles = new HashSet<>();

    private final WatchService watchService;

    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    public WatchServiceDirectoryWatcher() {
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        while (this.active) {
            try {
                WatchKey watchKey = this.watchService.poll(this.sleepTime, TimeUnit.MILLISECONDS);
                if (watchKey != null) {
                    List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
                    for (WatchEvent<?> watchEvent : watchEvents) {
                        WatchEvent.Kind<?> kind = watchEvent.kind();
                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            // TODO how is this supposed to be handled? I think the best thing to do is ignore it, but I'm not positive
                            logger.warn("WatchService Overflow occurred");
                            continue;
                        }
                        WatchEvent<Path> pathWatchEvent = cast(watchEvent);
                        Path name = pathWatchEvent.context();
                        Path dir = (Path) watchKey.watchable();
                        Path child = dir.resolve(name).toAbsolutePath();
                        File childFile = child.toFile();
                        if (this.individualWatchedFiles.contains(child) || this.individualWatchedFiles.contains(child.normalize())) {
                            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                fireOnNew(childFile);
                            }
                            else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                fireOnChange(childFile);
                            }
                            else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                // do nothing... there's no way to communicate deletions
                            }
                        }
                        else {
                            List<String> fileExtensions = this.watchKeyToExtensionsMap.get(watchKey);
                            if (fileExtensions == null) {
                                logger.debug("WatchService received an event for a file/directory that it's not interested in.");
                            }
                            else {
                                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                    // new directory created, so watch its contents
                                    addWatchDirectory(child, fileExtensions);
                                    if (childFile.isDirectory() && childFile.exists()) {
                                        File[] files = childFile.listFiles();
                                        if (files != null) {
                                            for (File newFile : files) {
                                                if (isValidFileToMonitor(newFile, fileExtensions)) {
                                                    fireOnNew(newFile);
                                                }
                                            }
                                        }
                                    }
                                }
                                if (isValidFileToMonitor(childFile, fileExtensions)) {
                                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                        fireOnNew(childFile);
                                    }
                                    else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                        fireOnChange(childFile);
                                    }
                                    else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                        // do nothing... there's no way to communicate deletions
                                    }
                                }
                            }
                        }
                    }
                    watchKey.reset();
                }
            }
            catch (InterruptedException ignored) {
            }
        }
        try {
            this.watchService.close();
        }
        catch (IOException e) {
            logger.debug("Exception while closing watchService", e);
        }
    }

    @Override
    public void addWatchFile(File fileToWatch) {
        if (!isValidFileToMonitor(fileToWatch, Collections.singletonList("*"))) {
            return;
        }
        try {
            if (!fileToWatch.exists()) {
                return;
            }
            Path pathToWatch = fileToWatch.toPath().toAbsolutePath();
            this.individualWatchedFiles.add(pathToWatch);
            pathToWatch.getParent().register(this.watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addWatchDirectory(File dir, List<String> fileExtensions) {
        Path dirPath = dir.toPath();
        addWatchDirectory(dirPath, fileExtensions);
    }

    private void addWatchDirectory(Path dir, List<String> fileExtensions) {
        if (!isValidDirectoryToMonitor(dir.toFile())) {
            return;
        }
        try {
            //add the subdirectories too
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!isValidDirectoryToMonitor(dir.toFile())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    WatchKey watchKey = dir.register(WatchServiceDirectoryWatcher.this.watchService, StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
                    List<String> originalFileExtensions = WatchServiceDirectoryWatcher.this.watchKeyToExtensionsMap.get(watchKey);
                    if (originalFileExtensions == null) {
                        WatchServiceDirectoryWatcher.this.watchKeyToExtensionsMap.put(watchKey, fileExtensions);
                    }
                    else {
                        HashSet<String> newFileExtensions = new HashSet<>(originalFileExtensions);
                        newFileExtensions.addAll(fileExtensions);
                        WatchServiceDirectoryWatcher.this.watchKeyToExtensionsMap.put(watchKey,
                                Collections.unmodifiableList(new ArrayList<>(newFileExtensions)));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
