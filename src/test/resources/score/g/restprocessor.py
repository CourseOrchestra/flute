#   coding=utf-8
from java.util import ArrayList

from ru.curs.flute.rest import FluteResponse
from ru.curs.flute.rest.decorator import Mapping, FilterBefore, FilterAfter

from _g_orm import table1Cursor

import json

@Mapping('/foo')
def foo(context, flute, request):
    dto = {
        "foo": 1,
        "bar": 2
    }
    return json.dumps(dto)

@Mapping('/fluteparam', "GET")
def fluteparam(context, flute, request):
    sid = flute.sourceId
    return sid

@Mapping('/params')
def params(context, flute, request):

    text = request.pathInfo()
    status = None

    params = request.queryMap().toMap()

    if params.size() != 0:
        text = text + "?"

        theFirst = True

        for key in params.keySet():

            for val in params.get(key):
                if theFirst != True:
                    text = text + "&"
                text = text + key + "=" + val
                theFirst = False

        status = 200
    else:
        text = "there are no params received"
        status = 422

    return FluteResponse.status(status).text(text)


@Mapping('/jsonPost', "POST")
def postWithJsonBody(context, flute, request):
    dto = json.loads(request.body())
    dto['numb'] = dto['numb'] + 1
    return json.dumps(dto)

@FilterBefore('/beforeTest')
def beforeFilter(context, flute, request):
    request.attribute('data', {'foo': 1})

@Mapping('/beforeTest')
def handlerForBeforeFilter(context, flute, request):
    data = request.attribute('data')
    data['foo'] = data['foo'] + 1
    return json.dumps(data)

@FilterBefore('/double/before/*')
def doubleBeforeFilter1(context, flute, request):
    request.attribute('data', ArrayList())
    data = request.attribute('data')
    data.add(1)

@FilterBefore('/double/before/test')
def doubleBeforeFilter2(context, flute, request):
    data = request.attribute('data')
    data.add(2)

@Mapping('/double/before/test')
def handlerForDoubleBeforeFilter(context, flute, request):
    data = request.attribute('data')
    data.add(3)
    return data

@FilterAfter('/afterTest')
def afterFilter(context, flute, request):
    data = request.attribute('data')
    data['foo'] = data['foo'] + 1
    return json.dumps(data)

@Mapping('/afterTest')
def handlerForAfterFilter(context, flute, request):
    request.attribute('data', {'foo': 1})

@FilterAfter('/double/after/test')
def doubleAfterFilter1(context, flute, request):
    data = request.attribute('data')
    data.add(2)

@FilterAfter('/double/after/*')
def doubleAfterFilter2(context, flute, request):
    data = request.attribute('data')
    data.add(3)
    return data

@Mapping('/double/after/test')
def handlerForDoubleAfterFilter(context, flute, request):
    request.attribute('data', ArrayList())
    data = request.attribute('data')
    data.add(1)

@FilterAfter('/afterAndBeforeTest')
def afterAndBeforeAfterFilter(context, flute, request):
    data = request.attribute('data')
    data.add(3)
    return data

@FilterBefore('/afterAndBeforeTest')
def afterAndBeforeBeforeFilter(context, flute, request):
    request.attribute('data', ArrayList())
    data = request.attribute('data')
    data.add(1)

@Mapping('/afterAndBeforeTest')
def handlerForAfterAndBeforeFilter(context, flute, request):
    data = request.attribute('data')
    data.add(2)


@Mapping('/globalCelestaUserIdTest')
def handlerForGlobalCelestaUserId(context, flute, request):
    dto = {
        "userId": context.getUserId()
    }
    return json.dumps(dto)

@FilterBefore('/customCelestaUserIdTest')
def customCelestaUserIdFilterTest(context, flute, request):
    request.attribute('userId', 'testUser')

@Mapping('/customCelestaUserIdTest')
def handlerForCustomCelestaUserIdFilter(context, flute, request):
    dto = {
        "userId": context.getUserId()
    }
    return json.dumps(dto)


@FilterBefore('/testReturnFromBefore')
def beforeTestReturnFromBefore(context, flute, request):
    dto = {
        "foo": 1
    }
    return json.dumps(dto)

@Mapping('/testReturnFromBefore')
def handlerForTestReturnFromBefore(context, flute, request):
    dto = {
        "foo": 2
    }
    return json.dumps(dto)

@FilterAfter('/testReturnFromBefore')
def afterTestReturnFromBefore(context, flute, request):
    dto = {
        "foo": 3
    }
    return json.dumps(dto)

@Mapping('/testReturnFromHandler')
def handlerForTestReturnFromHandler(context, flute, request):
    dto = {
        "foo": 1
    }
    return json.dumps(dto)

@FilterAfter('/testReturnFromHandler')
def afterTestReturnFromHandler(context, flute, request):
    dto = {
        "foo": 2
    }
    return json.dumps(dto)


@Mapping('/testReturnFromAfter')
def handlerForTestReturnFromAfter(context, flute, request):
    foo = 'bar'

@FilterAfter('/testReturnFromAfter')
def after1TestReturnFromAfter(context, flute, request):
    dto = {
        "foo": 1
    }
    return json.dumps(dto)

@FilterAfter('/testReturnFromAfter')
def after2TestReturnFromAfter(context, flute, request):
    dto = {
        "foo": 2
    }
    return json.dumps(dto)

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
    queryMap = request.queryMap()

    result = 'param1='

    param1 = queryMap.get(['param1']).values()
    for val in param1:
        result = result + val + ","

    result = result + 'param2='
    param2 = queryMap.get(['param2']).values()
    for val in param2:
        result = result + val + ","

    return result

@Mapping('/count')
def count(context, flute, request):
    c = table1Cursor(context)
    dto = {'count': c.count()}
    return json.dumps(dto)