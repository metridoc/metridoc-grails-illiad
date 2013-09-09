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
