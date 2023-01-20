package com.github.jasypt.encrypt

import com.github.jasypt.encrypt.tasks.file.DecryptPropertiesFileTask
import com.tvd12.properties.file.reader.MultiFileReader
import org.gradle.testfixtures.ProjectBuilder
import com.github.jasypt.encrypt.tasks.file.EncryptPropertiesFileTask

class PropertiesDecryptSpec extends BaseSpecTemplate {

    def "Test taskAction() - decrypt yaml/properties files"() {
        given: "Prepare project"
        File resourceDir = new File("src/test/resources")
        def project = ProjectBuilder.builder().build()
        copyDirectory(resourceDir, project.getRootDir())

        when: 'Encode some properties'
        project.getPlugins().apply(JasyptPlugin)
        assert project.tasks.names.contains(EncryptPropertiesFileTask.TASK_NAME)
        assert project.tasks.names.contains(DecryptPropertiesFileTask.TASK_NAME)

        def encryptPropsTask = project.tasks.getByName(EncryptPropertiesFileTask.TASK_NAME) as EncryptPropertiesFileTask
        encryptPropsTask.password = 'password'
        encryptPropsTask.taskAction()

        then: "Check console output"
        checkOutMessage()
        OUT_CONTENT.reset()

        when: "Decode parameters"
        def decryptPropsTask = project.tasks.getByName(DecryptPropertiesFileTask.TASK_NAME) as DecryptPropertiesFileTask
        decryptPropsTask.password = 'password'
        decryptPropsTask.taskAction()

        then: "Check decrypted values"
        def yaml = new MultiFileReader().read(new File(project.getRootDir(), "application.yaml"))
        yaml.get('some.very.secret.property') == 'ENCRYPT("private")'
        yaml.get('not.secret.property') == 'public'

        def props = new MultiFileReader().read(new File(project.getRootDir(), "application.properties"))
        props.get('some.very.secret.property') == 'ENCRYPT("private")'
        props.get('not.secret.property') == 'public'

        and: "Check console output"
        checkOutMessage()
    }
}
