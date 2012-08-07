package metridoc.Illiad

class IlliadController {

    static reportName = "Illiad Dashboards"
    def illiadService

    def index() {
        [
            'basicStatsData': illiadService.getBasicStatsData(null),
            'groups': illiadService.getGroupList()
        ]
    }
}