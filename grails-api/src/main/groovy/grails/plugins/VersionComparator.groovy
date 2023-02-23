/*
 * Copyright 2004-2023 the original author or authors.
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
package grails.plugins

import groovy.transform.CompileStatic

/**
 * A comparator capable of sorting versions from from newest to oldest
 * @author Graeme Rocher
 */
@CompileStatic
class VersionComparator implements Comparator<String> {

    static private final List<String> SNAPSHOT_SUFFIXES = ['-SNAPSHOT', '.BUILD-SNAPSHOT'].asImmutable()

    int compare(String o1, String o2) {
        int result = 0
        if (o1 == '*') {
            result = 1
        }
        else if (o2 == '*') {
            result = -1
        }
        else {
            List<Integer> nums1
            try {
                String[] tokens = deSnapshot(o1).split(/\./)
                Collection<String> tokenList = tokens.findAll { String it -> it.trim() ==~ /\d+/ }
                nums1 = tokenList*.toInteger()
            }
            catch (NumberFormatException e) {
                throw new InvalidVersionException("Cannot compare versions, left side [$o1] is invalid: ${e.message}")
            }
            List<Integer> nums2
            try {
                String[] tokens = deSnapshot(o2).split(/\./)
                Collection<String> tokenList = tokens.findAll { String it -> it.trim() ==~ /\d+/ }
                nums2 = tokenList*.toInteger()
            }
            catch (NumberFormatException e) {
                throw new InvalidVersionException("Cannot compare versions, right side [$o2] is invalid: ${e.message}")
            }
            boolean bigRight = nums2.size() > nums1.size()
            boolean bigLeft = nums1.size() > nums2.size()
            for (int i in 0..<nums1.size()) {
                if (nums2.size() > i) {
                    result = nums1[i].compareTo(nums2[i])
                    if (result != 0) {
                        break
                    }
                    if (i == (nums1.size() - 1) && bigRight) {
                        if (nums2[i + 1] != 0) {
                            result = -1
                            break
                        }
                    }
                }
                else if (bigLeft) {
                    if (nums1[i] != 0) {
                        result = 1
                        break
                    }
                }
            }
        }

        if (result == 0) {
            // Versions are equal, but one may be a snapshot.
            // A snapshot version is considered less than a non snapshot version
            boolean o1IsSnapshot = isSnapshot(o1)
            boolean o2IsSnapshot = isSnapshot(o2)

            if (o1IsSnapshot && !o2IsSnapshot) {
                result = -1
            }
            else if (!o1IsSnapshot && o2IsSnapshot) {
                result = 1
            }
        }

        result
    }

    boolean equals(obj) { false }

    /**
     * Removes any suffixes that indicate that the version is a kind of snapshot
     */
    @CompileStatic
    protected String deSnapshot(String version) {
        String suffix = SNAPSHOT_SUFFIXES.find { String it -> version?.endsWith(it) }
        if (suffix) {
            return version[0..-(suffix.size() + 1)]
        }
        version
    }

    @CompileStatic
    protected boolean isSnapshot(String version) {
        SNAPSHOT_SUFFIXES.any { String it -> version?.endsWith(it) }
    }

}
