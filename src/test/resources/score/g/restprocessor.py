#   coding=utf-8
from java.lang import String
from java.util import ArrayList, LinkedHashMap
from org.springframework.web.reactive.function.server import ServerResponse
from org.springframework.http import HttpStatus, MediaType
from reactor.core.publisher import Mono
from ru.curs.flute.rest.decorator import Mapping, FilterBefore, FilterAfter

@Mapping('/foo')
def foo(context, request):
    dto = {
        "foo": 1,
        "bar": 2
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@Mapping('/params')
def pRarams(context, request):

    response = LinkedHashMap()
    text = request.path()
    status = None

    if request.queryParams().size() != 0:
        text = text + "?"

        params = request.queryParams().toSingleValueMap()
        theFirst = True

        for key in params.keySet():
            if theFirst != True:
                text = text + "&"
            text = text + key + "=" + params.get(key)
            theFirst = False

        status = 200
    else:
        text = "there are no params received"
        status = 422

    response.put('text', text)
    response.put('status', status)

    return ServerResponse.status(HttpStatus.valueOf(status))\
        .body(Mono.just(text), String)


@Mapping('/jsonPost', "POST")
def postWithJsonBody(context, request):
    dto = request.bodyToMono(dict).block()
    dto['numb'] = dto['numb'] + 1
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@FilterBefore('/beforeTest')
def beforeFilter(context, request):
    request.attributes().put('data', {'foo': 1})

@Mapping('/beforeTest')
def handlerForBeforeFilter(context, request):
    data = request.attributes().get('data')
    data['foo'] = data['foo'] + 1
    return ServerResponse.ok().body(Mono.just(data), data.__class__)


@FilterBefore('/doubleBeforeTest')
def doubleBeforeFilter1(context, request):
    request.attributes().put('data', ArrayList())
    data = request.attributes().get('data')
    data.add(1)

@FilterBefore('/doubleBeforeTes*')
def doubleBeforeFilter2(context, request):
    data = request.attributes().get('data')
    data.add(2)

@Mapping('/doubleBeforeTest')
def handlerForDoubleBeforeFilter(context, request):
    data = request.attributes().get('data')
    data.add(3)
    return ServerResponse.ok().body(Mono.just(data), data.__class__)

@FilterAfter('/afterTest')
def afterFilter(context, request):
    data = request.attributes().get('data')
    data['foo'] = data['foo'] + 1
    return ServerResponse.ok().body(Mono.just(data), data.__class__)

@Mapping('/afterTest')
def handlerForAfterFilter(context, request):
    request.attributes().put('data', {'foo': 1})

@FilterAfter('/doubleAfterTest')
def doubleAfterFilter1(context, request):
    data = request.attributes().get('data')
    data.add(2)

@FilterAfter('/doubleAfterTes*')
def doubleAfterFilter2(context, request):
    data = request.attributes().get('data')
    data.add(3)
    return ServerResponse.ok().body(Mono.just(data), data.__class__)

@Mapping('/doubleAfterTest')
def handlerForDoubleAfterFilter(context, request):
    request.attributes().put('data', ArrayList())
    data = request.attributes().get('data')
    data.add(1)

@FilterAfter('/afterAndBeforeTest')
def afterAndBeforeAfterFilter(context, request):
    data = request.attributes().get('data')
    data.add(3)
    return ServerResponse.ok().body(Mono.just(data), data.__class__)

@FilterBefore('/afterAndBeforeTest')
def afterAndBeforeBeforeFilter(context, request):
    request.attributes().put('data', ArrayList())
    data = request.attributes().get('data')
    data.add(1)

@Mapping('/afterAndBeforeTest')
def handlerForAfterAndBeforeFilter(context, request):
    data = request.attributes().get('data')
    data.add(2)


@Mapping('/globalCelestaUserIdTest')
def handlerForGlobalCelestaUserId(context, request):
    dto = {
        "userId": context.getUserId()
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@FilterBefore('/customCelestaUserIdTest')
def customCelestaUserIdFilterTest(context, request):
    request.attributes().put('userId', 'testUser')

@Mapping('/customCelestaUserIdTest')
def handlerForCustomCelestaUserIdFilter(context, request):
    dto = {
        "userId": context.getUserId()
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)


@FilterBefore('/testReturnFromBefore')
def beforeTestReturnFromBefore(context, request):
    dto = {
        "foo": 1
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@Mapping('/testReturnFromBefore')
def handlerForTestReturnFromBefore(context, request):
    dto = {
        "foo": 2
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@FilterAfter('/testReturnFromBefore')
def afterTestReturnFromBefore(context, request):
    dto = {
        "foo": 3
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@Mapping('/testReturnFromHandler')
def handlerForTestReturnFromHandler(context, request):
    dto = {
        "foo": 1
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@FilterAfter('/testReturnFromHandler')
def afterTestReturnFromHandler(context, request):
    dto = {
        "foo": 2
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)


@Mapping('/testReturnFromAfter')
def handlerForTestReturnFromAfter(context, request):
    foo = 'bar'

@FilterAfter('/testReturnFromAfter')
def after1TestReturnFromAfter(context, request):
    dto = {
        "foo": 1
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@FilterAfter('/testReturnFromAfter')
def after2TestReturnFromAfter(context, request):
    dto = {
        "foo": 2
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@Mapping('/testNoResultForHandler')
def handlerForTestNoResultForHandler(context, request):
    foo = 'bar'

@Mapping('/testNoResultForAfterFilter')
def handlerForTestNoResultForAfterFilter(context, request):
    foo = 'bar'

@FilterAfter('/testNoResultForAfterFilter')
def afterForTestNoResultForAfterFilter(context, request):
    foo = 'bar'