package metridoc.illiad



class CacheIlliadDataJob {

    static ILLIAD_CACHE_INTERVAL = 1000 * 60 * 60 //1 hour
    def illiadService

    static triggers = {
        //run every hour, forever
        simple name: 'cacheIlliadData', repeatInterval: ILLIAD_CACHE_INTERVAL, startDelay: 0, repeatCount: -1
    }

    def execute() {
        illiadService.populateModel()
    }
}
