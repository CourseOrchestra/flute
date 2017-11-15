/*
Это сервис, поэтому установка только глобальная. 

- стартменю и рабочий стол --- глобальные 
- все установки в HKLM
- папку стартового меню в HKLM
- анинсталлерную информацию в HKLM
- режим установки в HKLM

Деинсталляция:

- Читаем режим установки из HKLM
- Удаляем стартменю и рабочий стол (автоматом на основе выставленного режима).
- Удаляем программные файлы из установочной директории
- Чистим реестр в HKLM
*/

!define PRODUCT_NAME "Flute"

;Строка с версией продукта генерируется автоматически Ant-скриптом!
!include "prodversion.nsh"
!include "TextFunc.nsh"
!define PRODUCT_PUBLISHER "ООО «КУРС»"
!define PRODUCT_WEB_SITE "http://www.curs.ru"
!define PRODUCT_DIR_REGKEY "Software\Microsoft\Windows\CurrentVersion\App Paths\${PRODUCT_NAME}-${PRODUCT_VERSION}"
!define PRODUCT_UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}-${PRODUCT_VERSION}"
!define PRODUCT_SETUP_KEY "Software\CURS\${PRODUCT_NAME}-${PRODUCT_VERSION}"

!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_RIGHT
!define MUI_HEADERIMAGE_BITMAP "headerimage.bmp"
!define MUI_WELCOMEFINISHPAGE_BITMAP "welcomepage.bmp"

RequestExecutionLevel admin
BrandingText " "

SetCompressor lzma

!include "dumpLog.nsh"
!include MUI2.nsh
!include "FileFunc.nsh"
!include "WinVer.nsh"


Var StartMenuFolder
Var JavaHome
Var JavaExe
Var JvmDll
Var Arch
Var ResetInstDir
Var FluteServiceName
Var FluteServiceDefaultName
Var FluteServiceFileName
Var FluteServiceManagerFileName
Var ScorePath

; Variables that store handles of dialog controls
Var CtlJavaHome
Var CtlFluteServiceName
Var CtlScorePath


!define MUI_ABORTWARNING
!define MUI_ICON "${NSISDIR}\Contrib\Graphics\Icons\modern-install.ico"
!define MUI_UNICON "${NSISDIR}\Contrib\Graphics\Icons\modern-uninstall.ico"

;WELCOME
!insertmacro MUI_PAGE_WELCOME

;ВЫБОР JAVA-виртуальной машины
Page custom pageChooseJVM pageChooseJVMLeave "$(TEXT_JVM_PAGETITLE)"

;ВЫБОР КОМПОНЕНТ ДЛЯ УСТАНОВКИ
!insertmacro MUI_PAGE_COMPONENTS


;ВЫБОР ДИРЕКТОРИИ ДЛЯ УСТАНОВКИ
!insertmacro MUI_PAGE_DIRECTORY

;ВЫБОР ПАПКИ ДЛЯ МЕНЮ ПУСК
!define MUI_STARTMENUPAGE_REGISTRY_ROOT "HKLM" 
!define MUI_STARTMENUPAGE_REGISTRY_KEY ${PRODUCT_SETUP_KEY}
!define MUI_STARTMENUPAGE_DEFAULTFOLDER "КУРС\${PRODUCT_NAME}-${PRODUCT_VERSION}"
!define MUI_STARTMENUPAGE_REGISTRY_VALUENAME "StartMenuFolder"
!insertmacro MUI_PAGE_STARTMENU APPLICATION $StartMenuFolder

;УСТАНОВКА ФАЙЛОВ
!insertmacro MUI_PAGE_INSTFILES

;ПРОВЕРКА ТИПА ПОЛЬЗОВАТЕЛЯ
Page custom CheckUserType

;ПОСЛЕДНЯЯ СТРАНИЦА:  СОЗДАНИЕ ЯРЛЫКА НА ДЕСКТОПЕ
;!define MUI_FINISHPAGE_RUN "$INSTDIR\FormsServer.exe"
;!define MUI_FINISHPAGE_RUN_FUNCTION "LaunchLink"

!define MUI_FINISHPAGE_SHOWREADME
!define MUI_FINISHPAGE_SHOWREADME_TEXT "Создать ярлык на рабочем столе"
!define MUI_FINISHPAGE_SHOWREADME_NOTCHECKED
!define MUI_FINISHPAGE_SHOWREADME_FUNCTION InstallDesktopShortcut
!insertmacro MUI_PAGE_FINISH

;===========================================================
; Uninstaller pages
!insertmacro MUI_UNPAGE_INSTFILES

