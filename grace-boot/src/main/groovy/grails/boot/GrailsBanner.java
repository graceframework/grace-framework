/*
 * Copyright 2021-2023 the original author or authors.
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
package grails.boot;

import java.io.PrintStream;

import org.springframework.boot.Banner;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;
import org.springframework.core.env.Environment;

import grails.util.GrailsUtil;

/**
 * Default Banner implementation which writes the 'Grails' banner.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
public class GrailsBanner implements Banner {

    private static final String[] BANNER = {
            "",
            "               ______",
            "              / _____)",
            " o  o   o  o | /  ___   ____   ____   ____   ____",
            "  \\/ \\^/ \\/  | | (___) / ___) / _  | / ___) / _  )",
            "   \\_____/   | \\____/|| |    ( ( | |( (___ ( (/ /",
            "    =====     \\_____/ |_|     \\_||_| \\____) \\____)",
            "  ================================================"
    };

    private static final String GRAILS = "  :: Grace :: ";

    private static final int STRAP_LINE_SIZE = 50;

    @Override
    public void printBanner(Environment environment, Class<?> sourceClass, PrintStream printStream) {
        AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
        for (String line : BANNER) {
            printStream.println(AnsiOutput.toString(line));
        }
        String version = GrailsUtil.getGrailsVersion();
        version = (version != null) ? " (v" + version + ")" : "";
        StringBuilder padding = new StringBuilder();
        while (padding.length() < STRAP_LINE_SIZE - (version.length() + GRAILS.length())) {
            padding.append(" ");
        }

        printStream.println(AnsiOutput.toString(AnsiColor.GREEN, GRAILS, AnsiColor.DEFAULT, padding.toString(),
                AnsiStyle.FAINT, version));
        printStream.println();
    }

}
