@echo off
set LOG4J=c:\java\lib\log4j-1.2.7.jar
set OJB=c:\java\lib\jakarta-ojb-0.9.9.jar
set CONFDIR=c:\java\photovault\conf
set OJBDEPS=c:\java\jakarta-ojb\lib\commons-beanutils.jar;c:\java\jakarta-ojb\lib\commons-collections-2.0.jar;c:\java\jakarta-ojb\lib\commons-dbcp.jar;c:\java\jakarta-ojb\lib\commons-lang-1.0-mod.jar;c:\java\jakarta-ojb\lib\commons-logging.jar;c:\java\jakarta-ojb\lib\commons-pool.jar;c:\java\jakarta-ojb\lib\hsqldb.jar;c:\java\jakarta-ojb\lib\jboss-common.jar;c:\java\jakarta-ojb\lib\jboss-system.jar;c:\java\jakarta-ojb\lib\jca1.0.jar;c:\java\jakarta-ojb\lib\jcs.jar;c:\java\jakarta-ojb\lib\jdbc2_0-stdext.jar;c:\java\jakarta-ojb\lib\jmxri.jar;c:\java\jakarta-ojb\lib\jndi.jar;c:\java\jakarta-ojb\lib\jta-spec1_0_1.jar;c:\java\jakarta-ojb\lib\p6spy.jar;c:\java\jakarta-ojb\lib\proxy.jar;c:\java\jakarta-ojb\lib\xdoclet.jar;c:\java\jakarta-ojb\lib\xercesImpl.jar;c:\java\jakarta-ojb\lib\xml-apis.jar;c:\java\jakarta-ojb\lib\antlr.jar

set CLASSPATH=%CONFDIR%;c:\java\junit3.8.1\junit.jar;c:\java\abbot-0.8.2\lib\abbot.jar;c:\java\lib\mysql-connector-java-3.0.2-beta-bin.jar;c:\java\lib\metadataExtractor.jar;c:\java\photovault\build\;%LOG4J%;%OJB%;%OJBDEPS%
