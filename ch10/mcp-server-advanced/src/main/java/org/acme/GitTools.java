package org.acme;


import io.quarkiverse.mcp.server.Root;
import io.quarkiverse.mcp.server.Roots;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolGuardrails;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class GitTools {

    @Inject
    Logger logger;

    public record GitFiles(Set<String> uncommitted, Set<String> untracked){}


    @ToolGuardrails(input = DirectoryToolInputGuardrail.class)
    @Tool(description = "Gets the untracked and uncommitted files of a Git repo")
    public GitFiles status(
                            @ToolArg(description = "Git directory") @NotBlank String directory,
                         Roots roots) throws IOException, GitAPIException {
        logger.info("Gets Git status");

        if(roots.isSupported()) {
            List<Root> rootsDirectories = roots.listAndAwait();
            logger.info(rootsDirectories.getFirst().uri());
            URI gitRepo = URI.create(
                            rootsDirectories.getFirst().uri())
                    .resolve(directory);

            logger.infof("Git directory %s", gitRepo);

            File repo = new File(gitRepo);
            try (Git git = Git.open(repo)) {
                Status status = git.status().call();
                return new GitFiles(status.getUncommittedChanges(), status.getUntracked());
            }
        } else {
            return new GitFiles(Collections.emptySet(), Collections.emptySet());
        }
    }

}