; Language files
!insertmacro MUI_LANGUAGE "Russian"
!insertmacro MUI_LANGUAGE "English"
LangString TEXT_JVM_TITLE ${LANG_ENGLISH} "Service parameters"
LangString TEXT_JVM_TITLE ${LANG_RUSSIAN} "Параметры сервиса"
LangString TEXT_JVM_SUBTITLE ${LANG_ENGLISH} "Java Virtual Machine path and service name selection."
LangString TEXT_JVM_SUBTITLE ${LANG_RUSSIAN} "Выбор пути к виртуальной машине Java и имени сервиса."
LangString TEXT_JVM_PAGETITLE ${LANG_ENGLISH} ": Java Virtual Machine path and service name selection"
LangString TEXT_JVM_PAGETITLE ${LANG_RUSSIAN} ": выбор пути к виртуальной машине Java и имени сервиса"
LangString TEXT_JVM_LABEL1 ${LANG_ENGLISH} "Please select the path of a Java SE 7.0 or later JRE installed on your system."
LangString TEXT_JVM_LABEL1 ${LANG_RUSSIAN} "Укажите папку установки Java SE 7.0 или более поздней версии JRE на Вашем компьютере."
LangString TEXT_JVM_LABEL2 ${LANG_ENGLISH} "Please select the Celesta score path."
LangString TEXT_JVM_LABEL2 ${LANG_RUSSIAN} "Укажите папку Celesta score."
LangString TEXT_CONF_LABEL_SERVICE_NAME ${LANG_ENGLISH} "Windows Service Name"
LangString TEXT_CONF_LABEL_SERVICE_NAME ${LANG_RUSSIAN} "Название сервиса Windows"

; MUI end ------

Function .onInit
  ${IfNot} ${IsNT}
    MessageBox MB_OK|MB_ICONEXCLAMATION "Несовместимая версия Windows. Требуется ОС c ядром NT."
    Abort
  ${EndIf}

  System::Call 'kernel32::CreateMutexA(i 0, i 0, t "FormsServerInstaller") i .r1 ?e'
  Pop $R0
  StrCmp $R0 0 +3
    MessageBox MB_OK|MB_ICONEXCLAMATION "Инсталлятор уже запущен."
    Abort
  StrCpy $ResetInstDir "$INSTDIR"
  ;Initialize default values
  StrCpy $JavaHome ""
  StrCpy $FluteServiceDefaultName "${PRODUCT_NAME}-${PRODUCT_VERSION}"
  StrCpy $FluteServiceName $FluteServiceDefaultName
  StrCpy $FluteServiceFileName "${PRODUCT_NAME}-${PRODUCT_VERSION}.exe"
  StrCpy $FluteServiceManagerFileName "${PRODUCT_NAME}-${PRODUCT_VERSION}w.exe"

FunctionEnd

Function .onMouseOverSection
    FindWindow $R0 "#32770" "" $HWNDPARENT
    GetDlgItem $R0 $R0 1043 ; description item (must be added to the UI)

    StrCmp $0 0 "" +2
      SendMessage $R0 ${WM_SETTEXT} 0 "STR:Система Flute."

    StrCmp $0 1 "" +2
      SendMessage $R0 ${WM_SETTEXT} 0 "STR:Компонента быстрого построения больших отчётов в формате XLSX на основе результатов выполнения хранимых процедур."

    StrCmp $0 2 "" +2
      SendMessage $R0 ${WM_SETTEXT} 0 "STR:Компонента гибкого построения отчётов в различных форматах и библиотека Apache POI для формирования MS Office файлов"
    
    StrCmp $0 3 "" +2
      SendMessage $R0 ${WM_SETTEXT} 0 "STR:Стандартная библиотека Python."
    
    StrCmp $0 4 "" +2
      SendMessage $R0 ${WM_SETTEXT} 0 "STR:Система вывода Excel-файлов на печать и в формат PDF."
    
    StrCmp $0 5 "" +2
      SendMessage $R0 ${WM_SETTEXT} 0 "STR:Библиотеки для работы с JDBC."
FunctionEnd


Name "${PRODUCT_NAME}-${PRODUCT_VERSION}"
OutFile "flute-setup.exe"

InstallDirRegKey HKLM "${PRODUCT_DIR_REGKEY}" ""
ShowInstDetails show
ShowUnInstDetails show

