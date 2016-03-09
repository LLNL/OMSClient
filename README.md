# OMS History Parser #

This moves information from OMS history files (.his) and stores it in HBase. The process in done in two steps:

* reads OSM data from OMS files and stores them in JSON or comma delimited files.
* reads data from JSON/comma delimited files and stores it in HBase (WIP)

## Usage ##

### 1. Reading '.his' ###

OSMHBase : IB Performance Processor
```
#!shell
 OSMHBase <operation> [<args>...] [json/del]
```

Usage:
```
#!shell

$ OMSHbase help                            - Shows this help/usage message.
$ OMSHbase parseHis /path/to/his/dir       - Extract data from OMS '.his' files located in a given path.
$ OMSHbase parseHis /path/to/hisFile       - Extract data from a single '.his'
$ OMSHbase parseHis <path> json            - Writes data in JSON format.
$ OMSHbase parseHis <path> del             - Writes data in delimited format.
```


*OMSHbase should be renamed to ibperfp in a future commit.*

### 2. Writing to HBase ###

The hbaseLoader will read JSON text and write to HBase - (WIP)