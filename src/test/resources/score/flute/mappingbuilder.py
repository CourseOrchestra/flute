#   coding=utf-8
###Модуль обработки декораторов
from ru.curs.flute.rest import RestMappingBuilder, RequestMapping, FilterMapping
from java.lang import Object

class mapping(Object):
    def __init__(self, url, method="GET"):

        self.url = url
        self.method = method

    def __call__(self, func):
        builder = RestMappingBuilder.getInstance()
        funcName = func.__module__  + "." + func.__name__
        requestMapping = RequestMapping(self.url, funcName, self.method)
        builder.addRequestMapping(requestMapping)
        return func

class filter(Object):
    def __init__(self, urlPattern, type="BEFORE"):

        self.urlPattern = urlPattern
        self.type = type

    def __call__(self, func):
        builder = RestMappingBuilder.getInstance()
        funcName = func.__module__  + "." + func.__name__
        filterMapping = FilterMapping(self.urlPattern, funcName, self.type)
        builder.addFilterMapping(filterMapping)
        return func
