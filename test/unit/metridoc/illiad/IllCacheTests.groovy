package metridoc.illiad

import grails.converters.JSON
import grails.test.mixin.Mock
import org.codehaus.groovy.grails.web.converters.configuration.ConverterConfiguration
import org.codehaus.groovy.grails.web.converters.marshaller.json.MapMarshaller
import org.junit.Test


@Mock(IllCache)
class IllCacheTests {

    @Test
    void "getMostRecentData gets the most recent data in cached format"() {
        IllCache.update("old")
        assert 1 == IllCache.count()
        IllCache.update("new")
        assert 1 == IllCache.count()
        def cache = IllCache.list().get(0)
        assert "new" == cache.jsonData
        assert cache.lastUpdated
    }

    @SuppressWarnings("GroovyAccessibility")
    @Test
    void "test storage and retrieval of data"() {
        assert null == IllCache.data
        def now = new Date()
        def json = new JSON([foo: "bar"])
        ConverterConfiguration config = json.config
        //compensates for unit tests ran in intellij
        if (config.orderedObjectMarshallers.size() == 0) {
            json.config.registerObjectMarshaller(new MapMarshaller())
        }
        IllCache.update(json)
        def data = IllCache.data
        assert now.time < data.lastUpdated.time
        assert "bar" == data.foo
    }
}
