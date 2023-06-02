package com.github.jasypt.encrypt.tasks.file;

import com.github.jasypt.encrypt.tasks.JasyptConfig;
import com.tvd12.properties.file.mapping.PropertiesMapper;
import com.tvd12.properties.file.reader.MultiFileReader;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.options.Option;
import com.github.jasypt.encrypt.tasks.PasswordAwareTask;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.slf4j.Marker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class PropertiesFileAwareTask extends PasswordAwareTask {

    private static final Pattern PROPERTIES_PATTERN = Pattern.compile(".*\\.properties|.*\\.yaml");
    private static final Set<String> EXCLUDED_DIRECTORIES = new HashSet<>(Arrays.asList(".gradle", "build", "out", "target", ".idea", "gradle"));
    
    private Pattern valueExtractorPattern;
    private String fileFilterPattern;

    public abstract String process(PBEStringEncryptor encryptor, String extractedValue);

    @Internal
    public abstract String getPropertyPrefix();

    @Internal
    public abstract String getPropertySuffix();

    @Internal
    public abstract Pattern getDefaultExtractPattern();

    @Override
    public void taskAction() {
        try {

            List<Path> matchingPaths = listApplicationPropertyPaths();
            AtomicInteger encryptedLinesCount = new AtomicInteger();
            PBEStringEncryptor encryptor = resolvePropertyEncryptor(matchingPaths);
            for (Path matching : matchingPaths) {
                List<String> allLines = Files.readAllLines(matching, StandardCharsets.UTF_8);
                AtomicBoolean haveValueToEncrypt = new AtomicBoolean(false);
                allLines.replaceAll((String line) -> {
                    Matcher matcher = getValueExtractorPattern().matcher(line);
                    if (matcher.find()) {
                        String extractedValue = matcher.group(1);
                        String matchGroup = matcher.group();
                        String encryptedValue = getPropertyPrefix() + process(encryptor, extractedValue) + getPropertySuffix();
                        encryptedLinesCount.getAndIncrement();
                        haveValueToEncrypt.set(true);
                        return line.replace(matchGroup, encryptedValue);
                    }
                    return line;
                });

                if (haveValueToEncrypt.get()) {
                    Files.write(matching, allLines, StandardCharsets.UTF_8);
                }
            }
            logProcessedStatus(matchingPaths, encryptedLinesCount.get());

        } catch (IOException e) {
            getLogger().error(Marker.ANY_MARKER, e);
        }
    }

    @Option(option = "value-extract-pattern", description = "Regular expression to extract the plain text. Defaults to ENCRYPT((.*))")
    public void setValueRegex(String valueRegex) {
        if (valueRegex != null && !valueRegex.trim().isEmpty()) {
            this.valueExtractorPattern = Pattern.compile(valueRegex);
        }
    }

    @Option(option = "file-filter-pattern", description = "Include only these files")
    public void setFileFilterPattern(String fileFilterPattern) {
        this.fileFilterPattern = fileFilterPattern;
    }

    private List<Path> listApplicationPropertyPaths() {
        Path rootPath = Paths.get(getProject().getRootDir().toURI());
        List<Path> propertyPaths = new ArrayList<>();
        try {
            Files.walkFileTree(rootPath, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Objects.requireNonNull(dir);
                    Objects.requireNonNull(attrs);
                    if (EXCLUDED_DIRECTORIES.contains(dir.getFileName().toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Objects.requireNonNull(file);
                    Objects.requireNonNull(attrs);
                    if (attrs.isRegularFile()
                        && PATTERN.matcher(file.getFileName().toString()).matches()
                        && (fileFilterPattern == null || file.getFileName().toString().matches(fileFilterPattern))) {
                        propertyPaths.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    Objects.requireNonNull(file);
                    getLogger().error(Marker.ANY_MARKER, exc);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Objects.requireNonNull(dir);
                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            getLogger().error(Marker.ANY_MARKER, e);
        }
        return propertyPaths;
    }

    private Pattern getValueExtractorPattern() {
        if (valueExtractorPattern == null) {
            valueExtractorPattern = getDefaultExtractPattern();
        }
        return valueExtractorPattern;
    }

    private PBEStringEncryptor resolvePropertyEncryptor(List<Path> matchingPaths) {
        if (matchingPaths.isEmpty()) {
            return initEncryptor(JasyptConfig.DEFAULT_JASYPT_CONFIG);
        }
        String[] filePaths = matchingPaths.stream()
                .map(Path::toString)
                .toArray(String[]::new);
        Properties mergedProperties = new MultiFileReader()
                .read(filePaths).stream()
                .collect(Properties::new, Map::putAll, Map::putAll);
        JasyptConfig config = new PropertiesMapper()
                .data(mergedProperties)
                .map(JasyptConfig.class);
        return initEncryptor(config);
    }

    private void logProcessedStatus(List<Path> matchingPaths, int encryptedLinesCount) {
        System.out.println("No of files found: " + matchingPaths.size());
        System.out.println("No of values changed: " + encryptedLinesCount);
        if (matchingPaths.size() > 0) {
            String filesProcessed = matchingPaths.stream()
                    .map((Path path) -> path.getFileName().toString())
                    .map((String fileName) -> "[" + fileName + "]")
                    .collect(Collectors.joining("\r\n"));
            System.out.println("Files processed:\r\n" + filesProcessed);
        }
    }
}
