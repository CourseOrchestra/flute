#   coding=utf-8

def run(context, params):
    # for 'context' attributes see Celesta manual.
    # 'params' has the following attributes:
    #   taskid Flute's task id
    #   params  XML parameters passed as string
    #   resultstream OutputStream to write the result
    #   message Script's message to log

    params.message = 'success! taskid: %d, param: %s' % (params.taskid, params.params)
