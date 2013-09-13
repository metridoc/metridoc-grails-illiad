package metridoc.illiad

import metridoc.utils.DateUtil

class IlliadController {

    static reportName = "Illiad Dashboards"
    def illiadReportingService

    static homePage = [
            title: "Illiad Dashboard",
            description: """
                Basic borrowing stats from Illiad
            """
    ]

    def index() {
        DateUtil.FY_START_MONTH = IllFiscalStartMonth.first()?.month ?: Calendar.JULY
        def data = IllCache.data
        if(data) {
            data.month = DateUtil.getFiscalMonth()
            return data
        }
        else {
             render(view: "dataNotAvailable")
        }
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
            illiadReportingService.streamIlliadDataAsCsv(type, borrowing, response.outputStream)
        }
    }
}