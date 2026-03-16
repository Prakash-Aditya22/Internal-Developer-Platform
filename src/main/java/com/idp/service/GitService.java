package com.idp.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

@Service
@Slf4j
public class GitService {

  @Value("${idp.workspace.base-path:${java.io.tmpdir}/idp-workspaces}")
  private String workspaceBasePath;

  public File cloneRepository(String repoUrl, String branch) throws GitAPIException, IOException {
    String workspaceId = UUID.randomUUID().toString().substring(0, 8);
    Path workspacePath = Path.of(workspaceBasePath, workspaceId);
    Files.createDirectories(workspacePath);

    log.info("Cloning repository {} (branch: {}) to {}", repoUrl, branch, workspacePath);

    try (Git git = Git.cloneRepository()
        .setURI(repoUrl)
        .setDirectory(workspacePath.toFile())
        .setBranch(branch)
        .setCloneAllBranches(false)
        .setDepth(1)
        .call()) {

      log.info("Repository cloned successfully to {}", workspacePath);
      return workspacePath.toFile();
    }
  }

  public String getLatestCommitHash(File repoDir) throws GitAPIException, IOException {
    try (Git git = Git.open(repoDir)) {
      Iterable<RevCommit> commits = git.log().setMaxCount(1).call();
      for (RevCommit commit : commits) {
        return commit.getName();
      }
    }
    return null;
  }

  public String getLatestCommitMessage(File repoDir) throws GitAPIException, IOException {
    try (Git git = Git.open(repoDir)) {
      Iterable<RevCommit> commits = git.log().setMaxCount(1).call();
      for (RevCommit commit : commits) {
        return commit.getShortMessage();
      }
    }
    return null;
  }

  public void pullLatestChanges(File repoDir, String branch) throws GitAPIException, IOException {
    log.info("Pulling latest changes for branch {} in {}", branch, repoDir);

    try (Git git = Git.open(repoDir)) {
      git.fetch().call();
      git.checkout().setName(branch).call();
      git.pull().call();
      log.info("Repository updated successfully");
    }
  }

  public void checkoutBranch(File repoDir, String branch) throws GitAPIException, IOException {
    try (Git git = Git.open(repoDir)) {
      // Check if branch exists locally
      boolean branchExists = git.branchList().call().stream()
          .anyMatch(ref -> ref.getName().equals("refs/heads/" + branch));

      if (branchExists) {
        git.checkout().setName(branch).call();
      } else {
        // Create local branch tracking remote
        git.checkout()
            .setCreateBranch(true)
            .setName(branch)
            .setStartPoint("origin/" + branch)
            .call();
      }
      log.info("Checked out branch: {}", branch);
    }
  }

  public void checkoutCommit(File repoDir, String commitHash) throws GitAPIException, IOException {
    try (Git git = Git.open(repoDir)) {
      git.checkout().setName(commitHash).call();
      log.info("Checked out commit: {}", commitHash);
    }
  }

  public boolean isValidRepository(File repoDir) {
    try (Git git = Git.open(repoDir)) {
      return git.getRepository().getDirectory().exists();
    } catch (Exception e) {
      return false;
    }
  }

  public void cleanWorkspace(File workspaceDir) {
    if (workspaceDir == null || !workspaceDir.exists()) {
      return;
    }

    log.info("Cleaning workspace: {}", workspaceDir);
    try {
      Files.walk(workspaceDir.toPath())
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
      log.info("Workspace cleaned successfully");
    } catch (IOException e) {
      log.error("Failed to clean workspace: {}", e.getMessage(), e);
    }
  }

  public Ref getCurrentBranch(File repoDir) throws GitAPIException, IOException {
    try (Git git = Git.open(repoDir)) {
      return git.getRepository().exactRef("HEAD");
    }
  }
}
