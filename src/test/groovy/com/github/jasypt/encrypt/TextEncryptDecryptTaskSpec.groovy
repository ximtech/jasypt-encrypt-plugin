package com.github.jasypt.encrypt

import com.github.jasypt.encrypt.tasks.text.DecryptTextTask
import com.github.jasypt.encrypt.tasks.text.EncryptTextTask
import org.gradle.testfixtures.ProjectBuilder

class TextEncryptDecryptTaskSpec extends BaseSpecTemplate {

    def "Test taskAction() - should correctly encrypt/decrypt text"() {
        given: "Prepare project"
        def project = ProjectBuilder.builder().build()

        when: 'Encode some text'
        project.getPlugins().apply(JasyptPlugin)
        assert project.tasks.names.contains(EncryptTextTask.TASK_NAME)

        def encryptTextTask = project.tasks.getByName(EncryptTextTask.TASK_NAME) as EncryptTextTask
        encryptTextTask.text = 'test'
        encryptTextTask.password = 'password'
        encryptTextTask.taskAction()

        then: 'Validate result. With random salt generator, output every time differs'
        def encodedText = extractFromOutput(OUT_CONTENT.toString())
        encodedText != null
        !encodedText.isEmpty()
        encodedText.length() == 64
        OUT_CONTENT.reset()

        when: 'Decrypt text'
        assert project.tasks.names.contains(DecryptTextTask.TASK_NAME)
        def decryptTextTask = project.tasks.getByName(DecryptTextTask.TASK_NAME) as DecryptTextTask
        decryptTextTask.text = encodedText
        decryptTextTask.password = 'password'
        decryptTextTask.taskAction()

        then: 'Validate output'
        def decodedText = extractFromOutput(OUT_CONTENT.toString())
        decodedText != null
        !decodedText.isEmpty()
        decodedText == 'test'
    }
}
