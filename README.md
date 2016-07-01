# OMS History Parser #

This moves information from OMS history files (.his) and stores it in HBase.
The process in done in two steps:

* reads OSM data from OMS files and stores them in JSON or comma delimited files.
* reads data from JSON/comma delimited files and stores it in HBase (WIP)

## Usage ##

### 1. Compiling OSMHBase

```
javac -extdirs ../../lib -encoding ISO-8859-1 OSMHBase.java
```

On LC machines, you can select Java 1.7.0 like this:
```
export JAVA_HOME=/usr/lib/jvm/java-1.7.0-oracle-1.7.0.101.x86_64
export PATH=$JAVA_HOME/bin:$PATH
```

### 2. Reading '.his' ###

OSMHBase : IB Performance Processor
```
OSMHBase <operation> [<args>...] [json/del]
```

Usage:
```
OMSHbase help                            - Shows this help/usage message.
OMSHbase parseHis /path/to/his/dir       - Extract data from OMS '.his' files located in a given path.
OMSHbase parseHis /path/to/hisFile       - Extract data from a single '.his'
OMSHbase parseHis <path> json            - Writes data in JSON format.
OMSHbase parseHis <path> del             - Writes data in delimited format.
```

On LC machines, you might have to add some arguments:
```
java -classpath .:../../lib/* OSMHBase parseHis <path> <json/del>
```

*OMSHbase should be renamed to ibperfp in a future commit.*

### 3. Writing to HBase ###

The hbaseLoader will read JSON text and write to HBase - (WIP)
