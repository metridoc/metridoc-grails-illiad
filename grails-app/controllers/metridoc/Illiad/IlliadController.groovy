package metridoc.Illiad

class IlliadController {

    static reportName = "Illiad Dashboards"
    def illiadService

    def index() {
        def refresh = false
        if(params.refresh) {
            refresh = params.refresh
        }

        if(refresh) {
            illiadService.populateModel()
        }

        illiadService.model
    }
}