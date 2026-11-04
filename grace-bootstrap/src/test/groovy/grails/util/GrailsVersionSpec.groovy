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

    def "Grace version 2022.x"() {
        expect:
        GrailsVersion.isGrace2022('2022.2')
        GrailsVersion.isGrace2022('2022.2-M1')
        GrailsVersion.isGrace2022('2022.2-RC1')
        GrailsVersion.isGrace2022('2022.2-SNAPSHOT')
        GrailsVersion.isGrace2022('2022.2.0')
        GrailsVersion.isGrace2022('2022.2.0-M1')
        GrailsVersion.isGrace2022('2022.2.0-RC1')
        GrailsVersion.isGrace2022('2022.2.0-SNAPSHOT')
        !GrailsVersion.isGrace2022('2021.0')
        !GrailsVersion.isGrace2022('2023.0')
    }

    def "Grace version 2023.x"() {
        expect:
        GrailsVersion.isGrace2023('2023.0')
        GrailsVersion.isGrace2023('2023.0-M1')
        GrailsVersion.isGrace2023('2023.0-RC1')
        GrailsVersion.isGrace2023('2023.0-SNAPSHOT')
        GrailsVersion.isGrace2023('2023.0.1')
        GrailsVersion.isGrace2023('2023.0.0-M1')
        GrailsVersion.isGrace2023('2023.0.0-RC1')
        GrailsVersion.isGrace2023('2023.0.0-SNAPSHOT')
        !GrailsVersion.isGrace2023('2021.0')
        !GrailsVersion.isGrace2023('2022.0')
    }

    def "Grace version 2024.x"() {
        expect:
        GrailsVersion.isGrace2024('2024.0')
        GrailsVersion.isGrace2024('2024.0-M1')
        GrailsVersion.isGrace2024('2024.0-RC1')
        GrailsVersion.isGrace2024('2024.0-SNAPSHOT')
        GrailsVersion.isGrace2024('2024.0.1')
        GrailsVersion.isGrace2024('2024.0.1-M1')
        GrailsVersion.isGrace2024('2024.0.1-RC1')
        GrailsVersion.isGrace2024('2024.0.1-SNAPSHOT')
        !GrailsVersion.isGrace2024('2021.0')
        !GrailsVersion.isGrace2024('2022.0')
        !GrailsVersion.isGrace2024('2023.0')
    }

    def "Grace versions"() {
        expect:
        GrailsVersion.isGrace('2022.2')
        GrailsVersion.isGrace('2022.2-M1')
        GrailsVersion.isGrace('2022.2-RC1')
        GrailsVersion.isGrace('2022.2-SNAPSHOT')
        GrailsVersion.isGrace('2023.0')
        GrailsVersion.isGrace('2023.0-M1')
        GrailsVersion.isGrace('2023.0-RC1')
        GrailsVersion.isGrace('2023.0-SNAPSHOT')
        GrailsVersion.isGrace('2023.0.1')
        GrailsVersion.isGrace('2023.0.1-M1')
        GrailsVersion.isGrace('2023.0.1-RC1')
        GrailsVersion.isGrace('2023.0.1-SNAPSHOT')
        GrailsVersion.isGrace('2024.0')
        GrailsVersion.isGrace('2024.0-M1')
        GrailsVersion.isGrace('2024.0-RC1')
        GrailsVersion.isGrace('2024.0-SNAPSHOT')
        GrailsVersion.isGrace('2024.0.1')
        GrailsVersion.isGrace('2024.0.1-M1')
        GrailsVersion.isGrace('2024.0.1-RC1')
        GrailsVersion.isGrace('2024.0.1-SNAPSHOT')
    }

    def "Grails version 3.x"() {
        expect:
        GrailsVersion.isGrails3('3.2')
        GrailsVersion.isGrails3('3.2-M1')
        GrailsVersion.isGrails3('3.2-RC1')
        GrailsVersion.isGrails3('3.2-SNAPSHOT')
        GrailsVersion.isGrails3('3.2.1')
        GrailsVersion.isGrails3('3.2.1-M1')
        GrailsVersion.isGrails3('3.2.1-RC1')
        GrailsVersion.isGrails3('3.2.1-SNAPSHOT')
        !GrailsVersion.isGrails3('2.3')
        !GrailsVersion.isGrails3('4.0')
    }

    def "Grails version 4.x"() {
        expect:
        GrailsVersion.isGrails4('4.3')
        GrailsVersion.isGrails4('4.3-M1')
        GrailsVersion.isGrails4('4.3-RC1')
        GrailsVersion.isGrails4('4.3-SNAPSHOT')
        GrailsVersion.isGrails4('4.1.0')
        GrailsVersion.isGrails4('4.1.0-M1')
        GrailsVersion.isGrails4('4.1.0-RC1')
        GrailsVersion.isGrails4('4.1.0-SNAPSHOT')
        !GrailsVersion.isGrails4('3.2')
        !GrailsVersion.isGrails4('5.0')
    }

    def "Grails version 5.x"() {
        expect:
        GrailsVersion.isGrails5('5.3')
        GrailsVersion.isGrails5('5.3-M1')
        GrailsVersion.isGrails5('5.3-RC1')
        GrailsVersion.isGrails5('5.2.0')
        GrailsVersion.isGrails5('5.2.0-M1')
        GrailsVersion.isGrails5('5.2.0-RC1')
        GrailsVersion.isGrails5('5.2.0-SNAPSHOT')
        !GrailsVersion.isGrails5('4.1')
        !GrailsVersion.isGrails5('6.0')
    }

    def "Grails version 6.x"() {
        expect:
        GrailsVersion.isGrails6('6.2')
        GrailsVersion.isGrails6('6.2-M1')
        GrailsVersion.isGrails6('6.2-RC1')
        GrailsVersion.isGrails6('6.2-SNAPSHOT')
        GrailsVersion.isGrails6('6.2.1')
        GrailsVersion.isGrails6('6.2.1-M1')
        GrailsVersion.isGrails6('6.2.1-RC1')
        GrailsVersion.isGrails6('6.2.1-SNAPSHOT')
        !GrailsVersion.isGrails6('5.3')
        !GrailsVersion.isGrails6('7.0')
    }

    def "Grails versions"() {
        expect:
        GrailsVersion.isGrails('3.2')
        GrailsVersion.isGrails('4.0')
        GrailsVersion.isGrails('4.1.1')
        GrailsVersion.isGrails('5.0')
        GrailsVersion.isGrails('5.3.1')
        GrailsVersion.isGrails('6.2')
        GrailsVersion.isGrails('6.2.1')
        !GrailsVersion.isGrails('2022.0')
        !GrailsVersion.isGrails('2023.0')
        !GrailsVersion.isGrails('2024.0')
    }

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
