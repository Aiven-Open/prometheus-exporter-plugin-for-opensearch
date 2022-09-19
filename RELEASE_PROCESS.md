# Release Process

Right now the release process of the plugin is a manual process and some steps require admin permissions. In the future the process should be automated.

## The Goal of the Release Process

The main goal of the release process is a new commit in either `main` or other relevant branch with a unique git tag assigned to it which can be used to reliably used to build identical final artifact(s) (mainly the ZIP archive in this case).

## Release Process Steps

1. [\[Optional\] Upgrade OpenSearch to a new version](RELEASE_PROCESS.md#1-optional-upgrade-opensearch-to-a-new-version)
2. [Open a new ticket with release checklist](RELEASE_PROCESS.md#2-open-a-new-ticket-with-release-checklist)
3. [Create and push the release commit](RELEASE_PROCESS.md#3-create-and-push-the-release-commit)
4. [\[Optional\] Update Compatibility Matrix in the main branch](RELEASE_PROCESS.md#4-optional-update-compatibility-matrix-in-the-main-branch)
5. [Prepare new release draft on GitHub](RELEASE_PROCESS.md#5-prepare-new-release-draft-on-github)
6. [Build release artifacts and upload them](RELEASE_PROCESS.md#6-build-release-artifacts-and-upload-them)
7. [Verify that the artifacts work with OpenSearch](RELEASE_PROCESS.md#7-verify-that-the-artifacts-work-with-opensearch)
8. [Write release notes and publish the release on GitHub](RELEASE_PROCESS.md#8-write-release-notes-and-publish-the-release-on-github) 
9. [\[Optional\] OpenSearch forum announcement](RELEASE_PROCESS.md#9-optional-opensearch-forum-announcement)

## Detailed instructions

### 1. \[Optional\] Upgrade OpenSearch to a new version

If the release includes upgrade to a new version of OpenSearch it is recommended to do the upgrade as a separate PR. Just make the PR to only upgrade the OpenSearch version and do not mix it with any other release related changes (such as Compatibility Matrix updates or any other updates).

This is only to make sure that if the upgrade will need to be reverted then we will not have to delete any git tags or otherwise manipulate public git repo history.

- PR example: https://github.com/aiven/prometheus-exporter-plugin-for-opensearch/pull/62

### 2. Open a new ticket with release checklist

It is helpful to track individual release steps. A good practice is opening a new ticket with the checklist and update it during the process to make sure nothing is forgotten and that steps are happening in correct order.

- Ticket example: https://github.com/aiven/prometheus-exporter-plugin-for-opensearch/issues/61

### 3. Create and push the release commit

Now we will create the commit that will be assigned the new release git tag. This commits must include everything that is needed to recreate the release build artifact(s) at later time (including documentation updates if possible).

In most cases the commit will include only updates to the README.md file. Specifically update to Compatibility Matrix and update to plugin install command.

_When updating the Compatibility Matrix information then remember that the release date is the date of the plugin release not the release date of OpenSearch._

```shell
# Do in our local repo clone
$ git checkout -b release_1.3.3.0

# Update the Compatibility Matrix and installation command example
$ vi README.md
$ git add README.md

# Check which files will be part of the commit
# Make sure you know what is going into this commit 
$ git status
$ git commit -s -m "Release 1.3.3.0"

# Push the commit into your clone and open new PR in upstream repo
$ git push <our_clone> release_1.3.3.0 
```
Notice that the commit must be `signed` (it is requirement of our [Contribution policy](CONTRIBUTING.md#developer-certificate-of-origin)).

When opening the PR pay attention to target branch. If you prepare release for older version of the plugin the chance is that it will not go against the `main` branch but against different branch (such as `v1.3` or similar).

- Example of commit: https://github.com/aiven/prometheus-exporter-plugin-for-opensearch/pull/63

If this commit passes CI tests and is merged then **the repo admin will create a new release tag** for it and push that tag into repo (`upstream` is the repo under the Aiven GitHub org).

```shell
# Preparation: recommended steps to pull all updates from upstream including tags
$ git checkout <branch>
$ git pull upstream <branch>
$ git fetch upstream --tags

# Create the release tag
# If the new release is 1.3.3.0
$ git tag -s -a 1.3.3.0 -m "Release 1.3.3.0" 

# Verify the tag before pushing
$ git tag -v 1.3.3.0

# Finally push the tag to upstream
$ git push upstream 1.3.3.0
```

### 4. \[Optional\] Update Compatibility Matrix in the main branch

When doing releases of older versions of the plugin it can happen that the Compatibility Matrix update do not go into the `main` branch. In this case it is useful to bring this update to the `main` branch as well.

### 5. Prepare new release draft on GitHub

Prepare new Release Draft based on the new release tag.

- Tip: Get some inspiration from our [past releases](https://github.com/aiven/prometheus-exporter-plugin-for-opensearch/releases).

### 6. Build release artifacts and upload them

To build the plugin from sources you need JDK of specific version. To learn which version is required check the GitHub CI definition file, it will always tell you the correct version.

```shell
$ cat .github/workflows/CI.yml
```

If you have [yq](https://mikefarah.gitbook.io/yq/) processor installed you can do:
```shell
$ cat .github/workflows/CI.yml | yq .jobs.build.strategy.matrix.java
```

Build the artifacts:

```shell
$ ./gradlew clean build
```
You will find final artifact (the ZIP archive) in the following location:

```shell
$ file -b ./build/distributions/prometheus-exporter-1.3.3.0.zip
Zip archive data, at least v1.0 to extract, compression method=deflate
```

Upload this file to the Release Draft.

### 7. Verify that the artifacts work with OpenSearch

The goal in this step is to verify that freshly downloaded plugin release artifact can be installed into OpenSearch and that OpenSearch can run and use it (kind of "smoke test").

I usually download appropriate version of the [minimal](https://opensearch.org/downloads.html#minimal) OpenSearch distribution and then I download the released artifact from the GitHub site. It is possible to download it from the Release Draft as well but the URL is a bit different. Then I use the `opensearch-plugin install` CLI to install the plugin from the Downloads folder into OpenSearch.

```shell
$ <opensearch_home>/bin/opensearch-plugin install file:///Users/${whoami}/Downloads/prometheus-exporter-1.3.3.0.zip
```

### 8. Write release notes and publish the release on GitHub

If the testing went fine, you write release notes and publish the release. 

### 9. \[Optional\] OpenSearch forum announcement

Let the community know. Create a new announcement in `OpenSearch` category and do not forget to add `releases` tag.

- Example: https://forum.opensearch.org/t/prometheus-exporter-plugin-1-3-3-0-released/9936

## Done!

It was a lot of steps and you deserve a special reward! Go have a nice cup of tea or coffee.
