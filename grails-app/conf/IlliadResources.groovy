modules = {
    illiad {
        dependsOn 'jquery-ui'
        resource id: 'css',
                url: [plugin: "metridocIlliad", dir:'illiad/css', file: 'illiad.css']
        resource id: 'js',
                url: [plugin: "metridocIlliad", dir:'illiad/js', file: 'illiad.js']
    }
}