Section "Flute" SEC01

  SectionIn RO

  DetailPrint "-----------------------------"
  DetailPrint "JVM Path: $JvmDll."
  DetailPrint "Architecture: $Arch."
  DetailPrint "Service name: $FluteServiceName."
  DetailPrint "-----------------------------"
  
  SetOutPath "$INSTDIR"
  File flute-6.0.jar
  SetOverwrite off
  File flute.xml
  SetOverwrite on
  
  Push $ScorePath
  Push "\"
  Call StrSlash
  Pop $R0
  #${nsisXML->OpenXML} "$INSTDIR\flute.xml"
  #${nsisXML->SetElementText} "//scorepath" $R0 $R0
  #${nsisXML->CloseXML}

  Push "C:/score" #text to be replaced
  Push $R0 #replace with
  Push all #start replacing after 1st occurrence
  Push all #replace next 4 occurrences
  Push "$INSTDIR\flute.xml" #file to replace in
  Call AdvReplaceInFile

  Push "$INSTDIR\pylib"
  Push "\"
  Call StrSlash
  Pop $R0
  #${nsisXML->OpenXML} "$INSTDIR\flute.xml"
  #${nsisXML->SetElementText} "//pylibpath" $R0 $R0
  #${nsisXML->CloseXML}
  Push "C:/Program Files/KURS/Flute-6.0/pylib" #text to be replaced
  Push $R0 #replace with
  Push all #start replacing after 1st occurrence
  Push all #replace next 4 occurrences
  Push "$INSTDIR\flute.xml" #file to replace in
  Call AdvReplaceInFile
  
  ;Link to website
  WriteIniStr "$INSTDIR\curs.url" "InternetShortcut" "URL" "${PRODUCT_WEB_SITE}"
  
  CreateDirectory $INSTDIR\logs
  CreateDirectory $INSTDIR\lib
  CreateDirectory $INSTDIR\pylib
  
  SetOutPath $ScorePath\flute
  SetOverwrite ifnewer
  File flute\__init__.py
  File flute\_flute.sql
  File flute\hello.py
  SetOverwrite on

  SetOutPath $INSTDIR\bin
  StrCpy $R0 $FluteServiceName
  StrCpy $FluteServiceFileName $R0.exe
  StrCpy $FluteServiceManagerFileName $R0w.exe

  SetOutPath $INSTDIR\bin
  File /oname=$FluteServiceManagerFileName procrun\prunmgr.exe

  ; Get the current platform x86 / AMD64 / IA64
  ${If} $Arch == "x86"
    File /oname=$FluteServiceFileName procrun\prunsrv.exe
  ${ElseIf} $Arch == "x64"
    File /oname=$FluteServiceFileName procrun\amd64\prunsrv.exe
  ${ElseIf} $Arch == "i64"
    File /oname=$FluteServiceFileName procrun\ia64\prunsrv.exe
  ${EndIf}
  
  InstallRetry:
  DetailPrint "Installing $FluteServiceName service"
  nsExec::ExecToStack '"$INSTDIR\bin\$FluteServiceFileName" //IS//$FluteServiceName --DisplayName "$FluteServiceName" --Description "${PRODUCT_PUBLISHER}: ${BUILD_VERSION}, http://www.curs.ru/" --LogPath "$INSTDIR\logs" --Install "$INSTDIR\bin\$FluteServiceFileName" --Jvm "$JvmDll" --StartMode=jvm --StartClass=ru.curs.flute.Main --StartParams=start --StopMode=jvm --StopClass=ru.curs.flute.Main --StopParams=stop --Classpath="$INSTDIR\flute-6.0.jar"  --StdOutput=auto --StdError=auto'
  Pop $0
  Pop $1
  StrCmp $0 "0" InstallOk
    DetailPrint "Install failed: $0 $1$\r$\n"
    MessageBox MB_ABORTRETRYIGNORE|MB_ICONSTOP \
      "Failed to install $FluteServiceName service.$\r$\nMay be the service already exists.$\r$\nCheck your settings and permissions.$\r$\nIgnore and continue anyway (not recommended)?" \
       /SD IDIGNORE IDIGNORE InstallOk IDRETRY InstallRetry
  Quit
  InstallOk:
  ClearErrors
  
  ;Корень (локальные или глобальные папки) автоматом определяется из контекста установки
  SetOutPath "$INSTDIR" ; Это нужно для прописывания рабочей папки в ярлыках
  !insertmacro MUI_STARTMENU_WRITE_BEGIN APPLICATION
    
  CreateDirectory "$SMPROGRAMS\$StartMenuFolder"
  CreateShortCut "$SMPROGRAMS\$StartMenuFolder\Monitor Flute.lnk" "$INSTDIR\bin\$FluteServiceManagerFileName" '//MS//$FluteServiceName' 
  CreateShortCut "$SMPROGRAMS\$StartMenuFolder\Configure Flute.lnk" "$INSTDIR\bin\$FluteServiceManagerFileName" '//ES//$FluteServiceName'
  CreateShortCut "$SMPROGRAMS\$StartMenuFolder\www.curs.ru.lnk" "$INSTDIR\curs.url"
  CreateShortCut "$SMPROGRAMS\$StartMenuFolder\Uninstall.lnk" "$INSTDIR\uninst.exe" '-ServiceName="$FluteServiceName"'
  !insertmacro MUI_STARTMENU_WRITE_END
  
  ${dumpLog} "$INSTDIR\install.log"
SectionEnd

Section "FastXL" SEC02
  SetOutPath $ScorePath\flute
  File flute\fastxl.py
SectionEnd

Section "Apache POI-ooxml" SEC03 
  SetOutPath $ScorePath\flute
  File flute\xml2spreadsheet.py
