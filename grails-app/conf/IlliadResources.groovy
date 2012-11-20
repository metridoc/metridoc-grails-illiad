modules = {
    illiad {
        dependsOn 'jquery-ui'
        resource id: 'css', attrs: [type: 'css'],
                url: [plugin: "metridocIlliad", dir:'illiad/css', file: 'illiad.css']
        resource id: 'js', attrs: [type: 'js'],
                url: [plugin: "metridocIlliad", dir:'illiad/js', file: 'illiad.js']
    }
}