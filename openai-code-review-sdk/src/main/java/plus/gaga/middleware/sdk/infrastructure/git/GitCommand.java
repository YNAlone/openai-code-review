package plus.gaga.middleware.sdk.infrastructure.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plus.gaga.middleware.sdk.types.utils.RandomStringUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Git 操作封装类
 * <p>
 * 提供两个核心能力：
 * <ul>
 *   <li>获取最新 commit 的代码差异（git diff）</li>
 *   <li>将评审结果写入文件并推送到指定 Git 仓库</li>
 * </ul>
 */
public class GitCommand {

    private final Logger logger = LoggerFactory.getLogger(GitCommand.class);

    /** 评审日志存储仓库地址（不含 .git 后缀） */
    private final String githubReviewLogUri;

    /** GitHub Personal Access Token，用于认证 */
    private final String githubToken;

    /** 项目名称，用于生成报告文件名 */
    private final String project;

    /** 分支名称，用于生成报告文件名 */
    private final String branch;

    /** 提交作者，用于生成报告文件名 */
    private final String author;

    /** 提交信息，用于记录 */
    private final String message;

    /**
     * @param githubReviewLogUri 评审日志仓库地址
     * @param githubToken        GitHub 访问令牌
     * @param project            项目名
     * @param branch             分支名
     * @param author             提交者
     * @param message            提交信息
     */
    public GitCommand(String githubReviewLogUri, String githubToken, String project, String branch, String author, String message) {
        this.githubReviewLogUri = githubReviewLogUri;
        this.githubToken = githubToken;
        this.project = project;
        this.branch = branch;
        this.author = author;
        this.message = message;
    }

    /**
     * 获取最新一次提交的代码差异
     * <p>
     * 分两步：
     * <ol>
     *   <li>通过 git log 获取最新 commit 的 hash</li>
     *   <li>使用 commitHash^..commitHash 执行 git diff 获取该 commit 的变更</li>
     * </ol>
     *
     * @return 最新 commit 的 diff 文本
     * @throws IOException          执行 git 命令出错
     * @throws InterruptedException 等待进程被中断
     */
    public String diff() throws IOException, InterruptedException {
        // 第一步：获取最新 commit hash
//        创建第一个进程，取出最新提交的哈希
        ProcessBuilder logProcessBuilder = new ProcessBuilder("git", "log", "-1", "--pretty=format:%H");
        logProcessBuilder.directory(new File("."));
        Process logProcess = logProcessBuilder.start();

        BufferedReader logReader = new BufferedReader(new InputStreamReader(logProcess.getInputStream()));
        String latestCommitHash = logReader.readLine();
        logReader.close();
        logProcess.waitFor();

        // 第二步：获取该 commit 的 diff（与父 commit 对比）
        ProcessBuilder diffProcessBuilder = new ProcessBuilder("git", "diff", latestCommitHash + "^", latestCommitHash);
        diffProcessBuilder.directory(new File("."));
        Process diffProcess = diffProcessBuilder.start();

        StringBuilder diffCode = new StringBuilder();
        BufferedReader diffReader = new BufferedReader(new InputStreamReader(diffProcess.getInputStream()));
        String line;
        while ((line = diffReader.readLine()) != null) {
            diffCode.append(line).append("\n");
        }
        diffReader.close();

        int exitCode = diffProcess.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Failed to get diff, exit code:" + exitCode);
        }

        return diffCode.toString();
    }

    /**
     * 将评审结果写入文件，提交并推送到评审日志仓库
     * <p>
     * 流程：clone 仓库 → 按日期创建目录 → 写入 .md 文件 → git add/commit/push
     *
     * @param recommend 评审结果内容（Markdown 格式）
     * @return 评审报告的可访问 URL
     * @throws Exception Git 操作或文件写入异常
     */
    public String commitAndPush(String recommend) throws Exception {
        // clone 评审日志仓库到本地 repo 目录
        Git git = Git.cloneRepository()
                .setURI(githubReviewLogUri + ".git")
                .setDirectory(new File("repo"))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubToken, ""))
                .call();

        // 按日期创建目录（如 repo/2026-05-03）
        String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        File dateFolder = new File("repo/" + dateFolderName);
        if (!dateFolder.exists()) {
            dateFolder.mkdirs();
        }

        // 文件名：项目-分支-作者-时间戳-4位随机数.md，保证唯一性
        String fileName = project + "-" + branch + "-" + author + System.currentTimeMillis() + "-" + RandomStringUtils.randomNumeric(4) + ".md";
        File newFile = new File(dateFolder, fileName);
        try (FileWriter writer = new FileWriter(newFile)) {
            writer.write(recommend);
        }

        // 提交并推送
        git.add().addFilepattern(dateFolderName + "/" + fileName).call();
        git.commit().setMessage("add code review new file" + fileName).call();
        git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubToken, "")).call();

        logger.info("openai-code-review git commit and push done! {}", fileName);

        return githubReviewLogUri + "/blob/master/" + dateFolderName + "/" + fileName;
    }

    // ==================== Getters ====================

    /** 获取项目名称 */
    public String getProject() {
        return project;
    }

    /** 获取分支名称 */
    public String getBranch() {
        return branch;
    }

    /** 获取提交作者 */
    public String getAuthor() {
        return author;
    }

    /** 获取提交信息 */
    public String getMessage() {
        return message;
    }
}
