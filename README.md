# jira-zephyr-executor.java


### SETUP
1. Download jar file to location [PROJECT-ROOT]/vendor/  
[zfj-cloud-rest-client-1.3-jar-with-dependencies.jar](https://github.com/zephyrdeveloper/zapi-cloud/blob/master/Samples/production/zapi-cloud/generator/java/target/zfj-cloud-rest-client-1.3-jar-with-dependencies.jar?raw=true)

2. Add following dependency in your test maven project:
    ```XML
    <dependency>
        <groupId>com.qainfotech.tap</groupId>
        <artifactId>jira-zephyr-executor</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>com.thed.zhephy.cloud.rest</groupId>
        <artifactId>zfj-cloud-rest-client</artifactId>
        <version>1.0</version>
        <scope>system</scope>
        <systemPath>${project.basedir}/vendor/zfj-cloud-rest-client-1.3-jar-with-dependencies.jar</systemPath>
    </dependency>
    ```

3. Add following plugin to your test maven project:
    ```XML
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>1.6.0</version>
        <executions>
          <execution>
            <goals>
              <goal>java</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <mainClass>com.qainfotech.tap.JiraZephyrExecutor</mainClass>
          <classpathScope>test</classpathScope>
        </configuration>
      </plugin>
    ``` 

4. create jiraConfig.properties in project root with following parameters:
    ```JAVA
    zephyr.accessKey=[Your Account's Zephyr Access Key]
    zephyr.secretKey=[Your Account's Zephyr Secret Key]
    jira.accountId=[Your Account ID]
    jira.userId=[Your Account's Jira username]
    jira.apiKey=[Your Account's Jira api key]

    test.projectId=[JIRA Project ID]
    test.versionId=[JIRA version id, usualy '-1']
    test.label=[Label name to create test cycle from]
    test.testCycleName=[Name of test cycle to be created/executed]
    ```

### EXECUTE TESTS
#### Running tests - pick configs from yaml file
    ```
    $> mvn clean test
    ```



----
