package plus.gaga.middleware.sdk;

import com.alibaba.fastjson2.JSON;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import plus.gaga.middleware.sdk.domain.model.Model;
import plus.gaga.middleware.sdk.infrastructure.openai.dto.ChatCompletionRequestDTO;
import plus.gaga.middleware.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;
import plus.gaga.middleware.sdk.types.utils.BearerTokenUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class OpenAiCodeReview {

    public static void main(String[] args) throws Exception {
        System.out.println("代码校验，测试执行");

        String token = System.getenv("GITHUB_TOKEN");
        if(token == null || token.isEmpty()){
            throw new RuntimeException("token is null");
        }

        // 1. 代码检出
        ProcessBuilder processBuilder = new ProcessBuilder("git", "diff", "HEAD~1", "HEAD");
        processBuilder.directory(new File("."));

        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        StringBuilder diffCode = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            diffCode.append(line);
        }

        int exitCode = process.waitFor();
        System.out.println("Exited with code:" + exitCode);

        System.out.println("diffCode：" + diffCode.toString());
        //    2、glm代码评审
        String log = codeReview(diffCode.toString());
        System.out.println("code Review" + log);
//3、写日志
        String logs = writeLog(token , log);
        System.out.println("日志" + logs);
    }


    private static String codeReview(String diffCode) throws Exception {
        String apiKeySecret = "50838fdd6ee3414594e47ece0cd5be30.fdn4C393LncqLKa6";
        String token = BearerTokenUtils.getToken(apiKeySecret);

        URL url = new URL("https://open.bigmodel.cn/api/paas/v4/chat/completions");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
        connection.setDoOutput(true);

        ChatCompletionRequestDTO chatCompletionRequest = new ChatCompletionRequestDTO();
        chatCompletionRequest.setModel(Model.GLM_4_FLASH.getCode());
        chatCompletionRequest.setMessages(new ArrayList<ChatCompletionRequestDTO.Prompt>() {
            private static final long serialVersionUID = -7988151926241837899L;

            {
                add(new ChatCompletionRequestDTO.Prompt("user", "你是一个高级编程架构师，精通各类场景方案、架构设计和编程语言请，请您根据git diff记录，对代码做出评审。代码如下:"));
                add(new ChatCompletionRequestDTO.Prompt("user", diffCode));
            }
        });


//        String jsonInpuString = "{"
//                + "\"model\":\"glm-4-flash\","
//                + "\"messages\": ["
//                + "    {"
//                + "        \"role\": \"user\","
//                + "        \"content\": \"你是一个高级编程架构师，精通各类场景方案、架构设计和编程语言请，请您根据git diff记录，对代码做出评审。代码为: " + diffCode + "\""
//                + "    }"
//                + "]"
//                + "}";


        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = JSON.toJSONString(chatCompletionRequest).getBytes(StandardCharsets.UTF_8);
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
        return response.getChoices().get(0).getMessage().getContent();
    }

    private static String writeLog(String token, String log) throws Exception {

        Git git = Git.cloneRepository()             //1.克隆一个Git仓库
                .setURI("https://github.com/YNAlone/openai-code-review-log.git")                         //2.设置远程仓库
                .setDirectory(new File("repo"))             //3.克隆到本地指定文件夹
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))         //4.设置身份凭证
                .call();

        String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        File dateFolder = new File("repo/" + dateFolderName);
        if (!dateFolder.exists()) {
            dateFolder.mkdirs();
        }
        String filename = generateRandomString(12) + ".md";
        File file = new File(dateFolderName , filename);
        try(FileWriter writer =  new FileWriter(file)) {
            writer.write(log);
        }

        git.add().addFilepattern(dateFolderName + "/" + filename).call();
        git.commit().setMessage("Add new File").call();
        git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""));


        return "https://github.com/YNAlone/openai-code-review-log/blob/master" + dateFolderName + "/" + filename;
    }

    private static String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopgrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return sb.toString();
    }
}