package plus.gaga.middleware.sdk.test;

import com.alibaba.fastjson2.JSON;
import org.junit.Test;
import plus.gaga.middleware.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;
import plus.gaga.middleware.sdk.types.utils.BearerTokenUtils;
import plus.gaga.middleware.sdk.types.utils.WXAccessTokenUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ApiTest {

    public static void main(String[] args) {
        String apiKeySecret = "50838fdd6ee3414594e47ece0cd5be30.fdn4C393LncqLKa6";
        String token = BearerTokenUtils.getToken(apiKeySecret);
        System.out.println(token);
    }

    @Test
    public void test_http() throws IOException {
        String apiKeySecret = "50838fdd6ee3414594e47ece0cd5be30.fdn4C393LncqLKa6";
        String token = BearerTokenUtils.getToken(apiKeySecret);

        URL url = new URL("https://open.bigmodel.cn/api/paas/v4/chat/completions");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
        connection.setDoOutput(true);

        String code = "1+1";

        String jsonInpuString = "{"
                + "\"model\":\"glm-4-flash\","
                + "\"messages\": ["
                + "    {"
                + "        \"role\": \"user\","
                + "        \"content\": \"你是一个高级编程架构师，精通各类场景方案、架构设计和编程语言请，请您根据git diff记录，对代码做出评审。代码为: " + code + "\""
                + "    }"
                + "]"
                + "}";


        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInpuString.getBytes(StandardCharsets.UTF_8);
            os.write(input);
        }

        int responseCode = connection.getResponseCode();
        System.out.println(responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;

        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }

        in.close();
        connection.disconnect();

        ChatCompletionSyncResponseDTO response = JSON.parseObject(content.toString(), ChatCompletionSyncResponseDTO.class);
        System.out.println(response.getChoices().get(0).getMessage().getContent());

    }

    /**
     * 微信模板消息推送测试
     * <p>
     * 流程：获取微信 access_token → 构造模板消息 JSON → 调用微信模板消息接口发送通知
     */
    @Test
    public void test_wx() {
        // 1. 获取微信 access_token
        String accessToken = WXAccessTokenUtils.getAccessToken();
        System.out.println(accessToken);

        // 2. 构造模板消息内容
        Message message = new Message();
        message.put("project", "big-market");
        message.put("branch", "weixin-push");
        message.put("author", "niecccccc");
        message.put("commit", "测试微信推送");

        // 3. 调用微信模板消息发送接口
        String url = String.format("https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=%s", accessToken);
        sendPostRequest(url, JSON.toJSONString(message));
    }

    /**
     * 发送 HTTP POST 请求（JSON 格式）
     *
     * @param urlString 请求地址
     * @param jsonBody  JSON 请求体
     */
    private static void sendPostRequest(String urlString, String jsonBody) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
                String response = scanner.useDelimiter("\\A").next();
                System.out.println(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 微信模板消息请求体
     * <p>
     * 对应微信模板消息 API 的 JSON 结构：
     * <pre>
     * {
     *   "touser": "接收者 openid",
     *   "template_id": "模板 ID",
     *   "url": "点击跳转链接",
     *   "data": {
     *     "key": { "value": "内容" }
     *   }
     * }
     * </pre>
     */
    public static class Message {
        private String touser = "oeDKo2C9UYXfb9h2csoMTg84K10U";
        private String template_id = "6phduhnE-FPrSbx4xqnd_9PekPXczY8LiQO_A0rB3R8";
        private String url = "https://github.com/YNAlone/openai-code-review-log/blob/main/2026-05-03/1IdpYf3CuAjh.md";
        private Map<String, Map<String, String>> data = new HashMap<>();

        public void put(String key, String value) {
            data.put(key, new HashMap<String, String>() {
                {
                    put("value", value);
                }
            });
        }

        public String getTouser() {
            return touser;
        }

        public void setTouser(String touser) {
            this.touser = touser;
        }

        public String getTemplate_id() {
            return template_id;
        }

        public void setTemplate_id(String template_id) {
            this.template_id = template_id;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Map<String, Map<String, String>> getData() {
            return data;
        }

        public void setData(Map<String, Map<String, String>> data) {
            this.data = data;
        }
    }

}