SectionEnd

Section "Python library" SEC04
  SetOutPath $INSTDIR
  File /r /x .svn pylib 
SectionEnd

Section "Xylophone, Excel2print, Apache POI-Scratchpad" SEC05
    SetOutPath $INSTDIR\lib
    File /r "..\lib\*.*"
SectionEnd

Section -AdditionalIcons
SectionEnd

Section -Post
  WriteUninstaller "$INSTDIR\uninst.exe"
  WriteRegStr HKLM "${PRODUCT_DIR_REGKEY}" "" "$INSTDIR\flute-6.0.jar"

  WriteRegStr HKLM "${PRODUCT_UNINST_KEY}" "DisplayName" "$(^Name)"
  WriteRegStr HKLM "${PRODUCT_UNINST_KEY}" "UninstallString" "$\"$INSTDIR\uninst.exe$\" -ServiceName=$\"$FluteServiceName$\""
 ; WriteRegStr HKLM "${PRODUCT_UNINST_KEY}" "DisplayIcon" "$INSTDIR\FormsServer.ico"
  WriteRegStr HKLM "${PRODUCT_UNINST_KEY}" "DisplayVersion" "${PRODUCT_VERSION}"
  WriteRegStr HKLM "${PRODUCT_UNINST_KEY}" "URLInfoAbout" "${PRODUCT_WEB_SITE}"
  WriteRegStr HKLM "${PRODUCT_UNINST_KEY}" "Publisher" "${PRODUCT_PUBLISHER}"

SectionEnd

Function un.onInit
  ${GetParameters} $R0
  ${GetOptions} $R0 "-ServiceName=" $R1
  StrCpy $FluteServiceName $R1
  StrCpy $FluteServiceFileName $R1.exe
  StrCpy $FluteServiceManagerFileName $R1w.exe
  MessageBox MB_ICONQUESTION|MB_YESNO|MB_DEFBUTTON2 "Вы уверены в том, что желаете удалить $(^Name)?" IDYES +2
  Abort
FunctionEnd

Function un.onUninstSuccess
  HideWindow
  MessageBox MB_ICONINFORMATION|MB_OK "Удаление $(^Name) было успешно завершено. Удалите Java Runtime Environment, если вы не используете его в других приложениях"
FunctionEnd

Section Uninstall
  ${If} $FluteServiceName == ""
    MessageBox MB_ICONSTOP|MB_OK \
        "No service name specified to uninstall. This will be provided automatically if you uninstall via \
         Add/Remove Programs or the shortcut on the Start menu. Alternatively, call the installer from \
         the command line with -ServiceName=$\"<name of service>$\"."
    Quit
  ${EndIf}

  ; Stop Flute service monitor if running
  DetailPrint "Stopping $FluteServiceName service monitor"
  nsExec::ExecToLog '"$INSTDIR\bin\$FluteServiceManagerFileName" //MQ//$FluteServiceName'
  ; Delete Flute service
  DetailPrint "Uninstalling $FluteServiceName service"
  nsExec::ExecToLog '"$INSTDIR\bin\$FluteServiceFileName" //DS//$FluteServiceName'
  ClearErrors

  ;Удаляем программные и вспомогательные файлы
  Delete "$INSTDIR\flute-6.0.jar"
  Delete "$INSTDIR\flute.xml"

  Delete "$INSTDIR\curs.url"
  Delete "$INSTDIR\install.log"
  Delete "$INSTDIR\uninst.exe"
  
  ;Безоглядно удаляем логи, библиотеки и кэшированную директорию
  RMDir  /r "$INSTDIR\logs"
  RMDir  /r "$INSTDIR\lib"
  RMDir  /r "$INSTDIR\pylib"
  RMDir  /r "$INSTDIR\cachedir"
    
  ;Удаляем директорию bin с сервис-раннером
  Delete "$INSTDIR\bin\$FluteServiceManagerFileName"
  Delete "$INSTDIR\bin\$FluteServiceFileName"
  RMDir  "$INSTDIR\bin"
  RMDir  "$INSTDIR"
  
  ;Удаляем ярлыки
  !insertmacro MUI_STARTMENU_GETFOLDER Application $StartMenuFolder
  Delete "$SMPROGRAMS\$StartMenuFolder\Uninstall.lnk"
  Delete "$SMPROGRAMS\$StartMenuFolder\www.curs.ru.lnk"
  Delete "$SMPROGRAMS\$StartMenuFolder\Monitor Flute.lnk"
  Delete "$SMPROGRAMS\$StartMenuFolder\Configure Flute.lnk"
  
  Delete "$DESKTOP\Configure Flute.lnk"
  RMDir "$SMPROGRAMS\$StartMenuFolder"

  ;Подчищаем реестр
 
  ; Don't know if 32-bit or 64-bit registry was used so, for now, remove both
  SetRegView 32
  DeleteRegKey HKLM "${PRODUCT_SETUP_KEY}"
  DeleteRegKey /ifempty HKLM "Software\CURS"
  DeleteRegKey HKLM "${PRODUCT_UNINST_KEY}"
  DeleteRegKey HKLM "${PRODUCT_DIR_REGKEY}"

  SetRegView 64
  DeleteRegKey HKLM "${PRODUCT_SETUP_KEY}"
  DeleteRegKey /ifempty HKLM "Software\CURS"
  DeleteRegKey HKLM "${PRODUCT_UNINST_KEY}"
  DeleteRegKey HKLM "${PRODUCT_DIR_REGKEY}"

  ; SetAutoClose true
