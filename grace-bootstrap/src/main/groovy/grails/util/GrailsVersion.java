/*
 * Copyright 2021-2026 the original author or authors.
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
package grails.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Get the version of Grails from build properties.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
public final class GrailsVersion implements Comparable<GrailsVersion> {

    private static final Pattern VERSION_PATTERN = Pattern.compile("((\\d+)(\\.\\d+)+)(-(\\w+))?(-(SNAPSHOT|(\\d{4})?))?");
    private static final int STAGE_MILESTONE = 0;
    private static final int STAGE_UNKNOWN = 1;
    private static final int STAGE_RC = 2;

    private final String version;

    private final int majorPart;

    private final String buildTime;

    private final String commitId;

    private final Long snapshot;

    private final String versionPart;

    private final Stage stage;

    private static final GrailsVersion CURRENT;

    public static final String RESOURCE_NAME = "/grails-build.properties";

    public static final String VERSION_PROPERTY = "git.build.version";

    public static final String BUILD_TIME_PROPERTY = "git.commit.time";

    public static final String GIT_COMMIT_ID_PROPERTY = "git.commit.id";

    public static final String GRACE_2024 = "2024";

    public static final String GRACE_2023 = "2023";

    public static final String GRACE_2022 = "2022";

    public static final String GRAILS_6 = "6";

    public static final String GRAILS_5 = "5";

    public static final String GRAILS_4 = "4";

    public static final String GRAILS_3 = "3";

    static {
        URL resource = GrailsVersion.class.getResource(RESOURCE_NAME);
        if (resource == null) {
            throw new IllegalArgumentException(String.format("Resource '%s' not found.", RESOURCE_NAME));
        }

        InputStream inputStream = null;
        try {
            URLConnection connection = resource.openConnection();
            connection.setUseCaches(false);
            inputStream = connection.getInputStream();
            Properties properties = new Properties();
            properties.load(inputStream);

            String version = properties.get(VERSION_PROPERTY).toString();

            String buildTime = properties.get(BUILD_TIME_PROPERTY).toString();
            String commitId = properties.get(GIT_COMMIT_ID_PROPERTY).toString();

            CURRENT = new GrailsVersion(version, "unknown".equals(buildTime) ? null : buildTime, commitId);
        }
        catch (Exception e) {
            throw new IllegalArgumentException(String.format("Could not load version details from resource '%s'.", resource), e);
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException ignore) {
                }
            }
        }
    }

    public static GrailsVersion current() {
        return CURRENT;
    }

    public static GrailsVersion version(String version) throws IllegalArgumentException {
        return new GrailsVersion(version, null, null);
    }

    public static boolean isGrace2024() {
        return GRACE_2024.equals(String.valueOf(current().getMajorVersion()));
    }

    public static boolean isGrace2023() {
        return GRACE_2023.equals(String.valueOf(current().getMajorVersion()));
    }

    public static boolean isGrace2022() {
        return GRACE_2022.equals(String.valueOf(current().getMajorVersion()));
    }

    public static boolean isGrace() {
        return isGrace2024() || isGrace2023() || isGrace2022();
    }

    public static boolean isGrace2024(String graceVersion) {
        return graceVersion != null && GRACE_2024.equals(String.valueOf(version(graceVersion).getMajorVersion()));
    }

    public static boolean isGrace2023(String graceVersion) {
        return graceVersion != null && GRACE_2023.equals(String.valueOf(version(graceVersion).getMajorVersion()));
    }

    public static boolean isGrace2022(String graceVersion) {
        return graceVersion != null && GRACE_2022.equals(String.valueOf(version(graceVersion).getMajorVersion()));
    }

    public static boolean isGrace(String graceVersion) {
        return isGrace2024(graceVersion) || isGrace2023(graceVersion) || isGrace2022(graceVersion);
    }

    public static boolean isGraceSnapshotVersion(String graceVersion) {
        return (isGrace2024(graceVersion) || isGrace2023(graceVersion) || isGrace2022(graceVersion)) && graceVersion.endsWith("-SNAPSHOT");
    }

    public static boolean isGrails6(String grailsVersion) {
        return grailsVersion != null && GRAILS_6.equals(String.valueOf(version(grailsVersion).getMajorVersion()));
    }

    public static boolean isGrails5(String grailsVersion) {
        return grailsVersion != null && GRAILS_5.equals(String.valueOf(version(grailsVersion).getMajorVersion()));
    }

    public static boolean isGrails4(String grailsVersion) {
        return grailsVersion != null && GRAILS_4.equals(String.valueOf(version(grailsVersion).getMajorVersion()));
    }

    public static boolean isGrails3(String grailsVersion) {
        return grailsVersion != null && GRAILS_3.equals(String.valueOf(version(grailsVersion).getMajorVersion()));
    }

    public static boolean isGrails(String grailsVersion) {
        return isGrails6(grailsVersion) || isGrails5(grailsVersion) || isGrails4(grailsVersion) || isGrails3(grailsVersion);
    }

    private GrailsVersion(String version, String buildTime, String commitId) {
        this.version = version;
        this.buildTime = buildTime;
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format(
                    "'%s' is not a valid Grails version string (examples: '1.0', '1.0-M1', '1.0-RC1')", version));
        }

        this.versionPart = matcher.group(1);
        this.majorPart = Integer.parseInt(matcher.group(2), 10);

        this.commitId = commitId;
        this.stage = parseStage(matcher);
        this.snapshot = parseSnapshot(matcher);
    }

    private Long parseSnapshot(Matcher matcher) {
        if ("SNAPSHOT".equals(matcher.group(5))) {
            return 0L;
        }
        return null;
    }

    private Stage parseStage(Matcher matcher) {
        if (matcher.group(5) == null) {
            return null;
        } else if (isStage("M", matcher)) {
            return Stage.from(STAGE_MILESTONE, matcher.group(5));
        } else if (isStage("RC", matcher)) {
            return Stage.from(STAGE_RC, matcher.group(5));
        } else {
            return Stage.from(STAGE_UNKNOWN, matcher.group(5));
        }
    }

    private boolean isStage(String stage, Matcher matcher) {
        return matcher.group(5).startsWith(stage);
    }

    @Override
    public String toString() {
        return "Grace " + this.version;
    }

    public String getVersion() {
        return this.version;
    }

    public int getMajorVersion() {
        return this.majorPart;
    }

    public String getBuildTimestamp() {
        return this.buildTime;
    }

    public String getGitRevision() {
        return this.commitId;
    }

    public boolean isSnapshot() {
        return this.snapshot != null;
    }

    public GrailsVersion getBaseVersion() {
        if (isFinal()) {
            return this;
        }
        return version(this.versionPart);
    }

    public boolean isFinal() {
        return this.stage == null && this.snapshot == null;
    }

    public GrailsVersion getNextMajorVersion() {
        return version((this.majorPart + 1) + ".0.0");
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != getClass()) {
            return false;
        }
        GrailsVersion other = (GrailsVersion) o;
        return this.version.equals(other.version);
    }

    @Override
    public int hashCode() {
        return this.version.hashCode();
    }

    @Override
    public int compareTo(GrailsVersion grailsVersion) {
        String[] majorVersionParts = this.versionPart.split("\\.");
        String[] otherMajorVersionParts = grailsVersion.versionPart.split("\\.");

        for (int i = 0; i < majorVersionParts.length && i < otherMajorVersionParts.length; i++) {
            int part = Integer.parseInt(majorVersionParts[i]);
            int otherPart = Integer.parseInt(otherMajorVersionParts[i]);

            if (part > otherPart) {
                return 1;
            }
            if (otherPart > part) {
                return -1;
            }
        }
        if (majorVersionParts.length > otherMajorVersionParts.length) {
            return 1;
        }
        if (majorVersionParts.length < otherMajorVersionParts.length) {
            return -1;
        }

        if (this.stage != null && grailsVersion.stage != null) {
            int diff = this.stage.compareTo(grailsVersion.stage);
            if (diff != 0) {
                return diff;
            }
        }
        if (this.stage == null && grailsVersion.stage != null) {
            return 1;
        }
        if (this.stage != null && grailsVersion.stage == null) {
            return -1;
        }

        Long thisSnapshot = this.snapshot == null ? Long.MAX_VALUE : this.snapshot;
        Long theirSnapshot = grailsVersion.snapshot == null ? Long.MAX_VALUE : grailsVersion.snapshot;

        if (thisSnapshot.equals(theirSnapshot)) {
            return this.version.compareTo(grailsVersion.version);
        } else {
            return thisSnapshot.compareTo(theirSnapshot);
        }
    }

    static final class Stage implements Comparable<Stage> {
        final int stage;
        final int number;

        private Stage(int stage, int number) {
            this.stage = stage;
            this.number = number;
        }

        static Stage from(int stage, String stageString) {
            Matcher m = Pattern.compile("(M|RC)(\\d+)?").matcher(stageString);
            int number;
            if (m.matches()) {
                number = Integer.parseInt(m.group(2));
            } else {
                return null;
            }

            return new Stage(stage, number);
        }

        @Override
        public int compareTo(Stage other) {
            if (this.stage > other.stage) {
                return 1;
            }
            if (this.stage < other.stage) {
                return -1;
            }
            if (this.number > other.number) {
                return 1;
            }
            if (this.number < other.number) {
                return -1;
            }
            return 0;
        }
    }

}
