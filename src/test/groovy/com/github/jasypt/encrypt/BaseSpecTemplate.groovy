package com.github.jasypt.encrypt

import groovy.io.FileType
import org.junit.Before
import spock.lang.Specification

import java.util.regex.Pattern

class BaseSpecTemplate extends Specification {

    static final ByteArrayOutputStream OUT_CONTENT = new ByteArrayOutputStream()

    @Before
    void setup() {
        System.setOut(new PrintStream(OUT_CONTENT))
        OUT_CONTENT.reset()
    }

    public static final String NEW_LINE_REGEX = "\\r|\\n"

    static def extractFromOutput(String rawValue) {
        return rawValue ? rawValue.substring(rawValue.lastIndexOf(" ")).replaceAll("\\s|\\n|\\r", "") : null
    }

    static void copyDirectory(File dirFrom, File dirTo) {
        if (!dirTo.exists()) {// creation the target dir
            dirTo.mkdir()
        }
        dirFrom.eachFile(FileType.FILES) { File source -> // copying the daughter files
            File target = new File(dirTo, source.getName())
            target.bytes = source.bytes;
        }
        dirFrom.eachFile(FileType.DIRECTORIES) { File source -> // copying the daughter dirs - recursion
            File target = new File(dirTo, source.getName());
            copyDirectory(source, target)
        }
    }

    static boolean checkOutMessage() {
        def str = OUT_CONTENT.toString().replaceAll(NEW_LINE_REGEX, "")
        return str.contains('No of files found: 4') &&
        str.contains('No of values changed: 2') &&
        str.contains('Files processed:') &&
        str.contains('[application.properties]') &&
        str.contains('[application.yaml]') &&
        str.contains('[config.yaml]') &&
        str.contains('[file-access.properties]')
    }
}
