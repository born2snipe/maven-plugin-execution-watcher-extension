# Maven Plugin Execution Time Monitor Extension

This maven extension logs all plugin executions to a H2 database. This allows for monitoring plugins over time to help determine if there is any performance decline.

## Setup
- Download the latest jar file
- Put the jar file in your maven's `lib/ext` directory. Maven auto-magically puts these jars in the classpath.
- The data is stored in a local [H2](http://www.h2database.com/html/main.html) database in `${user.home}/.m2-plugin-execution-watcher`. To view the data you just need a JDBC friendly database viewer.

## FAQ
- How to change the directory location of the database?
    - Provide the following system property `plugin.execution.watcher.directory=${directory-you-want-the-database}`

- I keep seeing the following error: `[WARNING] Failed to notify spy org.apache.maven.eventspy.PluginWatcherEventSpy: Could not get JDBC Connection; nested exception is org.h2.jdbc.JdbcSQLException: Database may be already in use: "Locked by another process". Possible solutions: close all other connection(s); use the server mode [90020-168]`
    - This is a limitation of the embedded [H2](http://www.h2database.com/html/main.html) database. Only one process can access the database. (ex. mvn, db viewer, etc)

- Can I change out the storage mechanism?
    - You just need to implement the PluginStatsRepository
    - Follow the steps for using Java's [ServiceLoader](http://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html) or use Netbeans [@ServiceProvider](http://bits.netbeans.org/dev/javadoc/org-openide-util-lookup/org/openide/util/lookup/ServiceProvider.html) annotation
    - Put your newly created class in a jar
    - Place your new jar with it's required dependencies in the `lib/ext`
    - You should be good to go

- Can I track some data that is specific to the build?
    - You can provide the following system property `plugin.execution.watcher.build.data=${data-to-store}` and the value provided will be stored with that run of the build
    - the data can be up to 1024 characters