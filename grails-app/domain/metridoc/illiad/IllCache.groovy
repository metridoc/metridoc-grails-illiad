package metridoc.illiad

import grails.converters.JSON
import groovy.json.JsonSlurper

/**
 * contains cached illiad data for the view in json
 */
class IllCache {

    String jsonData
    Date lastUpdated
    Date dateCreated

    static constraints = {
        jsonData(maxSize: Integer.MAX_VALUE)
    }

    static void update(String jsonData) {
        withNewTransaction {
            if (count()) {
                def illCache = list().get(0)
                illCache.jsonData = jsonData
                illCache.save(failOnError: true)
            } else {
                new IllCache(jsonData: jsonData).save(failOnError: true)
            }
        }
    }

    static void update(Map data) {
        //had to use grails's json marshalling instead of groovy's since it has an error that causes a stack overflow
        def converter = new JSON(data)
        update(converter)
    }

    static void update(JSON json) {
        update(json.toString())
    }

    static getData() {
        if (count() == 0) return null

        def cache = list().get(0)
        def slurper = new JsonSlurper()
        def data = slurper.parseText(cache.jsonData)

        if (cache.lastUpdated) {
            data.lastUpdated = cache.lastUpdated
        } else {
            data.lastUpdated = cache.dateCreated
        }

        return data
    }


}
