package plus.gaga.middleware.sdk.infrastructure.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plus.gaga.middleware.sdk.agent.ReviewRequest;
import plus.gaga.middleware.sdk.types.utils.RandomStringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GitCommand {

    private static final Logger logger = LoggerFactory.getLogger(GitCommand.class);

    private final String githubReviewLogUri;
    private final String githubToken;
    private final String project;
    private final String branch;
    private final String author;
    private final String message;

    public GitCommand(String githubReviewLogUri, String githubToken, String project, String branch, String author, String message) {
        this.githubReviewLogUri = githubReviewLogUri;
        this.githubToken = githubToken;
        this.project = project;
        this.branch = branch;
        this.author = author;
        this.message = message;
    }

    public String diff() throws IOException, InterruptedException {
        return diff(null);
    }

    public String diff(String commitRange) throws IOException, InterruptedException {
        ProcessBuilder diffProcessBuilder;
        if (commitRange == null || commitRange.trim().isEmpty()) {
            ProcessBuilder logProcessBuilder = new ProcessBuilder("git", "log", "-1", "--pretty=format:%H");
            logProcessBuilder.directory(new File("."));
            Process logProcess = logProcessBuilder.start();

            String latestCommitHash;
            try (BufferedReader logReader = new BufferedReader(new InputStreamReader(logProcess.getInputStream()))) {
                latestCommitHash = logReader.readLine();
            }
            logProcess.waitFor();
            diffProcessBuilder = new ProcessBuilder("git", "diff", latestCommitHash + "^", latestCommitHash);
        } else {
            diffProcessBuilder = new ProcessBuilder("git", "diff", commitRange);
        }

        diffProcessBuilder.directory(new File("."));
        Process diffProcess = diffProcessBuilder.start();

        StringBuilder diffCode = new StringBuilder();
        try (BufferedReader diffReader = new BufferedReader(new InputStreamReader(diffProcess.getInputStream()))) {
            String line;
            while ((line = diffReader.readLine()) != null) {
                diffCode.append(line).append("\n");
            }
        }

        int exitCode = diffProcess.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Failed to get diff, exit code:" + exitCode);
        }
        return diffCode.toString();
    }

    public String commitAndPush(String recommend) throws Exception {
        return archive(null, recommend);
    }

    public String archive(ReviewRequest request, String recommend) throws Exception {
        try (Git git = Git.cloneRepository()
                .setURI(githubReviewLogUri + ".git")
                .setDirectory(new File("repo"))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubToken, ""))
                .call()) {

            String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            File dateFolder = new File("repo/" + dateFolderName);
            if (!dateFolder.exists()) {
                dateFolder.mkdirs();
            }

            String fileName = project + "-" + branch + "-" + author + System.currentTimeMillis() + "-" + RandomStringUtils.randomNumeric(4) + ".md";
            File newFile = new File(dateFolder, fileName);
            try (FileWriter writer = new FileWriter(newFile)) {
                writer.write(recommend);
            }

            git.add().addFilepattern(dateFolderName + "/" + fileName).call();
            git.commit().setMessage("add code review new file" + fileName).call();
            git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubToken, "")).call();

            logger.info("openai-code-review git commit and push done! {}", fileName);
            return githubReviewLogUri + "/blob/master/" + dateFolderName + "/" + fileName;
        }
    }

    public String getProject() {
        return project;
    }

    public String getBranch() {
        return branch;
    }

    public String getAuthor() {
        return author;
    }

    public String getMessage() {
        return message;
    }
}
