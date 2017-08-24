#   coding=utf-8
from flute.mappingbuilder import mapping, filterBefore, filterAfter
from java.lang import String
from java.util import ArrayList, LinkedHashMap
from org.springframework.web.reactive.function.server import ServerResponse
from org.springframework.http import HttpStatus, MediaType
from reactor.core.publisher import Mono

@mapping('/foo')
def foo(context, request):
    dto = {
        "foo": 1,
        "bar": 2
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@mapping('/params')
def params(context, request):

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


@mapping('/jsonPost', "POST")
def postWithJsonBody(context, request):
    dto = request.bodyToMono(dict).block()
    dto['numb'] = dto['numb'] + 1
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@filterBefore('/beforeTest')
def beforeFilter(context, request):
    request.attributes().put('data', {'foo': 1})

@mapping('/beforeTest')
def handlerForBeforeFilter(context, request):
    data = request.attributes().get('data')
    data['foo'] = data['foo'] + 1
    return ServerResponse.ok().body(Mono.just(data), data.__class__)


@filterBefore('/doubleBeforeTest')
def doubleBeforeFilter1(context, request):
    request.attributes().put('data', ArrayList())
    data = request.attributes().get('data')
    data.add(1)

@filterBefore('/doubleBeforeTes*')
def doubleBeforeFilter2(context, request):
    data = request.attributes().get('data')
    data.add(2)

@mapping('/doubleBeforeTest')
def handlerForDoubleBeforeFilter(context, request):
    data = request.attributes().get('data')
    data.add(3)
    return ServerResponse.ok().body(Mono.just(data), data.__class__)

@filterAfter('/afterTest')
def afterFilter(context, request):
    data = request.attributes().get('data')
    data['foo'] = data['foo'] + 1
    return ServerResponse.ok().body(Mono.just(data), data.__class__)

@mapping('/afterTest')
def handlerForAfterFilter(context, request):
    request.attributes().put('data', {'foo': 1})

@filterAfter('/doubleAfterTest')
def doubleAfterFilter1(context, request):
    data = request.attributes().get('data')
    data.add(2)

@filterAfter('/doubleAfterTes*')
def doubleAfterFilter2(context, request):
    data = request.attributes().get('data')
    data.add(3)
    return ServerResponse.ok().body(Mono.just(data), data.__class__)

@mapping('/doubleAfterTest')
def handlerForDoubleAfterFilter(context, request):
    request.attributes().put('data', ArrayList())
    data = request.attributes().get('data')
    data.add(1)

@filterAfter('/afterAndBeforeTest')
def afterAndBeforeAfterFilter(context, request):
    data = request.attributes().get('data')
    data.add(3)
    return ServerResponse.ok().body(Mono.just(data), data.__class__)

@filterBefore('/afterAndBeforeTest')
def afterAndBeforeBeforeFilter(context, request):
    request.attributes().put('data', ArrayList())
    data = request.attributes().get('data')
    data.add(1)

@mapping('/afterAndBeforeTest')
def handlerForAfterAndBeforeFilter(context, request):
    data = request.attributes().get('data')
    data.add(2)


@mapping('/globalCelestaUserIdTest')
def handlerForGlobalCelestaUserId(context, request):
    dto = {
        "userId": context.getUserId()
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@filterBefore('/customCelestaUserIdTest')
def customCelestaUserIdFilterTest(context, request):
    request.attributes().put('userId', 'testUser')

@mapping('/customCelestaUserIdTest')
def handlerForCustomCelestaUserIdFilter(context, request):
    dto = {
        "userId": context.getUserId()
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)


@filterBefore('/testReturnFromBefore')
def beforeTestReturnFromBefore(context, request):
    dto = {
        "foo": 1
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@mapping('/testReturnFromBefore')
def handlerForTestReturnFromBefore(context, request):
    dto = {
        "foo": 2
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@filterAfter('/testReturnFromBefore')
def afterTestReturnFromBefore(context, request):
    dto = {
        "foo": 3
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@mapping('/testReturnFromHandler')
def handlerForTestReturnFromHandler(context, request):
    dto = {
        "foo": 1
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@filterAfter('/testReturnFromHandler')
def afterTestReturnFromHandler(context, request):
    dto = {
        "foo": 2
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)


@mapping('/testReturnFromAfter')
def handlerForTestReturnFromAfter(context, request):
    foo = 'bar'

@filterAfter('/testReturnFromAfter')
def after1TestReturnFromAfter(context, request):
    dto = {
        "foo": 1
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@filterAfter('/testReturnFromAfter')
def after2TestReturnFromAfter(context, request):
    dto = {
        "foo": 2
    }
    return ServerResponse.ok().body(Mono.just(dto), dto.__class__)

@mapping('/testNoResultForHandler')
def handlerForTestNoResultForHandler(context, request):
    foo = 'bar'

@mapping('/testNoResultForAfterFilter')
def handlerForTestNoResultForAfterFilter(context, request):
    foo = 'bar'

@filterAfter('/testNoResultForAfterFilter')
def afterForTestNoResultForAfterFilter(context, request):
    foo = 'bar'