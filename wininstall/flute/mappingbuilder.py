#   coding=utf-8
###Модуль обработки декораторов
from ru.curs.flute.rest import RestMappingBuilder, Mapping
from java.lang import Object

class mapping(Object):
    def __init__(self, url, method="GET"):

        self.url = url
        self.method = method

    def __call__(self, func):
        builder = RestMappingBuilder.getInstance()
        funcName = func.__module__  + "." + func.__name__
        mapping = Mapping(self.url, funcName, self.method)
        builder.addMapping(mapping)
        return func