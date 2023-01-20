package com.github.jasypt.encrypt.tasks.text;

import org.gradle.api.internal.tasks.options.OptionValidationException;
import org.gradle.api.tasks.options.Option;
import com.github.jasypt.encrypt.tasks.PasswordAwareTask;

public abstract class TextAwareTask extends PasswordAwareTask {

    String text;

    @Option(option = "text", description = "[required]")
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public void validateOptions() {
        if (this.text == null || this.text.isEmpty()) {
            throw new OptionValidationException("--text is needed");
        }
    }


}