SectionEnd
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

Function pageChooseJVM
  !insertmacro MUI_HEADER_TEXT "$(TEXT_JVM_TITLE)" "$(TEXT_JVM_SUBTITLE)"
  ${If} $JavaHome == ""
    Call findJavaHome
    Pop $JavaHome
  ${EndIf}

  ${If} $ScorePath == ""
    StrCpy $ScorePath 'C:\score'
  ${EndIf}

  nsDialogs::Create 1018
  Pop $R0

  ${NSD_CreateLabel} 0 5u 100% 25u "$(TEXT_JVM_LABEL1)"
  Pop $R0

  ${NSD_CreateDirRequest} 0 30u 280u 13u "$JavaHome"
  Pop $CtlJavaHome
  ${NSD_CreateBrowseButton} 282u 30u 15u 13u "..."
  Pop $R0
  ${NSD_OnClick} $R0 pageChooseJVM_onDirBrowse
  
  ${NSD_CreateLabel} 0 55u 100% 14u "$(TEXT_JVM_LABEL2)"
  Pop $R0
  ${NSD_CreateDirRequest} 0 70u 280u 13u "$ScorePath"
  Pop $CtlScorePath
  ${NSD_CreateBrowseButton} 282u 70u 15u 13u "..."
  Pop $R0
  ${NSD_OnClick} $R0 chooseScorePath_onDirBrowse


  ${NSD_CreateLabel} 0 100u 140u 14u "$(TEXT_CONF_LABEL_SERVICE_NAME)"
  Pop $R0

  ${NSD_CreateText} 150u 98u 140u 12u "$FluteServiceName"
  Pop $CtlFluteServiceName

  ${NSD_SetFocus} $CtlJavaHome
  nsDialogs::Show
FunctionEnd

Function chooseScorePath_onDirBrowse
  ; R0 is HWND of the button, it is on top of the stack
  Pop $R0

  ${NSD_GetText} $CtlScorePath $R1
  nsDialogs::SelectFolderDialog "" "$R1"
  Pop $R1

  ${If} $R1 != "error"
    ${NSD_SetText} $CtlScorePath $R1
  ${EndIf}
FunctionEnd

; onClick function for button next to DirRequest control
Function pageChooseJVM_onDirBrowse
  ; R0 is HWND of the button, it is on top of the stack
  Pop $R0

  ${NSD_GetText} $CtlJavaHome $R1
  nsDialogs::SelectFolderDialog "" "$R1"
  Pop $R1

  ${If} $R1 != "error"
    ${NSD_SetText} $CtlJavaHome $R1
  ${EndIf}
FunctionEnd

Function pageChooseJVMLeave
  ${NSD_GetText} $CtlJavaHome $JavaHome
  ${If} $JavaHome == ""
    MessageBox MB_ICONEXCLAMATION|MB_OK 'Choose valid Java installation directory'
    Abort 
  ${EndIf}

  ${NSD_GetText} $CtlScorePath $ScorePath
  ${If} $ScorePath == ""
    MessageBox MB_ICONEXCLAMATION|MB_OK 'Choose score path'
    Abort 
  ${EndIf}

  Call checkJava
  Pop $0
  IntCmp $0 0 checkJavaVer
  Abort "Config not right"
  
  checkJavaVer:
  Call checkJavaVersion
  Pop $0
  IntCmp $0 0 checkService
  Abort "Config not right"
  
  checkService:
  
  ${NSD_GetText} $CtlFluteServiceName $FluteServiceName

  ${If} $FluteServiceName == ""
    MessageBox MB_ICONEXCLAMATION|MB_OK 'The Service Name may not be empty'
    Abort "Config not right"
    Goto exit
  ${EndIf}

  Push $FluteServiceName
  Call validateServiceName
  Pop $0
  
  IntCmp $0 1 exit
  MessageBox MB_ICONEXCLAMATION|MB_OK 'The Service Name may not contain a space or any of the following characters: <>:"/\:|?*'
  Abort "Config not right"
  exit:
FunctionEnd

