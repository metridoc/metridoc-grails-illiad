package metridoc.illiad



class CacheIlliadDataJob {

    static ILLIAD_CACHE_INTERVAL = 1000 * 60 * 60 //1 hour
    def illiadService

    static triggers = {
      simple name: 'cacheIlliadData', repeatInterval: ILLIAD_CACHE_INTERVAL
    }

    def execute() {
        illiadService.populateModel()
    }
}
