rem Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
rem DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
rem
rem This code is free software; you can redistribute it and/or modify it
rem under the terms of the GNU General Public License version 2 only, as
rem published by the Free Software Foundation.  Oracle designates this
rem particular file as subject to the "Classpath" exception as provided
rem by Oracle in the LICENSE file that accompanied this code.
rem
rem This code is distributed in the hope that it will be useful, but WITHOUT
rem ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
rem FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
rem version 2 for more details (a copy is included in the LICENSE file that
rem accompanied this code).
rem
rem You should have received a copy of the GNU General Public License version
rem 2 along with this work; if not, write to the Free Software Foundation,
rem Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
rem
rem Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
rem or visit www.oracle.com if you need additional information or have any
rem questions.

SET BUILD_SRC_15=..\src-jdk15
SET BUILD_OUTPUT=dist
SET BUILD_DEPLOY=..\..\release\lib

set VS2005_COMMON_PATH=C:\Program Files\Microsoft Visual Studio 8\vc\bin

call "%VS2005_COMMON_PATH%\vcvars32"

mkdir %BUILD_OUTPUT%\deployed\cvm\windows

cl /I%CVM_HOME%/src/share/javavm/export /I%CVM_HOME%/src/share /I%CVM_HOME%/src ^
/I%CVM_HOME%/src/win32-x86 /I%CVM_HOME%/src/win32 ^
%BUILD_SRC_15%\class_file_cache.c ^
%BUILD_SRC_15%\attach.c ^
%BUILD_SRC_15%\Classes.c ^
%BUILD_SRC_15%\Timers.c ^
%BUILD_SRC_15%\GC.c ^
%BUILD_SRC_15%\Threads.c ^
%BUILD_SRC_15%\Stacks.c ^
%BUILD_SRC_15%\common_functions.c ^
/D WIN32 /D CVM /MD /Ox /c

link /DLL /MAP:%BUILD_OUTPUT%\deployed\cvm\windows\profilerinterface.map /OUT:%BUILD_OUTPUT%\deployed\cvm\windows\profilerinterface.dll ^
Classes.obj Timers.obj GC.obj Threads.obj Stacks.obj common_functions.obj class_file_cache.obj attach.obj
mt.exe -nologo -manifest %BUILD_OUTPUT%\deployed\cvm\windows\profilerinterface.dll.manifest -outputresource:%BUILD_OUTPUT%\deployed\cvm\windows\profilerinterface.dll;#2

cl /I%CVM_HOME%/src/share/javavm/export /I%CVM_HOME%/src/share /I%CVM_HOME%/src ^
/I%CVM_HOME%/src/win32-x86 /I%CVM_HOME%/src/win32 ^
%BUILD_SRC_15%\class_file_cache.c ^
%BUILD_SRC_15%\attach.c ^
%BUILD_SRC_15%\Classes.c ^
%BUILD_SRC_15%\Timers.c ^
%BUILD_SRC_15%\GC.c ^
%BUILD_SRC_15%\Threads.c ^
%BUILD_SRC_15%\Stacks.c ^
%BUILD_SRC_15%\common_functions.c ^
/D WIN32 /D CVM /MDd /Zi /c

link /DLL /DEBUG /MAP:%BUILD_OUTPUT%\deployed\cvm\windows\profilerinterface_g.map /OUT:%BUILD_OUTPUT%\deployed\cvm\windows\profilerinterface_g.dll ^
Classes.obj Timers.obj GC.obj Threads.obj Stacks.obj common_functions.obj class_file_cache.obj attach.obj
mt.exe -nologo -manifest %BUILD_OUTPUT%\deployed\cvm\windows\profilerinterface_g.dll.manifest -outputresource:%BUILD_OUTPUT%\deployed\cvm\windows\profilerinterface_g.dll;#2

del vc60.pdb
del *.obj
del %BUILD_OUTPUT%\deployed\cvm\windows\*.lib %BUILD_OUTPUT%\deployed\cvm\windows\*.exp %BUILD_OUTPUT%\deployed\cvm\windows\*.ilk %BUILD_OUTPUT%\deployed\cvm\windows\*.pdb

copy %BUILD_OUTPUT%\deployed\cvm\windows\*.dll %BUILD_DEPLOY%\deployed\cvm\windows
copy %BUILD_OUTPUT%\deployed\cvm\windows\*.map %BUILD_DEPLOY%\deployed\cvm\windows

