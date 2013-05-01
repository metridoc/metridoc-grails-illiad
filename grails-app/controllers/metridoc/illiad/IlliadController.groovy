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
        if (params.refresh) {
            refresh = params.refresh
        }

        if (refresh) {
            illiadService.storeCache()
        }

        return IllCache.data
    }

    def download(String type, Boolean borrowing) {
        if (!type) {
            flash.alerts << "[type] was not specified for file download"
            redirect(action: "index")
        } else {
            borrowing = borrowing != null ? borrowing : false
            response.setContentType("text/csv")
            def fileName = borrowing ? "${type.toLowerCase()}_borrowing.csv" : "${type.toLowerCase()}_lending.csv"
            response.setHeader("Content-Disposition", "attachment;filename=${fileName}")
            illiadService.streamIlliadDataAsCsv(type, borrowing, response.outputStream)
        }
    }
}