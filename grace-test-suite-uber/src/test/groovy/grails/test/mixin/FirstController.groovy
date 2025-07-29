package grails.test.mixin

import grails.artefact.Artefact
import grails.web.Controller

@Artefact("Controller")
@Controller
class FirstController {
    def index() {}
}
