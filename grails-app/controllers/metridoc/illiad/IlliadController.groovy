package metridoc.illiad

class IlliadController {

    static reportName = "Illiad Dashboards"
    def illiadService

    static homePage = [
            title: "Illiad Dashboard",
            description: """
                Basic borrowing stats from Illiad
            """
    ]

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