@echo off
set LOG4J=c:\java\lib\log4j-1.2.7.jar
set OJB=c:\java\lib\db-ojb-1.0.rc5.jar
set CONFDIR=c:\java\photovault\conf

set OJBDEPS=lib\antlr.jar;lib\btree.jar;lib\commons-beanutils.jar;lib\commons-collections.jar;lib\commons-dbcp-1.1.jar;lib\commons-lang-2.0.jar;lib\commons-logging.jar;lib\commons-pool-1.1.jar;lib\db-ojb-1.0.rc5.jar;lib\hsqldb.jar;lib\j2ee.jar;lib\jakarta-regexp-1.3.jar;lib\jcs.jar;lib\jdo.jar;lib\jdori-enhancer.jar;lib\jdori.jar;lib\log4j-1.2.8.jar;lib\p6spy.jar;lib\prevayler.jar;lib\xdoclet-1.2b3-dev.jar;lib\xdoclet-ojb-module-1.2b3-dev.jar;lib\xercesImpl.jar;lib\xjavadoc-1.0.jar;lib\xml-apis.jar

set CLASSPATH=%CONFDIR%;c:\java\junit3.8.1\junit.jar;c:\java\abbot-0.8.2\lib\abbot.jar;c:\java\lib\mysql-connector-java-3.0.2-beta-bin.jar;c:\java\lib\metadataExtractor.jar;c:\java\photovault\build\;%LOG4J%;%OJB%;%OJBDEPS%
