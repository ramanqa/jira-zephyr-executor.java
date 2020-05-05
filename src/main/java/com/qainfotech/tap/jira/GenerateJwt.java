package com.qainfotech.tap.jira;

import com.qainfotech.tap.ConfigReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.Map;
import java.util.HashMap;
import com.thed.zephyr.cloud.rest.ZFJCloudRestClient;
import com.thed.zephyr.cloud.rest.client.JwtGenerator;

public class GenerateJwt {

    public static Map<String, String> jwt(String method, String path) throws URISyntaxException, IllegalStateException, IOException {
        String zephyrBaseUrl = ConfigReader.get("zephyr.baseUrl");
        String accessKey = ConfigReader.get("zephyr.accessKey");
        String secretKey = ConfigReader.get("zephyr.secretKey");;
        String accountId = ConfigReader.get("jira.accountId");
        ZFJCloudRestClient client = ZFJCloudRestClient.restBuilder(zephyrBaseUrl, accessKey, secretKey, accountId).build();
        JwtGenerator jwtGenerator = client.getJwtGenerator();
        
        String actionUri = zephyrBaseUrl + path;
        
        URI uri = new URI(actionUri);
        int expirationInSec = 360;
        String jwt = jwtGenerator.generateJWT(method.toUpperCase(), uri, expirationInSec);
        
        Map<String, String> response = new HashMap<>();
        response.put("url", uri.toString());
        response.put("jwt", jwt);
        return response;
    }

}
