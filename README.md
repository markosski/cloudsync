# CloudSync

```
              ,,                           ,,
  .g8"""bgd `7MM                         `7MM   .M"""bgd
.dP'     `M   MM                           MM  ,MI    "Y
dM'       `   MM  ,pW"Wq.`7MM  `7MM   ,M""bMM  `MMb.  `7M'   `MF'`7MMpMMMb.  ,p6"bo
MM            MM 6W'   `Wb MM    MM ,AP    MM    `YMMNq.VA   ,V    MM    MM 6M'  OO
MM.           MM 8M     M8 MM    MM 8MI    MM  .     `MM VA ,V     MM    MM 8M
`Mb.     ,'   MM YA.   ,A9 MM    MM `Mb    MM  Mb     dM  VVV      MM    MM YM.    ,
  `"bmmmd'  .JMML.`Ybmd9'  `Mbod"YML.`Wbmd"MML.P"Ybmmd"   ,V     .JMML  JMML.YMbmd'
                                                         ,V
                                                      OOb"
```
Utility to sync files between local filesystem and AWS S3 and other cloud providers.

CloudSync can be configured to monitor multiple local directories listening to file
changes. Change to a file will sync the file with its counterpart in the cloud storage.
At the moment only AWS S3 storage is implemented. CloudSync can be easily extended to other
cloud storage by implementing `CloudClient` interface.

## How to use.

1. Build the project with `sbt assembly`
2. Provide configuration file to configure cloud client, see application.conf
3. Create a file with path maps similar to example below

```
/Users/marcin/Documents:/my-cloud-files/documents
/Users/marcin/Pictures:/my-cloud-files/pictures
```

4. Run CloudSync with `java -Dconfig.file=path/to/config` -jar <path_to_jar> file:///path/to/clousSyncPathMappings.txt
