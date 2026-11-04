/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.util

import spock.lang.Specification

/**
 * Tests of the GrailsVersion.
 *
 * @author Michael Yan
 * @since 2023.0
 */
class GrailsVersionSpec extends Specification {

    def "Grace base and final versions"() {
        when:
        GrailsVersion grailsVersion = GrailsVersion.version('2024.2.0-RC1')
        then:
        grailsVersion.majorVersion == 2024
        grailsVersion.nextMajorVersion == GrailsVersion.version('2025.0.0')
        grailsVersion.baseVersion == GrailsVersion.version('2024.2.0')
        GrailsVersion.version('2024.2.0').final
        !grailsVersion.snapshot
        !grailsVersion.final
    }

    def "Grace versions comparing"() {
        expect:
        GrailsVersion.version('2024.2.0-M1') < GrailsVersion.version('2024.2.0-M2')
        GrailsVersion.version('2024.2.0-M2') < GrailsVersion.version('2024.2.0-M3')
        GrailsVersion.version('2024.2.0-M1') < GrailsVersion.version('2024.2.0-RC1')
        GrailsVersion.version('2024.2.0-M3') < GrailsVersion.version('2024.2.0-RC1')
        GrailsVersion.version('2024.2.0-RC1') < GrailsVersion.version('2024.2.0-RC2')
        GrailsVersion.version('2024.2.0-RC2') < GrailsVersion.version('2024.2.0')
        GrailsVersion.version('2024.2.0-M3') < GrailsVersion.version('2024.2.0')
        GrailsVersion.version('2024.1.0') < GrailsVersion.version('2024.2.0')
        GrailsVersion.version('2024.1.0') < GrailsVersion.version('2024.2.0-M1')
        GrailsVersion.version('2024.1.0') < GrailsVersion.version('2024.2.0-RC1')
        GrailsVersion.version('2024.2.0') == GrailsVersion.version('2024.2.0')
        GrailsVersion.version('2024.2.0-M1') == GrailsVersion.version('2024.2.0-M1')
        GrailsVersion.version('2024.2.0-RC1') == GrailsVersion.version('2024.2.0-RC1')
    }

}
