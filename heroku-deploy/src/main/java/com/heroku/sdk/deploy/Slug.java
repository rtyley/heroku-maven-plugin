package com.heroku.sdk.deploy;

import java.io.*;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class Slug {

  private String blobUrl;

  private String slugId;

  private String stackName;

  private String createJson;

  private String appName;

  private Map<String,String> headers;

  public static final String BASE_URL = "https://api.heroku.com";

  public Slug(String buildPackDesc, String appName, String stack, String encodedApiKey, Map<String,String> processTypes) throws UnsupportedEncodingException {
    this.appName = appName;

    headers = new HashMap<String,String>();
    headers.put("Authorization", encodedApiKey);
    headers.put("Content-Type", "application/json");
    headers.put("Accept", "application/vnd.heroku+json; version=3");

    createJson = "{" +
        "\"buildpack_provided_description\":\"" + buildPackDesc +"\"," +
        "\"stack\":\"" + (stack == null ? "cedar-14" : stack) +"\"," +
        "\"process_types\":{";

    boolean first = true;
    for (String key : processTypes.keySet()) {
      String value = processTypes.get(key);
      if (!first) createJson += ", ";
      first = false;
      createJson += "\"" + key + "\"" + ":" + "\"" + sanitizeJson(value) + "\"";
    }
    createJson +=  "}}";
  }

  public String getBlobUrl() { return blobUrl; }
  public String getSlugId() { return slugId; }
  public String getStackName() { return stackName; }
  public String getSlugRequest() { return createJson; }

  public Map create() throws IOException, Curl.CurlException {
    String urlStr = BASE_URL + "/apps/" + URLEncoder.encode(appName, "UTF-8") + "/slugs";
    Map slugResponse = Curl.post(urlStr, createJson, headers);

    Map blobJson = (Map)slugResponse.get("blob");
    blobUrl = (String)blobJson.get("url");

    slugId = (String)slugResponse.get("id");

    Map stackJson = (Map)slugResponse.get("stack");
    stackName = (String)stackJson.get("name");

    return slugResponse;
  }

  public void upload(File slugFile) throws IOException, Curl.CurlException {
    if (blobUrl == null) {
      throw new IllegalStateException("Slug must be created before uploading!");
    }

    Curl.put(blobUrl, slugFile);
  }

  public Map release() throws IOException, Curl.CurlException {
    if (slugId == null) {
      throw new IllegalStateException("Slug must be created before releasing!");
    }

    String urlStr = BASE_URL + "/apps/" + appName + "/releases";

    String data = "{\"slug\":\"" + slugId + "\"}";

    return Curl.post(urlStr, data, headers);
  }

  private String sanitizeJson(String json) {
    return json.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
