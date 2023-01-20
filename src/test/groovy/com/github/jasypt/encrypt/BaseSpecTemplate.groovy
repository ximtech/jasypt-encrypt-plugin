package com.github.jasypt.encrypt

import groovy.io.FileType
import org.junit.Before
import spock.lang.Specification

class BaseSpecTemplate extends Specification {

    static final def OUT_MESSAGE =
                    'No of files found: 4\r\n' +
                    'No of values changed: 2\r\n' +
                    'Files processed:\r\n' +
                    '[application.properties]\r\n' +
                    '[application.yaml]\r\n' +
                    '[config.yaml]\r\n' +
                    '[file-access.properties]\r\n'

    static final ByteArrayOutputStream OUT_CONTENT = new ByteArrayOutputStream()

    @Before
    void setup() {
        System.setOut(new PrintStream(OUT_CONTENT))
        OUT_CONTENT.reset()
    }

    public static final String WHITESPACE_REGEX = "\\s|\\n|\\r"

    static def extractFromOutput(String rawValue) {
        return rawValue ? rawValue.substring(rawValue.lastIndexOf(" ")).replaceAll(WHITESPACE_REGEX, "") : null
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
}
