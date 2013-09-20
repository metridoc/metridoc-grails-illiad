Installation
------------

Within your grails application (would recommend using the 
[template](https://github.com/metridoc/metridoc-template-grails-app)), add the following to your `BuildConfig.groovy` file

```groovy
grails.project.dependency.resolution = {
    //other settings
    repositories {
        //other repos like mavenCentral, etc
        mavenRepo "http://dl.bintray.com/upennlib/metridoc"
        mavenRepo "http://dl.bintray.com/upennlib/maven"
        
    }

    plugins {
        //other plugins
        compile (":metridoc-illiad:0.4.2") {
            excludes "metridoc-core"
        }
    }
}
```

This plugin assumes that you have run the [illiad job](http://github.com/metridoc/metridoc-job-illiad) to migrate the data 
into the central repo.  All the plugin does is create a basic dashboard of illiad stats and download buttons to grab the 
data.

If all goes well, you should see a dashboard similar to:

![Illiad Dashboard](https://raw.github.com/metridoc/metridoc-grails-illiad/master/docs/dashboardScreenForREADME.png)

The plugin only relies on two tables that were created by the
[illiad job](http://github.com/metridoc/metridoc-job-illiad), `ill_cache` and `ill_transaction`.  `ill_cache` contains
all the dashboard data in json format while `ill_transaction` provides all the data that can be downloaded.  See the 
[illiad job](http://github.com/metridoc/metridoc-job-illiad) for more details about the illiad table structure.




