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

class filterBefore(Object):
    def __init__(self, *urlPatterns):

        self.urlPatterns = urlPatterns
        self.type = type

    def __call__(self, func):
        builder = RestMappingBuilder.getInstance()
        funcName = func.__module__  + "." + func.__name__
        filterMapping = FilterMapping(set(self.urlPatterns), funcName, "BEFORE")
        builder.addFilterMapping(filterMapping)
        return func

class filterAfter(Object):
    def __init__(self, *urlPatterns):

        self.urlPatterns = urlPatterns
        self.type = type

    def __call__(self, func):
        builder = RestMappingBuilder.getInstance()
        funcName = func.__module__  + "." + func.__name__
        filterMapping = FilterMapping(set(self.urlPatterns), funcName, "AFTER")
        builder.addFilterMapping(filterMapping)
        return func