; Validates that a service name does not use any of the invalid
; characters: <>:"/\:|?*
; Note that space is also not permitted although it will be once
; Flute is using Daemon 1.0.6 or later
;
; Put the proposed service name on the stack
; If the name is valid, a 1 will be left on the stack
; If the name is invalid, a 0 will be left on the stack
Function validateServiceName
  Pop $0
  StrLen $1 $0
  StrCpy $3 '<>:"/\:|?* '
  StrLen $4 $3
  
  loopInput:
    IntOp $1 $1 - 1
    IntCmp $1 -1 valid
    loopTestChars:
      IntOp $4 $4 - 1
      IntCmp $4 -1 loopTestCharsDone
      StrCpy $2 $0 1 $1
      StrCpy $5 $3 1 $4
      StrCmp $2 $5 invalid loopTestChars
    loopTestCharsDone:
    StrLen $4 $3
    Goto loopInput

  invalid:
  Push 0
  Goto exit
  
  valid:
  Push 1
  exit:
FunctionEnd

Function checkJavaVersion
  Push $0
  Push $1
  ${GetFileVersion} "$JavaExe" $0
  StrCpy $1 $0 1 0
  IntCmp 7 $1 FoundCorrectJavaVer FoundCorrectJavaVer JavaVerNotCorrect
  FoundCorrectJavaVer:
    Pop $1
    Pop $0
    Push 0
	Goto Done
  JavaVerNotCorrect:
	MessageBox MB_OK|MB_ICONEXCLAMATION "Java version 7 or later required, found version $0"
    Pop $1
    Pop $0
    Push 1
   Done:
FunctionEnd

; ==================
; checkJava Function
; ==================
;
; Checks that a valid JVM has been specified or a suitable default is available
; Sets $JavaHome, $JavaExe and $JvmDll accordingly
; Determines if the JVM is 32-bit or 64-bit and sets $Arch accordingly. For
; 64-bit JVMs, also determines if it is x64 or ia64
Function checkJava

  ${If} $JavaHome == ""
    ; E.g. if a silent install
    Call findJavaHome
    Pop $JavaHome
  ${EndIf}

  ${If} $JavaHome == ""
  ${OrIfNot} ${FileExists} "$JavaHome\bin\java.exe"
    IfSilent +2
    MessageBox MB_OK|MB_ICONEXCLAMATION "No Java Virtual Machine found in folder:$\r$\n$JavaHome"
    DetailPrint "No Java Virtual Machine found in folder:$\r$\n$JavaHome"
    Push 1
	Abort
	;Quit
  ${EndIf}

  StrCpy "$JavaExe" "$JavaHome\bin\java.exe"

  ; Need path to jvm.dll to configure the service - uses $JavaHome
  Call findJVMPath
  Pop $5
  ${If} $5 == ""
    IfSilent +2
    MessageBox MB_OK|MB_ICONSTOP "No Java Virtual Machine found in folder:$\r$\n$5"
    DetailPrint "No Java Virtual Machine found in folder:$\r$\n$5"
    Quit
  ${EndIf}

  StrCpy "$JvmDll" $5

  ; Read PE header of JvmDll to check for architecture
  ; 1. Jump to 0x3c and read offset of PE header
  ; 2. Jump to offset. Read PE header signature. It must be 'PE'\0\0 (50 45 00 00).
  ; 3. The next word gives the machine type.
  ; 0x014c: x86
  ; 0x8664: x64
  ; 0x0200: i64
  ClearErrors
  FileOpen $R1 "$JvmDll" r
  IfErrors WrongPEHeader

  FileSeek $R1 0x3c SET
  FileReadByte $R1 $R2
  FileReadByte $R1 $R3
  IntOp $R3 $R3 << 8
  IntOp $R2 $R2 + $R3

  FileSeek $R1 $R2 SET
  FileReadByte $R1 $R2
  IntCmp $R2 0x50 +1 WrongPEHeader WrongPEHeader
  FileReadByte $R1 $R2
  IntCmp $R2 0x45 +1 WrongPEHeader WrongPEHeader
  FileReadByte $R1 $R2
  IntCmp $R2 0 +1 WrongPEHeader WrongPEHeader
  FileReadByte $R1 $R2
  IntCmp $R2 0 +1 WrongPEHeader WrongPEHeader

  FileReadByte $R1 $R2
  FileReadByte $R1 $R3
  IntOp $R3 $R3 << 8
  IntOp $R2 $R2 + $R3

  IntCmp $R2 0x014c +1 +3 +3
  StrCpy "$Arch" "x86"
  Goto DonePEHeader

  IntCmp $R2 0x8664 +1 +3 +3
  StrCpy "$Arch" "x64"
  Goto DonePEHeader

  IntCmp $R2 0x0200 +1 +3 +3
  StrCpy "$Arch" "i64"
  Goto DonePEHeader

