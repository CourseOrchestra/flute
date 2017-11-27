#   coding=utf-8
from java.lang import String
from java.util import ArrayList, LinkedHashMap
from org.springframework.web.reactive.function.server import ServerResponse
from org.springframework.http import HttpStatus, MediaType
from org.springframework.web.reactive.function import BodyExtractors
from reactor.core.publisher import Mono
from ru.curs.flute.rest.decorator import Mapping, FilterBefore, FilterAfter

@Mapping('/foo')
def foo(context, flute, request):
    dto = {
        "foo": 1,
        "bar": 2
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@Mapping('/fluteparam', "GET")
def fluteparam(context, flute, request):
    sid = flute.sourceId
    return ServerResponse.ok().body(Mono.just(sid), String)

@Mapping('/params')
def pRarams(context, flute, request):

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
def postWithJsonBody(context, flute, request):
    dto = request.bodyToMono(dict).block()
    dto['numb'] = dto['numb'] + 1
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@FilterBefore('/beforeTest')
def beforeFilter(context, flute, request):
    request.attributes().put('data', {'foo': 1})

@Mapping('/beforeTest')
def handlerForBeforeFilter(context, flute, request):
    data = request.attributes().get('data')
    data['foo'] = data['foo'] + 1
    return ServerResponse.ok().body(Mono.just(data), data.__class__)


@FilterBefore('/doubleBeforeTest')
def doubleBeforeFilter1(context, flute, request):
    request.attributes().put('data', ArrayList())
    data = request.attributes().get('data')
    data.add(1)

@FilterBefore('/doubleBeforeTes*')
def doubleBeforeFilter2(context, flute, request):
    data = request.attributes().get('data')
    data.add(2)

@Mapping('/doubleBeforeTest')
def handlerForDoubleBeforeFilter(context, flute, request):
    data = request.attributes().get('data')
    data.add(3)
    return ServerResponse.ok().body(Mono.just(data), data.__class__)

@FilterAfter('/afterTest')
def afterFilter(context, flute, request):
    data = request.attributes().get('data')
    data['foo'] = data['foo'] + 1
    return ServerResponse.ok().body(Mono.just(data), data.__class__)

@Mapping('/afterTest')
def handlerForAfterFilter(context, flute, request):
    request.attributes().put('data', {'foo': 1})

@FilterAfter('/doubleAfterTest')
def doubleAfterFilter1(context, flute, request):
    data = request.attributes().get('data')
    data.add(2)

@FilterAfter('/doubleAfterTes*')
def doubleAfterFilter2(context, flute, request):
    data = request.attributes().get('data')
    data.add(3)
    return ServerResponse.ok().body(Mono.just(data), data.__class__)

@Mapping('/doubleAfterTest')
def handlerForDoubleAfterFilter(context, flute, request):
    request.attributes().put('data', ArrayList())
    data = request.attributes().get('data')
    data.add(1)

@FilterAfter('/afterAndBeforeTest')
def afterAndBeforeAfterFilter(context, flute, request):
    data = request.attributes().get('data')
    data.add(3)
    return ServerResponse.ok().body(Mono.just(data), data.__class__)

@FilterBefore('/afterAndBeforeTest')
def afterAndBeforeBeforeFilter(context, flute, request):
    request.attributes().put('data', ArrayList())
    data = request.attributes().get('data')
    data.add(1)

@Mapping('/afterAndBeforeTest')
def handlerForAfterAndBeforeFilter(context, flute, request):
    data = request.attributes().get('data')
    data.add(2)


@Mapping('/globalCelestaUserIdTest')
def handlerForGlobalCelestaUserId(context, flute, request):
    dto = {
        "userId": context.getUserId()
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@FilterBefore('/customCelestaUserIdTest')
def customCelestaUserIdFilterTest(context, flute, request):
    request.attributes().put('userId', 'testUser')

@Mapping('/customCelestaUserIdTest')
def handlerForCustomCelestaUserIdFilter(context, flute, request):
    dto = {
        "userId": context.getUserId()
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)


@FilterBefore('/testReturnFromBefore')
def beforeTestReturnFromBefore(context, flute, request):
    dto = {
        "foo": 1
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@Mapping('/testReturnFromBefore')
def handlerForTestReturnFromBefore(context, flute, request):
    dto = {
        "foo": 2
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@FilterAfter('/testReturnFromBefore')
def afterTestReturnFromBefore(context, flute, request):
    dto = {
        "foo": 3
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@Mapping('/testReturnFromHandler')
def handlerForTestReturnFromHandler(context, flute, request):
    dto = {
        "foo": 1
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@FilterAfter('/testReturnFromHandler')
def afterTestReturnFromHandler(context, flute, request):
    dto = {
        "foo": 2
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)


@Mapping('/testReturnFromAfter')
def handlerForTestReturnFromAfter(context, flute, request):
    foo = 'bar'

@FilterAfter('/testReturnFromAfter')
def after1TestReturnFromAfter(context, flute, request):
    dto = {
        "foo": 1
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@FilterAfter('/testReturnFromAfter')
def after2TestReturnFromAfter(context, flute, request):
    dto = {
        "foo": 2
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@Mapping('/testNoResultForHandler')
def handlerForTestNoResultForHandler(context, flute, request):
    foo = 'bar'

@Mapping('/testNoResultForAfterFilter')
def handlerForTestNoResultForAfterFilter(context, flute, request):
    foo = 'bar'

@FilterAfter('/testNoResultForAfterFilter')
def afterForTestNoResultForAfterFilter(context, flute, request):
    foo = 'bar'

@Mapping('/applicationFormUrlencoded', 'POST', 'application/x-www-form-urlencoded')
def handlerForApplicationFormUrlencoded(context, flute, request):
    data = request.body(BodyExtractors.toFormData()).block()

    result = 'param1=';

    param1 = data['param1'];
    for val in param1:
        result = result + val + ","

    result = result + 'param2=';
    param2 = data['param2'];
    for val in param2:
        result = result + val + ","

    return ServerResponse.ok().body(Mono.just(result), String)