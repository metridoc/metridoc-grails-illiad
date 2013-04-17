class MetridocIlliadGrailsPlugin {
    // the plugin version
    def version = "0.3.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.0 > *"
    // the other plugins this plugin depends on
    def dependsOn = ["dataSource": "1.0 > *"]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def title = "Metridoc Illiad Plugin" // Headline display name of the plugin
    def author = "Thomas Barker"
    def authorEmail = ""
    def description = '''\
Provides a job for collecting and storing illiad data for the current fiscal year.  Also a very simple dashboard is provided
to display some simple stats for the current fiscal year
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/metridoc-illiad"
}
