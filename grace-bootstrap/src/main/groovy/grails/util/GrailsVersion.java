/*
 * Copyright 2021-2022 the original author or authors.
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Get the version of Grails from build properties.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
public final class GrailsVersion {

    private static final Pattern VERSION_PATTERN = Pattern.compile("((\\d+)(\\.\\d+)+)(-(\\p{Alpha}+)-(\\w+))?(-(SNAPSHOT|\\d{14}([-+]\\d{4})?))?");

    private final String version;

    private final String buildTime;

    private final String commitId;

    private final Long snapshot;

    private static final GrailsVersion CURRENT;

    public static final String RESOURCE_NAME = "/grails-build.properties";

    public static final String VERSION_PROPERTY = "grails.version";

    public static final String BUILD_TIME_PROPERTY = "grails.buildTime";

    public static final String GIT_COMMIT_ID_PROPERTY = "git.commit.id";

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

    private GrailsVersion(String version, String buildTime, String commitId) {
        this.version = version;
        this.buildTime = buildTime;
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format(
                    "'%s' is not a valid Grails version string (examples: '1.0', '1.0-M1', '1.0-RC1')", version));
        }

        this.commitId = setOrParseCommitId(commitId, matcher);
        this.snapshot = parseSnapshot(matcher);
    }

    private Long parseSnapshot(Matcher matcher) {
        if ("snapshot".equals(matcher.group(5)) || isCommitVersion(matcher)) {
            return 0L;
        }
        else if (matcher.group(8) == null) {
            return null;
        }
        else if ("SNAPSHOT".equals(matcher.group(8))) {
            return 0L;
        }
        else {
            try {
                if (matcher.group(9) != null) {
                    return new SimpleDateFormat("yyyyMMddHHmmssZ").parse(matcher.group(8)).getTime();
                }
                else {
                    SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
                    format.setTimeZone(TimeZone.getTimeZone("UTC"));
                    return format.parse(matcher.group(8)).getTime();
                }
            }
            catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    private boolean isCommitVersion(Matcher matcher) {
        return "commit".equals(matcher.group(5));
    }

    private String setOrParseCommitId(String commitId, Matcher matcher) {
        if (commitId != null || !isCommitVersion(matcher)) {
            return commitId;
        }
        else {
            return matcher.group(6);
        }
    }

    @Override
    public String toString() {
        return "Grails " + this.version;
    }

    public String getVersion() {
        return this.version;
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

}
