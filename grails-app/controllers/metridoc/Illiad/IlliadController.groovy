package metridoc.Illiad

class IlliadController {

    static reportName = "Illiad Dashboards"
    def illiadService

    def index() {
        illiadService.model
    }
}