package com.github.jasypt.encrypt

import com.tvd12.properties.file.reader.MultiFileReader
import org.gradle.testfixtures.ProjectBuilder
import com.github.jasypt.encrypt.tasks.file.EncryptPropertiesFileTask
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor

class PropertiesEncryptSpec extends BaseSpecTemplate {

    def "Test taskAction() - encrypt yaml/properties files"() {
        given: "Prepare project"
        File resourceDir = new File("src/test/resources")
        def project = ProjectBuilder.builder().build()
        copyDirectory(resourceDir, project.getRootDir())

        when: 'Encode some properties'
        project.getPlugins().apply(JasyptPlugin)
        assert project.tasks.names.contains(EncryptPropertiesFileTask.TASK_NAME)

        def encryptPropsTask = project.tasks.getByName(EncryptPropertiesFileTask.TASK_NAME) as EncryptPropertiesFileTask
        encryptPropsTask.password = 'password'
        encryptPropsTask.taskAction()

        then: "Check that properties correctly mapped to the encryptor config"
        def encryptor = encryptPropsTask.getEncryptor() as PooledPBEStringEncryptor
        def config = encryptor.config
        config != null
        config.algorithm == 'PBEWITHMD5ANDDES'
        config.providerName == 'SunJCE'
        config.poolSize == 5
        config.keyObtentionIterations == 2000
        config.saltGenerator.getClass().getName() == 'org.jasypt.salt.ZeroSaltGenerator'
        config.ivGenerator.getClass().getName() == 'org.jasypt.iv.NoIvGenerator'

        and: "Check encrypted values"
        def yaml = new MultiFileReader().read(new File(project.getRootDir(), "application.yaml"))
        yaml.get('some.very.secret.property') == 'ENC(Lk5VWETH98C0/E/wOqzioQ==)'
        yaml.get('not.secret.property') == 'public'

        def props = new MultiFileReader().read(new File(project.getRootDir(), "application.properties"))
        props.get('some.very.secret.property') == 'ENC(Lk5VWETH98C0/E/wOqzioQ==)'
        props.get('not.secret.property') == 'public'

        and: "Check console output"
        checkOutMessage()
    }

}
