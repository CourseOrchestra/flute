#   coding=utf-8
#
# Any sript can use the following variables:
#	taskid	Flute's task id
#	params  XML parameters passed as string
#	conn	JDBC connection from the connecton pool (you should not close it, it will be reused!)
#	resultstream	OutputStream to write the result
#   message Script's message to log
# and procedure repair(conn) to obtain repaired version of connection, if needed

import ru.curs.flute.fastxl as fastxl

#	put the path to your own Excel template here
template='c:/temp/blah.xlsx'

#	write any Python stuff you want here, using the variables described above

proc = fastxl.FastXLProcessor(conn, template, params, resultstream)
proc.execute()
