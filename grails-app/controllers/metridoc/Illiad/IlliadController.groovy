package metridoc.Illiad

import metridoc.ReportController

class IlliadController extends ReportController {

    def illiadService

    @Override
    def getModel() {
        [
            'basicStatsData': illiadService.getBasicStatsData(null),
            'groups': illiadService.getGroupList()
        ]
    }
}