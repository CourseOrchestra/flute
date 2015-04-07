#   coding=utf-8
import ru.curs.flute.fastxl as fastxl

def run(context, params):
    # for 'context' attributes see Celesta manual.
    # 'params' has the following attributes:
    #   taskid Flute's task id
    #   params  XML parameters passed as string
    #   resultstream OutputStream to write the result
    #   message Script's message to log

    #	put the path to your own Excel template here
    template='c:/temp/blah.xlsx'

    #	write any Python stuff you want here, using the variables described above
    proc = fastxl.FastXLProcessor(context.conn, template, params.params, params.resultstream)
    proc.execute()
