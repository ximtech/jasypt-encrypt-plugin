package com.github.jasypt.encrypt.tasks.file;

import com.github.jasypt.encrypt.tasks.JasyptConfig;
import com.tvd12.properties.file.mapping.PropertiesMapper;
import com.tvd12.properties.file.reader.MultiFileReader;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.options.Option;
import com.github.jasypt.encrypt.tasks.PasswordAwareTask;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.slf4j.Marker;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class PropertiesFileAwareTask extends PasswordAwareTask {

    private static final Pattern PROPERTIES_PATTERN = Pattern.compile(".*\\.properties|.*\\.ya?ml");
    private static final Set<String> EXCLUDED_DIRECTORIES = new HashSet<>(Arrays.asList(".gradle", "build", "out", "target", ".idea", "gradle"));
    private static final int TAB_WHITESPACE_INDENTATION_COUNT = 4;
    private static final int SINGLE_WHITESPACE_INDENTATION_COUNT = 1;
    
    private Pattern valueExtractorPattern;
    private String fileFilterPattern;
    
    static final class FilePropertiesHolder {
        private final List<String> fileLines;
        private final AtomicInteger linesEncrypted;

        public FilePropertiesHolder(List<String> fileLines, AtomicInteger linesEncrypted) {
            this.fileLines = fileLines;
            this.linesEncrypted = linesEncrypted;
        }

        public List<String> getFileLines() {
            return fileLines;
        }
        public AtomicInteger getLinesEncrypted() {
            return linesEncrypted;
        }
    }

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
                try (FileInputStream fileInputStream = new FileInputStream(matching.toFile());
                     InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
                     BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

                    List<String> allLines = bufferedReader.lines().collect(Collectors.toList());
                    String fileExtension = getFileExtensionByName(matching.toFile().getName());
                    boolean isYamlFile = fileExtension.equals("yml") || fileExtension.equals("yaml");
                    FilePropertiesHolder resultFile = isYamlFile ? handleYamlFile(encryptor, allLines) : handlePropertiesFile(encryptor, allLines);
                    
                    if (resultFile.getLinesEncrypted().get() > 0) {
                        Files.write(matching, resultFile.getFileLines(), StandardCharsets.UTF_8);
                    }
                    encryptedLinesCount.addAndGet(resultFile.getLinesEncrypted().get());
                }
            }
            logProcessedStatus(matchingPaths, encryptedLinesCount);

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
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    Objects.requireNonNull(dir);
                    Objects.requireNonNull(attrs);
                    if (EXCLUDED_DIRECTORIES.contains(dir.getFileName().toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    Objects.requireNonNull(file);
                    Objects.requireNonNull(attrs);
                    if (attrs.isRegularFile()
                        && PROPERTIES_PATTERN.matcher(file.getFileName().toString()).matches()
                        && (fileFilterPattern == null || file.getFileName().toString().matches(fileFilterPattern))) {
                        propertyPaths.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    Objects.requireNonNull(file);
                    getLogger().error(Marker.ANY_MARKER, exc);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
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

    private void logProcessedStatus(List<Path> matchingPaths, AtomicInteger encryptedLinesCount) {
        System.out.println("No of files found: " + matchingPaths.size());
        System.out.println("No of values changed: " + encryptedLinesCount.get());
        if (matchingPaths.size() > 0) {
            String filesProcessed = matchingPaths.stream()
                    .map((Path path) -> path.getFileName().toString())
                    .map((String fileName) -> "[" + fileName + "]")
                    .collect(Collectors.joining("\r\n"));
            System.out.println("Files processed:\r\n" + filesProcessed);
        }
    }

    private FilePropertiesHolder handlePropertiesFile(PBEStringEncryptor encryptor, List<String> allLines) {
        List<String> resultFileLines = new ArrayList<>(allLines.size());
        AtomicInteger encryptedLinesCount = new AtomicInteger();
        for (int i = 0; i < allLines.size(); i++) {
            String line = allLines.get(i);

            if (isPropertiesMultiline(line)) {
                int indentationLevel = getLineIndentationLevel(allLines.get(i + 1));
                List<String> multilineList = propertiesMultilineToList(allLines, i);
                StringBuffer multilineAsSingleLine = convertMultilinesToSingleLine(multilineList);
                int lineLength = getMaxLineLength(multilineList);

                Matcher matcher = getValueExtractorPattern().matcher(multilineAsSingleLine);
                if (matcher.find()) {
                    resultFileLines.add(line);
                    String extractedValue = matcher.group(1);
                    String encryptedValue = getPropertyPrefix() + process(encryptor, extractedValue) + getPropertySuffix();

                    List<String> encryptedLineAsMultilineList = splitLongStringToMultiline(encryptedValue, lineLength);
                    List<String> encryptedLinesWithPadding = formatPropertyMultiline(encryptedLineAsMultilineList, indentationLevel);
                    resultFileLines.addAll(encryptedLinesWithPadding);
                    encryptedLinesCount.incrementAndGet();
                    i += multilineList.size();
                    continue;
                }
            }
            handleSingleLine(encryptor, encryptedLinesCount, resultFileLines, line);
        }

        return new FilePropertiesHolder(resultFileLines, encryptedLinesCount);
    }

    private List<String> propertiesMultilineToList(List<String> allLines, int lineIndex) {
        List<String> multilineList = new ArrayList<>();
        for (int multilineIndex = lineIndex + 1; multilineIndex < allLines.size(); multilineIndex++) {
            String multiline = allLines.get(multilineIndex).trim();
            if (!multiline.endsWith("\\")) {
                multilineList.add(multiline);
                break;
            }
            multilineList.add(multiline.substring(0, multiline.length() - 1));
        }
        
        return multilineList;
    }

    private FilePropertiesHolder handleYamlFile(PBEStringEncryptor encryptor, List<String> allLines) {
        List<String> resultFileLines = new ArrayList<>(allLines.size());
        AtomicInteger encryptedLinesCount = new AtomicInteger();
        for (int i = 0; i < allLines.size(); i++) {
            String line = allLines.get(i);

            if (isYamlMultiline(line)) {
                int indentationLevel = getLineIndentationLevel(allLines.get(i + 1));
                List<String> multilineList = yamlMultilineToList(allLines, i, indentationLevel);
                StringBuffer multilineAsSingleLine = convertMultilinesToSingleLine(multilineList);
                int lineLength = getMaxLineLength(multilineList);

                Matcher matcher = getValueExtractorPattern().matcher(multilineAsSingleLine);
                if (matcher.find()) {
                    resultFileLines.add(line);
                    String extractedValue = matcher.group(1);
                    String encryptedValue = getPropertyPrefix() + process(encryptor, extractedValue) + getPropertySuffix();

                    List<String> encryptedLineAsMultilineList = splitLongStringToMultiline(encryptedValue, lineLength);
                    List<String> encryptedLinesWithPadding = formatYamlMultiline(encryptedLineAsMultilineList, indentationLevel);
                    resultFileLines.addAll(encryptedLinesWithPadding);
                    encryptedLinesCount.incrementAndGet();
                    i += multilineList.size();
                    continue;
                }
            }
            handleSingleLine(encryptor, encryptedLinesCount, resultFileLines, line);
        }

        return new FilePropertiesHolder(resultFileLines, encryptedLinesCount);
    }

    private void handleSingleLine(PBEStringEncryptor encryptor, AtomicInteger encryptedLinesCount, List<String> resultFileLines, String line) {
        Matcher matcher = getValueExtractorPattern().matcher(line);
        if (matcher.find()) {
            String extractedValue = matcher.group(1);
            String matchGroup = matcher.group();
            String encryptedValue = getPropertyPrefix() + process(encryptor, extractedValue) + getPropertySuffix();
            resultFileLines.add(line.replace(matchGroup, encryptedValue));
            encryptedLinesCount.incrementAndGet();
        } else {
            resultFileLines.add(line);
        }
    }

    private List<String> yamlMultilineToList(List<String> allLines, int lineIndex, int indentationLevel) {
        List<String> multilineList = new ArrayList<>();
        for (int multilineIndex = lineIndex + 1; multilineIndex < allLines.size(); multilineIndex++) {
            String multiline = allLines.get(multilineIndex);
            int whitespaceCount = getLineIndentationLevel(multiline);

            if (multiline.trim().isEmpty() || whitespaceCount != indentationLevel) {
                break;
            }
            String trimmedLine = multiline.trim();
            multilineList.add(trimmedLine);
        }
        
        return multilineList;
    }

    private StringBuffer convertMultilinesToSingleLine(List<String> multilineList) {
        StringBuffer multilineAsSingleLine = new StringBuffer();
        multilineList.forEach(multilineAsSingleLine::append);
        return multilineAsSingleLine;
    }

    private boolean isYamlMultiline(String line) {
        return line.trim().endsWith("|");
    }

    private boolean isPropertiesMultiline(String line) {
        return line.trim().endsWith("\\");
    }

    private String getFileExtensionByName(String fileName) {
        return Optional.ofNullable(fileName)
                .filter((String name) -> name.contains("."))
                .map((String name) ->  name.substring(fileName.lastIndexOf(".") + 1))
                .orElse(null);
    }

    private int getMaxLineLength(List<String> multilineList) {
        return multilineList.stream()
                .mapToInt(String::length)
                .max()
                .orElse(0);
    }

    private int getLineIndentationLevel(String line) {
        int indentationLength = 0;
        for (Character lineChar : line.toCharArray()) {
            if (!Character.isWhitespace(lineChar)) {
                break;
            }
            int whitespaceLength = lineChar == '\t' ? TAB_WHITESPACE_INDENTATION_COUNT : SINGLE_WHITESPACE_INDENTATION_COUNT;
            indentationLength += whitespaceLength;
        }
        
        return indentationLength;
    }

    private List<String> splitLongStringToMultiline(String encryptedLine, int lineLength) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < encryptedLine.length(); i += lineLength) {
            int substringLength = Math.min(encryptedLine.length(), i + lineLength);
            result.add(encryptedLine.substring(i, substringLength));
        }
        return result;
    }

    private List<String> formatYamlMultiline(List<String> multilineList, int indentationLevel) {
        return multilineList.stream()
                .map((String line) -> {
                    String whitespacePadding = String.join("", Collections.nCopies(indentationLevel, " "));
                    return whitespacePadding.concat(line);
                }).collect(Collectors.toList());
    }

    private List<String> formatPropertyMultiline(List<String> multilineList, int indentationLevel) {
        List<String> resultList = new ArrayList<>(multilineList.size());
        for (int i = 0; i < multilineList.size(); i++) {
            String line = multilineList.get(i);
            String whitespacePadding = String.join("", Collections.nCopies(indentationLevel, " "));
            if (i == multilineList.size() - 1) {
                resultList.add(whitespacePadding.concat(line));
                break;
            }

            resultList.add(whitespacePadding.concat(line).concat("\\"));
        }
        return resultList;
    }
}
