#Any sript can use the following variables:
#	taskid	Flute's task id
#	params  XML parameters passed as string
#	conn	JDBC connection from the connecton pool (you should not close it, it will be reused!)
#	resultstream	OutputStream to write the result
#	message Script's message to log
import ru.curs.flute.xml2spreadsheet.XML2Spreadsheet as XML2Spreadsheet
import java.io.File as File
import java.io.FileInputStream as FileInputStream
import java.io.FileOutputStream as FileOutputStream

template=File('c:/temp/xml2spreadsheet/template.xls')
descriptor=File('c:/temp/xml2spreadsheet/descriptor.xml')

data=FileInputStream('c:/temp/xml2spreadsheet/data.xml')
result=FileOutputStream('c:/temp/xml2spreadsheet/result.xls')

# Parameters: 
# 1. Input data stream.
# 2. Descriptor file, describes the order of iterations through XML source data.
# 3. Template file. Template's type (XLS, XLSX or ODS) is being defined based on file extension.
# 4. Processing mode ('False' for DOM and 'True' for SAX).
# 5. Output data stream.
try:
	XML2Spreadsheet.process(data, descriptor, template, False, result)
finally:
	result.close()