WrongPEHeader:
  IfSilent +2
  MessageBox MB_OK|MB_ICONEXCLAMATION 'Cannot read PE header from "$JvmDll"$\r$\nWill assume that the architecture is x86.'
  DetailPrint 'Cannot read PE header from "$JvmDll". Assuming the architecture is x86.'
  StrCpy "$Arch" "x86"

DonePEHeader:
  FileClose $R1

  DetailPrint 'Architecture: "$Arch"'

  StrCpy $INSTDIR "$ResetInstDir"

  ; The default varies depending on 32-bit or 64-bit
  ${If} "$INSTDIR" == ""
    ${If} $Arch == "x86"
      ${If} $FluteServiceName == $FluteServiceDefaultName
        StrCpy $INSTDIR "$PROGRAMFILES32\KURS\${PRODUCT_NAME}-${PRODUCT_VERSION}"
      ${Else}
        StrCpy $INSTDIR "$PROGRAMFILES32\KURS\${PRODUCT_NAME}_$FluteServiceName"
      ${EndIf}
    ${Else}
      ${If} $FluteServiceName == $FluteServiceDefaultName
        StrCpy $INSTDIR "$PROGRAMFILES64\KURS\${PRODUCT_NAME}-${PRODUCT_VERSION}"
      ${Else}
        StrCpy $INSTDIR "$PROGRAMFILES64\KURS\${PRODUCT_NAME}_$FluteServiceName"
      ${EndIf}
    ${EndIf}
  ${EndIf}
  Push 0
FunctionEnd


; =====================
; findJavaHome Function
; =====================
;
; Find the JAVA_HOME used on the system, and put the result on the top of the
; stack
; Will return an empty string if the path cannot be determined
;
Function findJavaHome

  ClearErrors
  StrCpy $1 ""

  ; Use the 64-bit registry first on 64-bit machines
  ExpandEnvStrings $0 "%PROGRAMW6432%"
  ${If} $0 != "%PROGRAMW6432%"
    SetRegView 64
    ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
    ReadRegStr $1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$2" "JavaHome"
    ReadRegStr $3 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$2" "RuntimeLib"

    IfErrors 0 +2
    StrCpy $1 ""
    ClearErrors
  ${EndIf}

  ; If no 64-bit Java was found, look for 32-bit Java
  ${If} $1 == ""
    SetRegView 32
    ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
    ReadRegStr $1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$2" "JavaHome"
    ReadRegStr $3 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$2" "RuntimeLib"

    IfErrors 0 +2
    StrCpy $1 ""
    ClearErrors
    
    ; If using 64-bit, go back to using 64-bit registry
    ${If} $0 != "%PROGRAMW6432%"
      SetRegView 64
    ${EndIf}
  ${EndIf}

  ; Put the result in the stack
  Push $1

FunctionEnd

; ====================
; FindJVMPath Function
; ====================
;
; Find the full JVM path, and put the result on top of the stack
; Implicit argument: $JavaHome
; Will return an empty string if the path cannot be determined
;
Function findJVMPath

  ClearErrors

  ;Step one: Is this a JRE path (Program Files\Java\XXX)
  StrCpy $1 "$JavaHome"

  StrCpy $2 "$1\bin\hotspot\jvm.dll"
  IfFileExists "$2" FoundJvmDll
  StrCpy $2 "$1\bin\server\jvm.dll"
  IfFileExists "$2" FoundJvmDll
  StrCpy $2 "$1\bin\client\jvm.dll"
  IfFileExists "$2" FoundJvmDll
  StrCpy $2 "$1\bin\classic\jvm.dll"
  IfFileExists "$2" FoundJvmDll

  ;Step two: Is this a JDK path (Program Files\XXX\jre)
  StrCpy $1 "$JavaHome\jre"

  StrCpy $2 "$1\bin\hotspot\jvm.dll"
  IfFileExists "$2" FoundJvmDll
  StrCpy $2 "$1\bin\server\jvm.dll"
  IfFileExists "$2" FoundJvmDll
  StrCpy $2 "$1\bin\client\jvm.dll"
  IfFileExists "$2" FoundJvmDll
  StrCpy $2 "$1\bin\classic\jvm.dll"
  IfFileExists "$2" FoundJvmDll

  ClearErrors
  ;Step tree: Read defaults from registry

  ReadRegStr $1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$1" "RuntimeLib"

  IfErrors 0 FoundJvmDll
  StrCpy $2 ""

  FoundJvmDll:
  ClearErrors

  ; Put the result in the stack
  Push $2

FunctionEnd

; =====================
; CheckUserType Function
; =====================
;
; Check the user type, and warn if it's not an administrator.
; Taken from Examples/UserInfo that ships with NSIS.
Function CheckUserType
  ClearErrors
  UserInfo::GetName
  IfErrors Win9x
  Pop $0
  UserInfo::GetAccountType
  Pop $1
  StrCmp $1 "Admin" 0 +3
    ; This is OK, do nothing
    Goto done

    MessageBox MB_OK|MB_ICONEXCLAMATION 'Note: the current user is not an administrator. \
               To run Flute as a Windows service, you must be an administrator. \
               You can still run Flute from the command-line as this type of user.'
    Goto done

  Win9x:
    # This one means you don't need to care about admin or
    # not admin because Windows 9x doesn't either
    MessageBox MB_OK "Error! This DLL can't run under Windows 9x!"

  done:
