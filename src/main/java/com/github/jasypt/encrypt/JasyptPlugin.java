package com.github.jasypt.encrypt;

import com.github.jasypt.encrypt.tasks.file.DecryptPropertiesFileTask;
import com.github.jasypt.encrypt.tasks.text.DecryptTextTask;
import com.github.jasypt.encrypt.tasks.text.EncryptTextTask;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.util.GradleVersion;
import com.github.jasypt.encrypt.tasks.file.EncryptPropertiesFileTask;


public class JasyptPlugin implements Plugin<Project> {

    private static final String MIN_GRADLE_VERSION = "4.10";
    public static final String TASK_GROUP_NAME = "Jasypt Encryption";

    @Override
    public void apply(Project project) {
        verifyGradleVersion();

        project.getTasks().create(EncryptPropertiesFileTask.TASK_NAME, EncryptPropertiesFileTask.class);
        project.getTasks().create(DecryptPropertiesFileTask.TASK_NAME, DecryptPropertiesFileTask.class);
        project.getTasks().create(EncryptTextTask.TASK_NAME, EncryptTextTask.class);
        project.getTasks().create(DecryptTextTask.TASK_NAME, DecryptTextTask.class);
    }

    private void verifyGradleVersion() {
        if (GradleVersion.current().compareTo(GradleVersion.version(MIN_GRADLE_VERSION)) < 0) {
            throw new GradleException("Jasypt plugin requires Gradle " + MIN_GRADLE_VERSION +
                " or later. The current version is " + GradleVersion.current());
        }
    }

}
