package com.qainfotech.tap;

import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;

public class ConfigReader{

    public static String get(String configKey){
        String value = null;
        Properties properties = new Properties();
        
        // get from resources config (Baked config)
        try{
            properties.load(ConfigReader.class.getClassLoader().getResourceAsStream("jiraConfig.properties"));
            value = properties.getOrDefault(configKey, null).toString();
        }catch(Exception e){
        }

        // overwrite with root config
        try{
            File configFile = new File("./jiraConfig.properties");
            properties.load(new FileInputStream(configFile));
            value = properties.getOrDefault(configKey, value).toString();
        }catch(Exception e){}

        // overwrite with command line
        if(System.getProperties().containsKey(configKey)){
            value = System.getProperty(configKey);
        }
        return value;
    }

    public static Boolean isFlagSet(String flagName){
        return Boolean.valueOf(get(flagName));
    }
}