FunctionEnd


Function InstallDesktopShortcut
  CreateShortCut "$DESKTOP\Configure Flute.lnk" "$INSTDIR\bin\$FluteServiceManagerFileName" '//ES//$FluteServiceName'
FunctionEnd

; Push $filenamestring (e.g. 'c:\this\and\that\filename.htm')
; Push "\"
; Call StrSlash
; Pop $R0
; ;Now $R0 contains 'c:/this/and/that/filename.htm'
Function StrSlash
  Exch $R3 ; $R3 = needle ("\" or "/")
  Exch
  Exch $R1 ; $R1 = String to replacement in (haystack)
  Push $R2 ; Replaced haystack
  Push $R4 ; $R4 = not $R3 ("/" or "\")
  Push $R6
  Push $R7 ; Scratch reg
  StrCpy $R2 ""
  StrLen $R6 $R1
  StrCpy $R4 "\"
  StrCmp $R3 "/" loop
  StrCpy $R4 "/"  
loop:
  StrCpy $R7 $R1 1
  StrCpy $R1 $R1 $R6 1
  StrCmp $R7 $R3 found
  StrCpy $R2 "$R2$R7"
  StrCmp $R1 "" done loop
found:
  StrCpy $R2 "$R2$R4"
  StrCmp $R1 "" done loop
done:
  StrCpy $R3 $R2
  Pop $R7
  Pop $R6
  Pop $R4
  Pop $R2
  Pop $R1
  Exch $R3
FunctionEnd

;>>>>>> Function Junction BEGIN
;Original Written by Afrow UK
; Rewrite to Replace on line within text by rainmanx
; Creating the temp file in the same directory by lars
; This version works on R4 and R3 of Nullsoft Installer
; It replaces whatever is in the line throughout the entire text matching it.
Function AdvReplaceInFile
Exch $0 ;file to replace in
Exch
Exch $1 ;number to replace after
Exch
Exch 2
Exch $2 ;replace and onwards
Exch 2
Exch 3
Exch $3 ;replace with
Exch 3
Exch 4
Exch $4 ;to replace
Exch 4
Push $5 ;minus count
Push $6 ;universal
Push $7 ;end string
Push $8 ;left string
Push $9 ;right string
Push $R0 ;file1
Push $R1 ;file2
Push $R2 ;read
Push $R3 ;universal
Push $R4 ;count (onwards)
Push $R5 ;count (after)
Push $R6 ;temp file name
;-------------------------------
; Find folder with file to edit:
GetFullPathName $R1 $0\..
; Put temporary file in same folder to preserve access rights:
GetTempFileName $R6 $R1
FileOpen $R1 $0 r ;file to search in
FileOpen $R0 $R6 w ;temp file
StrLen $R3 $4
StrCpy $R4 -1
StrCpy $R5 -1
loop_read:
ClearErrors
FileRead $R1 $R2 ;read line
IfErrors exit
StrCpy $5 0
StrCpy $7 $R2
loop_filter:
IntOp $5 $5 - 1
StrCpy $6 $7 $R3 $5 ;search
StrCmp $6 "" file_write2
StrCmp $6 $4 0 loop_filter
StrCpy $8 $7 $5 ;left part
IntOp $6 $5 + $R3
StrCpy $9 $7 "" $6 ;right part
StrLen $6 $7
StrCpy $7 $8$3$9 ;re-join
StrCmp -$6 $5 0 loop_filter
IntOp $R4 $R4 + 1
StrCmp $2 all file_write1
StrCmp $R4 $2 0 file_write2
IntOp $R4 $R4 - 1
IntOp $R5 $R5 + 1
StrCmp $1 all file_write1
StrCmp $R5 $1 0 file_write1
IntOp $R5 $R5 - 1
Goto file_write2
file_write1:
FileWrite $R0 $7 ;write modified line
Goto loop_read
file_write2:
FileWrite $R0 $7 ;write modified line
Goto loop_read
exit:
FileClose $R0
FileClose $R1
SetDetailsPrint none
Delete $0
Rename $R6 $0
Delete $R6
SetDetailsPrint both
;-------------------------------
Pop $R6
Pop $R5
Pop $R4
Pop $R3
Pop $R2
Pop $R1
Pop $R0
Pop $9
Pop $8
Pop $7
Pop $6
Pop $5
Pop $4
Pop $3
Pop $2
Pop $1
Pop $0
FunctionEnd
;>>>>>>>>>>>>> Function END